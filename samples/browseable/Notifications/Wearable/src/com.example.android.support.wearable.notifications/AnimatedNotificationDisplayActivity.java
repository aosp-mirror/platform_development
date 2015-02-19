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

package com.example.android.support.wearable.notifications;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

/**
 * Custom display activity for an animated sample notification.
 */
public class AnimatedNotificationDisplayActivity extends Activity {
    public static final String EXTRA_TITLE = "title";

    private static final int BASE_ANIMATION_DURATION_MS = 2000;

    private Random mRandom;
    private int mAnimationRange;
    private ImageView mImageView;
    private Animator mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animated_notification_display);

        mRandom = new Random(System.currentTimeMillis());
        mAnimationRange = getResources().getDimensionPixelSize(R.dimen.animation_range);

        String title = getIntent().getStringExtra(EXTRA_TITLE);
        ((TextView) findViewById(R.id.title)).setText(title);

        mImageView = new ImageView(this);
        mImageView.setImageResource(R.drawable.example_big_picture);

        ImageZoomView zoomView = new ImageZoomView(this, mImageView, mAnimationRange);
        zoomView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ((FrameLayout) findViewById(R.id.container)).addView(zoomView, 0);

        createNextAnimation(false);
    }

    private void createNextAnimation(boolean start) {
        float startX = mImageView.getTranslationX();
        float startY = mImageView.getTranslationY();
        float endX = -mRandom.nextInt(mAnimationRange);
        float endY = -mRandom.nextInt(mAnimationRange);
        float distance = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));

        mAnimation = ObjectAnimator.ofPropertyValuesHolder(mImageView,
                PropertyValuesHolder.ofFloat("translationX", startX, endX),
                PropertyValuesHolder.ofFloat("translationY", startY, endY));
        mAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

        mAnimation.setDuration(Math.max(BASE_ANIMATION_DURATION_MS / 10,
                (int) (distance * BASE_ANIMATION_DURATION_MS / mAnimationRange)));

        mAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                createNextAnimation(true);
            }
        });
        if (start) {
            mAnimation.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAnimation.start();
    }

    @Override
    protected void onPause() {
        mAnimation.pause();
        super.onPause();
    }

    /** Helper view that zooms in on a child image view */
    private static class ImageZoomView extends ViewGroup {
        private final int mZoomLength;

        public ImageZoomView(Context context, ImageView imageView, int zoomLength) {
            super(context);
            addView(imageView);
            mZoomLength = zoomLength;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            ImageView imageView = (ImageView) getChildAt(0);

            // Resize the image view to be at least mZoomLength pixels larger in both
            // dimensions than the containing view.
            int imageWidth = imageView.getDrawable().getIntrinsicWidth();
            int imageHeight = imageView.getDrawable().getIntrinsicHeight();
            int minSize = Math.max(right - left, bottom - top) + mZoomLength;
            if (imageWidth > imageHeight) {
                imageWidth = minSize * imageWidth / imageHeight;
                imageHeight = minSize;
            } else {
                imageHeight = minSize * imageHeight / imageWidth;
                imageWidth = minSize;
            }
            imageView.layout(left, top, left + imageWidth, top + imageHeight);
        }
    }
}
