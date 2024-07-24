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

package com.example.android.vdmdemo.host;

import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** VDM Host Input fragment for mouse event injection. */
@AndroidEntryPoint(Fragment.class)
public abstract class MouseFragment extends Hilt_MouseFragment {

    @Inject
    InputController mInputController;

    private GestureDetectorCompat mDetector;

    protected GestureListener mGestureListener = new GestureListener();
    protected int mNumFingers = 0;


    public MouseFragment() {
        super(R.layout.fragment_input_mouse);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mDetector = new GestureDetectorCompat(requireContext(), mGestureListener);
        TextView touchpad = view.requireViewById(R.id.touchpad);
        touchpad.setText(getViewTextResourceId());
        touchpad.setOnTouchListener((v, e) -> {
            mNumFingers = e.getPointerCount();
            return mDetector.onTouchEvent(e);
        });
    }

    protected abstract @StringRes int getViewTextResourceId();

    protected class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final float SCROLL_THRESHOLD = 80f;
        private float mScrollX = 0;
        private float mScrollY = 0;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            mScrollX = mScrollY = 0;
            return true;
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            mInputController.sendMouseButtonEvent(MotionEvent.BUTTON_PRIMARY);
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            mInputController.sendMouseButtonEvent(MotionEvent.BUTTON_SECONDARY);
        }

        @Override
        public boolean onScroll(
                MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            mScrollX += distanceX;
            mScrollY += distanceY;
            if (Math.abs(mScrollX) > SCROLL_THRESHOLD && Math.abs(distanceY) > SCROLL_THRESHOLD) {
                mInputController.sendMouseScrollEvent(mScrollX, mScrollY);
                mScrollX = mScrollY = 0;
            } else if (Math.abs(mScrollX) > SCROLL_THRESHOLD) {
                mInputController.sendMouseScrollEvent(mScrollX, 0);
                mScrollX = 0;
            } else if (Math.abs(mScrollY) > SCROLL_THRESHOLD) {
                mInputController.sendMouseScrollEvent(0, mScrollY);
                mScrollY = 0;
            }
            return true;
        }
    }

    @AndroidEntryPoint(MouseFragment.class)
    public static final class TouchpadFragment extends Hilt_MouseFragment_TouchpadFragment {

        public TouchpadFragment() {
            mGestureListener = new GestureListener() {
                @Override
                public boolean onScroll(
                        MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
                    if (mNumFingers == 1) {
                        mInputController.sendMouseRelativeEvent(-distanceX, -distanceY);
                    } else if (mNumFingers == 2) {
                        return super.onScroll(e1, e2, distanceX, distanceY);
                    }
                    return true;
                }
            };
        }

        @Override
        protected @StringRes int getViewTextResourceId() {
            return R.string.touchpad_label;
        }
    }


    @AndroidEntryPoint(MouseFragment.class)
    public static final class RemoteFragment extends Hilt_MouseFragment_RemoteFragment {

        private static final float SENSOR_EVENT_THRESHOLD = 0.04f;
        private static final float SENSOR_EVENT_SCALE = 0.025f;

        private SensorManager mSensorManager;

        private final SensorEventListener mSensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = -event.values[2];
                float y = -event.values[0];
                PointF displaySize =
                        mInputController.getFocusedDisplaySize().orElse(new PointF(0, 0));
                if (Math.abs(x) > SENSOR_EVENT_THRESHOLD && Math.abs(y) > SENSOR_EVENT_THRESHOLD) {
                    x *= SENSOR_EVENT_SCALE * displaySize.x;
                    y *= SENSOR_EVENT_SCALE * displaySize.y;
                    mInputController.sendMouseRelativeEvent(x, y);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        @Override
        public void onResume() {
            super.onResume();
            mSensorManager = requireContext().getSystemService(SensorManager.class);
            mSensorManager.registerListener(
                    mSensorEventListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onPause() {
            super.onPause();
            mSensorManager.unregisterListener(mSensorEventListener);
        }

        @Override
        protected @StringRes int getViewTextResourceId() {
            return R.string.remote_control_label;
        }
    }
}
