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

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.app.PictureInPictureParams;
import android.app.RemoteAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.util.Rational;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.window.OnBackInvokedDispatcher;

import com.example.android.apis.R;
import com.example.android.apis.view.FixedAspectRatioImageView;

import java.util.ArrayList;
import java.util.List;

public class PictureInPicture extends Activity {
    private static final String EXTRA_ENABLE_AUTO_PIP = "auto_pip";
    private static final String EXTRA_ENABLE_SOURCE_RECT_HINT = "source_rect_hint";
    private static final String EXTRA_ENABLE_SEAMLESS_RESIZE = "seamless_resize";
    private static final String EXTRA_ENTER_PIP_ON_BACK = "enter_pip_on_back";
    private static final String EXTRA_CURRENT_POSITION = "current_position";
    private static final String EXTRA_ASPECT_RATIO = "aspect_ratio";

    private static final int TABLET_BREAK_POINT_DP = 700;

    private static final String ACTION_CUSTOM_CLOSE = "demo.pip.custom_close";
    private final BroadcastReceiver mRemoteActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_CUSTOM_CLOSE:
                    finish();
                    break;
            }
        }
    };

    public static final String KEY_ON_STOP_RECEIVER = "on_stop_receiver";
    private final ResultReceiver mOnStopReceiver = new ResultReceiver(
            new Handler(Looper.myLooper())) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            // Container activity for content-pip has stopped, replace the placeholder
            // with actual content in this host activity.
            mImageView.setImageResource(R.drawable.sample_1);
        }
    };

    private final View.OnLayoutChangeListener mOnLayoutChangeListener =
            (v, oldLeft, oldTop, oldRight, oldBottom, newLeft, newTop, newRight, newBottom) -> {
                updatePictureInPictureParams();
            };

    private final CompoundButton.OnCheckedChangeListener mOnToggleChangedListener =
            (v, isChecked) -> updatePictureInPictureParams();

    private final RadioGroup.OnCheckedChangeListener mOnPositionChangedListener =
            (v, id) -> updateContentPosition(id);

    private LinearLayout mContainer;
    private FixedAspectRatioImageView mImageView;
    private View mControlGroup;
    private Switch mAutoPipToggle;
    private Switch mSourceRectHintToggle;
    private Switch mSeamlessResizeToggle;
    private Switch mEnterPipOnBackToggle;
    private RadioGroup mCurrentPositionGroup;
    private Spinner mAspectRatioSpinner;
    private List<RemoteAction> mPipActions;
    private RemoteAction mCloseAction;

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
        mEnterPipOnBackToggle = findViewById(R.id.enter_pip_on_back);
        mCurrentPositionGroup = findViewById(R.id.current_position);
        mAspectRatioSpinner = findViewById(R.id.aspect_ratio);

        // Initiate views if applicable
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.aspect_ratio_list, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mAspectRatioSpinner.setAdapter(adapter);

        // Attach listeners
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        mAutoPipToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mSourceRectHintToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mSeamlessResizeToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        mEnterPipOnBackToggle.setOnCheckedChangeListener(mOnToggleChangedListener);
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
                    if (mEnterPipOnBackToggle.isChecked()) {
                        enterPictureInPictureMode();
                    } else {
                        finish();
                    }
                });
        mCurrentPositionGroup.setOnCheckedChangeListener(mOnPositionChangedListener);
        mAspectRatioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String rawText = parent.getItemAtPosition(position).toString();
                final String textToParse = rawText.substring(
                        rawText.indexOf('(') + 1,
                        rawText.indexOf(')'));
                mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
                mImageView.setAspectRatio(Rational.parseRational(textToParse));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing.
            }
        });
        findViewById(R.id.enter_pip_button).setOnClickListener(v -> enterPictureInPictureMode());
        findViewById(R.id.enter_content_pip_button).setOnClickListener(v -> enterContentPip());

        // Set defaults
        final Intent intent = getIntent();
        mAutoPipToggle.setChecked(intent.getBooleanExtra(EXTRA_ENABLE_AUTO_PIP, false));
        mSourceRectHintToggle.setChecked(
                intent.getBooleanExtra(EXTRA_ENABLE_SOURCE_RECT_HINT, false));
        mSeamlessResizeToggle.setChecked(
                intent.getBooleanExtra(EXTRA_ENABLE_SEAMLESS_RESIZE, false));
        mEnterPipOnBackToggle.setChecked(
                intent.getBooleanExtra(EXTRA_ENTER_PIP_ON_BACK, false));
        final int positionId = "end".equalsIgnoreCase(
                intent.getStringExtra(EXTRA_CURRENT_POSITION))
                ? R.id.radio_current_end
                : R.id.radio_current_start;
        mCurrentPositionGroup.check(positionId);
        mAspectRatioSpinner.setSelection(1);

        updateLayout(getResources().getConfiguration());
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupPipActions();
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

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRemoteActionReceiver);
    }

    /**
     * This is what we expect most host Activity would do to trigger content PiP.
     * - Get the bounds of the view to be transferred to content PiP
     * - Construct the PictureInPictureParams with source rect hint and aspect ratio from bounds
     * - Start the new content PiP container Activity with the ActivityOptions
     */
    private void enterContentPip() {
        final Intent intent = new Intent(this, ContentPictureInPicture.class);
        intent.putExtra(KEY_ON_STOP_RECEIVER, mOnStopReceiver);
        final Rect bounds = new Rect();
        mImageView.getGlobalVisibleRect(bounds);
        final PictureInPictureParams params = new PictureInPictureParams.Builder()
                .setSourceRectHint(bounds)
                .setAspectRatio(new Rational(bounds.width(), bounds.height()))
                .build();
        final ActivityOptions opts = ActivityOptions.makeLaunchIntoPip(params);
        startActivity(intent, opts.toBundle());
        // Swap the mImageView to placeholder content.
        mImageView.setImageResource(R.drawable.black_box);
    }

    private void updateLayout(Configuration configuration) {
        mImageView.addOnLayoutChangeListener(mOnLayoutChangeListener);
        final boolean isTablet = configuration.smallestScreenWidthDp >= TABLET_BREAK_POINT_DP;
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

    private void setupPipActions() {
        final IntentFilter remoteActionFilter = new IntentFilter();
        remoteActionFilter.addAction(ACTION_CUSTOM_CLOSE);
        registerReceiver(mRemoteActionReceiver, remoteActionFilter);
        final Intent intent = new Intent(ACTION_CUSTOM_CLOSE).setPackage(getPackageName());
        mCloseAction = new RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_call_end),
                getString(R.string.action_custom_close),
                getString(R.string.action_custom_close),
                PendingIntent.getBroadcast(this, 0 /* requestCode */, intent,
                        FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE));

        // Add close action as a regular PiP action
        mPipActions = new ArrayList<>(1);
        mPipActions.add(mCloseAction);
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
                .setAspectRatio(new Rational(imageViewRect.width(), imageViewRect.height()))
                .setActions(mPipActions)
                .setCloseAction(mCloseAction);
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
