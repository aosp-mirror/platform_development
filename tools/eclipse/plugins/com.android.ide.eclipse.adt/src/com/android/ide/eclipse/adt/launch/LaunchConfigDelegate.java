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

package com.android.ide.eclipse.adt.launch;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.launch.AndroidLaunchConfiguration.TargetMode;
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.common.project.AndroidManifestParser.Activity;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Implementation of an eclipse LauncConfigurationDelegate to launch android
 * application in debug.
 */
public class LaunchConfigDelegate extends LaunchConfigurationDelegate {
    final static int INVALID_DEBUG_PORT = -1;
    
    public final static String ANDROID_LAUNCH_TYPE_ID =
        "com.android.ide.eclipse.adt.debug.LaunchConfigType"; //$NON-NLS-1$

    /** Target mode parameters: true is automatic, false is manual */
    public static final String ATTR_TARGET_MODE = AdtPlugin.PLUGIN_ID + ".target"; //$NON-NLS-1$
    public static final TargetMode DEFAULT_TARGET_MODE = TargetMode.AUTO;

    /**
     * Launch action:
     * <ul>
     * <li>0: launch default activity</li>
     * <li>1: launch specified activity. See {@link #ATTR_ACTIVITY}</li>
     * <li>2: Do Nothing</li>
     * </ul>
     */
    public final static String ATTR_LAUNCH_ACTION = AdtPlugin.PLUGIN_ID + ".action"; //$NON-NLS-1$
    
    /** Default launch action. This launches the activity that is setup to be found in the HOME
     * screen.
     */
    public final static int ACTION_DEFAULT = 0;
    /** Launch action starting a specific activity. */
    public final static int ACTION_ACTIVITY = 1;
    /** Launch action that does nothing. */
    public final static int ACTION_DO_NOTHING = 2;
    /** Default launch action value. */
    public final static int DEFAULT_LAUNCH_ACTION = ACTION_DEFAULT;

    /**
     * Activity to be launched if {@link #ATTR_LAUNCH_ACTION} is 1
     */
    public static final String ATTR_ACTIVITY = AdtPlugin.PLUGIN_ID + ".activity"; //$NON-NLS-1$

    public static final String ATTR_AVD_NAME = AdtPlugin.PLUGIN_ID + ".avd"; //$NON-NLS-1$
    
    public static final String ATTR_SPEED = AdtPlugin.PLUGIN_ID + ".speed"; //$NON-NLS-1$

    /**
     * Index of the default network speed setting for the emulator.<br>
     * Get the emulator option with <code>EmulatorConfigTab.getSpeed(index)</code>
     */
    public static final int DEFAULT_SPEED = 0;

    public static final String ATTR_DELAY = AdtPlugin.PLUGIN_ID + ".delay"; //$NON-NLS-1$

    /**
     * Index of the default network latency setting for the emulator.<br>
     * Get the emulator option with <code>EmulatorConfigTab.getDelay(index)</code>
     */
    public static final int DEFAULT_DELAY = 0;

    public static final String ATTR_COMMANDLINE = AdtPlugin.PLUGIN_ID + ".commandline"; //$NON-NLS-1$

    public static final String ATTR_WIPE_DATA = AdtPlugin.PLUGIN_ID + ".wipedata"; //$NON-NLS-1$
    public static final boolean DEFAULT_WIPE_DATA = false;

    public static final String ATTR_NO_BOOT_ANIM = AdtPlugin.PLUGIN_ID + ".nobootanim"; //$NON-NLS-1$
    public static final boolean DEFAULT_NO_BOOT_ANIM = false;

