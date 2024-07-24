/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.vdmdemo.demos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

/** A view for drawing on the screen with a stylus. */
public class StylusDrawingView extends View {

    private final Path mDrawPath = new Path();
    private final Paint mCanvasPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint mDrawPaint = new Paint();
    private Canvas mDrawCanvas;
    private Bitmap mCanvasBitmap;

    public StylusDrawingView(Context context) {
        super(context);
        init();
    }

    public StylusDrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StylusDrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public StylusDrawingView(
            Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mDrawPaint.setAntiAlias(true);
        mDrawPaint.setStyle(Paint.Style.STROKE);
        mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
        mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        setBackgroundColor(Color.WHITE);

        Drawable hoverPointer = getContext().getResources().getDrawable(R.drawable.circle, null);
        setOnHoverListener((v, e) -> {
            Rect rect = new Rect();
            StylusDrawingView.this.getGlobalVisibleRect(rect);
            int x = (int) e.getX() + rect.left;
            int y = (int) e.getY() + rect.top;
            hoverPointer.setBounds(new Rect(x - 10, y - 10, x + 10, y + 10));
            switch (e.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    getRootView().getOverlay().add(hoverPointer);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    getRootView().getOverlay().clear();
                    break;
            }
            return true;
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mDrawCanvas = new Canvas(mCanvasBitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mCanvasBitmap, 0 , 0, mCanvasPaint);
        canvas.drawPath(mDrawPath, mDrawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO: do something fun with tilt and orientation

        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
            mDrawPaint.setStrokeWidth((int) Math.max(3, Math.pow(50, event.getPressure())));
            if (event.getButtonState() == MotionEvent.BUTTON_STYLUS_PRIMARY) {
                mDrawPaint.setColor(Color.MAGENTA);
            } else if (event.getButtonState() == MotionEvent.BUTTON_STYLUS_SECONDARY) {
                mDrawPaint.setColor(Color.YELLOW);
            } else {
                mDrawPaint.setColor(Color.CYAN);
            }
        } else if (event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER) {
            mDrawPaint.setStrokeWidth(100);
            mDrawPaint.setColor(Color.WHITE);
            mDrawPaint.setStrokeJoin(Paint.Join.ROUND);
            mDrawPaint.setStrokeCap(Paint.Cap.ROUND);
        } else {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                mDrawPath.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                mDrawPath.lineTo(x, y);
                mDrawCanvas.drawPath(mDrawPath, mDrawPaint);
                mDrawPath.reset();
                mDrawPath.moveTo(x, y);
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
}
