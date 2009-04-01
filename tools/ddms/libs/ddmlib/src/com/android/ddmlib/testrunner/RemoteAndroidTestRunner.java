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
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Runs a Android test command remotely and reports results.
 */
public class RemoteAndroidTestRunner  {

    private final String mPackageName;
    private final  String mRunnerName;
    private IDevice mRemoteDevice;
    /** map of name-value instrumentation argument pairs */
    private Map<String, String> mArgMap;
    private InstrumentationResultParser mParser;

    private static final String LOG_TAG = "RemoteAndroidTest";
    private static final String DEFAULT_RUNNER_NAME = "android.test.InstrumentationTestRunner";

    private static final char CLASS_SEPARATOR = ',';
    private static final char METHOD_SEPARATOR = '#';
    private static final char RUNNER_SEPARATOR = '/';

    // defined instrumentation argument names  
    private static final String CLASS_ARG_NAME = "class";
    private static final String LOG_ARG_NAME = "log";
    private static final String DEBUG_ARG_NAME = "debug";
    private static final String COVERAGE_ARG_NAME = "coverage";
    private static final String PACKAGE_ARG_NAME = "package";
 
    /**
     * Creates a remote Android test runner.
     * 
     * @param packageName the Android application package that contains the tests to run 
     * @param runnerName the instrumentation test runner to execute. If null, will use default
     *   runner 
     * @param remoteDevice the Android device to execute tests on
     */
    public RemoteAndroidTestRunner(String packageName, 
                                   String runnerName,
                                   IDevice remoteDevice) {
        
        mPackageName = packageName;
        mRunnerName = runnerName;
        mRemoteDevice = remoteDevice;
        mArgMap = new Hashtable<String, String>();
    }

    /**
     * Alternate constructor. Uses default instrumentation runner.
     * 
     * @param packageName the Android application package that contains the tests to run 
     * @param remoteDevice the Android device to execute tests on
     */
    public RemoteAndroidTestRunner(String packageName, 
                                   IDevice remoteDevice) {
        this(packageName, null, remoteDevice);
    }

    /**
     * Returns the application package name.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the runnerName.
     */
    public String getRunnerName() {
        if (mRunnerName == null) {
            return DEFAULT_RUNNER_NAME;
        }
        return mRunnerName;
    }

    /**
     * Returns the complete instrumentation component path.
     */
    private String getRunnerPath() {
        return getPackageName() + RUNNER_SEPARATOR + getRunnerName();
    }

    /**
     * Sets to run only tests in this class
     * Must be called before 'run'.
     * 
     * @param className fully qualified class name (eg x.y.z)
     */
    public void setClassName(String className) {
        addInstrumentationArg(CLASS_ARG_NAME, className);
    }

    /**
     * Sets to run only tests in the provided classes
     * Must be called before 'run'.
     * <p>
     * If providing more than one class, requires a InstrumentationTestRunner that supports 
     * the multiple class argument syntax.
     * 
     * @param classNames array of fully qualified class names (eg x.y.z)
     */
    public void setClassNames(String[] classNames) {
        StringBuilder classArgBuilder = new StringBuilder();
        
        for (int i = 0; i < classNames.length; i++) {
            if (i != 0) {
                classArgBuilder.append(CLASS_SEPARATOR);
            }
            classArgBuilder.append(classNames[i]);
        }
        setClassName(classArgBuilder.toString());
    }

    /**
     * Sets to run only specified test method
     * Must be called before 'run'.
     * 
     * @param className fully qualified class name (eg x.y.z)
     * @param testName method name
     */
    public void setMethodName(String className, String testName) {
        setClassName(className + METHOD_SEPARATOR + testName);
    }

    /**
     * Sets to run all tests in specified package
     * Must be called before 'run'.
     * 
     * @param packageName fully qualified package name (eg x.y.z)
     */
    public void setTestPackageName(String packageName) {
        addInstrumentationArg(PACKAGE_ARG_NAME, packageName);
    }

    /**
     * Adds a argument to include in instrumentation command.
     * <p/>
     * Must be called before 'run'. If an argument with given name has already been provided, it's
     * value will be overridden. 
     * 
     * @param name the name of the instrumentation bundle argument
     * @param value the value of the argument
     */
    public void addInstrumentationArg(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("name or value arguments cannot be null");
        }
        mArgMap.put(name, value);  
    }

    /**
     * Adds a boolean argument to include in instrumentation command.
     * <p/>
     * @see RemoteAndroidTestRunner#addInstrumentationArg
     * 
     * @param name the name of the instrumentation bundle argument
     * @param value the value of the argument
     */
    public void addBooleanArg(String name, boolean value) {
        addInstrumentationArg(name, Boolean.toString(value));
    }
  
    /**
     * Sets this test run to log only mode - skips test execution.
     */
    public void setLogOnly(boolean logOnly) {
        addBooleanArg(LOG_ARG_NAME, logOnly);
    }

    /**
     * Sets this debug mode of this test run. If true, the Android test runner will wait for a 
     * debugger to attach before proceeding with test execution.
     */
    public void setDebug(boolean debug) {
        addBooleanArg(DEBUG_ARG_NAME, debug);
    }

    /**
     * Sets this code coverage mode of this test run. 
     */
    public void setCoverage(boolean coverage) {
        addBooleanArg(COVERAGE_ARG_NAME, coverage);
    }

    /**
     * Execute this test run.
     * 
     * @param listener listens for test results
     */
    public void run(ITestRunListener listener) {
        final String runCaseCommandStr = String.format("am instrument -w -r %s %s",
            getArgsCommand(), getRunnerPath());
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
     * Requests cancellation of this test run.
     */
    public void cancel() {
        if (mParser != null) {
            mParser.cancel();
        }
    }

    /**
     * Returns the full instrumentation command line syntax for the provided instrumentation 
     * arguments.  
     * Returns an empty string if no arguments were specified.
     */
    private String getArgsCommand() {
        StringBuilder commandBuilder = new StringBuilder();
        for (Entry<String, String> argPair : mArgMap.entrySet()) {
            final String argCmd = String.format(" -e %s %s", argPair.getKey(),
                    argPair.getValue());
            commandBuilder.append(argCmd);
        }
        return commandBuilder.toString();
    }
}
