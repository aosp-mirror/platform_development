/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * A singleInstance activity that is responsible for a launching a bootstrap stack of activities.
 */
public class LauncherActivity extends BaseActivity {
    public static final String TAG = "LauncherActivity";
    private static final long SNACKBAR_DELAY = 75;
    private TestBase mTester;
    private boolean mFirstLaunch;
    private View mSnackBarRootView;
    private boolean mSnackBarIsVisible = false;
    private boolean mDontLaunch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupTaskPreset();
        mFirstLaunch = true;
    }

    /**
     * Launches activity with the selected options.
     */
    @Override
    public void launchActivity(Intent intent) {
        startActivityForResult(intent, LAUNCH_REQUEST_CODE);
        // If people press back we want them to see the overview rather than the launch fragment.
        // To achieve this we pop the launchFragment from the stack when we go to the next activity.
        getSupportFragmentManager().popBackStack();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
