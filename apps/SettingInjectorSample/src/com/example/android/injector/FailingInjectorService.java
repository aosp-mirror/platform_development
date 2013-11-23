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

package com.example.android.injector;

import android.location.SettingInjectorService;
import android.util.Log;

/**
 * Receiver that returns the status of the injected setting.
 */
public class FailingInjectorService extends SettingInjectorService {

    private static final String TAG = "FailingInjectorService";

    /**
     * Whether to actually throw an exception here. Pretty disruptive when true, because it causes
     * a "Unfortunately, My Setting Activity! has stopped" dialog to appear and also blocks the
     * update of other settings from this app. So only set true when need to test the handling
     * of the exception.
     */
    private static final boolean ACTUALLY_THROW = false;

    public FailingInjectorService() {
        super(TAG);
    }

    @Override
    protected String onGetSummary() {
        try {
            // Simulate a delay while reading the setting from disk
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e(TAG, "", e);
        }

        if (ACTUALLY_THROW) {
            throw new RuntimeException("Simulated failure reading setting");
        }
        return "Decided not to throw exception after all";
    }

    @Override
    protected boolean onGetEnabled() {
        return false;
    }
}
