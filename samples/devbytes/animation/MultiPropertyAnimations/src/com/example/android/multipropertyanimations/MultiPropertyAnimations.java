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

package com.example.android.multipropertyanimations;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

/**
 * This example shows various ways of animating multiple properties in parallel.
 *
 * Watch the associated video for this demo on the DevBytes channel of developer.android.com
 * or on the DevBytes playlist in the androiddevelopers channel on YouTube at
 * https://www.youtube.com/playlist?list=PLWz5rJ2EKKc_XOgcRukSoKKjewFJZrKV0.
 */
public class MultiPropertyAnimations extends Activity {

    private static final float TX_START = 0;
    private static final float TY_START = 0;
    private static final float TX_END = 400;
    private static final float TY_END = 200;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_property_animations);
    }

    /**
     * A very manual approach to animation uses a ValueAnimator to animate a fractional
     * value and then turns that value into the final property values which are then set
     * directly on the target object.
     */
    public void runValueAnimator(final View view) {
        ValueAnimator anim = ValueAnimator.ofFloat(0, 400);
        anim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float fraction = animator.getAnimatedFraction();
                view.setTranslationX(TX_START + fraction * (TX_END - TX_START));
                view.setTranslationY(TY_START + fraction * (TY_END - TY_START));
            }
        });
        anim.start();
    }

    /**
     * ViewPropertyAnimator is the cleanest and most efficient way of animating
     * View properties, even when there are multiple properties to be animated
     * in parallel.
     */
    public void runViewPropertyAnimator(View view) {
        view.animate().translationX(TX_END).translationY(TY_END);
    }

    /**
     * Multiple ObjectAnimator objects can be created and run in parallel.
     */
    public void runObjectAnimators(View view) {
        ObjectAnimator.ofFloat(view, View.TRANSLATION_X, TX_END).start();
        ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, TY_END).start();
        // Optional: use an AnimatorSet to run these in parallel
    }
    
    /**
     * Using PropertyValuesHolder objects enables the use of a single ObjectAnimator
     * per target, even when there are multiple properties being animated on that target.
     */
    public void runObjectAnimator(View view) {
        PropertyValuesHolder pvhTX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, TX_END);
        PropertyValuesHolder pvhTY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, TY_END);
        ObjectAnimator.ofPropertyValuesHolder(view, pvhTX, pvhTY).start();
    }
}
