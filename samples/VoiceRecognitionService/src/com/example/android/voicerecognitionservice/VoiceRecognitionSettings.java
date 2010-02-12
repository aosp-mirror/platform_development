/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.voicerecognitionservice;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * A settings activity for the sample voice recognizer.
 */
public class VoiceRecognitionSettings extends PreferenceActivity {
    
    // The name of the SharedPreferences file we'll store preferences in.
    public static final String SHARED_PREFERENCES_NAME = "VoiceRecognitionService";
    
    // The key to the preference for the type of results to show (letters or numbers).
    // Identical to the value specified in res/values/strings.xml.
    public static final String PREF_KEY_RESULTS_TYPE = "results_type";
    
    // The values of the preferences for the type of results to show (letters or numbers).
    // Identical to the values specified in res/values/strings.xml.
    public static final int RESULT_TYPE_LETTERS = 0;
    public static final int RESULT_TYPE_NUMBERS = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFERENCES_NAME);
        addPreferencesFromResource(R.xml.preferences);
    }
}
