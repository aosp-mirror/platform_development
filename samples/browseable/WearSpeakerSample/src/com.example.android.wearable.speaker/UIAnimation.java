/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
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

package com.example.android.wearable.speaker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

/**
 * A helper class to provide a simple animation when user selects any of the three icons on the
 * main UI.
 */
public class UIAnimation {

    private AnimatorSet mCurrentAnimator;
    private final int[] mLargeDrawables = new int[]{R.drawable.ic_mic_120dp,
            R.drawable.ic_play_arrow_120dp, R.drawable.ic_audiotrack_120dp};
    private final ImageView[] mThumbs;
    private ImageView expandedImageView;
    private final View mContainerView;
    private final int mAnimationDurationTime;

    private UIStateListener mListener;
    private UIState mState = UIState.HOME;

    public UIAnimation(View containerView, ImageView[] thumbs, ImageView expandedView,
            int animationDuration, UIStateListener listener) {
        mContainerView = containerView;
        mThumbs = thumbs;
        expandedImageView = expandedView;
        mAnimationDurationTime = animationDuration;
        mListener = listener;

        mThumbs[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImageFromThumb(0);
            }
        });

        mThumbs[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImageFromThumb(1);
            }
        });

        mThumbs[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomImageFromThumb(2);
            }
        });
    }

    private void zoomImageFromThumb(final int index) {
        int imageResId = mLargeDrawables[index];
        final ImageView thumbView = mThumbs[index];
        if (mCurrentAnimator != null) {
            return;
        }

        expandedImageView.setImageResource(imageResId);

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();
        thumbView.getGlobalVisibleRect(startBounds);
        mContainerView.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        for(int k=0; k < 3; k++) {
            mThumbs[k].setAlpha(0f);
        }
        expandedImageView.setVisibility(View.VISIBLE);

        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        AnimatorSet zommInAnimator = new AnimatorSet();
        zommInAnimator.play(ObjectAnimator
                .ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left)).with(
                ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds
                        .top)).with(
                ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
        zommInAnimator.setDuration(mAnimationDurationTime);
        zommInAnimator.setInterpolator(new DecelerateInterpolator());
        zommInAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
                if (mListener != null) {
                    mState = UIState.getUIState(index);
                    mListener.onUIStateChanged(mState);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        zommInAnimator.start();
        mCurrentAnimator = zommInAnimator;

        final float startScaleFinal = startScale;
        expandedImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCurrentAnimator != null) {
                    return;
                }
                AnimatorSet zoomOutAnimator = new AnimatorSet();
                zoomOutAnimator.play(ObjectAnimator
                        .ofFloat(expandedImageView, View.X, startBounds.left))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.Y, startBounds.top))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_X, startScaleFinal))
                        .with(ObjectAnimator
                                .ofFloat(expandedImageView,
                                        View.SCALE_Y, startScaleFinal));
                zoomOutAnimator.setDuration(mAnimationDurationTime);
                zoomOutAnimator.setInterpolator(new DecelerateInterpolator());
                zoomOutAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        for (int k = 0; k < 3; k++) {
                            mThumbs[k].setAlpha(1f);
                        }
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                        if (mListener != null) {
                            mState = UIState.HOME;
                            mListener.onUIStateChanged(mState);
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        thumbView.setAlpha(1f);
                        expandedImageView.setVisibility(View.GONE);
                        mCurrentAnimator = null;
                    }
                });
                zoomOutAnimator.start();
                mCurrentAnimator = zoomOutAnimator;
            }
        });
    }

    public enum UIState {
        MIC_UP(0), SOUND_UP(1), MUSIC_UP(2), HOME(3);
        private int mState;

        UIState(int state) {
            mState = state;
        }

        static UIState getUIState(int state) {
            for(UIState uiState : values()) {
                if (uiState.mState == state) {
                    return uiState;
                }
            }
           return null;
        }
    }

    public interface UIStateListener {
        void onUIStateChanged(UIState state);
    }

    public void transitionToHome() {
        if (mState == UIState.HOME) {
            return;
        }
        expandedImageView.callOnClick();

    }
}
