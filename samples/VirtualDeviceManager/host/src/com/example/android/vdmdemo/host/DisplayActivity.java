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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android.vdmdemo.common.EdgeToEdgeUtils;
import com.example.android.vdmdemo.common.RemoteEventProto;

import dagger.hilt.android.AndroidEntryPoint;

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

    static final String EXTRA_DISPLAY_ID = "displayId";

    @Inject
    InputController mInputController;

    DisplayManager mDisplayManager;

    private VdmService mVdmService = null;
    private int mDisplayId;
    private Surface mSurface;
    private int mSurfaceWidth;
    private int mSurfaceHeight;
    private int mDpi;

    private RemoteDisplay mDisplay;
    private boolean mPoweredOn = true;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "Connected to VDM Service");
            mVdmService = ((VdmService.LocalBinder) binder).getService();
            createDisplay();
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
        Toolbar toolbar = requireViewById(R.id.main_tool_bar);
        setSupportActionBar(toolbar);
        setTitle(getTitle() + " " + mDisplayId);
        EdgeToEdgeUtils.applyTopInsets(toolbar);

        TextureView textureView = requireViewById(R.id.display_surface_view);
        EdgeToEdgeUtils.applyBottomInsets(textureView);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {}

            @Override
            public void onSurfaceTextureAvailable(
                    @NonNull SurfaceTexture texture, int width, int height) {
                Log.v(TAG, "Setting surface for local display " + mDisplayId);
                mSurface = new Surface(texture);
                mSurfaceWidth = width;
                mSurfaceHeight = height;
                createDisplay();
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
                if (mDisplay != null) {
                    mDisplay.reset(width, height, mDpi);
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
                finish();
                return true;
            case R.id.pip:
                // TODO(b/404803361): enter PiP
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

    private synchronized void createDisplay() {
        if (mVdmService == null || mSurface == null || mDisplay != null) {
            return;
        }

        mDisplay = mVdmService.createRemoteDisplay(
                this, mDisplayId, mSurfaceWidth, mSurfaceHeight, mDpi, mSurface, null);
    }

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {
        if (mDisplay != null && displayId == mDisplay.getDisplayId()) {
            finish();
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {}
}
