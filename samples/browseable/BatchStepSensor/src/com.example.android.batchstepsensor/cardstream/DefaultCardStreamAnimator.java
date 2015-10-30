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
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.BounceInterpolator;

class DefaultCardStreamAnimator extends CardStreamAnimator {

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public ObjectAnimator getDisappearingAnimator(Context context){

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(new Object(),
                PropertyValuesHolder.ofFloat("alpha", 1.f, 0.f),
                PropertyValuesHolder.ofFloat("scaleX", 1.f, 0.f),
                PropertyValuesHolder.ofFloat("scaleY", 1.f, 0.f),
                PropertyValuesHolder.ofFloat("rotation", 0.f, 270.f));

        animator.setDuration((long) (200 * mSpeedFactor));
        return animator;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    public ObjectAnimator getAppearingAnimator(Context context){

        final Point outPoint = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(outPoint);

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(new Object(),
                PropertyValuesHolder.ofFloat("alpha", 0.f, 1.f),
                PropertyValuesHolder.ofFloat("translationY", outPoint.y / 2.f, 0.f),
                PropertyValuesHolder.ofFloat("rotation", -45.f, 0.f));

        animator.setDuration((long) (200 * mSpeedFactor));
        return animator;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    @Override
    public ObjectAnimator getInitalAnimator(Context context){

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(new Object(),
                PropertyValuesHolder.ofFloat("alpha", 0.5f, 1.f),
                PropertyValuesHolder.ofFloat("rotation", 60.f, 0.f));

        animator.setDuration((long) (200 * mSpeedFactor));
        return animator;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public ObjectAnimator getSwipeInAnimator(View view, float deltaX, float deltaY){

        float deltaXAbs = Math.abs(deltaX);

        float fractionCovered = 1.f - (deltaXAbs / view.getWidth());
        long duration = Math.abs((int) ((1 - fractionCovered) * 200 * mSpeedFactor));

        // Animate position and alpha of swiped item

        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("alpha", 1.f),
                PropertyValuesHolder.ofFloat("translationX", 0.f),
                PropertyValuesHolder.ofFloat("rotationY", 0.f));

        animator.setDuration(duration).setInterpolator(new BounceInterpolator());

        return  animator;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public ObjectAnimator getSwipeOutAnimator(View view, float deltaX, float deltaY){

        float endX;
        float endRotationY;

        float deltaXAbs = Math.abs(deltaX);

        float fractionCovered = 1.f - (deltaXAbs / view.getWidth());
        long duration = Math.abs((int) ((1 - fractionCovered) * 200 * mSpeedFactor));

        endX = deltaX < 0 ? -view.getWidth() : view.getWidth();
        if (deltaX > 0)
            endRotationY = -15.f;
        else
            endRotationY = 15.f;

        // Animate position and alpha of swiped item
        return ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat("alpha", 0.f),
                PropertyValuesHolder.ofFloat("translationX", endX),
                PropertyValuesHolder.ofFloat("rotationY", endRotationY)).setDuration(duration);

    }

}
