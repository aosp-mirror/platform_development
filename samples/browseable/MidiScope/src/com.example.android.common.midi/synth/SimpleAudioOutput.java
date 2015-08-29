/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.common.midi.synth;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Simple base class for implementing audio output for examples.
 * This can be sub-classed for experimentation or to redirect audio output.
 */
public class SimpleAudioOutput {

    private static final String TAG = "AudioOutputTrack";
    public static final int SAMPLES_PER_FRAME = 2;
    public static final int BYTES_PER_SAMPLE = 4; // float
    public static final int BYTES_PER_FRAME = SAMPLES_PER_FRAME * BYTES_PER_SAMPLE;
    private AudioTrack mAudioTrack;
    private int mFrameRate;

    /**
     *
     */
    public SimpleAudioOutput() {
        super();
    }

    /**
     * Create an audio track then call play().
     *
     * @param frameRate
     */
    public void start(int frameRate) {
        stop();
        mFrameRate = frameRate;
        mAudioTrack = createAudioTrack(frameRate);
        // AudioTrack will wait until it has enough data before starting.
        mAudioTrack.play();
    }

    public AudioTrack createAudioTrack(int frameRate) {
        int minBufferSizeBytes = AudioTrack.getMinBufferSize(frameRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT);
        Log.i(TAG, "AudioTrack.minBufferSize = " + minBufferSizeBytes
                + " bytes = " + (minBufferSizeBytes / BYTES_PER_FRAME)
                + " frames");
        int bufferSize = 8 * minBufferSizeBytes / 8;
        int outputBufferSizeFrames = bufferSize / BYTES_PER_FRAME;
        Log.i(TAG, "actual bufferSize = " + bufferSize + " bytes = "
                + outputBufferSizeFrames + " frames");

        AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC,
                mFrameRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_FLOAT, bufferSize,
                AudioTrack.MODE_STREAM);
        Log.i(TAG, "created AudioTrack");
        return player;
    }

    public int write(float[] buffer, int offset, int length) {
        return mAudioTrack.write(buffer, offset, length,
                AudioTrack.WRITE_BLOCKING);
    }

    public void stop() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }

    public int getFrameRate() {
        return mFrameRate;
    }

    public AudioTrack getAudioTrack() {
        return mAudioTrack;
    }
}