    public static final String ATTR_DEBUG_PORT = 
        AdtPlugin.PLUGIN_ID + ".debugPort"; //$NON-NLS-1$

    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {
        // We need to check if it's a standard launch or if it's a launch
        // to debug an application already running.
        int debugPort = AndroidLaunchController.getPortForConfig(configuration);

        // get the project
        IProject project = getProject(configuration);

        // first we make sure the launch is of the proper type
        AndroidLaunch androidLaunch = null;
        if (launch instanceof AndroidLaunch) {
            androidLaunch = (AndroidLaunch)launch;
        } else {
            // wrong type, not sure how we got there, but we don't do
            // anything else
            AdtPlugin.printErrorToConsole(project, "Wrong Launch Type!");
            return;
        }

        // if we have a valid debug port, this means we're debugging an app
        // that's already launched.
        if (debugPort != INVALID_DEBUG_PORT) {
            AndroidLaunchController.launchRemoteDebugger(debugPort, androidLaunch, monitor);
            return;
        }

        if (project == null) {
            AdtPlugin.printErrorToConsole("Couldn't get project object!");
            androidLaunch.stopLaunch();
            return;
        }

        // check if the project has errors, and abort in this case.
        if (ProjectHelper.hasError(project, true)) {
            AdtPlugin.displayError("Android Launch",
                    "Your project contains error(s), please fix them before running your application.");
            return;
        }

        AdtPlugin.printToConsole(project, "------------------------------"); //$NON-NLS-1$
        AdtPlugin.printToConsole(project, "Android Launch!");

        // check if the project is using the proper sdk.
        // if that throws an exception, we simply let it propagate to the caller.
        if (checkAndroidProject(project) == false) {
            AdtPlugin.printErrorToConsole(project, "Project is not an Android Project. Aborting!");
            androidLaunch.stopLaunch();
            return;
        }

        // Check adb status and abort if needed.
        AndroidDebugBridge bridge = AndroidDebugBridge.getBridge();
        if (bridge == null || bridge.isConnected() == false) {
            try {
                int connections = -1;
                int restarts = -1;
                if (bridge != null) {
                    connections = bridge.getConnectionAttemptCount();
                    restarts = bridge.getRestartAttemptCount();
                }

                // if we get -1, the device monitor is not even setup (anymore?).
                // We need to ask the user to restart eclipse.
                // This shouldn't happen, but it's better to let the user know in case it does.
                if (connections == -1 || restarts == -1) {
                    AdtPlugin.printErrorToConsole(project, 
                            "The connection to adb is down, and a severe error has occured.",
                            "You must restart adb and Eclipse.",
                            String.format(
                                    "Please ensure that adb is correctly located at '%1$s' and can be executed.",
                                    AdtPlugin.getOsAbsoluteAdb()));
                    return;
                }
                
                if (restarts == 0) {
                    AdtPlugin.printErrorToConsole(project,
                            "Connection with adb was interrupted.",
                            String.format("%1$s attempts have been made to reconnect.", connections),
                            "You may want to manually restart adb from the Devices view.");
                } else {
                    AdtPlugin.printErrorToConsole(project,
                            "Connection with adb was interrupted, and attempts to reconnect have failed.",
                            String.format("%1$s attempts have been made to restart adb.", restarts),
                            "You may want to manually restart adb from the Devices view.");
                    
                }
                return;
            } finally {
                androidLaunch.stopLaunch();
            }
        }
        
        // since adb is working, we let the user know
        // TODO have a verbose mode for launch with more info (or some of the less useful info we now have).
        AdtPlugin.printToConsole(project, "adb is running normally.");

        // make a config class
        AndroidLaunchConfiguration config = new AndroidLaunchConfiguration();

        // fill it with the config coming from the ILaunchConfiguration object
        config.set(configuration);

        // get the launch controller singleton
        AndroidLaunchController controller = AndroidLaunchController.getInstance();

        // get the application package
        IFile applicationPackage = ProjectHelper.getApplicationPackage(project);
        if (applicationPackage == null) {
            androidLaunch.stopLaunch();
            return;
        }

        // we need some information from the manifest
        AndroidManifestParser manifestParser = AndroidManifestParser.parse(
                BaseProjectHelper.getJavaProject(project), null /* errorListener */,
                true /* gatherData */, false /* markErrors */);
        
        if (manifestParser == null) {
            AdtPlugin.printErrorToConsole(project, "Failed to parse AndroidManifest: aborting!");
            androidLaunch.stopLaunch();
            return;
        }

        doLaunch(configuration, mode, monitor, project, androidLaunch, config, controller,
                applicationPackage, manifestParser);
    }

