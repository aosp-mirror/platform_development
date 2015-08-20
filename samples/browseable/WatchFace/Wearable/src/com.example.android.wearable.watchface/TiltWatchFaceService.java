/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.wearable.watchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.wearable.watchface.Gles2WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Sample watch face using OpenGL. The watch face is rendered using
 * {@link Gles2ColoredTriangleList}s. The camera moves around in interactive mode and stops moving
 * when the watch enters ambient mode.
 */
public class TiltWatchFaceService extends Gles2WatchFaceService {

    private static final String TAG = "TiltWatchFaceService";

    /** Expected frame rate in interactive mode. */
    private static final long FPS = 60;

    /** Z distance from the camera to the watchface. */
    private static final float EYE_Z = -2.3f;

    /** How long each frame is displayed at expected frame rate. */
    private static final long FRAME_PERIOD_MS = TimeUnit.SECONDS.toMillis(1) / FPS;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends Gles2WatchFaceService.Engine {
        /** Cycle time before the camera motion repeats. */
        private static final long CYCLE_PERIOD_SECONDS = 5;

        /** Number of camera angles to precompute. */
        private final int mNumCameraAngles = (int) (CYCLE_PERIOD_SECONDS * FPS);

        /** Projection transformation matrix. Converts from 3D to 2D. */
        private final float[] mProjectionMatrix = new float[16];

        /**
         * View transformation matrices to use in interactive mode. Converts from world to camera-
         * relative coordinates. One matrix per camera position.
         */
        private final float[][] mViewMatrices = new float[mNumCameraAngles][16];

        /** The view transformation matrix to use in ambient mode */
        private final float[] mAmbientViewMatrix = new float[16];

        /**
         * Model transformation matrices. Converts from model-relative coordinates to world
         * coordinates. One matrix per degree of rotation.
         */
        private final float[][] mModelMatrices = new float[360][16];

        /**
         * Products of {@link #mViewMatrices} and {@link #mProjectionMatrix}. One matrix per camera
         * position.
         */
        private final float[][] mVpMatrices = new float[mNumCameraAngles][16];

        /** The product of {@link #mAmbientViewMatrix} and {@link #mProjectionMatrix} */
        private final float[] mAmbientVpMatrix = new float[16];

        /**
         * Product of {@link #mModelMatrices}, {@link #mViewMatrices}, and
         * {@link #mProjectionMatrix}.
         */
        private final float[] mMvpMatrix = new float[16];

        /** Triangles for the 4 major ticks. These are grouped together to speed up rendering. */
        private Gles2ColoredTriangleList mMajorTickTriangles;

        /** Triangles for the 8 minor ticks. These are grouped together to speed up rendering. */
        private Gles2ColoredTriangleList mMinorTickTriangles;

        /** Triangle for the second hand. */
        private Gles2ColoredTriangleList mSecondHandTriangle;

        /** Triangle for the minute hand. */
        private Gles2ColoredTriangleList mMinuteHandTriangle;

        /** Triangle for the hour hand. */
        private Gles2ColoredTriangleList mHourHandTriangle;

        private Calendar mCalendar = Calendar.getInstance();

        /** Whether we've registered {@link #mTimeZoneReceiver}. */
        private boolean mRegisteredTimeZoneReceiver;

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(surfaceHolder);
            setWatchFaceStyle(new WatchFaceStyle.Builder(TiltWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .setHotwordIndicatorGravity(Gravity.LEFT | Gravity.TOP)
                    .setShowSystemUiTime(false)
                    .build());
        }

        @Override
        public void onGlContextCreated() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlContextCreated");
            }
            super.onGlContextCreated();

            // Create program for drawing triangles.
            Gles2ColoredTriangleList.Program triangleProgram =
                    new Gles2ColoredTriangleList.Program();

            // We only draw triangles which all use the same program so we don't need to switch
            // programs mid-frame. This means we can tell OpenGL to use this program only once
            // rather than having to do so for each frame. This makes OpenGL draw faster.
            triangleProgram.use();

            // Create triangles for the ticks.
            mMajorTickTriangles = createMajorTicks(triangleProgram);
            mMinorTickTriangles = createMinorTicks(triangleProgram);

