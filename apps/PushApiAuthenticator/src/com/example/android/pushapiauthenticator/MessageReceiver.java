/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.pushapiauthenticator;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MessageReceiver extends BroadcastReceiver {

    private static final String TAG = "PushApiAuthenticator";

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction()
                .equals("android.accounts.action.ACCOUNTS_LISTENER_PACKAGE_INSTALLED")) {
            String newPackage = intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
            Log.i(TAG, "new app is installed " + newPackage);
            Toast.makeText(context, "new app is installed" + newPackage, Toast.LENGTH_LONG).show();
        }

    }
}
