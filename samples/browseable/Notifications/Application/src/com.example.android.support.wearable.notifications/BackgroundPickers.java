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

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the background image pickers.
 */
public class BackgroundPickers {

    public interface OnBackgroundPickersChangedListener {
        public void onBackgroundPickersChanged(BackgroundPickers pickers);
    }

    private final ViewGroup mContainer;
    private final OnPickedListener mOnPickedListener;
    private final List<ViewGroup> mPickers;
    private final OnBackgroundPickersChangedListener listener;

    public BackgroundPickers(ViewGroup container, OnBackgroundPickersChangedListener listener) {
        this.mContainer = container;
        this.mOnPickedListener = new OnPickedListener();
        this.mPickers = new ArrayList<ViewGroup>();
        this.listener = listener;
    }

    /**
     * Generates the pickers as necessary.
     */
    public void generatePickers(int count) {
        // Clear existing containers.
        clear();

        // Fill in new pickers.
        LayoutInflater inflater = LayoutInflater.from(mContainer.getContext());
        Resources res = mContainer.getResources();
        for (int i = 0; i < count; i++) {
            View picker = inflater.inflate(R.layout.background_picker, mContainer, false);
            TextView label = (TextView) picker.findViewById(R.id.bg_picker_label);
            label.setText(String.format(res.getString(R.string.bg_picker_label), i+1));
            ViewGroup pickerBox = (ViewGroup) picker.findViewById(R.id.bg_picker_container);
            mPickers.add(pickerBox);
            for (int j = 0; j < pickerBox.getChildCount(); j++) {
                ImageView img = (ImageView) pickerBox.getChildAt(j);
                img.setOnClickListener(mOnPickedListener);
            }
            mContainer.addView(picker);
        }
    }

    /**
     * Returns the background resource for the picker at the given index.
     * @param position Index of the background picker.
     * @return Id of the background image resource. null if no image is picked.
     */
    public Integer getRes(int position) {
        String tag = (String) mPickers.get(position).getTag();
        if (tag == null) {
            return null;
        }

        Context context = mContainer.getContext();
        return context.getResources().getIdentifier(tag, "drawable", context.getPackageName());
    }

    /**
     * Returns the all the background resources for the pickers managed by this object. Returns null
     * if no pickers exist.
     */
    public Integer[] getRes() {
        if (mPickers.size() == 0) {
            return null;
        }

        Integer[] res = new Integer[mPickers.size()];
        for (int i = 0; i < mPickers.size(); i++) {
            res[i] = getRes(i);
        }
        return res;
    }

    /**
     * Clears the pickers.
     */
    public void clear() {
        mContainer.removeAllViews();
        mPickers.clear();
    }

    public int getCount() {
        return mPickers.size();
    }

    private class OnPickedListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            ImageView pickedView = (ImageView) view;
            ViewGroup pickerBox = (ViewGroup) view.getParent();

            // Clear old selection.
            for (int i = 0; i < pickerBox.getChildCount(); i++) {
                ImageView childView = (ImageView) pickerBox.getChildAt(i);
                childView.setBackgroundResource(R.drawable.unselected_background);
            }

            // Set new selection.
            pickedView.setBackgroundResource(R.drawable.selected_background);
            pickerBox.setTag(pickedView.getTag());

            if (listener != null) {
                listener.onBackgroundPickersChanged(BackgroundPickers.this);
            }
        }
    }
}
