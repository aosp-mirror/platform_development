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

import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.List;

public class AppListFragment extends BaseActivityListFragment {

    @Override
    protected List<LauncherActivityInfo> getActivities(UserHandle user) {
        return mLauncherApps.getActivityList(null, user);
    }

    @Override
    protected void onBindAction2(Button v, LauncherActivityInfo ai, OnClickListener listener) {
        try {
            if (mUserManager.isUserUnlocked(ai.getUser())
                    && mLauncherApps.hasShortcutHostPermission()) {
                mQuery.setPackage(ai.getComponentName().getPackageName());
                mQuery.setQueryFlags(ShortcutQuery.FLAG_MATCH_DYNAMIC
                        | ShortcutQuery.FLAG_MATCH_PINNED
                        | ShortcutQuery.FLAG_MATCH_MANIFEST
                        | ShortcutQuery.FLAG_GET_KEY_FIELDS_ONLY);
                mQuery.setActivity(ai.getComponentName());

                if (mLauncherApps.getShortcuts(mQuery, ai.getUser()).size() > 0) {
                    v.setOnClickListener(listener);
                    v.setVisibility(View.VISIBLE);
                    v.setText("Shortcuts");
                }
            }
        } catch (Exception e) {
            Log.w(Global.TAG, "Caught exception", e);
        }
    }

    @Override
    protected void onLaunch(LauncherActivityInfo ai) {
        mLauncherApps.startMainActivity(ai.getComponentName(), ai.getUser(), null, null);
    }

    @Override
    protected void onAction2(LauncherActivityInfo ai) {
        final Intent i = PackageShortcutActivity.getLaunchIntent(
                getActivity(),
                ai.getComponentName().getPackageName(),
                ai.getComponentName(),
                ai.getUser(),
                ai.getLabel());
        getActivity().startActivity(i);
    }}