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
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ShortcutInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public abstract class ShortcutAdapter extends BaseAdapter implements OnClickListener {
    public static final String TAG = "ShortcutDemo";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private LauncherApps mLauncherApps;
    private final AppLabelCache mAppLabelCache;
    private List<ShortcutInfo> mShortcuts;

    public ShortcutAdapter(Context context) {
        mContext = context;
        mAppLabelCache = new AppLabelCache(mContext);
        mInflater = mContext.getSystemService(LayoutInflater.class);
        mLauncherApps = mContext.getSystemService(LauncherApps.class);
    }

    protected abstract int getLayoutId();
    protected abstract int getText1Id();
    protected abstract int getText2Id();
    protected abstract int getImageId();
    protected abstract int getLaunchId();
    protected abstract int getAction2Id();

    protected boolean showLine2() {
        return true;
    }

    protected boolean showLaunch(ShortcutInfo si) {
        return false;
    }

    protected boolean showAction2(ShortcutInfo si) {
        return false;
    }

    protected String getAction2Text(ShortcutInfo si) {
        return "Action2";
    }

    protected void onLaunchClicked(ShortcutInfo si) {
    }

    protected void onAction2Clicked(ShortcutInfo si) {
    }

    public void setShortcuts(List<ShortcutInfo> shortcuts) {
        mShortcuts = shortcuts;
        notifyDataSetChanged();
    }

    public List<ShortcutInfo> getShortcuts() {
        return mShortcuts;
    }

    @Override
    public int getCount() {
        return mShortcuts == null ? 0 : mShortcuts.size();
    }

    @Override
    public Object getItem(int position) {
        return mShortcuts.get(position);
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
            view = mInflater.inflate(getLayoutId(), null);
        }

        bindView(view, position, mShortcuts.get(position));

        return view;
    }

    public void bindView(View view, int position, ShortcutInfo si) {
        {
            final View v = view.findViewById(getLaunchId());
            v.setVisibility(View.GONE);
            if (showLaunch(si)) {
                v.setOnClickListener(this);
                v.setVisibility(View.VISIBLE);
            }
        }
        {
            final Button v = (Button) view.findViewById(getAction2Id());
            v.setVisibility(View.GONE);
            if (showAction2(si)) {
                v.setOnClickListener(this);
                v.setVisibility(View.VISIBLE);
                v.setText(getAction2Text(si));
            }
        }

        final TextView line1 = (TextView) view.findViewById(getText1Id());
        final TextView line2 = (TextView) view.findViewById(getText2Id());

        view.setTag(si);

        line1.setText(si.getShortLabel());
        if (showLine2()) {
            line2.setText(
                    si.getId() + (si.isDynamic() ? " [dynamic]" : "")
                            + (si.isDeclaredInManifest() ? " [manifest]" : "")
                            + (si.isPinned() ? " [pinned]" : "") + "\n"
                            + "Long label: " + si.getLongLabel() + "\n"
                            + "App: " + mAppLabelCache.getAppLabel(si.getPackage()));
            line2.setVisibility(View.VISIBLE);
        } else {
            line2.setVisibility(View.GONE);
        }

        // view.setBackgroundColor(si.isPinned() ? Color.rgb(255, 255, 192) : Color.WHITE);

        // TODO Do it on worker thread
        final ImageView image = (ImageView) view.findViewById(getImageId());
        if (!mLauncherApps.hasShortcutHostPermission()) {
            image.setVisibility(View.GONE);
        } else {
            image.setVisibility(View.VISIBLE);
            image.setImageDrawable(mLauncherApps.getShortcutBadgedIconDrawable(si,
                    mContext.getResources().getDisplayMetrics().densityDpi));
        }
    }

    @Override
    public void onClick(View v) {
        final ShortcutInfo si = (ShortcutInfo)(((View) v.getParent()).getTag());
        if (v.getId() == getLaunchId()) {
            onLaunchClicked(si);
        } else if (v.getId() == getAction2Id()) {
            onAction2Clicked(si);
        }
    }
}
