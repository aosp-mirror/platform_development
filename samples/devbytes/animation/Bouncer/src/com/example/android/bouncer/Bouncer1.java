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
 * See the comments in Bouncer for the overall functionality of this app. Changes for this
 * variation are down in the animation setup code.
 */
public class Bouncer1 extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bouncer1);
    }

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

        public void setShapeX(int shapeX) {
            int minX = mShapeX;
            int maxX = mShapeX + mShapeW;
            mShapeX = shapeX;
            minX = Math.min(mShapeX, minX);
            maxX = Math.max(mShapeX + mShapeW, maxX);
            invalidate(minX, mShapeY, maxX, mShapeY + mShapeH);
        }

        public void setShapeY(int shapeY) {
            int minY = mShapeY;
            int maxY = mShapeY + mShapeH;
            mShapeY = shapeY;
            minY = Math.min(mShapeY, minY);
            maxY = Math.max(mShapeY + mShapeH, maxY);
            invalidate(mShapeX, minY, mShapeX + mShapeW, maxY);
        }

        private void setupShape() {
            mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.electricsheep);
            mShapeW = mBitmap.getWidth();
            mShapeH = mBitmap.getHeight();
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAnimation();
                }
            });
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
            ValueAnimator anim = getValueAnimator();
            // In this variation, we put the shape into an infinite bounce, where it keeps moving
            // up and down. Note that it's still not actually "bouncing" because it just uses
            // default time interpolation.
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setRepeatMode(ValueAnimator.REVERSE);
            anim.start();
        }

        ValueAnimator getValueAnimator() {
            ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
            anim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setShapeY((int) (animation.getAnimatedFraction() * (getHeight() - mShapeH)));
                }
            });
            return anim;
        }

    }
}
