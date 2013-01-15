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

package com.example.android.crossfading;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

/**
 * This example shows how to use TransitionDrawable to perform a simple cross-fade effect
 * between two drawables.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=atH3o2uh_94.
 */
public class CrossFading extends Activity {

    int mCurrentDrawable = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cross_fading);

        final ImageView imageview = (ImageView) findViewById(R.id.imageview);

        // Create red and green bitmaps to cross-fade between
        Bitmap bitmap0 = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Bitmap bitmap1 = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap0);
        canvas.drawColor(Color.RED);
        canvas = new Canvas(bitmap1);
        canvas.drawColor(Color.GREEN);
        BitmapDrawable drawables[] = new BitmapDrawable[2];
        drawables[0] = new BitmapDrawable(getResources(), bitmap0);
        drawables[1] = new BitmapDrawable(getResources(), bitmap1);

        // Add the red/green bitmap drawables to a TransitionDrawable. They are layered
        // in the transition drawalbe. The cross-fade effect happens by fading one out and the
        // other in.
        final TransitionDrawable crossfader = new TransitionDrawable(drawables);
        imageview.setImageDrawable(crossfader);

        // Clicking on the drawable will cause the cross-fade effect to run. Depending on
        // which drawable is currently being shown, we either 'start' or 'reverse' the
        // transition, which determines which drawable is faded out/in during the transition.
        imageview.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCurrentDrawable == 0) {
                    crossfader.startTransition(500);
                    mCurrentDrawable = 1;
                } else {
                    crossfader.reverseTransition(500);
                    mCurrentDrawable = 0;
                }
            }
        });
    }
}
