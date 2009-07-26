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

import junit.framework.TestCase;

/**
 * Helper JUnit test case that stores reference to an Android device and test data.
 *
 * Can be extended to verify an Android device's response to various adb commands.
 */
public abstract class DeviceTestCase extends TestCase implements DeviceTest {

    /** Android device under test */
    private IDevice mDevice = null;
    /** optionally, used to store path to test data files */
    private String mTestDataPath = null;

    protected DeviceTestCase() {
    }

    /**
     * {@inheritDoc}
     */
    public void setDevice(IDevice device) {
        mDevice = device;
    }

    /**
     * {@inheritDoc}
     */
    public IDevice getDevice() {
        return mDevice;
    }

    /**
     * {@inheritDoc}
     */
    public String getTestAppPath() {
        return mTestDataPath;
    }

    /**
     * {@inheritDoc}
     */
    public void setTestAppPath(String path) {
        mTestDataPath = path;
    }

    @Override
    protected void setUp() throws Exception {
        // ensure device has been set before test is run
        assertNotNull(getDevice());
    }
}
