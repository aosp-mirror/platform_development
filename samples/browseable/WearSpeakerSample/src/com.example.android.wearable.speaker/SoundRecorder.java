/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.speaker;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A helper class to provide methods to record audio input from the MIC to the internal storage
 * and to playback the same recorded audio file.
 */
public class SoundRecorder {

    private static final String TAG = "SoundRecorder";
    private static final int RECORDING_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord
            .getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);

    private final String mOutputFileName;
    private final AudioManager mAudioManager;
    private final Handler mHandler;
    private final Context mContext;
    private State mState = State.IDLE;

    private OnVoicePlaybackStateChangedListener mListener;
    private AsyncTask<Void, Void, Void> mRecordingAsyncTask;
    private AsyncTask<Void, Void, Void> mPlayingAsyncTask;

    enum State {
        IDLE, RECORDING, PLAYING
    }

    public SoundRecorder(Context context, String outputFileName,
            OnVoicePlaybackStateChangedListener listener) {
        mOutputFileName = outputFileName;
        mListener = listener;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mContext = context;
    }

    /**
     * Starts recording from the MIC.
     */
    public void startRecording() {
        if (mState != State.IDLE) {
            Log.w(TAG, "Requesting to start recording while state was not IDLE");
            return;
        }

        mRecordingAsyncTask = new AsyncTask<Void, Void, Void>() {

            private AudioRecord mAudioRecord;

            @Override
            protected void onPreExecute() {
                mState = State.RECORDING;
            }

            @Override
            protected Void doInBackground(Void... params) {
                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
                BufferedOutputStream bufferedOutputStream = null;
                try {
                    bufferedOutputStream = new BufferedOutputStream(
                            mContext.openFileOutput(mOutputFileName, Context.MODE_PRIVATE));
                    byte[] buffer = new byte[BUFFER_SIZE];
                    mAudioRecord.startRecording();
                    while (!isCancelled()) {
                        int read = mAudioRecord.read(buffer, 0, buffer.length);
                        bufferedOutputStream.write(buffer, 0, read);
                    }
                } catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
                    Log.e(TAG, "Failed to record data: " + e);
                } finally {
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mState = State.IDLE;
                mRecordingAsyncTask = null;
            }

            @Override
            protected void onCancelled() {
                if (mState == State.RECORDING) {
                    Log.d(TAG, "Stopping the recording ...");
                    mState = State.IDLE;
                } else {
                    Log.w(TAG, "Requesting to stop recording while state was not RECORDING");
                }
                mRecordingAsyncTask = null;
            }
        };

        mRecordingAsyncTask.execute();
    }

    public void stopRecording() {
        if (mRecordingAsyncTask != null) {
            mRecordingAsyncTask.cancel(true);
        }
    }

    public void stopPlaying() {
        if (mPlayingAsyncTask != null) {
            mPlayingAsyncTask.cancel(true);
        }
    }

    /**
     * Starts playback of the recorded audio file.
     */
    public void startPlay() {
        if (mState != State.IDLE) {
            Log.w(TAG, "Requesting to play while state was not IDLE");
            return;
        }

        if (!new File(mContext.getFilesDir(), mOutputFileName).exists()) {
            // there is no recording to play
            if (mListener != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onPlaybackStopped();
                    }
                });
            }
            return;
        }
        final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, FORMAT);

        mPlayingAsyncTask = new AsyncTask<Void, Void, Void>() {

            private AudioTrack mAudioTrack;

            @Override
            protected void onPreExecute() {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0 /* flags */);
                mState = State.PLAYING;
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE,
                            CHANNELS_OUT, FORMAT, intSize, AudioTrack.MODE_STREAM);
                    byte[] buffer = new byte[intSize * 2];
                    FileInputStream in = null;
                    BufferedInputStream bis = null;
                    mAudioTrack.setVolume(AudioTrack.getMaxVolume());
                    mAudioTrack.play();
                    try {
                        in = mContext.openFileInput(mOutputFileName);
                        bis = new BufferedInputStream(in);
                        int read;
                        while (!isCancelled() && (read = bis.read(buffer, 0, buffer.length)) > 0) {
                            mAudioTrack.write(buffer, 0, read);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to read the sound file into a byte array", e);
                    } finally {
                        try {
                            if (in != null) {
                                in.close();
                            }
                            if (bis != null) {
                                bis.close();
                            }
                        } catch (IOException e) { /* ignore */}

                        mAudioTrack.release();
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to start playback", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                cleanup();
            }

            @Override
            protected void onCancelled() {
                cleanup();
            }

            private void cleanup() {
                if (mListener != null) {
                    mListener.onPlaybackStopped();
                }
                mState = State.IDLE;
                mPlayingAsyncTask = null;
            }
        };

        mPlayingAsyncTask.execute();
    }

    public interface OnVoicePlaybackStateChangedListener {

        /**
         * Called when the playback of the audio file ends. This should be called on the UI thread.
         */
        void onPlaybackStopped();
    }

    /**
     * Cleans up some resources related to {@link AudioTrack} and {@link AudioRecord}
     */
    public void cleanup() {
        Log.d(TAG, "cleanup() is called");
        stopPlaying();
        stopRecording();
    }
}
