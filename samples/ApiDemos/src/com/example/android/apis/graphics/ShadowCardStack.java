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

package com.example.android.apis.graphics;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.android.apis.R;

import java.util.ArrayList;

public class ShadowCardStack extends Activity {

    private static final float X_SHIFT_DP = 1000;
    private static final float Y_SHIFT_DP = 50;
    private static final float Z_LIFT_DP = 8;
    private static final float ROTATE_DEGREES = 15;

    public AnimatorSet createSet(ArrayList<Animator> items, long startDelay) {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(items);
        set.setStartDelay(startDelay);
        return set;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shadow_card_stack);

        float density = getResources().getDisplayMetrics().density;

        final ViewGroup cardParent = (ViewGroup) findViewById(R.id.card_parent);

        final float X = X_SHIFT_DP * density;
        final float Y = Y_SHIFT_DP * density;
        final float Z = Z_LIFT_DP * density;

        ArrayList<Animator> towardAnimators = new ArrayList<Animator>();
        ArrayList<Animator> expandAnimators = new ArrayList<Animator>();
        ArrayList<Animator> moveAwayAnimators = new ArrayList<Animator>();
        ArrayList<Animator> moveBackAnimators = new ArrayList<Animator>();
        ArrayList<Animator> awayAnimators = new ArrayList<Animator>();
        ArrayList<Animator> collapseAnimators = new ArrayList<Animator>();

        final int max = cardParent.getChildCount();
        for (int i = 0; i < max; i++) {
            TextView card = (TextView) cardParent.getChildAt(i);
            card.setText("Card number " + i);

            float targetY = (i - (max-1) / 2.0f) * Y;
            Animator expand = ObjectAnimator.ofFloat(card, "translationY", targetY);
            expandAnimators.add(expand);

            Animator toward = ObjectAnimator.ofFloat(card, "translationZ", i * Z);
            toward.setStartDelay(200 * ((max) - i));
            towardAnimators.add(toward);

            card.setPivotX(X_SHIFT_DP);
            Animator rotateAway = ObjectAnimator.ofFloat(card, "rotationY",
                    i == 0 ? 0 : ROTATE_DEGREES);
            rotateAway.setStartDelay(200 * ((max) - i));
            rotateAway.setDuration(100);
            moveAwayAnimators.add(rotateAway);
            Animator slideAway = ObjectAnimator.ofFloat(card, "translationX",
                    i == 0 ? 0 : X);
            slideAway.setStartDelay(200 * ((max) - i));
            slideAway.setDuration(100);
            moveAwayAnimators.add(slideAway);

            Animator rotateBack = ObjectAnimator.ofFloat(card, "rotationY", 0);
            rotateBack.setStartDelay(200 * i);
            moveBackAnimators.add(rotateBack);
            Animator slideBack = ObjectAnimator.ofFloat(card, "translationX", 0);
            slideBack.setStartDelay(200 * i);
            moveBackAnimators.add(slideBack);

            Animator away = ObjectAnimator.ofFloat(card, "translationZ", 0);
            away.setStartDelay(200 * i);
            awayAnimators.add(away);

            Animator collapse = ObjectAnimator.ofFloat(card, "translationY", 0);
            collapseAnimators.add(collapse);
        }

        AnimatorSet totalSet = new AnimatorSet();
        totalSet.playSequentially(
                createSet(expandAnimators, 250),
                createSet(towardAnimators, 0),

                createSet(moveAwayAnimators, 250),
                createSet(moveBackAnimators, 0),

                createSet(awayAnimators, 250),
                createSet(collapseAnimators, 0));
        totalSet.start();
        totalSet.addListener(new RepeatListener(totalSet));
    }

    public static class RepeatListener implements Animator.AnimatorListener {
        final Animator mRepeatAnimator;
        public RepeatListener(Animator repeatAnimator) {
            mRepeatAnimator = repeatAnimator;
        }

        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            if (animation == mRepeatAnimator) {
                mRepeatAnimator.start();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    }
}
