/*
 * Copyright 2010, The Android Open Source Project
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

package com.android.settingscmd;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;
import android.provider.Settings;

/**
 * Utility for modifying system settings.
 * <p/>
 * Usage:
 * <p/>
 * adb shell am instrument -w [-e secure true/false] -e name <setting name>
 * -e value <setting value> -w com.android.settingscmd/.SettingsInstrument
 * <p/>
 * This apk must be signed with the platform certificate to modify secure settings.
 */
public class SettingsInstrument extends Instrumentation {

    private boolean mSecure;
    private String mName;
    private String mValue;

    public SettingsInstrument() {
        super();
    }

    @Override
    public void onCreate(Bundle arguments) {
        mSecure = getBooleanArgument(arguments, "secure");
        mName = arguments.getString("name");
        mValue = arguments.getString("value");
        start();
    }

    private boolean getBooleanArgument(Bundle arguments, String tag) {
        String tagString = arguments.getString(tag);
        return tagString != null && Boolean.parseBoolean(tagString);
    }

    @Override
    public void onStart() {
        if (mName == null || mValue == null) {
            reportError("Missing arguments. Usage:\n [-e secure true] "
                    + "-e name <setting name> -e value <setting value>");
            return;
        }
        boolean status = false;
        if (mSecure) {
            status = Settings.Secure.putString(getTargetContext().getContentResolver(), mName,
                    mValue);
        } else {
            status = Settings.System.putString(getTargetContext().getContentResolver(), mName,
                    mValue);
        }
        Bundle bundleResponse = new Bundle();
        bundleResponse.putBoolean("result", status);
        finish(Activity.RESULT_OK, bundleResponse);
    }

    private void reportError(String msg) {
        Bundle bundleResponse = new Bundle();
        bundleResponse.putString("error", msg);
        finish(Activity.RESULT_CANCELED, bundleResponse);
    }
}
