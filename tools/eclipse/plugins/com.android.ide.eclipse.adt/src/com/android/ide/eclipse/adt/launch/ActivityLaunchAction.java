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

package com.android.ide.eclipse.adt.launch;

import com.android.ddmlib.IDevice;
import com.android.ide.eclipse.adt.AdtPlugin;

import java.io.IOException;

/**
 * Launches the given activity
 */
public class ActivityLaunchAction implements IAndroidLaunchAction {

    private final String mActivity;
    private final ILaunchController mLaunchController;
    
    /**
     * Creates a ActivityLaunchAction
     * 
     * @param activity fully qualified activity name to launch
     * @param controller the {@link ILaunchController} that performs launch
     */
    public ActivityLaunchAction(String activity, ILaunchController controller) {
        mActivity = activity;
        mLaunchController = controller;
    }
    
    /**
     * Launches the activity on targeted device
     * 
     * @param info the {@link DelayedLaunchInfo} that contains launch details
     * @param device the Android device to perform action on
     * 
     * @see IAndroidLaunchAction#doLaunchAction(DelayedLaunchInfo, IDevice)
     */
    public boolean doLaunchAction(DelayedLaunchInfo info, IDevice device) {
        try {
            String msg = String.format("Starting activity %1$s on device ", mActivity,
                    device);
            AdtPlugin.printToConsole(info.getProject(), msg);

            // In debug mode, we need to add the info to the list of application monitoring
            // client changes.
            // increment launch attempt count, to handle retries and timeouts
            info.incrementAttemptCount();

            // now we actually launch the app.
            device.executeShellCommand("am start" //$NON-NLS-1$
                    + (info.isDebugMode() ? " -D" //$NON-NLS-1$
                            : "") //$NON-NLS-1$
                    + " -n " //$NON-NLS-1$
                    + info.getPackageName() + "/" //$NON-NLS-1$
                    + mActivity.replaceAll("\\$", "\\\\\\$"), //$NON-NLS-1$ //$NON-NLS-2$
                    new AMReceiver(info, device, mLaunchController));

            // if the app is not a debug app, we need to do some clean up, as
            // the process is done!
            if (info.isDebugMode() == false) {
                // stop the launch object, since there's no debug, and it can't
                // provide any control over the app
                return false;
            }
        } catch (IOException e) {
            // something went wrong trying to launch the app.
            // lets stop the Launch
            AdtPlugin.printErrorToConsole(info.getProject(),
                    String.format("Launch error: %s", e.getMessage()));
            return false;
        }
        return true;
    }
    
    /**
     * Returns a description of the activity being launched
     * 
     * @see IAndroidLaunchAction#getLaunchDescription()
     */
    public String getLaunchDescription() {
       return String.format("%1$s activity launch", mActivity);
    }
    
}
