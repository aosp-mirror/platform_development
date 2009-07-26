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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestResult;
import junit.textui.TestRunner;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener;

/**
 * Command line interface for running DeviceTest tests.
 *
 * Extends junit.textui.TestRunner to handle optional -s (device serial) and -p (test data)
 * arguments, and then pass their values to the instantiated DeviceTests.
 *
 * Provided test class must be a DeviceTest.
 *
 * @see junit.textui.TestRunner for more information on command line syntax.
 */
public class DeviceTestRunner extends TestRunner {

    private static final String LOG_TAG = "DeviceTestRunner";
    private String mDeviceSerial = null;
    private IDevice mDevice = null;
    private String mTestDataPath = null;

    private DeviceTestRunner() {
    }

    /**
     * Starts the test run.
     * Extracts out DeviceTestCase specific command line arguments, then passes control to parent
     * TestRunner.
     * @param args command line arguments
     * @return {@link TestResult}
     */
    @Override
    public TestResult start(String[] args) throws Exception {
        // holds unprocessed arguments to pass to parent
        List<String> parentArgs = new ArrayList<String>();
        for (int i=0; i < args.length; i++) {
            if (args[i].equals("-s")) {
                i++;
                mDeviceSerial = extractArg(args, i);
            } else if (args[i].equals("-p")) {
                i++;
                mTestDataPath = extractArg(args, i);
            } else {
                // unrecognized arg, must be for parent
                parentArgs.add(args[i]);
            }
        }
        mDevice = connectToDevice(mDeviceSerial);
        return super.start(parentArgs.toArray(new String[parentArgs.size()]));
    }

    private String extractArg(String[] args, int index) {
        if (args.length <= index) {
            printUsage();
            throw new IllegalArgumentException("Error: not enough arguments");
        }
        return args[index];
    }

    /**
     * Initializes DDMS library, and connects to specified Android device
     * @param deviceSerial
     * @return {@link IDevice}
     * @throws IllegalArgumentException if specified device cannot be found
     */
    private IDevice connectToDevice(String deviceSerial) {
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
        System.out.println("Waiting for device...");
        NewDeviceListener listener = new NewDeviceListener(deviceSerial);
        AndroidDebugBridge.addDeviceChangeListener(listener);
        IDevice device = listener.waitForDevice(5000);
        AndroidDebugBridge.removeDeviceChangeListener(listener);
        if (device == null) {
            throw new IllegalArgumentException("Could not connect to device");
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

    /**
     * Main entry point.
     *
     * Establishes connection to provided adb device and runs tests
     *
     * @param args expects:
     *     test class to run
     *     optionally, device serial number. If unspecified, will connect to first device found
     *     optionally, file system path to test data files
     */
    public static void main(String[] args) {
        DeviceTestRunner aTestRunner = new DeviceTestRunner();
        try {
            TestResult r = aTestRunner.start(args);
            if (!r.wasSuccessful())
                System.exit(FAILURE_EXIT);
            System.exit(SUCCESS_EXIT);
        } catch(Exception e) {
            System.err.println(e.getMessage());
            System.exit(EXCEPTION_EXIT);
        }
    }

    private static void printUsage() {
        System.out.println("Usage: DeviceTestRunner <test_class> [-s device_serial] " +
                "[-p test_data_path]");
    }

    /**
     * Override parent to set DeviceTest data
     */
    @Override
    public TestResult doRun(Test test, boolean wait) {
        if (test instanceof DeviceTest) {
            DeviceTest deviceTest = (DeviceTest)test;
            deviceTest.setDevice(mDevice);
            deviceTest.setTestAppPath(mTestDataPath);
        } else {
            Log.w(LOG_TAG, String.format("%s test class is not a DeviceTest.",
                    test.getClass().getName()));
        }
        return super.doRun(test, wait);
    }

    /**
     * Override parent to create DeviceTestSuite wrapper, instead of TestSuite
     */
    @Override
    protected TestResult runSingleMethod(String testCase, String method, boolean wait)
    throws Exception {
        Class testClass = loadSuiteClass(testCase);
        Test test = DeviceTestSuite.createTest(testClass, method);
        return doRun(test, wait);
    }
}
