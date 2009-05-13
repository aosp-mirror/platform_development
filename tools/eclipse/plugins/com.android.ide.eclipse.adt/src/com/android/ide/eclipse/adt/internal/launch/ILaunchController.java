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

/**
 * Interface for managing Android launches
 */
public interface ILaunchController {

   /**
    * Launches an application on a device or emulator
    *
    * @param launchInfo the {@link DelayedLaunchInfo} that indicates the launch action
    * @param device the device or emulator to launch the application on
    */
    public void launchApp(DelayedLaunchInfo launchInfo, IDevice device);
    
    /**
     * Cancels a launch
     * 
     * @param launchInfo the {@link DelayedLaunchInfo} to cancel
     */
    void stopLaunch(DelayedLaunchInfo launchInfo);
}
