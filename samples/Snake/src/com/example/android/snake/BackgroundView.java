/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.snake;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Arrays;

/**
 * Background View: Draw 4 full-screen RGBY triangles
 */
public class BackgroundView extends View {

    private int[] mColors = new int[4];

    private final short[] mIndices =
            { 0, 1, 2, 0, 3, 4, 0, 1, 4 // Corner points for triangles (with offset = 2)
    };

    private float[] mVertexPoints = null;

    public BackgroundView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);

        // retrieve colors for 4 segments from styleable properties
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BackgroundView);
        mColors[0] = a.getColor(R.styleable.BackgroundView_colorSegmentOne, Color.RED);
        mColors[1] = a.getColor(R.styleable.BackgroundView_colorSegmentTwo, Color.YELLOW);
        mColors[2] = a.getColor(R.styleable.BackgroundView_colorSegmentThree, Color.BLUE);
        mColors[3] = a.getColor(R.styleable.BackgroundView_colorSegmentFour, Color.GREEN);

        a.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        assert(mVertexPoints != null);

        // Colors for each vertex
        int[] mFillColors = new int[mVertexPoints.length];

        for (int triangle = 0; triangle < mColors.length; triangle++) {
            // Set color for all vertex points to current triangle color
            Arrays.fill(mFillColors, mColors[triangle]);

            // Draw one triangle
            canvas.drawVertices(Canvas.VertexMode.TRIANGLES, mVertexPoints.length, mVertexPoints,
                    0, null, 0, // No Textures
                    mFillColors, 0, mIndices,
                    triangle * 2, 3, // Use 3 vertices via Index Array with offset 2
                    new Paint());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

     // Construct our center and four corners
        mVertexPoints = new float[] {
                w / 2, h / 2,
                0, 0,
                w, 0,
                w, h,
                0, h
        };
    }

}
