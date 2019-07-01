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
package com.example.android.intentplayground;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager.widget.ViewPager;
import java.util.LinkedList;
import java.util.List;

/**
 * Displays a help overlay over the current activity.
 */
public class ShowcaseFragment extends Fragment {
    private ViewGroup mRoot;
    private List<Step> mSteps = new LinkedList<>();
    private ViewPager mPager;
    private StepAdapter mAdapter;
    private ScrollView mScrollView;
    private View mOldTarget;
    private Drawable mOldTargetBackground;
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();
    private Runnable mUserOnFinish;
    private int mIndex = 0;
    private static final int SCROLL_OFFSET = 50;
    private static final float HIGHLIGHT_ELEVATION = 4;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mAdapter = new StepAdapter(context, mSteps);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context context = getContext();
        mRoot = container;
        FrameLayout backgroundLayout = new FrameLayout(context);
        mPager = new ViewPager(context);
        PagerTitleStrip pagerTitleView = new PagerTitleStrip(context);
        pagerTitleView.setGravity(Gravity.TOP);
        ViewPager.LayoutParams params = new ViewPager.LayoutParams();
        params.width = ViewPager.LayoutParams.MATCH_PARENT;
        params.height = ViewPager.LayoutParams.MATCH_PARENT;
        mPager.setLayoutParams(params);
        backgroundLayout.setLayoutParams(params);
        params.height = ViewPager.LayoutParams.WRAP_CONTENT;
        params.isDecor = true;
        pagerTitleView.setLayoutParams(params);
        mPager.addView(pagerTitleView);
        backgroundLayout.addView(mPager);
        mAdapter.setButtonCallbacks(
                /* onFinish */ view -> {
                    cancel();
                    mScrollView.scrollTo(0, 0);
                },
                /* onCancel */ view -> cancel(),
                /* onNext */ view -> next()
        );
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {}
            @Override
            public void onPageScrollStateChanged(int i) {}
            @Override
            public void onPageSelected(int i) {
                executeStep(i);
            }
        });
        mPager.setAdapter(mAdapter);
        return backgroundLayout;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Get display metrics for converting dp to px
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        // Scroll to and highlight first step
        executeStep(mIndex);
    }

    @Override
    public void onStop() {
        super.onStop();
        clearHighlight();
        if (mUserOnFinish != null) mUserOnFinish.run();
    }

    public void setScroller(ScrollView scroller) {
        mScrollView = scroller;
    }

    public void addStep(Step step) {
        mSteps.add(step);
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    public void addStep(@StringRes int tutorialText, @IdRes int targetView) {
        addStep(new Step(tutorialText, targetView));
    }

    public void addStep(@StringRes int tutorialText, @IdRes int targetView,
                        @IdRes int highlightTargetView) {
        addStep(new Step(tutorialText, targetView, highlightTargetView));
    }

    public void addStep(@StringRes int tutorialText,
                        @IdRes int targetView,
                        Runnable callback) {
        addStep(new Step(tutorialText, targetView, callback));
    }

    /**
     * Advances the pager to the next step.
     */
    public void next() {
        mPager.setCurrentItem(++mIndex);
    }

    /**
     * Shows the indicated page.
     * @param i The index of the page to show.
     */
    private void executeStep(int i) {
        Step current = mAdapter.getStep(i);
        View target = mRoot.findViewById(current.targetViewRes);
        View highlightTarget = current.highlightTargetViewRes != 0 ?
                mRoot.findViewById(current.highlightTargetViewRes) : target;
        target.getParent().requestChildFocus(target, target);
        mScrollView.smoothScrollTo(0, Float.valueOf(target.getTop()).intValue()
                - SCROLL_OFFSET);
        highlightView(highlightTarget);
        if (current.callback != null) current.callback.run();
    }

    /**
     * Destroys this fragment.
     */
    public void cancel() {
        getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    private void clearHighlight() {
        if (mOldTarget != null) {
            mOldTarget.setBackground(mOldTargetBackground);
            mOldTarget = null;
        }
        mRoot.setBackground(null); // Clear root background
    }

    private void highlightView(View target) {
        Resources res = getContext().getResources();
        clearHighlight();
        mOldTarget = target;
        mOldTargetBackground = target.getBackground();
        target.setBackground(res.getDrawable(R.drawable.showcase_background, null /* theme*/));
        target.setElevation(HIGHLIGHT_ELEVATION * mDisplayMetrics.density);
        // Dull parent background
        mRoot.setBackground(res.getDrawable(R.drawable.shade, null /* theme */));
    }

    /**
     * Set a callback to be run in the onStop() method
     * @param onFinish Callback to be run when the Showcase is finished
     */
    public void setOnFinish(Runnable onFinish) {
        this.mUserOnFinish = onFinish;
    }

    /**
     * Represents a page in {@link ViewPager}, with associated text to show and a target element
     * to scroll to.
     */
    public class Step {
        @StringRes public int tutorialText;
        @IdRes public int targetViewRes;
        @IdRes public int highlightTargetViewRes;

        public Runnable callback;
        public Step(@StringRes int tutorialSentence, @IdRes int targetView) {
            tutorialText = tutorialSentence;
            targetViewRes = targetView;
        }
        public Step(@StringRes int tutorialSentence, @IdRes int targetView,
                    @IdRes int highlightTargetView) {
            tutorialText = tutorialSentence;
            targetViewRes = targetView;
            highlightTargetViewRes = highlightTargetView;
        }
        public Step(@StringRes int tutorialSentence, @IdRes int targetView,
                    Runnable onStepCallback) {
            tutorialText = tutorialSentence;
            targetViewRes = targetView;
            callback = onStepCallback;
        }
    }

}
