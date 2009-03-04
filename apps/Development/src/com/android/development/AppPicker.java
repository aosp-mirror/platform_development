/* //device/java/android/android/app/ResolveListActivity.java
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

import android.app.ActivityManagerNative;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppPicker extends ListActivity
{
    @Override
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        mAdapter = new AppListAdapter(this);
        if (mAdapter.getCount() <= 0) {
            finish();
        } else {
            setListAdapter(mAdapter);
        }
    }
    
    @Override
    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        ApplicationInfo app = mAdapter.appForPosition(position);
        Intent intent = new Intent();
        if (app != null) intent.setAction(app.packageName);
        setResult(RESULT_OK, intent);
        
        /* This is a temporary fix for 824637 while it is blocked by 805226.  When 805226 is resolved, please remove this. */
        try {
            boolean waitForDebugger = Settings.System.getInt(
                    getContentResolver(), Settings.System.WAIT_FOR_DEBUGGER, 0) != 0;
            ActivityManagerNative.getDefault().setDebugApp(
                    app != null ? app.packageName : null, waitForDebugger, true);
        } catch (RemoteException ex) {
        }
        
        finish();
    }

    private final class AppListAdapter extends BaseAdapter
    {
        public AppListAdapter(Context context)
        {
            mContext = context;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mList = context.getPackageManager().getInstalledApplications(0);
            if (mList != null) {
                Collections.sort(mList, sDisplayNameComparator);
                mList.add(0, null);
            }
        }
    
        public ApplicationInfo appForPosition(int position)
        {
            if (mList == null) {
                return null;
            }

            return mList.get(position);
        }

        public int getCount()
        {
            return mList != null ? mList.size() : 0;
        }

        public Object getItem(int position)
        {
            return position;
        }
    
        public long getItemId(int position)
        {
            return position;
        }
    
        public View getView(int position, View convertView, ViewGroup parent)
        {
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
    
        private final void bindView(View view, ApplicationInfo info)
        {
            TextView text = (TextView)view.findViewById(android.R.id.text1);
    
            text.setText(info != null ? info.packageName : "(none)");
        }
    
        protected final Context mContext;
        protected final LayoutInflater mInflater;
    
        protected List<ApplicationInfo> mList;
        
    }

    private final static Comparator sDisplayNameComparator = new Comparator() {
        public final int
        compare(Object a, Object b)
        {
            CharSequence  sa = ((ApplicationInfo) a).packageName;
            CharSequence  sb = ((ApplicationInfo) b).packageName;

            return collator.compare(sa, sb);
        }

        private final Collator   collator = Collator.getInstance();
    };

    private AppListAdapter mAdapter;
}

