/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.example.android.vdmdemo.common;

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

/** Fragment to show UI for a rotary input. */
@AndroidEntryPoint(Fragment.class)
public final class RotaryFragment extends Hilt_RotaryFragment {

    private Consumer<MotionEvent> mInputEventListener;

    private ImageView mView;
    private GestureDetectorCompat mDetector;
    private final GestureListener mGestureListener = new GestureListener();


    public RotaryFragment() {
        super(R.layout.rotary_fragment);
    }

    public void setInputEventListener(Consumer<MotionEvent> listener) {
        mInputEventListener = listener;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mView = (ImageView) view;
        mDetector = new GestureDetectorCompat(requireContext(), mGestureListener);
        mView.setOnTouchListener((v, e) -> mDetector.onTouchEvent(e));
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SCROLL_THRESHOLD = 80;
        private float mCurrentAngle = 0f;
        private int mScroll = 0;
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(
                MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            float angle = calculateAngle(e2.getX(), e2.getY());
            Matrix matrix = new Matrix();
            matrix.postRotate(angle, mView.getPivotX(), mView.getPivotY());
            mView.setImageMatrix(matrix);

            mScroll += angle > mCurrentAngle ? 1 : -1;
            mCurrentAngle = angle;
            if (Math.abs(mScroll) >= SCROLL_THRESHOLD) {
                final MotionEvent.PointerProperties pointerProperties =
                        new MotionEvent.PointerProperties();
                pointerProperties.toolType = MotionEvent.TOOL_TYPE_UNKNOWN;
                final MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
                pointerCoords.setAxisValue(MotionEvent.AXIS_SCROLL, mScroll > 0 ? 1 : -1);
                MotionEvent scrollEvent = MotionEvent.obtain(
                        0 /* downTime */,
                        0 /* eventTime */,
                        MotionEvent.ACTION_SCROLL,
                        1 /* pointerCount */,
                        new MotionEvent.PointerProperties[]{pointerProperties},
                        new MotionEvent.PointerCoords[]{pointerCoords},
                        0 /* metaState */,
                        0 /* buttonState */,
                        1f /* xPrecision */,
                        1f /* yPrecision */,
                        0 /* deviceId */,
                        0 /* edgeFlags */,
                        InputDevice.SOURCE_ROTARY_ENCODER,
                        0 /* flags */);
                mInputEventListener.accept(scrollEvent);
                mScroll = 0;
            }
            return true;
        }
    }

    private float calculateAngle(float x, float y) {
        float px = (x / (float) mView.getDrawable().getBounds().width()) - 0.5f;
        float py = (1 - y / (float) mView.getDrawable().getBounds().height()) - 0.5f;
        double angle = -Math.toDegrees(Math.atan2(py, px)) + 90f;
        if (angle > 180) angle -= 360;
        return (float) angle;
    }
}
