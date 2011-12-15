/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.training.deviceadmin;

import android.app.Activity;
import android.content.Intent;

public class SecureActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        // Check to see if the device is properly secured as per the policy.  Send user
        // back to policy set up screen if necessary.
        Policy policy = new Policy(this);
        policy.readFromLocal();
        if (!policy.isDeviceSecured()) {
            Intent intent = new Intent();
            intent.setClass(this, PolicySetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_secure);
    }
}

