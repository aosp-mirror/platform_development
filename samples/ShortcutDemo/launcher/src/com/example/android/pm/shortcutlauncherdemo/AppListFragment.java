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

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListFragment extends MyBaseListFragment {
    private AppAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new AppAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    protected void refreshList() {
        Log.d(Global.TAG, "Loading apps and shortcuts...");

        final List<LauncherActivityInfo> apps = new ArrayList<>();

        for (UserHandle user : mUserManager.getUserProfiles()) {
            apps.addAll(mLauncherApps.getActivityList(null, user));
        }
        Collections.sort(apps, sLauncherIconComparator);

        Log.d(Global.TAG, "Apps and shortcuts loaded.");

        mAdapter.setList(apps);
    }

    private static final Comparator<LauncherActivityInfo> sLauncherIconComparator =
            (LauncherActivityInfo l1, LauncherActivityInfo l2) -> {
                int ret = 0;
                ret = l1.getLabel().toString().compareTo(l2.getLabel().toString());
                if (ret != 0) return ret;

                // TODO Don't rely on hashCode being the user-id.
                ret = l1.getUser().hashCode() - l2.getUser().hashCode();
                if (ret != 0) return ret;

                return 0;
            };

    public class AppAdapter extends BaseAdapter implements OnClickListener {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final UserManager mUserManager;
        private final LauncherApps mLauncherApps;
        private List<LauncherActivityInfo> mList;

        public AppAdapter(Context context) {
            mContext = context;
            mInflater = mContext.getSystemService(LayoutInflater.class);
            mUserManager = mContext.getSystemService(UserManager.class);
            mLauncherApps = mContext.getSystemService(LauncherApps.class);
        }

        public void setList(List<LauncherActivityInfo> list) {
            mList = list;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList == null ? 0 : mList.size();
        }

        @Override
        public LauncherActivityInfo getItem(int position) {
            return mList == null ? null : mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.list_item, null);
            }

            bindView(view, getItem(position));

            return view;
        }

        public void bindView(View view, LauncherActivityInfo ai) {
            {
                final View v = view.findViewById(R.id.launch);

                v.setTag(ai);

                v.setOnClickListener(this);
                v.setVisibility(View.VISIBLE);
            }
            {
                final Button v = (Button) view.findViewById(R.id.action2);

                v.setTag(ai);

                v.setVisibility(View.INVISIBLE);
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
                            v.setOnClickListener(this);
                            v.setVisibility(View.VISIBLE);
                            v.setText("Shortcuts");
                        }
                    }
                } catch (Exception e) {
                    Log.w(Global.TAG, "Caught exception", e);
                }
            }

            final TextView line1 = (TextView) view.findViewById(R.id.line1);
            final TextView line2 = (TextView) view.findViewById(R.id.line2);

            line1.setText(ai.getLabel());

            // TODO Do it on worker thread
            final Drawable icon = ai.getBadgedIcon(DisplayMetrics.DENSITY_DEFAULT);
            final ImageView image = (ImageView) view.findViewById(R.id.image);
            image.setImageDrawable(icon);
        }

        @Override
        public void onClick (View v){
            final LauncherActivityInfo ai = (LauncherActivityInfo) v.getTag();
            switch (v.getId()) {
                case R.id.launch:
                    try {
                        mLauncherApps.startMainActivity(ai.getComponentName(), ai.getUser(),
                                null, null);
                    } catch (Exception e) {
                        Global.showToast(getContext(), e.getMessage());
                    }
                    return;
                case R.id.action2:
                    showShortcutsForPackage(ai);
                    return;
            }
        }
    }

    private void showShortcutsForPackage(LauncherActivityInfo ai) {
        final Intent i = PackageShortcutActivity.getLaunchIntent(
                getActivity(),
                ai.getComponentName().getPackageName(),
                ai.getComponentName(),
                ai.getUser(),
                ai.getLabel());
        getActivity().startActivity(i);
    }
}