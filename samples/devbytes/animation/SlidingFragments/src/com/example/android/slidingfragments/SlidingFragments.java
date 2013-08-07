/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.slidingfragments;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;

/**
 * This application shows a simple technique to animate and overlay two fragments
 * on top of each other in order to provide a more immersive experience,
 * as opposed to only having full screen transitions. When additional content
 * (text) related to the currently displayed content (image) is to be shown,
 * the currently visible content can be moved into the background instead of
 * being removed from the screen entirely. This effect can therefore
 * provide a more natural way of displaying additional information to the user
 * using a different fragment.
 *
 * In this specific demo, tapping on the screen toggles between the two
 * animated states of the fragment. When the animation is called,
 * the fragment with an image animates into the background while the fragment
 * containing text slides up on top of it. When the animation is toggled once
 * more, the text fragment slides back down and the image fragment regains
 * focus.
 */
public class SlidingFragments extends Activity implements
        OnTextFragmentAnimationEndListener, FragmentManager.OnBackStackChangedListener {

    ImageFragment mImageFragment;
    TextFragment mTextFragment;
    View mDarkHoverView;

    boolean mDidSlideOut = false;
    boolean mIsAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sliding_fragments_layout);

        mDarkHoverView = findViewById(R.id.dark_hover_view);
        mDarkHoverView.setAlpha(0);

        mImageFragment = (ImageFragment) getFragmentManager().findFragmentById(R.id.move_fragment);
        mTextFragment = new TextFragment();

        getFragmentManager().addOnBackStackChangedListener(this);

        mImageFragment.setClickListener(mClickListener);
        mTextFragment.setClickListener(mClickListener);
        mTextFragment.setOnTextFragmentAnimationEnd(this);
        mDarkHoverView.setOnClickListener(mClickListener);

    }

    View.OnClickListener mClickListener = new View.OnClickListener () {
        @Override
        public void onClick(View view) {
            switchFragments();
        }
    };

    /**
     * This method is used to toggle between the two fragment states by
     * calling the appropriate animations between them. The entry and exit
     * animations of the text fragment are specified in R.animator resource
     * files. The entry and exit animations of the image fragment are
     * specified in the slideBack and slideForward methods below. The reason
     * for separating the animation logic in this way is because the translucent
     * dark hover view must fade in at the same time as the image fragment
     * animates into the background, which would be difficult to time
     * properly given that the setCustomAnimations method can only modify the
     * two fragments in the transaction.
     */
    private void switchFragments () {
        if (mIsAnimating) {
            return;
        }
        mIsAnimating = true;
        if (mDidSlideOut) {
            mDidSlideOut = false;
            getFragmentManager().popBackStack();
        } else {
            mDidSlideOut = true;

            AnimatorListener listener = new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator arg0) {
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.animator.slide_fragment_in, 0, 0,
                            R.animator.slide_fragment_out);
                    transaction.add(R.id.move_to_back_container, mTextFragment);
                    transaction.addToBackStack(null);
                    transaction.commit();
                }
            };
            slideBack (listener);
        }
    }

    @Override
    public void onBackStackChanged() {
        if (!mDidSlideOut) {
            slideForward(null);
        }

    }

    /**
     * This method animates the image fragment into the background by both
     * scaling and rotating the fragment's view, as well as adding a
     * translucent dark hover view to inform the user that it is inactive.
     */
    public void slideBack(AnimatorListener listener)
    {
        View movingFragmentView = mImageFragment.getView();

        PropertyValuesHolder rotateX =  PropertyValuesHolder.ofFloat("rotationX", 40f);
        PropertyValuesHolder scaleX =  PropertyValuesHolder.ofFloat("scaleX", 0.8f);
        PropertyValuesHolder scaleY =  PropertyValuesHolder.ofFloat("scaleY", 0.8f);
        ObjectAnimator movingFragmentAnimator = ObjectAnimator.
                ofPropertyValuesHolder(movingFragmentView, rotateX, scaleX, scaleY);

        ObjectAnimator darkHoverViewAnimator = ObjectAnimator.
                ofFloat(mDarkHoverView, "alpha", 0.0f, 0.5f);

        ObjectAnimator movingFragmentRotator = ObjectAnimator.
                ofFloat(movingFragmentView, "rotationX", 0);
        movingFragmentRotator.setStartDelay(getResources().
                getInteger(R.integer.half_slide_up_down_duration));

        AnimatorSet s = new AnimatorSet();
        s.playTogether(movingFragmentAnimator, darkHoverViewAnimator, movingFragmentRotator);
        s.addListener(listener);
        s.start();
    }

    /**
     * This method animates the image fragment into the foreground by both
     * scaling and rotating the fragment's view, while also removing the
     * previously added translucent dark hover view. Upon the completion of
     * this animation, the image fragment regains focus since this method is
     * called from the onBackStackChanged method.
     */
    public void slideForward(AnimatorListener listener)
    {
        View movingFragmentView = mImageFragment.getView();

        PropertyValuesHolder rotateX =  PropertyValuesHolder.ofFloat("rotationX", 40f);
        PropertyValuesHolder scaleX =  PropertyValuesHolder.ofFloat("scaleX", 1.0f);
        PropertyValuesHolder scaleY =  PropertyValuesHolder.ofFloat("scaleY", 1.0f);
        ObjectAnimator movingFragmentAnimator = ObjectAnimator.
                ofPropertyValuesHolder(movingFragmentView, rotateX, scaleX, scaleY);

        ObjectAnimator darkHoverViewAnimator = ObjectAnimator.
                ofFloat(mDarkHoverView, "alpha", 0.5f, 0.0f);

        ObjectAnimator movingFragmentRotator = ObjectAnimator.
                ofFloat(movingFragmentView, "rotationX", 0);
        movingFragmentRotator.setStartDelay(
                getResources().getInteger(R.integer.half_slide_up_down_duration));

        AnimatorSet s = new AnimatorSet();
        s.playTogether(movingFragmentAnimator, movingFragmentRotator, darkHoverViewAnimator);
        s.setStartDelay(getResources().getInteger(R.integer.slide_up_down_duration));
        s.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
            }
        });
        s.start();
    }

    public void onAnimationEnd() {
        mIsAnimating = false;
    }
}
