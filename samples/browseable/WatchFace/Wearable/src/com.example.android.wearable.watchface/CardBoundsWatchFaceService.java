/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * Proof of concept sample watch face that demonstrates how a watch face can detect where the peek
 * card is. This watch face draws a border around the area where the peeking card is.
 */
public class CardBoundsWatchFaceService extends CanvasWatchFaceService {

    private static final String TAG = "CardBoundsWatchFace";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        static final int BORDER_WIDTH_PX = 5;

        final Rect mCardBounds = new Rect();
        final Paint mPaint = new Paint();

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(CardBoundsWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .build());
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
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
        public void onDraw(Canvas canvas, Rect bounds) {
            // Clear screen.
            canvas.drawColor(isInAmbientMode() ? Color.BLACK : Color.BLUE);

            // Draw border around card in interactive mode.
            if (!isInAmbientMode()) {
                mPaint.setColor(Color.MAGENTA);
                canvas.drawRect(mCardBounds.left - BORDER_WIDTH_PX,
                        mCardBounds.top - BORDER_WIDTH_PX,
                        mCardBounds.right + BORDER_WIDTH_PX,
                        mCardBounds.bottom + BORDER_WIDTH_PX, mPaint);
            }

            // Fill area under card.
            mPaint.setColor(isInAmbientMode() ? Color.RED : Color.GREEN);
            canvas.drawRect(mCardBounds, mPaint);
        }
    }
}
