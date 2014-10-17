/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.revealeffectbasic;

import com.example.android.common.logger.Log;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

/**
 * This sample shows a view that is revealed when a button is clicked.
 */
public class RevealEffectBasicFragment extends Fragment {

    private final static String TAG = "RevealEffectBasicFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.reveal_effect_basic, container, false);

        View button = rootView.findViewById(R.id.button);

        // Set a listener to reveal the view when clicked.
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View shape = rootView.findViewById(R.id.circle);

                // Create a reveal {@link Animator} that starts clipping the view from
                // the top left corner until the whole view is covered.
                Animator animator = ViewAnimationUtils.createCircularReveal(
                        shape,
                        0,
                        0,
                        0,
                        (float) Math.hypot(shape.getWidth(), shape.getHeight()));

                // Set a natural ease-in/ease-out interpolator.
                animator.setInterpolator(new AccelerateDecelerateInterpolator());

                // Finally start the animation
                animator.start();

                Log.d(TAG, "Starting Reveal animation");
            }
        });

        return rootView;
    }

}