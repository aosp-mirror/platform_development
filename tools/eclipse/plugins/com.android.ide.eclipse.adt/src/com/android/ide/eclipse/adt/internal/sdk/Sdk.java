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

package com.android.ide.eclipse.adt.internal.sdk;

import com.android.ddmlib.IDevice;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.project.AndroidClasspathContainerInitializer;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IProjectListener;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData.LayoutBridge;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.project.ApkConfigurationHelper;
import com.android.sdklib.internal.project.ProjectProperties;
import com.android.sdklib.internal.project.ProjectProperties.PropertyType;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Central point to load, manipulate and deal with the Android SDK. Only one SDK can be used
 * at the same time.
 *
 * To start using an SDK, call {@link #loadSdk(String)} which returns the instance of
 * the Sdk object.
 *
 * To get the list of platforms or add-ons present in the SDK, call {@link #getTargets()}.
 */
public class Sdk implements IProjectListener {
    private static Sdk sCurrentSdk = null;

    private final SdkManager mManager;
    private final AvdManager mAvdManager;

    private final HashMap<IProject, IAndroidTarget> mProjectTargetMap =
            new HashMap<IProject, IAndroidTarget>();
    private final HashMap<IAndroidTarget, AndroidTargetData> mTargetDataMap =
            new HashMap<IAndroidTarget, AndroidTargetData>();
    private final HashMap<IProject, Map<String, String>> mProjectApkConfigMap =
            new HashMap<IProject, Map<String, String>>();
    private final String mDocBaseUrl;

    /**
     * Classes implementing this interface will receive notification when targets are changed.
     */
    public interface ITargetChangeListener {
        /**
         * Sent when project has its target changed.
         */
        void onProjectTargetChange(IProject changedProject);

        /**
         * Called when the targets are loaded (either the SDK finished loading when Eclipse starts,
         * or the SDK is changed).
         */
        void onTargetsLoaded();
    }

    /**
     * Loads an SDK and returns an {@link Sdk} object if success.
     * @param sdkLocation the OS path to the SDK.
     */
    public static Sdk loadSdk(String sdkLocation) {
        if (sCurrentSdk != null) {
            sCurrentSdk.dispose();
            sCurrentSdk = null;
        }

        final ArrayList<String> logMessages = new ArrayList<String>();
        ISdkLog log = new ISdkLog() {
            public void error(Throwable throwable, String errorFormat, Object... arg) {
                if (errorFormat != null) {
                    logMessages.add(String.format("Error: " + errorFormat, arg));
                }

                if (throwable != null) {
                    logMessages.add(throwable.getMessage());
                }
            }

            public void warning(String warningFormat, Object... arg) {
                logMessages.add(String.format("Warning: " + warningFormat, arg));
            }

            public void printf(String msgFormat, Object... arg) {
                logMessages.add(String.format(msgFormat, arg));
            }
        };

        // get an SdkManager object for the location
        SdkManager manager = SdkManager.createManager(sdkLocation, log);
        if (manager != null) {
            AvdManager avdManager = null;
            try {
                avdManager = new AvdManager(manager, log);
            } catch (AndroidLocationException e) {
                log.error(e, "Error parsing the AVDs");
            }
            sCurrentSdk = new Sdk(manager, avdManager);
            return sCurrentSdk;
        } else {
            StringBuilder sb = new StringBuilder("Error Loading the SDK:\n");
            for (String msg : logMessages) {
                sb.append('\n');
                sb.append(msg);
            }
            AdtPlugin.displayError("Android SDK", sb.toString());
        }
        return null;
    }

    /**
     * Returns the current {@link Sdk} object.
     */
    public static Sdk getCurrent() {
        return sCurrentSdk;
    }

    /**
     * Returns the location (OS path) of the current SDK.
     */
    public String getSdkLocation() {
        return mManager.getLocation();
    }

    /**
     * Returns the URL to the local documentation.
     * Can return null if no documentation is found in the current SDK.
     *
     * @return A file:// URL on the local documentation folder if it exists or null.
     */
    public String getDocumentationBaseUrl() {
        return mDocBaseUrl;
    }

    /**
     * Returns the list of targets that are available in the SDK.
     */
    public IAndroidTarget[] getTargets() {
        return mManager.getTargets();
    }

    /**
     * Returns a target from a hash that was generated by {@link IAndroidTarget#hashString()}.
     *
     * @param hash the {@link IAndroidTarget} hash string.
     * @return The matching {@link IAndroidTarget} or null.
     */
    public IAndroidTarget getTargetFromHashString(String hash) {
        return mManager.getTargetFromHashString(hash);
    }

    /**
     * Sets a new target and a new list of Apk configuration for a given project.
     *
     * @param project the project to receive the new apk configurations
     * @param target The new target to set, or <code>null</code> to not change the current target.
     * @param apkConfigMap a map of apk configurations. The map contains (name, filter) where name
     * is the name of the configuration (a-zA-Z0-9 only), and filter is the comma separated list of
     * resource configuration to include in the apk (see aapt -c). Can be <code>null</code> if the
     * apk configurations should not be updated.
     */
    public void setProject(IProject project, IAndroidTarget target,
            Map<String, String> apkConfigMap) {
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            boolean resolveProject = false;
            boolean compileProject = false;
            boolean cleanProject = false;

            ProjectProperties properties = ProjectProperties.load(
                    project.getLocation().toOSString(), PropertyType.DEFAULT);
            if (properties == null) {
                // doesn't exist yet? we create it.
                properties = ProjectProperties.create(project.getLocation().toOSString(),
                        PropertyType.DEFAULT);
            }

            if (target != null) {
                // look for the current target of the project
                IAndroidTarget previousTarget = mProjectTargetMap.get(project);

                if (target != previousTarget) {
                    // save the target hash string in the project persistent property
                    properties.setAndroidTarget(target);

                    // put it in a local map for easy access.
                    mProjectTargetMap.put(project, target);

                    resolveProject = true;
                }
            }

            if (apkConfigMap != null) {
                // save the apk configs in the project persistent property
                cleanProject = ApkConfigurationHelper.setConfigs(properties, apkConfigMap);

                // put it in a local map for easy access.
                mProjectApkConfigMap.put(project, apkConfigMap);

                compileProject = true;
            }

            // we are done with the modification. Save the property file.
            try {
                properties.save();
            } catch (IOException e) {
                AdtPlugin.log(e, "Failed to save default.properties for project '%s'",
                        project.getName());
            }

            if (resolveProject) {
                // force a resolve of the project by updating the classpath container.
                IJavaProject javaProject = JavaCore.create(project);
                AndroidClasspathContainerInitializer.updateProjects(
                        new IJavaProject[] { javaProject });
            } else if (compileProject) {
                // If there was removed configs, we clean instead of build
                // (to remove the obsolete ap_ and apk file from removed configs).
                try {
                    project.build(cleanProject ?
                                IncrementalProjectBuilder.CLEAN_BUILD :
                                IncrementalProjectBuilder.FULL_BUILD,
                            null);
                } catch (CoreException e) {
                    // failed to build? force resolve instead.
                    IJavaProject javaProject = JavaCore.create(project);
                    AndroidClasspathContainerInitializer.updateProjects(
                            new IJavaProject[] { javaProject });
                }
            }

            // finally, update the opened editors.
            if (resolveProject) {
                AdtPlugin.getDefault().updateTargetListener(project);
            }
        }
    }

    /**
     * Returns the {@link IAndroidTarget} object associated with the given {@link IProject}.
     */
    public IAndroidTarget getTarget(IProject project) {
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            IAndroidTarget target = mProjectTargetMap.get(project);
            if (target == null) {
                // get the value from the project persistent property.
                String targetHashString = loadProjectProperties(project, this);

                if (targetHashString != null) {
                    target = mManager.getTargetFromHashString(targetHashString);
                }
            }

            return target;
        }
    }


    /**
     * Parses the project properties and returns the hash string uniquely identifying the
     * target of the given project.
     * <p/>
     * This methods reads the content of the <code>default.properties</code> file present in
     * the root folder of the project.
     * <p/>The returned string is equivalent to the return of {@link IAndroidTarget#hashString()}.
     * @param project The project for which to return the target hash string.
     * @param sdkStorage The sdk in which to store the Apk Configs. Can be null.
     * @return the hash string or null if the project does not have a target set.
     */
    private static String loadProjectProperties(IProject project, Sdk sdkStorage) {
        // load the default.properties from the project folder.
        IPath location = project.getLocation();
        if (location == null) {  // can return null when the project is being deleted.
            // do nothing and return null;
            return null;
        }
        ProjectProperties properties = ProjectProperties.load(location.toOSString(),
                PropertyType.DEFAULT);
        if (properties == null) {
            AdtPlugin.log(IStatus.ERROR, "Failed to load properties file for project '%s'",
                    project.getName());
            return null;
        }

        if (sdkStorage != null) {
            synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
                Map<String, String> configMap = ApkConfigurationHelper.getConfigs(properties);

                if (configMap != null) {
                    sdkStorage.mProjectApkConfigMap.put(project, configMap);
                }
            }
        }

        return properties.getProperty(ProjectProperties.PROPERTY_TARGET);
    }

    /**
     * Returns the hash string uniquely identifying the target of a project.
     * <p/>
     * This methods reads the content of the <code>default.properties</code> file present in
     * the root folder of the project.
     * <p/>The string is equivalent to the return of {@link IAndroidTarget#hashString()}.
     * @param project The project for which to return the target hash string.
     * @return the hash string or null if the project does not have a target set.
     */
    public static String getProjectTargetHashString(IProject project) {
        return loadProjectProperties(project, null /*storeConfigs*/);
    }

    /**
     * Sets a target hash string in given project's <code>default.properties</code> file.
     * @param project The project in which to save the hash string.
     * @param targetHashString The target hash string to save. This must be the result from
     * {@link IAndroidTarget#hashString()}.
     */
    public static void setProjectTargetHashString(IProject project, String targetHashString) {
        // because we don't want to erase other properties from default.properties, we first load
        // them
        ProjectProperties properties = ProjectProperties.load(project.getLocation().toOSString(),
                PropertyType.DEFAULT);
        if (properties == null) {
            // doesn't exist yet? we create it.
            properties = ProjectProperties.create(project.getLocation().toOSString(),
                    PropertyType.DEFAULT);
        }

        // add/change the target hash string.
        properties.setProperty(ProjectProperties.PROPERTY_TARGET, targetHashString);

        // and rewrite the file.
        try {
            properties.save();
        } catch (IOException e) {
            AdtPlugin.log(e, "Failed to save default.properties for project '%s'",
                    project.getName());
        }
    }
    /**
     * Return the {@link AndroidTargetData} for a given {@link IAndroidTarget}.
     */
    public AndroidTargetData getTargetData(IAndroidTarget target) {
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            return mTargetDataMap.get(target);
        }
    }

    /**
     * Returns the configuration map for a given project.
     * <p/>The Map key are name to be used in the apk filename, while the values are comma separated
     * config values. The config value can be passed directly to aapt through the -c option.
     */
    public Map<String, String> getProjectApkConfigs(IProject project) {
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            return mProjectApkConfigMap.get(project);
        }
    }

    /**
     * Returns the {@link AvdManager}. If the AvdManager failed to parse the AVD folder, this could
     * be <code>null</code>.
     */
    public AvdManager getAvdManager() {
        return mAvdManager;
    }

    public static AndroidVersion getDeviceVersion(IDevice device) {
        try {
            Map<String, String> props = device.getProperties();
            String apiLevel = props.get(IDevice.PROP_BUILD_API_LEVEL);
            if (apiLevel == null) {
                return null;
            }

            return new AndroidVersion(Integer.parseInt(apiLevel),
                    props.get((IDevice.PROP_BUILD_CODENAME)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Sdk(SdkManager manager, AvdManager avdManager) {
        mManager = manager;
        mAvdManager = avdManager;

        // listen to projects closing
        ResourceMonitor monitor = ResourceMonitor.getMonitor();
        monitor.addProjectListener(this);

        // pre-compute some paths
        mDocBaseUrl = getDocumentationBaseUrl(mManager.getLocation() +
                SdkConstants.OS_SDK_DOCS_FOLDER);
    }

    /**
     *  Cleans and unloads the SDK.
     */
    private void dispose() {
        ResourceMonitor.getMonitor().removeProjectListener(this);
    }

    void setTargetData(IAndroidTarget target, AndroidTargetData data) {
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            mTargetDataMap.put(target, data);
        }
    }

    /**
     * Returns the URL to the local documentation.
     * Can return null if no documentation is found in the current SDK.
     *
     * @param osDocsPath Path to the documentation folder in the current SDK.
     *  The folder may not actually exist.
     * @return A file:// URL on the local documentation folder if it exists or null.
     */
    private String getDocumentationBaseUrl(String osDocsPath) {
        File f = new File(osDocsPath);

        if (f.isDirectory()) {
            try {
                // Note: to create a file:// URL, one would typically use something like
                // f.toURI().toURL().toString(). However this generates a broken path on
                // Windows, namely "C:\\foo" is converted to "file:/C:/foo" instead of
                // "file:///C:/foo" (i.e. there should be 3 / after "file:"). So we'll
                // do the correct thing manually.

                String path = f.getAbsolutePath();
                if (File.separatorChar != '/') {
                    path = path.replace(File.separatorChar, '/');
                }

                // For some reason the URL class doesn't add the mandatory "//" after
                // the "file:" protocol name, so it has to be hacked into the path.
                URL url = new URL("file", null, "//" + path);  //$NON-NLS-1$ //$NON-NLS-2$
                String result = url.toString();
                return result;
            } catch (MalformedURLException e) {
                // ignore malformed URLs
            }
        }

        return null;
    }

    public void projectClosed(IProject project) {
        // get the target project
        synchronized (AdtPlugin.getDefault().getSdkLockObject()) {
            IAndroidTarget target = mProjectTargetMap.get(project);
            if (target != null) {
                // get the bridge for the target, and clear the cache for this project.
                AndroidTargetData data = mTargetDataMap.get(target);
                if (data != null) {
                    LayoutBridge bridge = data.getLayoutBridge();
                    if (bridge != null && bridge.status == LoadStatus.LOADED) {
                        bridge.bridge.clearCaches(project);
                    }
                }
            }

            // now remove the project for the maps.
            mProjectTargetMap.remove(project);
            mProjectApkConfigMap.remove(project);
        }
    }

    public void projectDeleted(IProject project) {
        projectClosed(project);
    }

    public void projectOpened(IProject project) {
        // ignore this. The project will be added to the map the first time the target needs
        // to be resolved.
    }

    public void projectOpenedWithWorkspace(IProject project) {
        // ignore this. The project will be added to the map the first time the target needs
        // to be resolved.
    }
}


