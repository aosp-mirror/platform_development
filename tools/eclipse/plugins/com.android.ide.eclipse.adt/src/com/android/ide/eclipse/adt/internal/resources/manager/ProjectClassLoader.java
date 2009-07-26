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

package com.android.ide.eclipse.adt.internal.resources.manager;

import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * ClassLoader able to load class from output of an Eclipse project.
 */
public final class ProjectClassLoader extends ClassLoader {

    private final IJavaProject mJavaProject;
    private URLClassLoader mJarClassLoader;
    private boolean mInsideJarClassLoader = false;

    public ProjectClassLoader(ClassLoader parentClassLoader, IProject project) {
        super(parentClassLoader);
        mJavaProject = JavaCore.create(project);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            // get the project output folder.
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IPath outputLocation = mJavaProject.getOutputLocation();
            IResource outRes = root.findMember(outputLocation);
            if (outRes == null) {
                throw new ClassNotFoundException(name);
            }

            File outFolder = new File(outRes.getLocation().toOSString());

            // get the class name segments
            String[] segments = name.split("\\."); //$NON-NLS-1$
            
            File classFile = getFile(outFolder, segments, 0);
            if (classFile == null) {
                if (mInsideJarClassLoader == false) {
                    // if no file matching the class name was found, look in the 3rd party jars
                    return loadClassFromJar(name);
                } else {
                    throw new ClassNotFoundException(name);
                }
            }
            
            // load the content of the file and create the class.
            FileInputStream fis = new FileInputStream(classFile);
            byte[] data = new byte[(int)classFile.length()];
            int read = 0;
            try {
                read = fis.read(data);
            } catch (IOException e) {
                data = null;
            }
            fis.close();
            
            if (data != null) {
                Class<?> clazz = defineClass(null, data, 0, read);
                if (clazz != null) {
                    return clazz;
                }
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(e.getMessage());
        }

        throw new ClassNotFoundException(name);
    }
    
    /**
     * Returns the File matching the a certain path from a root {@link File}.
     * <p/>The methods checks that the file ends in .class even though the last segment
     * does not.
     * @param parent the root of the file.
     * @param segments the segments containing the path of the file
     * @param index the offset at which to start looking into segments.
     * @throws FileNotFoundException
     */
    private File getFile(File parent, String[] segments, int index)
            throws FileNotFoundException {
        // reached the end with no match?
        if (index == segments.length) {
            throw new FileNotFoundException();
        }

        String toMatch = segments[index];
        File[] files = parent.listFiles();

        // we're at the last segments. we look for a matching <file>.class
        if (index == segments.length - 1) {
            toMatch = toMatch + ".class"; 

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().equals(toMatch)) {
                        return file;
                    }
                }
            }
            
            // no match? abort.
            throw new FileNotFoundException();
        }
        
        String innerClassName = null;
        
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (toMatch.equals(file.getName())) {
                        return getFile(file, segments, index+1);
                    }
                } else if (file.getName().startsWith(toMatch)) {
                    if (innerClassName == null) {
                        StringBuilder sb = new StringBuilder(segments[index]);
                        for (int i = index + 1 ; i < segments.length ; i++) {
                            sb.append('$');
                            sb.append(segments[i]);
                        }
                        sb.append(".class");
                        
                        innerClassName = sb.toString();
                    }
                    
                    if (file.getName().equals(innerClassName)) {
                        return file;
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Loads a class from the 3rd party jar present in the project
     * @throws ClassNotFoundException
     */
    private Class<?> loadClassFromJar(String name) throws ClassNotFoundException {
        if (mJarClassLoader == null) {
            // get the OS path to all the external jars
            URL[] jars = getExternalJars();
            
            mJarClassLoader = new URLClassLoader(jars, this /* parent */);
        }
        
        try {
            // because a class loader always look in its parent loader first, we need to know
            // that we are querying the jar classloader. This will let us know to not query
            // it again for classes we don't find, or this would create an infinite loop.
            mInsideJarClassLoader = true;
            return mJarClassLoader.loadClass(name);
        } finally {
            mInsideJarClassLoader = false;
        }
    }
    
    /**
     * Returns an array of external jar files used by the project.
     * @return an array of OS-specific absolute file paths
     */
    private final URL[] getExternalJars() {
        // get a java project from it
        IJavaProject javaProject = JavaCore.create(mJavaProject.getProject());
        
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();

        ArrayList<URL> oslibraryList = new ArrayList<URL>();
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_LIBRARY ||
                        e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                    // if this is a classpath variable reference, we resolve it.
                    if (e.getEntryKind() == IClasspathEntry.CPE_VARIABLE) {
                        e = JavaCore.getResolvedClasspathEntry(e); 
                    }

                    // get the IPath
                    IPath path = e.getPath();

                    // check the name ends with .jar
                    if (AndroidConstants.EXT_JAR.equalsIgnoreCase(path.getFileExtension())) {
                        boolean local = false;
                        IResource resource = wsRoot.findMember(path);
                        if (resource != null && resource.exists() &&
                                resource.getType() == IResource.FILE) {
                            local = true;
                            try {
                                oslibraryList.add(
                                        new File(resource.getLocation().toOSString()).toURL());
                            } catch (MalformedURLException mue) {
                                // pass
                            }
                        }

                        if (local == false) {
                            // if the jar path doesn't match a workspace resource,
                            // then we get an OSString and check if this links to a valid file.
                            String osFullPath = path.toOSString();

                            File f = new File(osFullPath);
                            if (f.exists()) {
                                try {
                                    oslibraryList.add(f.toURL());
                                } catch (MalformedURLException mue) {
                                    // pass
                                }
                            }
                        }
                    }
                }
            }
        }

        return oslibraryList.toArray(new URL[oslibraryList.size()]);
    }
}
