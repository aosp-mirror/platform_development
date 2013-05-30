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

package com.example.android.listviewremovalanimation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BackgroundContainer extends FrameLayout {

    boolean mShowing = false;
    Drawable mShadowedBackground;
    int mOpenAreaTop, mOpenAreaBottom, mOpenAreaHeight;
    boolean mUpdateBounds = false;
    
    public BackgroundContainer(Context context) {
        super(context);
        init();
    }

    public BackgroundContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BackgroundContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mShadowedBackground =
                getContext().getResources().getDrawable(R.drawable.shadowed_background);
    }

    public void showBackground(int top, int bottom) {
        setWillNotDraw(false);
        mOpenAreaTop = top;
        mOpenAreaHeight = bottom;
        mShowing = true;
        mUpdateBounds = true;
    }
    
    public void hideBackground() {
        setWillNotDraw(true);
        mShowing = false;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if (mShowing) {
            	if (mUpdateBounds) {
                mShadowedBackground.setBounds(0, 0, getWidth(), mOpenAreaHeight);
            	}
            canvas.save();
            canvas.translate(0, mOpenAreaTop);
            mShadowedBackground.draw(canvas);
            canvas.restore();
        }
    }

}
