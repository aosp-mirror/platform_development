/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.example.android.autofillkeyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/** The root view for the Input Method. */
public final class InputView extends FrameLayout {

    // If true, this InputView will simulate Gboard's InputView behavior, which expands its
    // region to the entire window regardless of its content view's size.
    private static final boolean EXPAND_TO_WINDOW = false;

    private int mRealHeight;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mRealHeight = getMeasuredHeight();
        if (EXPAND_TO_WINDOW && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST) {
            setMeasuredDimension(getMeasuredWidth(), MeasureSpec.getSize(heightMeasureSpec));
        }
    }

    int getTopInsets() {
        return getMeasuredHeight() - mRealHeight;
    }
}
