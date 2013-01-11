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

package com.example.android.pictureviewer;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

/**
 * This example shows how to use ViewPropertyAnimator to get a cross-fade effect as new
 * bitmaps get installed in an ImageView.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=9XbKMUtVnJA.
 */
public class PictureViewer extends Activity {

    int mCurrentDrawable = 0;
    int drawableIDs[] = {
            R.drawable.p1,
            R.drawable.p2,
            R.drawable.p3,
            R.drawable.p4,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture_viewer);

        // This app works by having two views, which get faded in/out for the cross-fade effect
        final ImageView prevImageView = (ImageView) findViewById(R.id.prevImageView);
        final ImageView nextImageView = (ImageView) findViewById(R.id.nextImageView);
        prevImageView.setBackgroundColor(Color.TRANSPARENT);
        nextImageView.setBackgroundColor(Color.TRANSPARENT);

        // Setup default ViewPropertyAnimator durations for the two ImageViews
        prevImageView.animate().setDuration(1000);
        nextImageView.animate().setDuration(1000);

        // NOte that a real app would do this more robustly, and not just load all possible
        // bitmaps at onCreate() time.
        final BitmapDrawable drawables[] = new BitmapDrawable[drawableIDs.length];
        for (int i = 0; i < drawableIDs.length; ++i) {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                    drawableIDs[i]);
            drawables[i] = new BitmapDrawable(getResources(), bitmap);
        }
        prevImageView.setImageDrawable(drawables[0]);
        nextImageView.setImageDrawable(drawables[1]);

        prevImageView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Use ViewPropertyAnimator to fade the previous imageView out and the next one in
                prevImageView.animate().alpha(0).withLayer();
                nextImageView.animate().alpha(1).withLayer().
                        withEndAction(new Runnable() {
                    // When the animation ends, set up references to change the prev/next
                    // associations
                    @Override
                    public void run() {
                        mCurrentDrawable =
                                (mCurrentDrawable + 1) % drawables.length;
                        int nextDrawableIndex =
                                (mCurrentDrawable + 1) % drawables.length;
                        prevImageView.setImageDrawable(drawables[mCurrentDrawable]);
                        nextImageView.setImageDrawable(drawables[nextDrawableIndex]);
                        nextImageView.setAlpha(0f);
                        prevImageView.setAlpha(1f);
                    }
                });
            }
        });
    }

}
