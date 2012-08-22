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

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

/**
 * Helper class to build tabs on Honeycomb. Call {@link TabCompatActivity#getTabHelper()}
 * to get the generic instance for compatibility with older versions.
 */
public class TabHelperHoneycomb extends TabHelper {

    ActionBar mActionBar;

    protected TabHelperHoneycomb(FragmentActivity activity) {
        super(activity);
    }

    @Override
    protected void setUp() {
        if (mActionBar == null) {
            mActionBar = mActivity.getActionBar();
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }
    }

    @Override
    public void addTab(CompatTab tab) {
        String tag = tab.getTag();

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

        if (tab.getCallback() == null) {
            throw new IllegalStateException("CompatTab must have a CompatTabListener");
        }

        // We know tab is a CompatTabHoneycomb instance, so its
        // native tab object is an ActionBar.Tab.
        mActionBar.addTab((ActionBar.Tab) tab.getTab());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        int position = mActionBar.getSelectedTab().getPosition();
        outState.putInt("tab_position", position);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        int position = savedInstanceState.getInt("tab_position");
        mActionBar.setSelectedNavigationItem(position);
    }
}
