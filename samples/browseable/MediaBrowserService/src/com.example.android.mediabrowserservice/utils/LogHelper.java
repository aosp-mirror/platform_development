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
package com.example.android.mediabrowserservice.utils;

import android.util.Log;

public class LogHelper {
    public static void v(String tag, Object... messages) {
        log(tag, Log.VERBOSE, null, messages);
    }

    public static void d(String tag, Object... messages) {
        log(tag, Log.DEBUG, null, messages);
    }

    public static void i(String tag, Object... messages) {
        log(tag, Log.INFO, null, messages);
    }

    public static void w(String tag, Object... messages) {
        log(tag, Log.WARN, null, messages);
    }

    public static void w(String tag, Throwable t, Object... messages) {
        log(tag, Log.WARN, t, messages);
    }

    public static void e(String tag, Object... messages) {
        log(tag, Log.ERROR, null, messages);
    }

    public static void e(String tag, Throwable t, Object... messages) {
        log(tag, Log.ERROR, t, messages);
    }

    public static void log(String tag, int level, Throwable t, Object... messages) {
        if (messages != null && Log.isLoggable(tag, level)) {
            String message;
            if (messages.length == 1) {
                message = messages[0] == null ? null : messages[0].toString();
            } else {
                StringBuilder sb = new StringBuilder();
                for (Object m: messages) {
                    sb.append(m);
                }
                if (t != null) {
                    sb.append("\n").append(Log.getStackTraceString(t));
                }
                message = sb.toString();
            }
            Log.println(level, tag, message);
        }
    }
}
