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

import android.net.Uri;

/** Used to hold constants. */
public final class Constants {

    public static final String START_TIME = "timer_start_time";
    public static final String ORIGINAL_TIME = "timer_original_time";
    public static final String DATA_ITEM_PATH = "/timer";
    public static final Uri URI_PATTERN_DATA_ITEMS =
            Uri.fromParts("wear", DATA_ITEM_PATH, null);

    public static final int NOTIFICATION_TIMER_COUNTDOWN = 1;
    public static final int NOTIFICATION_TIMER_EXPIRED = 2;

    public static final String ACTION_SHOW_ALARM
            = "com.android.example.clockwork.timer.ACTION_SHOW";
    public static final String ACTION_DELETE_ALARM
            = "com.android.example.clockwork.timer.ACTION_DELETE";
    public static final String ACTION_RESTART_ALARM
            = "com.android.example.clockwork.timer.ACTION_RESTART";

    private Constants() {
    }

}
