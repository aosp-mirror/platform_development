/*
** Copyright 2013, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.example.android.permdemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE_PREFS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivityForResult(
                new Intent(this, PrefsActivity.class)
                        .putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                        .putExtra(
                                PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                                PrefsFragment.class.getName()),
                REQUEST_CODE_PREFS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PREFS:
                // The PrefsActivity we launched has finished.
                finish();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

}
