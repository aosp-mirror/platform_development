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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Creates a sound on the paired phone to find it.
 */
public class FindPhoneService extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ExampleFindPhoneApp";

    private static final String FIELD_ALARM_ON = "alarm_on";
    private static final String PATH_SOUND_ALARM = "/sound_alarm";
    public static final String ACTION_TOGGLE_ALARM = "action_toggle_alarm";
    public static final String ACTION_CANCEL_ALARM = "action_alarm_off";

    // Timeout for making a connection to GoogleApiClient (in milliseconds).
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private GoogleApiClient mGoogleApiClient;

    public FindPhoneService() {
        super(FindPhoneService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "FindPhoneService.onHandleIntent");
        }
        if (mGoogleApiClient.isConnected()) {
            // Set the alarm off by default.
            boolean alarmOn = false;
            if (intent.getAction().equals(ACTION_TOGGLE_ALARM)) {
                // Get current state of the alarm.
                DataItemBuffer result = Wearable.DataApi.getDataItems(mGoogleApiClient).await();
                try {
                    if (result.getStatus().isSuccess()) {
                        if (result.getCount() == 1) {
                            alarmOn = DataMap.fromByteArray(result.get(0).getData())
                                    .getBoolean(FIELD_ALARM_ON, false);
                        } else {
                            Log.e(TAG, "Unexpected number of DataItems found.\n"
                                    + "\tExpected: 1\n"
                                    + "\tActual: " + result.getCount());
                        }
                    } else if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onHandleIntent: failed to get current alarm state");
                    }
                } finally {
                    result.release();
                }
                // Toggle alarm.
                alarmOn = !alarmOn;
                // Change notification text based on new value of alarmOn.
                String notificationText = alarmOn ? getString(R.string.turn_alarm_off)
                        : getString(R.string.turn_alarm_on);
                FindPhoneActivity.updateNotification(this, notificationText);
            }
            // Use alarmOn boolean to update the DataItem - phone will respond accordingly
            // when it receives the change.
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_SOUND_ALARM);
            putDataMapRequest.getDataMap().putBoolean(FIELD_ALARM_ON, alarmOn);
            putDataMapRequest.setUrgent();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                    .await();
        } else {
            Log.e(TAG, "Failed to toggle alarm on phone - Client disconnected from Google Play "
                    + "Services");
        }
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

}
