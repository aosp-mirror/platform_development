/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.smssample;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.provider.Telephony;
import android.provider.Telephony.Sms.Intents;

public class Utils {

    /**
     * Check if the device runs Android 4.3 (JB MR2) or higher.
     */
    public static boolean hasJellyBeanMR2() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR2;
    }

    /**
     * Check if the device runs Android 4.4 (KitKat) or higher.
     */
    public static boolean hasKitKat() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.KITKAT;
    }

    /**
     * Check if your app is the default system SMS app.
     * @param context The Context
     * @return True if it is default, False otherwise. Pre-KitKat will always return True.
     */
    public static boolean isDefaultSmsApp(Context context) {
        if (hasKitKat()) {
            return context.getPackageName().equals(Telephony.Sms.getDefaultSmsPackage(context));
        }

        return true;
    }

    /**
     * Trigger the intent to open the system dialog that asks the user to change the default
     * SMS app.
     * @param context The Context
     */
    public static void setDefaultSmsApp(Context context) {
        // This is a new intent which only exists on KitKat
        if (hasKitKat()) {
            Intent intent = new Intent(Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Intents.EXTRA_PACKAGE_NAME, context.getPackageName());
            context.startActivity(intent);
        }
    }
}
