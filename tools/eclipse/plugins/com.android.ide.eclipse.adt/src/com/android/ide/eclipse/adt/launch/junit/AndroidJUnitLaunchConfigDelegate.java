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

package com.android.ide.eclipse.adt.launch.junit;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.launch.AndroidLaunch;
import com.android.ide.eclipse.adt.launch.AndroidLaunchConfiguration;
import com.android.ide.eclipse.adt.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.launch.IAndroidLaunchAction;
import com.android.ide.eclipse.adt.launch.LaunchConfigDelegate;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.BaseProjectHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;

/**
 * Run configuration that can execute JUnit tests on an Android platform
 * <p/>
 * Will deploy apps on target Android platform by reusing functionality from ADT 
 * LaunchConfigDelegate, and then run JUnits tests by reusing functionality from JDT 
 * JUnitLaunchConfigDelegate.
 */
@SuppressWarnings("restriction") //$NON-NLS-1$
public class AndroidJUnitLaunchConfigDelegate extends LaunchConfigDelegate {

    /** Launch config attribute that stores instrumentation runner */
    static final String ATTR_INSTR_NAME = AdtPlugin.PLUGIN_ID + ".instrumentation"; //$NON-NLS-1$
    static final String INSTRUMENTATION_OK = null;
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    @Override
    protected void doLaunch(final ILaunchConfiguration configuration, final String mode,
            IProgressMonitor monitor, IProject project, final AndroidLaunch androidLaunch,
            AndroidLaunchConfiguration config, AndroidLaunchController controller,
            IFile applicationPackage, AndroidManifestParser manifestParser) {

        String testPackage = manifestParser.getPackage();        
        String runner = getRunnerFromConfig(configuration);
        if (runner == null) {
            AdtPlugin.displayError("Android Launch",
                    "An instrumention test runner is not specified!");
            androidLaunch.stopLaunch();
            return;
        }

        IAndroidLaunchAction junitLaunch = new AndroidJUnitLaunchAction(testPackage, runner);

        controller.launch(project, mode, applicationPackage, manifestParser.getPackage(),
                manifestParser.getDebuggable(), manifestParser.getApiLevelRequirement(),
                junitLaunch, config, androidLaunch, monitor);
    }

    private String getRunnerFromConfig(ILaunchConfiguration configuration) {
        String runner = EMPTY_STRING;
        try {
            runner = configuration.getAttribute(ATTR_INSTR_NAME, EMPTY_STRING);
        } catch (CoreException e) {
            AdtPlugin.log(e, "Error when retrieving instrumentation info from launch config"); //$NON-NLS-1$           
        }
        if (runner.length() < 1) {
            return null;
        }
        return runner;
    }

    /**
     * Helper method to return the set of instrumentations for the Android project
     * 
     * @param project the {@link IProject} to get instrumentations for
     * @return null if error occurred parsing instrumentations, otherwise returns array of
     * instrumentation class names
     */
    static String[] getInstrumentationsForProject(IProject project) {
        if (project != null) {
            try {
                // parse the manifest for the list of instrumentations
                AndroidManifestParser manifestParser = AndroidManifestParser.parse(
                        BaseProjectHelper.getJavaProject(project), null /* errorListener */,
                        true /* gatherData */, false /* markErrors */);
                if (manifestParser != null) {
                    return manifestParser.getInstrumentations(); 
                }
            } catch (CoreException e) {
                AdtPlugin.log(e, "%s: Error parsing AndroidManifest.xml",  //$NON-NLS-1$ 
                        project.getName());
            }
        }
        return null;
    }

    /**
     * Helper method to set JUnit-related attributes expected by JDT JUnit runner
     * 
     * @param config the launch configuration to modify
     */
    static void setJUnitDefaults(ILaunchConfigurationWorkingCopy config) {
        // set the test runner to JUnit3 to placate JDT JUnit runner logic
        config.setAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_RUNNER_KIND, 
                TestKindRegistry.JUNIT3_TEST_KIND_ID);
    }
    
    /**
     * Helper method to determine if specified instrumentation can be used as a test runner
     * 
     * @param project the {@link IJavaProject} to validate
     * @param instrumentation the instrumentation class name to validate
     * @return <code>INSTRUMENTATION_OK</code> if valid, otherwise returns error message
     */
    static String validateInstrumentationRunner(IJavaProject project, String instrumentation) {
        AndroidManifestParser manifestParser;
        try {
            manifestParser = AndroidManifestParser.parse(
                    project, null /* errorListener */,
                    true /* gatherData */, false /* markErrors */);
            // check if this instrumentation is the standard test runner
            if (!instrumentation.equals(AndroidConstants.CLASS_INSTRUMENTATION_RUNNER)) {
                // check if it extends the standard test runner
                String result = BaseProjectHelper.testClassForManifest(project,
                        instrumentation, AndroidConstants.CLASS_INSTRUMENTATION_RUNNER, true);
                if (result != BaseProjectHelper.TEST_CLASS_OK) {
                    return String.format("The instrumentation runner must be of type %s", 
                            AndroidConstants.CLASS_INSTRUMENTATION_RUNNER);
                }
            }
            if (!hasTestRunnerLibrary(manifestParser)) {
                return String.format("%s does not not use the %s library", 
                        project.getProject().getName(), AndroidConstants.LIBRARY_TEST_RUNNER);
            }
        } catch (CoreException e) {
            String err = String.format("Error parsing AndroidManifest for %s", 
                    project.getProject().getName());
            AdtPlugin.log(e, err);
            return err;
        }  
        return INSTRUMENTATION_OK;
    }
 
    /**
     * Helper method to determine if given manifest has a <code>AndroidConstants.LIBRARY_TEST_RUNNER
     * </code> library reference
     *
     * @param manifestParser the {@link AndroidManifestParser} to search
     * @return true if test runner library found, false otherwise
     */
    static boolean hasTestRunnerLibrary(AndroidManifestParser manifestParser) {
       for (String lib : manifestParser.getUsesLibraries()) {
           if (lib.equals(AndroidConstants.LIBRARY_TEST_RUNNER)) {
               return true;
           }
       }
       return false;
    }
}
