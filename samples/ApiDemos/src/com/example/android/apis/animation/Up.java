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
import android.animation.ObjectAnimator;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;

/**
 *
 */
public class Up extends Transition {
    private static final String PROPNAME_Z = "android:z:height";
    private static final String[] sTransitionProperties = {
            PROPNAME_Z,
    };

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues) {
        View view = transitionValues.view;
        transitionValues.values.put(PROPNAME_Z, view.getTranslationZ());
    }

    @Override
    public String[] getTransitionProperties() {
        return sTransitionProperties;
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        if (startValues == null || endValues == null) {
            return null;
        }
        final float startZ = (Float)startValues.values.get(PROPNAME_Z);
        final float endZ = (Float)endValues.values.get(PROPNAME_Z);
        if (startZ == endZ) {
            return null;
        }
        final View view = endValues.view;

        TransitionListener transitionListener = new TransitionListener() {
            boolean mCanceled = false;
            float mPausedZ;

            @Override
            public void onTransitionCancel(Transition transition) {
                view.setTranslationZ(endZ);
                mCanceled = true;
            }

            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                if (!mCanceled) {
                    view.setTranslationZ(endZ);
                }
            }

            @Override
            public void onTransitionPause(Transition transition) {
                mPausedZ = view.getTranslationZ();
                view.setTranslationZ(endZ);
            }

            @Override
            public void onTransitionResume(Transition transition) {
                view.setTranslationZ(mPausedZ);
            }
        };
        addListener(transitionListener);

        return ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, startZ, endZ);
    }
}
