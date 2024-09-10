/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.apis.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.android.apis.R;

import java.util.ArrayList;
import java.util.Arrays;

public class ViewFrameRateActivity extends Activity {
    private float mFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE;
    private long mLastFrameNanos = 0L;
    private int mLastRenderRate = 0;
    private final int[] mFrameRates = new int[] { 120, 90, 80, 60, 48, 30, 24, 15, 12, 8, 4, 2, 1 };
    private final long[] mFrameRateCutoffs = new long[mFrameRates.length - 1];

    private ProgressBar mProgressBar;
    private View mAnimatedView;
    private Animator mAnimator;

    public ViewFrameRateActivity() {
        for (int i = 0; i < mFrameRateCutoffs.length; i++) {
            long low = 1_000_000_000L / mFrameRates[i];
            long high = 1_000_000_000L / mFrameRates[i + 1];
            mFrameRateCutoffs[i] = (high * 3 + low) / 4; // 3/4 of the way to high is the cut-off
        }
    }

    private void updateFrameRate(float newFrameRate) {
        if (mFrameRate != newFrameRate) {
            mFrameRate = newFrameRate;
            mProgressBar.setRequestedFrameRate(newFrameRate);
            mAnimatedView.setRequestedFrameRate(newFrameRate);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_frame_rate);
        final SeekBar frameRateSeekBar = findViewById(R.id.frameRateSeekBar);
        assert frameRateSeekBar != null;
        frameRateSeekBar.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        frameRateSeekBar.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            Rect rect = new Rect(0, 0, v.getWidth(), v.getHeight());
            ArrayList<Rect> list = new ArrayList<>();
            list.add(rect);
            frameRateSeekBar.setSystemGestureExclusionRects(list);
        });
        final TextView frameRateText = findViewById(R.id.frameRateText);
        assert frameRateText != null;
        frameRateText.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        mProgressBar = findViewById(R.id.progressBar);
        assert mProgressBar != null;
        mProgressBar.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        mAnimatedView = findViewById(R.id.animatedView);
        assert mAnimatedView != null;
        mAnimatedView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);

        final RadioButton noPreference = findViewById(R.id.noPreference);
        assert noPreference != null;
        noPreference.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        noPreference.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                frameRateText.setText("No Pref");
                updateFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
            }
        });
        final RadioButton low = findViewById(R.id.low);
        assert low != null;
        low.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        low.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                frameRateText.setText("Low");
                updateFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_LOW);
            }
        });
        final RadioButton normal = findViewById(R.id.normal);
        assert normal != null;
        normal.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        normal.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                frameRateText.setText("Normal");
                updateFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NORMAL);
            }
        });
        final RadioButton high = findViewById(R.id.high);
        assert high != null;
        high.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);
        high.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                frameRateText.setText("High");
                updateFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH);
            }
        });
        frameRateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                frameRateText.setText(String.valueOf(seekBar.getProgress()));
                updateFrameRate(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                frameRateText.setText(String.valueOf(seekBar.getProgress()));
                noPreference.setChecked(false);
                low.setChecked(false);
                normal.setChecked(false);
                high.setChecked(false);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        final TextView frameRateOutput = findViewById(R.id.frameRate);
        assert frameRateOutput != null;
        frameRateOutput.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE);

        final RadioButton vectorDrawable = findViewById(R.id.vectorDrawable);
        assert vectorDrawable != null;
        vectorDrawable.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                mProgressBar.setVisibility(View.VISIBLE);
                mAnimatedView.setVisibility(View.GONE);
                mAnimator.pause();
                frameRateOutput.setVisibility(View.GONE);
            }
        });
        final RadioButton view = findViewById(R.id.view);
        assert view != null;
        view.setOnCheckedChangeListener((v, isChecked) -> {
            if (isChecked) {
                mProgressBar.setVisibility(View.GONE);
                mAnimatedView.setVisibility(View.VISIBLE);
                mAnimator.resume();
                frameRateOutput.setVisibility(View.VISIBLE);
            }
        });
        mProgressBar.getViewTreeObserver().addOnDrawListener(() -> {
            long now = System.nanoTime();
            long delta = now - mLastFrameNanos;
            if (mLastFrameNanos > 0L && delta > 0L) {
                int searchIndex = Arrays.binarySearch(mFrameRateCutoffs, delta);
                int frameRateIndex = searchIndex >= 0
                        ? searchIndex
                        : Math.min(mFrameRates.length - 1, ~searchIndex);
                int newRenderRate = mFrameRates[frameRateIndex];
                if (newRenderRate != mLastRenderRate) {
                    mLastRenderRate = newRenderRate;
                    frameRateOutput.setText(String.valueOf(mLastRenderRate));
                }
            }
            mLastFrameNanos = now;
        });
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(mAnimatedView, View.SCALE_X, 0.25f, 0.75f);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(mAnimatedView, View.SCALE_Y, 0.25f, 0.75f);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        AnimatorSet set = new AnimatorSet();
        set.setDuration(1000);
        set.playTogether(scaleX, scaleY);
        mAnimator = set;
        set.start();
        set.pause();
    }
}
