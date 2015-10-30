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

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Action;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;

public class FindPhoneActivity extends Activity {

    private static final int FIND_PHONE_NOTIFICATION_ID = 2;
    private static Notification.Builder notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a notification with an action to toggle an alarm on the phone.
        Intent toggleAlarmOperation = new Intent(this, FindPhoneService.class);
        toggleAlarmOperation.setAction(FindPhoneService.ACTION_TOGGLE_ALARM);
        PendingIntent toggleAlarmIntent = PendingIntent.getService(this, 0, toggleAlarmOperation,
                PendingIntent.FLAG_CANCEL_CURRENT);
        Action alarmAction = new Action(R.drawable.alarm_action_icon, "", toggleAlarmIntent);
        // This intent turns off the alarm if the user dismisses the card from the wearable.
        Intent cancelAlarmOperation = new Intent(this, FindPhoneService.class);
        cancelAlarmOperation.setAction(FindPhoneService.ACTION_CANCEL_ALARM);
        PendingIntent cancelAlarmIntent = PendingIntent.getService(this, 0, cancelAlarmOperation,
                PendingIntent.FLAG_CANCEL_CURRENT);
        // Use a spannable string for the notification title to resize it.
        SpannableString title = new SpannableString(getString(R.string.app_name));
        title.setSpan(new RelativeSizeSpan(0.85f), 0, title.length(), Spannable.SPAN_POINT_MARK);
        notification = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(getString(R.string.turn_alarm_on))
                .setSmallIcon(R.drawable.ic_launcher)
                .setVibrate(new long[] {0, 50})  // Vibrate to bring card to top of stream.
                .setDeleteIntent(cancelAlarmIntent)
                .extend(new Notification.WearableExtender()
                        .addAction(alarmAction)
                        .setContentAction(0)
                        .setHintHideIcon(true))
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(FIND_PHONE_NOTIFICATION_ID, notification.build());

        finish();
    }

    /**
     * Updates the text on the wearable notification. This is used so the notification reflects the
     * current state of the alarm on the phone. For instance, if the alarm is turned on, the
     * notification text indicates that the user can tap it to turn it off, and vice-versa.
     *
     * @param context
     * @param notificationText The new text to display on the wearable notification.
     */
    public static void updateNotification(Context context, String notificationText) {
        notification.setContentText(notificationText);
        ((NotificationManager) context.getSystemService(NOTIFICATION_SERVICE))
                .notify(FIND_PHONE_NOTIFICATION_ID, notification.build());
    }

}
