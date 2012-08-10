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
import android.app.ActionBar.Tab;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

/**
 * An implementation of the {@link CompatTab} interface that relies on API 11 APIs.
 */
public class CompatTabHoneycomb extends CompatTab implements ActionBar.TabListener {

    /**
     * The native tab object that this {@link CompatTab} acts as a proxy for.
     */
    ActionBar.Tab mTab;
    CompatTabListener mCallback;
    Fragment mFragment;

    protected CompatTabHoneycomb(FragmentActivity activity, String tag) {
        super(activity, tag);
        mTab = activity.getActionBar().newTab();
    }

    @Override
    public CompatTab setText(int resId) {
        mTab.setText(resId);
        return this;
    }

    @Override
    public CompatTab setIcon(int resId) {
        mTab.setIcon(resId);
        return this;
    }

    @Override
    public CompatTab setTabListener(CompatTabListener callback) {
        mCallback = callback;
        mTab.setTabListener(this);
        return this;
    }

    @Override
    public CharSequence getText() {
        return mTab.getText();
    }

    @Override
    public Drawable getIcon() {
        return mTab.getIcon();
    }

    @Override
    public Object getTab() {
        return mTab;
    }

    @Override
    public CompatTabListener getCallback() {
        return mCallback;
    }

    @Override
    public void onTabReselected(Tab tab, android.app.FragmentTransaction f) {
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.disallowAddToBackStack();
        mCallback.onTabReselected(this, ft);
        ft.commit();
    }

    @Override
    public void onTabSelected(Tab tab, android.app.FragmentTransaction f) {
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.disallowAddToBackStack();
        mCallback.onTabSelected(this, ft);
        ft.commit();
    }

    @Override
    public void onTabUnselected(Tab arg0, android.app.FragmentTransaction f) {
        FragmentTransaction ft = mActivity.getSupportFragmentManager().beginTransaction();
        ft.disallowAddToBackStack();
        mCallback.onTabUnselected(this, ft);
        ft.commit();
    }

    @Override
    public CompatTab setFragment(Fragment fragment) {
        mFragment = fragment;
        return this;
    }

    @Override
    public Fragment getFragment() {
        return mFragment;
    }
}
