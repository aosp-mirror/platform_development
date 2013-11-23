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

import android.content.Intent;
import android.location.SettingInjectorService;
import android.util.Log;

/**
 * Receiver that returns a constantly-updating status for an injected setting.
 */
public class UpdatingInjectorService extends SettingInjectorService {

    private static final String TAG = "UpdatingInjectorService";

    public UpdatingInjectorService() {
        super(TAG);
    }

    @Override
    protected String onGetSummary() {
        // Every time it asks for our status, we tell it the setting has just changed. This will
        // test the handling of races where we're getting many UPDATE_INTENT broadcasts in a short
        // period of time
        Intent intent = new Intent(ACTION_INJECTED_SETTING_CHANGED);
        sendBroadcast(intent);
        Log.d(TAG, "Broadcasting: " + intent);
        return String.valueOf(System.currentTimeMillis());
    }

    @Override
    protected boolean onGetEnabled() {
        return true;
    }
}
