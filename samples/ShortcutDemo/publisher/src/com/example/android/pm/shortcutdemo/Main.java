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

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main extends Activity {
    public static final String TAG = "ShortcutDemo";

    private ShortcutManager mShortcutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mShortcutManager = getSystemService(ShortcutManager.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void dumpCurrentShortcuts() {
        Log.d(TAG, "Dynamic shortcuts:");
        for (ShortcutInfo si : mShortcutManager.getDynamicShortcuts()) {
            Log.d(TAG, "  " + si.toString());
        }
        Log.d(TAG, "Pinned shortcuts:");
        for (ShortcutInfo si : mShortcutManager.getPinnedShortcuts()) {
            Log.d(TAG, "  " + si.toString());
        }
    }

    private void showThrottledToast() {
        Toast.makeText(this,
                "Throttled, use \"adb shell cmd shortcut reset-throttling\" to reset counters",
                Toast.LENGTH_SHORT).show();
    }

    public void onPublishPressed(View view) {
        dumpCurrentShortcuts();
        final Icon icon1 = Icon.createWithResource(this, R.drawable.icon_large_1);
        final Icon icon2 = Icon.createWithBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.icon_large_2));
        final Icon icon3 = Icon.createWithContentUri(
                Uri.parse("content://com.example.android.pm.shortcuts/" + R.drawable.icon_large_3));

        final Intent intent1 = new Intent(Intent.ACTION_VIEW);
        intent1.setClass(this, Main.class);
        intent1.putExtra("str", "str-value");
        intent1.putExtra("nest", new Bundle());
        intent1.getBundleExtra("nest").putInt("int", 123);

        final Intent intent2 = new Intent(Intent.ACTION_VIEW);
        intent2.setClass(this, Main.class);
        intent2.putExtra("str", "2");

        final Intent intent3 = new Intent(Intent.ACTION_VIEW);
        intent2.setClass(this, Main.class);

        final ShortcutInfo si1 = new ShortcutInfo.Builder(this)
                .setId("shortcut1")
                .setTitle("Title 1")
                .setIcon(icon1)
                .setWeight(10)
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com/")))
                .build();

        final ShortcutInfo si2 = new ShortcutInfo.Builder(this)
                .setId("shortcut2")
                .setTitle("Title 2")
                .setIcon(icon2)
                .setWeight(5)
                .setIntent(intent2)
                .build();

        final ShortcutInfo si3 = new ShortcutInfo.Builder(this)
                .setId("shortcut3")
                .setTitle("Title 3")
                .setIcon(icon3)
                .setWeight(15)
                .setIntent(intent3)
                .build();

        if (!mShortcutManager.setDynamicShortcuts(Arrays.asList(si1, si2, si3))) {
            showThrottledToast();
        }
    }

    public void onDeleteAllPressed(View view) {
        mShortcutManager.deleteAllDynamicShortcuts();
    }
}
