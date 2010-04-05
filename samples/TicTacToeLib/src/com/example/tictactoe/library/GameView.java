/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.tictactoe.library;

import java.lang.reflect.Field;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

//-----------------------------------------------

public class GameView extends View {

    private final static String TAG = "GameView";

    private static final int MSG_FRAME = 1;

    public static final long FPS_MS = 1000/2;

    public static final int EMPTY = 0;
    public static final int CROSS = 1;
    public static final int CIRCLE = 2;


    /** Contains one of {@link #EMPTY}, {@link #CROSS} or {@link #CIRCLE} */
    private final int[] mData = new int[9];

    private final Rect mBgRect = new Rect();
    private final Rect mTempDst = new Rect();

    private int mSxy;
    private int mOffetX;
    private int mOffetY;
    private Paint mLinePaint;
    private Drawable mDrawableBg;


    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();

        mDrawableBg = getResources().getDrawable(R.drawable.lib_bg);

        mLinePaint = new Paint();
        mLinePaint.setColor(0xFFFFFFFF);
        mLinePaint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int sxy = mSxy;
        int s3  = sxy * 3;
        int x7 = mOffetX;
        int y7 = mOffetY;

        mDrawableBg.draw(canvas);

        for (int i = 0, k = sxy; i < 2; i++, k += sxy) {
            canvas.drawLine(x7    , y7 + k, x7 + s3, y7 + k , mLinePaint);
            canvas.drawLine(x7 + k, y7    , x7 + k , y7 + s3, mLinePaint);
        }

        for (int j = 0, k = 0, y = y7; j < 3; j++, y += sxy) {
            for (int i = 0, x = x7; i < 3; i++, x += sxy) {

            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, String.format("onSizeChanged: %dx%d", w, h));

        int sx = w / 3;
        int sy = h / 3;

        int size = sx < sy ? sx : sy;

        mSxy = size;
        mOffetX = (w - 3 * size) / 2;
        mOffetY = (h - 3 * size) / 2;

        mDrawableBg.setBounds(mOffetX, mOffetY,
                mOffetX + 3 * size, mOffetY + 3 * size);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        } else if (action == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            // TODO
            return true;
        }

        return false;
    }

    public Bitmap getResBitmap(int bmpResId) {
        Options opts = new Options();
        opts.inDither = false;

        Resources res = getResources();
        Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);
        return bmp;
    }
}


