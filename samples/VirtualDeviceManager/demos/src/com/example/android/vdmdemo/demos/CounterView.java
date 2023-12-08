/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.demos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

/** A view rendering a simple counter that is incremented every time onDraw() is called. */
public class CounterView extends View {

    private static final String TAG = "CounterView";
    private static final int TEXT_SIZE_SP = 100;
    private long mCounter = 0;
    private final Paint mTextPaint = new Paint();

    public CounterView(Context context) {
        super(context);
        init();
    }

    public CounterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CounterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public CounterView(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mTextPaint.setColor(Color.RED);
        mTextPaint.setStyle(Style.FILL);
        mTextPaint.setTextSize(computeScaledTextSizeInPixels());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText(String.valueOf(mCounter), 0, 200, mTextPaint);
        Log.e(TAG, "Rendered counter: " + mCounter);
        mCounter++;
    }

    private float computeScaledTextSizeInPixels() {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                TEXT_SIZE_SP,
                getContext().getResources().getDisplayMetrics());
    }
}
