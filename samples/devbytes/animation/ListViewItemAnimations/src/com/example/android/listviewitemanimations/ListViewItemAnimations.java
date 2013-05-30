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

package com.example.android.listviewitemanimations;

import java.util.ArrayList;
import java.util.HashMap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ListView;

/**
 * This example shows how to use a swipe effect to remove items from a ListView,
 * and how to use animations to complete the swipe as well as to animate the other
 * items in the list into their final places. This code works on runtimes back to Gingerbread
 * (Android 2.3), by using the android.view.animation classes on earlier releases.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the androiddevelopers channel on YouTube at
 * https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0.
 */
public class ListViewItemAnimations extends Activity {

    final ArrayList<View> mCheckedViews = new ArrayList<View>();
    StableArrayAdapter mAdapter;
    ListView mListView;
    BackgroundContainer mBackgroundContainer;
    boolean mSwiping = false;
    boolean mItemPressed = false;
    HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
    boolean mAnimating = false;
    float mCurrentX = 0;
    float mCurrentAlpha = 1;

    private static final int SWIPE_DURATION = 250;
    private static final int MOVE_DURATION = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_view_item_animations);
        
        mBackgroundContainer = (BackgroundContainer) findViewById(R.id.listViewBackground);
        mListView = (ListView) findViewById(R.id.listview);
        final ArrayList<String> cheeseList = new ArrayList<String>();
        for (int i = 0; i < Cheeses.sCheeseStrings.length; ++i) {
            cheeseList.add(Cheeses.sCheeseStrings[i]);
        }
        mAdapter = new StableArrayAdapter(this,R.layout.opaque_text_view, cheeseList,
                mTouchListener);
        mListView.setAdapter(mAdapter);
    }
    
    /**
     * Returns true if the current runtime is Honeycomb or later
     */
    private boolean isRuntimePostGingerbread() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        
        float mDownX;
        private int mSwipeSlop = -1;
        
        @SuppressLint("NewApi")
        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(ListViewItemAnimations.this).
                        getScaledTouchSlop();
            }
            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mAnimating) {
                    // Multi-item swipes not handled
                    return true;
                }
                mItemPressed = true;
                mDownX = event.getX();
                break;
            case MotionEvent.ACTION_CANCEL:
                setSwipePosition(v, 0);
                mItemPressed = false;
                break;
            case MotionEvent.ACTION_MOVE:
                {
                    if (mAnimating) {
                        return true;
                    }
                    float x = event.getX();
                    if (isRuntimePostGingerbread()) {
                        x += v.getTranslationX();
                    }
                    float deltaX = x - mDownX;
                    float deltaXAbs = Math.abs(deltaX);
                    if (!mSwiping) {
                        if (deltaXAbs > mSwipeSlop) {
                            mSwiping = true;
                            mListView.requestDisallowInterceptTouchEvent(true);
                            mBackgroundContainer.showBackground(v.getTop(), v.getHeight());
                        }
                    }
                    if (mSwiping) {
                        setSwipePosition(v, deltaX);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                {
                    if (mAnimating) {
                        return true;
                    }
                    // User let go - figure out whether to animate the view out, or back into place
                    if (mSwiping) {
                        float x = event.getX();
                        if (isRuntimePostGingerbread()) {
                            x += v.getTranslationX();
                        }
                        float deltaX = x - mDownX;
                        float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered;
                        float endX;
                        final boolean remove;
                        if (deltaXAbs > v.getWidth() / 4) {
                            // Greater than a quarter of the width - animate it out
                            fractionCovered = deltaXAbs / v.getWidth();
                            endX = deltaX < 0 ? -v.getWidth() : v.getWidth();
                            remove = true;
                        } else {
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / v.getWidth());
                            endX = 0;
                            remove = false;
                        }
                        // Animate position and alpha
                        long duration = (int) ((1 - fractionCovered) * SWIPE_DURATION);
                        animateSwipe(v, endX, duration, remove);
                    } else {
                        mItemPressed = false;
                    }
                }
                break;
            default: 
                return false;
            }
            return true;
        }
    };

    /**
     * Animates a swipe of the item either back into place or out of the listview container.
     * NOTE: This is a simplified version of swipe behavior, for the purposes of this demo
     * about animation. A real version should use velocity (via the VelocityTracker class)
     * to send the item off or back at an appropriate speed.
     */
    @SuppressLint("NewApi")
    private void animateSwipe(final View view, float endX, long duration, final boolean remove) {
        mAnimating = true;
        mListView.setEnabled(false);
        if (isRuntimePostGingerbread()) {
            view.animate().setDuration(duration).
                    alpha(remove ? 0 : 1).translationX(endX).
                    setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            // Restore animated values
                            view.setAlpha(1);
                            view.setTranslationX(0);
                            if (remove) {
                                animateOtherViews(mListView, view);
                            } else {
                                mBackgroundContainer.hideBackground();
                                mSwiping = false;
                                mAnimating = false;
                                mListView.setEnabled(true);
                            }
                            mItemPressed = false;
                        }
                    });
        } else {
            TranslateAnimation swipeAnim = new TranslateAnimation(mCurrentX, endX, 0, 0);
            AlphaAnimation alphaAnim = new AlphaAnimation(mCurrentAlpha, remove ? 0 : 1);
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(swipeAnim);
            set.addAnimation(alphaAnim);
            set.setDuration(duration);
            view.startAnimation(set);
            setAnimationEndAction(set, new Runnable() {
                @Override
                public void run() {
                    if (remove) {
                        animateOtherViews(mListView, view);
                    } else {
                        mBackgroundContainer.hideBackground();
                        mSwiping = false;
                        mAnimating = false;
                        mListView.setEnabled(true);
                    }
                    mItemPressed = false;
                }
            });
        }
            
    }
        
    /**
     * Sets the horizontal position and translucency of the view being swiped.
     */
    @SuppressLint("NewApi")
    private void setSwipePosition(View view, float deltaX) {
        float fraction = Math.abs(deltaX) / view.getWidth();
        if (isRuntimePostGingerbread()) {
            view.setTranslationX(deltaX);
            view.setAlpha(1 - fraction);
        } else {
            // Hello, Gingerbread!
            TranslateAnimation swipeAnim = new TranslateAnimation(deltaX, deltaX, 0, 0);
            mCurrentX = deltaX;
            mCurrentAlpha = (1 - fraction);
            AlphaAnimation alphaAnim = new AlphaAnimation(mCurrentAlpha, mCurrentAlpha);
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(swipeAnim);
            set.addAnimation(alphaAnim);
            set.setFillAfter(true);
            set.setFillEnabled(true);
            view.startAnimation(set);
        }
    }

    /**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */
    private void animateOtherViews(final ListView listview, View viewToRemove) {
        int firstVisiblePosition = listview.getFirstVisiblePosition();
        for (int i = 0; i < listview.getChildCount(); ++i) {
            View child = listview.getChildAt(i);
            int position = firstVisiblePosition + i;
            long itemId = mAdapter.getItemId(position);
            if (child != viewToRemove) {
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        // Delete the item from the adapter
        int position = mListView.getPositionForView(viewToRemove);
        mAdapter.remove(mAdapter.getItem(position));
        
        // After layout runs, capture position of all itemIDs, compare to pre-layout
        // positions, and animate changes
        final ViewTreeObserver observer = listview.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = listview.getFirstVisiblePosition();
                for (int i = 0; i < listview.getChildCount(); ++i) {
                    final View child = listview.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = mAdapter.getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop == null) {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on whether they're coming in from the bottom (i > 0) or top.
                        int childHeight = child.getHeight() + listview.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                    }
                    int delta = startTop - top;
                    if (delta != 0) {
                        Runnable endAction = firstAnimation ?
                            new Runnable() {
                                public void run() {
                                    mBackgroundContainer.hideBackground();
                                    mSwiping = false;
                                    mAnimating = false;
                                    mListView.setEnabled(true);
                                }
                            } :
                            null;
                        firstAnimation = false;
                        moveView(child, 0, 0, delta, 0, endAction);
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
    
    /**
     * Animate a view between start and end X/Y locations, using either old (pre-3.0) or
     * new animation APIs.
     */
    @SuppressLint("NewApi")
    private void moveView(View view, float startX, float endX, float startY, float endY,
            Runnable endAction) {
        final Runnable finalEndAction = endAction;
        if (isRuntimePostGingerbread()) {
            view.animate().setDuration(MOVE_DURATION);
            if (startX != endX) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, endX);
                anim.setDuration(MOVE_DURATION);
                anim.start();
                setAnimatorEndAction(anim, endAction);
                endAction = null;
            }
            if (startY != endY) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY);
                anim.setDuration(MOVE_DURATION);
                anim.start();
                setAnimatorEndAction(anim, endAction);
            }
        } else {
            TranslateAnimation translator = new TranslateAnimation(startX, endX, startY, endY);
            translator.setDuration(MOVE_DURATION);
            view.startAnimation(translator);
            if (endAction != null) {
                view.getAnimation().setAnimationListener(new AnimationListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        finalEndAction.run();
                    }
                });
            }
        }
    }
    
    @SuppressLint("NewApi")
    private void setAnimatorEndAction(Animator animator, final Runnable endAction) {
        if (endAction != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endAction.run();
                }
            });
        }
    }

    private void setAnimationEndAction(Animation animation, final Runnable endAction) {
        if (endAction != null) {
            animation.setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    endAction.run();
                }
            });
        }
    }

    /**
     * Utility, to avoid having to implement every method in AnimationListener in
     * every implementation class
     */
    static class AnimationListenerAdapter implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

}
