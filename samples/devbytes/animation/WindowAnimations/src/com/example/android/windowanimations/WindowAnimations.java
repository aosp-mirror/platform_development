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

package com.example.android.windowanimations;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

/**
 * This example shows how to create custom Window animations to animate between different
 * sub-activities.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=Ho8vk61lVIU.
 */
public class WindowAnimations extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_window_animations);

        final Button defaultButton = (Button) findViewById(R.id.defaultButton);
        final Button translateButton = (Button) findViewById(R.id.translateButton);
        final Button scaleButton = (Button) findViewById(R.id.scaleButton);
        final ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);

        // By default, launching a sub-activity uses the system default for window animations
        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent subActivity = new Intent(WindowAnimations.this,
                        SubActivity.class);
                startActivity(subActivity);
            }
        });

        // Custom animations allow us to do things like slide the next activity in as we
        // slide this activity out
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Using the AnimatedSubActivity also allows us to animate exiting that
                // activity - see that activity for details
                Intent subActivity = new Intent(WindowAnimations.this,
                        AnimatedSubActivity.class);
                // The enter/exit animations for the two activities are specified by xml resources
                Bundle translateBundle =
                        ActivityOptions.makeCustomAnimation(WindowAnimations.this,
                        R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
                startActivity(subActivity, translateBundle);
            }
        });

        // Starting in Jellybean, you can provide an animation that scales up the new
        // activity from a given source rectangle
        scaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent subActivity = new Intent(WindowAnimations.this,
                        AnimatedSubActivity.class);
                Bundle scaleBundle = ActivityOptions.makeScaleUpAnimation(
                        v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
                startActivity(subActivity, scaleBundle);
            }
        });

        // Starting in Jellybean, you can also provide an animation that scales up the new
        // activity from a given bitmap, cross-fading between the starting and ending
        // representations. Here, we scale up from a thumbnail image of the final sub-activity
        thumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BitmapDrawable drawable = (BitmapDrawable) thumbnail.getDrawable();
                Bitmap bm = drawable.getBitmap();
                Intent subActivity = new Intent(WindowAnimations.this, AnimatedSubActivity.class);
                Bundle scaleBundle = ActivityOptions.makeThumbnailScaleUpAnimation(
                        thumbnail, bm, 0, 0).toBundle();
                startActivity(subActivity, scaleBundle);
            }
        });


    }

}
