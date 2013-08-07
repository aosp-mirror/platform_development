/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.imagepixelization;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.util.Arrays;

/**
 * This application shows three different graphics/animation concepts.
 *
 * A pixelization effect is applied to an image with varying pixelization
 * factors to achieve an image that is pixelized to varying degrees. In
 * order to optimize the amount of image processing performed on the image
 * being pixelized, the pixelization effect only takes place if a predefined
 * amount of time has elapsed since the main image was last pixelized. The
 * effect is also applied when the user stops moving the seekbar.
 *
 * This application also shows how to use a ValueAnimator to achieve a
 * smooth self-animating seekbar.
 *
 * Lastly, this application shows a use case of AsyncTask where some
 * computation heavy processing can be moved onto a background thread,
 * so as to keep the UI completely responsive to user input.
 */
public class ImagePixelization extends Activity {

    final private static int SEEKBAR_ANIMATION_DURATION = 10000;
    final private static int TIME_BETWEEN_TASKS = 400;
    final private static int SEEKBAR_STOP_CHANGE_DELTA = 5;
    final private static float PROGRESS_TO_PIXELIZATION_FACTOR = 4000.0f;

    Bitmap mImageBitmap;
    ImageView mImageView;
    SeekBar mSeekBar;
    boolean mIsChecked = false;
    boolean mIsBuiltinPixelizationChecked = false;
    int mLastProgress = 0;
    long mLastTime = 0;
    Bitmap mPixelatedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_pixelization);

        mImageView = (ImageView) findViewById(R.id.pixelView);
        mSeekBar = (SeekBar)findViewById(R.id.seekbar);

        mImageBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        mImageView.setImageBitmap(mImageBitmap);

        mSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener =
            new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (Math.abs(mSeekBar.getProgress() - mLastProgress) > SEEKBAR_STOP_CHANGE_DELTA) {
                invokePixelization();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            checkIfShouldPixelize();
        }
    };

    /**
     * Checks if enough time has elapsed since the last pixelization call was invoked.
     * This prevents too many pixelization processes from being invoked at the same time
     * while previous ones have not yet completed.
     */
    public void checkIfShouldPixelize() {
        if ((System.currentTimeMillis() - mLastTime) > TIME_BETWEEN_TASKS) {
            invokePixelization();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_pixelization, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.animate:
                ObjectAnimator animator = ObjectAnimator.ofInt(mSeekBar, "progress", 0,
                        mSeekBar.getMax());
                animator.setInterpolator(new LinearInterpolator());
                animator.setDuration(SEEKBAR_ANIMATION_DURATION);
                animator.start();
                break;
            case R.id.checkbox:
                if (mIsChecked) {
                    item.setChecked(false);
                    mIsChecked = false;
                } else {
                    item.setChecked(true);
                    mIsChecked = true;
                }
                break;
            case R.id.builtin_pixelation_checkbox:
                mIsBuiltinPixelizationChecked = !mIsBuiltinPixelizationChecked;
                item.setChecked(mIsBuiltinPixelizationChecked);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * A simple pixelization algorithm. This uses a box blur algorithm where all the
     * pixels within some region are averaged, and that average pixel value is then
     * applied to all the pixels within that region. A higher pixelization factor
     * imposes a smaller number of regions of greater size. Similarly, a smaller
     * pixelization factor imposes a larger number of regions of smaller size.
     */
    public BitmapDrawable customImagePixelization(float pixelizationFactor, Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (mPixelatedBitmap == null || !(width == mPixelatedBitmap.getWidth() && height ==
                mPixelatedBitmap.getHeight())) {
            mPixelatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }

        int xPixels = (int) (pixelizationFactor * ((float)width));
        xPixels = xPixels > 0 ? xPixels : 1;
        int yPixels = (int)  (pixelizationFactor * ((float)height));
        yPixels = yPixels > 0 ? yPixels : 1;
        int pixel = 0, red = 0, green = 0, blue = 0, numPixels = 0;

        int[] bitmapPixels = new int[width * height];
        bitmap.getPixels(bitmapPixels, 0, width, 0, 0, width, height);

        int[] pixels = new int[yPixels * xPixels];

        int maxX, maxY;

        for (int y = 0; y < height; y+=yPixels) {
            for (int x = 0; x < width; x+=xPixels) {

                numPixels = red = green = blue = 0;

                maxX = Math.min(x + xPixels, width);
                maxY = Math.min(y + yPixels, height);

                for (int i = x; i < maxX; i++) {
                    for (int j = y; j < maxY; j++) {
                        pixel = bitmapPixels[j * width + i];
                        red += Color.red(pixel);
                        green += Color.green(pixel);
                        blue += Color.blue(pixel);
                        numPixels ++;
                    }
                }

                pixel = Color.rgb(red / numPixels, green / numPixels, blue / numPixels);

                Arrays.fill(pixels, pixel);

                int w = Math.min(xPixels, width - x);
                int h = Math.min(yPixels, height - y);

                mPixelatedBitmap.setPixels(pixels, 0 , w, x , y, w, h);
            }
        }

        return new BitmapDrawable(getResources(), mPixelatedBitmap);
    }

    /**
     * This method of image pixelization utilizes the bitmap scaling operations built
     * into the framework. By downscaling the bitmap and upscaling it back to its
     * original size (while setting the filter flag to false), the same effect can be
     * achieved with much better performance.
     */
    public BitmapDrawable builtInPixelization(float pixelizationFactor, Bitmap bitmap) {

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int downScaleFactorWidth = (int)(pixelizationFactor * width);
        downScaleFactorWidth = downScaleFactorWidth > 0 ? downScaleFactorWidth : 1;
        int downScaleFactorHeight = (int)(pixelizationFactor * height);
        downScaleFactorHeight = downScaleFactorHeight > 0 ? downScaleFactorHeight : 1;

        int downScaledWidth =  width / downScaleFactorWidth;
        int downScaledHeight = height / downScaleFactorHeight;

        Bitmap pixelatedBitmap = Bitmap.createScaledBitmap(bitmap, downScaledWidth,
                downScaledHeight, false);

        /* Bitmap's createScaledBitmap method has a filter parameter that can be set to either
         * true or false in order to specify either bilinear filtering or point sampling
         * respectively when the bitmap is scaled up or now.
         *
         * Similarly, a BitmapDrawable also has a flag to specify the same thing. When the
         * BitmapDrawable is applied to an ImageView that has some scaleType, the filtering
         * flag is taken into consideration. However, for optimization purposes, this flag was
         * ignored in BitmapDrawables before Jelly Bean MR1.
         *
         * Here, it is important to note that prior to JBMR1, two bitmap scaling operations
         * are required to achieve the pixelization effect. Otherwise, a BitmapDrawable
         * can be created corresponding to the downscaled bitmap such that when it is
         * upscaled to fit the ImageView, the upscaling operation is a lot faster since
         * it uses internal optimizations to fit the ImageView.
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), pixelatedBitmap);
            bitmapDrawable.setFilterBitmap(false);
            return bitmapDrawable;
        } else {
            Bitmap upscaled = Bitmap.createScaledBitmap(pixelatedBitmap, width, height, false);
            return new BitmapDrawable(getResources(), upscaled);
        }
    }

    /**
     * Invokes pixelization either on the main thread or on a background thread
     * depending on whether or not the checkbox was checked.
     */
    public void invokePixelization () {
        mLastTime = System.currentTimeMillis();
        mLastProgress = mSeekBar.getProgress();
        if (mIsChecked) {
            PixelizeImageAsyncTask asyncPixelateTask = new PixelizeImageAsyncTask();
            asyncPixelateTask.execute(mSeekBar.getProgress() / PROGRESS_TO_PIXELIZATION_FACTOR,
                    mImageBitmap);
        } else {
            mImageView.setImageDrawable(pixelizeImage(mSeekBar.getProgress()
                    / PROGRESS_TO_PIXELIZATION_FACTOR, mImageBitmap));
        }
    }

    /**
     *  Selects either the custom pixelization algorithm that sets and gets bitmap
     *  pixels manually or the one that uses built-in bitmap operations.
     */
    public BitmapDrawable pixelizeImage(float pixelizationFactor, Bitmap bitmap) {
        if (mIsBuiltinPixelizationChecked) {
            return builtInPixelization(pixelizationFactor, bitmap);
        } else {
            return customImagePixelization(pixelizationFactor, bitmap);
        }
    }

    /**
     * Implementation of the AsyncTask class showing how to run the
     * pixelization algorithm in the background, and retrieving the
     * pixelated image from the resulting operation.
     */
    private class PixelizeImageAsyncTask extends AsyncTask<Object, Void, BitmapDrawable> {

        @Override
        protected BitmapDrawable doInBackground(Object... params) {
            float pixelizationFactor = (Float)params[0];
            Bitmap originalBitmap = (Bitmap)params[1];
            return pixelizeImage(pixelizationFactor, originalBitmap);
        }

        @Override
        protected void onPostExecute(BitmapDrawable result) {
            mImageView.setImageDrawable(result);
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... values) {

        }
    }
}