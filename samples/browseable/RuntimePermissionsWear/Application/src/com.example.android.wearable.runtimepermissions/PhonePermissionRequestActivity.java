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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
public class PhonePermissionRequestActivity extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "PhoneRationale";

    /* Id to identify Location permission request. */
    private static final int PERMISSION_REQUEST_READ_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If permissions granted, we start the main activity (shut this activity down).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            startMainActivity();
        }

        setContentView(R.layout.activity_phone_permission_request);
    }

    public void onClickApprovePermissionRequest(View view) {
        Log.d(TAG, "onClickApprovePermissionRequest()");

        // On 23+ (M+) devices, External storage permission not granted. Request permission.
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_READ_STORAGE);
    }

    public void onClickDenyPermissionRequest(View view) {
        Log.d(TAG, "onClickDenyPermissionRequest()");
        startMainActivity();
    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        String permissionResult = "Request code: " + requestCode + ", Permissions: " + permissions
                + ", Results: " + grantResults;
        Log.d(TAG, "onRequestPermissionsResult(): " + permissionResult);

        if (requestCode == PERMISSION_REQUEST_READ_STORAGE) {
            // Close activity regardless of user's decision (decision picked up in main activity).
            startMainActivity();
        }
    }

    private void startMainActivity() {

        Intent mainActivityIntent = new Intent(this, MainPhoneActivity.class);

        /*
         * If service started this Activity (b/c wear requested data where permissions were not
         * approved), tells MainPhoneActivity to send results to wear device (via this extra).
         */
        boolean serviceStartedActivity = getIntent().getBooleanExtra(
                MainPhoneActivity.EXTRA_PROMPT_PERMISSION_FROM_WEAR, false);

        if (serviceStartedActivity) {
           mainActivityIntent.putExtra(
                   MainPhoneActivity.EXTRA_PROMPT_PERMISSION_FROM_WEAR, true);
        }

        startActivity(mainActivityIntent);
    }
}