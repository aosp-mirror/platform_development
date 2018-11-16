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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;

import java.util.List;

/**
 * A {@link PagerAdapter} for {@link ShowcaseFragment} that handles the creation of Views based
 * on a list of {@link com.example.android.intentplayground.ShowcaseFragment.Step}.
 */
class StepAdapter extends PagerAdapter {
    private final Context mContext;
    private View.OnClickListener mNextCallback;
    private View.OnClickListener mCancelCallback;
    private View.OnClickListener mFinishCallback;
    private List<ShowcaseFragment.Step> mSteps;
    private LayoutInflater mInflater;

    /**
     * Constructs a new StepAdapter.
     * @param context The context that holds this adapter.
     * @param steps A list of {@link com.example.android.intentplayground.ShowcaseFragment.Step}s
     */
    public StepAdapter(Context context, List<ShowcaseFragment.Step> steps) {
        mContext = context;
        mSteps = steps;
        mInflater = LayoutInflater.from(mContext);
    }

    /**
     * Set the callbacks to be run when pager buttons are clicked.
     * @param finish The method to run when the finish action is requested.
     * @param cancel The method to run when the cancel action is requested.
     * @param next The method to run when the next action is requested.
     */
    public void setButtonCallbacks(View.OnClickListener finish, View.OnClickListener cancel,
                                   View.OnClickListener next) {
        mFinishCallback = finish;
        mCancelCallback = cancel;
        mNextCallback = next;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ShowcaseFragment.Step currentStep = getStep(position);
        ViewGroup skeletonLayout = (ViewGroup) mInflater
                .inflate(R.layout.showcase_skeleton, container, false /* attachToRoot */);
        TextView tutorialText = skeletonLayout.findViewById(R.id.tutorial_text);
        tutorialText.setText(mContext.getString(currentStep.tutorialText));
        Button cancelButton = skeletonLayout.findViewById(R.id.cancel_pager);
        Button nextButton = skeletonLayout.findViewById(R.id.next_pager);
        if (position == getCount() - 1) {
            // last item, adjust button bar
            cancelButton.setVisibility(View.GONE);
            nextButton.setText(R.string.help_step_finish);
            nextButton.setOnClickListener(mFinishCallback);
        } else {
            cancelButton.setOnClickListener(mCancelCallback);
            nextButton.setOnClickListener(mNextCallback);
        }
        container.addView(skeletonLayout);
        return skeletonLayout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object view) {
        container.removeView((View) view);
    }

    public ShowcaseFragment.Step getStep(int i) {
        return mSteps.get(i);
    }

    @Override
    public int getCount() {
        return mSteps.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view.equals(o);
    }
}
