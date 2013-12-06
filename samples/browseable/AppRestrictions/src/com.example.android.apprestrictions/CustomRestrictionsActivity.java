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

package com.example.android.apprestrictions;

import android.app.Activity;
import android.os.Bundle;

/**
 * This activity demonstrates how an app can integrate its own custom app restriction settings
 * with the restricted profile feature.
 *
 * This sample app maintains custom app restriction settings in shared preferences.  When
 * the activity is invoked (from Settings > Users), the stored settings are used to initialize
 * the custom configuration on the user interface.  Three sample input types are
 * shown: checkbox, single-choice, and multi-choice.  When the settings are modified by the user,
 * the corresponding restriction entries are saved, which are retrievable under a restricted
 * profile.
 */
public class CustomRestrictionsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(android.R.id.content,
                    new CustomRestrictionsFragment()).commit();
        }
    }
}
