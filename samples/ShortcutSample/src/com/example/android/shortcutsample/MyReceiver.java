/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.shortcutsample;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = Main.TAG;

    private static final String ACTION_PIN_REQUEST_ACCEPTED =
            "MyReceiver.ACTION_PIN_REQUEST_ACCEPTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: " + intent);
        if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
            // Refresh all shortcut to update the labels.
            // (Right now shortcut labels don't contain localized strings though.)
            new ShortcutHelper(context).refreshShortcuts(/*force=*/ true);
        } else if (ACTION_PIN_REQUEST_ACCEPTED.equals(intent.getAction())) {
            Utils.showToast(context, "Pin request accepted.");
            Main.refreshAllInstances();
        }
    }

    public static PendingIntent getPinRequestAcceptedIntent(Context context) {
        final Intent intent = new Intent(ACTION_PIN_REQUEST_ACCEPTED);
        intent.setComponent(new ComponentName(context, MyReceiver.class));

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