            // Create triangles for the hands.
            mSecondHandTriangle = createHand(
                    triangleProgram,
                    0.02f /* width */,
                    1.0f /* height */,
                    new float[]{
                            1.0f /* red */,
                            0.0f /* green */,
                            0.0f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mMinuteHandTriangle = createHand(
                    triangleProgram,
                    0.06f /* width */,
                    1f /* height */,
                    new float[]{
                            0.7f /* red */,
                            0.7f /* green */,
                            0.7f /* blue */,
                            1.0f /* alpha */
                    }
            );
            mHourHandTriangle = createHand(
                    triangleProgram,
                    0.1f /* width */,
                    0.6f /* height */,
                    new float[]{
                            0.9f /* red */,
                            0.9f /* green */,
                            0.9f /* blue */,
                            1.0f /* alpha */
                    }
            );

            // Precompute the clock angles.
            for (int i = 0; i < mModelMatrices.length; ++i) {
                Matrix.setRotateM(mModelMatrices[i], 0, i, 0, 0, 1);
            }

            // Precompute the camera angles.
            for (int i = 0; i < mNumCameraAngles; ++i) {
                // Set the camera position (View matrix). When active, move the eye around to show
                // off that this is 3D.
                final float cameraAngle = (float) (((float) i) / mNumCameraAngles * 2 * Math.PI);
                final float eyeX = (float) Math.cos(cameraAngle);
                final float eyeY = (float) Math.sin(cameraAngle);
                Matrix.setLookAtM(mViewMatrices[i],
                        0, // dest index
                        eyeX, eyeY, EYE_Z, // eye
                        0, 0, 0, // center
                        0, 1, 0); // up vector
            }

            Matrix.setLookAtM(mAmbientViewMatrix,
                    0, // dest index
                    0, 0, EYE_Z, // eye
                    0, 0, 0, // center
                    0, 1, 0); // up vector
        }

        @Override
        public void onGlSurfaceCreated(int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onGlSurfaceCreated: " + width + " x " + height);
            }
            super.onGlSurfaceCreated(width, height);

            // Update the projection matrix based on the new aspect ratio.
            final float aspectRatio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix,
                    0 /* offset */,
                    -aspectRatio /* left */,
                    aspectRatio /* right */,
                    -1 /* bottom */,
                    1 /* top */,
                    2 /* near */,
                    7 /* far */);

            // Precompute the products of Projection and View matrices for each camera angle.
            for (int i = 0; i < mNumCameraAngles; ++i) {
                Matrix.multiplyMM(mVpMatrices[i], 0, mProjectionMatrix, 0, mViewMatrices[i], 0);
            }

