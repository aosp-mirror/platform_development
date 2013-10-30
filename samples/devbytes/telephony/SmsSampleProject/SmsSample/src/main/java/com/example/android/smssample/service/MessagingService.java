/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.smssample.service;

import android.app.IntentService;
import android.content.Intent;

import com.example.android.smssample.receiver.MessagingReceiver;

/**
 * This service is triggered internally only and is used to process incoming SMS and MMS messages
 * that the {@link com.example.android.smssample.receiver.MessagingReceiver} passes over. It's
 * preferable to handle these in a service in case there is significant work to do which may exceed
 * the time allowed in a receiver.
 */
public class MessagingService extends IntentService {
    private static final String TAG = "MessagingService";

    // These actions are for this app only and are used by MessagingReceiver to start this service
    public static final String ACTION_MY_RECEIVE_SMS = "com.example.android.smssample.RECEIVE_SMS";
    public static final String ACTION_MY_RECEIVE_MMS = "com.example.android.smssample.RECEIVE_MMS";

    public MessagingService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String intentAction = intent.getAction();
            if (ACTION_MY_RECEIVE_SMS.equals(intentAction)) {
                // TODO: Handle incoming SMS

                // Ensure wakelock is released that was created by the WakefulBroadcastReceiver
                MessagingReceiver.completeWakefulIntent(intent);
            } else if (ACTION_MY_RECEIVE_MMS.equals(intentAction)) {
                // TODO: Handle incoming MMS

                // Ensure wakelock is released that was created by the WakefulBroadcastReceiver
                MessagingReceiver.completeWakefulIntent(intent);
            }
        }
    }
}
