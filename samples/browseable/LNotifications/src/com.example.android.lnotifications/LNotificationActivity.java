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

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import static com.example.android.lnotifications.R.id.pager;

/**
 * Launcher Activity for the L Notification samples application.
 */
public class LNotificationActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Show 3 tabs with the different notification options.
        ViewPager viewPager = (ViewPager) findViewById(pager);
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);

        NotificationsPagerAdapter pagerAdapter =
                new NotificationsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        tabs.setupWithViewPager(viewPager);
    }

    private static class NotificationsPagerAdapter extends FragmentPagerAdapter {

        NotificationsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return HeadsUpNotificationFragment.newInstance();
                case 1:
                    return VisibilityMetadataFragment.newInstance();
                case 2:
                    return OtherMetadataFragment.newInstance();
                default:
                    break;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Heads Up";
                case 1:
                    return "Visibility";
                case 2:
                    return "Others";
                default:
                    return super.getPageTitle(position);
            }
        }
    }
}
