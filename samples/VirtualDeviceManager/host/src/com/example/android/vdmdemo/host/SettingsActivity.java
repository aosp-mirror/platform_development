/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.host;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** VDM Host Settings activity. */
@AndroidEntryPoint(AppCompatActivity.class)
public class SettingsActivity extends Hilt_SettingsActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);
        Toolbar toolbar = requireViewById(R.id.main_tool_bar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        setTitle(getTitle() + " " + getString(R.string.settings));
    }

    @AndroidEntryPoint(PreferenceFragmentCompat.class)
    public static final class SettingsFragment extends Hilt_SettingsActivity_SettingsFragment {

        @Inject PreferenceController mPreferenceController;

        @Override
        public void onResume() {
            super.onResume();
            mPreferenceController.evaluate(getPreferenceManager());
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            mPreferenceController.evaluate(getPreferenceManager());
        }
    }
}
