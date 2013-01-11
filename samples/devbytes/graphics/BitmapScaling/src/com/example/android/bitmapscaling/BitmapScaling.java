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

package com.example.android.bitmapscaling;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

/**
 * This example shows how the use of BitmapOptions affects the resulting size of a loaded
 * bitmap. Sub-sampling can speed up load times and reduce the need for large bitmaps
 * in memory if your target bitmap size is much smaller, although it's good to understand
 * that you can't get specific Bitmap sizes, but rather power-of-two reductions in sizes.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=12cB7gnL6po.
 */
public class BitmapScaling extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_scaling);

        LinearLayout container = (LinearLayout) findViewById(R.id.scaledImageContainer);
        ImageView originalImageView = (ImageView) findViewById(R.id.originalImageHolder);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.jellybean_statue);
        originalImageView.setImageBitmap(bitmap);

        for (int i = 2; i < 10; ++i) {
            addScaledImageView(bitmap, i, container);
        }
    }

    private void addScaledImageView(Bitmap original, int sampleSize, LinearLayout container) {

        // inSampleSize tells the loader how much to scale the final image, which it does at
        // load time by simply reading less pixels for every pixel value in the final bitmap.
        // Note that it only scales by powers of two, so a value of two results in a bitmap
        // 1/2 the size of the original and a value of four results in a bitmap 1/4 the original
        // size. Intermediate values are rounded down, so a value of three results in a bitmap 1/2
        // the original size.

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = sampleSize;

        Bitmap scaledBitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.jellybean_statue, bitmapOptions);
        ImageView scaledImageView = new ImageView(this);
        scaledImageView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        scaledImageView.setImageBitmap(scaledBitmap);
        container.addView(scaledImageView);
    }
}
