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

package com.example.android.cardflip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * This CardView object is a view which can flip horizontally about its edges,
 * as well as rotate clockwise or counter-clockwise about any of its corners. In
 * the middle of a flip animation, this view darkens to imitate a shadow-like effect.
 *
 * The key behind the design of this view is the fact that the layout parameters and
 * the animation properties of this view are updated and reset respectively after
 * every single animation. Therefore, every consecutive animation that this
 * view experiences is completely independent of what its prior state was.
 */
public class CardView extends ImageView {

    enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private final int CAMERA_DISTANCE = 8000;
    private final int MIN_FLIP_DURATION = 300;
    private final int VELOCITY_TO_DURATION_CONSTANT = 15;
    private final int MAX_FLIP_DURATION = 700;
    private final int ROTATION_PER_CARD = 2;
    private final int ROTATION_DELAY_PER_CARD = 50;
    private final int ROTATION_DURATION = 2000;
    private final int ANTIALIAS_BORDER = 1;

    private BitmapDrawable mFrontBitmapDrawable, mBackBitmapDrawable, mCurrentBitmapDrawable;

    private boolean mIsFrontShowing = true;
    private boolean mIsHorizontallyFlipped = false;

    private Matrix mHorizontalFlipMatrix;

    private CardFlipListener mCardFlipListener;

