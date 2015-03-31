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

package com.example.android.wearable.flashlight;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

/**
 * Flashing party lights!
 */
public class PartyLightView extends View {

    private int[] mColors = new int[] {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA
    };

    private int mFromColorIndex;
    private int mToColorIndex;

    /**
     * Value b/t 0 and 1.
     */
    private float mProgress;

    private ArgbEvaluator mEvaluator;

    private int mCurrentColor;

    private Handler mHandler;

    public PartyLightView(Context context) {
        super(context);
        init();
    }

    public PartyLightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(mCurrentColor);
        super.onDraw(canvas);
    }

    public void startCycling() {
        mHandler.sendEmptyMessage(0);
    }

    public void stopCycling() {
        mHandler.removeMessages(0);
    }

    private void init() {
        mEvaluator = new ArgbEvaluator();
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                mCurrentColor = getColor(mProgress, mColors[mFromColorIndex],
                        mColors[mToColorIndex]);
                postInvalidate();
                mProgress += 0.1;
                if (mProgress > 1.0) {
                    mFromColorIndex = mToColorIndex;
                    // Find a new color.
                    mToColorIndex++;
                    if (mToColorIndex >= mColors.length) {
                        mToColorIndex = 0;
                    }
                }
                mHandler.sendEmptyMessageDelayed(0, 100);
            }
        };
    }

    private int getColor(float fraction, int colorStart, int colorEnd) {
        int startInt = colorStart;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = colorEnd;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (startA + (int)(fraction * (endA - startA))) << 24 |
                (startR + (int)(fraction * (endR - startR))) << 16 |
                (startG + (int)(fraction * (endG - startG))) << 8 |
                ((startB + (int)(fraction * (endB - startB))));
    }
}
