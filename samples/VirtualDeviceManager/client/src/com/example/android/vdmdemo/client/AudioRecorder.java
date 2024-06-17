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

package com.example.android.vdmdemo.client;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.example.android.vdmdemo.common.RemoteEventProto;
import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.protobuf.ByteString;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class AudioRecorder implements Consumer<RemoteEvent> {
    private static final String TAG = AudioRecorder.class.getSimpleName();

    private final Object mLock = new Object();
    private StreamingThread mStreamingThread;
    private final RemoteIo mRemoteIo;

    @Inject
    AudioRecorder(RemoteIo remoteIo) {
        this.mRemoteIo = remoteIo;
    }

    private void startRecording(int sampleRate, int channelMask, int encoding) {
        synchronized (mLock) {
            if (mStreamingThread != null) {
                Log.w(TAG, "Received startPlayback command without stopping the playback first");
                stopRecording();
            }

            AudioFormat audioFormat =
                    new AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(encoding)
                            .setChannelMask(channelMask)
                            .build();
            int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelMask, encoding);
            int bufferSize = 4 * minBufferSize;

            AudioRecord audioRecord =
                    new AudioRecord.Builder()
                            .setAudioFormat(audioFormat)
                            .setBufferSizeInBytes(bufferSize)
                            .build();
            mStreamingThread = new StreamingThread(audioRecord, bufferSize, mRemoteIo);
            mStreamingThread.start();
        }
    }

    private void stopRecording() {
        synchronized (mLock) {
            if (mStreamingThread == null) {
                return;
            }

            mStreamingThread.stopStreaming();
            mStreamingThread = null;
        }
    }

    public void stop() {
        stopRecording();
    }

    @Override
    public void accept(RemoteEvent remoteEvent) {
        if (remoteEvent.hasStartAudioInput()) {
            RemoteEventProto.StartAudioInput input = remoteEvent.getStartAudioInput();
            startRecording(input.getSampleRate(), input.getChannelMask(), input.getEncoding());
        }
        if (remoteEvent.hasStopAudioInput()) {
            stopRecording();
        }
    }

    private static class StreamingThread extends Thread {
        private final AtomicBoolean mIsRunning = new AtomicBoolean(true);
        private final AudioRecord mAudioRecord;
        private final int mBufferSize;
        private final RemoteIo mRemoteIo;

        StreamingThread(AudioRecord audioRecord, int bufferSize, RemoteIo remoteIo) {
            super();
            mAudioRecord = audioRecord;
            mBufferSize = bufferSize;
            mRemoteIo = remoteIo;
        }

        @Override
        public void run() {
            super.run();

            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Audio record is not initialized");
                return;
            }

            mRemoteIo.sendMessage(
                    RemoteEvent.newBuilder()
                            .setStartAudio(RemoteEventProto.StartAudio.newBuilder())
                            .build());
            mAudioRecord.startRecording();
            byte[] buffer = new byte[mBufferSize];
            while (mIsRunning.get()) {
                int ret = mAudioRecord.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                if (ret <= 0) {
                    Log.e(TAG, "AudioRecord.read returned error code " + ret);
                    continue;
                }

                mRemoteIo.sendMessage(
                        RemoteEvent.newBuilder()
                                .setAudioFrame(
                                        AudioFrame.newBuilder()
                                                .setData(ByteString.copyFrom(buffer, 0, ret)))
                                .build());
            }
            mAudioRecord.stop();
            mAudioRecord.release();
            mRemoteIo.sendMessage(
                    RemoteEvent.newBuilder()
                            .setStopAudio(RemoteEventProto.StopAudio.newBuilder())
                            .build());
        }

        void stopStreaming() {
            mIsRunning.set(false);
        }
    }
}
