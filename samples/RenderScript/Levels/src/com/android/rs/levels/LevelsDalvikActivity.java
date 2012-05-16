/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.rs.levels;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.renderscript.Matrix3f;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class LevelsDalvikActivity extends Activity
                                  implements SeekBar.OnSeekBarChangeListener {
    private final String TAG = "Img";
    private Bitmap mBitmapIn;
    private Bitmap mBitmapOut;
    private float mInBlack = 0.0f;
    private SeekBar mInBlackSeekBar;
    private float mOutBlack = 0.0f;
    private SeekBar mOutBlackSeekBar;
    private float mInWhite = 255.0f;
    private SeekBar mInWhiteSeekBar;
    private float mOutWhite = 255.0f;
    private SeekBar mOutWhiteSeekBar;
    private float mGamma = 1.0f;
    private SeekBar mGammaSeekBar;
    private float mSaturation = 1.0f;
    private SeekBar mSaturationSeekBar;
    private TextView mBenchmarkResult;
    private ImageView mDisplayView;

    Matrix3f satMatrix = new Matrix3f();
    float mInWMinInB;
    float mOutWMinOutB;
    float mOverInWMinInB;

    int mInPixels[];
    int mOutPixels[];

    private void setLevels() {
        mInWMinInB = mInWhite - mInBlack;
        mOutWMinOutB = mOutWhite - mOutBlack;
        mOverInWMinInB = 1.f / mInWMinInB;
    }

    private void setSaturation() {
        float rWeight = 0.299f;
        float gWeight = 0.587f;
        float bWeight = 0.114f;
        float oneMinusS = 1.0f - mSaturation;

        satMatrix.set(0, 0, oneMinusS * rWeight + mSaturation);
        satMatrix.set(0, 1, oneMinusS * rWeight);
        satMatrix.set(0, 2, oneMinusS * rWeight);
        satMatrix.set(1, 0, oneMinusS * gWeight);
        satMatrix.set(1, 1, oneMinusS * gWeight + mSaturation);
        satMatrix.set(1, 2, oneMinusS * gWeight);
        satMatrix.set(2, 0, oneMinusS * bWeight);
        satMatrix.set(2, 1, oneMinusS * bWeight);
        satMatrix.set(2, 2, oneMinusS * bWeight + mSaturation);
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar == mInBlackSeekBar) {
                mInBlack = (float)progress;
                setLevels();
            } else if (seekBar == mOutBlackSeekBar) {
                mOutBlack = (float)progress;
                setLevels();
            } else if (seekBar == mInWhiteSeekBar) {
                mInWhite = (float)progress + 127.0f;
                setLevels();
            } else if (seekBar == mOutWhiteSeekBar) {
                mOutWhite = (float)progress + 127.0f;
                setLevels();
            } else if (seekBar == mGammaSeekBar) {
                mGamma = (float)progress/100.0f;
                mGamma = Math.max(mGamma, 0.1f);
                mGamma = 1.0f / mGamma;
            } else if (seekBar == mSaturationSeekBar) {
                mSaturation = (float)progress / 50.0f;
                setSaturation();
            }

            filter();
            mDisplayView.invalidate();
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBitmapIn = loadBitmap(R.drawable.city);
        mBitmapOut = loadBitmap(R.drawable.city);

        mDisplayView = (ImageView) findViewById(R.id.display);
        mDisplayView.setImageBitmap(mBitmapOut);

        mInBlackSeekBar = (SeekBar)findViewById(R.id.inBlack);
        mInBlackSeekBar.setOnSeekBarChangeListener(this);
        mInBlackSeekBar.setMax(128);
        mInBlackSeekBar.setProgress(0);
        mOutBlackSeekBar = (SeekBar)findViewById(R.id.outBlack);
        mOutBlackSeekBar.setOnSeekBarChangeListener(this);
        mOutBlackSeekBar.setMax(128);
        mOutBlackSeekBar.setProgress(0);

        mInWhiteSeekBar = (SeekBar)findViewById(R.id.inWhite);
        mInWhiteSeekBar.setOnSeekBarChangeListener(this);
        mInWhiteSeekBar.setMax(128);
        mInWhiteSeekBar.setProgress(128);
        mOutWhiteSeekBar = (SeekBar)findViewById(R.id.outWhite);
        mOutWhiteSeekBar.setOnSeekBarChangeListener(this);
        mOutWhiteSeekBar.setMax(128);
        mOutWhiteSeekBar.setProgress(128);

        mGammaSeekBar = (SeekBar)findViewById(R.id.inGamma);
        mGammaSeekBar.setOnSeekBarChangeListener(this);
        mGammaSeekBar.setMax(150);
        mGammaSeekBar.setProgress(100);

        mSaturationSeekBar = (SeekBar)findViewById(R.id.inSaturation);
        mSaturationSeekBar.setOnSeekBarChangeListener(this);
        mSaturationSeekBar.setProgress(50);

        mBenchmarkResult = (TextView) findViewById(R.id.benchmarkText);
        mBenchmarkResult.setText("Result: not run");

        mInPixels = new int[mBitmapIn.getHeight() * mBitmapIn.getWidth()];
        mOutPixels = new int[mBitmapOut.getHeight() * mBitmapOut.getWidth()];
        mBitmapIn.getPixels(mInPixels, 0, mBitmapIn.getWidth(), 0, 0,
                            mBitmapIn.getWidth(), mBitmapIn.getHeight());

        setLevels();
        setSaturation();
        filter();
    }

    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeResource(getResources(), resource, options);
        Bitmap b2 = Bitmap.createBitmap(b.getWidth(), b.getHeight(), b.getConfig());
        Canvas c = new Canvas(b2);
        c.drawBitmap(b, 0, 0, null);
        b.recycle();
        return b2;
    }



    private void filter() {
        final float[] m = satMatrix.getArray();

        for (int i=0; i < mInPixels.length; i++) {
            float r = (float)(mInPixels[i] & 0xff);
            float g = (float)((mInPixels[i] >> 8) & 0xff);
            float b = (float)((mInPixels[i] >> 16) & 0xff);

            float tr = r * m[0] + g * m[3] + b * m[6];
            float tg = r * m[1] + g * m[4] + b * m[7];
            float tb = r * m[2] + g * m[5] + b * m[8];
            r = tr;
            g = tg;
            b = tb;

            if (r < 0.f) r = 0.f;
            if (r > 255.f) r = 255.f;
            if (g < 0.f) g = 0.f;
            if (g > 255.f) g = 255.f;
            if (b < 0.f) b = 0.f;
            if (b > 255.f) b = 255.f;

            r = (r - mInBlack) * mOverInWMinInB;
            g = (g - mInBlack) * mOverInWMinInB;
            b = (b - mInBlack) * mOverInWMinInB;

            if (mGamma != 1.0f) {
                r = (float)java.lang.Math.pow(r, mGamma);
                g = (float)java.lang.Math.pow(g, mGamma);
                b = (float)java.lang.Math.pow(b, mGamma);
            }

            r = (r * mOutWMinOutB) + mOutBlack;
            g = (g * mOutWMinOutB) + mOutBlack;
            b = (b * mOutWMinOutB) + mOutBlack;

            if (r < 0.f) r = 0.f;
            if (r > 255.f) r = 255.f;
            if (g < 0.f) g = 0.f;
            if (g > 255.f) g = 255.f;
            if (b < 0.f) b = 0.f;
            if (b > 255.f) b = 255.f;

            mOutPixels[i] = ((int)r) + (((int)g) << 8) + (((int)b) << 16)
                            + (mInPixels[i] & 0xff000000);
        }

        mBitmapOut.setPixels(mOutPixels, 0, mBitmapOut.getWidth(), 0, 0,
                             mBitmapOut.getWidth(), mBitmapOut.getHeight());
    }

    public void benchmark(View v) {
        filter();
        long t = java.lang.System.currentTimeMillis();
        filter();
        t = java.lang.System.currentTimeMillis() - t;
        mDisplayView.invalidate();
        mBenchmarkResult.setText("Result: " + t + " ms");
    }
}
