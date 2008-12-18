/*
**
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package com.android.development;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppHwConfigList extends ListActivity {
    private static final String TAG = "AppHwConfigList";
    PackageManager mPm;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPm = getPackageManager();
        mAdapter = new AppListAdapter(this);
        if (mAdapter.getCount() <= 0) {
            finish();
        } else {
            setListAdapter(mAdapter);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PackageInfo app = mAdapter.appForPosition(position);
        // TODO display all preference settings
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, AppHwPref.class);
        intent.putExtra("packageName", app.packageName);
        startActivity(intent);
    }

    private final class AppListAdapter extends BaseAdapter {
        public AppListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            List<ApplicationInfo> appList = mPm.getInstalledApplications(0);
            for (ApplicationInfo app : appList) {
                PackageInfo pkgInfo = null;
                try {
                    pkgInfo = mPm.getPackageInfo(app.packageName, 0);
                } catch (NameNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ((pkgInfo != null)) {
                        if(mList == null) {
                             mList = new ArrayList<PackageInfo>();
                         }
                         mList.add(pkgInfo);
                }
            }
            if (mList != null) {
                Collections.sort(mList, sDisplayNameComparator);
            }
        }
    
        public PackageInfo appForPosition(int position) {
            if (mList == null) {
                return null;
            }
            return mList.get(position);
        }

        public int getCount() {
            return mList != null ? mList.size() : 0;
        }

        public Object getItem(int position) {
            return position;
        }
    
        public long getItemId(int position) {
            return position;
        }
    
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = mInflater.inflate(
                        android.R.layout.simple_list_item_1, parent, false);
            } else {
                view = convertView;
            }
            bindView(view, mList.get(position));
            return view;
        }
    
        private final void bindView(View view, PackageInfo info) {
            TextView text = (TextView)view.findViewById(android.R.id.text1);
            text.setText(info != null ? info.applicationInfo.loadLabel(mPm) : "(none)");
        }
    
        protected final Context mContext;
        protected final LayoutInflater mInflater;
        protected List<PackageInfo> mList;
        
    }

    private final Comparator sDisplayNameComparator = new Comparator() {
        public final int compare(Object a, Object b) {
            CharSequence  sa = ((PackageInfo) a).applicationInfo.loadLabel(mPm);
            CharSequence  sb = ((PackageInfo) b).applicationInfo.loadLabel(mPm);
            return collator.compare(sa, sb);
        }
        private final Collator   collator = Collator.getInstance();
    };

    private AppListAdapter mAdapter;
}

