/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.voicemail.common.logging;

import android.util.Log;

/**
 * Simplifies usage of Android logging class {@link Log} by abstracting the TAG field that is
 * required to be passed to every logging method. Also, allows automatic insertion of the owner
 * class name prefix to log outputs for better debugging.
 * <p>
 * Use {@link #getLogger(Class)} to create an instance of Logger that automatically inserts the
 * class name as a prefix to each log output. If you do not want the class name to be prefixed to
 * log output then use {@link #getLogger()} to create the instance of Logger.
 */
public class Logger {
    private static final String APP_TAG = "VoicemailSample";

    /**
     * Use this method if you want your class name to be prefixed to each log output.
     */
    public static Logger getLogger(Class<?> classZ) {
        return new Logger(classZ.getSimpleName() + ": ");
    }

    /**
     * Use this factory method if you DO NOT want your class name to be prefixed into the log
     * output.
     */
    public static Logger getLogger() {
        return new Logger();
    }

    private final String mLogPrefix;

    /** No custom log prefix used. */
    private Logger() {
        mLogPrefix = null;
    }

    /** Use the supplied custom prefix in log output. */
    private Logger(String logPrefix) {
        mLogPrefix = logPrefix;
    }

    private String getMsg(String msg) {
        if (mLogPrefix != null) {
            return mLogPrefix + msg;
        } else {
            return msg;
        }
    }

    public void i(String msg) {
        Log.i(APP_TAG, getMsg(msg));
    }

    public void i(String msg, Throwable t) {
        Log.i(APP_TAG, getMsg(msg), t);
    }

    public void d(String msg) {
        Log.d(APP_TAG, getMsg(msg));
    }

    public void d(String msg, Throwable t) {
        Log.d(APP_TAG, getMsg(msg), t);
    }

    public void w(String msg) {
        Log.w(APP_TAG, getMsg(msg));
    }

    public void w(String msg, Throwable t) {
        Log.w(APP_TAG, getMsg(msg), t);
    }

    public void e(String msg) {
        Log.e(APP_TAG, getMsg(msg));
    }

    public void e(String msg, Throwable t) {
        Log.e(APP_TAG, getMsg(msg), t);
    }
}
