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

package com.example.android.foldinglayout;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * The folding layout where the number of folds, the anchor point and the
 * orientation of the fold can be specified. Each of these parameters can
 * be modified individually and updates and resets the fold to a default
 * (unfolded) state. The fold factor varies between 0 (completely unfolded
 * flat image) to 1.0 (completely folded, non-visible image).
 *
 * This layout throws an exception if there is more than one child added to the view.
 * For more complicated view hierarchy's inside the folding layout, the views should all
 * be nested inside 1 parent layout.
 *
 * This layout folds the contents of its child in real time. By applying matrix
 * transformations when drawing to canvas, the contents of the child may change as
 * the fold takes place. It is important to note that there are jagged edges about
 * the perimeter of the layout as a result of applying transformations to a rectangle.
 * This can be avoided by having the child of this layout wrap its content inside a
 * 1 pixel transparent border. This will cause an anti-aliasing like effect and smoothen
 * out the edges.
 *
 */
public class FoldingLayout extends ViewGroup {

    public static enum Orientation {
        VERTICAL,
        HORIZONTAL
    }

    private final String FOLDING_VIEW_EXCEPTION_MESSAGE = "Folding Layout can only 1 child at " +
            "most";

    private final float SHADING_ALPHA = 0.8f;
    private final float SHADING_FACTOR = 0.5f;
    private final int DEPTH_CONSTANT = 1500;
    private final int NUM_OF_POLY_POINTS = 8;

    private Rect[] mFoldRectArray;

    private Matrix [] mMatrix;

    private Orientation mOrientation = Orientation.HORIZONTAL;

    private float mAnchorFactor = 0;
    private float mFoldFactor = 0;

    private int mNumberOfFolds = 2;

    private boolean mIsHorizontal = true;

    private int mOriginalWidth = 0;
    private int mOriginalHeight = 0;

    private float mFoldMaxWidth = 0;
    private float mFoldMaxHeight = 0;
    private float mFoldDrawWidth = 0;
    private float mFoldDrawHeight = 0;

    private boolean mIsFoldPrepared = false;
    private boolean mShouldDraw = true;

    private Paint mSolidShadow;
    private Paint mGradientShadow;
    private LinearGradient mShadowLinearGradient;
    private Matrix mShadowGradientMatrix;

    private float [] mSrc;
    private float [] mDst;

    private OnFoldListener mFoldListener;

    private float mPreviousFoldFactor = 0;

    private Bitmap mFullBitmap;
    private Rect mDstRect;

    public FoldingLayout(Context context) {
        super(context);
    }

