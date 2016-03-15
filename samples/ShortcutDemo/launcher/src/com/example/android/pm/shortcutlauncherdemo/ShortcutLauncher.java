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

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.example.android.pm.shortcutdemo.ShortcutAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutLauncher extends ListActivity {
    public static final String TAG = "ShortcutLauncherDemo";

    private LauncherApps mLauncherApps;

    private MyAdapter mAdapter;

    private ArrayMap<String, String> mAppNames = new ArrayMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mLauncherApps = getSystemService(LauncherApps.class);
        mLauncherApps.registerCallback(mLauncherCallback);

        if (mLauncherApps.hasShortcutHostPermission()) {
            mAdapter = new MyAdapter(this);

            setListAdapter(mAdapter);
        } else {
            showToast("Please make this app as the default launcher.");
            finish();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshList();
    }

    @Override
    protected void onDestroy() {
        mLauncherApps.unregisterCallback(mLauncherCallback);

        super.onDestroy();
    }

    private void togglePin(ShortcutInfo selected) {
        final String packageName = selected.getPackageName();

        final List<String> pinned = new ArrayList<>();
        for (ShortcutInfo si : mAdapter.getShortcuts()) {
            if (si.isPinned() && si.getPackageName().equals(packageName)) {
                pinned.add(si.getId());
            }
        }
        if (selected.isPinned()) {
            pinned.remove(selected.getId());
        } else {
            pinned.add(selected.getId());
        }
        mLauncherApps.pinShortcuts(packageName, pinned, Process.myUserHandle());
    }

    private void launch(ShortcutInfo si) {
        mLauncherApps.startShortcut(si.getPackageName(), si.getId(), null, null,
                Process.myUserHandle());
    }

    private String getAppLabel(String packageName) {
        String name = mAppNames.get(packageName);
        if (name != null) {
            return name;
        }
        PackageManager pm = getPackageManager();
        try {
            final ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (NameNotFoundException e) {
            return packageName;
        }
    }

    private void refreshList() {
        final ShortcutQuery q = new ShortcutQuery();
        q.setQueryFlags(ShortcutQuery.FLAG_GET_DYNAMIC | ShortcutQuery.FLAG_GET_PINNED);

        final List<ShortcutInfo> list = mLauncherApps.getShortcuts(q, Process.myUserHandle());
        Collections.sort(list, mShortcutComparator);
        Log.i(TAG, "All shortcuts:");
        for (ShortcutInfo si : list) {
            Log.i(TAG, si.toString());
        }

        mAdapter.setShortcuts(list);
    }

    private final Comparator<ShortcutInfo> mShortcutComparator =
            (ShortcutInfo s1, ShortcutInfo s2) -> {
                int ret = 0;
                ret = getAppLabel(s1.getPackageName()).compareTo(getAppLabel(s2.getPackageName()));
                if (ret != 0) return ret;

                ret = s1.getId().compareTo(s2.getId());
                if (ret != 0) return ret;

                return 0;
            };

    private final LauncherApps.Callback mLauncherCallback = new LauncherApps.Callback() {
        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {
        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {
        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user,
                boolean replacing) {
        }

        @Override
        public void onShortcutsChanged(String packageName,
                List<ShortcutInfo> shortcuts, UserHandle user) {
            Log.w(TAG, "onShortcutsChanged: user=" + user + " package=" + packageName);
            for (ShortcutInfo si : shortcuts) {
                Log.i(TAG, si.toString());
            }
            refreshList();
        }
    };

    class MyAdapter extends ShortcutAdapter {
        public MyAdapter(Context context) {
            super(context);
        }

        @Override
        protected int getLayoutId() {
            return R.layout.list_item;
        }

        @Override
        protected int getText1Id() {
            return R.id.line1;
        }

        @Override
        protected int getText2Id() {
            return R.id.line2;
        }

        @Override
        protected int getImageId() {
            return R.id.image;
        }

        @Override
        protected int getLaunchId() {
            return R.id.launch;
        }

        @Override
        protected int getAction2Id() {
            return R.id.action2;
        }

        @Override
        protected boolean showLaunch(ShortcutInfo si) {
            return true;
        }

        @Override
        protected boolean showAction2(ShortcutInfo si) {
            return true;
        }

        @Override
        protected String getAction2Text(ShortcutInfo si) {
            return si.isPinned() ? "Unpin" : "Pin";
        }

        @Override
        protected void onLaunchClicked(ShortcutInfo si) {
            launch(si);
        }

        @Override
        protected void onAction2Clicked(ShortcutInfo si) {
            togglePin(si);
        }
    }
}
