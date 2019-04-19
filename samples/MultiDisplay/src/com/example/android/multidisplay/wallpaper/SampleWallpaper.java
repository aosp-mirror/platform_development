/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.example.android.multidisplay.wallpaper;


import android.app.WallpaperColors;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManager.DisplayListener;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import java.util.Random;
import com.example.android.multidisplay.R;

public class SampleWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new MySampleEngine();
    }

    private class MySampleEngine extends Engine {
        private boolean mVisible = false;
        private DisplayMetrics mDisplayMetrics;
        private Display mDisplay;
        private Paint mPaint = new Paint();

        private final Handler mHandler = new Handler();
        private final Runnable mDrawRunner = this::draw;

        private String mShowingText;
        private final Rect mTextBounds = new Rect();

        private Bitmap mTipImage;

        private final Point mCircleShift = new Point();
        private final Point mCirclePosition = new Point();
        private float mCircleRadioShift;
        private final float MaxCircleRadioShift = 6f;
        private boolean mRadioRevert = false;

        private int mBackgroundColor = Color.BLACK;
        private int mPaintColor = Color.WHITE;

        private boolean mCircleDirectionX = false;
        private boolean mCircleDirectionY = false;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            initDisplay();
            updateDisplay();
            initPaint();
            genNewShift();
            genNewColor();
            mHandler.post(mDrawRunner);
        }

        @Override
        public void onDestroy() {
            final DisplayManager dm = getSystemService(DisplayManager.class);
            if (dm != null) {
                dm.unregisterDisplayListener(mDisplayListener);
            }
        }

        @Override
        public WallpaperColors onComputeColors() {
            super.onComputeColors();
            ColorDrawable drawable = new ColorDrawable(mBackgroundColor);
            drawable.setBounds(0, 0, mDisplayMetrics.widthPixels, mDisplayMetrics.widthPixels);
            return WallpaperColors.fromDrawable(drawable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                mHandler.post(mDrawRunner);
            } else {
                mHandler.removeCallbacks(mDrawRunner);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mCirclePosition.x = (int) event.getX();
                mCirclePosition.y = (int) event.getY();
                invertCircleDirectionIfNeeded();
            }
            super.onTouchEvent(event);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mDrawRunner);
        }

        private void draw() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            float centerX = (float) mDisplayMetrics.widthPixels/2;
            float centerY = (float) mDisplayMetrics.heightPixels/2;

            updateShift();
            invertCircleDirectionIfNeeded();

            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(mBackgroundColor);
                    if (mTipImage != null) {
                        canvas.drawBitmap(mTipImage, 0, 0, mPaint);
                    }
                    canvas.drawText(mShowingText, centerX - mTextBounds.exactCenterX(),
                        centerY - mTextBounds.exactCenterY(), mPaint);

                    canvas.drawCircle(mCirclePosition.x, mCirclePosition.y,
                        20.0f + mCircleRadioShift, mPaint);
                }
            } finally {
                if (canvas != null)
                    holder.unlockCanvasAndPost(canvas);
            }
            mHandler.removeCallbacks(mDrawRunner);
            if (mVisible) {
                mHandler.postDelayed(mDrawRunner, 40);
            }
        }

        private void invertCircleDirectionIfNeeded() {
            boolean invertX = mCirclePosition.x < 0
                || mCirclePosition.x > mDisplayMetrics.widthPixels;
            boolean invertY = mCirclePosition.y < 0
                || mCirclePosition.y > mDisplayMetrics.heightPixels;

            if (!invertX && !invertY) return;

            if (invertX) {
                mCircleDirectionX = mCirclePosition.x < 0;
            }
            if (invertY) {
                mCircleDirectionY = mCirclePosition.y < 0;
            }

            genNewShift();
            genNewColor();
        }

        private void updateShift() {
            mCirclePosition.x = mCircleDirectionX
                ? mCirclePosition.x + mCircleShift.x
                : mCirclePosition.x - mCircleShift.x;
            mCirclePosition.y = mCircleDirectionY
                ? mCirclePosition.y + mCircleShift.y
                : mCirclePosition.y - mCircleShift.y;

            mCircleRadioShift = mRadioRevert ? mCircleRadioShift + 1f : mCircleRadioShift - 1f;
            if (Math.abs(mCircleRadioShift) > MaxCircleRadioShift) {
                mRadioRevert = !mRadioRevert;
            }
        }

        private void genNewShift() {
            Random random = new Random();
            mCircleShift.x = Math.abs(random.nextInt(5));
            mCircleShift.y = Math.abs(5 - mCircleShift.x);
        }

        private void genNewColor() {
            final Random random = new Random();
            int br = random.nextInt(256);
            int bg = random.nextInt(256);
            int bb = random.nextInt(256);

            // Keep some contrast...
            int pg = Math.abs(bg - 128);
            int pr = Math.abs(br - 128);
            int pb = Math.abs(bb - 128);
            mBackgroundColor = Color.argb(255, br, bg, bb);
            mPaintColor = Color.argb(255, pr, pg, pb);
            mPaint.setColor(mPaintColor);
        }

        private void initDisplay() {
            // If we want to get display, use getDisplayContext().getSystemService so the
            // WindowManager is created for this context.
            final WindowManager wm = getDisplayContext().getSystemService(WindowManager.class);
            if (wm != null) {
                mDisplay = wm.getDefaultDisplay();
            }
            final DisplayManager dm = getSystemService(DisplayManager.class);
            if (dm != null) {
                dm.registerDisplayListener(mDisplayListener, null);
            }
        }

        private void updateDisplay() {
            // Use getDisplayContext() to get the context for current display.
            mDisplayMetrics = getDisplayContext().getResources().getDisplayMetrics();
            mCirclePosition.x = mDisplayMetrics.widthPixels/2;
            mCirclePosition.y = mDisplayMetrics.heightPixels/2 + 60;

            mShowingText = "densityDpi= " + mDisplayMetrics.densityDpi;
            if (mTipImage != null) {
                mTipImage.recycle();
                mTipImage = null;
            }
            mTipImage = BitmapFactory
                .decodeResource(getDisplayContext().getResources(), R.drawable.res_image);
            mPaint.getTextBounds(mShowingText, 0, mShowingText.length(), mTextBounds);
        }

        public MySampleEngine() {

        }

        private void initPaint() {
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(1f);
            mPaint.setTextSize(50f);
        }

        // Use DisplayListener to know display changed.
        private final DisplayListener mDisplayListener = new DisplayListener() {
            @Override
            public void onDisplayChanged(int displayId) {
                if (mDisplay.getDisplayId() == displayId) {
                    updateDisplay();
                }
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                // handle here or wait onDestroy
            }

            @Override
            public void onDisplayAdded(int displayId) {
            }
        };
    }
}
