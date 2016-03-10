/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pm.shortcutdemo;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.ArrayMap;

public class AppLabelCache {
    private final Context mContext;
    private ArrayMap<String, String> mAppNames = new ArrayMap<>();

    public AppLabelCache(Context context) {
        mContext = context;
    }

    public String getAppLabel(String packageName) {
        String name = mAppNames.get(packageName);
        if (name != null) {
            return name;
        }
        PackageManager pm = mContext.getPackageManager();
        try {
            final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            name = pm.getApplicationLabel(ai).toString();
        } catch (NameNotFoundException e) {
            return packageName;
        }
        mAppNames.put(packageName, name);
        return name;
    }
}
