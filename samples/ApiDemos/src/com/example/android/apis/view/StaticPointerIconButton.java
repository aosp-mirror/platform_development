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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.widget.Button;
import com.example.android.apis.R;

public class StaticPointerIconButton extends Button {
    PointerIcon mCustomIcon;

    public StaticPointerIconButton(Context context) {
        this(context, null);
    }

    public StaticPointerIconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StaticPointerIconButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StaticPointerIconButton(Context context, AttributeSet attrs,
                                    int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public PointerIcon onResolvePointerIcon(MotionEvent event, int pointerIndex) {
        if (mCustomIcon == null) {
            Drawable d = getContext().getDrawable(R.drawable.smile);
            final BitmapDrawable bitmapDrawable = (BitmapDrawable) d;
            final int hotSpotX = d.getIntrinsicWidth() / 2;
            final int hotSpotY = d.getIntrinsicHeight() / 2;
            mCustomIcon = PointerIcon.create(bitmapDrawable.getBitmap(), hotSpotX, hotSpotY);
        }
        return mCustomIcon;
    }
}
