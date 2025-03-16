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
import android.app.AlertDialog;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Demo activity for showcasing Virtual Devices with permission requests. */
public final class PermissionsDemoActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;

    private static final String[] DEVICE_AWARE_PERMISSIONS = {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
    };

    private static final String[] NON_DEVICE_AWARE_PERMISSIONS = {
            Manifest.permission.READ_CONTACTS,
    };

    private static final String DEVICE_NAME_UNKNOWN = "Unknown";
    private static final String DEVICE_NAME_DEFAULT = "Default - " + Build.MODEL;

    private VirtualDeviceManager mVirtualDeviceManager;
    private Context mDeviceContext;

    private View mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permissions_demo_activity);
        mLayout = findViewById(R.id.main_layout);

        mVirtualDeviceManager = getSystemService(VirtualDeviceManager.class);

        registerDeviceIdChangeListener(getMainExecutor(), this::changeTargetDevice);

        mDeviceContext = this;
        changeTargetDevice(mDeviceContext.getDeviceId());
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
            requestPermissions(DEVICE_AWARE_PERMISSIONS, REQUEST_CODE_PERMISSIONS,
                    mDeviceContext.getDeviceId());
        } else {
            requestPermissions(NON_DEVICE_AWARE_PERMISSIONS, REQUEST_CODE_PERMISSIONS,
                    mDeviceContext.getDeviceId());
        }
    }

    /** Handle permission revoke. */
    public void onRevokePermissions(View view) {
        revokeSelfPermissionsOnKill(Arrays.asList(DEVICE_AWARE_PERMISSIONS));
        revokeSelfPermissionsOnKill(Arrays.asList(NON_DEVICE_AWARE_PERMISSIONS));
        Snackbar.make(mLayout, "Restart app to take effect", Snackbar.LENGTH_SHORT).show();
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

    private void updateCurrentDeviceTextView(Context context) {
        String deviceName = DEVICE_NAME_UNKNOWN;
        if (context.getDeviceId() == Context.DEVICE_ID_DEFAULT) {
            deviceName = DEVICE_NAME_DEFAULT;
        } else if (BuildCompat.isAtLeastV()) {
            VirtualDevice device = mVirtualDeviceManager.getVirtualDevice(context.getDeviceId());
            deviceName = Objects.requireNonNull(device).getName();
        } else {
            for (VirtualDevice virtualDevice : mVirtualDeviceManager.getVirtualDevices()) {
                if (virtualDevice.getDeviceId() == context.getDeviceId()) {
                    deviceName = virtualDevice.getName();
                    break;
                }
            }
        }
        TextView currentDevice = requireViewById(R.id.current_device);
        currentDevice.setText(context.getString(R.string.current_device, deviceName));
    }

    /** Handle device change request. */
    public void onChangeDevice(View view) {
        List<VirtualDevice> virtualDevices = mVirtualDeviceManager.getVirtualDevices();
        String[] devices = new String[virtualDevices.size() + 1];
        devices[0] = DEVICE_NAME_DEFAULT;
        for (int i = 0; i < virtualDevices.size(); ++i) {
            devices[i + 1] = virtualDevices.get(i).getName();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Available devices");
        alertDialogBuilder.setItems(
                devices,
                (dialog, which) -> {
                    int deviceId =
                            which > 0
                                    ? virtualDevices.get(which - 1).getDeviceId()
                                    : Context.DEVICE_ID_DEFAULT;
                    changeTargetDevice(deviceId);
                });
        alertDialogBuilder.show();
    }

    private void changeTargetDevice(int deviceId) {
        mDeviceContext.unregisterDeviceIdChangeListener(this::changeTargetDevice);
        mDeviceContext = createDeviceContext(deviceId);
        updateCurrentDeviceTextView(mDeviceContext);
    }
}
