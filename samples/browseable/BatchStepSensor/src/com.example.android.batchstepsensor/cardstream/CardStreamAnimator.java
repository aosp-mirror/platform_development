/*
* Copyright 2013 The Android Open Source Project
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


package com.example.android.batchstepsensor.cardstream;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.View;

/**
 * An abstract class which defines animators for CardStreamLinearLayout.
 */
abstract class CardStreamAnimator {

    protected float mSpeedFactor = 1.f;

    /**
     * Set speed factor of animations. Higher value means longer duration & slow animation.
     *
     * @param speedFactor speed type 1: SLOW, 2: NORMAL, 3:FAST
     */
    public void setSpeedFactor(float speedFactor) {
        mSpeedFactor = speedFactor;
    }

    /**
     * Define initial animation of each child which fired when a user rotate a screen.
     *
     * @param context
     * @return ObjectAnimator for initial animation
     */
    public abstract ObjectAnimator getInitalAnimator(Context context);

    /**
     * Define disappearing animation of a child which fired when a view is removed programmatically
     *
     * @param context
     * @return ObjectAnimator for disappearing animation
     */
    public abstract ObjectAnimator getDisappearingAnimator(Context context);

    /**
     * Define appearing animation of a child which fired when a view is added programmatically
     *
     * @param context
     * @return ObjectAnimator for appearing animation
     */
    public abstract ObjectAnimator getAppearingAnimator(Context context);

    /**
     * Define swipe-in (back to the origin position) animation of a child
     * which fired when a view is not moved enough to be removed.
     *
     * @param view   target view
     * @param deltaX delta distance by x-axis
     * @param deltaY delta distance by y-axis
     * @return ObjectAnimator for swipe-in animation
     */
    public abstract ObjectAnimator getSwipeInAnimator(View view, float deltaX, float deltaY);

    /**
     * Define swipe-out animation of a child
     * which fired when a view is removing by a user swipe action.
     *
     * @param view   target view
     * @param deltaX delta distance by x-axis
     * @param deltaY delta distance by y-axis
     * @return ObjectAnimator for swipe-out animation
     */
    public abstract ObjectAnimator getSwipeOutAnimator(View view, float deltaX, float deltaY);

    /**
     * A simple CardStreamAnimator implementation which is used to turn animations off.
     */
    public static class EmptyAnimator extends CardStreamAnimator {

        @Override
        public ObjectAnimator getInitalAnimator(Context context) {
            return null;
        }

        @Override
        public ObjectAnimator getDisappearingAnimator(Context context) {
            return null;
        }

        @Override
        public ObjectAnimator getAppearingAnimator(Context context) {
            return null;
        }

        @Override
        public ObjectAnimator getSwipeInAnimator(View view, float deltaX, float deltaY) {
            return null;
        }

        @Override
        public ObjectAnimator getSwipeOutAnimator(View view, float deltaX, float deltaY) {
            return null;
        }
    }

}

