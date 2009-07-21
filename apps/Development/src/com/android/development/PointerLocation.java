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
    
    public static class PointerState {
        private final ArrayList<Float> mXs = new ArrayList<Float>();
        private final ArrayList<Float> mYs = new ArrayList<Float>();
        private boolean mCurDown;
        private int mCurX;
        private int mCurY;
        private float mCurPressure;
        private float mCurSize;
        private int mCurWidth;
        private VelocityTracker mVelocity;
    }
    
    public class MyView extends View {
        private final Paint mTextPaint;
        private final Paint mTextBackgroundPaint;
        private final Paint mTextLevelPaint;
        private final Paint mPaint;
        private final Paint mTargetPaint;
        private final FontMetricsInt mTextMetrics = new FontMetricsInt();
        private int mHeaderBottom;
        private boolean mCurDown;
        private final ArrayList<PointerState> mPointers
                 = new ArrayList<PointerState>();
        
        public MyView(Context c) {
            super(c);
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(10
                    * getResources().getDisplayMetrics().density);
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
            
            final int NP = mPointers.size();
            
            if (NP > 0) {
                final PointerState ps = mPointers.get(0);
                canvas.drawRect(0, 0, w-1, bottom, mTextBackgroundPaint);
                canvas.drawText("X: " + ps.mCurX, 1, base, mTextPaint);
                canvas.drawRect(w, 0, (w * 2) - 1, bottom, mTextBackgroundPaint);
                canvas.drawText("Y: " + ps.mCurY, 1 + w, base, mTextPaint);
                canvas.drawRect(w * 2, 0, (w * 3) - 1, bottom, mTextBackgroundPaint);
                canvas.drawRect(w * 2, 0, (w * 2) + (ps.mCurPressure * w) - 1, bottom, mTextLevelPaint);
                canvas.drawText("Pres: " + ps.mCurPressure, 1 + w * 2, base, mTextPaint);
                canvas.drawRect(w * 3, 0, (w * 4) - 1, bottom, mTextBackgroundPaint);
                canvas.drawRect(w * 3, 0, (w * 3) + (ps.mCurSize * w) - 1, bottom, mTextLevelPaint);
                canvas.drawText("Size: " + ps.mCurSize, 1 + w * 3, base, mTextPaint);
                canvas.drawRect(w * 4, 0, getWidth(), bottom, mTextBackgroundPaint);
                int velocity = ps.mVelocity == null ? 0 : (int) (ps.mVelocity.getYVelocity() * 1000);
                canvas.drawText("yVel: " + velocity, 1 + w * 4, base, mTextPaint);
            }
            
            for (int p=0; p<NP; p++) {
                final PointerState ps = mPointers.get(p);
                
                final int N = ps.mXs.size();
                float lastX=0, lastY=0;
                mPaint.setARGB(255, 0, 255, 255);
                for (int i=0; i<N; i++) {
                    float x = ps.mXs.get(i);
                    float y = ps.mYs.get(i);
                    if (i > 0) {
                        canvas.drawLine(lastX, lastY, x, y, mTargetPaint);
                        canvas.drawPoint(lastX, lastY, mPaint);
                    }
                    lastX = x;
                    lastY = y;
                }
                if (ps.mVelocity != null) {
                    mPaint.setARGB(255, 255, 0, 0);
                    float xVel = ps.mVelocity.getXVelocity() * (1000/60);
                    float yVel = ps.mVelocity.getYVelocity() * (1000/60);
                    canvas.drawLine(lastX, lastY, lastX+xVel, lastY+yVel, mPaint);
                } else {
                    canvas.drawPoint(lastX, lastY, mPaint);
                }
            }
            
            if (mCurDown && NP > 0) {
                final PointerState ps = mPointers.get(0);
                canvas.drawLine(0, (int)ps.mCurY, getWidth(), (int)ps.mCurY, mTargetPaint);
                canvas.drawLine((int)ps.mCurX, 0, (int)ps.mCurX, getHeight(), mTargetPaint);
                int pressureLevel = (int)(ps.mCurPressure*255);
                mPaint.setARGB(255, pressureLevel, 128, 255-pressureLevel);
                canvas.drawPoint(ps.mCurX, ps.mCurY, mPaint);
                canvas.drawCircle(ps.mCurX, ps.mCurY, ps.mCurWidth, mPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int action = event.getAction();
            
            //Log.i("Pointer", "Motion: action=0x" + Integer.toHexString(action)
            //        + " pointers=" + event.getPointerCount());
            
            int NP = mPointers.size();
            
            //mRect.set(0, 0, getWidth(), mHeaderBottom+1);
            //invalidate(mRect);
            //if (mCurDown) {
            //    mRect.set(mCurX-mCurWidth-3, mCurY-mCurWidth-3,
            //            mCurX+mCurWidth+3, mCurY+mCurWidth+3);
            //} else {
            //    mRect.setEmpty();
            //}
            if (action == MotionEvent.ACTION_DOWN) {
                for (int p=0; p<NP; p++) {
                    final PointerState ps = mPointers.get(p);
                    ps.mXs.clear();
                    ps.mYs.clear();
                    ps.mVelocity = VelocityTracker.obtain();
                }
            }
            
            while (NP < event.getPointerCount()) {
                PointerState ps = new PointerState();
                ps.mVelocity = VelocityTracker.obtain();
                mPointers.add(ps);
                NP++;
            }
            
            if ((action&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                final PointerState ps = mPointers.get(
                        (action&MotionEvent.ACTION_POINTER_MASK)
                                >> MotionEvent.ACTION_POINTER_SHIFT);
                ps.mXs.clear();
                ps.mYs.clear();
                ps.mVelocity = VelocityTracker.obtain();
            }
            
            if (NP > event.getPointerCount()) {
                NP = event.getPointerCount();
            }
            
            mCurDown = action != MotionEvent.ACTION_UP
                    && action != MotionEvent.ACTION_CANCEL;
            
            for (int p=0; p<NP; p++) {
                final PointerState ps = mPointers.get(p);
                ps.mVelocity.addMovement(event);
                ps.mVelocity.computeCurrentVelocity(1);
                final int N = event.getHistorySize();
                for (int i=0; i<N; i++) {
                    ps.mXs.add(event.getHistoricalX(p, i));
                    ps.mYs.add(event.getHistoricalY(p, i));
                }
                ps.mXs.add(event.getX(p));
                ps.mYs.add(event.getY(p));
                ps.mCurX = (int)event.getX(p);
                ps.mCurY = (int)event.getY(p);
                //Log.i("Pointer", "Pointer #" + p + ": (" + ps.mCurX
                //        + "," + ps.mCurY + ")");
                ps.mCurPressure = event.getPressure(p);
                ps.mCurSize = event.getSize(p);
                ps.mCurWidth = (int)(ps.mCurSize*(getWidth()/3));
            }
            
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
