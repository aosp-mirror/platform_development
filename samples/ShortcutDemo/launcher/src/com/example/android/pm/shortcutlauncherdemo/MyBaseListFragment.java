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
package com.example.android.pm.shortcutlauncherdemo;

import android.app.ListFragment;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public abstract class MyBaseListFragment extends ListFragment {
    protected UserManager mUserManager;
    protected LauncherApps mLauncherApps;

    private ArrayMap<String, String> mAppNames = new ArrayMap<>();

    protected final ShortcutQuery mQuery = new ShortcutQuery();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserManager = getActivity().getSystemService(UserManager.class);
        mLauncherApps = getActivity().getSystemService(LauncherApps.class);
        mLauncherApps.registerCallback(mLauncherCallback);
    }

    @Override
    public void onStart() {
        super.onStart();

        Log.d(Global.TAG, "Started");

        refreshList();
    }

    @Override
    public void onResume() {
        super.onResume();

        showPermissionWarningToastWhenNeeded();
    }

    @Override
    public void onDestroy() {
        mLauncherApps.unregisterCallback(mLauncherCallback);

        super.onDestroy();
    }

    protected void showPermissionWarningToastWhenNeeded() {
        if (!mLauncherApps.hasShortcutHostPermission()) {
            Toast.makeText(getActivity(), "App doesn't have the shortcut permissions",
                    Toast.LENGTH_SHORT).show();
        }
    }

    protected final String getAppLabel(String packageName) {
        String name = mAppNames.get(packageName);
        if (name != null) {
            return name;
        }
        PackageManager pm = getActivity().getPackageManager();
        try {
            final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (NameNotFoundException e) {
            return packageName;
        }
    }

    protected abstract void refreshList();

    private final LauncherApps.Callback mLauncherCallback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
            refreshList();
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
            refreshList();
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
            refreshList();
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
            refreshList();
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user,
                boolean replacing) {
            refreshList();
        }

        @Override
        public void onShortcutsChanged(String packageName,
                List<ShortcutInfo> shortcuts, UserHandle user) {
            refreshList();
        }
    };

}
