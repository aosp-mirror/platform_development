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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.project.ProjectHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Classpath container initializer responsible for binding {@link AndroidClasspathContainer} to
 * {@link IProject}s. This removes the hard-coded path to the android.jar.
 */
public class AndroidClasspathContainerInitializer extends ClasspathContainerInitializer {
    /** The old container id */
    private final static String OLD_CONTAINER_ID =
        "com.android.ide.eclipse.adt.project.AndroidClasspathContainerInitializer"; //$NON-NLS-1$

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
        String id = null;
        if (OLD_CONTAINER_ID.equals(containerPath.toString())) {
            id = OLD_CONTAINER_ID;
        } else if (CONTAINER_ID.equals(containerPath.toString())) {
            id = CONTAINER_ID;
        }
        
        if (id != null) {
            JavaCore.setClasspathContainer(new Path(id),
                    new IJavaProject[] { project },
                    new IClasspathContainer[] { allocateAndroidContainer(id) },
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
     * Checks the {@link IPath} objects against the old android framework container id and
     * returns <code>true</code> if they are identical.
     * @param path the <code>IPath</code> to check.
     */
    public static boolean checkOldPath(IPath path) {
        return OLD_CONTAINER_ID.equals(path.toString());
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
            // because those projects could have the old id, we are going to fix
            // them dynamically here.
            for (IJavaProject javaProject: androidProjects) {
                IClasspathEntry[] entries = javaProject.getRawClasspath();
    
                int containerIndex = ProjectHelper.findClasspathEntryByPath(entries,
                        OLD_CONTAINER_ID,
                        IClasspathEntry.CPE_CONTAINER);
                if (containerIndex != -1) {
                    // the project has the old container, we remove it
                    entries = ProjectHelper.removeEntryFromClasspath(entries, containerIndex);
                    
                    // we add the new one instead
                    entries = ProjectHelper.addEntryToClasspath(entries, getContainerEntry());

                    // and give the new entries to the project
                    javaProject.setRawClasspath(entries, new NullProgressMonitor());
                }
            }
            
            // Allocate a new AndroidClasspathContainer, and associate it to the android framework 
            // container id for each projects.
            // By providing a new association between a container id and a IClasspathContainer,
            // this forces the JDT to query the IClasspathContainer for new IClasspathEntry (with
            // IClasspathContainer#getClasspathEntries()), and therefore force recompilation of 
            // the projects.
            // TODO: We could only do that for the projects haven't fixed above
            // (this isn't something that will happen a lot though)
            int projectCount = androidProjects.length;

            IClasspathContainer[] containers = new IClasspathContainer[projectCount];
            for (int i = 0 ; i < projectCount; i++) {
                containers[i] = allocateAndroidContainer(CONTAINER_ID);
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
     */
    private static IClasspathContainer allocateAndroidContainer(String containerId) {
        return new AndroidClasspathContainer(createFrameworkClasspath(), new Path(containerId));
    }

    /**
     * Creates and returns a new {@link IClasspathEntry} object for the android framework.
     * <p/>This references the OS path to the android.jar and the java doc directory. This is
     * dynamically created when a project is opened, and never saved in the project itself, so
     * there's no risk of storing an obsolete path. 
     */
    private static IClasspathEntry createFrameworkClasspath() {
        // now add the android framework to the class path.
        // create the path object.
        IPath android_lib = new Path(AdtPlugin.getOsAbsoluteFramework());

        IPath android_src = new Path(AdtPlugin.getOsAbsoluteAndroidSources());
        
        // create the java doc link.
        IClasspathAttribute cpAttribute = JavaCore.newClasspathAttribute(
                IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME, AdtPlugin.getUrlDoc());
        
        // create the access rule to restrict access to classes in com.android.internal
        IAccessRule accessRule = JavaCore.newAccessRule(
                new Path("com/android/internal/**"), //$NON-NLS-1$
                IAccessRule.K_NON_ACCESSIBLE);

        IClasspathEntry classpathEntry = JavaCore.newLibraryEntry(android_lib,
                android_src, // source attachment path
                null,        // default source attachment root path.
                new IAccessRule[] { accessRule },
                new IClasspathAttribute[] { cpAttribute },
                false // not exported.
                );

        return classpathEntry;
    }
}
