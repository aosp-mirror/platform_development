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

package com.example.android.vdmdemo.demos;

import android.app.AlertDialog;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;

import java.util.List;
import java.util.Objects;

/** A minimal activity switching between vibration on the default device and the virtual devices. */
public final class VibrationDemoActivity extends AppCompatActivity {

    private static final String DEVICE_NAME_UNKNOWN = "Unknown";
    private static final String DEVICE_NAME_DEFAULT = "Default - " + Build.MODEL;

    private VirtualDeviceManager mVirtualDeviceManager;
    private Vibrator mVibrator;
    private Context mDeviceContext;

    private Ringtone mRingtone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vibration_demo_activity);

        mVirtualDeviceManager = getSystemService(VirtualDeviceManager.class);
        mVibrator = getSystemService(Vibrator.class);

        registerDeviceIdChangeListener(getMainExecutor(), this::changeVibratorDevice);

        mDeviceContext = this;
        changeVibratorDevice(mDeviceContext.getDeviceId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDeviceContext.unregisterDeviceIdChangeListener(this::changeVibratorDevice);
    }

    private void updateCurrentDeviceTextView(Context context) {
        String deviceName = DEVICE_NAME_UNKNOWN;
        if (context.getDeviceId() == Context.DEVICE_ID_DEFAULT) {
            deviceName = DEVICE_NAME_DEFAULT;
        } else if (BuildCompat.isAtLeastV()) {
            VirtualDevice device = mVirtualDeviceManager.getVirtualDevice(context.getDeviceId());
            deviceName = Objects.requireNonNull(device).getName();
        } else {
            for (VirtualDevice virtualDevice : mVirtualDeviceManager.getVirtualDevices()) {
                if (virtualDevice.getDeviceId() == context.getDeviceId()) {
                    deviceName = virtualDevice.getName();
                    break;
                }
            }
        }
        TextView currentDevice = requireViewById(R.id.current_device);
        currentDevice.setText(context.getString(R.string.current_device, deviceName));
    }

    /** Handle device change request. */
    public void onChangeDevice(View view) {
        List<VirtualDevice> virtualDevices = mVirtualDeviceManager.getVirtualDevices();
        String[] devices = new String[virtualDevices.size() + 1];
        devices[0] = DEVICE_NAME_DEFAULT;
        for (int i = 0; i < virtualDevices.size(); ++i) {
            devices[i + 1] = virtualDevices.get(i).getName();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Available devices");
        alertDialogBuilder.setItems(
                devices,
                (dialog, which) -> {
                    int deviceId =
                            which > 0
                                    ? virtualDevices.get(which - 1).getDeviceId()
                                    : Context.DEVICE_ID_DEFAULT;
                    changeVibratorDevice(deviceId);
                });
        alertDialogBuilder.show();
    }

    private void changeVibratorDevice(int deviceId) {
        mDeviceContext.unregisterDeviceIdChangeListener(this::changeVibratorDevice);
        mDeviceContext = createDeviceContext(deviceId);

        updateCurrentDeviceTextView(mDeviceContext);

        mVibrator = mDeviceContext.getSystemService(Vibrator.class);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mRingtone = RingtoneManager.getRingtone(mDeviceContext, uri);
        mRingtone.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setHapticChannelsMuted(false)
                        .build());
        mRingtone.setHapticGeneratorEnabled(true);
    }

    /** Handle vibration request. */
    public void onVibrate(View view) {
        mVibrator.vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    }

    /** Handle haptic feedback request. */
    public void onPerformHapticFeedback(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    /** Handle request for ringtone with haptics enabled. */
    public void onRingtoneVibration(View view) {
        if (mRingtone.isPlaying()) {
            mRingtone.stop();
        } else {
            mRingtone.play();
        }
    }
}
