/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.android.autofillkeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.inline.InlineContentView;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.ArraySet;

/**
 * This class is a container for showing {@link InlineContentView}s for cases
 * where you want to ensure they appear only in a given area in your app. An
 * example is having a scrollable list of items. Note that without this container
 * the InlineContentViews' surfaces would cover parts of your app as these surfaces
 * are owned by another process and always appearing on top of your app.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public class InlineContentClipView extends FrameLayout {
    @NonNull
    private final ArraySet<InlineContentView> mClippedDescendants = new ArraySet<>();

    @NonNull
    private final ViewTreeObserver.OnDrawListener mOnDrawListener =
            this::clipDescendantInlineContentViews;

    @NonNull
    private final Rect mParentBounds = new Rect();

    @NonNull
    private final Rect mContentBounds = new Rect();

    @NonNull
    private SurfaceView mBackgroundView;

    private int mBackgroundColor;

    public InlineContentClipView(@NonNull Context context) {
        this(context, /*attrs*/ null);
    }

    public InlineContentClipView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, /*defStyleAttr*/ 0);
    }

    public InlineContentClipView(@NonNull Context context, @Nullable AttributeSet attrs,
            @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mBackgroundView = new SurfaceView(context);
        mBackgroundView.setZOrderOnTop(true);
        mBackgroundView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mBackgroundView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mBackgroundView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                drawBackgroundColorIfReady();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                    int height) { /*do nothing*/ }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                /*do nothing*/
            }
        });

        addView(mBackgroundView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnDrawListener(mOnDrawListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnDrawListener(mOnDrawListener);
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        Choreographer.getInstance().postFrameCallback((frameTimeNanos) ->
                drawBackgroundColorIfReady());
    }

    private void drawBackgroundColorIfReady() {
        final Surface surface = mBackgroundView.getHolder().getSurface();
        if (surface.isValid()) {
            final Canvas canvas = surface.lockCanvas(null);
            try {
                canvas.drawColor(mBackgroundColor);
            } finally {
                surface.unlockCanvasAndPost(canvas);
            }
        }
    }

    /**
     * Sets whether the surfaces of the {@link InlineContentView}s wrapped by this view
     * should appear on top or behind this view's window. Normally, they are placed on top
     * of the window, to allow interaction ith the embedded UI. Via this method, you can
     * place the surface below the window. This means that all of the contents of the window
     * this view is in will be visible on top of the  {@link InlineContentView}s' surfaces.
     *
     * @param onTop Whether to show the surface on top of this view's window.
     *
     * @see InlineContentView
     * @see InlineContentView#setZOrderedOnTop(boolean)
     */
    public void setZOrderedOnTop(boolean onTop) {
        mBackgroundView.setZOrderOnTop(onTop);
        for (InlineContentView inlineContentView : mClippedDescendants) {
            inlineContentView.setZOrderedOnTop(onTop);
        }
    }

    private void clipDescendantInlineContentViews() {
        mParentBounds.right = getWidth();
        mParentBounds.bottom = getHeight();
        mClippedDescendants.clear();
        clipDescendantInlineContentViews(this);
    }

    private void clipDescendantInlineContentViews(@Nullable View root) {
        if (root == null) {
            return;
        }

        if (root instanceof InlineContentView) {
            final InlineContentView inlineContentView = (InlineContentView) root;
            mContentBounds.set(mParentBounds);
            offsetRectIntoDescendantCoords(inlineContentView, mContentBounds);
            inlineContentView.setClipBounds(mContentBounds);
            mClippedDescendants.add(inlineContentView);
            return;
        }

        if (root instanceof ViewGroup) {
            final ViewGroup rootGroup = (ViewGroup) root;
            final int childCount = rootGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = rootGroup.getChildAt(i);
                clipDescendantInlineContentViews(child);
            }
        }
    }
}