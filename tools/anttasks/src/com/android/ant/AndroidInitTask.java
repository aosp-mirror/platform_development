/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ant;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkManager;
import com.android.sdklib.project.ProjectProperties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;

import java.io.File;
import java.util.ArrayList;

/**
 * Import Target Ant task. This task accomplishes:
 * <ul>
 * <li>Gets the project target hash string from {@link ProjectProperties#PROPERTY_TARGET},
 * and resolves it.</li>
 * <li>Sets up ant properties so that the rest of the Ant scripts finds:
 *    <ul>
 *       <li>Path to the underlying platform to access the build rules ('android-platform')<li>
 *    </ul>
 * </li>
 * </ul>
 * 
 * This is used in build.xml/template.
 *
 */
public class AndroidInitTask extends ImportTask {
    private final static String ANDROID_RULES = "android_rules.xml";
    
    private final static String PROPERTY_ANDROID_JAR = "android-jar";
    private final static String PROPERTY_ANDROID_AIDL = "android-aidl";

    @Override
    public void execute() throws BuildException {
        Project antProject = getProject();
        
        // get the SDK location
        String sdkLocation = antProject.getProperty(ProjectProperties.PROPERTY_SDK);
        
        // check if it's valid and exists
        if (sdkLocation == null || sdkLocation.length() == 0) {
            throw new BuildException("SDK Location is not set.");
        }
        
        File sdk = new File(sdkLocation);
        if (sdk.isDirectory() == false) {
            throw new BuildException(String.format("SDK Location '%s' is not valid.", sdkLocation));
        }

        // get the target property value
        String targetHashString = antProject.getProperty(ProjectProperties.PROPERTY_TARGET);
        if (targetHashString == null) {
            throw new BuildException("Android Target is not set.");
        }

        // load up the sdk targets.
        final ArrayList<String> messages = new ArrayList<String>();
        SdkManager manager = SdkManager.createManager(sdkLocation, new ISdkLog() {
            public void error(Throwable t, String errorFormat, Object... args) {
                if (errorFormat != null) {
                    messages.add(String.format("Error: " + errorFormat, args));
                }
                if (t != null) {
                    messages.add("Error: " + t.getMessage());
                }
            }

            public void printf(String msgFormat, Object... args) {
                messages.add(String.format(msgFormat, args));
            }

            public void warning(String warningFormat, Object... args) {
                messages.add(String.format("Warning: " + warningFormat, args));
            }
        });

        if (manager == null) {
            // since we failed to parse the SDK, lets display the parsing output.
            for (String msg : messages) {
                System.out.println(msg);
            }
            throw new BuildException("Failed to parse SDK content.");
        }

        // resolve it
        IAndroidTarget androidTarget = manager.getTargetFromHashString(targetHashString);
        
        if (androidTarget == null) {
            throw new BuildException(String.format(
                    "Unable to resolve target '%s'", targetHashString));
        }
        
        // display it
        System.out.println("Project Target: " + androidTarget.getName());
        if (androidTarget.isPlatform() == false) {
            System.out.println("Vendor: " + androidTarget.getVendor());
        }
        System.out.println("Platform Version: " + androidTarget.getApiVersionName());
        System.out.println("API level: " + androidTarget.getApiVersionNumber());
        
        // sets up the properties.
        String androidJar = androidTarget.getPath(IAndroidTarget.ANDROID_JAR);
        String androidAidl = androidTarget.getPath(IAndroidTarget.ANDROID_AIDL);
        
        antProject.setProperty(PROPERTY_ANDROID_JAR, androidJar);
        antProject.setProperty(PROPERTY_ANDROID_AIDL, androidAidl);
        
        // find the file to import, and import it.
        String templateFolder = androidTarget.getPath(IAndroidTarget.TEMPLATES);
        
        // make sure the file exists.
        File templates = new File(templateFolder);
        if (templates.isDirectory() == false) {
            throw new BuildException(String.format("Template directory '%s' is missing.",
                    templateFolder));
        }
        
        // now check the rules file exists.
        File rules = new File(templateFolder, ANDROID_RULES);
        if (rules.isFile() == false) {
            throw new BuildException(String.format("Build rules file '%s' is missing.",
                    templateFolder));
       }
        
        // set the file location to import
        setFile(rules.getAbsolutePath());
        
        // and import it
        super.execute();
    }
}
