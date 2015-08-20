package com.example.android.xyztouristattractions.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.transition.TransitionValues;
import android.transition.Visibility;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple scale transition class to allow an element to scale in or out.
 * This is used by the floating action button on the attraction detail screen
 * when it appears and disappears during the Activity transitions.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class ScaleTransition extends Visibility {

    public ScaleTransition(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Animator createAnimation(View view, float startScale, float endScale) {
        view.setScaleX(startScale);
        view.setScaleY(startScale);
        PropertyValuesHolder holderX = PropertyValuesHolder.ofFloat("scaleX", startScale, endScale);
        PropertyValuesHolder holderY = PropertyValuesHolder.ofFloat("scaleY", startScale, endScale);
        return ObjectAnimator.ofPropertyValuesHolder(view, holderX, holderY);
    }

    @Override
    public Animator onAppear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                             TransitionValues endValues) {
        return createAnimation(view, 0, 1);
    }

    @Override
    public Animator onDisappear(ViewGroup sceneRoot, View view, TransitionValues startValues,
                                TransitionValues endValues) {
        return createAnimation(view, 1, 0);
    }
}