            Matrix.multiplyMM(mAmbientVpMatrix, 0, mProjectionMatrix, 0, mAmbientViewMatrix, 0);
        }

        /**
         * Creates a triangle for a hand on the watch face.
         *
         * @param program program for drawing triangles
         * @param width width of base of triangle
         * @param length length of triangle
         * @param color color in RGBA order, each in the range [0, 1]
         */
        private Gles2ColoredTriangleList createHand(Gles2ColoredTriangleList.Program program,
                float width, float length, float[] color) {
            // Create the data for the VBO.
            float[] triangleCoords = new float[]{
                    // in counterclockwise order:
                    0, length, 0,   // top
                    -width / 2, 0, 0,   // bottom left
                    width / 2, 0, 0    // bottom right
            };
            return new Gles2ColoredTriangleList(program, triangleCoords, color);
        }

        /**
         * Creates a triangle list for the major ticks on the watch face.
         *
         * @param program program for drawing triangles
         */
        private Gles2ColoredTriangleList createMajorTicks(
                Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            float[] trianglesCoords = new float[9 * 4];
            for (int i = 0; i < 4; i++) {
                float[] triangleCoords = getMajorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, i * 9, triangleCoords.length);
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            1.0f /* red */,
                            1.0f /* green */,
                            1.0f /* blue */,
                            1.0f /* alpha */
                    }
            );
        }

        /**
         * Creates a triangle list for the minor ticks on the watch face.
         *
         * @param program program for drawing triangles
         */
        private Gles2ColoredTriangleList createMinorTicks(
                Gles2ColoredTriangleList.Program program) {
            // Create the data for the VBO.
            float[] trianglesCoords = new float[9 * (12 - 4)];
            int index = 0;
            for (int i = 0; i < 12; i++) {
                if (i % 3 == 0) {
                    // This is where a major tick goes, so skip it.
                    continue;
                }
                float[] triangleCoords = getMinorTickTriangleCoords(i);
                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.length);
                index += 9;
            }

            return new Gles2ColoredTriangleList(program, trianglesCoords,
                    new float[]{
                            0.5f /* red */,
                            0.5f /* green */,
                            0.5f /* blue */,
                            1.0f /* alpha */
                    }
            );
        }

        private float[] getMajorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.03f /* width */, 0.09f /* length */,
                    index * 360 / 4 /* angleDegrees */);
        }

        private float[] getMinorTickTriangleCoords(int index) {
            return getTickTriangleCoords(0.02f /* width */, 0.06f /* length */,
                    index * 360 / 12 /* angleDegrees */);
        }

        private float[] getTickTriangleCoords(float width, float length, int angleDegrees) {
            // Create the data for the VBO.
            float[] coords = new float[]{
                    // in counterclockwise order:
                    0, 1, 0,   // top
                    width / 2, length + 1, 0,   // bottom left
                    -width / 2, length + 1, 0    // bottom right
            };

            rotateCoords(coords, angleDegrees);
            return coords;
        }

        /**
         * Destructively rotates the given coordinates in the XY plane about the origin by the given
         * angle.
         *
         * @param coords flattened 3D coordinates
         * @param angleDegrees angle in degrees clockwise when viewed from negative infinity on the
         *                     Z axis
         */
        private void rotateCoords(float[] coords, int angleDegrees) {
            double angleRadians = Math.toRadians(angleDegrees);
            double cos = Math.cos(angleRadians);
            double sin = Math.sin(angleRadians);
            for (int i = 0; i < coords.length; i += 3) {
                float x = coords[i];
                float y = coords[i + 1];
                coords[i] = (float) (cos * x - sin * y);
                coords[i + 1] = (float) (sin * x + cos * y);
            }
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we were detached.
                mCalendar.setTimeZone(TimeZone.getDefault());

                invalidate();
            } else {
                unregisterReceiver();
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TiltWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TiltWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }

        @Override
        public void onDraw() {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "onDraw");
            }
            super.onDraw();
            final float[] vpMatrix;

            // Draw background color and select the appropriate view projection matrix. The
            // background should always be black in ambient mode. The view projection matrix used is
            // overhead in ambient. In interactive mode, it's tilted depending on the current time.
            if (isInAmbientMode()) {
                GLES20.glClearColor(0, 0, 0, 1);
                vpMatrix = mAmbientVpMatrix;
            } else {
                GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1);
                final int cameraIndex =
                        (int) ((System.currentTimeMillis() / FRAME_PERIOD_MS) % mNumCameraAngles);
                vpMatrix = mVpMatrices[cameraIndex];
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            // Compute angle indices for the three hands.
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            float seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f;
            float minutes = mCalendar.get(Calendar.MINUTE) + seconds / 60f;
            float hours = mCalendar.get(Calendar.HOUR) + minutes / 60f;
            final int secIndex = (int) (seconds / 60f * 360f);
            final int minIndex = (int) (minutes / 60f * 360f);
            final int hoursIndex = (int) (hours / 12f * 360f);

            // Draw triangles from back to front. Don't draw the second hand in ambient mode.

            // Combine the model matrix with the projection and camera view.
            Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[hoursIndex], 0);

            // Draw the triangle.
            mHourHandTriangle.draw(mMvpMatrix);

            // Combine the model matrix with the projection and camera view.
            Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[minIndex], 0);

            // Draw the triangle.
            mMinuteHandTriangle.draw(mMvpMatrix);
            if (!isInAmbientMode()) {
                // Combine the model matrix with the projection and camera view.
                Matrix.multiplyMM(mMvpMatrix, 0, vpMatrix, 0, mModelMatrices[secIndex], 0);

                // Draw the triangle.
                mSecondHandTriangle.draw(mMvpMatrix);
            }

            // Draw the major and minor ticks.
            mMajorTickTriangles.draw(vpMatrix);
            mMinorTickTriangles.draw(vpMatrix);

            // Draw every frame as long as we're visible and in interactive mode.
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }
    }
}
