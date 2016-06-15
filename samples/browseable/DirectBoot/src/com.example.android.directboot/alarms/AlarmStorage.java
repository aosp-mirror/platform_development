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

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.os.BuildCompat;
import android.util.Log;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class responsible for saving/retrieving alarms. This class uses SharedPreferences as storage.
 */
public class AlarmStorage {

    private static final String TAG = AlarmStorage.class.getSimpleName();
    private static final String ALARM_PREFERENCES_NAME = "alarm_preferences";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SharedPreferences mSharedPreferences;

    public AlarmStorage(Context context) {
        Context storageContext;
        if (BuildCompat.isAtLeastN()) {
            // All N devices have split storage areas, but we may need to
            // move the existing preferences to the new device protected
            // storage area, which is where the data lives from now on.
            final Context deviceContext = context.createDeviceProtectedStorageContext();
            if (!deviceContext.moveSharedPreferencesFrom(context,
                    ALARM_PREFERENCES_NAME)) {
                Log.w(TAG, "Failed to migrate shared preferences.");
            }
            storageContext = deviceContext;
        } else {
            storageContext = context;
        }
        mSharedPreferences = storageContext
                .getSharedPreferences(ALARM_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Stores an alarm in the SharedPreferences.
     *
     * @param month the integer represents a month
     * @param date the integer represents a date
     * @param hour the integer as 24-hour format the alarm goes off
     * @param minute the integer of the minute the alarm goes off
     * @return the saved {@link Alarm} instance
     */
    public Alarm saveAlarm(int month, int date, int hour, int minute) {
        Alarm alarm = new Alarm();
        // Ignore the Id duplication if that happens
        alarm.id = SECURE_RANDOM.nextInt();
        alarm.month = month;
        alarm.date = date;
        alarm.hour = hour;
        alarm.minute = minute;
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(String.valueOf(alarm.id), alarm.toJson());
        editor.apply();
        return alarm;
    }

    /**
     * Retrieves the alarms stored in the SharedPreferences.
     * This method takes linear time as the alarms count.
     *
     * @return a {@link Set} of alarms.
     */
    public Set<Alarm> getAlarms() {
        Set<Alarm> alarms = new HashSet<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            alarms.add(Alarm.fromJson(entry.getValue().toString()));
        }
        return alarms;
    }

    /**
     * Delete the alarm instance passed as an argument from the SharedPreferences.
     * This method iterates through the alarms stored in the SharedPreferences, takes linear time
     * as the alarms count.
     *
     * @param toBeDeleted the alarm instance to be deleted
     */
    public void deleteAlarm(Alarm toBeDeleted) {
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            Alarm alarm = Alarm.fromJson(entry.getValue().toString());
            if (alarm.id == toBeDeleted.id) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.remove(String.valueOf(alarm.id));
                editor.apply();
                return;
            }
        }
    }
}
