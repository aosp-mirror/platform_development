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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;
import org.osgi.framework.Bundle;
import java.io.IOException;
import java.net.URL;

/**
 * <p>
 * For Android projects, android.jar gets added to the launch configuration of
 * JUnit tests as a bootstrap entry. This breaks JUnit tests as android.jar
 * contains a skeleton version of JUnit classes and the JVM will stop with an error similar
 * to: <blockquote> Error occurred during initialization of VM
 * java/lang/NoClassDefFoundError: java/lang/ref/FinalReference </blockquote>
 * <p>
 * At compile time, Eclipse does not know that there is no valid junit.jar in
 * the classpath since it can find a correct reference to all the necessary
 * org.junit.* classes in the android.jar so it does not prompt the user to add
 * the JUnit3 or JUnit4 jar.
 * <p>
 * This delegates removes the android.jar from the bootstrap path and if
 * necessary also puts back the junit.jar in the user classpath.
 * <p>
 * This delegate will be present for both Java and Android projects (delegates
 * setting instead of only the current project) but the behavior for Java
 * projects should be neutral since:
 * <ol>
 * <li>Java tests can only compile (and then run) when a valid junit.jar is
 * present
 * <li>There is no android.jar in Java projects
 * </ol>
 */
public class JUnitLaunchConfigDelegate extends JUnitLaunchConfigurationDelegate {

    private static final String JUNIT_JAR = "junit.jar"; //$NON-NLS-1$

    @Override
    public String[][] getBootpathExt(ILaunchConfiguration configuration) throws CoreException {
        String[][] bootpath = super.getBootpathExt(configuration);
        return fixBootpathExt(bootpath);
    }

    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        String[] classpath = super.getClasspath(configuration);
        return fixClasspath(classpath, getJavaProjectName(configuration));
    }

    /**
     * Removes the android.jar from the bootstrap path if present.
     * 
     * @param bootpath Array of Arrays of bootstrap class paths
     * @return a new modified (if applicable) bootpath
     */
    public static String[][] fixBootpathExt(String[][] bootpath) {
        for (int i = 0; i < bootpath.length; i++) {
            if (bootpath[i] != null) {
                // we assume that the android.jar can only be present in the
                // bootstrap path of android tests
                if (bootpath[i][0].endsWith(SdkConstants.FN_FRAMEWORK_LIBRARY)) {
                    bootpath[i] = null;
                }
            }
        }
        return bootpath;
    }

    /**
     * Add the junit.jar to the user classpath; since Eclipse was relying on
     * android.jar to provide the appropriate org.junit classes, it does not
     * know it actually needs the junit.jar.
     * 
     * @param classpath Array containing classpath 
     * @param projectName The name of the project (for logging purposes) 
     * 
     * @return a new modified (if applicable) classpath
     */
    public static String[] fixClasspath(String[] classpath, String projectName) {
        // search for junit.jar; if any are found return immediately
        for (int i = 0; i < classpath.length; i++) {
            if (classpath[i].endsWith(JUNIT_JAR)) { 
                return classpath;
            }
        }

        // This delegate being called without a junit.jar present is only
        // possible for Android projects. In a non-Android project, the test
        // would not compile and would be unable to run.
        try {
            // junit4 is backward compatible with junit3 and they uses the
            // same junit.jar from bundle org.junit:
            // When a project has mixed JUnit3 and JUnit4 tests, if JUnit3 jar
            // is added first it is then replaced by the JUnit4 jar when user is
            // prompted to fix the JUnit4 test failure
            String jarLocation = getJunitJarLocation();
            // we extend the classpath by one element and append junit.jar
            String[] newClasspath = new String[classpath.length + 1];
            System.arraycopy(classpath, 0, newClasspath, 0, classpath.length);
            newClasspath[newClasspath.length - 1] = jarLocation;
            classpath = newClasspath;
        } catch (IOException e) {
            // This should not happen as we depend on the org.junit
            // plugin explicitly; the error is logged here so that the user can
            // trace back the cause when the test fails to run
            AdtPlugin.log(e, "Could not find a valid junit.jar");
            AdtPlugin.printErrorToConsole(projectName,
                    "Could not find a valid junit.jar");
            // Return the classpath as-is (with no junit.jar) anyway because we
            // will let the actual launch config fails.
        }

        return classpath;
    }

    /**
     * Returns the path of the junit jar in the highest version bundle.
     * 
     * (This is public only so that the test can call it)
     * 
     * @return the path as a string
     * @throws IOException
     */
    public static String getJunitJarLocation() throws IOException {
        Bundle bundle = Platform.getBundle("org.junit"); //$NON-NLS-1$
        if (bundle == null) {
            throw new IOException("Cannot find org.junit bundle");
        }
        URL jarUrl = bundle.getEntry(AndroidConstants.WS_SEP + JUNIT_JAR);
        return FileLocator.resolve(jarUrl).getFile();
    }
}
