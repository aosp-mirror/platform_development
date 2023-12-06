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

import static android.view.Display.DEFAULT_DISPLAY;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;

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

    // Approximately, see
    // https://developer.android.com/reference/android/util/DisplayMetrics#density
    private static final float DIP_TO_DPI = 160f;

    @Inject ConnectionManager mConnectionManager;
    @Inject RemoteIo mRemoteIo;
    @Inject VirtualSensorController mSensorController;
    @Inject AudioPlayer mAudioPlayer;
    @Inject InputManager mInputManager;

    private DisplayController mDisplayController;
    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;

    private final ConnectionManager.ConnectionCallback mConnectionCallback =
            new ConnectionManager.ConnectionCallback() {

                @Override
                public void onDisconnected() {
                    finish();
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

        OnBackPressedCallback callback =
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        mInputManager.sendBack(DEFAULT_DISPLAY);
                    }
                };
        getOnBackPressedDispatcher().addCallback(this, callback);

        mDisplayController = new DisplayController(DEFAULT_DISPLAY, mRemoteIo);
        mDisplayController.setDpi((int) (getResources().getDisplayMetrics().density * DIP_TO_DPI));

        TextureView textureView = findViewById(R.id.immersive_surface_view);
        textureView.setOnTouchListener(
                (v, event) -> {
                    if (event.getDevice().supportsSource(InputDevice.SOURCE_TOUCHSCREEN)) {
                        textureView.getParent().requestDisallowInterceptTouchEvent(true);
                        mInputManager.sendInputEvent(
                                InputDeviceType.DEVICE_TYPE_TOUCHSCREEN, event, DEFAULT_DISPLAY);
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
                        mDisplayController.setSurface(new Surface(texture), width, height);
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture texture) {
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(
                            @NonNull SurfaceTexture texture, int width, int height) {}
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
        mDisplayController.close();
        mSensorController.close();
    }

    private void processRemoteEvent(RemoteEvent event) {
        if (event.hasStopStreaming() && !event.getStopStreaming().getPause()) {
            finish();
        } else if (event.hasStartStreaming()) {
            mDisplayController.sendDisplayCapabilities();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP -> {
                mInputManager.sendHome(DEFAULT_DISPLAY);
                return true;
            }
            case KeyEvent.KEYCODE_VOLUME_DOWN -> {
                finish();
                return true;
            }
            default -> {
                return super.dispatchKeyEvent(event);
            }
        }
    }
}
