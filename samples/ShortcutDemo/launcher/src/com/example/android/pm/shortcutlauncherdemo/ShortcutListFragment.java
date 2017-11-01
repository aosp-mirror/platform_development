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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.pm.shortcutdemo.ShortcutAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutListFragment extends MyBaseListFragment {
    private static final String TAG = "ShortcutListFragment";

    private static final String ARG_TARGET_PACKAGE = "target_package";
    private static final String ARG_TARGET_ACTIVITY = "target_activity";
    private static final String ARG_INCLUDE_DYNAMIC = "include_dynamic";
    private static final String ARG_INCLUDE_MANIFEST = "include_manifest";
    private static final String ARG_INCLUDE_PINNED = "include_pinned";
    private static final String ARG_USER = "user";
    private static final String ARG_SHOW_DETAILS = "show_details";

    private MyAdapter mAdapter;

    public ShortcutListFragment setArguments(String targetPackage, ComponentName targetActivity,
            boolean includeDynamic, boolean includeManifest,
            boolean includePinned, UserHandle user, boolean showDetails) {
        final Bundle b = new Bundle();
        b.putString(ARG_TARGET_PACKAGE, targetPackage);
        b.putParcelable(ARG_TARGET_ACTIVITY, targetActivity);
        b.putBoolean(ARG_INCLUDE_DYNAMIC, includeDynamic);
        b.putBoolean(ARG_INCLUDE_MANIFEST, includeManifest);
        b.putBoolean(ARG_INCLUDE_PINNED, includePinned);
        b.putParcelable(ARG_USER, user);
        b.putBoolean(ARG_SHOW_DETAILS, showDetails);

        setArguments(b);

        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserManager = getActivity().getSystemService(UserManager.class);
        mLauncherApps = getActivity().getSystemService(LauncherApps.class);

        mAdapter = new MyAdapter(getActivity(), getArguments().getBoolean(ARG_SHOW_DETAILS));
        setListAdapter(mAdapter);

        if (!mLauncherApps.hasShortcutHostPermission()) {
            Toast.makeText(getActivity(), "App doesn't have the shortcut permissions",
                    Toast.LENGTH_LONG).show();
        }
    }

    private List<UserHandle> getTargetUsers() {
        final UserHandle arg = getArguments().getParcelable(ARG_USER);
        if (arg == null) {
            return Compat.getProfiles(getActivity());
        } else {
            final List<UserHandle> ret = new ArrayList<>();
            ret.add(arg);
            return ret;
        }
    }

    private void togglePin(ShortcutInfo selected) {
        final String packageName = selected.getPackage();
        final UserHandle user = selected.getUserHandle();

        try {
            final ShortcutQuery q = new ShortcutQuery()
                    .setPackage(packageName)
                    .setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED)
                    ;

            final List<String> pinned = new ArrayList<>();
            for (ShortcutInfo si : mLauncherApps.getShortcuts(q, user)) {
                pinned.add(si.getId());
            }
            if (selected.isPinned()) {
                pinned.remove(selected.getId());
            } else {
                pinned.add(selected.getId());
            }
            mLauncherApps.pinShortcuts(packageName, pinned, selected.getUserHandle());
        } catch (Exception e) {
            Global.showToast(getContext(), e.getMessage());
        }
    }

    private void launch(ShortcutInfo si) {
        try {
            mLauncherApps.startShortcut(si.getPackage(), si.getId(), null, null,
                    si.getUserHandle());
        } catch (Exception e) {
            Global.showToast(getContext(), e.getMessage());
        }
    }

    @Override
    protected void refreshList() {
        Log.i(TAG, "Refreshing shortcuts");
        try {
            if (!mLauncherApps.hasShortcutHostPermission()) {
                return;
            }

            final List<ShortcutInfo> list = new ArrayList<>();

            for (UserHandle user : getTargetUsers()) {
                if (!mUserManager.isUserUnlocked(user)) {
                    continue;
                }

                // To detect a race condition, first fetch all shortcuts and report if none found.
                mQuery.setQueryFlags(
                        ShortcutQuery.FLAG_MATCH_PINNED | ShortcutQuery.FLAG_MATCH_DYNAMIC
                                | ShortcutQuery.FLAG_MATCH_MANIFEST
                                | ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY);
                mQuery.setPackage(null);
                mQuery.setActivity(null);
                mQuery.setChangedSince(0);
                final int numShortcuts = mLauncherApps.getShortcuts(mQuery, user).size();
                if (numShortcuts == 0) {
                    final String message = "No shortcut found for " + user;
                    Log.e(TAG, message);
                    Global.showToast(getContext(), message);
                }

                final Bundle b = getArguments();
                mQuery.setQueryFlags(
                        (b.getBoolean(ARG_INCLUDE_DYNAMIC) ? ShortcutQuery.FLAG_MATCH_DYNAMIC : 0) |
                        (b.getBoolean(ARG_INCLUDE_MANIFEST) ? ShortcutQuery.FLAG_MATCH_MANIFEST : 0) |
                        (b.getBoolean(ARG_INCLUDE_PINNED) ? ShortcutQuery.FLAG_MATCH_PINNED : 0));
                mQuery.setPackage(b.getString(ARG_TARGET_PACKAGE));
                mQuery.setActivity(b.getParcelable(ARG_TARGET_ACTIVITY));

                list.addAll(mLauncherApps.getShortcuts(mQuery, user));
            }
            Collections.sort(list, mShortcutComparator);

            mAdapter.setShortcuts(list);
        } catch (Exception e) {
            Log.w(Global.TAG, "Caught exception", e);
        }
    }

    private final Comparator<ShortcutInfo> mShortcutComparator =
            (ShortcutInfo s1, ShortcutInfo s2) -> {
                int ret = 0;
                ret = getAppLabel(s1.getPackage()).compareTo(getAppLabel(s2.getPackage()));
                if (ret != 0) return ret;

                ret = s1.getUserHandle().hashCode() - s2.getUserHandle().hashCode();
                if (ret != 0) return ret;

                ret = s1.getId().compareTo(s2.getId());
                if (ret != 0) return ret;

                return 0;
            };

    class MyAdapter extends ShortcutAdapter {
        private final boolean mShowLine2;

        public MyAdapter(Context context, boolean showLine2) {
            super(context);
            mShowLine2 = showLine2;
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

        @Override
        protected boolean showLine2() {
            return mShowLine2;
        }
    }
}
