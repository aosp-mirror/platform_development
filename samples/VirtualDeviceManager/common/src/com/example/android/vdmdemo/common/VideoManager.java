/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.vdmdemo.common;

import static com.google.common.util.concurrent.Uninterruptibles.putUninterruptibly;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import com.example.android.vdmdemo.common.RemoteEventProto.EncodedFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.google.protobuf.ByteString;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Shared class between the client and the host, managing the video encoding and decoding. */
public class VideoManager {
    private static final String TAG = "VideoManager";
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

    @GuardedBy("mCodecLock")
    private MediaCodec mMediaCodec;

    private final Object mCodecLock = new Object();
    private final HandlerThread mCallbackThread;
    private final boolean mRecordEncoderOutput;
    private final BlockingQueue<EncodedFrame> mEventQueue = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<Integer> mFreeInputBuffers = new LinkedBlockingQueue<>(100);
    private final RemoteIo mRemoteIo;
    private final Consumer<RemoteEvent> mRemoteFrameConsumer = this::processFrameProto;
    private StorageFile mStorageFile;
    private DecoderThread mDecoderThread;

    private VideoManagerProtoHelper mProtoHelper;

    private interface VideoManagerProtoHelper {

        Optional<EncodedFrame> extractEncodedFrame(RemoteEvent event);

        RemoteEvent createFrameProto(byte[] data, int flags, long presentationTimeUs);

        String getVideoManagerId();
    }

    private VideoManager(
            VideoManagerProtoHelper protoHelper, RemoteIo remoteIo, MediaCodec mediaCodec,
            boolean recordEncoderOutput) {
        mProtoHelper = protoHelper;
        mRemoteIo = remoteIo;
        mMediaCodec = mediaCodec;
        mRecordEncoderOutput = recordEncoderOutput;

        mCallbackThread = new HandlerThread("VideoManager-" + protoHelper.getVideoManagerId());
        mCallbackThread.start();
        mediaCodec.setCallback(new MediaCodecCallback(), new Handler(mCallbackThread.getLooper()));

        if (!mediaCodec.getCodecInfo().isEncoder()) {
            remoteIo.addMessageConsumer(mRemoteFrameConsumer);
        }

        if (recordEncoderOutput) {
            mStorageFile = new StorageFile(protoHelper.getVideoManagerId());
        }
    }

    /** Creates a VideoManager instance for encoding display stream. */
    public static VideoManager createDisplayEncoder(
            int displayId, RemoteIo remoteIo, boolean recordEncoderOutput) {
        try {
            MediaCodec mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            return new VideoManager(new DisplayProtoHelper(displayId), remoteIo, mediaCodec,
                    recordEncoderOutput);
        } catch (IOException e) {
            throw new AssertionError("Unhandled exception", e);
        }
    }

    /** Creates a VideoManager instance for decoding display stream. */
    public static VideoManager createDisplayDecoder(int displayId, RemoteIo remoteIo) {
        try {
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            return new VideoManager(new DisplayProtoHelper(displayId), remoteIo, mediaCodec, false);
        } catch (IOException e) {
            throw new AssertionError("Unhandled exception", e);
        }
    }

    /** Creates a VideoManager instance for encoding camera stream. */
    public static VideoManager createCameraEncoder(
            String cameraId, RemoteIo remoteIo, boolean recordEncoderOutput) {
        try {
            MediaCodec mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            return new VideoManager(new CameraProtoHelper(cameraId), remoteIo, mediaCodec,
                    recordEncoderOutput);
        } catch (IOException e) {
            throw new AssertionError("Unhandled exception", e);
        }
    }

    /** Creates a VideoManager instance for decoding camera stream. */
    public static VideoManager createCameraDecoder(String cameraId, RemoteIo remoteIo) {
        try {
            MediaCodec mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
            return new VideoManager(new CameraProtoHelper(cameraId), remoteIo, mediaCodec, false);
        } catch (IOException e) {
            throw new AssertionError("Unhandled exception", e);
        }
    }

