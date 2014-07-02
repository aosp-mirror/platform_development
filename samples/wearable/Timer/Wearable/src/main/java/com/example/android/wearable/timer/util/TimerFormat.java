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

package com.example.android.wearable.timer.util;

/** Helper class to format the timer. Based on com.android.deskclock.timer.CountingTimerView. */
public final class TimerFormat {

    private static final String TWO_DIGITS = "%02d";

    private static final String ONE_DIGIT = "%01d";

    private static final String NEG_TWO_DIGITS = "-%02d";

    private static final String NEG_ONE_DIGIT = "-%01d";

    private static String mHours;

    private static String mMinutes;

    private static String mSeconds;

    private TimerFormat() {

    }

    /**
     * Update the time to display. Separates that time into the hours, minutes, seconds.
     * Copied and shortened from com.android.deskclock.timer.CountingTimerView.
     *
     * @param time new time to display - in milliseconds
     */
    private static void setTime(long time) {
        boolean neg = false;
        boolean showNeg = false;
        String format;
        if (time < 0) {
            time = -time;
            neg = showNeg = true;
        }
        long seconds = time / 1000;
        long hundreds = (time - seconds * 1000) / 10;
        long minutes = seconds / 60;
        seconds = seconds - minutes * 60;
        long hours = minutes / 60;
        minutes = minutes - hours * 60;
        if (hours > 999) {
            hours = 0;
        }
        // The time  can be between 0 and -1 seconds, but the "truncated" equivalent time of hours
        // and minutes and seconds could be zero, so since we do not show fractions of seconds
        // when counting down, do not show the minus sign.
        if (hours == 0 && minutes == 0 && seconds == 0) {
            showNeg = false;
        }

        // Normalize and check if it is 'time' to invalidate
        if (!neg && hundreds != 0) {
            seconds++;
            if (seconds == 60) {
                seconds = 0;
                minutes++;
                if (minutes == 60) {
                    minutes = 0;
                    hours++;
                }
            }
        }

        // Hours may be empty
        if (hours >= 10) {
            format = showNeg ? NEG_TWO_DIGITS : TWO_DIGITS;
            mHours = String.format(format, hours);
        } else if (hours > 0) {
            format = showNeg ? NEG_ONE_DIGIT : ONE_DIGIT;
            mHours = String.format(format, hours);
        } else {
            mHours = null;
        }

        // Minutes are never empty and when hours are non-empty, must be two digits
        if (minutes >= 10 || hours > 0) {
            format = (showNeg && hours == 0) ? NEG_TWO_DIGITS : TWO_DIGITS;
            mMinutes = String.format(format, minutes);
        } else {
            format = (showNeg && hours == 0) ? NEG_ONE_DIGIT : ONE_DIGIT;
            mMinutes = String.format(format, minutes);
        }

        // Seconds are always two digits
        mSeconds = String.format(TWO_DIGITS, seconds);
    }

    /**
     * Based on com.android.deskclock.timer.CountingTimerView.
     *
     * @param time the time to format.
     * @return nicely formatted time.
     */
    public static String getTimeString(long time) {
        setTime(time);
        if (mHours == null) {
            return String.format("%s:%s", mMinutes, mSeconds);
        }
        return String.format("%s:%s:%s", mHours, mMinutes, mSeconds);

    }
}
