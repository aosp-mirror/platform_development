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

package com.android.ide.eclipse.adt.sdk;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.project.internal.AndroidClasspathContainerInitializer;
import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.SdkManager;
import com.android.sdklib.project.ProjectProperties;
import com.android.sdklib.project.ProjectProperties.PropertyType;
import com.android.sdklib.vm.VmManager;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Central point to load, manipulate and deal with the Android SDK. Only one SDK can be used
 * at the same time.
 * 
 * To start using an SDK, call {@link #loadSdk(String)} which returns the instance of
 * the Sdk object.
 * 
 * To get the list of platforms or add-ons present in the SDK, call {@link #getTargets()}.
 */
public class Sdk {
    private static Sdk sCurrentSdk = null;

    private final SdkManager mManager;
    private final VmManager mVmManager;

    private final HashMap<IProject, IAndroidTarget> mProjectMap =
            new HashMap<IProject, IAndroidTarget>();
    private final HashMap<IAndroidTarget, AndroidTargetData> mTargetMap = 
            new HashMap<IAndroidTarget, AndroidTargetData>();
    private final String mDocBaseUrl;

    
    /**
     * Loads an SDK and returns an {@link Sdk} object if success.
     * @param sdkLocation the OS path to the SDK.
     */
    public static Sdk loadSdk(String sdkLocation) {
        if (sCurrentSdk != null) {
            // manual unload?
            sCurrentSdk = null;
        }

        final ArrayList<String> logMessages = new ArrayList<String>();
        ISdkLog log = new ISdkLog() {
            public void error(Throwable throwable, String errorFormat, Object... arg) {
                if (errorFormat != null) {
                    logMessages.add(String.format(errorFormat, arg));
                }
                
                if (throwable != null) {
                    logMessages.add(throwable.getMessage());
                }
            }
            public void warning(String warningFormat, Object... arg) {
                logMessages.add(String.format(warningFormat, arg));
            }
            public void printf(String msgFormat, Object... arg) {
                logMessages.add(String.format(msgFormat, arg));
            }
        };

        // get an SdkManager object for the location
        SdkManager manager = SdkManager.createManager(sdkLocation, log);
        if (manager != null) {
            VmManager vmManager = null;
            try {
                vmManager = new VmManager(manager, log);
            } catch (AndroidLocationException e) {
                log.error(e, "Error parsing the VMs");
            }
            sCurrentSdk = new Sdk(manager, vmManager);
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
     * @param hash the hash
     */
    public IAndroidTarget getTargetFromHashString(String hash) {
        return mManager.getTargetFromHashString(hash);
    }
    
    /**
     * Associates an {@link IProject} and an {@link IAndroidTarget}.
     */
    public void setProject(IProject project, IAndroidTarget target) {
        synchronized (mProjectMap) {
            // look for the current target of the project
            IAndroidTarget previousTarget = mProjectMap.get(project);
            
            if (target != previousTarget) {
                // save the target hash string in the project persistent property
                setProjectTargetHashString(project, target.hashString());
                
                // put it in a local map for easy access.
                mProjectMap.put(project, target);

                // recompile the project if needed.
                IJavaProject javaProject = JavaCore.create(project);
                AndroidClasspathContainerInitializer.updateProjects(
                        new IJavaProject[] { javaProject });
            }
        }
    }
    
    /**
     * Returns the {@link IAndroidTarget} object associated with the given {@link IProject}.
     */
    public IAndroidTarget getTarget(IProject project) {
        synchronized (mProjectMap) {
            IAndroidTarget target = mProjectMap.get(project);
            if (target == null) {
                // get the value from the project persistent property.
                String targetHashString = getProjectTargetHashString(project);

                if (targetHashString != null) {
                    target = mManager.getTargetFromHashString(targetHashString);
                }
            }

            return target;
        }
    }
    
    /**
     * Returns the hash string uniquely identifying the target of a project. This methods reads
     * the string from the project persistent preferences/properties.
     * <p/>The string is equivalent to the return of {@link IAndroidTarget#hashString()}.
     * @param project The project for which to return the target hash string.
     * @return the hash string or null if the project does not have a target set.
     */
    public static String getProjectTargetHashString(IProject project) {
        // load the default.properties from the project folder.
        ProjectProperties properties = ProjectProperties.load(project.getLocation().toOSString(),
                PropertyType.DEFAULT);
        if (properties == null) {
            AdtPlugin.log(IStatus.ERROR, "Failed to load properties file for project '%s'",
                    project.getName());
            return null;
        }
        
        return properties.getProperty(ProjectProperties.PROPERTY_TARGET);
    }

    /**
     * Sets a target hash string in a project's persistent preferences/property storage.
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
        synchronized (mTargetMap) {
            return mTargetMap.get(target);
        }
    }
    
    /**
     * Returns the {@link VmManager}. If the VmManager failed to parse the VM folder, this could
     * be <code>null</code>.
     */
    public VmManager getVmManager() {
        return mVmManager;
    }
    
    private Sdk(SdkManager manager, VmManager vmManager) {
        mManager = manager;
        mVmManager = vmManager;
        
        // pre-compute some paths
        mDocBaseUrl = getDocumentationBaseUrl(mManager.getLocation() +
                SdkConstants.OS_SDK_DOCS_FOLDER);
    }
    
    void setTargetData(IAndroidTarget target, AndroidTargetData data) {
        synchronized (mTargetMap) {
            mTargetMap.put(target, data);
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

}


