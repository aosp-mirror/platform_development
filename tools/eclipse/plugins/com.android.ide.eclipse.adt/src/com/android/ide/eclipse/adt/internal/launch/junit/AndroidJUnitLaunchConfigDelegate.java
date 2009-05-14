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

package com.android.ide.eclipse.adt.internal.launch.junit;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunch;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchConfiguration;
import com.android.ide.eclipse.adt.internal.launch.AndroidLaunchController;
import com.android.ide.eclipse.adt.internal.launch.IAndroidLaunchAction;
import com.android.ide.eclipse.adt.internal.launch.LaunchConfigDelegate;
import com.android.ide.eclipse.adt.internal.launch.junit.runtime.AndroidJUnitLaunchInfo;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser.Instrumentation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.launcher.TestKindRegistry;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;

/**
 * Run configuration that can execute JUnit tests on an Android platform.
 * <p/>
 * Will deploy apps on target Android platform by reusing functionality from ADT
 * LaunchConfigDelegate, and then run JUnits tests by reusing functionality from JDT
 * JUnitLaunchConfigDelegate.
 */
@SuppressWarnings("restriction")
public class AndroidJUnitLaunchConfigDelegate extends LaunchConfigDelegate {

    /** Launch config attribute that stores instrumentation runner. */
    static final String ATTR_INSTR_NAME = AdtPlugin.PLUGIN_ID + ".instrumentation"; //$NON-NLS-1$

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    @Override
    protected void doLaunch(final ILaunchConfiguration configuration, final String mode,
            IProgressMonitor monitor, IProject project, final AndroidLaunch androidLaunch,
            AndroidLaunchConfiguration config, AndroidLaunchController controller,
            IFile applicationPackage, AndroidManifestParser manifestParser) {

        String runner = getRunner(project, configuration, manifestParser);
        if (runner == null) {
            AdtPlugin.displayError("Android Launch",
                    "An instrumention test runner is not specified!");
            androidLaunch.stopLaunch();
            return;
        }
        // get the target app's package
        String targetAppPackage = getTargetPackage(manifestParser, runner); 
        if (targetAppPackage == null) {
            AdtPlugin.displayError("Android Launch",
                    String.format("A target package for instrumention test runner %1$s could not be found!", 
                    runner));
            androidLaunch.stopLaunch();
            return; 
        }
        String testAppPackage = manifestParser.getPackage();
        AndroidJUnitLaunchInfo junitLaunchInfo = new AndroidJUnitLaunchInfo(project, 
                testAppPackage, runner);
        junitLaunchInfo.setTestClass(getTestClass(configuration));
        junitLaunchInfo.setTestPackage(getTestPackage(configuration));
        junitLaunchInfo.setTestMethod(getTestMethod(configuration));
        junitLaunchInfo.setLaunch(androidLaunch);
        IAndroidLaunchAction junitLaunch = new AndroidJUnitLaunchAction(junitLaunchInfo);
        
        controller.launch(project, mode, applicationPackage, testAppPackage, targetAppPackage,
                manifestParser.getDebuggable(), manifestParser.getApiLevelRequirement(),
                junitLaunch, config, androidLaunch, monitor);
    }

    /**
     * Get the target Android application's package for the given instrumentation runner, or 
     * <code>null</code> if it could not be found.
     *
     * @param manifestParser the {@link AndroidManifestParser} for the test project
     * @param runner the instrumentation runner class name
     * @return the target package or <code>null</code>
     */
    private String getTargetPackage(AndroidManifestParser manifestParser, String runner) {
        for (Instrumentation instr : manifestParser.getInstrumentations()) {
            if (instr.getName().equals(runner)) {
                return instr.getTargetPackage();
            }
        }
        return null;
    }

    /**
     * Returns the test package stored in the launch configuration, or <code>null</code> if not 
     * specified.
     * 
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test package info from
     * @return the test package or <code>null</code>.
     */
    private String getTestPackage(ILaunchConfiguration configuration) {
        // try to retrieve a package name from the JUnit container attribute
        String containerHandle = getStringLaunchAttribute(
                JUnitLaunchConfigurationConstants.ATTR_TEST_CONTAINER, configuration);
        if (containerHandle != null && containerHandle.length() > 0) {
            IJavaElement element = JavaCore.create(containerHandle);
            // containerHandle could be a IProject, check if its a java package
            if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
                return element.getElementName();
            }
        }
        return null;
    }

    /**
     * Returns the test class stored in the launch configuration.
     *
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test class info from
     * @return the test class. <code>null</code> if not specified.
     */
    private String getTestClass(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME,
                configuration);
    }
    
    /**
     * Returns the test method stored in the launch configuration.
     *
     * @param configuration the {@link ILaunchConfiguration} to retrieve the test method info from
     * @return the test method. <code>null</code> if not specified.
     */
    private String getTestMethod(ILaunchConfiguration configuration) {
        return getStringLaunchAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_METHOD_NAME,
                configuration);
    }

    /**
     * Gets a instrumentation runner for the launch. 
     * <p/>
     * If a runner is stored in the given <code>configuration</code>, will return that.
     * Otherwise, will try to find the first valid runner for the project.
     * If a runner can still not be found, will return <code>null</code>.
     * 
     * @param project the {@link IProject} for the app 
     * @param configuration the {@link ILaunchConfiguration} for the launch
     * @param manifestParser the {@link AndroidManifestParser} for the project
     * 
     * @return <code>null</code> if no instrumentation runner can be found, otherwise return
     *   the fully qualified runner name.
     */
    private String getRunner(IProject project, ILaunchConfiguration configuration,
            AndroidManifestParser manifestParser) {
        try {
            String runner = getRunnerFromConfig(configuration);
            if (runner != null) {
                return runner;
            }
            final InstrumentationRunnerValidator instrFinder = new InstrumentationRunnerValidator(
                    BaseProjectHelper.getJavaProject(project), manifestParser);
            runner = instrFinder.getValidInstrumentationTestRunner();
            if (runner != null) {
                AdtPlugin.printErrorToConsole(project,
                        String.format("Warning: No instrumentation runner found for the launch, " +
                                "using %1$s", runner));
                return runner;
            }
            AdtPlugin.printErrorToConsole(project,
                    String.format("ERROR: Application does not specify a %1$s instrumentation or does not declare uses-library %2$s",
                    AndroidConstants.CLASS_INSTRUMENTATION_RUNNER, 
                    AndroidConstants.LIBRARY_TEST_RUNNER));
            return null;
        } catch (CoreException e) {
            AdtPlugin.log(e, "Error when retrieving instrumentation info"); //$NON-NLS-1$           
        }
        return null;

    }

    private String getRunnerFromConfig(ILaunchConfiguration configuration) throws CoreException {
        return getStringLaunchAttribute(ATTR_INSTR_NAME, configuration);
    }
    
    /**
     * Helper method to retrieve a string attribute from the launch configuration
     * 
     * @param attributeName name of the launch attribute
     * @param configuration the {@link ILaunchConfiguration} to retrieve the attribute from
     * @return the attribute's value. <code>null</code> if not found.
     */
    private String getStringLaunchAttribute(String attributeName,
            ILaunchConfiguration configuration) {
        try {
            String attrValue = configuration.getAttribute(attributeName, EMPTY_STRING);
            if (attrValue.length() < 1) {
                return null;
            }
            return attrValue;
        } catch (CoreException e) {
            AdtPlugin.log(e, String.format("Error when retrieving launch info %1$s",  //$NON-NLS-1$
                    attributeName));
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
}
