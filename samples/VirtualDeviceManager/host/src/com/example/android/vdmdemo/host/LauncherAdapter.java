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

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.ResolveInfoFlags;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class LauncherAdapter extends BaseAdapter {

    private final List<ResolveInfo> mAvailableApps = new ArrayList<>();
    private final PackageManager mPackageManager;
    private int mTextColor = Color.BLACK;

    LauncherAdapter(PackageManager packageManager) {
        this(packageManager, null);
    }

    LauncherAdapter(PackageManager packageManager, WallpaperManager wallpaperManager) {
        mPackageManager = packageManager;

        if (wallpaperManager != null) {
            WallpaperColors wallpaperColors =
                    wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
            if ((wallpaperColors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) == 0) {
                mTextColor = Color.WHITE;
            }
        }

        mAvailableApps.addAll(
                packageManager.queryIntentActivities(
                        new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
                        ResolveInfoFlags.of(PackageManager.MATCH_ALL)));
    }

    @Override
    public int getCount() {
        return mAvailableApps.size();
    }

    @Override
    public Object getItem(int position) {
        return mAvailableApps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ResolveInfo ri = mAvailableApps.get(position);
        final Drawable img = ri.loadIcon(mPackageManager);
        if (convertView == null) {
            convertView =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.launcher_grid_item, parent, false);
        }
        ImageView imageView = convertView.requireViewById(R.id.app_icon);
        final Drawable background = new ShapeDrawable(new OvalShape());
        imageView.setBackground(background);
        imageView.setImageDrawable(img);

        TextView textView = convertView.requireViewById(R.id.app_title);
        textView.setText(ri.loadLabel(mPackageManager));
        textView.setTextColor(mTextColor);
        return convertView;
    }

    public Intent createPendingRemoteIntent(int position) {
        if (position >= mAvailableApps.size()) {
            return null;
        }
        ResolveInfo ri = mAvailableApps.get(position);
        if (ri == null) {
            return null;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        if (ri.activityInfo != null) {
            intent.setComponent(
                    new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
        } else {
            intent.setComponent(new ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
}
