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

package com.example.android.vdmdemo.host;

import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import dagger.hilt.android.AndroidEntryPoint;

/** Simple activity that can act as a home/launcher on a virtual device. */
@AndroidEntryPoint(AppCompatActivity.class)
public class CustomLauncherActivity extends Hilt_CustomLauncherActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_launcher);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        GridView launcher = requireViewById(R.id.app_grid);
        LauncherAdapter launcherAdapter =
                new LauncherAdapter(getPackageManager(), WallpaperManager.getInstance(this));
        launcher.setAdapter(launcherAdapter);
        launcher.setOnItemClickListener(
                (parent, v, position, id) -> {
                    Intent intent = launcherAdapter.createPendingRemoteIntent(position);
                    if (intent != null) {
                        startActivity(intent);
                    }
                });
    }

    @Override
    @SuppressWarnings("MissingSuperCall")
    public void onBackPressed() {
        // Do nothing
    }
}
