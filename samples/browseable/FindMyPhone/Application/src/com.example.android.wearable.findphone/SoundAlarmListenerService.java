/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.findphone;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.IOException;

/**
 * Listens for disconnection from home device.
 */
public class SoundAlarmListenerService extends WearableListenerService {

    private static final String TAG = "ExampleFindPhoneApp";

    private static final String FIELD_ALARM_ON = "alarm_on";

    private AudioManager mAudioManager;
    private static int mOrigVolume;
    private int mMaxVolume;
    private Uri mAlarmSound;
    private MediaPlayer mMediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mOrigVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        mAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        mMediaPlayer = new MediaPlayer();
    }

    @Override
    public void onDestroy() {
        // Reset the alarm volume to the user's original setting.
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mOrigVolume, 0);
        mMediaPlayer.release();
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents + " for " + getPackageName());
        }
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.i(TAG, event + " deleted");
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Boolean alarmOn =
                        DataMap.fromByteArray(event.getDataItem().getData()).get(FIELD_ALARM_ON);
                if (alarmOn) {
                    mOrigVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_ALARM);
                    mMediaPlayer.reset();
                    // Sound alarm at max volume.
                    mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mMaxVolume, 0);
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    try {
                        mMediaPlayer.setDataSource(getApplicationContext(), mAlarmSound);
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to prepare media player to play alarm.", e);
                    }
                    mMediaPlayer.start();
                } else {
                    // Reset the alarm volume to the user's original setting.
                    mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, mOrigVolume, 0);
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                    }
                }
            }
        }
    }

}
