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
package com.example.android.apis.animation;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 * This activity demonstrates the use of Path trimming and its use in animations.
 * The trim method works by extracting just a portion of the Path relative to its
 * length. By modifying the offset of the trim, we can make the trim path span
 * the start and end. In this demo, the line segment appears to be traversing
 * a circle.
 */
public class PathLengthAnimation extends Activity {
    private static final String TAG = "PathLengthAnimation";
    private static final float CIRCLE_RADIUS = 500f;
    private static final int CIRCLE_PADDING = 100;
    private static final int CIRCLE_STROKE = 30;
    private static final int CIRCLE_SIZE = Math.round(2 * (CIRCLE_RADIUS + CIRCLE_PADDING))
            + CIRCLE_STROKE;

    static final Path sPath = new Path();

    static {
        sPath.addCircle(CIRCLE_RADIUS + CIRCLE_PADDING, CIRCLE_RADIUS + CIRCLE_PADDING,
                CIRCLE_RADIUS, Path.Direction.CCW);
    }

    private ShapeDrawable mDrawable;
    private Path mPath = new Path();
    private float mTrimEnd = 1;
    private float mTrimOffset = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageView view = new ImageView(this);
        setContentView(view);

        PathShape pathShape = new PathShape(mPath, CIRCLE_SIZE, CIRCLE_SIZE);
        mDrawable = new ShapeDrawable(pathShape);
        mDrawable.setIntrinsicHeight(CIRCLE_SIZE);
        mDrawable.setIntrinsicWidth(CIRCLE_SIZE);
        Paint paint = mDrawable.getPaint();
        paint.setColor(0xFFFF0000);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(CIRCLE_STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
        view.setImageDrawable(mDrawable);

        ObjectAnimator endAnimator = ObjectAnimator.ofFloat(this, "trimEnd", 0.5f, 0.75f);
        endAnimator.setDuration(769);
        endAnimator.setRepeatCount(ValueAnimator.INFINITE);
        endAnimator.setRepeatMode(ValueAnimator.REVERSE);
        endAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        endAnimator.start();

        ObjectAnimator offsetAnimator = ObjectAnimator.ofFloat(this, "trimOffset", 1, 0);
        offsetAnimator.setDuration(1783);
        offsetAnimator.setRepeatCount(ValueAnimator.INFINITE);
        offsetAnimator.setInterpolator(new LinearInterpolator());
        offsetAnimator.start();

        setTrimEnd(1);
        setTrimOffset(0);
    }


    public void setTrimEnd(float trimEnd) {
        mTrimEnd = trimEnd;
        sPath.trim(0, mTrimEnd, mTrimOffset, mPath);
        mDrawable.invalidateSelf();
    }

    public void setTrimOffset(float trimOffset) {
        mTrimOffset = trimOffset;
        sPath.trim(0, mTrimEnd, mTrimOffset, mPath);
        mDrawable.invalidateSelf();
    }
}
