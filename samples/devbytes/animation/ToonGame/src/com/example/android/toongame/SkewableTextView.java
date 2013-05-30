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

package com.example.android.toongame;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * This custom TextView can be skewed to the left or right to enable anticipation and
 * follow-through effects
 */
public class SkewableTextView extends TextView {

    private float mSkewX;
    RectF mTempRect = new RectF();
    
    public SkewableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SkewableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkewableTextView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mSkewX != 0) {
            canvas.translate(0, getHeight());
            canvas.skew(mSkewX, 0);
            canvas.translate(0,  -getHeight());
        }
        super.onDraw(canvas);
    }

    public float getSkewX() {
        return mSkewX;
    }
    
    public void setSkewX(float value) {
        if (value != mSkewX) {
            mSkewX = value;
            invalidate();             // force redraw with new skew value
            invalidateSkewedBounds(); // also invalidate appropriate area of parent
        }
    }
    
    /**
     * Need to invalidate proper area of parent for skewed bounds
     */
    private void invalidateSkewedBounds() {
        if (mSkewX != 0) {
            Matrix matrix = new Matrix();
            matrix.setSkew(-mSkewX, 0);
            mTempRect.set(0, 0, getRight(), getBottom());
            matrix.mapRect(mTempRect);
            mTempRect.offset(getLeft() + getTranslationX(), getTop() + getTranslationY());
            ((View) getParent()).invalidate((int) mTempRect.left, (int) mTempRect.top,
                    (int) (mTempRect.right +.5f), (int) (mTempRect.bottom + .5f));
        }
    }
}
