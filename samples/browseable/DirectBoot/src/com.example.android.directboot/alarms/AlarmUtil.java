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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.Calendar;

/**
 * Utility class for alarms.
 */
public class AlarmUtil {

    private static final String TAG = "AlarmUtil";
    private final Context mContext;
    private final AlarmManager mAlarmManager;

    public AlarmUtil(Context context) {
        mContext = context;
        mAlarmManager = mContext.getSystemService(AlarmManager.class);
    }

    /**
     * Schedules an alarm using {@link AlarmManager}.
     *
     * @param alarm the alarm to be scheduled
     */
    public void scheduleAlarm(Alarm alarm) {
        Intent intent = new Intent(mContext, AlarmIntentService.class);
        Bundle extras = writeAlarm(alarm);
        intent.putExtras(extras);

        PendingIntent pendingIntent = PendingIntent
                .getService(mContext, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.MONTH, alarm.month);
        alarmTime.set(Calendar.DATE, alarm.date);
        alarmTime.set(Calendar.HOUR_OF_DAY, alarm.hour);
        alarmTime.set(Calendar.MINUTE, alarm.minute);

        AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                alarmTime.getTimeInMillis(),
                pendingIntent);
        mAlarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        Log.i(TAG,
                String.format("Alarm scheduled at (%2d:%02d) Date: %d, Month: %d",
                        alarm.hour, alarm.minute,
                        alarm.month, alarm.date));
    }

    /**
     * Cancels the scheduled alarm.
     *
     * @param alarm the alarm to be canceled.
     */
    public void cancelAlarm(Alarm alarm) {
        Intent intent = new Intent(mContext, AlarmIntentService.class);
        PendingIntent pendingIntent = PendingIntent
                .getService(mContext, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mAlarmManager.cancel(pendingIntent);
    }

    /**
     * Returns a next alarm time (nearest day) Calendar instance with the hour and the minute.
     *
     * @param hour the integer of the hour an alarm should go off
     * @param minute the integer of the minute an alarm should go off
     * @return a {@link Calendar} instance an alarm should go off given the passed hour and the
     *         minute
     */
    public Calendar getNextAlarmTime(int hour, int minute) {
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        if ((alarmTime.getTimeInMillis() - System.currentTimeMillis()) < 0) {
            alarmTime.add(Calendar.DATE, 1);
        }
        return alarmTime;
    }

    public static Alarm readAlarm(Bundle extras) {
        int id = extras.getInt(AlarmIntentService.KEY_ALARM_ID);
        int month = extras.getInt(AlarmIntentService.KEY_ALARM_MONTH);
        int date = extras.getInt(AlarmIntentService.KEY_ALARM_DATE);
        int hour = extras.getInt(AlarmIntentService.KEY_ALARM_HOUR);
        int minute = extras.getInt(AlarmIntentService.KEY_ALARM_MINUTE);

        return new Alarm(id, month, date, hour, minute);
    }

    public static Bundle writeAlarm(Alarm alarm){
        Bundle extras = new Bundle();
        extras.putInt(AlarmIntentService.KEY_ALARM_ID, alarm.id);
        extras.putInt(AlarmIntentService.KEY_ALARM_MONTH, alarm.month);
        extras.putInt(AlarmIntentService.KEY_ALARM_DATE, alarm.date);
        extras.putInt(AlarmIntentService.KEY_ALARM_HOUR, alarm.hour);
        extras.putInt(AlarmIntentService.KEY_ALARM_MINUTE, alarm.minute);

        return extras;
    }
}
