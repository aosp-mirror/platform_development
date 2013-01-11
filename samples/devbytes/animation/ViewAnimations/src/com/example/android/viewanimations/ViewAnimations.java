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

package com.example.android.viewanimations;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * This example shows how to use pre-3.0 view Animation classes to create various animated UI
 * effects. See also the demo PropertyAnimations, which shows how this is done using the new
 * ObjectAnimator API introduced in Android 3.0.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=_UWXqFBF86U.
 */
public class ViewAnimations extends Activity {

    CheckBox mCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_animations);

        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        final Button alphaButton = (Button) findViewById(R.id.alphaButton);
        final Button translateButton = (Button) findViewById(R.id.translateButton);
        final Button rotateButton = (Button) findViewById(R.id.rotateButton);
        final Button scaleButton = (Button) findViewById(R.id.scaleButton);
        final Button setButton = (Button) findViewById(R.id.setButton);

        // Fade the button out and back in
        final AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(1000);

        // Move the button over and then back
        final TranslateAnimation translateAnimation =
                new TranslateAnimation(Animation.ABSOLUTE, 0,
                Animation.RELATIVE_TO_PARENT, 1,
                Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 100);
        translateAnimation.setDuration(1000);

        // Spin the button around in a full circle
        final RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        rotateAnimation.setDuration(1000);

        // Scale the button in X and Y.
        final ScaleAnimation scaleAnimation = new ScaleAnimation(1, 2, 1, 2);
        scaleAnimation.setDuration(1000);

        // Run the animations above in sequence on the final button. Looks horrible.
        final AnimationSet setAnimation = new AnimationSet(true);
        setAnimation.addAnimation(alphaAnimation);
        setAnimation.addAnimation(translateAnimation);
        setAnimation.addAnimation(rotateAnimation);
        setAnimation.addAnimation(scaleAnimation);

        setupAnimation(alphaButton, alphaAnimation, R.anim.alpha_anim);
        setupAnimation(translateButton, translateAnimation, R.anim.translate_anim);
        setupAnimation(rotateButton, rotateAnimation, R.anim.rotate_anim);
        setupAnimation(scaleButton, scaleAnimation, R.anim.scale_anim);
        setupAnimation(setButton, setAnimation, R.anim.set_anim);

    }

    private void setupAnimation(View view, final Animation animation,
            final int animationID) {
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // If the button is checked, load the animation from the given resource
                // id instead of using the passed-in animation paramter. See the xml files
                // for the details on those animations.
                v.startAnimation(mCheckBox.isChecked() ?
                        AnimationUtils.loadAnimation(ViewAnimations.this, animationID) :
                        animation);
            }
        });
    }
}
