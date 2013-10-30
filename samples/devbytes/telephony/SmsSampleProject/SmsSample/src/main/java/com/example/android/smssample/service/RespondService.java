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

package com.example.android.smssample.service;

import android.app.IntentService;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.example.android.smssample.Utils;

/**
 * This service handles the system intent ACTION_RESPOND_VIA_MESSAGE when we are the default SMS
 * app.
 */
public class RespondService extends IntentService {
    private static final String TAG = "RespondService";

    public RespondService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            if (Utils.hasJellyBeanMR2() && Utils.isDefaultSmsApp(this) &&
                    // ACTION_RESPOND_VIA_MESSAGE was added in JB MR2
                    TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) {
                // TODO: Handle "respond via message" quick reply
            }
        }
    }
}
