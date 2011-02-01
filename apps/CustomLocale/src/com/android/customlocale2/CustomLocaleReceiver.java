/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.customlocale2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

//-----------------------------------------------

/**
 * Broadcast receiver that can change the system's locale.
 * <p/>
 * This allows an external script such as an automated testing framework
 * to easily trigger a locale change on an emulator as such:
 * <pre>
 * $ adb shell am broadcast -a com.android.intent.action.SET_LOCALE \
 *                          --es com.android.intent.extra.LOCALE en_US
 * </pre>
 */
public class CustomLocaleReceiver extends BroadcastReceiver {

    private static final String TAG = CustomLocaleReceiver.class.getSimpleName();
    private static final boolean DEBUG = true;

    /** Intent action that triggers this receiver. */
    public static final String ACTION_SET_LOCALE = "com.android.intent.action.SET_LOCALE";
    /** An extra String that specifies the locale to set, in the form "en_US". */
    public static final String EXTRA_LOCALE = "com.android.intent.extra.LOCALE";

    @Override
    public void onReceive(Context context, Intent intent) {
        setResult(Activity.RESULT_CANCELED, null, null);
        if (intent == null || ! ACTION_SET_LOCALE.equals(intent.getAction())) {
            if (DEBUG) {
                Log.d(TAG, "Invalid intent: " + (intent == null ? "null" : intent.toString()));
            }
            return;
        }

        String locale = intent.getStringExtra(EXTRA_LOCALE);

        // Enforce the locale string is either in the form "ab" or "ab_cd"
        boolean is_ok = locale != null;
        is_ok = is_ok && (locale.length() == 2 || locale.length() == 5);
        if (is_ok && locale.length() >= 2) {
            is_ok = Character.isLetter(locale.charAt(0)) &&
                    Character.isLetter(locale.charAt(1));
        }
        if (is_ok && locale.length() == 5) {
            is_ok = locale.charAt(2) == '_' &&
                    Character.isLetter(locale.charAt(3)) &&
                    Character.isLetter(locale.charAt(4));
        }

        if (!is_ok && DEBUG) {
            Log.e(TAG, "Invalid locale: expected ab_CD but got " + locale);
        } else if (is_ok) {
            ChangeLocale.changeSystemLocale(locale);
            setResult(Activity.RESULT_OK, locale, null);
        }
    }

}


