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

package com.example.android.tictactoe.library;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.Handler.Callback;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

//-----------------------------------------------

public class GameView extends View {

    public static final long FPS_MS = 1000/2;

    public enum State {
        UNKNOWN(-3),
        WIN(-2),
        EMPTY(0),
        PLAYER1(1),
        PLAYER2(2);

        private int mValue;

        private State(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static State fromInt(int i) {
            for (State s : values()) {
                if (s.getValue() == i) {
                    return s;
                }
            }
            return EMPTY;
        }
    }

    private static final int MARGIN = 4;
    private static final int MSG_BLINK = 1;

    private final Handler mHandler = new Handler(new MyHandler());

    private final Rect mSrcRect = new Rect();
    private final Rect mDstRect = new Rect();

    private int mSxy;
    private int mOffetX;
    private int mOffetY;
    private Paint mWinPaint;
    private Paint mLinePaint;
    private Paint mBmpPaint;
    private Bitmap mBmpPlayer1;
    private Bitmap mBmpPlayer2;
    private Drawable mDrawableBg;

    private ICellListener mCellListener;

    /** Contains one of {@link State#EMPTY}, {@link State#PLAYER1} or {@link State#PLAYER2}. */
    private final State[] mData = new State[9];

    private int mSelectedCell = -1;
    private State mSelectedValue = State.EMPTY;
    private State mCurrentPlayer = State.UNKNOWN;
    private State mWinner = State.EMPTY;

    private int mWinCol = -1;
    private int mWinRow = -1;
    private int mWinDiag = -1;

    private boolean mBlinkDisplayOff;
    private final Rect mBlinkRect = new Rect();



    public interface ICellListener {
        abstract void onCellSelected();
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();

        mDrawableBg = getResources().getDrawable(R.drawable.lib_bg);
        setBackgroundDrawable(mDrawableBg);

        mBmpPlayer1 = getResBitmap(R.drawable.lib_cross);
        mBmpPlayer2 = getResBitmap(R.drawable.lib_circle);

        if (mBmpPlayer1 != null) {
            mSrcRect.set(0, 0, mBmpPlayer1.getWidth() -1, mBmpPlayer1.getHeight() - 1);
        }

        mBmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mLinePaint = new Paint();
        mLinePaint.setColor(0xFFFFFFFF);
        mLinePaint.setStrokeWidth(5);
        mLinePaint.setStyle(Style.STROKE);

        mWinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWinPaint.setColor(0xFFFF0000);
        mWinPaint.setStrokeWidth(10);
        mWinPaint.setStyle(Style.STROKE);

        for (int i = 0; i < mData.length; i++) {
            mData[i] = State.EMPTY;
        }

        if (isInEditMode()) {
            // In edit mode (e.g. in the Eclipse ADT graphical layout editor)
            // we'll use some random data to display the state.
            Random rnd = new Random();
            for (int i = 0; i < mData.length; i++) {
                mData[i] = State.fromInt(rnd.nextInt(3));
            }
        }
    }

    public State[] getData() {
        return mData;
    }

    public void setCell(int cellIndex, State value) {
        mData[cellIndex] = value;
        invalidate();
    }

    public void setCellListener(ICellListener cellListener) {
        mCellListener = cellListener;
    }

    public int getSelection() {
        if (mSelectedValue == mCurrentPlayer) {
            return mSelectedCell;
        }

        return -1;
    }

    public State getCurrentPlayer() {
        return mCurrentPlayer;
    }

    public void setCurrentPlayer(State player) {
        mCurrentPlayer = player;
        mSelectedCell = -1;
    }

    public State getWinner() {
        return mWinner;
    }

    public void setWinner(State winner) {
        mWinner = winner;
    }

    /** Sets winning mark on specified column or row (0..2) or diagonal (0..1). */
    public void setFinished(int col, int row, int diagonal) {
        mWinCol = col;
        mWinRow = row;
        mWinDiag = diagonal;
    }

