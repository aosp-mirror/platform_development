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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Launch configuration data. This stores the result of querying the
 * {@link ILaunchConfiguration} so that it's only done once. 
 */
public class AndroidLaunchConfiguration {
    
    /**
     * Launch action. See {@link LaunchConfigDelegate#ACTION_DEFAULT},
     * {@link LaunchConfigDelegate#ACTION_ACTIVITY},
     * {@link LaunchConfigDelegate#ACTION_DO_NOTHING}
     */
    public int mLaunchAction = LaunchConfigDelegate.DEFAULT_LAUNCH_ACTION;
    
    /**
     * Target selection mode for the configuration: either {@link #AUTO} or {@link #MANUAL}.
     */
    public enum TargetMode {
        /** Automatic target selection mode. */
        AUTO(true),
        /** Manual target selection mode. */
        MANUAL(false);
        
        private boolean mValue;

        TargetMode(boolean value) {
            mValue = value;
        }
        
        public boolean getValue() {
            return mValue;
        }
        
        public static TargetMode getMode(boolean value) {
            for (TargetMode mode : values()) {
                if (mode.mValue == value) {
                    return mode;
                }
            }
            
            return null;
        }
    }
    
    /**
     * Target selection mode.
     * @see TargetMode
     */
    public TargetMode mTargetMode = LaunchConfigDelegate.DEFAULT_TARGET_MODE;

    /**
     * Indicates whether the emulator should be called with -wipe-data
     */
    public boolean mWipeData = LaunchConfigDelegate.DEFAULT_WIPE_DATA;

    /**
     * Indicates whether the emulator should be called with -no-boot-anim
     */
    public boolean mNoBootAnim = LaunchConfigDelegate.DEFAULT_NO_BOOT_ANIM;
    
    /**
     * AVD Name.
     */
    public String mAvdName = null;
    
    public String mNetworkSpeed = EmulatorConfigTab.getSpeed(
            LaunchConfigDelegate.DEFAULT_SPEED);
    public String mNetworkDelay = EmulatorConfigTab.getDelay(
            LaunchConfigDelegate.DEFAULT_DELAY);

    /**
     * Optional custom command line parameter to launch the emulator
     */
    public String mEmulatorCommandLine;

    /**
     * Initialized the structure from an ILaunchConfiguration object.
     * @param config
     */
    public void set(ILaunchConfiguration config) {
        try {
            mLaunchAction = config.getAttribute(LaunchConfigDelegate.ATTR_LAUNCH_ACTION,
                    mLaunchAction);
        } catch (CoreException e1) {
            // nothing to be done here, we'll use the default value
        }

        try {
            boolean value = config.getAttribute(LaunchConfigDelegate.ATTR_TARGET_MODE,
                    mTargetMode.getValue());
            mTargetMode = TargetMode.getMode(value);
        } catch (CoreException e) {
            // nothing to be done here, we'll use the default value
        }

        try {
            mAvdName = config.getAttribute(LaunchConfigDelegate.ATTR_AVD_NAME, mAvdName);
        } catch (CoreException e) {
            // ignore
        }

        int index = LaunchConfigDelegate.DEFAULT_SPEED;
        try {
            index = config.getAttribute(LaunchConfigDelegate.ATTR_SPEED, index);
        } catch (CoreException e) {
            // nothing to be done here, we'll use the default value
        }
        mNetworkSpeed = EmulatorConfigTab.getSpeed(index);

        index = LaunchConfigDelegate.DEFAULT_DELAY;
        try {
            index = config.getAttribute(LaunchConfigDelegate.ATTR_DELAY, index);
        } catch (CoreException e) {
            // nothing to be done here, we'll use the default value
        }
        mNetworkDelay = EmulatorConfigTab.getDelay(index);

        try {
            mEmulatorCommandLine = config.getAttribute(
                    LaunchConfigDelegate.ATTR_COMMANDLINE, ""); //$NON-NLS-1$
        } catch (CoreException e) {
            // lets not do anything here, we'll use the default value
        }

        try {
            mWipeData = config.getAttribute(LaunchConfigDelegate.ATTR_WIPE_DATA, mWipeData);
        } catch (CoreException e) {
            // nothing to be done here, we'll use the default value
        }

        try {
            mNoBootAnim = config.getAttribute(LaunchConfigDelegate.ATTR_NO_BOOT_ANIM,
                                              mNoBootAnim);
        } catch (CoreException e) {
            // nothing to be done here, we'll use the default value
        }
    }
}

