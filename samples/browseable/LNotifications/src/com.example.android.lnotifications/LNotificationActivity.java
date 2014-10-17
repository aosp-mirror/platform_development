/*
* Copyright 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.lnotifications;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * Launcher Activity for the L Notification samples application.
 */
public class LNotificationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        setTitle(R.string.title_lnotification_activity);
        ActionBar actionBar = getActionBar();

        // Use ViewPager in the support library where possible.
        // At this time, the support library for L is not ready so using the deprecated method
        // to create tabs.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ActionBar.Tab tabHeadsUpNotification = actionBar.newTab().setText("Heads Up");
        ActionBar.Tab tabVisibilityMetadata = actionBar.newTab().setText("Visibility");
        ActionBar.Tab tabOtherMetadata = actionBar.newTab().setText("Others");
        tabHeadsUpNotification.setTabListener(new FragmentTabListener(HeadsUpNotificationFragment
                .newInstance()));
        tabVisibilityMetadata.setTabListener(new FragmentTabListener(VisibilityMetadataFragment
                .newInstance()));
        tabOtherMetadata.setTabListener(new FragmentTabListener(OtherMetadataFragment.newInstance
                ()));
        actionBar.addTab(tabHeadsUpNotification, 0);
        actionBar.addTab(tabVisibilityMetadata, 1);
        actionBar.addTab(tabOtherMetadata, 2);
    }

    /**
     * TabListener that replaces a Fragment when a tab is clicked.
     */
    private static class FragmentTabListener implements ActionBar.TabListener {
        public Fragment fragment;

        public FragmentTabListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            //do nothing.
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.replace(R.id.container, fragment);
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            ft.remove(fragment);
        }
    }
}
