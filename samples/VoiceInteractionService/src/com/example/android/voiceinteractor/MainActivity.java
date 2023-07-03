/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.example.android.voiceinteractor;

import static android.media.AudioTrack.PLAYSTATE_PAUSED;
import static android.media.AudioTrack.PLAYSTATE_STOPPED;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.service.voice.HotwordDetector;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    private static final String TAG = "VIS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        attachClickListener(R.id.buffer1, "1");
        attachClickListener(R.id.buffer2, "2");

        Button button = (Button) findViewById(R.id.startReco);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService == null) {
                    Log.e(TAG, "No service");
                    return;
                }
                mService.mHotwordDetector.startRecognition();
            }
        });
        button = (Button) findViewById(R.id.directRecord);
        button.setOnClickListener(v -> {
            if (mService == null) {
                Log.e(TAG, "No service");
                return;
            }
            mService.mHotwordDetectorCallback.onDetected(mService.mLastPayload, true);
        });
    }

    private void attachClickListener(int id, String key) {
        Button button = (Button) findViewById(id);
        button.setOnClickListener(v -> {
            if (mService == null) {
                Log.e(TAG, "No service");
                return;
            }
            playAudio(mService.mData.getByteArray(key));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, SampleVoiceInteractionService.class).setAction("local");
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    SampleVoiceInteractionService mService;
    boolean mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SampleVoiceInteractionService.LocalBinder binder = (SampleVoiceInteractionService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.i(TAG, "Connected to local service");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
            mBound = false;
        }
    };

    private void playAudio(byte[] buffer) {
        AudioTrack track = new AudioTrack(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
//                        .setInternalCapturePreset(MediaRecorder.AudioSource.HOTWORD)
                        .build(),
                new AudioFormat.Builder()
                        .setChannelMask(AudioFormat.CHANNEL_IN_DEFAULT)
                        .setSampleRate(mService.mAudioFormat.getSampleRate())
                        .setEncoding(mService.mAudioFormat.getEncoding())
                        .build(),
//                mService.mAudioFormat,
                buffer.length,
                AudioTrack.MODE_STATIC,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );
        Log.i(TAG, "track state=" + track.getState());
        if (track.getState() == AudioTrack.STATE_UNINITIALIZED) {
            return;
        }
        track.write(buffer, 0, buffer.length);
//        track.setNotificationMarkerPosition(track.getP)
        track.play();
        // TODO: Doesn't work.. fix the releasing.
        track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {

            @Override
            public void onMarkerReached(AudioTrack track) {

            }

            @Override
            public void onPeriodicNotification(AudioTrack track) {
                if (track.getPlayState() == PLAYSTATE_STOPPED
                        || track.getPlayState() == PLAYSTATE_PAUSED) {
                    Log.i(TAG, "Stopped/paused playback; releasing.");
                    track.release();
                }
            }
        });
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            Thread.interrupted();
//            throw new RuntimeException(e);
//        }
//        track.release();

//        MediaPlayer player = new MediaPlayer();
//        player.setAudioAttributes(
//                new AudioAttributes.Builder()
//                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                        .setUsage(AudioAttributes.USAGE_MEDIA)
//                        .set
//                        .build());
//        player.setDataSource(
//                "data:audio/mp3;base64,"
////                new ByteArrayMediaSource(buffer)
//        );
//        try {
//            player.prepare();
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to play: " + e);
//        }
//        player.start();
    }

//    private static class ByteArrayMediaSource extends MediaDataSource {
//        final byte[] mData;
//
//        public ByteArrayMediaSource(byte[] data) {
//            mData = data;
//        }
//
//        @Override
//        public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
//            if (position >= mData.length) {
//                return -1; // end of stream
//            }
//            if (position + size > mData.length) {
//                size = (int) (mData.length - position);
//            }
//
//            System.arraycopy(mData, (int) position, buffer, offset, size);
//            return size;
//        }
//
//        @Override
//        public long getSize() throws IOException {
//            return 0;
//        }
//
//        @Override
//        public void close() throws IOException {
//
//        }
//    }
}
