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
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.renderscript.Allocation;
import android.renderscript.Matrix3f;
import android.renderscript.RenderScript;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class LevelsRSActivity extends Activity
                              implements SeekBar.OnSeekBarChangeListener,
                                         TextureView.SurfaceTextureListener
{
    private final String TAG = "Img";
    private Bitmap mBitmapIn;
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
    private TextureView mDisplayView;

    Matrix3f satMatrix = new Matrix3f();
    float mInWMinInB;
    float mOutWMinOutB;
    float mOverInWMinInB;

    private RenderScript mRS;
    private Allocation mInPixelsAllocation;
    private Allocation mOutPixelsAllocation;
    private ScriptC_levels mScript;

    private void setLevels() {
        mInWMinInB = mInWhite - mInBlack;
        mOutWMinOutB = mOutWhite - mOutBlack;
        mOverInWMinInB = 1.f / mInWMinInB;

        mScript.set_inBlack(mInBlack);
        mScript.set_outBlack(mOutBlack);
        mScript.set_inWMinInB(mInWMinInB);
        mScript.set_outWMinOutB(mOutWMinOutB);
        mScript.set_overInWMinInB(mOverInWMinInB);
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
        mScript.set_colorMat(satMatrix);
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
                mScript.set_gamma(mGamma);
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
        setContentView(R.layout.rs);

        mBitmapIn = loadBitmap(R.drawable.city);
        mDisplayView = (TextureView) findViewById(R.id.display);

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

        mRS = RenderScript.create(this);
        mInPixelsAllocation = Allocation.createFromBitmap(mRS, mBitmapIn,
                                                          Allocation.MipmapControl.MIPMAP_NONE,
                                                          Allocation.USAGE_SCRIPT);
        mOutPixelsAllocation = Allocation.createTyped(mRS, mInPixelsAllocation.getType(),
                                                      Allocation.USAGE_SCRIPT |
                                                      Allocation.USAGE_IO_OUTPUT);
        mDisplayView.setSurfaceTextureListener(this);

        mScript = new ScriptC_levels(mRS, getResources(), R.raw.levels);
        mScript.set_gamma(mGamma);

        setSaturation();
        setLevels();
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
        mScript.forEach_root(mInPixelsAllocation, mOutPixelsAllocation);
        mOutPixelsAllocation.ioSend();
        mRS.finish();
    }

    public void benchmark(View v) {
        filter();
        long t = java.lang.System.currentTimeMillis();
        filter();
        t = java.lang.System.currentTimeMillis() - t;
        mDisplayView.invalidate();
        mBenchmarkResult.setText("Result: " + t + " ms");
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            mOutPixelsAllocation.setSurface(new Surface(surface));
        }
        filter();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (surface != null) {
            mOutPixelsAllocation.setSurface(new Surface(surface));
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mOutPixelsAllocation.setSurface(null);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
}
