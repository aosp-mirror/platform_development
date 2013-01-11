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

package com.example.android.bouncer;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

/**
 * This example shows simple uses of ValueAnimator, ObjectAnimator, and interpolators
 * to control how a shape is moved around on the screen.
 *
 * The Bouncer1, Bouncer2, and Vouncer3 files are exactly like this one except for variations
 * in how the animation is set up and run.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=vCTcmPIKgpM.
 */
public class Bouncer extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bouncer);
    }

    /**
     * A custom view is used to paint the green background and the shape.
     */
    static class MyView extends View {

        Bitmap mBitmap;
        Paint paint = new Paint();
        int mShapeX, mShapeY;
        int mShapeW, mShapeH;

        public MyView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            setupShape();
        }

        public MyView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setupShape();
        }

        public MyView(Context context) {
            super(context);
            setupShape();
        }

        private void setupShape() {
            mBitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.electricsheep);
            mShapeW = mBitmap.getWidth();
            mShapeH = mBitmap.getHeight();
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAnimation();
                }
            });
        }

        /**
         * Moving the shape in x or y causes an invalidation of the area it used to occupy
         * plus the area it now occupies.
         */
        public void setShapeX(int shapeX) {
            int minX = mShapeX;
            int maxX = mShapeX + mShapeW;
            mShapeX = shapeX;
            minX = Math.min(mShapeX, minX);
            maxX = Math.max(mShapeX + mShapeW, maxX);
            invalidate(minX, mShapeY, maxX, mShapeY + mShapeH);
        }

        /**
         * Moving the shape in x or y causes an invalidation of the area it used to occupy
         * plus the area it now occupies.
         */
        public void setShapeY(int shapeY) {
            int minY = mShapeY;
            int maxY = mShapeY + mShapeH;
            mShapeY = shapeY;
            minY = Math.min(mShapeY, minY);
            maxY = Math.max(mShapeY + mShapeH, maxY);
            invalidate(mShapeX, minY, mShapeX + mShapeW, maxY);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mShapeX = (w - mBitmap.getWidth()) / 2;
            mShapeY = 0;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, mShapeX, mShapeY, paint);
        }

        void startAnimation() {
            // This version uses ValueAnimator, which requires adding an update
            // listener and manually updating the shape position on each frame.
            ValueAnimator anim = getValueAnimator();
            anim.start();
        }

        ValueAnimator getValueAnimator() {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setShapeY((int) (animation.getAnimatedFraction() *
                            (getHeight() - mShapeH)));
                }
            });
            return anim;
        }

    }
}
