/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.ninepatchlab;

import com.android.ninepatchlab.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.*;

public class NinePatchLab extends Activity {
    public NinePatchLab() {}

    Drawable[]  mButtons;
    Drawable[]  mBGs;
    float       mScale;
    
    boolean     mShowFPS = true;
    boolean     mDoDither = true;
    boolean     mDoFilter = true;
    int         mCurrBGIndex;

    private static final int FPS_COUNTER_LIMIT = 30;
    private int mFPSTime;
    private int mFPSCounter;
    private int mFPSAve;
    
    private View mView;
    
    private void updateTitle() {
        String title = "D=" + mDoDither + " F=" + mDoFilter;
        if (mShowFPS) {
            title += " FPS=" + mFPSAve;
        }
        setTitle(title);
    }

    private static Drawable make_custom_bg() {
        int[] colors = new int[] {
            //            0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,  0xFFFFFF00, 0xFFFF0000
            0xFFFF0000, 0xFF0000FF
        };
        return new GradientDrawable(GradientDrawable.Orientation.TR_BL,
                                    colors);
    }
    
    private static Drawable make_solid_bg() {
        return new ColorDrawable(0xFF008800);
    }
    
    private class NPView extends View {

        public NPView(Context context) {
            super(context);
            setFocusable(true);

            int[] bgs = new int[] {
                R.drawable.bg_grad_blue,
                R.drawable.bg_grad_green,
                R.drawable.bg_grad_grey,
                R.drawable.bg_grad_red,
                R.drawable.bg_grad_yellow,
            };
            int[] ids = new int[] {
                R.drawable.btn_dark_ticks_stretch_multiple,
                R.drawable.btn_dark_ticks_stretch_single,
                R.drawable.btn_transparent_ticks_stretch_multiple,
                R.drawable.btn_transparent_ticks_stretch_single,
                R.drawable.btn_light_ticks_stretch_multiple,
                R.drawable.btn_light_ticks_stretch_single,
            };
            
            mButtons = new Drawable[ids.length];
            mBGs = new Drawable[bgs.length + 2];
            
            Resources res = context.getResources();

            for (int i = 0; i < ids.length; i++) {
                mButtons[i] = res.getDrawable(ids[i]);
            }
            for (int i = 0; i < bgs.length; i++) {
                mBGs[i] = res.getDrawable(bgs[i]);
            }
            mBGs[bgs.length] = make_custom_bg();
            mBGs[bgs.length+1] = make_solid_bg();
            
            mScale = res.getDisplayMetrics().density;
        }

        private static final int MARGIN_X = 16;
        private static final int MARGIN_Y = 8;

        private void setDrawableFlags(Drawable dr) {
            dr.setDither(mDoDither);
            dr.setFilterBitmap(mDoFilter);
        }

        protected void onDraw(Canvas canvas) {
            long now = 0;
            if (mShowFPS) {
                now = SystemClock.uptimeMillis();
            }

            Drawable bg = mBGs[mCurrBGIndex];
            bg.setBounds(0, 0, getWidth(), getHeight());
            setDrawableFlags(bg);
            bg.draw(canvas);

            final int WIDTH = getWidth() - 2*MARGIN_X;
            final int HEIGHT = getHeight() - 2*MARGIN_Y;
            final int N = mButtons.length;
            final int gapSize = Math.round(mScale * 8);
            final int drHeight = (HEIGHT - (N - 1) * gapSize) / N;
            final int drWidth = WIDTH;
            
//            canvas.drawColor(0xFF5F810C);
            canvas.translate(MARGIN_X, MARGIN_Y);

            for (Drawable dr : mButtons) {
                dr.setBounds(0, 0, drWidth, drHeight);
                setDrawableFlags(dr);
                dr.draw(canvas);
                canvas.translate(0, drHeight + gapSize);
            }

            if (mShowFPS) {
                mFPSTime += (int)(SystemClock.uptimeMillis() - now);
                mFPSCounter += 1;
                if (mFPSCounter > FPS_COUNTER_LIMIT) {
                    mFPSAve = mFPSCounter * 1000 / mFPSTime;
                    updateTitle();
                    mFPSTime = 0;
                    mFPSCounter = 0;
                }
                invalidate();
            }
        }
    }

    private void toggleFPS() {
        mShowFPS = !mShowFPS;
        if (mShowFPS) {
            mFPSCounter = 0;
            mFPSTime = 0;
            mView.invalidate();
        }
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mDoFilter = !mDoFilter;
                updateTitle();
                mView.invalidate();
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                mDoDither = !mDoDither;
                updateTitle();
                mView.invalidate();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mCurrBGIndex = (mCurrBGIndex + 1) % mBGs.length;
                mView.invalidate();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mCurrBGIndex -= 1;
                if (mCurrBGIndex < 0) {
                    mCurrBGIndex = 0;
                }
                mView.invalidate();
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                toggleFPS();
                return true;
            case KeyEvent.KEYCODE_U:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return super.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mView = new NPView(this);
        setContentView(mView);
    }
        
}