    /** Stops processing and resets the internal state. */
    public void stop() {
        synchronized (mCodecLock) {
            if (mMediaCodec == null) {
                return;
            }
            if (mMediaCodec.getCodecInfo().isEncoder()) {
                mMediaCodec.signalEndOfInputStream();
            } else {
                mRemoteIo.removeMessageConsumer(mRemoteFrameConsumer);
                mEventQueue.clear();
                mDecoderThread.exit();
            }
            mCallbackThread.quitSafely();
            try {
                mMediaCodec.flush();
                mMediaCodec.stop();
            } catch (IllegalStateException exception) {
                // It's possible the codec transitioned from "executing" state, because
                // the surface was already released.
                Log.w(TAG, "IllegalStateException while flushing codec", exception);
            }
            mMediaCodec.release();
            mMediaCodec = null;
        }
        if (mRecordEncoderOutput) {
            mStorageFile.closeOutputFile();
        }
    }

    /** Creates a surface for encoding. */
    public Surface createInputSurface(int width, int height, int frameRate) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(
                MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500000);
        mediaFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        synchronized (mCodecLock) {
            mMediaCodec.configure(
                    mediaFormat, /* surface= */ null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            return mMediaCodec.createInputSurface();
        }
    }

    /** Starts encoding. {@link #createInputSurface} must have been called already. */
    public void startEncoding() {
        synchronized (mCodecLock) {
            mMediaCodec.start();
        }
    }

