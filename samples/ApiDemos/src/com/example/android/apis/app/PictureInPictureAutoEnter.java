/*
 * Copyright (C) 2021 The Android Open Source Project
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
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.example.android.apis.R;

public class PictureInPictureAutoEnter extends Activity {
    private final View.OnLayoutChangeListener mOnLayoutChangeListener =
            (v, oldLeft, oldTop, oldRight, oldBottom, newLeft, newTop, newRight, newBottom) -> {
                updatePictureInPictureParams();
            };

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener =
            (v, isChecked) -> updatePictureInPictureParams();

    private View mImageView;
    private View mButtonView;
    private Switch mSwitchView;
    private int mLastOrientation = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_in_picture_auto_enter);

        mImageView = findViewById(R.id.image);
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        mButtonView = findViewById(R.id.change_orientation);
        mButtonView.setOnClickListener((v) -> {
            final int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        mSwitchView = findViewById(R.id.source_rect_hint_toggle);
        mSwitchView.setOnCheckedChangeListener(mOnCheckedChangeListener);

        // there is a bug that setSourceRectHint(null) does not clear the source rect hint
        // once there is a non-null source rect hint ever been set. set this to false by default
        // therefore this demo activity can be used for testing autoEnterPip behavior without
        // source rect hint when launched for the first time.
        mSwitchView.setChecked(false);

        updateLayout(getResources().getConfiguration());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
        if (!isInPictureInPictureMode()) {
            updateLayout(newConfiguration);
        }
    }

    private void updateLayout(Configuration configuration) {
        if (configuration.orientation == mLastOrientation) return;
        mLastOrientation = configuration.orientation;
        final boolean isLandscape = (mLastOrientation == Configuration.ORIENTATION_LANDSCAPE);
        mButtonView.setVisibility(isLandscape ? View.GONE : View.VISIBLE);
        mSwitchView.setVisibility(isLandscape ? View.GONE: View.VISIBLE);
        final LinearLayout.LayoutParams layoutParams;
        // Toggle the fullscreen mode as well.
        // TODO(b/188001699) switch to use insets controller once the bug is fixed.
        final View decorView = getWindow().getDecorView();
        final int systemUiNavigationBarFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (isLandscape) {
            layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                    | systemUiNavigationBarFlags);
        } else {
            layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                    & ~systemUiNavigationBarFlags);
        }
        mImageView.setLayoutParams(layoutParams);
    }

    private void updatePictureInPictureParams() {
        // do not bother PictureInPictureParams update when it's already in pip mode.
        if (isInPictureInPictureMode()) return;
        final Rect imageViewRect = new Rect();
        mImageView.getGlobalVisibleRect(imageViewRect);
        // bail early if mImageView has not been measured yet
        if (imageViewRect.isEmpty()) return;
        final Rect sourceRectHint = mSwitchView.isChecked() ? new Rect(imageViewRect) : null;
        final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAutoEnterEnabled(true)
                .setAspectRatio(new Rational(imageViewRect.width(), imageViewRect.height()))
                .setSourceRectHint(sourceRectHint);
        setPictureInPictureParams(builder.build());
    }
}
