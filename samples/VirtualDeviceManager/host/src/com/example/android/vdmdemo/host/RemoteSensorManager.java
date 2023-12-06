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

package com.example.android.vdmdemo.host;

import android.companion.virtual.sensor.VirtualSensor;
import android.companion.virtual.sensor.VirtualSensorCallback;
import android.companion.virtual.sensor.VirtualSensorEvent;
import android.os.SystemClock;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import com.example.android.vdmdemo.common.RemoteEventProto.RemoteEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.RemoteSensorEvent;
import com.example.android.vdmdemo.common.RemoteEventProto.SensorConfiguration;
import com.example.android.vdmdemo.common.RemoteIo;
import com.google.common.primitives.Floats;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class RemoteSensorManager implements AutoCloseable {

    private final RemoteIo mRemoteIo;
    private final SparseArray<VirtualSensor> mVirtualSensors = new SparseArray<>(); // Keyed by type
    private final Consumer<RemoteEvent> mRemoteEventConsumer = this::processRemoteEvent;

    private final VirtualSensorCallback mVirtualSensorCallback = new SensorCallback();

    private class SensorCallback implements VirtualSensorCallback {
        @Override
        public void onConfigurationChanged(
                VirtualSensor sensor,
                boolean enabled,
                @NonNull Duration samplingPeriod,
                @NonNull Duration batchReportLatency) {
            mRemoteIo.sendMessage(RemoteEvent.newBuilder()
                    .setSensorConfiguration(SensorConfiguration.newBuilder()
                            .setSensorType(sensor.getType())
                            .setEnabled(enabled)
                            .setSamplingPeriodUs(
                                    (int) TimeUnit.MICROSECONDS.convert(samplingPeriod))
                            .setBatchReportingLatencyUs(
                                    (int) TimeUnit.MICROSECONDS.convert(batchReportLatency)))
                    .build());
        }
    }

    RemoteSensorManager(RemoteIo remoteIo) {
        this.mRemoteIo = remoteIo;
        remoteIo.addMessageConsumer(mRemoteEventConsumer);
    }

    @Override
    public void close() {
        mVirtualSensors.clear();
        mRemoteIo.removeMessageConsumer(mRemoteEventConsumer);
    }

    VirtualSensorCallback getVirtualSensorCallback() {
        return mVirtualSensorCallback;
    }

    void setVirtualSensors(List<VirtualSensor> virtualSensorList) {
        for (VirtualSensor virtualSensor : virtualSensorList) {
            mVirtualSensors.put(virtualSensor.getType(), virtualSensor);
        }
    }

    void processRemoteEvent(RemoteEvent remoteEvent) {
        if (remoteEvent.hasSensorEvent()) {
            RemoteSensorEvent sensorEvent = remoteEvent.getSensorEvent();
            VirtualSensor sensor = mVirtualSensors.get(sensorEvent.getSensorType());
            if (sensor != null) {
                sensor.sendEvent(
                        new VirtualSensorEvent.Builder(Floats.toArray(sensorEvent.getValuesList()))
                                .setTimestampNanos(SystemClock.elapsedRealtimeNanos())
                                .build());
            }
        }
    }
}
