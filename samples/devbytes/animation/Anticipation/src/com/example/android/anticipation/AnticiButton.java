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

package com.example.android.anticipation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

/**
 * Custom button which can be deformed by skewing the top left and right, to simulate
 * anticipation and follow-through animation effects. Clicking on the button runs
 * an animation which moves the button left or right, applying the skew effect to the
 * button. The logic of drawing the button with a skew transform is handled in the
 * draw() override.
 */
public class AnticiButton extends Button {

    private static final LinearInterpolator sLinearInterpolator = new LinearInterpolator();
    private static final DecelerateInterpolator sDecelerator = new DecelerateInterpolator(8);
    private static final AccelerateInterpolator sAccelerator = new AccelerateInterpolator();
    private static final OvershootInterpolator sOvershooter = new OvershootInterpolator();
    private static final DecelerateInterpolator sQuickDecelerator = new DecelerateInterpolator();
    
    private float mSkewX = 0;
    ObjectAnimator downAnim = null;
    boolean mOnLeft = true;
    RectF mTempRect = new RectF();
    
    public AnticiButton(Context context) {
        super(context);
        init();
    }

    public AnticiButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public AnticiButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOnTouchListener(mTouchListener);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                runClickAnim();
            }
        });
    }

    /**
     * The skew effect is handled by changing the transform of the Canvas
     * and then calling the usual superclass draw() method.
     */
    @Override
    public void draw(Canvas canvas) {
        if (mSkewX != 0) {
            canvas.translate(0, getHeight());
            canvas.skew(mSkewX, 0);
            canvas.translate(0,  -getHeight());
        }
        super.draw(canvas);
    }

    /**
     * Anticipate the future animation by rearing back, away from the direction of travel
     */
    private void runPressAnim() {
        downAnim = ObjectAnimator.ofFloat(this, "skewX", mOnLeft ? .5f : -.5f);
        downAnim.setDuration(2500);
        downAnim.setInterpolator(sDecelerator);
        downAnim.start();
    }

    /**
     * Finish the "anticipation" animation (skew the button back from the direction of
     * travel), animate it to the other side of the screen, then un-skew the button
     * with an Overshoot effect.
     */
    private void runClickAnim() {
        // Anticipation
        ObjectAnimator finishDownAnim = null;
        if (downAnim != null && downAnim.isRunning()) {
            // finish the skew animation quickly
            downAnim.cancel();
            finishDownAnim = ObjectAnimator.ofFloat(this, "skewX",
                    mOnLeft ? .5f : -.5f);
            finishDownAnim.setDuration(150);
            finishDownAnim.setInterpolator(sQuickDecelerator);
        }
        
        // Slide. Use LinearInterpolator in this rare situation where we want to start
        // and end fast (no acceleration or deceleration, since we're doing that part
        // during the anticipation and overshoot phases).
        ObjectAnimator moveAnim = ObjectAnimator.ofFloat(this,
                View.TRANSLATION_X, mOnLeft ? 400 : 0);
        moveAnim.setInterpolator(sLinearInterpolator);
        moveAnim.setDuration(150);
        
        // Then overshoot by stopping the movement but skewing the button as if it couldn't
        // all stop at once
        ObjectAnimator skewAnim = ObjectAnimator.ofFloat(this, "skewX",
                mOnLeft ? -.5f : .5f);
        skewAnim.setInterpolator(sQuickDecelerator);
        skewAnim.setDuration(100);
        // and wobble it
        ObjectAnimator wobbleAnim = ObjectAnimator.ofFloat(this, "skewX", 0);
        wobbleAnim.setInterpolator(sOvershooter);
        wobbleAnim.setDuration(150);
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(moveAnim, skewAnim, wobbleAnim);
        if (finishDownAnim != null) {
            set.play(finishDownAnim).before(moveAnim);
        }
        set.start();
        mOnLeft = !mOnLeft;
    }

    /**
     * Restore the button to its un-pressed state
     */
    private void runCancelAnim() {
        if (downAnim != null && downAnim.isRunning()) {
            downAnim.cancel();
            ObjectAnimator reverser = ObjectAnimator.ofFloat(this, "skewX", 0);
            reverser.setDuration(200);
            reverser.setInterpolator(sAccelerator);
            reverser.start();
            downAnim = null;
        }
    }

    /**
     * Handle touch events directly since we want to react on down/up events, not just
     * button clicks
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (isPressed()) {
                    performClick();
                    setPressed(false);
                    break;
                }
                // No click: Fall through; equivalent to cancel event
            case MotionEvent.ACTION_CANCEL:
                // Run the cancel animation in either case
                runCancelAnim();
                break;
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                boolean isInside = (x > 0 && x < getWidth() &&
                        y > 0 && y < getHeight());
                if (isPressed() != isInside) {
                    setPressed(isInside);
                }
                break;
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                runPressAnim();
                break;
            default:
                break;
            }
            return true;
        }
    };
    
    public float getSkewX() {
        return mSkewX;
    }
    
    /**
     * Sets the amount of left/right skew on the button, which determines how far the button
     * leans.
     */
    public void setSkewX(float value) {
        if (value != mSkewX) {
            mSkewX = value;
            invalidate();             // force button to redraw with new skew value
            invalidateSkewedBounds(); // also invalidate appropriate area of parent
        }
    }
    
    /**
     * Need to invalidate proper area of parent for skewed bounds
     */
    private void invalidateSkewedBounds() {
        if (mSkewX != 0) {
            Matrix matrix = new Matrix();
            matrix.setSkew(-mSkewX, 0);
            mTempRect.set(0, 0, getRight(), getBottom());
            matrix.mapRect(mTempRect);
            mTempRect.offset(getLeft() + getTranslationX(), getTop() + getTranslationY());
            ((View) getParent()).invalidate((int) mTempRect.left, (int) mTempRect.top,
                    (int) (mTempRect.right +.5f), (int) (mTempRect.bottom + .5f));
        }
    }
}
