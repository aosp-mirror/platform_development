/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.launch.junit.runtime;

import com.android.ddmlib.IDevice;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunch;

/**
 * Contains info about Android JUnit launch
 */
public class AndroidJUnitLaunchInfo {
    private final IProject mProject;
    private final String mAppPackage;
    private final String mRunner;

    private boolean mDebugMode = false;
    private IDevice mDevice = null;
    private String mTestPackage = null;
    private String mTestClass = null;
    private String mTestMethod = null;
    private ILaunch mLaunch = null;

    public AndroidJUnitLaunchInfo(IProject project, String appPackage, String runner) {
        mProject = project;
        mAppPackage = appPackage;
        mRunner = runner;
    }

    public IProject getProject() {
        return mProject;
    }

    public String getAppPackage() {
        return mAppPackage;
    }

    public String getRunner() {
        return mRunner;
    }

    public boolean isDebugMode() {
        return mDebugMode;
    }
    
    public void setDebugMode(boolean debugMode) {
        mDebugMode = debugMode;
    }

    public IDevice getDevice() {
        return mDevice;
    }

    public void setDevice(IDevice device) {
        mDevice = device;
    }

    /**
     * Specify to run all tests within given package.
     *
     * @param testPackage fully qualified java package
     */
    public void setTestPackage(String testPackage) {
        mTestPackage = testPackage;
    }

    /**
     * Return the package of tests to run.
     *
     * @return fully qualified java package. <code>null</code> if not specified.
     */
    public String getTestPackage() {
        return mTestPackage;       
    }

    /**
     * Sets the test class to run.
     * 
     * @param testClass fully qualfied test class to run
     *    Expected format: x.y.x.testclass
     */
    public void setTestClass(String testClass) {
        mTestClass = testClass;
    }

    /** 
     * Returns the test class to run.
     *
     * @return fully qualfied test class to run.
     *   <code>null</code> if not specified.
     */
    public String getTestClass() {
        return mTestClass;
    }
    
    /**
     * Sets the test method to run. testClass must also be set. 
     * 
     * @param testMethod test method to run
     */
    public void setTestMethod(String testMethod) {
        mTestMethod = testMethod;
    }

    /** 
     * Returns the test method to run.
     *
     * @return test method to run. <code>null</code> if not specified.
     */
    public String getTestMethod() {
        return mTestMethod;
    }

    public ILaunch getLaunch() {
        return mLaunch;
    }

    public void setLaunch(ILaunch launch) {
        mLaunch = launch;
    }
}
