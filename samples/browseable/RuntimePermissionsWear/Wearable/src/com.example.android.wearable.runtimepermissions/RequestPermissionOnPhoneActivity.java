/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.runtimepermissions;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;

/**
 * Asks user if they want to open permission screen on their remote device (phone).
 */
public class RequestPermissionOnPhoneActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permission_on_phone);
        setAmbientEnabled();
    }

    public void onClickPermissionPhoneStorage(View view) {
        setResult(RESULT_OK);
        finish();
    }
}