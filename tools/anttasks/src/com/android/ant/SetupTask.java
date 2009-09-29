/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ant;

import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkManager;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.xml.AndroidXPathFactory;
import com.android.sdklib.xml.AndroidManifest;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ImportTask;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Path.PathElement;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 * Setup/Import Ant task. This task accomplishes:
 * <ul>
 * <li>Gets the project target hash string from {@link ProjectProperties#PROPERTY_TARGET},
 * and resolves it to get the project's {@link IAndroidTarget}.</li>
 * <li>Sets up properties so that aapt can find the android.jar in the resolved target.</li>
 * <li>Sets up the boot classpath ref so that the <code>javac</code> task knows where to find
 * the libraries. This includes the default android.jar from the resolved target but also optional
 * libraries provided by the target (if any, when the target is an add-on).</li>
 * <li>Imports the build rules located in the resolved target so that the build actually does
 * something. This can be disabled with the attribute <var>import</var> set to <code>false</code>
 * </li></ul>
 *
 * This is used in build.xml/template.
 *
 */
public final class SetupTask extends ImportTask {
    private final static String ANDROID_RULES = "android_rules.xml";
    // additional android rules for test project - depends on android_rules.xml
    private final static String ANDROID_TEST_RULES = "android_test_rules.xml";
    // ant property with the path to the android.jar
    private final static String PROPERTY_ANDROID_JAR = "android.jar";
    // LEGACY - compatibility with 1.6 and before
    private final static String PROPERTY_ANDROID_JAR_LEGACY = "android-jar";
    // ant property with the path to the framework.jar
    private final static String PROPERTY_ANDROID_AIDL = "android.aidl";
    // LEGACY - compatibility with 1.6 and before
    private final static String PROPERTY_ANDROID_AIDL_LEGACY = "android-aidl";
    // ant property with the path to the aapt tool
    private final static String PROPERTY_AAPT = "aapt";
    // ant property with the path to the aidl tool
    private final static String PROPERTY_AIDL = "aidl";
    // ant property with the path to the dx tool
    private final static String PROPERTY_DX = "dx";
    // ref id to the <path> object containing all the boot classpaths.
    private final static String REF_CLASSPATH = "android.target.classpath";

    private boolean mDoImport = true;

    @Override
    public void execute() throws BuildException {
        Project antProject = getProject();

        // get the SDK location
        String sdkLocation = antProject.getProperty(ProjectProperties.PROPERTY_SDK);

        // check if it's valid and exists
        if (sdkLocation == null || sdkLocation.length() == 0) {
            // LEGACY support: project created with 1.6 or before may be using a different
            // property to declare the location of the SDK. At this point, we cannot
            // yet check which target is running so we check both always.
            sdkLocation = antProject.getProperty(ProjectProperties.PROPERTY_SDK_LEGACY);
            if (sdkLocation == null || sdkLocation.length() == 0) {
                throw new BuildException("SDK Location is not set.");
            }
        }

        File sdk = new File(sdkLocation);
        if (sdk.isDirectory() == false) {
            throw new BuildException(String.format("SDK Location '%s' is not valid.", sdkLocation));
        }

        // get the target property value
        String targetHashString = antProject.getProperty(ProjectProperties.PROPERTY_TARGET);

        boolean isTestProject = false;

        if (antProject.getProperty("tested.project.dir") != null) {
            isTestProject = true;
        }

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
            System.out.println("Platform Version: " + androidTarget.getVersionName());
        }
        System.out.println("API level: " + androidTarget.getVersion().getApiString());

        // always check the manifest minSdkVersion.
        checkManifest(antProject, androidTarget.getVersion());

        // sets up the properties to find android.jar/framework.aidl/target tools
        String androidJar = androidTarget.getPath(IAndroidTarget.ANDROID_JAR);
        antProject.setProperty(PROPERTY_ANDROID_JAR, androidJar);

        String androidAidl = androidTarget.getPath(IAndroidTarget.ANDROID_AIDL);
        antProject.setProperty(PROPERTY_ANDROID_AIDL, androidAidl);

        antProject.setProperty(PROPERTY_AAPT, androidTarget.getPath(IAndroidTarget.AAPT));
        antProject.setProperty(PROPERTY_AIDL, androidTarget.getPath(IAndroidTarget.AIDL));
        antProject.setProperty(PROPERTY_DX, androidTarget.getPath(IAndroidTarget.DX));

        // sets up the boot classpath

        // create the Path object
        Path bootclasspath = new Path(antProject);

        // create a PathElement for the framework jar
        PathElement element = bootclasspath.createPathElement();
        element.setPath(androidJar);

        // create PathElement for each optional library.
        IOptionalLibrary[] libraries = androidTarget.getOptionalLibraries();
        if (libraries != null) {
            HashSet<String> visitedJars = new HashSet<String>();
            for (IOptionalLibrary library : libraries) {
                String jarPath = library.getJarPath();
                if (visitedJars.contains(jarPath) == false) {
                    visitedJars.add(jarPath);

                    element = bootclasspath.createPathElement();
                    element.setPath(library.getJarPath());
                }
            }
        }

        // finally sets the path in the project with a reference
        antProject.addReference(REF_CLASSPATH, bootclasspath);

        // find the file to import, and import it.
        String templateFolder = androidTarget.getPath(IAndroidTarget.TEMPLATES);

        // LEGACY support. android_rules.xml in older platforms expects properties with
        // older names. This sets those properties to make sure the rules will work.
        if (androidTarget.getVersion().getApiLevel() <= 4) { // 1.6 and earlier
            antProject.setProperty(PROPERTY_ANDROID_JAR_LEGACY, androidJar);
            antProject.setProperty(PROPERTY_ANDROID_AIDL_LEGACY, androidAidl);
            antProject.setProperty(ProjectProperties.PROPERTY_SDK_LEGACY, sdkLocation);
            String appPackage = antProject.getProperty(ProjectProperties.PROPERTY_APP_PACKAGE);
            if (appPackage != null && appPackage.length() > 0) {
                antProject.setProperty(ProjectProperties.PROPERTY_APP_PACKAGE_LEGACY, appPackage);
            }
        }

        // Now the import section. This is only executed if the task actually has to import a file.
        if (mDoImport) {
            // make sure the file exists.
            File templates = new File(templateFolder);

            if (templates.isDirectory() == false) {
                throw new BuildException(String.format("Template directory '%s' is missing.",
                        templateFolder));
            }

            String importedRulesFileName = isTestProject ? ANDROID_TEST_RULES : ANDROID_RULES;

            // now check the rules file exists.
            File rules = new File(templateFolder, importedRulesFileName);

            if (rules.isFile() == false) {
                throw new BuildException(String.format("Build rules file '%s' is missing.",
                        templateFolder));
            }

            // set the file location to import
            setFile(rules.getAbsolutePath());

            // and import
            super.execute();
        }
    }

    /**
     * Sets the value of the "import" attribute.
     * @param value the value.
     */
    public void setImport(boolean value) {
        mDoImport = value;
    }

    /**
     * Checks the manifest <code>minSdkVersion</code> attribute.
     * @param antProject the ant project
     * @param androidVersion the version of the platform the project is compiling against.
     */
    private void checkManifest(Project antProject, AndroidVersion androidVersion) {
        try {
            File manifest = new File(antProject.getBaseDir(), "AndroidManifest.xml");

            XPath xPath = AndroidXPathFactory.newXPath();

            String value = xPath.evaluate(
                    "/"  + AndroidManifest.NODE_MANIFEST +
                    "/"  + AndroidManifest.NODE_USES_SDK +
                    "/@" + AndroidXPathFactory.DEFAULT_NS_PREFIX + ":" +
                            AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION,
                    new InputSource(new FileInputStream(manifest)));

            if (androidVersion.isPreview()) {
                // in preview mode, the content of the minSdkVersion must match exactly the
                // platform codename.
                String codeName = androidVersion.getCodename();
                if (codeName.equals(value) == false) {
                    throw new BuildException(String.format(
                            "For '%1$s' SDK Preview, attribute minSdkVersion in AndroidManifest.xml must be '%1$s'",
                            codeName));
                }
            } else if (value.length() > 0) {
                // for normal platform, we'll only display warnings if the value is lower or higher
                // than the target api level.
                // First convert to an int.
                int minSdkValue = -1;
                try {
                    minSdkValue = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // looks like it's not a number: error!
                    throw new BuildException(String.format(
                            "Attribute %1$s in AndroidManifest.xml must be an Integer!",
                            AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION));
                }

                int projectApiLevel = androidVersion.getApiLevel();
                if (minSdkValue < projectApiLevel) {
                    System.out.println(String.format(
                            "WARNING: Attribute %1$s in AndroidManifest.xml (%2$d) is lower than the project target API level (%3$d)",
                            AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION,
                            minSdkValue, projectApiLevel));
                } else if (minSdkValue > androidVersion.getApiLevel()) {
                    System.out.println(String.format(
                            "WARNING: Attribute %1$s in AndroidManifest.xml (%2$d) is higher than the project target API level (%3$d)",
                            AndroidManifest.ATTRIBUTE_MIN_SDK_VERSION,
                            minSdkValue, projectApiLevel));
                }
            } else {
                // no minSdkVersion? display a warning
                System.out.println(
                        "WARNING: No minSdkVersion value set. Application will install on all Android versions.");
            }

        } catch (XPathExpressionException e) {
            throw new BuildException(e);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        }
    }
}
