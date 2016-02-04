/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.apis.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.widget.Button;

public class SystemPointerIconButton extends Button {

    public SystemPointerIconButton(Context context) {
        this(context, null);
    }

    public SystemPointerIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SystemPointerIconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SystemPointerIconButton(Context context, AttributeSet attrs,
                                    int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public PointerIcon getPointerIcon(MotionEvent event, float x, float y) {
        final int minX = getWidth() / 4;
        final int maxX = getWidth() - minX;
        final int minY = getHeight() / 4;
        final int maxY = getHeight() - minY;
        int style;
        if ((x < minX && y < minY) || (x > maxX && y > maxY)) {
            // Top/left or bottom/right corner.
            style = PointerIcon.STYLE_TOP_LEFT_DIAGONAL_DOUBLE_ARROW;
        } else if ((x < minX && y > maxY) || (x > maxX && y < minY)) {
            // Top/rightor bottom/left corner.
            style = PointerIcon.STYLE_TOP_RIGHT_DIAGONAL_DOUBLE_ARROW;
        } else if (x < minX || x > maxX) {
            // Left or right edge.
            style = PointerIcon.STYLE_HORIZONTAL_DOUBLE_ARROW;
        } else if (y < minY || y > maxY) {
            // Top or bottom edge edge.
            style = PointerIcon.STYLE_VERTICAL_DOUBLE_ARROW;
        } else {
            // Everything else (the middle).
            style = PointerIcon.STYLE_ALL_SCROLL;
        }
        return PointerIcon.getSystemIcon(getContext(), style);
    }
}