    /** Starts decoding from the given surface. */
    public void startDecoding(Surface surface, int width, int height) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 100);
        synchronized (mCodecLock) {
            mMediaCodec.configure(mediaFormat, surface, null, 0);
            mMediaCodec.start();
        }
        mDecoderThread = new DecoderThread();
        mDecoderThread.start();
    }

    private void processFrameProto(RemoteEvent event) {
        mProtoHelper.extractEncodedFrame(event).ifPresent(
                encodedFrame -> putUninterruptibly(mEventQueue, encodedFrame));
    }

    private final class MediaCodecCallback extends MediaCodec.Callback {
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int i) {
            mFreeInputBuffers.add(i);
        }

        @Override
        public void onOutputBufferAvailable(
                @NonNull MediaCodec codec, int i, @NonNull BufferInfo bufferInfo) {
            synchronized (mCodecLock) {
                if (mMediaCodec == null) {
                    return;
                }
                if (mMediaCodec.getCodecInfo().isEncoder()) {
                    ByteBuffer buffer = mMediaCodec.getOutputBuffer(i);
                    byte[] data = new byte[bufferInfo.size];
                    Objects.requireNonNull(buffer).get(data, bufferInfo.offset, bufferInfo.size);
                    mMediaCodec.releaseOutputBuffer(i, false);
                    if (mRecordEncoderOutput) {
                        mStorageFile.writeOutputFile(data);
                    }

                    mRemoteIo.sendMessage(
                            mProtoHelper.createFrameProto(
                                    data, bufferInfo.flags, bufferInfo.presentationTimeUs));
                } else {
                    try {
                        mMediaCodec.releaseOutputBuffer(i, true);
                    } catch (CodecException exception) {
                        Log.e(TAG, "Codec exception ", exception);
                    }
                }
            }
        }

        @Override
        public void onError(@NonNull MediaCodec mediaCodec, @NonNull CodecException e) {
        }

        @Override
        public void onOutputFormatChanged(
                @NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
        }
    }

    private class DecoderThread extends Thread {

        private final AtomicBoolean mExit = new AtomicBoolean(false);

        @SuppressWarnings("Interruption")
        void exit() {
            mExit.set(true);
            interrupt();
        }

        @Override
        public void run() {
            while (!(Thread.interrupted() && mExit.get())) {
                try {
                    EncodedFrame encodedFrame = mEventQueue.take();
                    int inputBuffer = mFreeInputBuffers.take();

                    synchronized (mCodecLock) {
                        if (mMediaCodec == null) {
                            continue;
                        }
                        try {
                            ByteBuffer inBuffer = mMediaCodec.getInputBuffer(inputBuffer);
                            byte[] data = encodedFrame.getFrameData().toByteArray();
                            Objects.requireNonNull(inBuffer).put(data);
                            if (mRecordEncoderOutput) {
                                mStorageFile.writeOutputFile(data);
                            }
                            mMediaCodec.queueInputBuffer(
                                    inputBuffer,
                                    0,
                                    encodedFrame.getFrameData().size(),
                                    encodedFrame.getPresentationTimeUs(),
                                    encodedFrame.getFlags());
                        } catch (MediaCodec.CodecException exception) {
                            Log.e(TAG, "MediaCodec exception while queuing input", exception);
                            mMediaCodec.release();
                            mMediaCodec = null;
                        }
                    }
                } catch (InterruptedException e) {
                    if (mExit.get()) {
                        break;
                    }
                }
            }
        }
    }

    private static class StorageFile {
        private static final String DIR = "Download";
        private static final String FILENAME = "vdmdemo_encoder_output";

        private OutputStream mOutputStream;

        private StorageFile(String id) {
            String filePath = DIR + "/" + FILENAME + "_" + id + ".h264";
            File f = new File(Environment.getExternalStorageDirectory(), filePath);
            try {
                mOutputStream = new BufferedOutputStream(new FileOutputStream(f));
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Error creating or opening storage file", e);
            }
        }

        private void writeOutputFile(byte[] data) {
            if (mOutputStream == null) {
                return;
            }
            try {
                mOutputStream.write(data);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output file", e);
            }
        }

        private void closeOutputFile() {
            if (mOutputStream == null) {
                return;
            }
            try {
                mOutputStream.flush();
                mOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing output file", e);
            }
        }
    }

    private static class DisplayProtoHelper implements VideoManagerProtoHelper {
        private final int mDisplayId;
        private int mFrameIndex = 0;

        DisplayProtoHelper(int displayId) {
            mDisplayId = displayId;
        }

        @Override
        public Optional<EncodedFrame> extractEncodedFrame(RemoteEvent event) {
            if (event.hasDisplayFrame() && event.getDisplayId() == mDisplayId) {
                return Optional.of(event.getDisplayFrame());
            }
            return Optional.empty();
        }

        @Override
        public RemoteEvent createFrameProto(byte[] data, int flags, long presentationTimeUs) {
            return RemoteEvent.newBuilder()
                    .setDisplayId(mDisplayId)
                    .setDisplayFrame(
                            EncodedFrame.newBuilder()
                                    .setFrameData(ByteString.copyFrom(data))
                                    .setFrameIndex(mFrameIndex++)
                                    .setPresentationTimeUs(presentationTimeUs)
                                    .setFlags(flags))
                    .build();
        }

        @Override
        public String getVideoManagerId() {
            return "display" + mDisplayId;
        }
    }

    private static class CameraProtoHelper implements VideoManagerProtoHelper {
        private final String mCameraId;
        private int mFrameIndex = 0;

        CameraProtoHelper(String cameraId) {
            mCameraId = cameraId;
        }

        @Override
        public Optional<EncodedFrame> extractEncodedFrame(RemoteEvent event) {
            if (event.hasCameraFrame() && event.getCameraFrame().getCameraId().equals(mCameraId)) {
                Log.d(TAG, "Received encoded frame "
                        + event.getCameraFrame().getCameraFrame().getFrameIndex());
                return Optional.of(event.getCameraFrame().getCameraFrame());
            }
            return Optional.empty();
        }

        @Override
        public RemoteEvent createFrameProto(byte[] data, int flags, long presentationTimeUs) {
            Log.d(TAG, "Sending " + data.length + "B encoded camera frame");
            return RemoteEvent.newBuilder()
                    .setCameraFrame(
                            RemoteEventProto.CameraFrame.newBuilder()
                                    .setCameraId(mCameraId)
                                    .setCameraFrame(
                                            EncodedFrame.newBuilder()
                                                    .setFrameData(ByteString.copyFrom(data))
                                                    .setFrameIndex(mFrameIndex++)
                                                    .setPresentationTimeUs(presentationTimeUs)
                                                    .setFlags(flags))
                    )
                    .build();
        }

        @Override
        public String getVideoManagerId() {
            return "camera" + mCameraId;
        }
    }
}
