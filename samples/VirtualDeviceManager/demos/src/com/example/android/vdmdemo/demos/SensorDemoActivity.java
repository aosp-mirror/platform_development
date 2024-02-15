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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.BuildCompat;

import java.util.List;
import java.util.Objects;

/**
 * A minimal activity switching between sensor data from the default device and the virtual devices.
 */
public final class SensorDemoActivity extends AppCompatActivity implements SensorEventListener {

    private static final String DEVICE_NAME_UNKNOWN = "Unknown";
    private static final String DEVICE_NAME_DEFAULT = "Default - " + Build.MODEL;

    private VirtualDeviceManager mVirtualDeviceManager;
    private SensorManager mSensorManager;
    private View mBeam;
    private Context mDeviceContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sensor_demo_activity);

        mBeam = requireViewById(R.id.beam);

        mVirtualDeviceManager = getSystemService(VirtualDeviceManager.class);
        mSensorManager = getSystemService(SensorManager.class);

        registerDeviceIdChangeListener(getMainExecutor(), this::changeSensorDevice);

        mDeviceContext = this;
        changeSensorDevice(mDeviceContext.getDeviceId());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDeviceContext.unregisterDeviceIdChangeListener(this::changeSensorDevice);
        mSensorManager.unregisterListener(this);
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
                    changeSensorDevice(deviceId);
                });
        alertDialogBuilder.show();
    }

    private void changeSensorDevice(int deviceId) {
        mDeviceContext.unregisterDeviceIdChangeListener(this::changeSensorDevice);
        mSensorManager.unregisterListener(this);

        mDeviceContext = createDeviceContext(deviceId);

        updateCurrentDeviceTextView(mDeviceContext);

        mSensorManager = mDeviceContext.getSystemService(SensorManager.class);
        Objects.requireNonNull(mSensorManager);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor != null) {
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float z = event.values[2];
        float magnitude = (float) Math.sqrt(x * x + z * z);
        float angle = (float) (Math.signum(x) * Math.acos(z / magnitude));
        float angleDegrees = (float) Math.toDegrees(angle);
        mBeam.setRotation(angleDegrees);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
