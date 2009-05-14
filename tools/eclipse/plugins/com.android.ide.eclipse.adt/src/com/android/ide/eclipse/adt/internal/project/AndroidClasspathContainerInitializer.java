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

package com.android.ide.eclipse.adt.internal.project;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.sdk.LoadStatus;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * Classpath container initializer responsible for binding {@link AndroidClasspathContainer} to
 * {@link IProject}s. This removes the hard-coded path to the android.jar.
 */
public class AndroidClasspathContainerInitializer extends ClasspathContainerInitializer {
    /** The container id for the android framework jar file */
    private final static String CONTAINER_ID =
        "com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"; //$NON-NLS-1$

    /** path separator to store multiple paths in a single property. This is guaranteed to not
     * be in a path.
     */
    private final static String PATH_SEPARATOR = "\u001C"; //$NON-NLS-1$

    private final static String PROPERTY_CONTAINER_CACHE = "androidContainerCache"; //$NON-NLS-1$
    private final static String PROPERTY_TARGET_NAME = "androidTargetCache"; //$NON-NLS-1$
    private final static String CACHE_VERSION = "01"; //$NON-NLS-1$
    private final static String CACHE_VERSION_SEP = CACHE_VERSION + PATH_SEPARATOR;
    
    private final static int CACHE_INDEX_JAR = 0;
    private final static int CACHE_INDEX_SRC = 1;
    private final static int CACHE_INDEX_DOCS_URI = 2;
    private final static int CACHE_INDEX_OPT_DOCS_URI = 3;
    private final static int CACHE_INDEX_ADD_ON_START = CACHE_INDEX_OPT_DOCS_URI;
    
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
                    new IClasspathContainer[] { allocateAndroidContainer(project) },
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
                containers[i] = allocateAndroidContainer(androidProjects[i]);
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
     * @param javaProject The java project that will receive the container.
     */
    private static IClasspathContainer allocateAndroidContainer(IJavaProject javaProject) {
        final IProject iProject = javaProject.getProject();

        String markerMessage = null;
        boolean outputToConsole = true;
        
        try {
            AdtPlugin plugin = AdtPlugin.getDefault();
            
            // get the lock object for project manipulation during SDK load.
            Object lock = plugin.getSdkLockObject();
            synchronized (lock) {
                boolean sdkIsLoaded = plugin.getSdkLoadStatus() == LoadStatus.LOADED;
                
                // check if the project has a valid target.
                IAndroidTarget target = null;
                if (sdkIsLoaded) {
                    target = Sdk.getCurrent().getTarget(iProject);
                }

                // if we are loaded and the target is non null, we create a valid ClassPathContainer
                if (sdkIsLoaded && target != null) {
                    
                    String targetName = target.getClasspathName();

                    return new AndroidClasspathContainer(
                            createClasspathEntries(iProject, target, targetName),
                            new Path(CONTAINER_ID), targetName);
                }

                // In case of error, we'll try different thing to provide the best error message
                // possible.
                // Get the project's target's hash string (if it exists)
                String hashString = Sdk.getProjectTargetHashString(iProject);

                if (hashString == null || hashString.length() == 0) {
                    // if there is no hash string we only show this if the SDK is loaded.
                    // For a project opened at start-up with no target, this would be displayed
                    // twice, once when the project is opened, and once after the SDK has
                    // finished loading.
                    // By testing the sdk is loaded, we only show this once in the console.
                    if (sdkIsLoaded) {
                        markerMessage = String.format(
                                "Project has no target set. Edit the project properties to set one.");
                    }
                } else if (sdkIsLoaded) {
                    markerMessage = String.format(
                            "Unable to resolve target '%s'", hashString);
                } else {
                    // this is the case where there is a hashString but the SDK is not yet
                    // loaded and therefore we can't get the target yet.
                    // We check if there is a cache of the needed information.
                    AndroidClasspathContainer container = getContainerFromCache(iProject);
                    
                    if (container == null) {
                        // either the cache was wrong (ie folder does not exists anymore), or 
                        // there was no cache. In this case we need to make sure the project
                        // is resolved again after the SDK is loaded.
                        plugin.setProjectToResolve(javaProject);
                        
                        markerMessage = String.format(
                                "Unable to resolve target '%s' until the SDK is loaded.",
                                hashString);

                        // let's not log this one to the console as it will happen at every boot,
                        // and it's expected. (we do keep the error marker though).
                        outputToConsole = false;

                    } else {
                        // we created a container from the cache, so we register the project
                        // to be checked for cache validity once the SDK is loaded
                        plugin.setProjectToCheck(javaProject);
                        
                        // and return the container
                        return container;
                    }
                    
                }
                
                // return a dummy container to replace the one we may have had before.
                // It'll be replaced by the real when if/when the target is resolved if/when the
                // SDK finishes loading.
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
        } finally {
            if (markerMessage != null) {
                // log the error and put the marker on the project if we can.
                if (outputToConsole) {
                    AdtPlugin.printErrorToConsole(iProject, markerMessage);
                }
                
                try {
                    BaseProjectHelper.addMarker(iProject, AdtConstants.MARKER_TARGET, markerMessage,
                            -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_HIGH);
                } catch (CoreException e) {
                    // In some cases, the workspace may be locked for modification when we
                    // pass here.
                    // We schedule a new job to put the marker after.
                    final String fmessage = markerMessage;
                    Job markerJob = new Job("Android SDK: Resolving error markers") {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                BaseProjectHelper.addMarker(iProject, AdtConstants.MARKER_TARGET,
                                        fmessage, -1, IMarker.SEVERITY_ERROR,
                                        IMarker.PRIORITY_HIGH);
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
            } else {
                // no error, remove potential MARKER_TARGETs.
                try {
                    if (iProject.exists()) {
                        iProject.deleteMarkers(AdtConstants.MARKER_TARGET, true,
                                IResource.DEPTH_INFINITE);
                    }
                } catch (CoreException ce) {
                    // In some cases, the workspace may be locked for modification when we pass
                    // here, so we schedule a new job to put the marker after.
                    Job markerJob = new Job("Android SDK: Resolving error markers") {
                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            try {
                                iProject.deleteMarkers(AdtConstants.MARKER_TARGET, true,
                                        IResource.DEPTH_INFINITE);
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
        }
    }

    /**
     * Creates and returns an array of {@link IClasspathEntry} objects for the android
     * framework and optional libraries.
     * <p/>This references the OS path to the android.jar and the
     * java doc directory. This is dynamically created when a project is opened,
     * and never saved in the project itself, so there's no risk of storing an
     * obsolete path.
     * The method also stores the paths used to create the entries in the project persistent
     * properties. A new {@link AndroidClasspathContainer} can be created from the stored path
     * using the {@link #getContainerFromCache(IProject)} method.
     * @param project 
     * @param target The target that contains the libraries.
     * @param targetName 
     */
    private static IClasspathEntry[] createClasspathEntries(IProject project,
            IAndroidTarget target, String targetName) {
        
        // get the path from the target
        String[] paths = getTargetPaths(target);
        
        // create the classpath entry from the paths
        IClasspathEntry[] entries = createClasspathEntriesFromPaths(paths);
        
        // paths now contains all the path required to recreate the IClasspathEntry with no
        // target info. We encode them in a single string, with each path separated by
        // OS path separator.
        StringBuilder sb = new StringBuilder(CACHE_VERSION);
        for (String p : paths) {
            sb.append(PATH_SEPARATOR);
            sb.append(p);
        }
        
        // store this in a project persistent property
        ProjectHelper.saveStringProperty(project, PROPERTY_CONTAINER_CACHE, sb.toString());
        ProjectHelper.saveStringProperty(project, PROPERTY_TARGET_NAME, targetName);

        return entries;
    }
    
    /**
     * Generates an {@link AndroidClasspathContainer} from the project cache, if possible.
     */
    private static AndroidClasspathContainer getContainerFromCache(IProject project) {
        // get the cached info from the project persistent properties.
        String cache = ProjectHelper.loadStringProperty(project, PROPERTY_CONTAINER_CACHE);
        String targetNameCache = ProjectHelper.loadStringProperty(project, PROPERTY_TARGET_NAME);
        if (cache == null || targetNameCache == null) {
            return null;
        }
        
        // the first 2 chars must match CACHE_VERSION. The 3rd char is the normal separator.
        if (cache.startsWith(CACHE_VERSION_SEP) == false) {
            return null;
        }
        
        cache = cache.substring(CACHE_VERSION_SEP.length());
        
        // the cache contains multiple paths, separated by a character guaranteed to not be in
        // the path (\u001C).
        // The first 3 are for android.jar (jar, source, doc), the rest are for the optional
        // libraries and should contain at least one doc and a jar (if there are any libraries).
        // Therefore, the path count should be 3 or 5+
        String[] paths = cache.split(Pattern.quote(PATH_SEPARATOR));
        if (paths.length < 3 || paths.length == 4) {
            return null;
        }
        
        // now we check the paths actually exist.
        // There's an exception: If the source folder for android.jar does not exist, this is
        // not a problem, so we skip it.
        // Also paths[CACHE_INDEX_DOCS_URI] is a URI to the javadoc, so we test it a
        // bit differently.
        try {
            if (new File(paths[CACHE_INDEX_JAR]).exists() == false ||
                    new File(new URI(paths[CACHE_INDEX_DOCS_URI])).exists() == false) {
                return null;
            }
        
            // check the path for the add-ons, if they exist.
            if (paths.length > CACHE_INDEX_ADD_ON_START) {
                
                // check the docs path separately from the rest of the paths as it's a URI.
                if (new File(new URI(paths[CACHE_INDEX_OPT_DOCS_URI])).exists() == false) {
                    return null;
                }

                // now just check the remaining paths.
                for (int i = CACHE_INDEX_ADD_ON_START + 1; i < paths.length; i++) {
                    String path = paths[i];
                    if (path.length() > 0) {
                        File f = new File(path);
                        if (f.exists() == false) {
                            return null;
                        }
                    }
                }
            }
        } catch (URISyntaxException e) {
            return null;
        }

        IClasspathEntry[] entries = createClasspathEntriesFromPaths(paths);

        return new AndroidClasspathContainer(entries,
                new Path(CONTAINER_ID), targetNameCache);
    }
    
    /**
     * Generates an array of {@link IClasspathEntry} from a set of paths.
     * @see #getTargetPaths(IAndroidTarget)
     */
    private static IClasspathEntry[] createClasspathEntriesFromPaths(String[] paths) {
        ArrayList<IClasspathEntry> list = new ArrayList<IClasspathEntry>();
        
        // First, we create the IClasspathEntry for the framework.
        // now add the android framework to the class path.
        // create the path object.
        IPath android_lib = new Path(paths[CACHE_INDEX_JAR]);
        IPath android_src = new Path(paths[CACHE_INDEX_SRC]);

        // create the java doc link.
        IClasspathAttribute cpAttribute = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                paths[CACHE_INDEX_DOCS_URI]);
        
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
        if (paths.length >= 5) {
            String docPath = paths[CACHE_INDEX_OPT_DOCS_URI];
            int i = 4;
            while (i < paths.length) {
                Path jarPath = new Path(paths[i++]);

                IClasspathAttribute[] attributes = null;
                if (docPath.length() > 0) {
                    attributes = new IClasspathAttribute[] {
                            JavaCore.newClasspathAttribute(
                                    IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                                    docPath)
                    };
                }
    
                IClasspathEntry entry = JavaCore.newLibraryEntry(
                        jarPath,
                        null, // source attachment path
                        null, // default source attachment root path.
                        null,
                        attributes,
                        false // not exported.
                        );
                list.add(entry);
            }
        }
        
        return list.toArray(new IClasspathEntry[list.size()]);
    }

    /**
     * Checks the projects' caches. If the cache was valid, the project is removed from the list.
     * @param projects the list of projects to check.
     */
    public static void checkProjectsCache(ArrayList<IJavaProject> projects) {
        int i = 0;
        projectLoop: while (i < projects.size()) {
            IJavaProject javaProject = projects.get(i);
            IProject iProject = javaProject.getProject();
            
            // check if the project is opened
            if (iProject.isOpen() == false) {
                // remove from the list
                // we do not increment i in this case.
                projects.remove(i);

                continue;
            }

            // get the target from the project and its paths
            IAndroidTarget target = Sdk.getCurrent().getTarget(javaProject.getProject());
            if (target == null) {
                // this is really not supposed to happen. This would mean there are cached paths,
                // but default.properties was deleted. Keep the project in the list to force
                // a resolve which will display the error.
                i++;
                continue;
            }
            
            String[] targetPaths = getTargetPaths(target);
            
            // now get the cached paths
            String cache = ProjectHelper.loadStringProperty(iProject, PROPERTY_CONTAINER_CACHE);
            if (cache == null) {
                // this should not happen. We'll force resolve again anyway.
                i++;
                continue;
            }
            
            String[] cachedPaths = cache.split(Pattern.quote(PATH_SEPARATOR));
            if (cachedPaths.length < 3 || cachedPaths.length == 4) {
                // paths length is wrong. simply resolve the project again
                i++;
                continue;
            }
            
            // Now we compare the paths. The first 4 can be compared directly.
            // because of case sensitiveness we need to use File objects
            
            if (targetPaths.length != cachedPaths.length) {
                // different paths, force resolve again.
                i++;
                continue;
            }
            
            // compare the main paths (android.jar, main sources, main javadoc)
            if (new File(targetPaths[CACHE_INDEX_JAR]).equals(
                            new File(cachedPaths[CACHE_INDEX_JAR])) == false ||
                    new File(targetPaths[CACHE_INDEX_SRC]).equals(
                            new File(cachedPaths[CACHE_INDEX_SRC])) == false ||
                    new File(targetPaths[CACHE_INDEX_DOCS_URI]).equals(
                            new File(cachedPaths[CACHE_INDEX_DOCS_URI])) == false) {
                // different paths, force resolve again.
                i++;
                continue;
            }
            
            if (cachedPaths.length > CACHE_INDEX_OPT_DOCS_URI) {
                // compare optional libraries javadoc
                if (new File(targetPaths[CACHE_INDEX_OPT_DOCS_URI]).equals(
                        new File(cachedPaths[CACHE_INDEX_OPT_DOCS_URI])) == false) {
                    // different paths, force resolve again.
                    i++;
                    continue;
                }
                
                // testing the optional jar files is a little bit trickier.
                // The order is not guaranteed to be identical.
                // From a previous test, we do know however that there is the same number.
                // The number of libraries should be low enough that we can simply go through the
                // lists manually.
                targetLoop: for (int tpi = 4 ; tpi < targetPaths.length; tpi++) {
                    String targetPath = targetPaths[tpi];
                    
                    // look for a match in the other array
                    for (int cpi = 4 ; cpi < cachedPaths.length; cpi++) {
                        if (new File(targetPath).equals(new File(cachedPaths[cpi]))) {
                            // found a match. Try the next targetPath
                            continue targetLoop;
                        }
                    }
                    
                    // if we stop here, we haven't found a match, which means there's a
                    // discrepancy in the libraries. We force a resolve.
                    i++;
                    continue projectLoop;
                }
            }

            // at the point the check passes, and we can remove the project from the list.
            // we do not increment i in this case.
            projects.remove(i);
        }
    }
    
    /**
     * Returns the paths necessary to create the {@link IClasspathEntry} for this targets.
     * <p/>The paths are always in the same order.
     * <ul>
     * <li>Path to android.jar</li>
     * <li>Path to the source code for android.jar</li>
     * <li>Path to the javadoc for the android platform</li>
     * </ul>
     * Additionally, if there are optional libraries, the array will contain:
     * <ul>
     * <li>Path to the librairies javadoc</li>
     * <li>Path to the first .jar file</li>
     * <li>(more .jar as needed)</li>
     * </ul>
     */
    private static String[] getTargetPaths(IAndroidTarget target) {
        ArrayList<String> paths = new ArrayList<String>();
        
        // first, we get the path for android.jar
        // The order is: android.jar, source folder, docs folder
        paths.add(target.getPath(IAndroidTarget.ANDROID_JAR));
        paths.add(target.getPath(IAndroidTarget.SOURCES));
        paths.add(AdtPlugin.getUrlDoc());
        
        // now deal with optional libraries.
        IOptionalLibrary[] libraries = target.getOptionalLibraries();
        if (libraries != null) {
            // all the optional libraries use the same javadoc, so we start with this
            String targetDocPath = target.getPath(IAndroidTarget.DOCS);
            if (targetDocPath != null) {
                paths.add(ProjectHelper.getJavaDocPath(targetDocPath));
            } else {
                // we add an empty string, to always have the same count.
                paths.add("");
            }
            
            // because different libraries could use the same jar file, we make sure we add
            // each jar file only once.
            HashSet<String> visitedJars = new HashSet<String>();
            for (IOptionalLibrary library : libraries) {
                String jarPath = library.getJarPath();
                if (visitedJars.contains(jarPath) == false) {
                    visitedJars.add(jarPath);
                    paths.add(jarPath);
                }
            }
        }

        return paths.toArray(new String[paths.size()]);
    }
}
