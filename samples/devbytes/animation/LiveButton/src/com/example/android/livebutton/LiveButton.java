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

package com.example.android.livebutton;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

/**
 * This app shows a simple application of anticipation and follow-through techniques as
 * the button animates into its pressed state and animates back out of it, overshooting
 * end state before resolving.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the androiddevelopers channel on YouTube at
 * https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0.
 */
public class LiveButton extends Activity {
    
    DecelerateInterpolator sDecelerator = new DecelerateInterpolator();
    OvershootInterpolator sOvershooter = new OvershootInterpolator(10f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overshoot);
        
        final Button clickMeButton = (Button) findViewById(R.id.clickMe);
        clickMeButton.animate().setDuration(200);
        
        clickMeButton.setOnTouchListener(new View.OnTouchListener() {
            
            @Override
            public boolean onTouch(View arg0, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
                    clickMeButton.animate().setInterpolator(sDecelerator).
                            scaleX(.7f).scaleY(.7f);
                } else if (arg1.getAction() == MotionEvent.ACTION_UP) {
                    clickMeButton.animate().setInterpolator(sOvershooter).
                            scaleX(1f).scaleY(1f);
                }
                return false;
            }
        });
        
    }
}
