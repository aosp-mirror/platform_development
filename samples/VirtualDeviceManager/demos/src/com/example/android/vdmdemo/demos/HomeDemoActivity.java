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
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/** Demo activity for showcasing Virtual Devices with home experience. */
public final class HomeDemoActivity extends AppCompatActivity {

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
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(category);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ActivityOptions options = ActivityOptions.makeBasic();
                    if (which > 0) {
                        options.setLaunchDisplayId(displays[which - 1].getDisplayId());
                    }
                    startActivity(intent, options.toBundle());
                });
        alertDialogBuilder.show();
    }
}
