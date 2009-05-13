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
import com.android.ide.eclipse.adt.internal.launch.DelayedLaunchInfo;

/**
 * An action to perform after performing a launch of an Android application
 */
public interface IAndroidLaunchAction {

    /** 
     * Do the launch
     * 
     * @param info the {@link DelayedLaunchInfo} that contains launch details
     * @param device the Android device to perform action on
     * @returns true if launch was successfully, and controller should wait for debugger to attach
     *     (if applicable)
     */
    boolean doLaunchAction(DelayedLaunchInfo info, IDevice device);
    
    /**
     * Return a description of launch, to be used for logging and error messages
     */
    String getLaunchDescription();
    
}
