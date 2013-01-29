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

import android.app.ActivityManagerNative;
import android.app.IInstrumentationWatcher;
import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

class InstrumentationAdapter extends BaseAdapter
{
    private PackageManager mPM;

    public InstrumentationAdapter(Context context, String targetPackage)
    {
        mContext = context;
        mTargetPackage = targetPackage;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPM = context.getPackageManager();

        mList = context.getPackageManager().queryInstrumentation(mTargetPackage, 0);
        if (mList != null) {
            Collections.sort(mList, new InstrumentationInfo.DisplayNameComparator(mPM));
        }
    }

    public ComponentName instrumentationForPosition(int position)
    {
        if (mList == null) {
            return null;
        }
        InstrumentationInfo ii = mList.get(position);
        return new ComponentName(ii.packageName, ii.name);
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

    private final void bindView(View view, InstrumentationInfo info)
    {
        TextView text = (TextView)view.findViewById(android.R.id.text1);
        CharSequence label = info.loadLabel(mPM);
        text.setText(label != null ? label : info.name);
    }

    protected final Context mContext;
    protected final String mTargetPackage;
    protected final LayoutInflater mInflater;

    protected List<InstrumentationInfo> mList;
}

public class InstrumentationList extends ListActivity
{
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mProfilingMode = icicle != null && icicle.containsKey("profiling");
        setListAdapter(new InstrumentationAdapter(this, null));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        mProfilingItem = menu.add(0, 0, 0, "Profiling Mode")
        .setOnMenuItemClickListener(mProfilingCallback);
        mProfilingItem.setCheckable(true);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        mProfilingItem.setChecked(mProfilingMode);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        if (mProfilingMode) {
            outState.putBoolean("profiling", true);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        ComponentName className = ((InstrumentationAdapter)getListAdapter()).
            instrumentationForPosition(position);
        if (className != null) {
            String profilingFile = null;
            if (mProfilingMode) {
                profilingFile = "/tmp/trace/" + className + ".dmtrace";
            }
            try {
                ActivityManagerNative.getDefault().
                    startInstrumentation(className, profilingFile, 0, null, mWatcher, null,
                            UserHandle.myUserId());
            } catch (RemoteException ex) {
            }
        }
    }

    private MenuItem.OnMenuItemClickListener mProfilingCallback =
            new MenuItem.OnMenuItemClickListener()
    {
        public boolean onMenuItemClick(MenuItem item) {
            mProfilingMode = !mProfilingMode;
            return true;
        }
    };

    private IInstrumentationWatcher mWatcher = new IInstrumentationWatcher.Stub() {
        
        public void instrumentationStatus(ComponentName name, int resultCode, Bundle results) {
            if (results != null) {
                for (String key : results.keySet()) {
                    Log.i("instrumentation", 
                            "INSTRUMENTATION_STATUS_RESULT: " + key + "=" + results.get(key));
                }
            }
            Log.i("instrumentation", "INSTRUMENTATION_STATUS_CODE: " + resultCode);
        }
    	public void instrumentationFinished(ComponentName name,
    	            int resultCode, Bundle results) {
            if (results != null) {
                for (String key : results.keySet()) {
                    Log.i("instrumentation", 
                            "INSTRUMENTATION_RESULT: " + key + "=" + results.get(key));
                }
            }
            Log.i("instrumentation", "INSTRUMENTATION_CODE: " + resultCode);
    	}
    };

    private MenuItem mProfilingItem;
    private boolean mProfilingMode;
}
