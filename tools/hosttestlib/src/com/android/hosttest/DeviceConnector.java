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
package com.android.hosttest;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;


/**
 * A helper class that can connect to a ddmlib {@link IDevice}
 */
public class DeviceConnector {

    /**
     * The maximum time to wait for a device to be connected.
     */
    private static final int MAX_WAIT_DEVICE_TIME = 5000;

    /**
     * Initializes DDMS library, and connects to specified Android device
     *
     * @param deviceSerial the device serial to connect to. If <code>null</code> connect to first
     * discovered device.
     *
     * @return the {@link IDevice} found
     * @throws IllegalArgumentException if no device cannot be found.
     */
    public IDevice connectToDevice(String deviceSerial) {
        // initialize DDMS with no clientSupport aka debugger support
        AndroidDebugBridge.init(false /* clientSupport */);
        AndroidDebugBridge adbBridge = AndroidDebugBridge.createBridge();
        for (IDevice device : adbBridge.getDevices()) {
            if (deviceSerial == null) {
                return device;
            } else if (deviceSerial.equals(device.getSerialNumber())) {
                return device;
            }
        }
        // TODO: get some sort of logger interface as param instead
        System.out.println("Waiting for device...");
        NewDeviceListener listener = new NewDeviceListener(deviceSerial);
        AndroidDebugBridge.addDeviceChangeListener(listener);
        IDevice device = listener.waitForDevice(MAX_WAIT_DEVICE_TIME);
        AndroidDebugBridge.removeDeviceChangeListener(listener);
        if (device == null) {
            throw new IllegalArgumentException("Could not connect to device");
        } else {
            System.out.println(String.format("Connected to %s", device.getSerialNumber()));
        }
        return device;
    }

    /**
     * Listener for new Android devices
     */
    private static class NewDeviceListener implements IDeviceChangeListener {
        private IDevice mDevice;
        private String mSerial;

        public NewDeviceListener(String serial) {
            mSerial = serial;
        }

        public void deviceChanged(IDevice device, int changeMask) {
        }

        public void deviceConnected(IDevice device) {
            if (mSerial == null) {
                setDevice(device);
            } else if (mSerial.equals(device.getSerialNumber())) {
                setDevice(device);
            }
        }

        private synchronized void setDevice(IDevice device) {
            mDevice = device;
            notify();
        }

        public void deviceDisconnected(IDevice device) {
        }

        public IDevice waitForDevice(long waitTime) {
            synchronized(this) {
                if (mDevice == null) {
                    try {
                        wait(waitTime);
                    } catch (InterruptedException e) {
                        System.out.println("Waiting for device interrupted");
                    }
                }
            }
            return mDevice;
        }
    }
}