    public CardView(Context context) {
        super(context);
        init(context);
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /** Loads the bitmap drawables used for the front and back for this card.*/
    public void init(Context context) {
        mHorizontalFlipMatrix = new Matrix();

        setCameraDistance(CAMERA_DISTANCE);

        mFrontBitmapDrawable = bitmapWithBorder((BitmapDrawable)getResources()
                .getDrawable(R.drawable.red));
        mBackBitmapDrawable = bitmapWithBorder((BitmapDrawable) getResources()
                .getDrawable(R.drawable.blue));

        updateDrawableBitmap();
    }

    /**
     *  Adding a 1 pixel transparent border around the bitmap can be used to
     *  anti-alias the image as it rotates.
     */
    private BitmapDrawable bitmapWithBorder(BitmapDrawable bitmapDrawable) {
        Bitmap bitmapWithBorder = Bitmap.createBitmap(bitmapDrawable.getIntrinsicWidth() +
                ANTIALIAS_BORDER * 2, bitmapDrawable.getIntrinsicHeight() + ANTIALIAS_BORDER * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapWithBorder);
        canvas.drawBitmap(bitmapDrawable.getBitmap(), ANTIALIAS_BORDER, ANTIALIAS_BORDER, null);
        return new BitmapDrawable(getResources(), bitmapWithBorder);
    }

    /** Initiates a horizontal flip from right to left. */
    public void flipRightToLeft(int numberInPile, int velocity) {
        setPivotX(0);
        flipHorizontally(numberInPile, false, velocity);
    }

    /** Initiates a horizontal flip from left to right. */
    public void flipLeftToRight(int numberInPile, int velocity) {
        setPivotX(getWidth());
        flipHorizontally(numberInPile, true, velocity);
    }

    /**
     * Animates a horizontal (about the y-axis) flip of this card.
     * @param numberInPile Specifies how many cards are underneath this card in the new
     *                     pile so as to properly adjust its position offset in the stack.
     * @param clockwise Specifies whether the horizontal animation is 180 degrees
     *                  clockwise or 180 degrees counter clockwise.
     */
    public void flipHorizontally (int numberInPile, boolean clockwise, int velocity) {
        toggleFrontShowing();

        PropertyValuesHolder rotation = PropertyValuesHolder.ofFloat(View.ROTATION_Y,
                clockwise ? 180 : -180);

        PropertyValuesHolder xOffset = PropertyValuesHolder.ofFloat(View.TRANSLATION_X,
                numberInPile * CardFlip.CARD_PILE_OFFSET);
        PropertyValuesHolder yOffset = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y,
                numberInPile * CardFlip.CARD_PILE_OFFSET);

        ObjectAnimator cardAnimator = ObjectAnimator.ofPropertyValuesHolder(this, rotation,
                xOffset, yOffset);
        cardAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (valueAnimator.getAnimatedFraction() >= 0.5) {
                    updateDrawableBitmap();
                }
            }
        });

        Keyframe shadowKeyFrameStart = Keyframe.ofFloat(0, 0);
        Keyframe shadowKeyFrameMid = Keyframe.ofFloat(0.5f, 1);
        Keyframe shadowKeyFrameEnd = Keyframe.ofFloat(1, 0);
        PropertyValuesHolder shadowPropertyValuesHolder = PropertyValuesHolder.ofKeyframe
                ("shadow", shadowKeyFrameStart, shadowKeyFrameMid, shadowKeyFrameEnd);
        ObjectAnimator colorizer = ObjectAnimator.ofPropertyValuesHolder(this,
                shadowPropertyValuesHolder);

        mCardFlipListener.onCardFlipStart();
        AnimatorSet set = new AnimatorSet();
        int duration = MAX_FLIP_DURATION - Math.abs(velocity) / VELOCITY_TO_DURATION_CONSTANT;
        duration = duration < MIN_FLIP_DURATION ? MIN_FLIP_DURATION : duration;
        set.setDuration(duration);
        set.playTogether(cardAnimator, colorizer);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                toggleIsHorizontallyFlipped();
                updateDrawableBitmap();
                updateLayoutParams();
                mCardFlipListener.onCardFlipEnd();
            }
        });
        set.start();
    }

    /** Darkens this ImageView's image by applying a shadow color filter over it. */
    public void setShadow(float value) {
        int colorValue = (int)(255 - 200 * value);
        setColorFilter(Color.rgb(colorValue, colorValue, colorValue),
                android.graphics.PorterDuff.Mode.MULTIPLY);
    }

    public void toggleFrontShowing() {
        mIsFrontShowing = !mIsFrontShowing;
    }

    public void toggleIsHorizontallyFlipped() {
        mIsHorizontallyFlipped = !mIsHorizontallyFlipped;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHorizontalFlipMatrix.setScale(-1, 1, w / 2, h / 2);
    }

    /**
     *  Scale the canvas horizontally about its midpoint in the case that the card
     *  is in a horizontally flipped state.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (mIsHorizontallyFlipped) {
            canvas.concat(mHorizontalFlipMatrix);
        }
        super.onDraw(canvas);
    }

    /**
     *  Updates the layout parameters of this view so as to reset the rotationX and
     *  rotationY parameters, and remain independent of its previous position, while
     *  also maintaining its current position in the layout.
     */
    public void updateLayoutParams () {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) getLayoutParams();

        params.leftMargin = (int)(params.leftMargin + ((Math.abs(getRotationY()) % 360) / 180) *
                (2 * getPivotX () - getWidth()));

        setRotationX(0);
        setRotationY(0);

        setLayoutParams(params);
    }

    /**
     * Toggles the visible bitmap of this view between its front and back drawables
     * respectively.
     */
    public void updateDrawableBitmap () {
        mCurrentBitmapDrawable = mIsFrontShowing ? mFrontBitmapDrawable : mBackBitmapDrawable;
        setImageDrawable(mCurrentBitmapDrawable);
    }

    /**
     * Sets the appropriate translation of this card depending on how many cards
     * are in the pile underneath it.
     */
    public void updateTranslation (int numInPile) {
        setTranslationX(CardFlip.CARD_PILE_OFFSET * numInPile);
        setTranslationY(CardFlip.CARD_PILE_OFFSET * numInPile);
    }

    /**
     * Returns a rotation animation which rotates this card by some degree about
     * one of its corners either in the clockwise or counter-clockwise direction.
     * Depending on how many cards lie below this one in the stack, this card will
     * be rotated by a different amount so all the cards are visible when rotated out.
     */
    public ObjectAnimator getRotationAnimator (int cardFromTop, Corner corner,
                                               boolean isRotatingOut, boolean isClockwise) {
        rotateCardAroundCorner(corner);
        int rotation = cardFromTop * ROTATION_PER_CARD;

        if (!isClockwise) {
            rotation = -rotation;
        }

        if (!isRotatingOut) {
            rotation = 0;
        }

        return ObjectAnimator.ofFloat(this, View.ROTATION, rotation);
    }

    /**
     * Returns a full rotation animator which rotates this card by 360 degrees
     * about one of its corners either in the clockwise or counter-clockwise direction.
     * Depending on how many cards lie below this one in the stack, a different start
     * delay is applied to the animation so the cards don't all animate at once.
     */
    public ObjectAnimator getFullRotationAnimator (int cardFromTop, Corner corner,
                                                   boolean isClockwise) {
        final int currentRotation = (int)getRotation();

        rotateCardAroundCorner(corner);
        int rotation = 360 - currentRotation;
        rotation =  isClockwise ? rotation : -rotation;

        ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ROTATION, rotation);

        animator.setStartDelay(ROTATION_DELAY_PER_CARD * cardFromTop);
        animator.setDuration(ROTATION_DURATION);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setRotation(currentRotation);
            }
        });

        return animator;
    }

    /**
     * Sets the appropriate pivot of this card so that it can be rotated about
     * any one of its four corners.
     */
    public void rotateCardAroundCorner(Corner corner) {
        switch(corner) {
            case TOP_LEFT:
                setPivotX(0);
                setPivotY(0);
                break;
            case TOP_RIGHT:
                setPivotX(getWidth());
                setPivotY(0);
                break;
            case BOTTOM_LEFT:
                setPivotX(0);
                setPivotY(getHeight());
                break;
            case BOTTOM_RIGHT:
                setPivotX(getWidth());
                setPivotY(getHeight());
                break;
        }
    }

    public void setCardFlipListener(CardFlipListener cardFlipListener) {
        mCardFlipListener = cardFlipListener;
    }

}
