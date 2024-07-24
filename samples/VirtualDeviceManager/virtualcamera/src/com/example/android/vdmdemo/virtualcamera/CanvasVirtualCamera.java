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

package com.example.android.vdmdemo.virtualcamera;

import android.companion.virtual.camera.VirtualCameraCallback;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

/** Implementation of virtual camera callback drawing to camera Surface via canvas. */
public class CanvasVirtualCamera implements VirtualCameraCallback {
    private static final String TAG = CanvasVirtualCamera.class.getSimpleName();

    private final Object mLock = new Object();
    private final String mName;

    CanvasVirtualCamera(String name) {
        mName = name;
    }

    @GuardedBy("mLock")
    RenderThread mRenderThread = null;

    @Override
    public void onStreamConfigured(int streamId, @NonNull Surface surface, int width, int height,
            int format) {
        synchronized (mLock) {
            mRenderThread = new RenderThread(surface);
        }
    }

    @Override
    public void onStreamClosed(int streamId) {
        synchronized (mLock) {
            if (mRenderThread != null) {
                mRenderThread.stopRendering();
            }
        }
    }

    private class RenderThread extends Thread {
        private final Surface mSurface;
        private final AtomicBoolean mStopRequested = new AtomicBoolean(false);

        private float[] mHsv = new float[]{0f, 1f, 1f};

        RenderThread(Surface surface) {
            mSurface = surface;
            start();
        }

        @Override
        public void run() {
            while (!mStopRequested.get()) {
                Canvas canvas = mSurface.lockCanvas(null);

                mHsv[0] += 1f;
                if (mHsv[0] >= 360f) {
                    mHsv[0] = 0f;
                }
                Log.d(TAG, "Drawing with h:" + mHsv[0]);

                canvas.drawColor(Color.HSVToColor(mHsv));
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                paint.setTextSize(50);
                canvas.drawText("Virtual camera: " + mName, 50, 50, paint);
                mSurface.unlockCanvasAndPost(canvas);

                try {
                    Thread.sleep(1000 / 25);
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }
        }

        public void stopRendering() {
            mStopRequested.set(true);
            mSurface.release();
        }
    }
}
