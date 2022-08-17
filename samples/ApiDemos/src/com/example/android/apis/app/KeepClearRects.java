/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.example.android.apis.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.example.android.apis.R;

import java.util.Arrays;
import java.util.List;

public class KeepClearRects extends Activity {
    private static final String EXTRA_SET_PREFER_KEEP_CLEAR = "prefer_keep_clear";
    private static final String EXTRA_SET_PREFER_KEEP_CLEAR_RECTS = "prefer_keep_clear_rects";

    private RelativeLayout mRootView;
    private View mKeepClearView;
    private Switch mViewAsRestrictedKeepClearAreaToggle;
    private Switch mBottomRightCornerKeepClearAreaToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.keep_clear_rects_activity);

        // Find views
        mRootView = findViewById(R.id.container);
        mKeepClearView = findViewById(R.id.keep_clear_view_bottom_right);
        mViewAsRestrictedKeepClearAreaToggle = findViewById(R.id.set_prefer_keep_clear_toggle);
        mBottomRightCornerKeepClearAreaToggle = findViewById(
                R.id.set_bottom_right_rectangle_keep_clear_toggle);

        // Add listeners
        mViewAsRestrictedKeepClearAreaToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mBottomRightCornerKeepClearAreaToggle.setOnCheckedChangeListener(
                mBottomRightCornerToggleChangedListener);

        // Get defaults
        final Intent intent = getIntent();
        mViewAsRestrictedKeepClearAreaToggle.setChecked(
                intent.getBooleanExtra(EXTRA_SET_PREFER_KEEP_CLEAR, false));
        mBottomRightCornerKeepClearAreaToggle.setChecked(
                intent.getBooleanExtra(EXTRA_SET_PREFER_KEEP_CLEAR_RECTS, false));
    }

    private final CompoundButton.OnCheckedChangeListener mOnToggleChangedListener =
            (v, isChecked) -> mKeepClearView.setPreferKeepClear(isChecked);

    private final CompoundButton.OnCheckedChangeListener mBottomRightCornerToggleChangedListener =
            (v, isChecked) -> {
                if (isChecked) {
                    mRootView.setPreferKeepClearRects(
                            Arrays.asList(new Rect(
                                    mKeepClearView.getLeft(),
                                    mKeepClearView.getTop(),
                                    mKeepClearView.getRight(),
                                    mKeepClearView.getBottom())));
                } else {
                    mRootView.setPreferKeepClearRects(List.of());
                }
            };
}
