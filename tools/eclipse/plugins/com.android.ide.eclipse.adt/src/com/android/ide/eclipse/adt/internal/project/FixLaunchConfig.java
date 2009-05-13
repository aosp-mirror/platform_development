/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.project;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.launch.LaunchConfigDelegate;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

import java.util.ArrayList;

/**
 * Class to fix the launch configuration of a project if the java package
 * defined in the manifest has been changed.<br>
 * This fix can be done synchronously, or asynchronously.<br>
 * <code>start()</code> will start a thread that will do the fix.<br>
 * <code>run()</code> will do the fix in the current thread.<br><br>
 * By default, the fix first display a dialog to the user asking if he/she wants to
 * do the fix. This can be overriden by calling <code>setDisplayPrompt(false)</code>.
 *
 */
public class FixLaunchConfig extends Thread {

    private IProject mProject;
    private String mOldPackage;
    private String mNewPackage;

    private boolean mDisplayPrompt = true;

    public FixLaunchConfig(IProject project, String oldPackage, String newPackage) {
        super();

        mProject = project;
        mOldPackage = oldPackage;
        mNewPackage = newPackage;
    }

    /**
     * Set the display prompt. If true run()/start() first ask the user if he/she wants
     * to fix the Launch Config
     * @param displayPrompt
     */
    public void setDisplayPrompt(boolean displayPrompt) {
        mDisplayPrompt = displayPrompt;
    }

    /**
     * Fix the Launch configurations.
     */
    @Override
    public void run() {

        if (mDisplayPrompt) {
            // ask the user if he really wants to fix the launch config
            boolean res = AdtPlugin.displayPrompt(
                    "Launch Configuration Update",
                    "The package definition in the manifest changed.\nDo you want to update your Launch Configuration(s)?");

            if (res == false) {
                return;
            }
        }

        // get the list of config for the project
        String projectName = mProject.getName();
        ILaunchConfiguration[] configs = findConfigs(mProject.getName());

        // loop through all the config and update the package
        for (ILaunchConfiguration config : configs) {
            try {
                // get the working copy so that we can make changes.
                ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();

                // get the attributes for the activity
                String activity = config.getAttribute(LaunchConfigDelegate.ATTR_ACTIVITY,
                        ""); //$NON-NLS-1$

                // manifests can define activities that are not in the defined package,
                // so we need to make sure the activity is inside the old package.
                if (activity.startsWith(mOldPackage)) {
                    // create the new activity
                    activity = mNewPackage + activity.substring(mOldPackage.length());

                    // put it in the copy
                    copy.setAttribute(LaunchConfigDelegate.ATTR_ACTIVITY, activity);

                    // save the config
                    copy.doSave();
                }
            } catch (CoreException e) {
                // couldn't get the working copy. we output the error in the console
                String msg = String.format("Failed to modify %1$s: %2$s", projectName,
                        e.getMessage());
                AdtPlugin.printErrorToConsole(mProject, msg);
            }
        }

    }

    /**
     * Looks for and returns all existing Launch Configuration object for a
     * specified project.
     * @param projectName The name of the project
     * @return all the ILaunchConfiguration object. If none are present, an empty array is
     * returned.
     */
    private static ILaunchConfiguration[] findConfigs(String projectName) {
        // get the launch manager
        ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();

        // now get the config type for our particular android type.
        ILaunchConfigurationType configType = manager.
                getLaunchConfigurationType(LaunchConfigDelegate.ANDROID_LAUNCH_TYPE_ID);

        // create a temp list to hold all the valid configs
        ArrayList<ILaunchConfiguration> list = new ArrayList<ILaunchConfiguration>();

        try {
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(configType);

            for (ILaunchConfiguration config : configs) {
                if (config.getAttribute(
                        IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME,
                        "").equals(projectName)) {  //$NON-NLS-1$
                    list.add(config);
                }
            }
        } catch (CoreException e) {
        }

        return list.toArray(new ILaunchConfiguration[list.size()]);

    }

}
