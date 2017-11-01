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

import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Objects;

/**
 * Class represents a single alarm.
 */
public class Alarm implements Comparable<Alarm> {

    public int id;

    public int month;

    public int date;

    /** Integer as a 24-hour format */
    public int hour;

    public int minute;

    public Alarm(int id, int month, int date, int hour, int minute) {
        this.id = id;
        this.month = month;
        this.date = date;
        this.hour = hour;
        this.minute = minute;
    }

    public Alarm() {
    }

    /**
     * Serialize the instance as a JSON String.
     *
     * @return serialized JSON String.
     */
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("month", month);
            jsonObject.put("date", date);
            jsonObject.put("hour", hour);
            jsonObject.put("minute", minute);
        } catch (JSONException e) {
            throw new IllegalStateException("Failed to convert the object to JSON");
        }
        return jsonObject.toString();
    }

    /**
     * Parses a Json string to an {@link Alarm} instance.
     *
     * @param string The String representation of an alarm
     * @return an instance of {@link Alarm}
     */
    public static Alarm fromJson(String string) {
        JSONObject jsonObject;
        Alarm alarm = new Alarm();
        try {
            jsonObject = new JSONObject(string);
            alarm.id = jsonObject.getInt("id");
            alarm.month = jsonObject.getInt("month");
            alarm.date = jsonObject.getInt("date");
            alarm.hour = jsonObject.getInt("hour");
            alarm.minute = jsonObject.getInt("minute");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to parse the String: " + string);
        }

        return alarm;
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "id=" + id +
                ", month=" + month +
                ", date=" + date +
                ", hour=" + hour +
                ", minute=" + minute +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Alarm)) {
            return false;
        }
        Alarm alarm = (Alarm) o;
        return id == alarm.id &&
                month == alarm.month &&
                date == alarm.date &&
                hour == alarm.hour &&
                minute == alarm.minute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, month, date, hour, minute);
    }

    @Override
    public int compareTo(@NonNull Alarm other) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, date);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        Calendar otherCal = Calendar.getInstance();
        otherCal.set(Calendar.MONTH, other.month);
        otherCal.set(Calendar.DATE, other.date);
        otherCal.set(Calendar.HOUR_OF_DAY, other.hour);
        otherCal.set(Calendar.MINUTE, other.minute);
        return calendar.compareTo(otherCal);
    }
}
