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
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Time;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class ShortcutPublisher extends Activity {
    public static final String TAG = "ShortcutDemo";

    private ShortcutManager mShortcutManager;

    private ListView mList;
    private MyAdapter mAdapter;

    private final Random mRandom = new Random();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mShortcutManager = getSystemService(ShortcutManager.class);

        mList = (ListView) findViewById(android.R.id.list);
        mAdapter = new MyAdapter(this);
        mList.setAdapter(mAdapter);

        Log.d(TAG, "extras=" + getIntent().getExtras());
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private List<ShortcutInfo> getAllShortcuts() {
        final Map<String, ShortcutInfo> map = new ArrayMap<>();
        for (ShortcutInfo si : mShortcutManager.getDynamicShortcuts()) {
            if (!map.containsKey(si.getId())) {
                map.put(si.getId(), si);
            }
        }
        for (ShortcutInfo si : mShortcutManager.getPinnedShortcuts()) {
            if (!map.containsKey(si.getId())) {
                map.put(si.getId(), si);
            }
        }
        return new ArrayList<>(map.values());
    }

    private void refreshList() {
        final List<ShortcutInfo> list = getAllShortcuts();
        Collections.sort(list, mShortcutComparator);
        mAdapter.setShortcuts(list);
    }

    private final Comparator<ShortcutInfo> mShortcutComparator =
            (ShortcutInfo s1, ShortcutInfo s2) -> {
                int ret = 0;
                ret = (s1.isDynamic() ? 0 : 1) - (s2.isDynamic() ? 0 : 1);
                if (ret != 0) return ret;

                ret = s1.getId().compareTo(s2.getId());
                if (ret != 0) return ret;

                return 0;
            };

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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showThrottledToast() {
        showToast("Throttled, use \"adb shell cmd shortcut reset-throttling\" to reset counters");
    }

    private void callApi(BooleanSupplier call) {
        try {
            if (!call.getAsBoolean()) {
                showThrottledToast();
            }
            refreshList();
        } catch (RuntimeException r) {
            Log.w(TAG, r.getMessage(), r);
            showToast(r.getMessage());
        }
    }

    private List<Pair<String, String>> mIntentList = Arrays.asList(
            Pair.create("Google Search", "http://www.google.com"),
            Pair.create("Google Mail", "http://mail.google.com"),
            Pair.create("Google Maps", "http://maps.google.com"),
            Pair.create("Google Drive", "http://drive.google.com"),
            Pair.create("Google Photos", "http://photos.google.com"),
            Pair.create("Google Hangouts", "http://hangouts.google.com"),
            Pair.create("Google+", "http://plus.google.com")
    );

    public ShortcutInfo.Builder addRandomIntents(ShortcutInfo.Builder b) {
        final int i = mRandom.nextInt(mIntentList.size());
        b.setTitle(mIntentList.get(i).first);
        b.setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(mIntentList.get(i).second)));
        b.setIcon(Icon.createWithResource(this, R.drawable.icon2));
        return b;
    }

    public void onPublishPressed(View view) {
        dumpCurrentShortcuts();
        final Icon icon2 = Icon.createWithBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.icon_large_2));
        final Icon icon3 = Icon.createWithContentUri(
                Uri.parse("content://" + getPackageName() + "/" + R.drawable.icon_large_3));

        final Intent intent2 = new Intent(Intent.ACTION_VIEW);
        intent2.setClass(this, ShortcutPublisher.class);

        final Intent intent3 = new Intent(Intent.ACTION_VIEW);
        intent3.setClass(this, ShortcutPublisher.class);
        intent3.putExtra("str", "str-value");
        intent3.putExtra("nest", new Bundle());
        intent3.getBundleExtra("nest").putInt("int", 123);

        final ShortcutInfo si1 = addRandomIntents(new ShortcutInfo.Builder(this)
                .setId("shortcut1")
                .setWeight(10)).build();

        final ShortcutInfo si2 = new ShortcutInfo.Builder(this)
                .setId("shortcut2")
                .setTitle("Shortcut Demo Main")
                .setIcon(icon2)
                .setWeight(5)
                .setIntent(intent2)
                .build();

        final ShortcutInfo si3 = new ShortcutInfo.Builder(this)
                .setId("shortcut3")
                .setTitle("Shortcut Demo Main with extras")
                .setIcon(icon3)
                .setWeight(15)
                .setIntent(intent3)
                .build();

        callApi(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return mShortcutManager.setDynamicShortcuts(Arrays.asList(si1, si2, si3));
            }
        });
    }

    public void onDeleteAllPressed(View view) {
        callApi(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                mShortcutManager.deleteAllDynamicShortcuts();
                return true;
            }
        });
    }

    static String formatTime(long time) {
        Time tobj = new Time();
        tobj.set(time);
        return tobj.format("%Y-%m-%d %H:%M:%S");
    }

    public void onAddPressed(View view) {
        final ShortcutInfo si = addRandomIntents(new ShortcutInfo.Builder(this)
                .setId("shortcut-" + formatTime(System.currentTimeMillis()))
                .setWeight(10)).build();
        callApi(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return mShortcutManager.addDynamicShortcut(si);
            }
        });
    }

    public void onUpdatePressed(View view) {
        final List updateList = new ArrayList<>();

        for (ShortcutInfo si : getAllShortcuts()) {
            updateList.add(addRandomIntents(new ShortcutInfo.Builder(this)
                    .setId(si.getId()))
                    .build());
        }
        callApi(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return mShortcutManager.updateShortcuts(updateList);
            }
        });
    }

    void launch(ShortcutInfo si) {
        startActivity(si.getIntent());
    }

    void deleteDynamic(ShortcutInfo si) {
        mShortcutManager.deleteDynamicShortcut(si.getId());
        refreshList();
    }

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
            return si.isDynamic();
        }

        @Override
        protected String getAction2Text(ShortcutInfo si) {
            return "Delete";
        }

        @Override
        protected void onLaunchClicked(ShortcutInfo si) {
            launch(si);
        }

        @Override
        protected void onAction2Clicked(ShortcutInfo si) {
            deleteDynamic(si);
        }
    }
}
