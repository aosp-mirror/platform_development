/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class DebugBridge {
    private final Logger log = Logger.getLogger(DebugBridge.class.getName());

    private final List<IDevice> devices = Lists.newArrayList();

    private final AndroidDebugBridge bridge;

    public DebugBridge() {
        this.bridge = AndroidDebugBridge.createBridge();
    }

    public DebugBridge(AndroidDebugBridge bridge) {
        this.bridge = bridge;
    }

    /* package */ void addDevice(IDevice device) {
        devices.add(device);
    }

    /* package */ void removeDevice(IDevice device) {
        devices.remove(device);
    }

    public Collection<IDevice> getConnectedDevices() {
        if (devices.size() > 0) {
            return ImmutableList.copyOf(devices);
        }
        return Collections.emptyList();
    }

    public IDevice getPreferredDevice() {
        if (devices.size() > 0) {
            return devices.get(0);
        }
        return null;
    }

    public static DebugBridge createDebugBridge() {
        AndroidDebugBridge.init(false);

        final DebugBridge bridge = new DebugBridge(AndroidDebugBridge.createBridge());
        AndroidDebugBridge.addDeviceChangeListener(new IDeviceChangeListener() {
            public void deviceDisconnected(IDevice device) {
                bridge.removeDevice(device);
            }

            public void deviceConnected(IDevice device) {
                bridge.addDevice(device);
            }

            public void deviceChanged(IDevice device, int arg1) {
                // TODO Auto-generated method stub

            }
        });

        return bridge;
    }
}
