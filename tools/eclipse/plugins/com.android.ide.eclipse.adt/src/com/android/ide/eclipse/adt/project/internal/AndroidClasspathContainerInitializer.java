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

package com.android.ide.eclipse.adt.project.internal;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.sdk.LoadStatus;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.IAndroidTarget.IOptionalLibrary;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Classpath container initializer responsible for binding {@link AndroidClasspathContainer} to
 * {@link IProject}s. This removes the hard-coded path to the android.jar.
 */
public class AndroidClasspathContainerInitializer extends ClasspathContainerInitializer {
    /** The container id for the android framework jar file */
    private final static String CONTAINER_ID =
        "com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"; //$NON-NLS-1$

    public AndroidClasspathContainerInitializer() {
        // pass
    }

    /**
     * Binds a classpath container  to a {@link IClasspathContainer} for a given project,
     * or silently fails if unable to do so.
     * @param containerPath the container path that is the container id.
     * @param project the project to bind
     */
    @Override
    public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
        if (CONTAINER_ID.equals(containerPath.toString())) {
            JavaCore.setClasspathContainer(new Path(CONTAINER_ID),
                    new IJavaProject[] { project },
                    new IClasspathContainer[] { allocateAndroidContainer(CONTAINER_ID, project) },
                    new NullProgressMonitor());
        }
    }

    /**
     * Creates a new {@link IClasspathEntry} of type {@link IClasspathEntry#CPE_CONTAINER}
     * linking to the Android Framework.
     */
    public static IClasspathEntry getContainerEntry() {
        return JavaCore.newContainerEntry(new Path(CONTAINER_ID));
    }

    /**
     * Checks the {@link IPath} objects against the android framework container id and
     * returns <code>true</code> if they are identical.
     * @param path the <code>IPath</code> to check.
     */
    public static boolean checkPath(IPath path) {
        return CONTAINER_ID.equals(path.toString());
    }
    
    /**
     * Updates the {@link IJavaProject} objects with new android framework container. This forces
     * JDT to recompile them.
     * @param androidProjects the projects to update.
     * @return <code>true</code> if success, <code>false</code> otherwise.
     */
    public static boolean updateProjects(IJavaProject[] androidProjects) {
        try {
            // Allocate a new AndroidClasspathContainer, and associate it to the android framework 
            // container id for each projects.
            // By providing a new association between a container id and a IClasspathContainer,
            // this forces the JDT to query the IClasspathContainer for new IClasspathEntry (with
            // IClasspathContainer#getClasspathEntries()), and therefore force recompilation of 
            // the projects.
            int projectCount = androidProjects.length;

            IClasspathContainer[] containers = new IClasspathContainer[projectCount];
            for (int i = 0 ; i < projectCount; i++) {
                containers[i] = allocateAndroidContainer(CONTAINER_ID, androidProjects[i]);
            }

            // give each project their new container in one call.
            JavaCore.setClasspathContainer(
                    new Path(CONTAINER_ID),
                    androidProjects, containers, new NullProgressMonitor());
            
            return true;
        } catch (JavaModelException e) {
            return false;
        }
    }

    /**
     * Allocates and returns an {@link AndroidClasspathContainer} object with the proper
     * path to the framework jar file.
     * @param containerId the container id to be used.
     * @param javaProject The java project that will receive the container.
     */
    private static IClasspathContainer allocateAndroidContainer(String containerId,
            IJavaProject javaProject) {
        final IProject iProject = javaProject.getProject();

        // remove potential MARKER_TARGETs.
        try {
            if (iProject.exists()) {
                iProject.deleteMarkers(AdtConstants.MARKER_TARGET, true,
                        IResource.DEPTH_INFINITE);
            }
        } catch (CoreException ce) {
            // just log the error
            AdtPlugin.log(ce, "Error removing target marker.");
        }
        
        // First we check if the SDK has been loaded.
        // By passing the javaProject to getSdkLoadStatus(), we ensure that, should the SDK
        // not be loaded yet, the classpath container will be resolved again once the SDK is loaded.
        boolean sdkIsLoaded = AdtPlugin.getDefault().getSdkLoadStatus(javaProject) ==
            LoadStatus.LOADED;

        // then we check if the project has a valid target.
        IAndroidTarget target = null;
        if (sdkIsLoaded) {
            target = Sdk.getCurrent().getTarget(iProject);
        }

        // if we are loaded and the target is non null, we create a valid ClassPathContainer
        if (sdkIsLoaded && target != null) {
            String targetName = null;
            if (target.isPlatform()) {
                targetName = target.getName();
            } else {
                targetName = String.format("%1$s (%2$s)", target.getName(),
                        target.getApiVersionName());
            }
    
            return new AndroidClasspathContainer(createFrameworkClasspath(target),
                    new Path(containerId), targetName);
        }

        // else we put a marker on the project, and return a dummy container (to replace the
        // previous one if there was one.)
        
        // Get the project's target's hash string (if it exists)
        String hashString = Sdk.getProjectTargetHashString(iProject);
        
        String message = null;
        boolean outputToConsole = true;
        if (hashString == null || hashString.length() == 0) {
            // if there is no hash string we only show this if the SDK is loaded.
            // For a project opened at start-up with no target, this would be displayed twice,
            // once when the project is opened, and once after the SDK has finished loading.
            // By testing the sdk is loaded, we only show this once in the console.
            if (sdkIsLoaded) {
                message = String.format(
                        "Project has no target set. Edit the project properties to set one.");
            }
        } else if (sdkIsLoaded) {
            message = String.format(
                    "Unable to resolve target '%s'", hashString);
        } else {
            // this is the case where there is a hashString but the SDK is not yet
            // loaded and therefore we can't get the target yet.
            message = String.format(
                    "Unable to resolve target '%s' until the SDK is loaded.", hashString);
            
            // let's not log this one to the console as it will happen at every boot,
            // and it's expected. (we do keep the error marker though).
            outputToConsole = false;
        }
        
        if (message != null) {
            // log the error and put the marker on the project if we can.
            if (outputToConsole) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_ALWAYS, iProject, message);
            }
            
            try {
                BaseProjectHelper.addMarker(iProject, AdtConstants.MARKER_TARGET, message, -1,
                        IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH);
            } catch (CoreException e) {
                // In some cases, the workspace may be locked for modification when we pass here.
                // We schedule a new job to put the marker after.
                final String fmessage = message;
                Job markerJob = new Job("Android SDK: Resolving error markers") {
                    @SuppressWarnings("unchecked")
                    @Override
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            BaseProjectHelper.addMarker(iProject, AdtConstants.MARKER_TARGET,
                                    fmessage, -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH);
                        } catch (CoreException e2) {
                            return e2.getStatus();
                        }

                        return Status.OK_STATUS;
                    }
                };

                // build jobs are run after other interactive jobs
                markerJob.setPriority(Job.BUILD);
                markerJob.schedule();
            }
        }

        // return a dummy container to replace the one we may have had before.
        return new IClasspathContainer() {
            public IClasspathEntry[] getClasspathEntries() {
                return new IClasspathEntry[0];
            }

            public String getDescription() {
                return "Unable to get system library for the project";
            }

            public int getKind() {
                return IClasspathContainer.K_DEFAULT_SYSTEM;
            }

            public IPath getPath() {
                return null;
            }
        };
    }

    /**
     * Creates and returns an array of {@link IClasspathEntry} objects for the android
     * framework and optional libraries.
     * <p/>This references the OS path to the android.jar and the
     * java doc directory. This is dynamically created when a project is opened,
     * and never saved in the project itself, so there's no risk of storing an
     * obsolete path.
     * 
     * @param target The target that contains the libraries.
     */
    private static IClasspathEntry[] createFrameworkClasspath(IAndroidTarget target) {
        ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>();
        
        // First, we create the IClasspathEntry for the framework.
        // now add the android framework to the class path.
        // create the path object.
        IPath android_lib = new Path(target.getPath(IAndroidTarget.ANDROID_JAR));
        IPath android_src = new Path(target.getPath(IAndroidTarget.SOURCES));
        
        // create the java doc link.
        IClasspathAttribute cpAttribute = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, AdtPlugin.getUrlDoc());
        
        // create the access rule to restrict access to classes in com.android.internal
        IAccessRule accessRule = JavaCore.newAccessRule(
                new Path("com/android/internal/**"), //$NON-NLS-1$
                IAccessRule.K_NON_ACCESSIBLE);

        IClasspathEntry frameworkClasspathEntry = JavaCore.newLibraryEntry(android_lib,
                android_src, // source attachment path
                null,        // default source attachment root path.
                new IAccessRule[] { accessRule },
                new IClasspathAttribute[] { cpAttribute },
                false // not exported.
                );
        
        list.add(frameworkClasspathEntry);
        
        // now deal with optional libraries
        IOptionalLibrary[] libraries = target.getOptionalLibraries();
        if (libraries != null) {
            HashSet<String> visitedJars = new HashSet<String>();
            for (IOptionalLibrary library : libraries) {
                String jarPath = library.getJarPath();
                if (visitedJars.contains(jarPath) == false) {
                    visitedJars.add(jarPath);

                    // create the java doc link, if needed
                    String targetDocPath = target.getPath(IAndroidTarget.DOCS);
                    IClasspathAttribute[] attributes = null;
                    if (targetDocPath != null) {
                        attributes = new IClasspathAttribute[] {
                                JavaCore.newClasspathAttribute(
                                        IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                                        targetDocPath)
                        };
                    }

                    IClasspathEntry entry = JavaCore.newLibraryEntry(
                            new Path(library.getJarPath()),
                            null, // source attachment path
                            null, // default source attachment root path.
                            null,
                            attributes,
                            false // not exported.
                            );
                    list.add(entry);
                }
            }
        }

        return list.toArray(new IClasspathEntry[list.size()]);
    }
}
