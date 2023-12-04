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
import com.example.android.vdmdemo.common.RemoteEventProto.DisplayFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.protobuf.ByteString;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Shared class between the client and the host, managing the video encoding and decoding. */
public class VideoManager {
  private static final String TAG = "VideoManager";
  private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;

  @GuardedBy("codecLock")
  private MediaCodec mediaCodec;

  private final Object codecLock = new Object();
  private final HandlerThread callbackThread;
  private final boolean recordEncoderOutput;
  private final BlockingQueue<RemoteEvent> eventQueue = new LinkedBlockingQueue<>(100);
  private final BlockingQueue<Integer> freeInputBuffers = new LinkedBlockingQueue<>(100);
  private final RemoteIo remoteIo;
  private final Consumer<RemoteEvent> remoteFrameConsumer = this::processFrameProto;
  private final int displayId;
  private int frameIndex = 0;
  private StorageFile storageFile;
  private DecoderThread decoderThread;

  private VideoManager(
      int displayId, RemoteIo remoteIo, MediaCodec mediaCodec, boolean recordEncoderOutput) {
    this.displayId = displayId;
    this.remoteIo = remoteIo;
    this.mediaCodec = mediaCodec;
    this.recordEncoderOutput = recordEncoderOutput;

    callbackThread = new HandlerThread("VideoManager-" + displayId);
    callbackThread.start();
    mediaCodec.setCallback(mediaCodecCallback, new Handler(callbackThread.getLooper()));

    if (!mediaCodec.getCodecInfo().isEncoder()) {
      remoteIo.addMessageConsumer(remoteFrameConsumer);
    }

    if (recordEncoderOutput) {
      storageFile = new StorageFile(displayId);
    }
  }

  public static VideoManager createEncoder(
      int displayId, RemoteIo remoteIo, boolean recordEncoderOutput) {
    try {
      MediaCodec mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
      return new VideoManager(displayId, remoteIo, mediaCodec, recordEncoderOutput);
    } catch (IOException e) {
      throw new AssertionError("Unhandled exception", e);
    }
  }

  public static VideoManager createDecoder(int displayId, RemoteIo remoteIo) {
    try {
      MediaCodec mediaCodec = MediaCodec.createDecoderByType(MIME_TYPE);
      return new VideoManager(displayId, remoteIo, mediaCodec, false);
    } catch (IOException e) {
      throw new AssertionError("Unhandled exception", e);
    }
  }

  public void stop() {
    synchronized (codecLock) {
      if (mediaCodec == null) {
        return;
      }
      if (mediaCodec.getCodecInfo().isEncoder()) {
        mediaCodec.signalEndOfInputStream();
      } else {
        remoteIo.removeMessageConsumer(remoteFrameConsumer);
        eventQueue.clear();
        decoderThread.exit();
      }
      callbackThread.quitSafely();
      mediaCodec.flush();
      mediaCodec.stop();
      mediaCodec.release();
      mediaCodec = null;
    }
    if (recordEncoderOutput) {
      storageFile.closeOutputFile();
    }
  }

  public Surface createInputSurface(int width, int height, int frameRate) {
    MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
    mediaFormat.setInteger(
        MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 500000);
    mediaFormat.setInteger(MediaFormat.KEY_MAX_B_FRAMES, 0);
    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
    mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
    synchronized (codecLock) {
      mediaCodec.configure(
          mediaFormat, /* surface= */ null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
      return mediaCodec.createInputSurface();
    }
  }

  public void startEncoding() {
    synchronized (codecLock) {
      mediaCodec.start();
    }
  }

  public void startDecoding(Surface surface, int width, int height) {
    MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
    mediaFormat.setInteger(MediaFormat.KEY_LOW_LATENCY, 1);
    mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 100);
    synchronized (codecLock) {
      mediaCodec.configure(mediaFormat, surface, null, 0);
      mediaCodec.start();
    }
    decoderThread = new DecoderThread();
    decoderThread.start();
  }

