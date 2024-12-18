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

package com.example.android.vdmdemo.demos;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;

/** Demo activity for showcasing Virtual Devices with permission requests. */
public final class PermissionsDemoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;

    private static final String[] DEVICE_AWARE_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    };

    private View mLayout;

    private static final String[] NON_DEVICE_AWARE_PERMISSIONS = {
        Manifest.permission.READ_CONTACTS,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permissions_demo_activity);
        mLayout = findViewById(R.id.main_layout);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            String output = parseGrantResults(permissions, grantResults);
            Snackbar.make(mLayout, output, Snackbar.LENGTH_SHORT).show();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /** Handle permission request. */
    public void onRequestPermissions(View view) {
        if (view.getId() == R.id.request_device_aware_permissions) {
            requestPermissions(DEVICE_AWARE_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            requestPermissions(NON_DEVICE_AWARE_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    /** Handle permission revoke. */
    public void onRevokePermissions(View view) {
        revokeSelfPermissionsOnKill(Arrays.asList(DEVICE_AWARE_PERMISSIONS));
    }

    private String parseGrantResults(String[] permissions, int[] grantResults) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int grantResult = grantResults[i];

            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                result.append(permission).append(" is granted. ");
            } else {
                result.append(permission).append(" is denied. ");
            }
        }

        return result.toString();
    }
}
