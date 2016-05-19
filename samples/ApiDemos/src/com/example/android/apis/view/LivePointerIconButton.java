/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.apis.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.widget.Button;

public class LivePointerIconButton extends Button {
    public LivePointerIconButton(Context context) {
        this(context, null);
    }

    public LivePointerIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LivePointerIconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LivePointerIconButton(Context context, AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        int cursorSize = getHeight();

        Bitmap bitmap = Bitmap.createBitmap(cursorSize, cursorSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setARGB(255, 255, 255, 255);
        paint.setStyle(Paint.Style.STROKE);
        final int strokeWidth = 4;
        paint.setStrokeWidth(strokeWidth);

        // Draw a large circle filling the bitmap.
        final int outerCenterX = cursorSize / 2;
        final int outerCenterY = cursorSize / 2;
        final int outerRadius = cursorSize / 2 - strokeWidth;
        canvas.drawCircle(outerCenterX, outerCenterY, outerRadius, paint);

        // Compute relative offset of the mouse pointer from the view center.
        // It should be between -0.5 and 0.5.
        final float relativeX = (event.getX(pointerIndex) / getWidth()) - 0.5f;
        final float relativeY = (event.getY(pointerIndex) / getHeight()) - 0.5f;

        // Draw a smaller circle inside the large circle, offset towards the center of the view.
        final int innerCenterX = (int) (cursorSize * (1 - relativeX) / 2);
        final int innerCenterY = (int) (cursorSize * (1 - relativeY) / 2);
        final int innerRadius = cursorSize / 6;
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            // Fill the inner circle if the mouse button is down.
            paint.setStyle(Paint.Style.FILL);
        }
        canvas.drawCircle(innerCenterX, innerCenterY, innerRadius, paint);

        final int hotSpotX = bitmap.getWidth() / 2;
        final int hotSpotY = bitmap.getHeight() / 2;
        return PointerIcon.create(bitmap, hotSpotX, hotSpotY);
    }
}
