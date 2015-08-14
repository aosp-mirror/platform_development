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

package com.example.android.messagingservice;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple logger that uses shared preferences to log messages, their reads
 * and replies. Don't use this in a real world application. This logger is only
 * used for displaying the messages in the text view.
 */
class MessageLogger {

    private static final String PREF_MESSAGE = "MESSAGE_LOGGER";
    private static final DateFormat DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();
    private static final String LINE_BREAKS = "\n\n";

    public static final String LOG_KEY = "message_data";

    public static void logMessage(Context context, String message) {
        SharedPreferences prefs = getPrefs(context);
        message = DATE_FORMAT.format(new Date(System.currentTimeMillis())) + ": " + message;
        prefs.edit()
                .putString(LOG_KEY, prefs.getString(LOG_KEY, "") + LINE_BREAKS + message)
                .apply();
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_MESSAGE, Context.MODE_PRIVATE);
    }

    public static String getAllMessages(Context context) {
        return getPrefs(context).getString(LOG_KEY, "");
    }

    public static void clear(Context context) {
        getPrefs(context).edit().remove(LOG_KEY).apply();
    }
}
