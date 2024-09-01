/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.os.PowerManager;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.stream.Stream;

/** Demo activity for display power and wake locks with VDM. */
public final class DisplayPowerDemoActivity extends AppCompatActivity {

    private PowerManager.WakeLock mProximityWakeLock;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_power_demo_activity);

        SwitchMaterial proximitySwitch = requireViewById(R.id.proximity_lock);
        SwitchMaterial keepScreenOnSwitch = requireViewById(R.id.keep_screen_on);
        SwitchMaterial overrideBrightnessSwitch = requireViewById(R.id.override_screen_brightness);
        Slider brightnessSlider = requireViewById(R.id.screen_brightness);

        PowerManager powerManager = getSystemService(PowerManager.class);
        if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            mProximityWakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "VdmDemo:proximityLock");
            proximitySwitch.setEnabled(true);
            proximitySwitch.setOnCheckedChangeListener((v, isChecked) -> {
                if (isChecked && !mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.acquire();
                } else if (!isChecked && mProximityWakeLock.isHeld()) {
                    mProximityWakeLock.release();
                }
            });
        } else {
            proximitySwitch.setEnabled(false);
        }

        keepScreenOnSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });

        overrideBrightnessSwitch.setOnCheckedChangeListener((v, isChecked) -> {
            brightnessSlider.setEnabled(isChecked);
            float value = isChecked ? brightnessSlider.getValue() : -1f;
            setScreenBrightness(value);
        });
        brightnessSlider.addOnChangeListener((slider, value, user) -> setScreenBrightness(value));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mProximityWakeLock != null && mProximityWakeLock.isHeld()) {
            mProximityWakeLock.release();
        }
    }

    private void setScreenBrightness(float value) {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = value;
        getWindow().setAttributes(layout);
    }

    /** Launches an activity with android:turnScreenOn */
    public void onLaunchTurnScreenOnActivity(View v) {
        Intent intent = new Intent(this, TurnScreenOnActivity.class);

        DisplayManager displayManager = getSystemService(DisplayManager.class);
        Display[] displays = displayManager.getDisplays();

        if (displays.length == 1) {
            startActivity(intent);
            return;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Choose display");
        alertDialogBuilder.setItems(
                Stream.of(displays).map(d -> d.getName()).toArray(String[]::new),
                (dialog, i) -> {
                    startActivity(
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            ActivityOptions.makeBasic().setLaunchDisplayId(
                                    displays[i].getDisplayId()).toBundle());
                });
        alertDialogBuilder.show();
    }
}
