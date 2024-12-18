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

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;

/** Demo activity for showcasing Virtual Devices with home experience. */
public class HomeDemoActivity extends AppCompatActivity {

    private DisplayManager mDisplayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_demo_activity);

        mDisplayManager = getSystemService(DisplayManager.class);
    }

    /** Handle home intent request. */
    public void onSendHomeIntent(View view) {
        sendIntentToDisplay(Intent.CATEGORY_HOME);
    }

    /** Handle secondary home intent request. */
    public void onSendSecondaryHomeIntent(View view) {
        sendIntentToDisplay(Intent.CATEGORY_SECONDARY_HOME);
    }

    /** Handle a request to move the task to back. */
    public void onMoveTaskToBack(View view) {
        moveTaskToBack(true);
    }

    /** Handle calculator intent request. */
    public void onCalculatorNewTask(View view) {
        sendIntentToDisplay(Intent.CATEGORY_APP_CALCULATOR);
    }

    private void sendIntentToDisplay(String category) {
        Display[] displays = mDisplayManager.getDisplays();

        String[] displayNames = new String[displays.length + 1];
        displayNames[0] = "No display";
        for (int i = 0; i < displays.length; ++i) {
            displayNames[i + 1] = displays[i].getName();
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(HomeDemoActivity.this);
        alertDialogBuilder.setTitle("Choose display");
        alertDialogBuilder.setItems(
                displayNames,
                (dialog, which) -> {
                    int displayId = which == 0
                            ? Display.INVALID_DISPLAY
                            : displays[which - 1].getDisplayId();
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(category);
                    String requiredDisplayCategory = getRequiredDisplayCategory(displayId);
                    if (requiredDisplayCategory != null) {
                        intent.addCategory(requiredDisplayCategory);
                    }
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ActivityOptions options = ActivityOptions.makeBasic();
                    if (displayId != Display.INVALID_DISPLAY) {
                        options.setLaunchDisplayId(displayId);
                    }
                    startActivity(intent, options.toBundle());
                });
        alertDialogBuilder.show();
    }

    private String getRequiredDisplayCategory(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY || !BuildCompat.isAtLeastV()) {
            return null;
        }
        if (displayId == Display.INVALID_DISPLAY) {
            displayId = getDisplay().getDisplayId();
        }
        // Check if the desired display id belongs to the current virtual device (if any).
        // If the current display has a category, assume all displays on that device have the same
        // category (not true in the general case).
        // There's no API to get the categories for a given display.
        VirtualDeviceManager vdm = getSystemService(VirtualDeviceManager.class);
        VirtualDevice virtualDevice = vdm.getVirtualDevice(getDeviceId());
        if (virtualDevice == null) {
            return null;
        }
        boolean sameDevice = false;
        for (int deviceDisplayId : vdm.getVirtualDevice(getDeviceId()).getDisplayIds()) {
            if (deviceDisplayId == displayId) {
                sameDevice = true;
                break;
            }
        }
        if (!sameDevice) {
            return null;
        }

        // We're running on a virtual device and the launch display belongs to the same device.
        // Return the current requiredDisplayCategory.
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(
                    getComponentName(), PackageManager.GET_ACTIVITIES);
            return ai.requiredDisplayCategory;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
