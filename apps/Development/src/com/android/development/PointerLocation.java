/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.development;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.FontMetricsInt;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.VelocityTracker;
import android.view.View;

import java.util.ArrayList;

/**
 * Demonstrates wrapping a layout in a ScrollView.
 *
 */
public class PointerLocation extends Activity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(new MyView(this));
        
        // Make the screen full bright for this activity.
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
    }
    
    public class MyView extends View {
        private final Paint mTextPaint;
        private final Paint mTextBackgroundPaint;
        private final Paint mTextLevelPaint;
        private final Paint mPaint;
        private final Paint mTargetPaint;
        private final FontMetricsInt mTextMetrics = new FontMetricsInt();
        private final ArrayList<Float> mXs = new ArrayList<Float>();
        private final ArrayList<Float> mYs = new ArrayList<Float>();
        private int mHeaderBottom;
        private boolean mCurDown;
        private int mCurX;
        private int mCurY;
        private float mCurPressure;
        private float mCurSize;
        private int mCurWidth;
        private VelocityTracker mVelocity;
        
        public MyView(Context c) {
            super(c);
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(10);
            mTextPaint.setARGB(255, 0, 0, 0);
            mTextBackgroundPaint = new Paint();
            mTextBackgroundPaint.setAntiAlias(false);
            mTextBackgroundPaint.setARGB(128, 255, 255, 255);
            mTextLevelPaint = new Paint();
            mTextLevelPaint.setAntiAlias(false);
            mTextLevelPaint.setARGB(192, 255, 0, 0);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setARGB(255, 255, 255, 255);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(2);
            mTargetPaint = new Paint();
            mTargetPaint.setAntiAlias(false);
            mTargetPaint.setARGB(192, 0, 0, 255);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            mTextPaint.getFontMetricsInt(mTextMetrics);
            mHeaderBottom = -mTextMetrics.ascent+mTextMetrics.descent+2;
            Log.i("foo", "Metrics: ascent=" + mTextMetrics.ascent
                    + " descent=" + mTextMetrics.descent
                    + " leading=" + mTextMetrics.leading
                    + " top=" + mTextMetrics.top
                    + " bottom=" + mTextMetrics.bottom);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth()/5;
            int base = -mTextMetrics.ascent+1;
            int bottom = mHeaderBottom;
            canvas.drawRect(0, 0, w-1, bottom, mTextBackgroundPaint);
            canvas.drawText("X: " + mCurX, 1, base, mTextPaint);
            canvas.drawRect(w, 0, (w * 2) - 1, bottom, mTextBackgroundPaint);
            canvas.drawText("Y: " + mCurY, 1 + w, base, mTextPaint);
            canvas.drawRect(w * 2, 0, (w * 3) - 1, bottom, mTextBackgroundPaint);
            canvas.drawRect(w * 2, 0, (w * 2) + (mCurPressure * w) - 1, bottom, mTextLevelPaint);
            canvas.drawText("Pres: " + mCurPressure, 1 + w * 2, base, mTextPaint);
            canvas.drawRect(w * 3, 0, (w * 4) - 1, bottom, mTextBackgroundPaint);
            canvas.drawRect(w * 3, 0, (w * 3) + (mCurSize * w) - 1, bottom, mTextLevelPaint);
            canvas.drawText("Size: " + mCurSize, 1 + w * 3, base, mTextPaint);
            canvas.drawRect(w * 4, 0, getWidth(), bottom, mTextBackgroundPaint);
            int velocity = mVelocity == null ? 0 : (int) (mVelocity.getYVelocity() * 1000);
            canvas.drawText("yVel: " + velocity, 1 + w * 4, base, mTextPaint);
            
            final int N = mXs.size();
            float lastX=0, lastY=0;
            mPaint.setARGB(255, 0, 255, 255);
            for (int i=0; i<N; i++) {
                float x = mXs.get(i);
                float y = mYs.get(i);
                if (i > 0) {
                    canvas.drawLine(lastX, lastY, x, y, mTargetPaint);
                    canvas.drawPoint(lastX, lastY, mPaint);
                }
                lastX = x;
                lastY = y;
            }
            if (mVelocity != null) {
                mPaint.setARGB(255, 255, 0, 0);
                float xVel = mVelocity.getXVelocity() * (1000/60);
                float yVel = mVelocity.getYVelocity() * (1000/60);
                canvas.drawLine(lastX, lastY, lastX+xVel, lastY+yVel, mPaint);
            } else {
                canvas.drawPoint(lastX, lastY, mPaint);
            }
            
            if (mCurDown) {
                canvas.drawLine(0, (int)mCurY, getWidth(), (int)mCurY, mTargetPaint);
                canvas.drawLine((int)mCurX, 0, (int)mCurX, getHeight(), mTargetPaint);
                int pressureLevel = (int)(mCurPressure*255);
                mPaint.setARGB(255, pressureLevel, 128, 255-pressureLevel);
                canvas.drawPoint(mCurX, mCurY, mPaint);
                canvas.drawCircle(mCurX, mCurY, mCurWidth, mPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            //mRect.set(0, 0, getWidth(), mHeaderBottom+1);
            //invalidate(mRect);
            //if (mCurDown) {
            //    mRect.set(mCurX-mCurWidth-3, mCurY-mCurWidth-3,
            //            mCurX+mCurWidth+3, mCurY+mCurWidth+3);
            //} else {
            //    mRect.setEmpty();
            //}
            if (action == MotionEvent.ACTION_DOWN) {
                mXs.clear();
                mYs.clear();
                mVelocity = VelocityTracker.obtain();
            }
            mVelocity.addMovement(event);
            mVelocity.computeCurrentVelocity(1);
            final int N = event.getHistorySize();
            for (int i=0; i<N; i++) {
                mXs.add(event.getHistoricalX(i));
                mYs.add(event.getHistoricalY(i));
            }
            mXs.add(event.getX());
            mYs.add(event.getY());
            mCurDown = action == MotionEvent.ACTION_DOWN
                    || action == MotionEvent.ACTION_MOVE;
            mCurX = (int)event.getX();
            mCurY = (int)event.getY();
            mCurPressure = event.getPressure();
            mCurSize = event.getSize();
            mCurWidth = (int)(mCurSize*(getWidth()/3));
            //if (mCurDown) {
            //    mRect.union(mCurX-mCurWidth-3, mCurY-mCurWidth-3,
            //            mCurX+mCurWidth+3, mCurY+mCurWidth+3);
            //}
            //invalidate(mRect);
            invalidate();
            return true;
        }
        
    }
}
