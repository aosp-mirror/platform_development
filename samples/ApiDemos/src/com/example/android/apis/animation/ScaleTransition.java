/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.example.android.apis.animation;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 */
public class ScaleTransition extends Transition {
    private static final String PROPNAME_SCALE_X = "android:scale:x";
    private static final String PROPNAME_SCALE_Y = "android:scale:y";
    private static final String[] sTransitionProperties = {
            PROPNAME_SCALE_X,
            PROPNAME_SCALE_Y,
    };

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        transitionValues.values.put(PROPNAME_SCALE_X, transitionValues.view.getScaleX());
        transitionValues.values.put(PROPNAME_SCALE_Y, transitionValues.view.getScaleY());
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        Animator scaleXAnimator = createScaleAnimator(startValues, endValues, PROPNAME_SCALE_X,
                View.SCALE_X);
        Animator scaleYAnimator = createScaleAnimator(startValues, endValues, PROPNAME_SCALE_Y,
                View.SCALE_Y);
        if (scaleXAnimator == null) {
            return scaleYAnimator;
        } else if (scaleYAnimator == null) {
            return scaleXAnimator;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator);
        return animatorSet;
    }

    private Animator createScaleAnimator(TransitionValues startValues, TransitionValues endValues,
            String propertyName, Property<View, Float> scaleProperty) {
        float start = (Float)startValues.values.get(propertyName);
        float end = (Float)endValues.values.get(propertyName);
        if (start == end) {
            return null;
        }
        return ObjectAnimator.ofFloat(endValues.view, scaleProperty, start, end);
    }
}
