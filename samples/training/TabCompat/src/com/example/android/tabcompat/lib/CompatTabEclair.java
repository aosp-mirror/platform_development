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

import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

/**
 * A base implementation of the {@link CompatTab} interface.
 */
public class CompatTabEclair extends CompatTab {
    private CompatTabListener mCallback;
    private CharSequence mText;
    private Drawable mIcon;
    private Fragment mFragment;

    protected CompatTabEclair(FragmentActivity activity, String tag) {
        super(activity, tag);
    }

    @Override
    public CompatTab setText(int resId) {
        mText = mActivity.getResources().getText(resId);
        return this;
    }

    @Override
    public CompatTab setIcon(int resId) {
        mIcon = mActivity.getResources().getDrawable(resId);
        return this;
    }

    @Override
    public CompatTab setTabListener(CompatTabListener callback) {
        mCallback = callback;
        return this;
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

    @Override
    public CharSequence getText() {
        return mText;
    }

    @Override
    public Drawable getIcon() {
        return mIcon;
    }

    @Override
    public Object getTab() {
        return null;
    }

    @Override
    public CompatTabListener getCallback() {
        return mCallback;
    }
}
