/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.corpapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Arrays;
import java.util.List;

/**
 * A minimal Globl Proxy-setting corp app  application.
 */
public class CorpAppActivity extends Activity {
    /**
     * Called with the activity is first created.
     */

    Button mSetButton;
    TextView mStatusText;
    String mProxyName;
    String mProxyExclList;
    String mSuccess;
    String mFailure;

    DevicePolicyManager mDPM;
    ActivityManager mAM;
    ComponentName mCorpDeviceAdmin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAM = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        mCorpDeviceAdmin = new ComponentName(CorpAppActivity.this, CorpDeviceAdmin.class);

        // Set the layout for this activity.  You can find it
        // in res/layout/corp_app_activity.xml
        setContentView(R.layout.corp_app_activity);

        mSetButton = (Button)findViewById(R.id.set_button);
        mSetButton.setOnClickListener(mSetListener);
        mStatusText = (Button)findViewById(R.id.status_text);

        boolean active = mDPM.isAdminActive(mCorpDeviceAdmin);
        mSetButton.setEnabled(active);
        mProxyName = getResources().getString(R.string.corp_app_proxy_name);
        mProxyExclList = getResources().getString(R.string.corp_app_proxy_excl_list);
        mSuccess = getResources().getString(R.string.corp_app_status_success_text);
        mFailure = getResources().getString(R.string.corp_app_status_failed_text);
    }
    
    private OnClickListener mSetListener = new OnClickListener() {
        public void onClick(View v) {
            String[] proxyComponents = mProxyName.split(":");
            if (proxyComponents.length != 2) {
                Toast.makeText(CorpAppActivity.this, "Wrong proxy specification.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            Proxy instProxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyComponents[0],
                            Integer.parseInt(proxyComponents[1])));
            String[] listDoms = mProxyExclList.split(",");
            if (listDoms.length == 0) {
                Toast.makeText(CorpAppActivity.this, "Wrong exclusion list format.",
                        Toast.LENGTH_SHORT).show();
            }
            List<String> exclList =  Arrays.asList(listDoms);
            boolean active = mDPM.isAdminActive(mCorpDeviceAdmin);
            if (active) {
                mDPM.setGlobalProxy(mCorpDeviceAdmin, instProxy, exclList);
                ComponentName proxyAdmin = mDPM.getGlobalProxyAdmin();
                if ((proxyAdmin != null) && (proxyAdmin.equals(mCorpDeviceAdmin))) {
                    Toast.makeText(CorpAppActivity.this, "Global Proxy set by device admin.",
                            Toast.LENGTH_SHORT).show();
                    mStatusText.setText(mSuccess);
                } else {
                    Toast.makeText(CorpAppActivity.this, "Failed to set Global Proxy.",
                            Toast.LENGTH_SHORT).show();
                    mStatusText.setText(mFailure);
                }
            }
        }
    };

}