    //-----------------------------------------


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int sxy = mSxy;
        int s3  = sxy * 3;
        int x7 = mOffetX;
        int y7 = mOffetY;

        for (int i = 0, k = sxy; i < 2; i++, k += sxy) {
            canvas.drawLine(x7    , y7 + k, x7 + s3 - 1, y7 + k     , mLinePaint);
            canvas.drawLine(x7 + k, y7    , x7 + k     , y7 + s3 - 1, mLinePaint);
        }

        for (int j = 0, k = 0, y = y7; j < 3; j++, y += sxy) {
            for (int i = 0, x = x7; i < 3; i++, k++, x += sxy) {
                mDstRect.offsetTo(MARGIN+x, MARGIN+y);

                State v;
                if (mSelectedCell == k) {
                    if (mBlinkDisplayOff) {
                        continue;
                    }
                    v = mSelectedValue;
                } else {
                    v = mData[k];
                }

                switch(v) {
                case PLAYER1:
                    if (mBmpPlayer1 != null) {
                        canvas.drawBitmap(mBmpPlayer1, mSrcRect, mDstRect, mBmpPaint);
                    }
                    break;
                case PLAYER2:
                    if (mBmpPlayer2 != null) {
                        canvas.drawBitmap(mBmpPlayer2, mSrcRect, mDstRect, mBmpPaint);
                    }
                    break;
                }
            }
        }

