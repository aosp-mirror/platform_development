/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.watchface;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Wearable;

/**
 * The watch-side config activity for {@link DigitalWatchFaceService}, which allows for setting the
 * background color.
 */
public class DigitalWatchFaceWearableConfigActivity extends Activity implements
        WearableListView.ClickListener, WearableListView.OnScrollListener {
    private static final String TAG = "DigitalWatchFaceConfig";

    private GoogleApiClient mGoogleApiClient;
    private TextView mHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_config);

        mHeader = (TextView) findViewById(R.id.header);
        WearableListView listView = (WearableListView) findViewById(R.id.color_picker);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });

        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);

        String[] colors = getResources().getStringArray(R.array.color_array);
        listView.setAdapter(new ColorListAdapter(colors));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnected: " + connectionHint);
                        }
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionSuspended: " + cause);
                        }
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "onConnectionFailed: " + result);
                        }
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override // WearableListView.ClickListener
    public void onClick(WearableListView.ViewHolder viewHolder) {
        ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) viewHolder;
        updateConfigDataItem(colorItemViewHolder.mColorItem.getColor());
        finish();
    }

    @Override // WearableListView.ClickListener
    public void onTopEmptyRegionClick() {}

    @Override // WearableListView.OnScrollListener
    public void onScroll(int scroll) {}

    @Override // WearableListView.OnScrollListener
    public void onAbsoluteScrollChange(int scroll) {
        float newTranslation = Math.min(-scroll, 0);
        mHeader.setTranslationY(newTranslation);
    }

    @Override // WearableListView.OnScrollListener
    public void onScrollStateChanged(int scrollState) {}

    @Override // WearableListView.OnScrollListener
    public void onCentralPositionChanged(int centralPosition) {}

    private void updateConfigDataItem(final int backgroundColor) {
        DataMap configKeysToOverwrite = new DataMap();
        configKeysToOverwrite.putInt(DigitalWatchFaceUtil.KEY_BACKGROUND_COLOR,
                backgroundColor);
        DigitalWatchFaceUtil.overwriteKeysInConfigDataMap(mGoogleApiClient, configKeysToOverwrite);
    }

    private class ColorListAdapter extends WearableListView.Adapter {
        private final String[] mColors;

        public ColorListAdapter(String[] colors) {
            mColors = colors;
        }

        @Override
        public ColorItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ColorItemViewHolder(new ColorItem(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ColorItemViewHolder colorItemViewHolder = (ColorItemViewHolder) holder;
            String colorName = mColors[position];
            colorItemViewHolder.mColorItem.setColor(colorName);

            RecyclerView.LayoutParams layoutParams =
                    new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
            int colorPickerItemMargin = (int) getResources()
                    .getDimension(R.dimen.digital_config_color_picker_item_margin);
            // Add margins to first and last item to make it possible for user to tap on them.
            if (position == 0) {
                layoutParams.setMargins(0, colorPickerItemMargin, 0, 0);
            } else if (position == mColors.length - 1) {
                layoutParams.setMargins(0, 0, 0, colorPickerItemMargin);
            } else {
                layoutParams.setMargins(0, 0, 0, 0);
            }
            colorItemViewHolder.itemView.setLayoutParams(layoutParams);
        }

        @Override
        public int getItemCount() {
            return mColors.length;
        }
    }

    /** The layout of a color item including image and label. */
    private static class ColorItem extends LinearLayout implements
            WearableListView.OnCenterProximityListener {
        /** The duration of the expand/shrink animation. */
        private static final int ANIMATION_DURATION_MS = 150;
        /** The ratio for the size of a circle in shrink state. */
        private static final float SHRINK_CIRCLE_RATIO = .75f;

        private static final float SHRINK_LABEL_ALPHA = .5f;
        private static final float EXPAND_LABEL_ALPHA = 1f;

        private final TextView mLabel;
        private final CircledImageView mColor;

        private final float mExpandCircleRadius;
        private final float mShrinkCircleRadius;

        private final ObjectAnimator mExpandCircleAnimator;
        private final ObjectAnimator mExpandLabelAnimator;
        private final AnimatorSet mExpandAnimator;

        private final ObjectAnimator mShrinkCircleAnimator;
        private final ObjectAnimator mShrinkLabelAnimator;
        private final AnimatorSet mShrinkAnimator;

        public ColorItem(Context context) {
            super(context);
            View.inflate(context, R.layout.color_picker_item, this);

            mLabel = (TextView) findViewById(R.id.label);
            mColor = (CircledImageView) findViewById(R.id.color);

            mExpandCircleRadius = mColor.getCircleRadius();
            mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

            mShrinkCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius",
                    mExpandCircleRadius, mShrinkCircleRadius);
            mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
            mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mShrinkAnimator.playTogether(mShrinkCircleAnimator, mShrinkLabelAnimator);

            mExpandCircleAnimator = ObjectAnimator.ofFloat(mColor, "circleRadius",
                    mShrinkCircleRadius, mExpandCircleRadius);
            mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                    SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
            mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
            mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandLabelAnimator);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            if (animate) {
                mShrinkAnimator.cancel();
                if (!mExpandAnimator.isRunning()) {
                    mExpandCircleAnimator.setFloatValues(mColor.getCircleRadius(), mExpandCircleRadius);
                    mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                    mExpandAnimator.start();
                }
            } else {
                mExpandAnimator.cancel();
                mColor.setCircleRadius(mExpandCircleRadius);
                mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            }
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            if (animate) {
                mExpandAnimator.cancel();
                if (!mShrinkAnimator.isRunning()) {
                    mShrinkCircleAnimator.setFloatValues(mColor.getCircleRadius(), mShrinkCircleRadius);
                    mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                    mShrinkAnimator.start();
                }
            } else {
                mShrinkAnimator.cancel();
                mColor.setCircleRadius(mShrinkCircleRadius);
                mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            }
        }

        private void setColor(String colorName) {
            mLabel.setText(colorName);
            mColor.setCircleColor(Color.parseColor(colorName));
        }

        private int getColor() {
            return mColor.getDefaultCircleColor();
        }
    }

    private static class ColorItemViewHolder extends WearableListView.ViewHolder {
        private final ColorItem mColorItem;

        public ColorItemViewHolder(ColorItem colorItem) {
            super(colorItem);
            mColorItem = colorItem;
        }
    }
}
