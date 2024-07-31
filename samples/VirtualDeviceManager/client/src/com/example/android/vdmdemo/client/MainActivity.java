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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.android.vdmdemo.common.ConnectionManager;
import com.example.android.vdmdemo.common.DpadFragment;
import com.example.android.vdmdemo.common.NavTouchpadFragment;
import com.example.android.vdmdemo.common.RemoteEventProto.DeviceCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.InputDeviceType;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteIo;
import com.example.android.vdmdemo.common.RotaryFragment;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.function.Consumer;

import javax.inject.Inject;

/**
 * VDM Client activity, showing apps running on a host device and sending input back to the host.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class MainActivity extends Hilt_MainActivity {
    private static final String TAG = "VdmClient";

    @Inject RemoteIo mRemoteIo;
    @Inject ConnectionManager mConnectionManager;
    @Inject InputManager mInputManager;
    @Inject VirtualSensorController mSensorController;

    @Inject VirtualCameraController mVirtualCameraController;
    @Inject AudioPlayer mAudioPlayer;
    @Inject AudioRecorder mAudioRecorder;

    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;
    private DisplayAdapter mDisplayAdapter;
    private InputMethodManager mInputMethodManager;
    private final ActivityResultLauncher<String> mRequestPermissionLauncher =
            registerForActivityResult(new RequestPermission(), isGranted -> {
                if (isGranted) {
                    mRemoteIo.addMessageConsumer(mAudioRecorder);
                } else {
                    mRemoteIo.removeMessageConsumer(mAudioRecorder);
                }
            });

    private final Consumer<ConnectionManager.ConnectionStatus> mConnectionCallback =
            (status) -> {
                if (status.state == ConnectionManager.ConnectionStatus.State.CONNECTED) {
                    boolean supportsAudioOutput =
                            MainActivity.this.getPackageManager().hasSystemFeature(
                                    PackageManager.FEATURE_AUDIO_OUTPUT);
                    boolean supportsAudioInput = hasRecordAudioPermission(MainActivity.this);
                    mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                            .setDeviceCapabilities(DeviceCapabilities.newBuilder()
                                    .setDeviceName(Build.MODEL)
                                    .addAllSensorCapabilities(
                                            mSensorController.getSensorCapabilities())
                                    .addAllCameraCapabilities(
                                            mVirtualCameraController.getCameraCapabilities())
                                    .setSupportsAudioOutput(supportsAudioOutput)
                                    .setSupportsAudioInput(supportsAudioInput)
                            ).build());
                } else {
                    if (mDisplayAdapter != null) {
                        runOnUiThread(mDisplayAdapter::clearDisplays);
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = requireViewById(R.id.main_tool_bar);
        setSupportActionBar(toolbar);

        ClientView displaysView = requireViewById(R.id.displays);
        displaysView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        displaysView.setItemAnimator(null);
        mDisplayAdapter = new DisplayAdapter(displaysView, mRemoteIo, mInputManager);
        displaysView.setAdapter(mDisplayAdapter);

        ActivityResultLauncher<Intent> fullscreenLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                mDisplayAdapter::onFullscreenActivityResult);
        mDisplayAdapter.setFullscreenLauncher(fullscreenLauncher);

        mInputMethodManager = getSystemService(InputMethodManager.class);

        DpadFragment dpadFragment =
                (DpadFragment) getSupportFragmentManager().findFragmentById(
                        R.id.dpad_fragment_container);
        dpadFragment.setInputEventListener((event) ->
                mInputManager.sendInputEventToFocusedDisplay(
                        InputDeviceType.DEVICE_TYPE_DPAD, event));
        NavTouchpadFragment navTouchpadFragment =
                (NavTouchpadFragment) getSupportFragmentManager().findFragmentById(
                        R.id.nav_touchpad_fragment_container);
        navTouchpadFragment.setInputEventListener((event) ->
                mInputManager.sendInputEventToFocusedDisplay(
                        InputDeviceType.DEVICE_TYPE_NAVIGATION_TOUCHPAD, event));
        RotaryFragment rotaryFragment =
                (RotaryFragment) getSupportFragmentManager().findFragmentById(
                        R.id.rotary_fragment_container);
        rotaryFragment.setInputEventListener((event) ->
                mInputManager.sendInputEventToFocusedDisplay(
                        InputDeviceType.DEVICE_TYPE_ROTARY_ENCODER, event));

        mConnectionManager.startClientSession();
    }

    @Override
    public void onStart() {
        super.onStart();
        mConnectionManager.addConnectionCallback(mConnectionCallback);
        mRemoteIo.addMessageConsumer(mAudioPlayer);
        mRemoteIo.addMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    public void onResume() {
        super.onResume();
        mDisplayAdapter.resumeAllDisplays();

        if (hasRecordAudioPermission(this)) {
            mRemoteIo.addMessageConsumer(mAudioRecorder);
        } else {
            mRequestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mDisplayAdapter.pauseAllDisplays();
        mAudioRecorder.stop();
        mRemoteIo.removeMessageConsumer(mAudioRecorder);
    }

    @Override
    public void onStop() {
        super.onStop();
        mConnectionManager.removeConnectionCallback(mConnectionCallback);
        mRemoteIo.removeMessageConsumer(mRemoteEventConsumer);
        mRemoteIo.removeMessageConsumer(mAudioPlayer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDisplayAdapter.clearDisplays();
        mConnectionManager.disconnect();
        mSensorController.close();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mInputManager.sendInputEventToFocusedDisplay(
                        InputDeviceType.DEVICE_TYPE_KEYBOARD, event)
                || super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.input -> toggleInputVisibility();
            default -> {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    private void processRemoteEvent(RemoteEvent event) {
        if (event.hasStartStreaming()) {
            runOnUiThread(
                    () -> mDisplayAdapter.addDisplay(event.getStartStreaming().getHomeEnabled(),
                            event.getStartStreaming().getRotationSupported()));
        } else if (event.hasStopStreaming()) {
            runOnUiThread(() -> mDisplayAdapter.removeDisplay(event.getDisplayId()));
        } else if (event.hasDisplayRotation()) {
            runOnUiThread(() -> mDisplayAdapter.rotateDisplay(
                    event.getDisplayId(), event.getDisplayRotation().getRotationDegrees()));
        } else if (event.hasDisplayChangeEvent()) {
            runOnUiThread(() -> mDisplayAdapter.processDisplayChange(event));
        } else if (event.hasKeyboardVisibilityEvent()) {
            if (event.getKeyboardVisibilityEvent().getVisible()) {
                mInputMethodManager.showSoftInput(getWindow().getDecorView(), 0);
            } else {
                mInputMethodManager.hideSoftInputFromWindow(
                        getWindow().getDecorView().getWindowToken(), 0);
            }
        }
    }

    private void toggleInputVisibility() {
        View dpad = requireViewById(R.id.dpad_fragment_container);
        int visibility = dpad.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        dpad.setVisibility(visibility);
        requireViewById(R.id.nav_touchpad_fragment_container).setVisibility(visibility);
        requireViewById(R.id.rotary_fragment_container).setVisibility(visibility);
    }

    private static boolean hasRecordAudioPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
}
