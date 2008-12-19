/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ddmlib.testrunner;


import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;

import java.io.IOException;

/**
 * Runs a Android test command remotely and reports results
 */
public class RemoteAndroidTestRunner  {

    private static final char CLASS_SEPARATOR = ',';
    private static final char METHOD_SEPARATOR = '#';
    private static final char RUNNER_SEPARATOR = '/';
    private String mClassArg;
    private final String mPackageName;
    private final  String mRunnerName;
    private String mExtraArgs;
    private boolean mLogOnlyMode;
    private IDevice mRemoteDevice;
    private InstrumentationResultParser mParser;

    private static final String LOG_TAG = "RemoteAndroidTest";
    private static final String DEFAULT_RUNNER_NAME = 
        "android.test.InstrumentationTestRunner";
    
    /**
     * Creates a remote android test runner.
     * @param packageName - the Android application package that contains the tests to run 
     * @param runnerName - the instrumentation test runner to execute. If null, will use default
     *   runner 
     * @param remoteDevice - the Android device to execute tests on
     */
    public RemoteAndroidTestRunner(String packageName, 
                                   String runnerName,
                                   IDevice remoteDevice) {
        
        mPackageName = packageName;
        mRunnerName = runnerName;
        mRemoteDevice = remoteDevice;  
        mClassArg = null;
        mExtraArgs = "";
        mLogOnlyMode = false;
    }
    
    /**
     * Alternate constructor. Uses default instrumentation runner
     * @param packageName - the Android application package that contains the tests to run 
     * @param remoteDevice - the Android device to execute tests on
     */
    public RemoteAndroidTestRunner(String packageName, 
                                   IDevice remoteDevice) {
        this(packageName, null, remoteDevice);
    }
    
    /**
     * Returns the application package name
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the runnerName
     */
    public String getRunnerName() {
        if (mRunnerName == null) {
            return DEFAULT_RUNNER_NAME;
        }
        return mRunnerName;
    }
    
    /**
     * Returns the complete instrumentation component path 
     */
    private String getRunnerPath() {
        return getPackageName() + RUNNER_SEPARATOR + getRunnerName();
    }
    
    /**
     * Sets to run only tests in this class
     * Must be called before 'run'
     * @param className - fully qualified class name (eg x.y.z)
     */
    public void setClassName(String className) {
        mClassArg = className;
    }

    /**
     * Sets to run only tests in the provided classes
     * Must be called before 'run'
     * If providing more than one class, requires a InstrumentationTestRunner that supports 
     * the multiple class argument syntax 
     * @param classNames - array of fully qualified class name (eg x.y.z)
     */
    public void setClassNames(String[] classNames) {
        StringBuilder classArgBuilder = new StringBuilder();
        
        for (int i=0; i < classNames.length; i++) {
            if (i != 0) {
                classArgBuilder.append(CLASS_SEPARATOR);
            }
            classArgBuilder.append(classNames[i]);
        }
        mClassArg = classArgBuilder.toString();
    }
    
    /**
     * Sets to run only specified test method
     * Must be called before 'run'
     * @param className - fully qualified class name (eg x.y.z)
     * @param testName - method name
     */
    public void setMethodName(String className, String testName) {
        mClassArg = className + METHOD_SEPARATOR + testName;
    }
    
    /**
     * Sets extra arguments to include in instrumentation command.
     * Must be called before 'run'
     * @param instrumentationArgs - must not be null
     */
    public void setExtraArgs(String instrumentationArgs) {
        if (instrumentationArgs == null) {
            throw new IllegalArgumentException("instrumentationArgs cannot be null");
        }
        mExtraArgs = instrumentationArgs;  
    }
    
    /**
     * Returns the extra instrumentation arguments
     */
    public String getExtraArgs() {
        return mExtraArgs;
    }
    
    /**
     * Sets this test run to log only mode - skips test execution
     */
    public void setLogOnly(boolean logOnly) {
        mLogOnlyMode = logOnly;
    }
    
    /**
     * Execute this test run
     * 
     * @param listener - listener to report results to
     */
    public void run(ITestRunListener listener) {
        final String runCaseCommandStr = "am instrument -w -r "
            + getClassCmd() + " " + getLogCmd() + " " + getExtraArgs() + " " + getRunnerPath();
        Log.d(LOG_TAG, runCaseCommandStr);
        mParser = new InstrumentationResultParser(listener);
        
        try {
            mRemoteDevice.executeShellCommand(runCaseCommandStr, mParser);
        } catch (IOException e) {
            Log.e(LOG_TAG, e);
            listener.testRunFailed(e.toString());
        }
    }
    
    /**
     * Requests cancellation of this test run
     */
    public void cancel() {
        if (mParser != null) {
            mParser.cancel();
        }
    }
    
    /**
     * Returns the test class argument
     */
    private String getClassArg() {
        return mClassArg;
    }
    
    /**
     * Returns the full instrumentation command which specifies the test classes to execute. 
     * Returns an empty string if no classes were specified
     */
    private String getClassCmd() {
        String classArg = getClassArg();
        if (classArg != null) {
            return "-e class " + classArg;
        }
        return "";
    }

    /**
     * Returns the full command to enable log only mode - if specified. Otherwise returns an 
     * empty string
     */
    private String getLogCmd() {
        if (mLogOnlyMode) {
            return "-e log true";
        }
        else {
            return "";
        }
    }
}