        if (mWinRow >= 0) {
            int y = y7 + mWinRow * sxy + sxy / 2;
            canvas.drawLine(x7 + MARGIN, y, x7 + s3 - 1 - MARGIN, y, mWinPaint);

        } else if (mWinCol >= 0) {
            int x = x7 + mWinCol * sxy + sxy / 2;
            canvas.drawLine(x, y7 + MARGIN, x, y7 + s3 - 1 - MARGIN, mWinPaint);

        } else if (mWinDiag == 0) {
            // diagonal 0 is from (0,0) to (2,2)

            canvas.drawLine(x7 + MARGIN, y7 + MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + s3 - 1 - MARGIN, mWinPaint);

        } else if (mWinDiag == 1) {
            // diagonal 1 is from (0,2) to (2,0)

            canvas.drawLine(x7 + MARGIN, y7 + s3 - 1 - MARGIN,
                    x7 + s3 - 1 - MARGIN, y7 + MARGIN, mWinPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Keep the view squared
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int d = w == 0 ? h : h == 0 ? w : w < h ? w : h;
        setMeasuredDimension(d, d);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int sx = (w - 2 * MARGIN) / 3;
        int sy = (h - 2 * MARGIN) / 3;

        int size = sx < sy ? sx : sy;

        mSxy = size;
        mOffetX = (w - 3 * size) / 2;
        mOffetY = (h - 3 * size) / 2;

        mDstRect.set(MARGIN, MARGIN, size - MARGIN, size - MARGIN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            int sxy = mSxy;
            x = (x - MARGIN) / sxy;
            y = (y - MARGIN) / sxy;

            if (isEnabled() && x >= 0 && x < 3 && y >= 0 & y < 3) {
                int cell = x + 3 * y;

                State state = cell == mSelectedCell ? mSelectedValue : mData[cell];
                state = state == State.EMPTY ? mCurrentPlayer : State.EMPTY;

                stopBlink();

                mSelectedCell = cell;
                mSelectedValue = state;
                mBlinkDisplayOff = false;
                mBlinkRect.set(MARGIN + x * sxy, MARGIN + y * sxy,
                               MARGIN + (x + 1) * sxy, MARGIN + (y + 1) * sxy);

                if (state != State.EMPTY) {
                    // Start the blinker
                    mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
                }

                if (mCellListener != null) {
                    mCellListener.onCellSelected();
                }
            }

            return true;
        }

        return false;
    }

    public void stopBlink() {
        boolean hadSelection = mSelectedCell != -1 && mSelectedValue != State.EMPTY;
        mSelectedCell = -1;
        mSelectedValue = State.EMPTY;
        if (!mBlinkRect.isEmpty()) {
            invalidate(mBlinkRect);
        }
        mBlinkDisplayOff = false;
        mBlinkRect.setEmpty();
        mHandler.removeMessages(MSG_BLINK);
        if (hadSelection && mCellListener != null) {
            mCellListener.onCellSelected();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();

        Parcelable s = super.onSaveInstanceState();
        b.putParcelable("gv_super_state", s);

        b.putBoolean("gv_en", isEnabled());

        int[] data = new int[mData.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = mData[i].getValue();
        }
        b.putIntArray("gv_data", data);

        b.putInt("gv_sel_cell", mSelectedCell);
        b.putInt("gv_sel_val",  mSelectedValue.getValue());
        b.putInt("gv_curr_play", mCurrentPlayer.getValue());
        b.putInt("gv_winner", mWinner.getValue());

        b.putInt("gv_win_col", mWinCol);
        b.putInt("gv_win_row", mWinRow);
        b.putInt("gv_win_diag", mWinDiag);

        b.putBoolean("gv_blink_off", mBlinkDisplayOff);
        b.putParcelable("gv_blink_rect", mBlinkRect);

        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {

        if (!(state instanceof Bundle)) {
            // Not supposed to happen.
            super.onRestoreInstanceState(state);
            return;
        }

        Bundle b = (Bundle) state;
        Parcelable superState = b.getParcelable("gv_super_state");

        setEnabled(b.getBoolean("gv_en", true));

        int[] data = b.getIntArray("gv_data");
        if (data != null && data.length == mData.length) {
            for (int i = 0; i < data.length; i++) {
                mData[i] = State.fromInt(data[i]);
            }
        }

        mSelectedCell = b.getInt("gv_sel_cell", -1);
        mSelectedValue = State.fromInt(b.getInt("gv_sel_val", State.EMPTY.getValue()));
        mCurrentPlayer = State.fromInt(b.getInt("gv_curr_play", State.EMPTY.getValue()));
        mWinner = State.fromInt(b.getInt("gv_winner", State.EMPTY.getValue()));

        mWinCol = b.getInt("gv_win_col", -1);
        mWinRow = b.getInt("gv_win_row", -1);
        mWinDiag = b.getInt("gv_win_diag", -1);

        mBlinkDisplayOff = b.getBoolean("gv_blink_off", false);
        Rect r = b.getParcelable("gv_blink_rect");
        if (r != null) {
            mBlinkRect.set(r);
        }

        // let the blink handler decide if it should blink or not
        mHandler.sendEmptyMessage(MSG_BLINK);

        super.onRestoreInstanceState(superState);
    }

    //-----

    private class MyHandler implements Callback {
        public boolean handleMessage(Message msg) {
            if (msg.what == MSG_BLINK) {
                if (mSelectedCell >= 0 && mSelectedValue != State.EMPTY && mBlinkRect.top != 0) {
                    mBlinkDisplayOff = !mBlinkDisplayOff;
                    invalidate(mBlinkRect);

                    if (!mHandler.hasMessages(MSG_BLINK)) {
                        mHandler.sendEmptyMessageDelayed(MSG_BLINK, FPS_MS);
                    }
                }
                return true;
            }
            return false;
        }
    }

    private Bitmap getResBitmap(int bmpResId) {
        Options opts = new Options();
        opts.inDither = false;

        Resources res = getResources();
        Bitmap bmp = BitmapFactory.decodeResource(res, bmpResId, opts);

        if (bmp == null && isInEditMode()) {
            // BitmapFactory.decodeResource doesn't work from the rendering
            // library in Eclipse's Graphical Layout Editor. Use this workaround instead.

            Drawable d = res.getDrawable(bmpResId);
            int w = d.getIntrinsicWidth();
            int h = d.getIntrinsicHeight();
            bmp = Bitmap.createBitmap(w, h, Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, w - 1, h - 1);
            d.draw(c);
        }

        return bmp;
    }
}


