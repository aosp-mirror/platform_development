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
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * Helper JUnit test suite that stores reference to an Android device and test data.
 */
public class DeviceTestSuite extends TestSuite implements DeviceTest {

    private IDevice mDevice = null;
    private String mTestDataPath = null;

    public DeviceTestSuite(Class testClass) {
        super(testClass);
    }

    public DeviceTestSuite() {
        super();
    }

    /**
     * Adds the tests from the given class to the suite
     */
    @Override
    public void addTestSuite(Class testClass) {
        addTest(new DeviceTestSuite(testClass));
    }

    /**
     * Overrides parent method to pass in device and test app path to included test
     */
    @Override
    public void runTest(Test test, TestResult result) {
        if (test instanceof DeviceTest) {
            DeviceTest deviceTest = (DeviceTest)test;
            deviceTest.setDevice(mDevice);
            deviceTest.setTestAppPath(mTestDataPath);
        }
        test.run(result);
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
    public void setDevice(IDevice device) {
        mDevice = device;
    }

    /**
     * {@inheritDoc}
     */
    public void setTestAppPath(String path) {
        mTestDataPath = path;
    }
}
