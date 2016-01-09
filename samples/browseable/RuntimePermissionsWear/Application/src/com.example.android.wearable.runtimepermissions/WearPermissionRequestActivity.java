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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

/**
 * This is a simple splash screen (activity) for giving more details on why the user should approve
 * phone permissions for storage. If they choose to move forward, the permission screen
 * is brought up. Either way (approve or disapprove), this will exit to the MainPhoneActivity after
 * they are finished with their final decision.
 *
 * If this activity is started by our service (IncomingRequestPhoneService) it is marked via an
 * extra (MainPhoneActivity.EXTRA_PROMPT_PERMISSION_FROM_WEAR). That service only starts
 * this activity if the phone permission hasn't been approved for the data wear is trying to access.
 * When the user decides within this Activity what to do with the permission request, it closes and
 * opens the MainPhoneActivity (to maintain the app experience). It also again passes along the same
 * extra (MainPhoneActivity.EXTRA_PROMPT_PERMISSION_FROM_WEAR) to alert MainPhoneActivity to
 * send the results of the user's decision to the wear device.
 */
public class WearPermissionRequestActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "WearRationale";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_permission_request);
    }

    public void onClickApprovePermissionRequest(View view) {
        Log.d(TAG, "onClickApprovePermissionRequest()");
        setResult(Activity.RESULT_OK);
        finish();
    }

    public void onClickDenyPermissionRequest(View view) {
        Log.d(TAG, "onClickDenyPermissionRequest()");
        setResult(Activity.RESULT_CANCELED);
        finish();
    }
}