    public FoldingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FoldingLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, LayoutParams params,
                                      boolean preventRequestLayout) {
        throwCustomException(getChildCount());
        boolean returnValue = super.addViewInLayout(child, index, params, preventRequestLayout);
        return returnValue;
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        throwCustomException(getChildCount());
        super.addView(child, index, params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        View child = getChildAt(0);
        measureChild(child,widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View child = getChildAt(0);
        child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        updateFold();
    }

    /**
     * The custom exception to be thrown so as to limit the number of views in this
     * layout to at most one.
     */
    private class NumberOfFoldingLayoutChildrenException extends RuntimeException {
        public NumberOfFoldingLayoutChildrenException(String message) {
            super(message);
        }
    }

    /** Throws an exception if the number of views added to this layout exceeds one.*/
    private void throwCustomException (int numOfChildViews) {
        if (numOfChildViews == 1) {
            throw new NumberOfFoldingLayoutChildrenException(FOLDING_VIEW_EXCEPTION_MESSAGE);
        }
    }

    public void setFoldListener(OnFoldListener foldListener) {
        mFoldListener = foldListener;
    }

    /**
     * Sets the fold factor of the folding view and updates all the corresponding
     * matrices and values to account for the new fold factor. Once that is complete,
     * it redraws itself with the new fold. */
    public void setFoldFactor(float foldFactor) {
        if (foldFactor != mFoldFactor) {
            mFoldFactor = foldFactor;
            calculateMatrices();
            invalidate();
        }
    }

    public void setOrientation(Orientation orientation) {
        if (orientation != mOrientation) {
            mOrientation = orientation;
            updateFold();
        }
    }

    public void setAnchorFactor(float anchorFactor) {
        if (anchorFactor != mAnchorFactor) {
            mAnchorFactor = anchorFactor;
            updateFold();
        }
    }

    public void setNumberOfFolds(int numberOfFolds) {
        if (numberOfFolds != mNumberOfFolds) {
            mNumberOfFolds = numberOfFolds;
            updateFold();
        }
    }

    public float getAnchorFactor() {
        return mAnchorFactor;
    }

    public Orientation getOrientation() {
        return mOrientation;
    }

    public float getFoldFactor() {
        return mFoldFactor;
    }

    public int getNumberOfFolds() {
        return mNumberOfFolds;
    }

    private void updateFold() {
        prepareFold(mOrientation, mAnchorFactor, mNumberOfFolds);
        calculateMatrices();
        invalidate();
    }

    /**
     * This method is called in order to update the fold's orientation, anchor
     * point and number of folds. This creates the necessary setup in order to
     * prepare the layout for a fold with the specified parameters. Some of the
     * dimensions required for the folding transformation are also acquired here.
     *
     * After this method is called, it will be in a completely unfolded state by default.
     */
    private void prepareFold(Orientation orientation, float anchorFactor, int numberOfFolds) {

        mSrc = new float[NUM_OF_POLY_POINTS];
        mDst = new float[NUM_OF_POLY_POINTS];

        mDstRect = new Rect();

        mFoldFactor = 0;
        mPreviousFoldFactor = 0;

        mIsFoldPrepared = false;

        mSolidShadow = new Paint();
        mGradientShadow = new Paint();

        mOrientation = orientation;
        mIsHorizontal = (orientation == Orientation.HORIZONTAL);

        if (mIsHorizontal) {
            mShadowLinearGradient = new LinearGradient(0, 0, SHADING_FACTOR, 0, Color.BLACK,
                    Color.TRANSPARENT, TileMode.CLAMP);
        } else {
            mShadowLinearGradient = new LinearGradient(0, 0, 0, SHADING_FACTOR, Color.BLACK,
                    Color.TRANSPARENT, TileMode.CLAMP);
        }

        mGradientShadow.setStyle(Style.FILL);
        mGradientShadow.setShader(mShadowLinearGradient);
        mShadowGradientMatrix = new Matrix();

        mAnchorFactor = anchorFactor;
        mNumberOfFolds = numberOfFolds;

        mOriginalWidth = getMeasuredWidth();
        mOriginalHeight = getMeasuredHeight();

        mFoldRectArray = new Rect[mNumberOfFolds];
        mMatrix = new Matrix [mNumberOfFolds];

        for (int x = 0; x < mNumberOfFolds; x++) {
            mMatrix[x] = new Matrix();
        }

        int h = mOriginalHeight;
        int w = mOriginalWidth;

        if (FoldingLayoutActivity.IS_JBMR2) {
            mFullBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mFullBitmap);
            getChildAt(0).draw(canvas);
        }

        int delta = Math.round(mIsHorizontal ? ((float) w) / ((float) mNumberOfFolds) :
                ((float) h) /((float) mNumberOfFolds));

        /* Loops through the number of folds and segments the full layout into a number
         * of smaller equal components. If the number of folds is odd, then one of the
         * components will be smaller than all the rest. Note that deltap below handles
         * the calculation for an odd number of folds.*/
        for (int x = 0; x < mNumberOfFolds; x++) {
            if (mIsHorizontal) {
                int deltap = (x + 1) * delta > w ? w - x * delta : delta;
                mFoldRectArray[x] = new Rect(x * delta, 0, x * delta + deltap, h);
            } else {
                int deltap = (x + 1) * delta > h ? h - x * delta : delta;
                mFoldRectArray[x] = new Rect(0, x * delta, w, x * delta + deltap);
            }
        }

        if (mIsHorizontal) {
            mFoldMaxHeight = h;
            mFoldMaxWidth = delta;
        } else {
            mFoldMaxHeight = delta;
            mFoldMaxWidth = w;
        }

        mIsFoldPrepared = true;
    }

    /*
    * Calculates the transformation matrices used to draw each of the separate folding
    * segments from this view.
    */
    private void calculateMatrices() {

        mShouldDraw = true;

        if (!mIsFoldPrepared) {
            return;
        }

        /** If the fold factor is 1 than the folding view should not be seen
         * and the canvas can be left completely empty. */
        if (mFoldFactor == 1) {
            mShouldDraw = false;
            return;
        }

        if (mFoldFactor == 0 &&  mPreviousFoldFactor > 0) {
            mFoldListener.onEndFold();
        }

        if (mPreviousFoldFactor == 0 && mFoldFactor > 0) {
            mFoldListener.onStartFold();
        }

        mPreviousFoldFactor = mFoldFactor;

        /* Reset all the transformation matrices back to identity before computing
         * the new transformation */
        for (int x = 0; x < mNumberOfFolds; x++) {
            mMatrix[x].reset();
        }

        float cTranslationFactor = 1 - mFoldFactor;

        float translatedDistance = mIsHorizontal ? mOriginalWidth * cTranslationFactor :
                mOriginalHeight * cTranslationFactor;

        float translatedDistancePerFold = Math.round(translatedDistance / mNumberOfFolds);

        /* For an odd number of folds, the rounding error may cause the
         * translatedDistancePerFold to be grater than the max fold width or height. */
        mFoldDrawWidth = mFoldMaxWidth < translatedDistancePerFold ?
                translatedDistancePerFold : mFoldMaxWidth;
        mFoldDrawHeight = mFoldMaxHeight < translatedDistancePerFold ?
                translatedDistancePerFold : mFoldMaxHeight;

        float translatedDistanceFoldSquared = translatedDistancePerFold * translatedDistancePerFold;

        /* Calculate the depth of the fold into the screen using pythagorean theorem. */
        float depth = mIsHorizontal ?
                (float)Math.sqrt((double)(mFoldDrawWidth * mFoldDrawWidth -
                        translatedDistanceFoldSquared)) :
                (float)Math.sqrt((double)(mFoldDrawHeight * mFoldDrawHeight -
                        translatedDistanceFoldSquared));

        /* The size of some object is always inversely proportional to the distance
        *  it is away from the viewpoint. The constant can be varied to to affect the
        *  amount of perspective. */
        float scaleFactor = DEPTH_CONSTANT / (DEPTH_CONSTANT + depth);

        float scaledWidth, scaledHeight, bottomScaledPoint, topScaledPoint, rightScaledPoint,
                leftScaledPoint;

        if (mIsHorizontal) {
            scaledWidth = mFoldDrawWidth * cTranslationFactor;
            scaledHeight = mFoldDrawHeight * scaleFactor;
        } else {
            scaledWidth = mFoldDrawWidth * scaleFactor;
            scaledHeight = mFoldDrawHeight * cTranslationFactor;
        }

        topScaledPoint = (mFoldDrawHeight - scaledHeight) / 2.0f;
        bottomScaledPoint = topScaledPoint + scaledHeight;

        leftScaledPoint = (mFoldDrawWidth - scaledWidth) / 2.0f;
        rightScaledPoint = leftScaledPoint + scaledWidth;

        float anchorPoint = mIsHorizontal ? mAnchorFactor * mOriginalWidth :
                mAnchorFactor * mOriginalHeight;

        /* The fold along which the anchor point is located. */
        float midFold = mIsHorizontal ? (anchorPoint / mFoldDrawWidth) : anchorPoint /
                mFoldDrawHeight;

        mSrc[0] = 0;
        mSrc[1] = 0;
        mSrc[2] = 0;
        mSrc[3] = mFoldDrawHeight;
        mSrc[4] = mFoldDrawWidth;
        mSrc[5] = 0;
        mSrc[6] = mFoldDrawWidth;
        mSrc[7] = mFoldDrawHeight;

        /* Computes the transformation matrix for each fold using the values calculated above. */
        for (int x = 0; x < mNumberOfFolds; x++) {

            boolean isEven = (x % 2 == 0);

            if (mIsHorizontal) {
                mDst[0] = (anchorPoint > x * mFoldDrawWidth) ? anchorPoint + (x - midFold) *
                        scaledWidth : anchorPoint - (midFold - x) * scaledWidth;
                mDst[1] = isEven ? 0 : topScaledPoint;
                mDst[2] = mDst[0];
                mDst[3] = isEven ? mFoldDrawHeight: bottomScaledPoint;
                mDst[4] = (anchorPoint > (x + 1) * mFoldDrawWidth) ? anchorPoint + (x + 1 - midFold)
                        * scaledWidth : anchorPoint - (midFold - x - 1) * scaledWidth;
                mDst[5] = isEven ? topScaledPoint : 0;
                mDst[6] = mDst[4];
                mDst[7] = isEven ? bottomScaledPoint : mFoldDrawHeight;

            } else {
                mDst[0] = isEven ? 0 : leftScaledPoint;
                mDst[1] = (anchorPoint > x * mFoldDrawHeight) ? anchorPoint + (x - midFold) *
                        scaledHeight : anchorPoint - (midFold - x) * scaledHeight;
                mDst[2] = isEven ? leftScaledPoint: 0;
                mDst[3] = (anchorPoint > (x + 1) * mFoldDrawHeight) ? anchorPoint + (x + 1 -
                        midFold) * scaledHeight : anchorPoint - (midFold - x - 1) * scaledHeight;
                mDst[4] = isEven ? mFoldDrawWidth : rightScaledPoint;
                mDst[5] = mDst[1];
                mDst[6] = isEven ? rightScaledPoint : mFoldDrawWidth;
                mDst[7] = mDst[3];
            }

            /* Pixel fractions are present for odd number of folds which need to be
             * rounded off here.*/
            for (int y = 0; y < 8; y ++) {
                mDst[y] = Math.round(mDst[y]);
            }

            /* If it so happens that any of the folds have reached a point where
            *  the width or height of that fold is 0, then nothing needs to be
            *  drawn onto the canvas because the view is essentially completely
            *  folded.*/
            if (mIsHorizontal) {
                if (mDst[4] <= mDst[0] || mDst[6] <= mDst[2]) {
                    mShouldDraw = false;
                    return;
                }
            } else {
                if (mDst[3] <= mDst[1] || mDst[7] <= mDst[5]) {
                    mShouldDraw = false;
                    return;
                }
            }

            /* Sets the shadow and bitmap transformation matrices.*/
            mMatrix[x].setPolyToPoly(mSrc, 0, mDst, 0, NUM_OF_POLY_POINTS / 2);
        }
        /* The shadows on the folds are split into two parts: Solid shadows and gradients.
         * Every other fold has a solid shadow which overlays the whole fold. Similarly,
         * the folds in between these alternating folds also have an overlaying shadow.
         * However, it is a gradient that takes up part of the fold as opposed to a solid
         * shadow overlaying the whole fold.*/

        /* Solid shadow paint object. */
        int alpha = (int) (mFoldFactor * 255 * SHADING_ALPHA);

        mSolidShadow.setColor(Color.argb(alpha, 0, 0, 0));

        if (mIsHorizontal) {
            mShadowGradientMatrix.setScale(mFoldDrawWidth, 1);
            mShadowLinearGradient.setLocalMatrix(mShadowGradientMatrix);
        } else {
            mShadowGradientMatrix.setScale(1, mFoldDrawHeight);
            mShadowLinearGradient.setLocalMatrix(mShadowGradientMatrix);
        }

        mGradientShadow.setAlpha(alpha);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        /** If prepareFold has not been called or if preparation has not completed yet,
         * then no custom drawing will take place so only need to invoke super's
         * onDraw and return. */
        if (!mIsFoldPrepared || mFoldFactor == 0) {
            super.dispatchDraw(canvas);
            return;
        }

        if (!mShouldDraw) {
            return;
        }

        Rect src;
         /* Draws the bitmaps and shadows on the canvas with the appropriate transformations. */
        for (int x = 0; x < mNumberOfFolds; x++) {

            src = mFoldRectArray[x];
            /* The canvas is saved and restored for every individual fold*/
            canvas.save();

            /* Concatenates the canvas with the transformation matrix for the
             *  the segment of the view corresponding to the actual image being
             *  displayed. */
            canvas.concat(mMatrix[x]);
            if (FoldingLayoutActivity.IS_JBMR2) {
                mDstRect.set(0, 0, src.width(), src.height());
                canvas.drawBitmap(mFullBitmap, src, mDstRect, null);
            } else {
                /* The same transformation matrix is used for both the shadow and the image
                 * segment. The canvas is clipped to account for the size of each fold and
                 * is translated so they are drawn in the right place. The shadow is then drawn on
                 * top of the different folds using the sametransformation matrix.*/
                canvas.clipRect(0, 0, src.right - src.left, src.bottom - src.top);

                if (mIsHorizontal) {
                    canvas.translate(-src.left, 0);
                } else {
                    canvas.translate(0, -src.top);
                }

                super.dispatchDraw(canvas);

                if (mIsHorizontal) {
                    canvas.translate(src.left, 0);
                } else {
                    canvas.translate(0, src.top);
                }
            }
            /* Draws the shadows corresponding to this specific fold. */
            if (x % 2 == 0) {
                canvas.drawRect(0, 0, mFoldDrawWidth, mFoldDrawHeight, mSolidShadow);
            } else {
                canvas.drawRect(0, 0, mFoldDrawWidth, mFoldDrawHeight, mGradientShadow);
            }

            canvas.restore();
        }
    }

}