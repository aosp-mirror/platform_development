/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ddmlib.IDevice;

import junit.framework.Test;

/**
 * Helper JUnit test that stores reference to a Android device and test data.
 */
public interface DeviceTest extends Test {

    /**
     * Sets the device under test
     * @param device the Android device to test
     */
    public void setDevice(IDevice device);

    /**
     * Retrieves the Android device under test
     * @return the {@link IDevice} device.
     */
    public IDevice getDevice();

    /**
     * Retrieves host file system path that contains test app files
     * @return {@link String} containing path, or <code>null</code>
     */
    public String getTestAppPath();

    /**
     * Sets host file system path that contains test app files
     * @param path absolute file system path to test data files
     */
    public void setTestAppPath(String path);
}
