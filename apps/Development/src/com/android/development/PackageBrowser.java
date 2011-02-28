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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.IPackageDeleteObserver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PackageBrowser extends ListActivity {
    static class MyPackageInfo {
        PackageInfo info;
        String label;
    }
    
    private PackageListAdapter mAdapter;
    private List<MyPackageInfo> mPackageInfoList = new ArrayList<MyPackageInfo>();
    private Handler mHandler;
    private BroadcastReceiver mRegisteredReceiver;

    public class PackageListAdapter extends ArrayAdapter<MyPackageInfo> {

        public PackageListAdapter(Context context) {
            super(context, R.layout.package_list_item);
            List<PackageInfo> pkgs = context.getPackageManager().getInstalledPackages(0);
            for (int i=0; i<pkgs.size(); i++) {
                MyPackageInfo info = new MyPackageInfo();
                info.info = pkgs.get(i);
                info.label = info.info.applicationInfo.loadLabel(getPackageManager()).toString();
                mPackageInfoList.add(info);
            }
            if (mPackageInfoList != null) {
                Collections.sort(mPackageInfoList, sDisplayNameComparator);
            }
            setSource(mPackageInfoList);
        }
    
        @Override
        public void bindView(View view, MyPackageInfo info) {
            ImageView icon = (ImageView)view.findViewById(R.id.icon);
            TextView name = (TextView)view.findViewById(R.id.name);
            TextView description = (TextView)view.findViewById(R.id.description);
            icon.setImageDrawable(info.info.applicationInfo.loadIcon(getPackageManager()));
            name.setText(info.label);
            description.setText(info.info.packageName);
        }
    }

    /**
     * Receives notifications when applications are added/removed.
     */
    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // todo: this is a bit brute force.  We should probably get the action and package name
            //       from the intent and just add to or delete from the mPackageInfoList
            setupAdapter();
        }
    }

    private final static Comparator<MyPackageInfo> sDisplayNameComparator
            = new Comparator<MyPackageInfo>() {
        public final int
        compare(MyPackageInfo a, MyPackageInfo b) {
            return collator.compare(a.label, b.label);
        }

        private final Collator   collator = Collator.getInstance();
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setupAdapter();
        mHandler= new Handler();
        registerIntentReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRegisteredReceiver != null) {
            unregisterReceiver(mRegisteredReceiver);
        }
    }

    private void setupAdapter() {
        mAdapter = new PackageListAdapter(this);
        setListAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Delete package").setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                deletePackage();
                return true;
            }
        });
        return true;
    }

    private void deletePackage() {
        final int curSelection = getSelectedItemPosition();
        if (curSelection >= 0) {
            // todo: verification dialog for package deletion
            final MyPackageInfo packageInfo = mAdapter.itemForPosition(curSelection);
            if (packageInfo != null) {
                getPackageManager().deletePackage(packageInfo.info.packageName,
                                                  new IPackageDeleteObserver.Stub() {
                    public void packageDeleted(String packageName, int returnCode)
                            throws RemoteException {
                        if (returnCode == PackageManager.DELETE_SUCCEEDED) {
                            mPackageInfoList.remove(curSelection);
                            mHandler.post(new Runnable() {
                                    public void run() {
                                        mAdapter.notifyDataSetChanged();
                                    }
                                });

                            // todo: verification dialog for data directory
                            final String dataPath = packageInfo.info.applicationInfo.dataDir;
                            // todo: delete the data directory
                        } else {
                            mHandler.post(new Runnable() {
                                    public void run() {
                                        new AlertDialog.Builder(PackageBrowser.this)
                                            .setTitle("Oops")
                                            .setMessage("Could not delete package." +
                                                "  Maybe it is in /system/app rather than /data/app?")
                                            .show();
                                    }
                                });

                        }
                    }
                },
                                                  0);
            }
        }
    }

    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        mRegisteredReceiver = new ApplicationsIntentReceiver();
        registerReceiver(mRegisteredReceiver, filter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        MyPackageInfo info =
            mAdapter.itemForPosition(position);
        if (info != null) {
            Intent intent = new Intent(
                null, Uri.fromParts("package", info.info.packageName, null));
            intent.setClass(this, PackageSummary.class);
            startActivity(intent);
        }
    }
}
