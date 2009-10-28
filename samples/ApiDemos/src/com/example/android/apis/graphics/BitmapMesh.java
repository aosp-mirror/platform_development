/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.example.android.apis.graphics;

import com.example.android.apis.R;

import android.content.Context;
import android.graphics.*;
import android.graphics.utils.BoundaryPatch;
import android.os.Bundle;
import android.view.*;
import android.util.FloatMath;

public class BitmapMesh extends GraphicsActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SampleView(this));
    }

    private static class SampleView extends View {
        private static final int ROWS = 16;
        private static final int COLS = 16;

        private BoundaryPatch mPatch;
        private float[] mCubics;
        private Paint mPaint0;
        private Paint mPaint1;
        private int mCurrIndex = -1;

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
            mPatch.setCubicBoundary(mCubics, 0, ROWS, COLS);

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

        @Override public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mCurrIndex = findPtIndex(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mCurrIndex >= 0) {
                        mCubics[mCurrIndex + 0] = x;
                        mCubics[mCurrIndex + 1] = y;
                        mPatch.setCubicBoundary(mCubics, 0, ROWS, COLS);
                        invalidate();
                    }
                    break;
            }
            return true;
        }
    }
}

