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

package com.example.android.propertyanimations;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

/**
 * This example shows how to use property animations, specifically ObjectAnimator, to perform
 * various view animations. Compare this approach to that of the ViewAnimations demo, which
 * shows how to achieve similar effects using the pre-3.0 animation APIs.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on YouTube at https://www.youtube.com/watch?v=3UbJhmkeSig.
 */
public class PropertyAnimations extends Activity {

    CheckBox mCheckBox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_animations);

        mCheckBox = (CheckBox) findViewById(R.id.checkbox);
        final Button alphaButton = (Button) findViewById(R.id.alphaButton);
        final Button translateButton = (Button) findViewById(R.id.translateButton);
        final Button rotateButton = (Button) findViewById(R.id.rotateButton);
        final Button scaleButton = (Button) findViewById(R.id.scaleButton);
        final Button setButton = (Button) findViewById(R.id.setButton);

        // Fade the button out and back in
        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(alphaButton,
                View.ALPHA, 0);
        alphaAnimation.setRepeatCount(1);
        alphaAnimation.setRepeatMode(ValueAnimator.REVERSE);

        // Move the button over to the right and then back
        ObjectAnimator translateAnimation =
                ObjectAnimator.ofFloat(translateButton, View.TRANSLATION_X, 800);
        translateAnimation.setRepeatCount(1);
        translateAnimation.setRepeatMode(ValueAnimator.REVERSE);

        // Spin the button around in a full circle
        ObjectAnimator rotateAnimation =
                ObjectAnimator.ofFloat(rotateButton, View.ROTATION, 360);
        rotateAnimation.setRepeatCount(1);
        rotateAnimation.setRepeatMode(ValueAnimator.REVERSE);

        // Scale the button in X and Y. Note the use of PropertyValuesHolder to animate
        // multiple properties on the same object in parallel.
        PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat(View.SCALE_X, 2);
        PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 2);
        ObjectAnimator scaleAnimation =
                ObjectAnimator.ofPropertyValuesHolder(scaleButton, pvhX, pvhY);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(ValueAnimator.REVERSE);

        // Run the animations above in sequence
        AnimatorSet setAnimation = new AnimatorSet();
        setAnimation.play(translateAnimation).after(alphaAnimation).before(rotateAnimation);
        setAnimation.play(rotateAnimation).before(scaleAnimation);

        setupAnimation(alphaButton, alphaAnimation, R.animator.fade);
        setupAnimation(translateButton, translateAnimation, R.animator.move);
        setupAnimation(rotateButton, rotateAnimation, R.animator.spin);
        setupAnimation(scaleButton, scaleAnimation, R.animator.scale);
        setupAnimation(setButton, setAnimation, R.animator.combo);

    }

    private void setupAnimation(View view, final Animator animation, final int animationID) {
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // If the button is checked, load the animation from the given resource
                // id instead of using the passed-in animation parameter. See the xml files
                // for the details on those animations.
                if (mCheckBox.isChecked()) {
                    Animator anim = AnimatorInflater.loadAnimator(PropertyAnimations.this, animationID);
                    anim.setTarget(v);
                    anim.start();
                    return;
                }
                animation.start();
            }
        });
    }
}
