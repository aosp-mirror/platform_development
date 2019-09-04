/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.dumpviewer.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.dumpviewer.DumpActivity;

import java.util.regex.Pattern;

public class Utils {
    public static final String TAG = DumpActivity.TAG;

    public static Handler sMainHandler = new Handler(Looper.getMainLooper());

    private Utils() {
    }

    private static final Pattern sSafeStringPattern = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.]$");

    public static String shellEscape(String s) {
        if (sSafeStringPattern.matcher(s).matches()) {
            return s;
        }

        return "'" + s.replace("'", "'\\''") + "'";
    }

    /** Parse a value as a base-10 integer. */
    public static int parseInt(String value, int defValue) {
        return parseIntWithBase(value, 10, defValue);
    }

    /** Parse a value as an integer of a given base. */
    public static int parseIntWithBase(String value, int base, int defValue) {
        if (value == null) {
            return defValue;
        }
        try {
            return Integer.parseInt(value, base);
        } catch (NumberFormatException e) {
            return defValue;
        }
    }

    public static void toast(Context context, String message) {
        sMainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT));
    }
}