  private RemoteEvent createFrameProto(byte[] data, int flags, long presentationTimeUs) {
    return RemoteEvent.newBuilder()
        .setDisplayId(displayId)
        .setDisplayFrame(
            DisplayFrame.newBuilder()
                .setFrameData(ByteString.copyFrom(data))
                .setFrameIndex(frameIndex++)
                .setPresentationTimeUs(presentationTimeUs)
                .setFlags(flags))
        .build();
  }

  private void processFrameProto(RemoteEvent event) {
    if (event.hasDisplayFrame() && event.getDisplayId() == displayId) {
      Uninterruptibles.putUninterruptibly(eventQueue, event);
    }
  }

  private final MediaCodec.Callback mediaCodecCallback =
      new MediaCodec.Callback() {
        @Override
        public void onInputBufferAvailable(MediaCodec codec, int i) {
          freeInputBuffers.add(i);
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int i, BufferInfo bufferInfo) {
          synchronized (codecLock) {
            if (mediaCodec == null) {
              return;
            }
            if (mediaCodec.getCodecInfo().isEncoder()) {
              ByteBuffer buffer = mediaCodec.getOutputBuffer(i);
              byte[] data = new byte[bufferInfo.size];
              buffer.get(data, bufferInfo.offset, bufferInfo.size);
              mediaCodec.releaseOutputBuffer(i, false);
              if (recordEncoderOutput) {
                storageFile.writeOutputFile(data);
              }

              remoteIo.sendMessage(
                  createFrameProto(data, bufferInfo.flags, bufferInfo.presentationTimeUs));
            } else {
              mediaCodec.releaseOutputBuffer(i, true);
            }
          }
        }

        @Override
        public void onError(MediaCodec mediaCodec, CodecException e) {}

        @Override
        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {}
      };

  private class DecoderThread extends Thread {

    private final AtomicBoolean exit = new AtomicBoolean(false);

    @SuppressWarnings("Interruption")
    void exit() {
      exit.set(true);
      interrupt();
    }

    @Override
    public void run() {
      while (!(Thread.interrupted() && exit.get())) {
        try {
          RemoteEvent event = eventQueue.take();
          int inputBuffer = freeInputBuffers.take();

          synchronized (codecLock) {
            if (mediaCodec == null) {
              continue;
            }
            ByteBuffer inBuffer = mediaCodec.getInputBuffer(inputBuffer);
            byte[] data = event.getDisplayFrame().getFrameData().toByteArray();
            inBuffer.put(data);
            if (recordEncoderOutput) {
              storageFile.writeOutputFile(data);
            }
            mediaCodec.queueInputBuffer(
                inputBuffer,
                0,
                event.getDisplayFrame().getFrameData().size(),
                event.getDisplayFrame().getPresentationTimeUs(),
                event.getDisplayFrame().getFlags());
          }
        } catch (InterruptedException e) {
          if (exit.get()) {
            break;
          }
        }
      }
    }
  }

  private static class StorageFile {
    private static final String DIR = "Download";
    private static final String FILENAME = "vdmdemo_encoder_output";

    private OutputStream outputStream;

    private StorageFile(int displayId) {
      String filePath = DIR + "/" + FILENAME + "_" + displayId + ".h264";
      File f = new File(Environment.getExternalStorageDirectory(), filePath);
      try {
        outputStream = new BufferedOutputStream(new FileOutputStream(f));
      } catch (FileNotFoundException e) {
        Log.e(TAG, "Error creating or opening storage file", e);
      }
    }

    private void writeOutputFile(byte[] data) {
      if (outputStream == null) {
        return;
      }
      try {
        outputStream.write(data);
      } catch (IOException e) {
        Log.e(TAG, "Error writing to output file", e);
      }
    }

    private void closeOutputFile() {
      if (outputStream == null) {
        return;
      }
      try {
        outputStream.flush();
        outputStream.close();
      } catch (IOException e) {
        Log.e(TAG, "Error closing output file", e);
      }
    }
  }
}
