/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Rational;
import android.widget.ImageView;

import com.example.android.apis.R;

/**
 * Extended {@link ImageView} that keeps fixed aspect ratio (specified in layout file) when
 * one of the dimension is in exact while the other one in wrap_content size mode.
 */
public class FixedAspectRatioImageView extends ImageView {
    private Rational mAspectRatio;

    public FixedAspectRatioImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.FixedAspectRatioImageView, 0, 0);
        try {
            mAspectRatio = Rational.parseRational(
                    a.getString(R.styleable.FixedAspectRatioImageView_aspectRatio));
        } finally {
            a.recycle();
        }
    }

    public void setAspectRatio(Rational aspectRatio) {
        if (!mAspectRatio.equals(aspectRatio)) {
            mAspectRatio = aspectRatio;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        final int width, height;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
                && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            height = (int) (width / mAspectRatio.floatValue());
        } else {
            height = MeasureSpec.getSize(heightMeasureSpec);
            width = (int) (height * mAspectRatio.floatValue());
        }
        setMeasuredDimension(width, height);
    }
}