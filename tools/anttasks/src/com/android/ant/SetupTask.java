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

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkManager;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.xml.AndroidXPathFactory;
import com.android.sdklib.xml.ManifestConstants;

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

    // ant property with the path to the android.jar
    private final static String PROPERTY_ANDROID_JAR = "android-jar";
    // ant property with the path to the framework.jar
    private final static String PROPERTY_ANDROID_AIDL = "android-aidl";
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
            System.out.println("Platform Version: " + androidTarget.getVersionName());
        }
        System.out.println("API level: " + androidTarget.getVersion().getApiString());

        // if needed check the manifest so that it matches the target
        if (androidTarget.getVersion().isPreview()) {
            // for preview, the manifest minSdkVersion node *must* match the target codename
            checkManifest(antProject, androidTarget.getVersion().getCodename());
        }

        // sets up the properties to find android.jar/framework.aidl/target tools
        String androidJar = androidTarget.getPath(IAndroidTarget.ANDROID_JAR);
        antProject.setProperty(PROPERTY_ANDROID_JAR, androidJar);

        antProject.setProperty(PROPERTY_ANDROID_AIDL,
                androidTarget.getPath(IAndroidTarget.ANDROID_AIDL));
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

        // Now the import section. This is only executed if the task actually has to import a file.
        if (mDoImport) {
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

    private void checkManifest(Project antProject, String codename) {
        try {
            File manifest = new File(antProject.getBaseDir(), "AndroidManifest.xml");

            XPath xPath = AndroidXPathFactory.newXPath();

            String value = xPath.evaluate("/" + ManifestConstants.NODE_MANIFEST +"/" +
                    ManifestConstants.NODE_USES_SDK + "/@" +
                    AndroidXPathFactory.DEFAULT_NS_PREFIX + ":" +
                    ManifestConstants.ATTRIBUTE_MIN_SDK_VERSION,
                    new InputSource(new FileInputStream(manifest)));

            if (codename.equals(value) == false) {
                throw new BuildException(String.format("For '%1$s' SDK Preview, application manifest must declare minSdkVersion to '%1$s'",
                        codename));
            }
        } catch (XPathExpressionException e) {
            throw new BuildException(e);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        }
    }
}
