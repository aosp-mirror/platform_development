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

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

public class RunningProcesses extends ListActivity {
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
        ListItem app = mAdapter.appForPosition(position);
        // Create intent to start new activity
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setClass(this, ProcessInfo.class);
        intent.putExtra("processName", app.procInfo.processName);
        intent.putExtra("packageList", app.procInfo.pkgList);
        // start new activity to display extended information
        startActivity(intent);
    }

    private final class AppListAdapter extends BaseAdapter {
        public AppListAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo app : appList) {
                if(mList == null) {
                    mList = new ArrayList<ListItem>();
                }
                mList.add(new ListItem(app));    
            }
            if (mList != null) {
                Collections.sort(mList, sDisplayNameComparator);
            }
        }
    
        public ListItem appForPosition(int position) {
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
    
        private final void bindView(View view, ListItem info) {
            TextView text = (TextView)view.findViewById(android.R.id.text1);
            text.setText(info != null ? info.procInfo.processName : "(none)");
        }
    
        protected final Context mContext;
        protected final LayoutInflater mInflater;
        protected List<ListItem> mList;
        
    }

    private final Comparator sDisplayNameComparator = new Comparator() {
        public final int compare(Object a, Object b) {
            CharSequence  sa = ((ListItem) a).procInfo.processName;
            CharSequence  sb = ((ListItem) b).procInfo.processName;
            return collator.compare(sa, sb);
        }
        private final Collator   collator = Collator.getInstance();
    };
    
    private class ListItem {
        ActivityManager.RunningAppProcessInfo procInfo;
        public ListItem(ActivityManager.RunningAppProcessInfo pInfo) {
            procInfo = pInfo;
        }
    }

    private AppListAdapter mAdapter;
}

