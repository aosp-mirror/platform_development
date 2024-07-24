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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.android.vdmdemo.common.ConnectionManager;
import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

import javax.inject.Inject;

/**
 * VDM Client activity, showing apps running on a host device and sending input back to the host.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class ImmersiveActivity extends Hilt_ImmersiveActivity {

    private static final String TAG = "VdmClientImmersiveActivity";

    static final String EXTRA_DISPLAY_ID = "displayId";
    static final String EXTRA_REQUESTED_ROTATION = "requestedRotation";

    static final int RESULT_MINIMIZE = 1;
    static final int RESULT_CLOSE = 2;

    // Approximately, see
    // https://developer.android.com/reference/android/util/DisplayMetrics#density
    private static final float DIP_TO_DPI = 160f;

    @Inject ConnectionManager mConnectionManager;
    @Inject RemoteIo mRemoteIo;
    @Inject VirtualSensorController mSensorController;
    @Inject AudioPlayer mAudioPlayer;
    @Inject InputManager mInputManager;

    private int mDisplayId = Display.INVALID_DISPLAY;
    private DisplayController mDisplayController;
    private Surface mSurface;
    private InputMethodManager mInputMethodManager;

    private int mPortraitWidth;
    private int mPortraitHeight;
    private int mRequestedRotation = 0;

    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;

    private final Consumer<ConnectionManager.ConnectionStatus> mConnectionCallback =
            (status) -> {
                if (status.state == ConnectionManager.ConnectionStatus.State.DISCONNECTED) {
                    finish(/* minimize= */ false);
                }
            };

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_immersive);

        WindowInsetsControllerCompat windowInsetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        mInputMethodManager = getSystemService(InputMethodManager.class);

        mDisplayId = getIntent().getIntExtra(EXTRA_DISPLAY_ID, Display.INVALID_DISPLAY);

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        mInputManager.sendBack(mDisplayId);
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, callback);

        mDisplayController = new DisplayController(mDisplayId, mRemoteIo);
        mDisplayController.setDpi((int) (getResources().getDisplayMetrics().density * DIP_TO_DPI));

        TextureView textureView = requireViewById(R.id.immersive_surface_view);
        textureView.setOnTouchListener(
                (v, event) -> {
                    if (event.getDevice().supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                        textureView.getParent().requestDisallowInterceptTouchEvent(true);
                        mInputManager.sendInputEvent(
                                InputDeviceType.DEVICE_TYPE_TOUCHSCREEN, event, mDisplayId);
                    }
                    return true;
                });
        textureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture texture) {}

                    @Override
                    public void onSurfaceTextureAvailable(
                            @NonNull SurfaceTexture texture, int width, int height) {
                        Log.v(TAG, "Setting surface for immersive display " + mDisplayId);
                        mSurface = new Surface(texture);
                        mPortraitWidth = Math.min(width, height);
                        mPortraitHeight = Math.max(width, height);
                        mDisplayController.setSurface(mSurface, width, height);
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
                        Log.v(TAG, "onSurfaceTextureDestroyed for immersive display " + mDisplayId);
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            @NonNull SurfaceTexture texture, int width, int height) {}
                });
        textureView.setOnGenericMotionListener(
                (v, event) -> {
                    if (event.getDevice() == null
                            || !event.getDevice().supportsSource(InputDevice.SOURCE_MOUSE)) {
                        return false;
                    }
                    mInputManager.sendInputEvent(
                            InputDeviceType.DEVICE_TYPE_MOUSE, event, mDisplayId);
                    return true;
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        mConnectionManager.addConnectionCallback(mConnectionCallback);
        mRemoteIo.addMessageConsumer(mAudioPlayer);
        mRemoteIo.addMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    public void onStop() {
        super.onStop();
        mConnectionManager.removeConnectionCallback(mConnectionCallback);
        mRemoteIo.removeMessageConsumer(mAudioPlayer);
        mRemoteIo.removeMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorController.close();
    }

    private void processRemoteEvent(RemoteEvent event) {
        if (event.hasStopStreaming()) {
            finish(/* minimize= */ false);
        } else if (event.hasDisplayRotation()) {
            mRequestedRotation = event.getDisplayRotation().getRotationDegrees();
        } else if (event.hasKeyboardVisibilityEvent()) {
            if (event.getKeyboardVisibilityEvent().getVisible()) {
                mInputMethodManager.showSoftInput(getWindow().getDecorView(), 0);
            } else {
                mInputMethodManager.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP -> mInputManager.sendHome(mDisplayId);
            case KeyEvent.KEYCODE_VOLUME_DOWN -> finish(/* minimize= */ true);
            case KeyEvent.KEYCODE_BACK -> {
                return super.dispatchKeyEvent(event);
            }
            default -> mInputManager.sendInputEvent(
                    InputDeviceType.DEVICE_TYPE_KEYBOARD, event, mDisplayId);
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d(TAG, "Switching landscape");
            mDisplayController.setSurface(
                    mSurface, /* width= */ mPortraitHeight, /* height= */ mPortraitWidth);
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Switching to portrait");
            mDisplayController.setSurface(mSurface, mPortraitWidth, mPortraitHeight);
        }
    }

    private void finish(boolean minimize) {
        if (minimize) {
            mDisplayController.pause();
        } else {
            mDisplayController.close();
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_DISPLAY_ID, mDisplayId);
        result.putExtra(EXTRA_REQUESTED_ROTATION, mRequestedRotation);
        setResult(minimize ? RESULT_MINIMIZE : RESULT_CLOSE, result);
        finish();
    }
}
