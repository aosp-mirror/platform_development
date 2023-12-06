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

package com.example.android.vdmdemo.host;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/** NotificationListenerService to forward notifications shown on the host to the client. */
public final class NotificationListener extends NotificationListenerService {

    private static final String TAG = "VdmHost";

    @Override
    public void onNotificationRemoved(
            StatusBarNotification notification,
            NotificationListenerService.RankingMap rankingMap,
            int reason) {

        if (reason == NotificationListenerService.REASON_LOCKDOWN) {
            Intent lockdownIntent = new Intent(this, VdmService.class);
            lockdownIntent.setAction(VdmService.ACTION_LOCKDOWN);
            PendingIntent pendingIntentLockdown =
                    PendingIntent.getService(this, 0, lockdownIntent, PendingIntent.FLAG_IMMUTABLE);

            try {
                Log.i(
                        TAG,
                        "Notification removed due to lockdown. Sending lockdown "
                        + "Intent to VdmService");
                pendingIntentLockdown.send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, "Error sending lockdown Intent", e);
            }
        }
    }
}
