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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.android.vdmdemo.common.ConnectionManager;
import com.example.android.vdmdemo.common.RemoteEventProto.DeviceCapabilities;
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
public class MainActivity extends Hilt_MainActivity {

    @Inject RemoteIo mRemoteIo;
    @Inject ConnectionManager mConnectionManager;
    @Inject InputManager mInputManager;
    @Inject VirtualSensorController mSensorController;
    @Inject AudioPlayer mAudioPlayer;
    @Inject Settings mSettings;

    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;
    private DisplayAdapter mDisplayAdapter;
    private final InputManager.FocusListener mFocusListener = this::onDisplayFocusChange;

    private final ConnectionManager.ConnectionCallback mConnectionCallback =
            new ConnectionManager.ConnectionCallback() {
                @Override
                public void onConnected(String remoteDeviceName) {
                    mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                            .setDeviceCapabilities(DeviceCapabilities.newBuilder()
                                    .setDeviceName(Build.MODEL)
                                    .addAllSensorCapabilities(
                                            mSensorController.getSensorCapabilities()))
                            .build());
                }

                @Override
                public void onDisconnected() {
                    if (mDisplayAdapter != null) {
                        runOnUiThread(mDisplayAdapter::clearDisplays);
                    }
                    mConnectionManager.startClientSession();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.main_tool_bar);
        toolbar.setOverflowIcon(getDrawable(R.drawable.settings));
        setSupportActionBar(toolbar);

        ClientView displaysView = findViewById(R.id.displays);
        displaysView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        displaysView.setItemAnimator(null);
        mDisplayAdapter = new DisplayAdapter(displaysView, mRemoteIo, mInputManager);
        displaysView.setAdapter(mDisplayAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        mConnectionManager.addConnectionCallback(mConnectionCallback);
        mConnectionManager.startClientSession();
        mInputManager.addFocusListener(mFocusListener);
        mRemoteIo.addMessageConsumer(mAudioPlayer);
        mRemoteIo.addMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    public void onStop() {
        super.onStop();
        mInputManager.removeFocusListener(mFocusListener);
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
        if (event.getDevice() == null
                || !event.getDevice().supportsSource(InputDevice.SOURCE_KEYBOARD)) {
            return false;
        }
        mInputManager.sendInputEventToFocusedDisplay(InputDeviceType.DEVICE_TYPE_KEYBOARD, event);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings, menu);
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);
            switch (item.getItemId()) {
                case R.id.enable_dpad -> item.setChecked(mSettings.dpadEnabled);
                case R.id.enable_nav_touchpad -> item.setChecked(mSettings.navTouchpadEnabled);
                case R.id.enable_external_keyboard -> item.setChecked(
                        mSettings.externalKeyboardEnabled);
                case R.id.enable_external_mouse -> item.setChecked(mSettings.externalMouseEnabled);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        item.setChecked(!item.isChecked());

        switch (item.getItemId()) {
            case R.id.enable_dpad -> mSettings.dpadEnabled = item.isChecked();
            case R.id.enable_nav_touchpad -> mSettings.navTouchpadEnabled = item.isChecked();
            case R.id.enable_external_keyboard ->
                    mSettings.externalKeyboardEnabled = item.isChecked();
            case R.id.enable_external_mouse -> {
                mSettings.externalMouseEnabled = item.isChecked();
                return true;
            }
            default -> {
                return super.onOptionsItemSelected(item);
            }
        }

        mInputManager.updateFocusTracking();
        return true;
    }

    private void processRemoteEvent(RemoteEvent event) {
        if (event.hasStartStreaming()) {
            if (event.getStartStreaming().getImmersive()) {
                startActivity(new Intent(this, ImmersiveActivity.class));
            } else {
                runOnUiThread(
                        () ->
                                mDisplayAdapter.addDisplay(
                                        event.getStartStreaming().getHomeEnabled()));
            }
        } else if (event.hasStopStreaming()) {
            runOnUiThread(() -> mDisplayAdapter.removeDisplay(event.getDisplayId()));
        } else if (event.hasDisplayRotation()) {
            runOnUiThread(() -> mDisplayAdapter.rotateDisplay(event));
        } else if (event.hasDisplayChangeEvent()) {
            runOnUiThread(() -> mDisplayAdapter.processDisplayChange(event));
        }
    }

    private void onDisplayFocusChange(int displayId) {
        findViewById(R.id.dpad_fragment_container)
                .setVisibility(
                        mSettings.dpadEnabled && displayId != Display.INVALID_DISPLAY
                                ? View.VISIBLE
                                : View.GONE);
        findViewById(R.id.nav_touchpad_fragment_container)
                .setVisibility(
                        mSettings.navTouchpadEnabled && displayId != Display.INVALID_DISPLAY
                                ? View.VISIBLE
                                : View.GONE);
    }
}
