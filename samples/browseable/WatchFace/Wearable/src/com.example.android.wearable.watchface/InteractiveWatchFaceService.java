/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.wearable.watchface;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

/**
 * Demostrates interactive watch face capabilities, i.e., touching the display and registering
 * three different events: touch, touch-cancel and tap. The watch face UI will show the count of
 * these events as they occur. See the {@code onTapCommand} below.
 */
public class InteractiveWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "InteractiveWatchFace";

    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private Paint mTextPaint;
        private final Paint mPeekCardBackgroundPaint = new Paint();

        private float mXOffset;
        private float mYOffset;
        private float mTextSpacingHeight;
        private int mScreenTextColor = Color.WHITE;

        private int mTouchCommandTotal;
        private int mTouchCancelCommandTotal;
        private int mTapCommandTotal;

        private int mTouchCoordinateX;
        private int mTouchCoordinateY;

        private final Rect mCardBounds = new Rect();

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            /** Accepts tap events via WatchFaceStyle (setAcceptsTapEvents(true)). */
            setWatchFaceStyle(new WatchFaceStyle.Builder(InteractiveWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = InteractiveWatchFaceService.this.getResources();
            mTextSpacingHeight = resources.getDimension(R.dimen.interactive_text_size);

            mTextPaint = new Paint();
            mTextPaint.setColor(mScreenTextColor);
            mTextPaint.setTypeface(BOLD_TYPEFACE);
            mTextPaint.setAntiAlias(true);

            mTouchCommandTotal = 0;
            mTouchCancelCommandTotal = 0;
            mTapCommandTotal = 0;

            mTouchCoordinateX = 0;
            mTouchCoordinateX = 0;
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onApplyWindowInsets: " + (insets.isRound() ? "round" : "square"));
            }
            super.onApplyWindowInsets(insets);

            /** Loads offsets / text size based on device type (square vs. round). */
            Resources resources = InteractiveWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(
                    isRound ? R.dimen.interactive_x_offset_round : R.dimen.interactive_x_offset);
            mYOffset = resources.getDimension(
                    isRound ? R.dimen.interactive_y_offset_round : R.dimen.interactive_y_offset);

            float textSize = resources.getDimension(
                    isRound ? R.dimen.interactive_text_size_round : R.dimen.interactive_text_size);

            mTextPaint.setTextSize(textSize);
        }

        @Override
        public void onPeekCardPositionUpdate(Rect bounds) {
            super.onPeekCardPositionUpdate(bounds);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPeekCardPositionUpdate: " + bounds);
            }
            super.onPeekCardPositionUpdate(bounds);
            if (!bounds.equals(mCardBounds)) {
                mCardBounds.set(bounds);
                invalidate();
            }
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);

            boolean burnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
            mTextPaint.setTypeface(burnInProtection ? NORMAL_TYPEFACE : BOLD_TYPEFACE);

            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: burn-in protection = " + burnInProtection
                        + ", low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }

            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mTextPaint.setAntiAlias(antiAlias);
            }
            invalidate();
        }

        /*
         * Captures tap event (and tap type) and increments correct tap type total.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Tap Command: " + tapType);
            }

            mTouchCoordinateX = x;
            mTouchCoordinateY = y;

            switch(tapType) {
                case TAP_TYPE_TOUCH:
                    mTouchCommandTotal++;
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    mTouchCancelCommandTotal++;
                    break;
                case TAP_TYPE_TAP:
                    mTapCommandTotal++;
                    break;
            }

            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /** Draws background */
            canvas.drawColor(Color.BLACK);

            canvas.drawText(
                    "TAP: " + String.valueOf(mTapCommandTotal),
                    mXOffset,
                    mYOffset,
                    mTextPaint);

            canvas.drawText(
                    "CANCEL: " + String.valueOf(mTouchCancelCommandTotal),
                    mXOffset,
                    mYOffset + mTextSpacingHeight,
                    mTextPaint);

            canvas.drawText(
                    "TOUCH: " + String.valueOf(mTouchCommandTotal),
                    mXOffset,
                    mYOffset + (mTextSpacingHeight * 2),
                    mTextPaint);

            canvas.drawText(
                    "X, Y: " + mTouchCoordinateX + ", " + mTouchCoordinateY,
                    mXOffset,
                    mYOffset + (mTextSpacingHeight * 3),
                    mTextPaint
            );

            /** Covers area under peek card */
            if (isInAmbientMode()) {
                canvas.drawRect(mCardBounds, mPeekCardBackgroundPaint);
            }
        }
    }
}
