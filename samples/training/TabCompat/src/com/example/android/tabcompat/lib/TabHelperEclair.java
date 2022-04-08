/*
 * Copyright 2012 The Android Open Source Project
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

package com.example.android.tabcompat.lib;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import java.util.HashMap;

/**
 * This is a helper class to build tabs on pre-Honeycomb. Call {@link
 * TabCompatActivity#getTabHelper()} to get the generic instance for
 * compatibility with other versions.
 *
 * It implements a generic mechanism for associating fragments with the tabs in a tab host.  It
 * relies on a trick:  Normally a tab host has a simple API for supplying a View or Intent that each
 * tab will show.  This is not sufficient for switching between fragments.  So instead we make the
 * content part of the tab host 0dp high (it is not shown) and this supplies its own dummy view to
 * show as the tab content.  It listens to changes in tabs, then passes the event back to the tab's
 * callback interface so the activity can take care of switching to the correct fragment.
 */
public class TabHelperEclair extends TabHelper implements TabHost.OnTabChangeListener {

    private final HashMap<String, CompatTab> mTabs = new HashMap<String, CompatTab>();
    private TabHost mTabHost;
    CompatTabListener mCallback;
    CompatTab mLastTab;

    protected TabHelperEclair(FragmentActivity activity) {
        super(activity);
        mActivity = activity;
    }

    @Override
    protected void setUp() {
        if (mTabHost == null) {
            mTabHost = (TabHost) mActivity.findViewById(android.R.id.tabhost);
            mTabHost.setup();
            mTabHost.setOnTabChangedListener(this);
        }
    }

    @Override
    public void addTab(CompatTab tab) {
        String tag = tab.getTag();
        TabSpec spec;

        if (tab.getIcon() != null) {
            spec = mTabHost.newTabSpec(tag).setIndicator(tab.getText(), tab.getIcon());
        } else {
            spec = mTabHost.newTabSpec(tag).setIndicator(tab.getText());
        }

        spec.setContent(new DummyTabFactory(mActivity));

        // Check to see if we already have a fragment for this tab, probably
        // from a previously saved state.  If so, deactivate it, because our
        // initial state is that a tab isn't shown.

        Fragment fragment = mActivity.getSupportFragmentManager().findFragmentByTag(tag);
        tab.setFragment(fragment);

        if (fragment != null && !fragment.isDetached()) {
            FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
            ft.detach(fragment);
            ft.commit();
        }

        mTabs.put(tag, tab);
        mTabHost.addTab(spec);
    }

    /**
     * Converts the basic "tab changed" event for TabWidget into the three possible events for
     * CompatTabListener: selected, unselected, reselected.
     */
    @Override
    public void onTabChanged(String tabId) {
        CompatTab newTab = mTabs.get(tabId);
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();

        if (mLastTab != newTab) {
            if (mLastTab != null) {
                if (mLastTab.getFragment() != null) {
                    // Pass the unselected event back to the tab's CompatTabListener
                    mLastTab.getCallback().onTabUnselected(mLastTab, ft);
                }
            }
            if (newTab != null) {
                // Pass the selected event back to the tab's CompatTabListener
                newTab.getCallback().onTabSelected(newTab, ft);
            }

            mLastTab = newTab;
        } else {
            // Pass the re-selected event back to the tab's CompatTabListener
            newTab.getCallback().onTabReselected(newTab, ft);
        }

        ft.commit();
        mActivity.getSupportFragmentManager().executePendingTransactions();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save and restore the selected tab for rotations/restarts.
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
    }

    /**
     * Backwards-compatibility mumbo jumbo
     */
    static class DummyTabFactory implements TabHost.TabContentFactory {

        private final Context mContext;

        public DummyTabFactory(Context context) {
            mContext = context;
        }

        @Override
        public View createTabContent(String tag) {
            View v = new View(mContext);
            v.setMinimumWidth(0);
            v.setMinimumHeight(0);
            return v;
        }
    }
}
