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

package com.android.ide.eclipse.platform;

import com.android.ddmuilib.StackTracePanel;
import com.android.ddmuilib.StackTracePanel.ISourceRevealer;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.platform.project.PlatformNature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.core.runtime.Preferences.PropertyChangeEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import java.io.File;

/**
 * The activator class controls the plug-in life cycle
 */
public class AndroidPlatformPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.android.ide.eclipse.apdt"; //$NON-NLS-1$

    public final static String PREFS_DEVICE_DIRECTORY = PLUGIN_ID + ".deviceDir"; //$NON-NLS-1$

    // The shared instance
    private static AndroidPlatformPlugin sPlugin;

    private IPreferenceStore mStore;
    private String mOsDeviceDirectory;

    /**
     * The constructor
     */
    public AndroidPlatformPlugin() {
        sPlugin = this;
    }
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        // get the eclipse store
        mStore = getPreferenceStore();
        
        // set the listener for the preference change
        Preferences prefs = getPluginPreferences();
        prefs.addPropertyChangeListener(new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // get the name of the property that changed.
                String property = event.getProperty();

                // if the SDK changed, we update the cached version
                if (PREFS_DEVICE_DIRECTORY.equals(property)) {
                    // get the new one from the preferences
                    mOsDeviceDirectory = (String)event.getNewValue();

                    // make sure it does not ends with a separator
                    if (mOsDeviceDirectory.endsWith(File.separator)) {
                        mOsDeviceDirectory = mOsDeviceDirectory.substring(0,
                                mOsDeviceDirectory.length() - 1);
                    }

                    // finally restart adb, in case it's a different version
                    String adbLocation = getOsAdbLocation();
                    if (adbLocation != null) {
                        DdmsPlugin.setAdb(adbLocation, true /* startAdb */);
                    }
                }
            }
        });


        mOsDeviceDirectory = mStore.getString(PREFS_DEVICE_DIRECTORY);
        
        if (mOsDeviceDirectory.length() == 0) {
            // get the current Display
            final Display display = sPlugin.getWorkbench().getDisplay();

            // dialog box only run in ui thread..
            display.asyncExec(new Runnable() {
                public void run() {
                    Shell shell = display.getActiveShell();
                    MessageDialog.openError(shell, "Android Preferences",
                            "Location of the device directory is missing.");
                }
            });
        } else {
            // give the location of adb to ddms
            String adbLocation = getOsAdbLocation();
            if (adbLocation != null) {
                DdmsPlugin.setAdb(adbLocation, true);
            }
        }
        
        // and give it the debug launcher for android projects
        DdmsPlugin.setRunningAppDebugLauncher(new DdmsPlugin.IDebugLauncher() {
            public boolean debug(String packageName, int port) {
                return false;
            }
        });
        
        StackTracePanel.setSourceRevealer(new ISourceRevealer() {
            public void reveal(String applicationName, String className, int line) {
                IProject project = getDeviceProject();
                if (project != null) {
                    BaseProjectHelper.revealSource(project, className, line);
                }
            }
        });

    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        sPlugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static AndroidPlatformPlugin getDefault() {
        return sPlugin;
    }
    
    /**
     * Returns the Android project. This is the first project which has the PlatformNature.
     * @return the <code>IProject</code> of the Android project, or <code>null</code> if it was
     * not found.
     */
    public static IProject getDeviceProject() {
        // Get the list of projects for the current workspace.
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        
        for (IProject project : projects) {
            try {
                if (project.hasNature(PlatformNature.ID)) {
                    return project;
                }
            } catch (CoreException e) {
                // Failed to get the nature for this project. Let's just ignore
                // it and move on to the next one.
            }
        }
        
        return null;
    }
    
    /**
     * Returns the OS path of the adb location.
     * @return the location of adb or null if it cannot be computed.
     */
    private String getOsAdbLocation() {
        if (mOsDeviceDirectory == null || mOsDeviceDirectory.length() == 0) {
            return null;
        }

        if (AndroidConstants.CURRENT_PLATFORM == AndroidConstants.PLATFORM_LINUX) {
            return mOsDeviceDirectory + "/out/host/linux-x86/bin/adb"; //$NON-NLS-1$
        } else if (AndroidConstants.CURRENT_PLATFORM == AndroidConstants.PLATFORM_DARWIN) {
            return mOsDeviceDirectory + "/out/host/darwin-x86/bin/adb"; //$NON-NLS-1$
        }
        return null;
    }

}
