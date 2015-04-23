/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.apprestrictionenforcer;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity implements StatusFragment.StatusUpdatedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_real);
        if (null == savedInstanceState) {
            DevicePolicyManager devicePolicyManager =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            PackageManager packageManager = getPackageManager();
            if (!devicePolicyManager.isProfileOwnerApp(getApplicationContext().getPackageName())) {
                // If the managed profile is not yet set up, we show the setup screen.
                showSetupProfile();
            } else {
                try {
                    ApplicationInfo info = packageManager.getApplicationInfo(
                            Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA,
                            PackageManager.GET_UNINSTALLED_PACKAGES);
                    if (0 == (info.flags & ApplicationInfo.FLAG_INSTALLED)) {
                        // Need to reinstall the sample app
                        showStatusProfile();
                    } else if (devicePolicyManager.isApplicationHidden(
                            EnforcerDeviceAdminReceiver.getComponentName(this),
                            Constants.PACKAGE_NAME_APP_RESTRICTION_SCHEMA)) {
                        // The app is installed but hidden in this profile
                        showStatusProfile();
                    } else {
                        // Everything is clear; show the main screen
                        showMainFragment();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    showStatusProfile();
                }
            }
        }
    }

    private void showSetupProfile() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SetupProfileFragment())
                .commit();
    }

    private void showStatusProfile() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new StatusFragment())
                .commit();
    }

    private void showMainFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new AppRestrictionEnforcerFragment())
                .commit();
    }

    @Override
    public void onStatusUpdated() {
        showMainFragment();
    }

}
