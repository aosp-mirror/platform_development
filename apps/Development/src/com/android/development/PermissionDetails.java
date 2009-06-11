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

import com.android.development.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This activity displays permission details including
 * the list of apps using a permission.
 */
public class PermissionDetails extends Activity implements OnCancelListener, OnItemClickListener {
    private static final String TAG = "PermissionDetails";
    PackageManager mPm;
    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    private AppListAdapter mAdapter;

    // Dialog related
    private static final int DLG_BASE = 0;
    private static final int DLG_ERROR = DLG_BASE + 1;
    private static final String PROTECTION_NORMAL="Normal";
    private static final String PROTECTION_DANGEROUS="Dangerous";
    private static final String PROTECTION_SIGNATURE="Signature";
    private static final String PROTECTION_SIGNATURE_OR_SYSTEM="SignatureOrSystem";

    private static final String KEY_APPS_USING_PERM="AppsUsingPerm";

    private static final int HANDLER_MSG_BASE = 0;
    private static final int HANDLER_MSG_GET_APPS = HANDLER_MSG_BASE + 1;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HANDLER_MSG_GET_APPS:
                ArrayList<PackageInfo> appList = msg.getData().getParcelableArrayList(KEY_APPS_USING_PERM);
                createAppList(appList);
                break;
            }
        }
    };

    // View Holder used when displaying views
    static class AppViewHolder {
        TextView pkgName;
    }

    class AppListAdapter extends BaseAdapter {
        private List<PackageInfo> mList;

        AppListAdapter(List<PackageInfo> list) {
            mList = list;
        }

        public int getCount() {
            return mList.size();
        }

        public Object getItem(int position) {
            return mList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public PackageInfo getPkg(int position) {
            return mList.get(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            AppViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.pkg_list_item, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new AppViewHolder();
                holder.pkgName = (TextView) convertView.findViewById(R.id.pkg_name);
                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (AppViewHolder) convertView.getTag();
            }
            // Bind the data efficiently with the holder
            PackageInfo pInfo = mList.get(position);
            holder.pkgName.setText(pInfo.packageName);
            return convertView;
        }
    }

    private void createAppList(List<PackageInfo> list) {
        Log.i(TAG, "list.size=" + list.size());
        for (PackageInfo pkg : list) {
            Log.i(TAG, "Adding pkg : " +  pkg.packageName);
        }
        ListView listView = (ListView)findViewById(android.R.id.list);
        mAdapter = new AppListAdapter(list);
        ListView lv= (ListView) findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        lv.setSaveEnabled(true);
        lv.setItemsCanFocus(true);
        listView.setAdapter(mAdapter);
    }

    private  void getAppsUsingPerm(PermissionInfo pInfo) {
        List<PackageInfo> list = mPm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
        HashSet<PackageInfo> set = new HashSet<PackageInfo>();
        for (PackageInfo pkg : list) {
            if (pkg.requestedPermissions == null) {
                continue;
            }
            for (String perm : pkg.requestedPermissions) {
                if (perm.equalsIgnoreCase(pInfo.name)) {
                    Log.i(TAG, "Pkg:" + pkg.packageName+" uses permission");
                    set.add(pkg);
                    break;
                }
            }
        }
        ArrayList<PackageInfo> retList = new ArrayList<PackageInfo>();
        for (PackageInfo pkg : set) {
            retList.add(pkg);
        }
        Message msg = mHandler.obtainMessage(HANDLER_MSG_GET_APPS);
        Bundle data = msg.getData();
        data.putParcelableArrayList(KEY_APPS_USING_PERM, retList);
        mHandler.dispatchMessage(msg);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.permission_details);
        Intent intent = getIntent();
        String permName = intent.getStringExtra("permission");
        if(permName == null) {
            showDialogInner(DLG_ERROR);
        }
        mPm = getPackageManager();
        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        PermissionInfo pInfo = null;
        try {
            pInfo = mPm.getPermissionInfo(permName,
                PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            showDialogInner(DLG_ERROR);
        }
        setTextView(R.id.perm_name, pInfo.name);
        setTextView(R.id.perm_desc, pInfo.descriptionRes);
        setTextView(R.id.perm_group, pInfo.group);
        setProtectionLevel(R.id.perm_protection, pInfo.protectionLevel);
        setTextView(R.id.perm_source, pInfo.packageName);
        ApplicationInfo appInfo = null;
        try {
            appInfo =  mPm.getApplicationInfo(pInfo.packageName, 0);
            String uidStr = mPm.getNameForUid(appInfo.uid);
            setTextView(R.id.source_uid, uidStr);
        } catch (NameNotFoundException e) {
        }
        boolean sharedVisibility = false;
        // List of apps acquiring access via shared user id
        LinearLayout sharedPanel = (LinearLayout) findViewById(R.id.shared_pkgs_panel);
        if (appInfo != null) {
            String[] sharedList = mPm.getPackagesForUid(appInfo.uid);
            if ((sharedList != null) && (sharedList.length > 1)) {
                sharedVisibility = true;
                TextView label = (TextView) sharedPanel.findViewById(R.id.shared_pkgs_label);
                TextView sharedView = (TextView) sharedPanel.findViewById(R.id.shared_pkgs);
                label.setVisibility(View.VISIBLE);
                StringBuilder buff = new StringBuilder();
                buff.append(sharedList[0]);
                for (int i = 1; i < sharedList.length; i++) {
                    buff.append(", ");
                    buff.append(sharedList[i]);
                }
                sharedView.setText(buff.toString());
            }
        }
        if (sharedVisibility) {
            sharedPanel.setVisibility(View.VISIBLE);
        } else {
            sharedPanel.setVisibility(View.GONE);
        }
        getAppsUsingPerm(pInfo);
    }

    private void setProtectionLevel(int viewId, int protectionLevel) {
        String levelStr = "";
        if (protectionLevel == PermissionInfo.PROTECTION_NORMAL) {
            levelStr = PROTECTION_NORMAL;
        } else if (protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
            levelStr = PROTECTION_DANGEROUS;
        } else if (protectionLevel == PermissionInfo.PROTECTION_SIGNATURE) {
            levelStr = PROTECTION_SIGNATURE;
        } else if (protectionLevel == PermissionInfo.PROTECTION_SIGNATURE_OR_SYSTEM) {
            levelStr = PROTECTION_SIGNATURE_OR_SYSTEM;
        } else {
            levelStr = "Invalid";
        }
        setTextView(viewId, levelStr);
    }

    private void setTextView(int viewId, int textId) {
        TextView view = (TextView)findViewById(viewId);
        view.setText(textId);
    }

    private void setTextView(int viewId, String text) {
        TextView view = (TextView)findViewById(viewId);
        view.setText(text);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DLG_ERROR) {
            return new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_error)
            .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }})
            .setMessage(R.string.invalid_perm_name)
            .setOnCancelListener(this)
            .create();
        }
        return null;
    }

    private void showDialogInner(int id) {
        // TODO better fix for this? Remove dialog so that it gets created again
        removeDialog(id);
        showDialog(id);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        // TODO Launch app details activity
    }
}
