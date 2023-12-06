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

import java.util.List;

/** A minimal activity switching between vibration on the default device and the virtual devices. */
public final class VibrationDemoActivity extends AppCompatActivity {

    private static final String DEVICE_NAME_UNKNOWN = "Unknown";
    private static final String DEVICE_NAME_DEFAULT = "Default - " + Build.MODEL;

    private VirtualDeviceManager vdm;
    private Vibrator vibrator;
    private Context deviceContext;

    private Ringtone ringtone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.vibration_demo_activity);

        vdm = getSystemService(VirtualDeviceManager.class);
        vibrator = getSystemService(Vibrator.class);
        deviceContext = this;

        changeVibratorDevice(deviceContext.getDeviceId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deviceContext.unregisterDeviceIdChangeListener(this::changeVibratorDevice);
    }

    private void updateCurrentDeviceTextView(Context context) {
        String deviceName = DEVICE_NAME_UNKNOWN;
        if (context.getDeviceId() == Context.DEVICE_ID_DEFAULT) {
            deviceName = DEVICE_NAME_DEFAULT;
        } else {
            for (VirtualDevice virtualDevice : vdm.getVirtualDevices()) {
                if (virtualDevice.getDeviceId() == context.getDeviceId()) {
                    deviceName = virtualDevice.getName();
                    break;
                }
            }
        }
        TextView currentDevice = findViewById(R.id.current_device);
        currentDevice.setText(context.getString(R.string.current_device, deviceName));
    }

    public void onChangeDevice(View view) {
        List<VirtualDevice> virtualDevices = vdm.getVirtualDevices();
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
        deviceContext.unregisterDeviceIdChangeListener(this::changeVibratorDevice);
        deviceContext = createDeviceContext(deviceId);
        deviceContext.registerDeviceIdChangeListener(getMainExecutor(), this::changeVibratorDevice);

        updateCurrentDeviceTextView(deviceContext);

        vibrator = deviceContext.getSystemService(Vibrator.class);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(deviceContext, uri);
        ringtone.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setHapticChannelsMuted(false)
                        .build());
        ringtone.setHapticGeneratorEnabled(true);
    }

    public void onVibrate(View view) {
        vibrator.vibrate(VibrationEffect.EFFECT_HEAVY_CLICK);
    }

    public void onPerformHapticFeedback(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
    }

    public void onRingtoneVibration(View view) {
        if (ringtone.isPlaying()) {
            ringtone.stop();
        } else {
            ringtone.play();
        }
    }
}
