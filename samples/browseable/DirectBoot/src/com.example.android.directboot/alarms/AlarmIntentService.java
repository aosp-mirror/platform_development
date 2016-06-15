/*
* Copyright 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.directboot.alarms;

import com.example.android.directboot.R;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

/**
 * IntentService to set off an alarm.
 */
public class AlarmIntentService extends IntentService {

    public static final String ALARM_WENT_OFF_ACTION = AlarmIntentService.class.getName()
            + ".ALARM_WENT_OFF";
    public static final String ALARM_KEY = "alarm_instance";

    public AlarmIntentService() {
        super(AlarmIntentService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        Alarm alarm = intent.getParcelableExtra(ALARM_KEY);

        NotificationManager notificationManager = context
                .getSystemService(NotificationManager.class);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_fbe_notification)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setSound(Settings.System.DEFAULT_ALARM_ALERT_URI)
                        .setContentTitle(context.getString(R.string.alarm_went_off, alarm.hour,
                                alarm.minute));
        notificationManager.notify(alarm.id, builder.build());

        AlarmStorage alarmStorage = new AlarmStorage(context);
        alarmStorage.deleteAlarm(alarm);
        Intent wentOffIntent = new Intent(ALARM_WENT_OFF_ACTION);
        wentOffIntent.putExtra(ALARM_KEY, alarm);
        LocalBroadcastManager.getInstance(context).sendBroadcast(wentOffIntent);
    }
}
