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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ProcessInfo extends Activity {
    PackageManager mPm;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Intent intent = getIntent();
        String processName = intent.getStringExtra("processName");
        String pkgList[] = intent.getStringArrayExtra("packageList");
        mPm = getPackageManager();
        setContentView(R.layout.process_info);
       TextView processNameView = (TextView) findViewById(R.id.process_name);
       LinearLayout pkgListView = (LinearLayout) findViewById(R.id.package_list);
       if(processName != null) {
           processNameView.setText(processName);
       }
       if(pkgList != null) {
           for(String pkg : pkgList) {
               TextView pkgView = new TextView(this);
               pkgView.setText(pkg);
               pkgListView.addView(pkgView);
           }
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
}

