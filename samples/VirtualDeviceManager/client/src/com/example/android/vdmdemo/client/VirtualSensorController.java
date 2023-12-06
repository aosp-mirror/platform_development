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
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

@ActivityScoped
final class VirtualSensorController implements AutoCloseable {

    private final RemoteIo remoteIo;
    private final Consumer<RemoteEvent> remoteEventConsumer = this::processRemoteEvent;
    private final SensorManager sensorManager;
    private final HandlerThread listenerThread;
    private final Handler handler;

    private final SensorEventListener sensorEventListener =
            new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    remoteIo.sendMessage(
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
        this.sensorManager = context.getSystemService(SensorManager.class);
        this.remoteIo = remoteIo;

        listenerThread = new HandlerThread("VirtualSensorListener");
        listenerThread.start();
        handler = new Handler(listenerThread.getLooper());

        remoteIo.addMessageConsumer(remoteEventConsumer);
    }

    @Override
    public void close() {
        sensorManager.unregisterListener(sensorEventListener);
        listenerThread.quitSafely();
        remoteIo.removeMessageConsumer(remoteEventConsumer);
    }

    public List<SensorCapabilities> getSensorCapabilities() {
        return sensorManager.getSensorList(Sensor.TYPE_ALL).stream()
                .map(
                        sensor ->
                                SensorCapabilities.newBuilder()
                                        .setType(sensor.getType())
                                        .setName(sensor.getName())
                                        .setVendor(sensor.getVendor())
                                        .setMaxRange(sensor.getMaximumRange())
                                        .setResolution(sensor.getResolution())
                                        .setPower(sensor.getPower())
                                        .setMinDelayUs(sensor.getMinDelay())
                                        .setMaxDelayUs(sensor.getMaxDelay())
                                        .build())
                .collect(Collectors.toList());
    }

    private void processRemoteEvent(RemoteEvent remoteEvent) {
        if (!remoteEvent.hasSensorConfiguration()) {
            return;
        }
        SensorConfiguration config = remoteEvent.getSensorConfiguration();
        Sensor sensor = sensorManager.getDefaultSensor(config.getSensorType());
        if (sensor == null) {
            return;
        }
        if (config.getEnabled()) {
            sensorManager.registerListener(
                    sensorEventListener,
                    sensor,
                    config.getSamplingPeriodUs(),
                    config.getBatchReportingLatencyUs(),
                    handler);
        } else {
            sensorManager.unregisterListener(sensorEventListener, sensor);
        }
    }
}
