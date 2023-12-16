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

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import com.example.android.vdmdemo.common.RemoteEventProto.AudioFrame;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
final class AudioPlayer implements Consumer<RemoteEvent> {
    private static final String TAG = AudioPlayer.class.getSimpleName();

    private static final int SAMPLE_RATE = 44000;
    private static final AudioFormat AUDIO_FORMAT =
            new AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build();
    private static final AudioAttributes AUDIO_ATTRIBUTES =
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build();
    private static final int MIN_AUDIOTRACK_BUFFER_SIZE =
            AudioTrack.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
    private static final int AUDIOTRACK_BUFFER_SIZE = 4 * MIN_AUDIOTRACK_BUFFER_SIZE;

    private final Object mLock = new Object();
    private AudioTrack mAudioTrack;

    @Inject
    AudioPlayer() {}

    private void startPlayback() {
        synchronized (mLock) {
            if (mAudioTrack != null) {
                Log.w(TAG, "Received startPlayback command without stopping the playback first");
                stopPlayback();
            }
            mAudioTrack =
                    new AudioTrack.Builder()
                            .setAudioFormat(AUDIO_FORMAT)
                            .setAudioAttributes(AUDIO_ATTRIBUTES)
                            .setBufferSizeInBytes(AUDIOTRACK_BUFFER_SIZE)
                            .build();
            mAudioTrack.play();
        }
    }

    private void playAudioFrame(AudioFrame audioFrame) {
        byte[] data = audioFrame.getData().toByteArray();
        int bytesToWrite = data.length;
        if (bytesToWrite == 0) {
            return;
        }
        int bytesWritten = 0;
        synchronized (mLock) {
            if (mAudioTrack == null) {
                Log.e(TAG, "Received audio frame, but audio track was not initialized yet");
                return;
            }

            while (bytesToWrite > 0) {
                int ret = mAudioTrack.write(data, bytesWritten, bytesToWrite);
                if (ret <= 0) {
                    Log.e(TAG, "AudioTrack.write returned error code " + ret);
                }
                bytesToWrite -= ret;
                bytesWritten += ret;
            }
        }
    }

    private void stopPlayback() {
        synchronized (mLock) {
            if (mAudioTrack == null) {
                Log.w(TAG, "Received stopPlayback command for already stopped playback");
            } else {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            }
        }
    }

    @Override
    public void accept(RemoteEvent remoteEvent) {
        if (remoteEvent.hasStartAudio()) {
            startPlayback();
        }
        if (remoteEvent.hasAudioFrame()) {
            playAudioFrame(remoteEvent.getAudioFrame());
        }
        if (remoteEvent.hasStopAudio()) {
            stopPlayback();
        }
    }
}
