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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

/** Launcher activity for all VDM demos. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
    }

    /** Handles demo launch request. */
    public void onDemoSelected(View view) {
        switch (view.getId()) {
            case R.id.activity_policy_demo -> startActivity(
                    new Intent(this, ActivityPolicyDemoActivity.class));
            case R.id.home_demo -> startActivity(new Intent(this, HomeDemoActivity.class));
            case R.id.sensor_demo -> startActivity(new Intent(this, SensorDemoActivity.class));
            case R.id.display_power_demo -> startActivity(
                    new Intent(this, DisplayPowerDemoActivity.class));
            case R.id.rotation_demo -> startActivity(new Intent(this, RotationDemoActivity.class));
            case R.id.secure_window_demo -> startActivity(
                    new Intent(this, SecureWindowDemoActivity.class));
            case R.id.permissions_demo -> startActivity(
                    new Intent(this, PermissionsDemoActivity.class));
            case R.id.latency_demo -> startActivity(new Intent(this, LatencyDemoActivity.class));
            case R.id.vibration_demo -> startActivity(
                    new Intent(this, VibrationDemoActivity.class));
            case R.id.stylus_demo -> startActivity(new Intent(this, StylusDemoActivity.class));
        }
    }
}
