/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pm.shortcutlauncherdemo;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

public class ShortcutLauncherMain extends Activity {
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        final ActionBar ab = getActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ab.addTab(ab.newTab().setText("App list").setTabListener(mTabListener));
        ab.addTab(ab.newTab().setText("Pinned shortcuts").setTabListener(mTabListener));
    }

    @Override
    public void onBackPressed() {
        // Ignore.
    }

    private TabListener mTabListener = new TabListener() {
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {

        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
    };

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new AppListFragment();
                case 1:
                    return new ShortcutListFragment().setArguments(
                            /* targetPackage =*/ null,
                            /* targetActivity =*/ null,
                            /* includeDynamic = */ false,
                            /* includeManifest = */ false,
                            /* includePinned =*/ true,
                            null /* means "all profiles" of this user*/,
                            /* showDetails =*/ true
                            );
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);

            getActionBar().selectTab(getActionBar().getTabAt(position));
        }
    }
}
