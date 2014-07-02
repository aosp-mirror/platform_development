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

import android.os.SystemClock;

/** This class represents a timer. */
public class TimerObj {

    // Start time in milliseconds.
    public long startTime;

    // Length of the timer in milliseconds.
    public long originalLength;

    /**
     * Construct a timer with a specific start time and length.
     *
     * @param startTime the start time of the timer.
     * @param timerLength the length of the timer.
     */
    public TimerObj(long startTime, long timerLength) {
        this.startTime = startTime;
        this.originalLength = timerLength;
    }

    /**
     * Calculate the time left of this timer.
     * @return the time left for this timer.
     */
    public long timeLeft() {
        long millis = SystemClock.elapsedRealtime();
        return originalLength - (millis - startTime);
    }
}