    protected void doLaunch(ILaunchConfiguration configuration, String mode,
            IProgressMonitor monitor, IProject project, AndroidLaunch androidLaunch,
            AndroidLaunchConfiguration config, AndroidLaunchController controller,
            IFile applicationPackage, AndroidManifestParser manifestParser) {
        
       String activityName = null;
        
        if (config.mLaunchAction == ACTION_ACTIVITY) { 
            // Get the activity name defined in the config
            activityName = getActivityName(configuration);
    
            // Get the full activity list and make sure the one we got matches.
            Activity[] activities = manifestParser.getActivities();
    
            // first we check that there are, in fact, activities.
            if (activities.length == 0) {
                // if the activities list is null, then the manifest is empty
                // and we can't launch the app. We'll revert to a sync-only launch
                AdtPlugin.printErrorToConsole(project,
                        "The Manifest defines no activity!",
                        "The launch will only sync the application package on the device!");
                config.mLaunchAction = ACTION_DO_NOTHING;
            } else if (activityName == null) {
                // if the activity we got is null, we look for the default one.
                AdtPlugin.printErrorToConsole(project,
                        "No activity specified! Getting the launcher activity.");
                Activity launcherActivity = manifestParser.getLauncherActivity();
                if (launcherActivity != null) {
                    activityName = launcherActivity.getName();
                }

                // if there's no default activity. We revert to a sync-only launch.
                if (activityName == null) {
                    revertToNoActionLaunch(project, config);
                }
            } else {
    
                // check the one we got from the config matches any from the list
                boolean match = false;
                for (Activity a : activities) {
                    if (a != null && a.getName().equals(activityName)) {
                        match = true;
                        break;
                    }
                }
    
                // if we didn't find a match, we revert to the default activity if any.
                if (match == false) {
                    AdtPlugin.printErrorToConsole(project,
                            "The specified activity does not exist! Getting the launcher activity.");
                    Activity launcherActivity = manifestParser.getLauncherActivity();
                    if (launcherActivity != null) {
                        activityName = launcherActivity.getName();
                    }
            
                    // if there's no default activity. We revert to a sync-only launch.
                    if (activityName == null) {
                        revertToNoActionLaunch(project, config);
                    }
                }
            }
        } else if (config.mLaunchAction == ACTION_DEFAULT) {
            Activity launcherActivity = manifestParser.getLauncherActivity();
            if (launcherActivity != null) {
                activityName = launcherActivity.getName();
            }
            
            // if there's no default activity. We revert to a sync-only launch.
            if (activityName == null) {
                revertToNoActionLaunch(project, config);
            }
        }

        IAndroidLaunchAction launchAction = new EmptyLaunchAction();
        if (activityName != null) {
            launchAction = new ActivityLaunchAction(activityName, controller);
        }

        // everything seems fine, we ask the launch controller to handle
        // the rest
        controller.launch(project, mode, applicationPackage,manifestParser.getPackage(),
                manifestParser.getPackage(), manifestParser.getDebuggable(),
                manifestParser.getApiLevelRequirement(), launchAction, config, androidLaunch,
                monitor);
    }
    
    @Override
    public boolean buildForLaunch(ILaunchConfiguration configuration,
            String mode, IProgressMonitor monitor) throws CoreException {

        // need to check we have everything
        IProject project = getProject(configuration);

        if (project != null) {
            // force an incremental build to be sure the resources will
            // be updated if they were not saved before the launch was launched.
            return true;
        }

        throw new CoreException(new Status(IStatus.ERROR, AdtPlugin.PLUGIN_ID,
                        1 /* code, unused */, "Can't find the project!", null /* exception */));
    }

    /**
     * {@inheritDoc}
     * @throws CoreException
     */
    @Override
    public ILaunch getLaunch(ILaunchConfiguration configuration, String mode)
            throws CoreException {
        return new AndroidLaunch(configuration, mode, null);
    }

    /**
     * Returns the IProject object matching the name found in the configuration
     * object under the name
     * <code>IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME</code>
     * @param configuration
     * @return The IProject object or null
     */
    private IProject getProject(ILaunchConfiguration configuration){
        // get the project name from the config
        String projectName;
        try {
            projectName = configuration.getAttribute(
                    IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        } catch (CoreException e) {
            return null;
        }

        // get the current workspace
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        // and return the project with the name from the config
        return workspace.getRoot().getProject(projectName);
    }

    /**
     * Checks the project is an android project.
     * @param project The project to check
     * @return true if the project is an android SDK.
     * @throws CoreException
     */
    private boolean checkAndroidProject(IProject project) throws CoreException {
        // check if the project is a java and an android project.
        if (project.hasNature(JavaCore.NATURE_ID) == false) {
            String msg = String.format("%1$s is not a Java project!", project.getName());
            AdtPlugin.displayError("Android Launch", msg);
            return false;
        }

        if (project.hasNature(AndroidConstants.NATURE) == false) {
            String msg = String.format("%1$s is not an Android project!", project.getName());
            AdtPlugin.displayError("Android Launch", msg);
            return false;
        }

        return true;
    }


    /**
     * Returns the name of the activity.
     */
    private String getActivityName(ILaunchConfiguration configuration) {
        String empty = "";
        String activityName;
        try {
            activityName = configuration.getAttribute(ATTR_ACTIVITY, empty);
        } catch (CoreException e) {
            return null;
        }

        return (activityName != empty) ? activityName : null;
    }
    
    private final void revertToNoActionLaunch(IProject project, AndroidLaunchConfiguration config) {
        AdtPlugin.printErrorToConsole(project,
                "No Launcher activity found!",
                "The launch will only sync the application package on the device!");
        config.mLaunchAction = ACTION_DO_NOTHING;
    }
}
