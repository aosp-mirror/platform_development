/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.development;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PreferredPackages extends ListActivity {
    private static final int ADD_APP_REQUEST = 1;
    
    private PackageListAdapter mAdapter;
    private Handler mHandler;

    final static class Entry {
        final PackageInfo info;
        final CharSequence label;
        
        Entry(PackageInfo _info, CharSequence _label) {
            info = _info;
            label = _label;
        }
    }
    private final ArrayList<Entry> mPackageInfoList = new ArrayList<Entry>();
    
    final class PackageListAdapter extends ArrayAdapter<Entry> {
        public PackageListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
            List<PackageInfo> pkgs =
                    context.getPackageManager().getPreferredPackages(0);
            final int N = pkgs.size();
            mPackageInfoList.clear();
            for (int i=0; i<N; i++) {
                PackageInfo pi = pkgs.get(i);
                if (pi.applicationInfo == null) {
                    continue;
                }
                mPackageInfoList.add(new Entry(pi,
                        getPackageManager().getApplicationLabel(
                                pi.applicationInfo)));
            }
            Collections.sort(mPackageInfoList, sDisplayNameComparator);
            setSource(mPackageInfoList);
        }
    
        public void bindView(View view, Entry info) {
            TextView text = (TextView)view.findViewById(android.R.id.text1);
            text.setText(info.label);
        }
    }

    private final static Comparator<Entry> sDisplayNameComparator = new Comparator<Entry>() {
        public final int
        compare(Entry a, Entry b) {
            return collator.compare(a.toString(), b.toString());
        }

        private final Collator   collator = Collator.getInstance();
    };

    /**
     * Receives notifications when applications are added/removed.
     */
    private final BroadcastReceiver mAppsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            setupAdapter();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setupAdapter();
        mHandler = new Handler();
        registerIntentReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterIntentReceivers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Add Package").setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                addPackage();
                return true;
            }
        });
        menu.add(0, 0, 0, "Remove Package").setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                removePackage();
                return true;
            }
        });
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == ADD_APP_REQUEST && resultCode == RESULT_OK) {
            getPackageManager().addPackageToPreferred(intent.getAction());
            setupAdapter();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Entry info =
            mAdapter.itemForPosition(position);
        if (info != null) {
            Intent intent = new Intent(
                Intent.ACTION_VIEW,
                Uri.fromParts("package", info.info.packageName, null));
            intent.setClass(this, PackageSummary.class);
            startActivity(intent);
        }
    }

    private void setupAdapter() {
        mAdapter = new PackageListAdapter(this);
        setListAdapter(mAdapter);
    }

    private void removePackage() {
        final int curSelection = this.getSelectedItemPosition();
        if (curSelection >= 0) {
            final Entry packageInfo = mAdapter.itemForPosition(curSelection);
            if (packageInfo != null) {
                getPackageManager().removePackageFromPreferred(
                        packageInfo.info.packageName);
            }
            setupAdapter();
        }
    }

    private void addPackage() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(this, AppPicker.class);
        startActivityForResult(intent, ADD_APP_REQUEST);
    }
    
    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mAppsReceiver, filter);
    }

    private void unregisterIntentReceivers() {
        unregisterReceiver(mAppsReceiver);
    }
}
