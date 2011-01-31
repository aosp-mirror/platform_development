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


import java.util.Locale;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.res.Configuration;
import android.os.RemoteException;
import android.util.Log;

/**
 * Helper class to change the system locale.
 */
public final class ChangeLocale {

    private static final String TAG = ChangeLocale.class.getSimpleName();
    private static final boolean DEBUG = true;

    /**
     * Sets the system locale to the new one specified.
     *
     * @param locale A locale name in the form "ab_AB". Must not be null or empty.
     * @return True if the locale was succesfully changed.
     */
    public static boolean changeSystemLocale(String locale) {
        if (DEBUG) {
            Log.d(TAG, "Change locale to: " + locale);
        }

        try {
            IActivityManager am = ActivityManagerNative.getDefault();
            Configuration config = am.getConfiguration();

            Locale loc = null;

            String[] langCountry = locale.split("_");
            if (langCountry.length == 2) {
                loc = new Locale(langCountry[0], langCountry[1]);
            } else {
                loc = new Locale(locale);
            }

            config.locale = loc;

            // indicate this isn't some passing default - the user wants this
            // remembered
            config.userSetLocale = true;

            am.updateConfiguration(config);

            return true;

        } catch (RemoteException e) {
            if (DEBUG) {
                Log.e(TAG, "Change locale failed", e);
            }
        }

        return false;
    }
}
