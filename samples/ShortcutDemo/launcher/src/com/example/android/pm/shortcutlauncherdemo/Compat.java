/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.pm.shortcutlauncherdemo;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.os.UserManager;
import androidx.core.os.BuildCompat;

import java.util.List;

public class Compat {
    public static List<UserHandle> getProfiles(Context context) {
        if (BuildCompat.isAtLeastO()) {
            return context.getSystemService(LauncherApps.class).getProfiles();
        } else {
            return context.getSystemService(UserManager.class).getUserProfiles();
        }
    }
}
