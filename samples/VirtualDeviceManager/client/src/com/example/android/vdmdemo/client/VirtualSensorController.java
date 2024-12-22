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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteSensorEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.SensorCapabilities;
import com.example.android.vdmdemo.common.RemoteEventProto.SensorConfiguration;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.primitives.Floats;

import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.inject.Inject;

@ActivityScoped
final class VirtualSensorController implements AutoCloseable {

    private final RemoteIo mRemoteIo;
    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;
    private final SensorManager mSensorManager;
    private final HandlerThread mListenerThread;
    private final Handler mHandler;

    private final SensorEventListener mSensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    mRemoteIo.sendMessage(
                            RemoteEvent.newBuilder()
                                    .setSensorEvent(
                                            RemoteSensorEvent.newBuilder()
                                                    .setSensorType(event.sensor.getType())
                                                    .addAllValues(Floats.asList(event.values)))
                                    .build());
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            };

    @Inject
    VirtualSensorController(@ApplicationContext Context context, RemoteIo remoteIo) {
        mSensorManager = context.getSystemService(SensorManager.class);
        mRemoteIo = remoteIo;

        mListenerThread = new HandlerThread("VirtualSensorListener");
        mListenerThread.start();
        mHandler = new Handler(mListenerThread.getLooper());

        remoteIo.addMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    public void close() {
        mSensorManager.unregisterListener(mSensorEventListener);
        mListenerThread.quitSafely();
        mRemoteIo.removeMessageConsumer(mRemoteEventConsumer);
    }

    public List<SensorCapabilities> getSensorCapabilities() {
        // For demo purposes we only need a single accelerometer and proximity sensor.
        return Stream.of(
                        mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                        mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY))
                .filter(Objects::nonNull)
                .map(VirtualSensorController::createSensorCapabilitiesFromSensor)
                .toList();
    }

    private static SensorCapabilities createSensorCapabilitiesFromSensor(Sensor sensor) {
        return SensorCapabilities.newBuilder()
                .setType(sensor.getType())
                .setName(sensor.getName())
                .setVendor(sensor.getVendor())
                .setMaxRange(sensor.getMaximumRange())
                .setResolution(sensor.getResolution())
                .setPower(sensor.getPower())
                .setMinDelayUs(sensor.getMinDelay())
                .setMaxDelayUs(sensor.getMaxDelay())
                .setIsWakeUpSensor(sensor.isWakeUpSensor())
                .setReportingMode(sensor.getReportingMode())
                .build();
    }

    private void processRemoteEvent(RemoteEvent remoteEvent) {
        if (!remoteEvent.hasSensorConfiguration()) {
            return;
        }
        SensorConfiguration config = remoteEvent.getSensorConfiguration();
        Sensor sensor = mSensorManager.getDefaultSensor(config.getSensorType());
        if (sensor == null) {
            return;
        }
        if (config.getEnabled()) {
            mSensorManager.registerListener(
                    mSensorEventListener,
                    sensor,
                    config.getSamplingPeriodUs(),
                    config.getBatchReportingLatencyUs(),
                    mHandler);
        } else {
            mSensorManager.unregisterListener(mSensorEventListener, sensor);
        }
    }
}
