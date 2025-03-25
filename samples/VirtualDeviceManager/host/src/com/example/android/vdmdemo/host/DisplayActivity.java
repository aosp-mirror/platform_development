/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Rational;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android.vdmdemo.common.EdgeToEdgeUtils;
import com.example.android.vdmdemo.common.RemoteEventProto;

import dagger.hilt.android.AndroidEntryPoint;

import java.nio.ByteBuffer;

import javax.inject.Inject;

/**
 * VDM activity, showing an interactive virtual display.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class DisplayActivity extends Hilt_DisplayActivity
        implements DisplayManager.DisplayListener {

    public static final String TAG = "VdmHost_DisplayActivity";

    // Approximately, see
    // https://developer.android.com/reference/android/util/DisplayMetrics#density
    private static final float DIP_TO_DPI = 160f;

    /** @see android.app.PictureInPictureParams.Builder#setAspectRatio(android.util.Rational) */
    private static final Rational MAX_PIP_RATIO = new Rational(239, 100);
    private static final Rational MIN_PIP_RATIO = new Rational(100, 239);

    static final String EXTRA_DISPLAY_ID = "displayId";


    @Inject
    InputController mInputController;

    DisplayManager mDisplayManager;

    private VdmService mVdmService = null;
    private int mDisplayId;

    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private Surface mSurface;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mDpi;

    private RemoteDisplay mDisplay;
    private boolean mPoweredOn = true;
    private ImageReader mImageReader;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            synchronized (mLock) {
                Log.d(TAG, "Connected to VDM Service");
                mVdmService = ((VdmService.LocalBinder) binder).getService();
                mDisplay = mVdmService.getRemoteDisplay(mDisplayId).orElseGet(() ->
                        mVdmService.createRemoteDisplay(
                                DisplayActivity.this, mDisplayId, 200, 200, mDpi, null));
            }
            if (isInPictureInPictureMode()) {
                Log.v(TAG, "Initializing copy from display " + mDisplayId + " to PIP window");
                mImageReader = ImageReader.newInstance(
                        mDisplay.getWidth(), mDisplay.getHeight(), PixelFormat.RGBA_8888, 2);
                mDisplay.setSurface(mImageReader.getSurface());
                mImageReader.setOnImageAvailableListener((reader) -> {
                    Image image = reader.acquireLatestImage();
                    synchronized (mLock) {
                        if (image != null && mSurface != null) {
                            copyImageToSurfaceLocked(image);
                            image.close();
                        }
                    }
                }, null);
            } else {
                synchronized (mLock) {
                    if (mSurface != null) {
                        resetDisplayLocked();
                        setPictureInPictureParams(buildPictureInPictureParams());
                    }
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Disconnected from VDM Service");
            mVdmService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDisplayManager = getSystemService(DisplayManager.class);
        mDisplayManager.registerDisplayListener(this, null);
        mDisplayId = getIntent().getIntExtra(EXTRA_DISPLAY_ID, Display.INVALID_DISPLAY);
        mDpi = (int) (getResources().getDisplayMetrics().density * DIP_TO_DPI);

        setContentView(R.layout.activity_display);
        TextureView textureView = requireViewById(R.id.display_surface_view);
        Toolbar toolbar = requireViewById(R.id.main_tool_bar);
        if (isInPictureInPictureMode()) {
            toolbar.setVisibility(View.GONE);
        } else {
            setSupportActionBar(toolbar);
            setTitle(getTitle() + " " + mDisplayId);
            EdgeToEdgeUtils.applyTopInsets(toolbar);
            EdgeToEdgeUtils.applyBottomInsets(textureView);
        }

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {}

            @Override
            public void onSurfaceTextureAvailable(
                    @NonNull SurfaceTexture texture, int width, int height) {
                synchronized (mLock) {
                    Log.d(TAG, "onSurfaceTextureAvailable for local display " + mDisplayId);
                    mSurfaceWidth = width;
                    mSurfaceHeight = height;
                    mSurface = new Surface(texture);
                    if (!isInPictureInPictureMode() && mDisplay != null) {
                        resetDisplayLocked();
                    }
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
                Log.v(TAG, "onSurfaceTextureDestroyed for local display " + mDisplayId);
                mSurface = null;
                return true;
            }

            @Override
            public void onSurfaceTextureSizeChanged(
                    @NonNull SurfaceTexture texture, int width, int height) {
                Log.v(TAG, "onSurfaceTextureSizeChanged for local display " + mDisplayId);
                synchronized (mLock) {
                    mSurfaceWidth = width;
                    mSurfaceHeight = height;
                    if (!isInPictureInPictureMode() && mDisplay != null) {
                        resetDisplayLocked();
                    }
                }
            }
        });

        textureView.setOnTouchListener((v, event) -> {
            if (event.getDevice().supportsSource(InputDevice.SOURCE_TOUCHSCREEN)
                    && mDisplay != null) {
                textureView.getParent().requestDisallowInterceptTouchEvent(true);
                mDisplay.processInputEvent(
                        RemoteEventProto.InputDeviceType.DEVICE_TYPE_TOUCHSCREEN, event);
            }
            return true;
        });

        textureView.setOnGenericMotionListener((v, event) -> {
            if (event.getDevice() == null || mDisplay == null
                    || !event.getDevice().supportsSource(InputDevice.SOURCE_MOUSE)) {
                return false;
            }
            mDisplay.processVirtualMouseEvent(event);
            return true;
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDisplay != null) {
                    mDisplay.sendBack();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, VdmService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mInputController.setFocusedRemoteDisplayId(mDisplayId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisplayManager.unregisterDisplayListener(this);
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.display, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.close:
                if (mVdmService != null) {
                    mVdmService.closeRemoteDisplay(mDisplayId);
                }
                return true;
            case R.id.pip:
                enterPictureInPictureMode(buildPictureInPictureParams());
                return true;
            case R.id.power:
                if (mDisplay != null) {
                    mPoweredOn = !mPoweredOn;
                    mVdmService.setPowerState(mPoweredOn);
                }
                return true;
            case R.id.home:
                if (mDisplay != null) {
                    mDisplay.goHome();
                }
                return true;
            case R.id.back:
                if (mDisplay != null) {
                    mDisplay.sendBack();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        mDisplay.processInputEvent(RemoteEventProto.InputDeviceType.DEVICE_TYPE_KEYBOARD, event);
        return true;
    }

    private PictureInPictureParams buildPictureInPictureParams() {
        Rational ratio = new Rational(mDisplay.getWidth(), mDisplay.getHeight());
        if (ratio.compareTo(MAX_PIP_RATIO) > 0) {
            ratio = MAX_PIP_RATIO;
        } else if (ratio.compareTo(MIN_PIP_RATIO) < 0) {
            ratio = MIN_PIP_RATIO;
        }
        Rect rect = new Rect();
        View textureView = requireViewById(R.id.display_surface_view);
        textureView.getGlobalVisibleRect(rect);
        return new PictureInPictureParams.Builder()
                .setAutoEnterEnabled(true)
                .setAspectRatio(ratio)
                .setExpandedAspectRatio(ratio)
                .setSourceRectHint(rect)
                .setSeamlessResizeEnabled(false)
                .build();
    }

    @GuardedBy("mLock")
    private void resetDisplayLocked() {
        if (mDisplay.getWidth() != mSurfaceWidth || mDisplay.getHeight() != mSurfaceHeight) {
            Log.v(TAG, "Resizing display " + mDisplayId + " to " + mSurfaceWidth
                    + "/" + mSurfaceHeight);
            mDisplay.reset(mSurfaceWidth, mSurfaceHeight, mDpi);
        }
        mDisplay.setSurface(mSurface);
    }

    @GuardedBy("mLock")
    private void copyImageToSurfaceLocked(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        int pixelStride = image.getPlanes()[0].getPixelStride();
        int rowStride = image.getPlanes()[0].getRowStride();
        int pixelBytesPerRow = pixelStride * image.getWidth();
        int rowPadding = rowStride - pixelBytesPerRow;

        // Remove the row padding bytes from the buffer before converting to a Bitmap
        ByteBuffer trimmedBuffer = ByteBuffer.allocate(buffer.remaining());
        buffer.rewind();
        while (buffer.hasRemaining()) {
            for (int i = 0; i < pixelBytesPerRow; ++i) {
                trimmedBuffer.put(buffer.get());
            }
            buffer.position(buffer.position() + rowPadding); // Skip the padding bytes
        }
        trimmedBuffer.flip(); // Prepare the trimmed buffer for reading

        Canvas canvas = mSurface.lockCanvas(null);
        Bitmap bitmap =
                Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(trimmedBuffer);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, mSurfaceWidth, mSurfaceHeight, false);
        // Draw the Bitmap onto the Canvas
        canvas.drawBitmap(scaled, 0f, 0f, null);

        bitmap.recycle();
        mSurface.unlockCanvasAndPost(canvas);
    }

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {
        if (mDisplay != null && displayId == mDisplay.getDisplayId()) {
            finishAndRemoveTask();
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {}
}
