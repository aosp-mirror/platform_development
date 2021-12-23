/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;

import com.example.android.apis.R;

public class PictureInPicture extends Activity {
    private static final String EXTRA_ENABLE_AUTO_PIP = "auto_pip";
    private static final String EXTRA_ENABLE_SOURCE_RECT_HINT = "source_rect_hint";
    private static final String EXTRA_ENABLE_SEAMLESS_RESIZE = "seamless_resize";
    private static final String EXTRA_CURRENT_POSITION = "current_position";

    private final View.OnLayoutChangeListener mOnLayoutChangeListener =
            (v, oldLeft, oldTop, oldRight, oldBottom, newLeft, newTop, newRight, newBottom) -> {
                updatePictureInPictureParams();
            };

    private final CompoundButton.OnCheckedChangeListener mOnToggleChangedListener =
            (v, isChecked) -> updatePictureInPictureParams();

    private final RadioGroup.OnCheckedChangeListener mOnPositionChangedListener =
            (v, id) -> updateContentPosition(id);

    private LinearLayout mContainer;
    private View mImageView;
    private View mControlGroup;
    private Switch mAutoPipToggle;
    private Switch mSourceRectHintToggle;
    private Switch mSeamlessResizeToggle;
    private RadioGroup mCurrentPositionGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_in_picture);

        // Find views
        mContainer = findViewById(R.id.container);
        mImageView = findViewById(R.id.image);
        mControlGroup = findViewById(R.id.control_group);
        mAutoPipToggle = findViewById(R.id.auto_pip_toggle);
        mSourceRectHintToggle = findViewById(R.id.source_rect_hint_toggle);
        mSeamlessResizeToggle = findViewById(R.id.seamless_resize_toggle);
        mCurrentPositionGroup = findViewById(R.id.current_position);

        // Attach listeners
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        mAutoPipToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mSourceRectHintToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mSeamlessResizeToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mCurrentPositionGroup.setOnCheckedChangeListener(mOnPositionChangedListener);
        findViewById(R.id.enter_pip_button).setOnClickListener(v -> enterPictureInPictureMode());
        findViewById(R.id.enter_content_pip_button).setOnClickListener(v -> enterContentPip());

        // Set defaults
        final Intent intent = getIntent();
        mAutoPipToggle.setChecked(intent.getBooleanExtra(EXTRA_ENABLE_AUTO_PIP, false));
        mSourceRectHintToggle.setChecked(
                intent.getBooleanExtra(EXTRA_ENABLE_SOURCE_RECT_HINT, false));
        mSeamlessResizeToggle.setChecked(
                intent.getBooleanExtra(EXTRA_ENABLE_SEAMLESS_RESIZE, false));
        final int positionId = "end".equalsIgnoreCase(
                intent.getStringExtra(EXTRA_CURRENT_POSITION))
                ? R.id.radio_current_end
                : R.id.radio_current_start;
        mCurrentPositionGroup.check(positionId);

        updateLayout(getResources().getConfiguration());
    }

    @Override
    protected void onUserLeaveHint() {
        // Only used when auto PiP is disabled. This is to simulate the behavior that an app
        // supports regular PiP but not auto PiP.
        if (!mAutoPipToggle.isChecked()) {
            enterPictureInPictureMode();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfiguration) {
        super.onConfigurationChanged(newConfiguration);
        updateLayout(newConfiguration);
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
            Configuration newConfig) {
        if (!isInPictureInPictureMode) {
            // When it's about to exit PiP mode, always reset the mImageView position to start.
            // If position is previously set to end, this should demonstrate the exit
            // source rect hint behavior introduced in S.
            mCurrentPositionGroup.check(R.id.radio_current_start);
        }
    }

    private void enterContentPip() {
        // TBD
    }

    private void updateLayout(Configuration configuration) {
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        final boolean isTablet = configuration.screenWidthDp >= 800;
        final boolean isLandscape =
                (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE);
        final boolean isPictureInPicture = isInPictureInPictureMode();
        if (isPictureInPicture) {
            setupPictureInPictureLayout();
        } else if (isTablet && isLandscape) {
            setupTabletLandscapeLayout();
        } else if (isLandscape) {
            setupFullScreenLayout();
        } else {
            setupRegularLayout();
        }
    }

    private void setupPictureInPictureLayout() {
        mControlGroup.setVisibility(View.GONE);
        final LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        imageLp.gravity = Gravity.NO_GRAVITY;
        mImageView.setLayoutParams(imageLp);
    }

    private void setupTabletLandscapeLayout() {
        mControlGroup.setVisibility(View.VISIBLE);
        exitFullScreenMode();

        final LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        imageLp.gravity = Gravity.NO_GRAVITY;
        enterTwoPaneMode(imageLp);
    }

    private void setupFullScreenLayout() {
        mControlGroup.setVisibility(View.GONE);
        enterFullScreenMode();

        final LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        imageLp.gravity = Gravity.CENTER_HORIZONTAL;
        enterOnePaneMode(imageLp);
    }

    private void setupRegularLayout() {
        mControlGroup.setVisibility(View.VISIBLE);
        exitFullScreenMode();

        final LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        imageLp.gravity = Gravity.NO_GRAVITY;
        enterOnePaneMode(imageLp);
    }

    private void enterOnePaneMode(LinearLayout.LayoutParams imageLp) {
        mContainer.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout.LayoutParams controlLp =
                (LinearLayout.LayoutParams) mControlGroup.getLayoutParams();
        controlLp.width = LinearLayout.LayoutParams.MATCH_PARENT;
        controlLp.height = 0;
        controlLp.weight = 1;
        mControlGroup.setLayoutParams(controlLp);

        imageLp.weight = 0;
        mImageView.setLayoutParams(imageLp);
    }

    private void enterTwoPaneMode(LinearLayout.LayoutParams imageLp) {
        mContainer.setOrientation(LinearLayout.HORIZONTAL);

        final LinearLayout.LayoutParams controlLp =
                (LinearLayout.LayoutParams) mControlGroup.getLayoutParams();
        controlLp.width = 0;
        controlLp.height = LinearLayout.LayoutParams.MATCH_PARENT;
        controlLp.weight = 1;
        mControlGroup.setLayoutParams(controlLp);

        imageLp.width = 0;
        imageLp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        imageLp.weight = 1;
        mImageView.setLayoutParams(imageLp);
    }

    private void enterFullScreenMode() {
        // TODO(b/188001699) switch to use insets controller once the bug is fixed.
        final View decorView = getWindow().getDecorView();
        final int systemUiNavigationBarFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                | systemUiNavigationBarFlags);
    }

    private void exitFullScreenMode() {
        final View decorView = getWindow().getDecorView();
        final int systemUiNavigationBarFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility()
                & ~systemUiNavigationBarFlags);
    }

    private void updatePictureInPictureParams() {
        mImageView.removeOnLayoutChangeListener(mOnLayoutChangeListener);
        // do not bother PictureInPictureParams update when it's already in pip mode.
        if (isInPictureInPictureMode()) return;
        final Rect imageViewRect = new Rect();
        mImageView.getGlobalVisibleRect(imageViewRect);
        // bail early if mImageView has not been measured yet
        if (imageViewRect.isEmpty()) return;
        final PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder()
                .setAutoEnterEnabled(mAutoPipToggle.isChecked())
                .setSourceRectHint(mSourceRectHintToggle.isChecked()
                        ? new Rect(imageViewRect) : null)
                .setSeamlessResizeEnabled(mSeamlessResizeToggle.isChecked())
                .setAspectRatio(new Rational(imageViewRect.width(), imageViewRect.height()));
        setPictureInPictureParams(builder.build());
    }

    private void updateContentPosition(int checkedId) {
        mContainer.removeAllViews();
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        if (checkedId == R.id.radio_current_start) {
            mContainer.addView(mImageView, 0);
            mContainer.addView(mControlGroup, 1);
        } else {
            mContainer.addView(mControlGroup, 0);
            mContainer.addView(mImageView, 1);
        }
    }
}
