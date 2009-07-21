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

package com.android.ide.eclipse.adt.internal.launch;

import com.android.ddmlib.IDevice;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A delayed launch waiting for a device to be present or ready before the
 * application is launched.
 */
public final class DelayedLaunchInfo {

    /**
     * Used to indicate behavior when Android app already exists
     */
    enum InstallRetryMode {
        NEVER, ALWAYS, PROMPT;
    }

    /** The device on which to launch the app */
    private IDevice mDevice = null;

    /** The eclipse project */
    private final IProject mProject;

    /** Package name */
    private final String mPackageName;

    /** Debug package name */
    private final String mDebugPackageName;

    /** IFile to the package (.apk) file */
    private final IFile mPackageFile;

    /** debuggable attribute of the manifest file. */
    private final Boolean mDebuggable;

    /** Required Api level by the app. null means no requirements */
    private final String mRequiredApiVersionNumber;

    private InstallRetryMode mRetryMode = InstallRetryMode.NEVER;

    /** Launch action. */
    private final IAndroidLaunchAction mLaunchAction;

    /** the launch object */
    private final AndroidLaunch mLaunch;

    /** the monitor object */
    private final IProgressMonitor mMonitor;

    /** debug mode flag */
    private boolean mDebugMode;

    /** current number of launch attempts */
    private int mAttemptCount = 0;

    /** cancellation state of launch */
    private boolean mCancelled = false;

    /**
     * Basic constructor with activity and package info.
     *
     * @param project the eclipse project that corresponds to Android app
     * @param packageName package name of Android app
     * @param debugPackageName the package name of the Andriod app to debug
     * @param launchAction action to perform after app install
     * @param pack IFile to the package (.apk) file
     * @param debuggable debuggable attribute of the app's manifest file.
     * @param requiredApiVersionNumber required SDK version by the app. null means no requirements.
     * @param launch the launch object
     * @param monitor progress monitor for launch
     */
    public DelayedLaunchInfo(IProject project, String packageName, String debugPackageName,
            IAndroidLaunchAction launchAction, IFile pack, Boolean debuggable,
            String requiredApiVersionNumber, AndroidLaunch launch, IProgressMonitor monitor) {
        mProject = project;
        mPackageName = packageName;
        mDebugPackageName = debugPackageName;
        mPackageFile = pack;
        mLaunchAction = launchAction;
        mLaunch = launch;
        mMonitor = monitor;
        mDebuggable = debuggable;
        mRequiredApiVersionNumber = requiredApiVersionNumber;
    }

    /**
     * @return the device on which to launch the app
     */
    public IDevice getDevice() {
        return mDevice;
    }

    /**
     * Set the device on which to launch the app
     */
    public void setDevice(IDevice device) {
        mDevice = device;
    }

    /**
     * @return the eclipse project that corresponds to Android app
     */
    public IProject getProject() {
        return mProject;
    }

    /**
     * @return the package name of the Android app
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Returns the Android app process name that the debugger should connect to. Typically this is
     * the same value as {@link #getPackageName()}.
     */
    public String getDebugPackageName() {
        if (mDebugPackageName == null) {
            return getPackageName();
        }
        return mDebugPackageName;
    }

    /**
     * @return the application package file
     */
    public IFile getPackageFile() {
        return mPackageFile;
    }

    /**
     * @return true if Android app is marked as debuggable in its manifest
     */
    public Boolean getDebuggable() {
        return mDebuggable;
    }

    /**
     * @return the required api version number for the Android app.
     */
    public String getRequiredApiVersionNumber() {
        return mRequiredApiVersionNumber;
    }

    /**
     * @param retryMode the install retry mode to set
     */
    public void setRetryMode(InstallRetryMode retryMode) {
        this.mRetryMode = retryMode;
    }

    /**
     * @return the installation retry mode
     */
    public InstallRetryMode getRetryMode() {
        return mRetryMode;
    }

    /**
     * @return the launch action
     */
    public IAndroidLaunchAction getLaunchAction() {
        return mLaunchAction;
    }

    /**
     * @return the launch
     */
    public AndroidLaunch getLaunch() {
        return mLaunch;
    }

    /**
     * @return the launch progress monitor
     */
    public IProgressMonitor getMonitor() {
        return mMonitor;
    }

    /**
     * @param debugMode the debug mode to set
     */
    public void setDebugMode(boolean debugMode) {
        this.mDebugMode = debugMode;
    }

    /**
     * @return true if this is a debug launch
     */
    public boolean isDebugMode() {
        return mDebugMode;
    }

    /**
     * Increases the number of launch attempts
     */
    public void incrementAttemptCount() {
        mAttemptCount++;
    }

    /**
     * @return the number of launch attempts made
     */
    public int getAttemptCount() {
        return mAttemptCount;
    }

    /**
     * Set if launch has been cancelled
     */
    public void setCancelled(boolean cancelled) {
        this.mCancelled = cancelled;
    }

    /**
     * @return true if launch has been cancelled
     */
    public boolean isCancelled() {
        return mCancelled;
    }
}
