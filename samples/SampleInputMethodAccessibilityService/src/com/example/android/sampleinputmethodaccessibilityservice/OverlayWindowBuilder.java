/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.example.android.sampleinputmethodaccessibilityservice;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

final class OverlayWindowBuilder {
    @NonNull
    private final View mContentView;
    private int mWidth = WindowManager.LayoutParams.WRAP_CONTENT;
    private int mHeight = WindowManager.LayoutParams.WRAP_CONTENT;
    private int mGravity = Gravity.NO_GRAVITY;
    private int mRelX = 0;
    private int mRelY = 0;
    private Integer mBackgroundColor = null;
    private boolean mShown = false;

    private OverlayWindowBuilder(@NonNull View contentView) {
        mContentView = contentView;
    }

    static OverlayWindowBuilder from(@NonNull View contentView) {
        return new OverlayWindowBuilder(contentView);
    }

    OverlayWindowBuilder setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        return this;
    }

    OverlayWindowBuilder setGravity(int gravity) {
        mGravity = gravity;
        return this;
    }

    OverlayWindowBuilder setRelativePosition(int relX, int relY) {
        mRelX = relX;
        mRelY = relY;
        return this;
    }

    OverlayWindowBuilder setBackgroundColor(@ColorInt int color) {
        mBackgroundColor = color;
        return this;
    }

    void show() {
        if (mShown) {
            throw new UnsupportedOperationException("show() can be called only once.");
        }

        final Context context = mContentView.getContext();
        final WindowManager windowManager = context.getSystemService(WindowManager.class);

        final FrameLayout contentFrame = new FrameLayout(context) {
            @Override
            public boolean requestSendAccessibilityEvent(View view, AccessibilityEvent event) {
                return false;
            }

            @Override
            public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
            }
        };
        if (mBackgroundColor != null) {
            contentFrame.setBackgroundColor(mBackgroundColor);
        }
        contentFrame.setOnTouchListener(new DragToMoveTouchListener((dx, dy) -> {
            final WindowManager.LayoutParams lp =
                    (WindowManager.LayoutParams) contentFrame.getLayoutParams();
            lp.x += dx;
            lp.y += dy;
            windowManager.updateViewLayout(contentFrame, lp);
        }));
        contentFrame.addView(mContentView);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                mWidth, mHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = mGravity;
        params.x = mRelX;
        params.y = mRelY;
        windowManager.addView(contentFrame, params);
        mShown = true;
    }
}
