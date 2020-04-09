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
import android.os.Build;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
    // The trick that we use here is to have a hidden SurfaceView to whose
    // surface we reparent the surfaces of remote content views which are
    // InlineContentViews. Since surface locations are based off the window
    // top-left making, making one surface parent of another compounds the
    // offset from the child's point of view. To compensate for that we
    // apply transformation to the InlineContentViews.

    @NonNull
    private final ArraySet<InlineContentView> mReparentedDescendants = new ArraySet<>();

    @NonNull
    private final int[] mTempLocation = new int[2];

    @NonNull
    SurfaceView mSurfaceClipView;

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

        mSurfaceClipView = new SurfaceView(context);
        mSurfaceClipView.setZOrderOnTop(true);
        mSurfaceClipView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mSurfaceClipView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mSurfaceClipView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                drawBackgroundColorIfReady();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width,
                    int height) { /*do nothing*/ }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                updateState(InlineContentClipView.this, /*parentSurfaceProvider*/ null);
            }
        });

        addView(mSurfaceClipView);

        setWillNotDraw(false);

        getViewTreeObserver().addOnPreDrawListener(() -> {
            updateState(InlineContentClipView.this, mSurfaceClipView);
            return true;
        });
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        Choreographer.getInstance().postFrameCallback((frameTimeNanos) ->
                drawBackgroundColorIfReady());
    }

    private void drawBackgroundColorIfReady() {
        final Surface surface = mSurfaceClipView.getHolder().getSurface();
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
        mSurfaceClipView.setZOrderOnTop(onTop);
        for (InlineContentView inlineContentView : mReparentedDescendants) {
            inlineContentView.setZOrderedOnTop(onTop);
        }
    }

    void updateState(@NonNull View root,
            @Nullable SurfaceView parentSurfaceProvider) {
        if (parentSurfaceProvider != null) {
            mSurfaceClipView.getLocationInWindow(mTempLocation);
        } else {
            mTempLocation[0] = 0;
            mTempLocation[1] = 0;
        }
        reparentChildSurfaceViewSurfacesRecursive(root, parentSurfaceProvider,
                /*parentSurfaceLeft*/ mTempLocation[0], /*parentSurfaceTop*/ mTempLocation[1]);
    }

    private void reparentChildSurfaceViewSurfacesRecursive(@Nullable View root,
            @Nullable SurfaceView parentSurfaceProvider, int parentSurfaceLeft,
            int parentSurfaceTop) {
        if (root == null || root == mSurfaceClipView) {
            return;
        }


        if (root instanceof InlineContentView) {
            // Surfaces of a surface view have a transformation matrix relative
            // to the top-left of the window and when one is reparented to the
            // other the transformation adds up and we need to compensate.
            root.setTranslationX(-parentSurfaceLeft);
            root.setTranslationY(-parentSurfaceTop);

            final InlineContentView inlineContentView = (InlineContentView) root;
            if (parentSurfaceProvider != null) {
                if (mReparentedDescendants.contains(inlineContentView)) {
                    return;
                }

                inlineContentView.setSurfaceControlCallback(
                        new InlineContentView.SurfaceControlCallback() {
                    @Override
                    public void onCreated(SurfaceControl surfaceControl) {
                        // Our surface and its descendants are initially hidden until
                        // the descendants are reparented and their containers scrolled.
                        new SurfaceControl.Transaction()
                                .reparent(surfaceControl, parentSurfaceProvider.getSurfaceControl())
                                .apply();
                    }

                    @Override
                    public void onDestroyed(SurfaceControl surfaceControl) {
                        /* do nothing */
                    }
                });

                mReparentedDescendants.add(inlineContentView);
            } else {
                if (!mReparentedDescendants.contains(inlineContentView)) {
                    return;
                }

                // Unparent the surface control of the removed surface view.
                final SurfaceControl surfaceControl = inlineContentView.getSurfaceControl();
                if (surfaceControl != null && surfaceControl.isValid()) {
                    new SurfaceControl.Transaction()
                            .reparent(surfaceControl, /*newParent*/ null)
                            .apply();
                }

                mReparentedDescendants.remove(inlineContentView);
            }

            return;
        }

        if (root instanceof ViewGroup) {
            final ViewGroup rootGroup = (ViewGroup) root;
            final int childCount = rootGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = rootGroup.getChildAt(i);
                reparentChildSurfaceViewSurfacesRecursive(child, parentSurfaceProvider,
                        parentSurfaceLeft, parentSurfaceTop);
            }
        }
    }
}