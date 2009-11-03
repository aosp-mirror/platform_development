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

package com.android.graphicslab;

import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.utils.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;

public class GraphicsLab extends Activity {
    public GraphicsLab() {}

    private int mCurrView = 1;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(new SampleView(this));
//        setTitle("Graphics Lab");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (mCurrView == 1) {
                    setContentView(new SampleView2(this));
                    mCurrView = 2;
                } else {
                    setContentView(new SampleView(this));
                    mCurrView = 1;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    private static class SampleView2 extends View {
        private static final int ROWS = 16;
        private static final int COLS = 16;
        private static final int UNSTRETCH_MSEC = 250;
        
        private Interpolator mInterp;
        private BoundaryPatch mPatch;
        private float[] mCubics;
        private float[] mOrig = new float[24];
        private Paint mPaint0;
        private Paint mPaint1;
        private int mCurrIndex = -1;
        private float mPrevX;
        private float mPrevY;
        
        public SampleView2(Context context) {
            super(context);
            setFocusable(true);
            
            Bitmap bm = BitmapFactory.decodeResource(getResources(),
                                                     R.drawable.news_img);
            
            mPatch = new BoundaryPatch();
            mPatch.setTexture(bm);
            
            float unit = 90;
            mCubics = new float[] {
                0, 0, 1, 0, 2, 0,
                3, 0, 3, 1, 3, 2,
                3, 3, 2, 3, 1, 3,
                0, 3, 0, 2, 0, 1
            };
            for (int i = 0; i < 24; i++) {
                mCubics[i] *= 90;
                mCubics[i] += 20;
            }
            rebuildPatch();
            
            mPaint0 = new Paint();
            mPaint0.setAntiAlias(true);
            mPaint0.setStrokeWidth(12);
            mPaint0.setStrokeCap(Paint.Cap.ROUND);
            mPaint1 = new Paint(mPaint0);
            mPaint1.setColor(0xFFFFFFFF);
            mPaint1.setStrokeWidth(10);
        }
        
        @Override
        protected void onSizeChanged(int nw, int nh, int ow, int oh) {
            float[] pts = mCubics;
            float x1 = nw*0.3333f;
            float y1 = nh*0.3333f;
            float x2 = nw*0.6667f;
            float y2 = nh*0.6667f;
            pts[0*2+0] = 0;  pts[0*2+1] = 0;
            pts[1*2+0] = x1; pts[1*2+1] = 0;
            pts[2*2+0] = x2; pts[2*2+1] = 0;

            pts[3*2+0] = nw; pts[3*2+1] = 0;
            pts[4*2+0] = nw; pts[4*2+1] = y1;
            pts[5*2+0] = nw; pts[5*2+1] = y2;

            pts[6*2+0] = nw; pts[6*2+1] = nh;
            pts[7*2+0] = x2; pts[7*2+1] = nh;
            pts[8*2+0] = x1; pts[8*2+1] = nh;

            pts[9*2+0] = 0;  pts[9*2+1] = nh;
            pts[10*2+0] = 0; pts[10*2+1] = y2;
            pts[11*2+0] = 0; pts[11*2+1] = y1;
            
            System.arraycopy(pts, 0, mOrig, 0, 24);
            rebuildPatch();
        }

        @Override protected void onDraw(Canvas canvas) {
            if (mInterp != null) {
                int now = (int)SystemClock.uptimeMillis();
                Interpolator.Result result = mInterp.timeToValues(now, mCubics);
                if (result != Interpolator.Result.NORMAL) {
                    mInterp = null;
                } else {
                    invalidate();
                }
                rebuildPatch();
            }
            mPatch.draw(canvas);
        }

        private void rebuildPatch() {
            mPatch.setCubicBoundary(mCubics, 0, ROWS, COLS);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    System.arraycopy(mOrig, 0, mCubics, 0, 24);
                    mPrevX = x;
                    mPrevY = y;
                    break;
                case MotionEvent.ACTION_MOVE: {
                    float scale = 1.5f;
                    float dx = (x - mPrevX) * scale;
                    float dy = (y - mPrevY) * scale;
                    int index;

                    if (dx < 0) {
                        index = 10;
                    } else {
                        index = 4;
                    }
                    mCubics[index*2 + 0] = mOrig[index*2 + 0] + dx;
                    mCubics[index*2 + 2] = mOrig[index*2 + 2] + dx;
                    
                    if (dy < 0) {
                        index = 1;
                    } else {
                        index = 7;
                    }
                    mCubics[index*2 + 1] = mOrig[index*2 + 1] + dy;
                    mCubics[index*2 + 3] = mOrig[index*2 + 3] + dy;
        
                    rebuildPatch();
                    invalidate();
                } break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    int start = (int)SystemClock.uptimeMillis();
                    mInterp = new Interpolator(24);
                    mInterp.setKeyFrame(0, start, mCubics,
                                        new float[] { 0, 0.5f, 0.5f, 1 });
                    mInterp.setKeyFrame(1, start + UNSTRETCH_MSEC, mOrig
                                        );
                    invalidate();
                } break;
            }
            return true;
        }
    }

    private static class SampleView extends View {
        private static final int ROWS = 16;
        private static final int COLS = 16;
        
        private BoundaryPatch mPatch;
        private float[] mCubics;
        private float[] mOrig = new float[24];
        private Paint mPaint0;
        private Paint mPaint1;
        private int mCurrIndex = -1;
        private float mPrevX;
        private float mPrevY;
        
        public SampleView(Context context) {
        super(context);
        setFocusable(true);
        
        Bitmap bm = BitmapFactory.decodeResource(getResources(),
                                                 R.drawable.beach);
        
        mPatch = new BoundaryPatch();
        mPatch.setTexture(bm);
        
        float unit = 90;
        mCubics = new float[] {
            0, 0, 1, 0, 2, 0,
            3, 0, 3, 1, 3, 2,
            3, 3, 2, 3, 1, 3,
            0, 3, 0, 2, 0, 1
        };
        for (int i = 0; i < 24; i++) {
            mCubics[i] *= 90;
            mCubics[i] += 20;
        }
        rebuildPatch();
        
        mPaint0 = new Paint();
        mPaint0.setAntiAlias(true);
        mPaint0.setStrokeWidth(12);
        mPaint0.setStrokeCap(Paint.Cap.ROUND);
        mPaint1 = new Paint(mPaint0);
        mPaint1.setColor(0xFFFFFFFF);
        mPaint1.setStrokeWidth(10);
    }
    
    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFCCCCCC);
        mPatch.draw(canvas);
        canvas.drawPoints(mCubics, mPaint0);
        canvas.drawPoints(mCubics, mPaint1);
    }
    
    private void rebuildPatch() {
        mPatch.setCubicBoundary(mCubics, 0, ROWS, COLS);
    }
    
    private int findPtIndex(float x, float y) {
        final float tolerance = 25;
        final float[] pts = mCubics;
        for (int i = 0; i < (pts.length >> 1); i++) {
            if (Math.abs(pts[i*2 + 0] - x) <= tolerance &&
                Math.abs(pts[i*2 + 1] - y) <= tolerance) {
                return i*2;
            }
        }
        return -1;
    }
    
    private void offsetPts(float dx, float dy) {
        final float[] pts = mCubics;
        for (int i = 0; i < (pts.length >> 1); i++) {
            pts[i*2 + 0] += dx;
            pts[i*2 + 1] += dy;
        }
        rebuildPatch();
    }
    
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mCurrIndex = findPtIndex(x, y);
                mPrevX = x;
                mPrevY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCurrIndex >= 0) {
                    mCubics[mCurrIndex + 0] = x;
                    mCubics[mCurrIndex + 1] = y;
                    mPatch.setCubicBoundary(mCubics, 0, ROWS, COLS);
                } else {
                    offsetPts(x - mPrevX, y - mPrevY);
                    mPrevX = x;
                    mPrevY = y;
                }
                invalidate();
                break;
        }
        return true;
    }
}
}

