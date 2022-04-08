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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

/**
 * We first get the required permission to use the MIC. If it is granted, then we continue with
 * the application and present the UI with three icons: a MIC icon (if pressed, user can record up
 * to 10 seconds), a Play icon (if clicked, it wil playback the recorded audio file) and a music
 * note icon (if clicked, it plays an MP3 file that is included in the app).
 */
public class MainActivity extends WearableActivity implements UIAnimation.UIStateListener,
        SoundRecorder.OnVoicePlaybackStateChangedListener {

    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final long COUNT_DOWN_MS = TimeUnit.SECONDS.toMillis(10);
    private static final long MILLIS_IN_SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final String VOICE_FILE_NAME = "audiorecord.pcm";
    private MediaPlayer mMediaPlayer;
    private AppState mState = AppState.READY;
    private UIAnimation.UIState mUiState = UIAnimation.UIState.HOME;
    private SoundRecorder mSoundRecorder;

    private UIAnimation mUIAnimation;
    private ProgressBar mProgressBar;
    private CountDownTimer mCountDownTimer;

    enum AppState {
        READY, PLAYING_VOICE, PLAYING_MUSIC, RECORDING
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mProgressBar.setMax((int) (COUNT_DOWN_MS / MILLIS_IN_SECOND));
        setAmbientEnabled();
    }

    private void setProgressBar(long progressInMillis) {
        mProgressBar.setProgress((int) (progressInMillis / MILLIS_IN_SECOND));
    }

    @Override
    public void onUIStateChanged(UIAnimation.UIState state) {
        Log.d(TAG, "UI State is: " + state);
        if (mUiState == state) {
            return;
        }
        switch (state) {
            case MUSIC_UP:
                mState = AppState.PLAYING_MUSIC;
                mUiState = state;
                playMusic();
                break;
            case MIC_UP:
                mState = AppState.RECORDING;
                mUiState = state;
                mSoundRecorder.startRecording();
                setProgressBar(COUNT_DOWN_MS);
                mCountDownTimer = new CountDownTimer(COUNT_DOWN_MS, MILLIS_IN_SECOND) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        setProgressBar(millisUntilFinished);
                        Log.d(TAG, "Time Left: " + millisUntilFinished / MILLIS_IN_SECOND);
                    }

                    @Override
                    public void onFinish() {
                        mProgressBar.setProgress(0);
                        mProgressBar.setVisibility(View.INVISIBLE);
                        mSoundRecorder.stopRecording();
                        mUIAnimation.transitionToHome();
                        mUiState = UIAnimation.UIState.HOME;
                        mState = AppState.READY;
                        mCountDownTimer = null;
                    }
                };
                mCountDownTimer.start();
                break;
            case SOUND_UP:
                mState = AppState.PLAYING_VOICE;
                mUiState = state;
                mSoundRecorder.startPlay();
                break;
            case HOME:
                switch (mState) {
                    case PLAYING_MUSIC:
                        mState = AppState.READY;
                        mUiState = state;
                        stopMusic();
                        break;
                    case PLAYING_VOICE:
                        mState = AppState.READY;
                        mUiState = state;
                        mSoundRecorder.stopPlaying();
                        break;
                    case RECORDING:
                        mState = AppState.READY;
                        mUiState = state;
                        mSoundRecorder.stopRecording();
                        if (mCountDownTimer != null) {
                            mCountDownTimer.cancel();
                            mCountDownTimer = null;
                        }
                        mProgressBar.setVisibility(View.INVISIBLE);
                        setProgressBar(COUNT_DOWN_MS);
                        break;
                }
                break;
        }
    }

    /**
     * Plays back the MP3 file embedded in the application
     */
    private void playMusic() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer.create(this, R.raw.sound);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // we need to transition to the READY/Home state
                    Log.d(TAG, "Music Finished");
                    mUIAnimation.transitionToHome();
                }
            });
        }
        mMediaPlayer.start();
    }

    /**
     * Stops the playback of the MP3 file.
     */
    private void stopMusic() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     * Checks the permission that this app needs and if it has not been granted, it will
     * prompt the user to grant it, otherwise it shuts down the app.
     */
    private void checkPermissions() {
        boolean recordAudioPermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED;

        if (recordAudioPermissionGranted) {
            start();
        } else {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            String permissions[], int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                start();
            } else {
                // Permission has been denied before. At this point we should show a dialog to
                // user and explain why this permission is needed and direct him to go to the
                // Permissions settings for the app in the System settings. For this sample, we
                // simply exit to get to the important part.
                Toast.makeText(this, R.string.exiting_for_permissions, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Starts the main flow of the application.
     */
    private void start() {
        mSoundRecorder = new SoundRecorder(this, VOICE_FILE_NAME, this);
        int[] thumbResources = new int[] {R.id.mic, R.id.play, R.id.music};
        ImageView[] thumbs = new ImageView[3];
        for(int i=0; i < 3; i++) {
            thumbs[i] = (ImageView) findViewById(thumbResources[i]);
        }
        View containerView = findViewById(R.id.container);
        ImageView expandedView = (ImageView) findViewById(R.id.expanded);
        int animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mUIAnimation = new UIAnimation(containerView, thumbs, expandedView, animationDuration,
                this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (speakerIsSupported()) {
            checkPermissions();
        } else {
            findViewById(R.id.container2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, R.string.no_speaker_supported,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onStop() {
        if (mSoundRecorder != null) {
            mSoundRecorder.cleanup();
            mSoundRecorder = null;
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onStop();
    }

    @Override
    public void onPlaybackStopped() {
        mUIAnimation.transitionToHome();
        mUiState = UIAnimation.UIState.HOME;
        mState = AppState.READY;
    }

    /**
     * Determines if the wear device has a built-in speaker and if it is supported. Speaker, even if
     * physically present, is only supported in Android M+ on a wear device..
     */
    public final boolean speakerIsSupported() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager packageManager = getPackageManager();
            // The results from AudioManager.getDevices can't be trusted unless the device
            // advertises FEATURE_AUDIO_OUTPUT.
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
                return false;
            }
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }
}
