/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.android.vdmdemo.client;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.StyleRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

/** Recycler view that can resize a child dynamically. */
public final class ClientView extends RecyclerView {

    private static final int MIN_SIZE = 100;
    private int mMaxSize = 0;

    private boolean mIsResizing = false;
    private Consumer<Rect> mResizeDoneCallback = null;
    private Drawable mResizingRect = null;
    private final Rect mResizingBounds = new Rect();
    private float mResizeOffsetX = 0;
    private float mResizeOffsetY = 0;

    public ClientView(Context context) {
        super(context);
        init();
    }

    public ClientView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClientView(Context context, AttributeSet attrs, @StyleRes int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mResizingRect = getContext().getResources().getDrawable(R.drawable.resize_rect, null);
    }

    void startResizing(View viewToResize, MotionEvent origin, int maxSize,
            Consumer<Rect> callback) {
        mIsResizing = true;
        mMaxSize = maxSize;
        mResizeDoneCallback = callback;
        viewToResize.getGlobalVisibleRect(mResizingBounds);
        mResizingRect.setBounds(mResizingBounds);
        getRootView().getOverlay().add(mResizingRect);
        mResizeOffsetX = origin.getRawX() - mResizingBounds.right;
        mResizeOffsetY = origin.getRawY() - mResizingBounds.top;
    }

    private void stopResizing() {
        if (!mIsResizing) {
            return;
        }
        mIsResizing = false;
        mResizeOffsetX = mResizeOffsetY = 0;
        getRootView().getOverlay().clear();
        if (mResizeDoneCallback != null) {
            mResizeDoneCallback.accept(mResizingBounds);
            mResizeDoneCallback = null;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!mIsResizing) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP -> stopResizing();
            case MotionEvent.ACTION_MOVE -> {
                mResizingBounds.right = (int) (ev.getRawX() - mResizeOffsetX);
                if (mResizingBounds.width() > mMaxSize) {
                    mResizingBounds.right = mResizingBounds.left + mMaxSize;
                }
                if (mResizingBounds.width() < MIN_SIZE) {
                    mResizingBounds.right = mResizingBounds.left + MIN_SIZE;
                }
                mResizingBounds.top = (int) (ev.getRawY() - mResizeOffsetY);
                if (mResizingBounds.height() > mMaxSize) {
                    mResizingBounds.top = mResizingBounds.bottom - mMaxSize;
                }
                if (mResizingBounds.height() < MIN_SIZE) {
                    mResizingBounds.top = mResizingBounds.bottom - MIN_SIZE;
                }
                mResizingRect.setBounds(mResizingBounds);
            }
        }
        return true;
    }
}
