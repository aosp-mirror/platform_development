/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.commands.monkey;

import android.util.Log;

public abstract class Logger {
    private static final String TAG = "Monkey";

    public static Logger out = new Logger() {
        public void println(String s) {
            if (stdout) {
                System.out.println(s);
            }
            if (logcat) {
                Log.i(TAG, s);
            }
        }
    };
    public static Logger err = new Logger() {
        public void println(String s) {
            if (stdout) {
                System.err.println(s);
            }
            if (logcat) {
                Log.w(TAG, s);
            }
        }
    };

    public static boolean stdout = true;
    public static boolean logcat = true;

    public abstract void println(String s);

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying message.
     *
     * @param msg The message accompanying the exception.
     * @param t The exception (throwable) to log.
     */
    public static void error(String msg, Throwable t) {
        err.println(msg);
        err.println(Log.getStackTraceString(t));
    }
}
