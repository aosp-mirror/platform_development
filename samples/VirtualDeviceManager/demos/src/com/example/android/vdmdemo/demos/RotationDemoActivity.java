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

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Demo activity for display rotation with VDM. */
public final class RotationDemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rotation_demo_activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((TextView) requireViewById(R.id.current_orientation))
                .setText(getString(R.string.current_orientation, getOrientationString()));
    }

    /** Handle orientation change request. */
    public void onChangeOrientation(View view) {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if (view.getId() == R.id.portrait) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        } else if (view.getId() == R.id.landscape) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        setRequestedOrientation(orientation);
    }

    private String getOrientationString() {
        return switch (getRequestedOrientation()) {
            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED -> "unspecified";
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT -> "portrait";
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE -> "landscape";
            default -> "unknown";
        };
    }
}
