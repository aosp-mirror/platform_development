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

package com.example.android.keyframeanimation;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

/**
 * This example shows how to use AnimationDrawable to construct a keyframe animation where each
 * frame is shown for a specified duration.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=V3ksidLf7vA.
 */
public class KeyframeAnimation extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyframe_animation);

        ImageView imageview = (ImageView) findViewById(R.id.imageview);

        // Create the AnimationDrawable in which we will store all frames of the animation
        final AnimationDrawable animationDrawable = new AnimationDrawable();
        for (int i = 0; i < 10; ++i) {
            animationDrawable.addFrame(getDrawableForFrameNumber(i), 300);
        }
        // Run until we say stop
        animationDrawable.setOneShot(false);

        imageview.setImageDrawable(animationDrawable);

        // When the user clicks on the image, toggle the animation on/off
        imageview.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (animationDrawable.isRunning()) {
                    animationDrawable.stop();
                } else {
                    animationDrawable.start();
                }
            }
        });
    }

    /**
     * The 'frames' in this app are nothing more than a gray background with text indicating
     * the number of the frame.
     */
    private BitmapDrawable getDrawableForFrameNumber(int frameNumber) {
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.GRAY);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(80);
        paint.setColor(Color.BLACK);
        canvas.drawText("Frame " + frameNumber, 40, 220, paint);
        return new BitmapDrawable(getResources(), bitmap);
    }

}
