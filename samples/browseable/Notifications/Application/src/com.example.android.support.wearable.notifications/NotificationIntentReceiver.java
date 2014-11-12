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

package com.example.android.support.wearable.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.widget.Toast;

/**
 * Broadcast receiver to post toast messages in response to notification intents firing.
 */
public class NotificationIntentReceiver extends BroadcastReceiver {
    public static final String ACTION_EXAMPLE =
            "com.example.android.support.wearable.notifications.ACTION_EXAMPLE";
    public static final String ACTION_ENABLE_MESSAGES =
            "com.example.android.support.wearable.notifications.ACTION_ENABLE_MESSAGES";
    public static final String ACTION_DISABLE_MESSAGES =
            "com.example.android.support.wearable.notifications.ACTION_DISABLE_MESSAGES";

    private boolean mEnableMessages = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_EXAMPLE)) {
            if (mEnableMessages) {
                String message = intent.getStringExtra(NotificationUtil.EXTRA_MESSAGE);
                Bundle remoteInputResults = RemoteInput.getResultsFromIntent(intent);
                CharSequence replyMessage = null;
                if (remoteInputResults != null) {
                    replyMessage = remoteInputResults.getCharSequence(NotificationUtil.EXTRA_REPLY);
                }
                if (replyMessage != null) {
                    message = message + ": \"" + replyMessage + "\"";
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } else if (intent.getAction().equals(ACTION_ENABLE_MESSAGES)) {
            mEnableMessages = true;
        } else if (intent.getAction().equals(ACTION_DISABLE_MESSAGES)) {
            mEnableMessages = false;
        }
    }
}
