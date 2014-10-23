/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.speedtracker.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.example.android.wearable.speedtracker.R;

/**
 * A simple activity that allows the user to start or stop recording of GPS location data.
 */
public class LocationSettingActivity extends Activity {

    private static final String PREFS_KEY_SAVE_GPS = "save-gps";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.saving_activity);
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(getGpsRecordingStatusFromPreferences(this) ? R.string.stop_saving_gps
                : R.string.start_saving_gps);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.submitBtn:
                saveGpsRecordingStatusToPreferences(LocationSettingActivity.this,
                        !getGpsRecordingStatusFromPreferences(this));
                break;
            case R.id.cancelBtn:
                break;
        }
        finish();
    }

    /**
     * Get the persisted value for whether the app should record the GPS location data or not. If
     * there is no prior value persisted, it returns {@code false}.
     */
    public static boolean getGpsRecordingStatusFromPreferences(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(PREFS_KEY_SAVE_GPS, false);
    }

    /**
     * Persists the user selection to whether save the GPS location data or not.
     */
    public static void saveGpsRecordingStatusToPreferences(Context context, boolean value) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean(PREFS_KEY_SAVE_GPS, value).apply();

    }
}
