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
package com.android.ide.eclipse.adt.launch.junit.runtime;

import org.eclipse.core.resources.IProject;

import com.android.ddmlib.IDevice;

/**
 * Contains info about Android JUnit launch
 */
public class AndroidJUnitLaunchInfo {
    private final IProject mProject;
    private final String mTestPackage;
    private final String mRunner;
    private final boolean mDebugMode;
    private final IDevice mDevice;
    
    public AndroidJUnitLaunchInfo(IProject project, String testPackage, String runner,
            boolean debugMode, IDevice device) {
        mProject = project;
        mTestPackage = testPackage;
        mRunner = runner;
        mDebugMode = debugMode;
        mDevice = device;
    }
    
    public IProject getProject() {
        return mProject;
    }

    public String getTestPackage() {
        return mTestPackage;
    }

    public String getRunner() {
        return mRunner;
    }

    public boolean isDebugMode() {
        return mDebugMode;
    }

    public IDevice getDevice() {
        return mDevice;
    }
}
