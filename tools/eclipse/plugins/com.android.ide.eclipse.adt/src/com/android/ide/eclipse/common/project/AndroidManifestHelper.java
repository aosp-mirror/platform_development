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

package com.android.ide.eclipse.common.project;

import com.android.ide.eclipse.common.AndroidConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

/**
 * Utility class that manages the AndroidManifest.xml file.
 * <p/>
 * All the get method work by XPath. Repeated calls to those may warrant using
 * {@link AndroidManifestParser} instead.
 */
public class AndroidManifestHelper {
    private IFile mManifestIFile;
    private File mManifestFile;
    private XPath mXPath;

    /**
     * Creates an AndroidManifest based on an existing Eclipse {@link IProject} object.
     * </p>
     * Use {@link #exists()} to check if the manifest file really exists in the project.
     *
     * @param project The project to search for the manifest.
     */
    public AndroidManifestHelper(IProject project) {
        mXPath = AndroidXPathFactory.newXPath();
        mManifestIFile = getManifest(project);
    }
    
    /**
     * Creates an AndroidManifest based on a file path.
     * <p/>
     * Use {@link #exists()} to check if the manifest file really exists.
     *
     * @param osManifestFilePath the os path to the AndroidManifest.xml file.
     */
    public AndroidManifestHelper(String osManifestFilePath) {
        mXPath = AndroidXPathFactory.newXPath();
        mManifestFile = new File(osManifestFilePath);
    }


    /**
     * Returns the underlying {@link IFile} for the android manifest XML file, if found in the
     * given Eclipse project.
     *
     * Always return null if the constructor that takes an {@link IProject} was NOT called.
     *
     * @return The IFile for the androidManifest.xml or null if no such file could be found.
     */
    public IFile getManifestIFile() {
        return mManifestIFile;
    }
    
    /**
     * Returns the underlying {@link File} for the android manifest XML file.
     */
    public File getManifestFile() {
        if (mManifestIFile != null) {
            return mManifestIFile.getLocation().toFile();
        }
        
        return mManifestFile;
    }

     /**
     * Returns the package name defined in the manifest file.
     *
     * @return A String object with the package or null if any error happened.
     */
    public String getPackageName() {
        try {
            return getPackageNameInternal(mXPath, getSource());
        } catch (XPathExpressionException e1) {
            // If the XPath failed to evaluate, we'll return null.
        } catch (Exception e) {
            // if this happens this is due to the resource being out of sync.
            // so we must refresh it and do it again

            // for any other kind of exception we must return null as well;
        }

        return null;
    }

    /**
     * Returns the i-th activity defined in the manifest file.
     *
     * @param manifest The manifest's IFile object.
     * @param index The 1-based index of the activity to return.
     * @param xpath An optional xpath object. If null is provided a new one will
     *        be created.
     * @return A String object with the activity or null if any error happened.
     */
    public String getActivityName(int index) {
        try {
            return getActivityNameInternal(index, mXPath, getSource());
        } catch (XPathExpressionException e1) {
            // If the XPath failed to evaluate, we'll return null.
        } catch (Exception e) {
            // if this happens this is due to the resource being out of sync.
            // so we must refresh it and do it again

            // for any other kind of exception we must return null as well;
        }
        return null;
    }

    /**
     * Returns an IFile object representing the manifest for the specified
     * project.
     *
     * @param project The project containing the manifest file.
     * @return An IFile object pointing to the manifest or null if the manifest
     *         is missing.
     */
    public static IFile getManifest(IProject project) {
        IResource r = project.findMember(AndroidConstants.WS_SEP
                + AndroidConstants.FN_ANDROID_MANIFEST);

        if (r == null || r.exists() == false || (r instanceof IFile) == false) {
            return null;
        }
        return (IFile) r;
    }

    /**
     * Combines a java package, with a class value from the manifest to make a fully qualified
     * class name
     * @param javaPackage the java package from the manifest.
     * @param className the class name from the manifest. 
     * @return the fully qualified class name.
     */
    public static String combinePackageAndClassName(String javaPackage, String className) {
        if (className == null || className.length() == 0) {
            return javaPackage;
        }
        if (javaPackage == null || javaPackage.length() == 0) {
            return className;
        }

        // the class name can be a subpackage (starts with a '.'
        // char), a simple class name (no dot), or a full java package
        boolean startWithDot = (className.charAt(0) == '.');
        boolean hasDot = (className.indexOf('.') != -1);
        if (startWithDot || hasDot == false) {

            // add the concatenation of the package and class name
            if (startWithDot) {
                return javaPackage + className;
            } else {
                return javaPackage + '.' + className;
            }
        } else {
            // just add the class as it should be a fully qualified java name.
            return className;
        }
    }
    
    

    /**
     * Returns true either if an androidManifest.xml file was found in the project
     * or if the given file path exists.
     */
    public boolean exists() {
        if (mManifestIFile != null) {
            return mManifestIFile.exists();
        } else if (mManifestFile != null) {
            return mManifestFile.exists();
        }
        
        return false;
    }

    /**
     * Returns an InputSource for XPath.
     *
     * @throws FileNotFoundException if file does not exist.
     * @throws CoreException if the {@link IFile} does not exist.
     */
    private InputSource getSource() throws FileNotFoundException, CoreException {
        if (mManifestIFile != null) {
            return new InputSource(mManifestIFile.getContents());
        } else if (mManifestFile != null) {
            return new InputSource(new FileReader(mManifestFile));
        }
        
        return null;
    }

    /**
     * Performs the actual XPath evaluation to get the package name.
     * Extracted so that we can share it with AndroidManifestFromProject.
     */
    private static String getPackageNameInternal(XPath xpath, InputSource source)
        throws XPathExpressionException {
        return xpath.evaluate("/manifest/@package", source);  //$NON-NLS-1$
    }

    /**
     * Performs the actual XPath evaluation to get the activity name.
     * Extracted so that we can share it with AndroidManifestFromProject.
     */
    private static String getActivityNameInternal(int index, XPath xpath, InputSource source)
        throws XPathExpressionException {
        return xpath.evaluate("/manifest/application/activity[" //$NON-NLS-1$
                              + index
                              + "]/@" //$NON-NLS-1$
                              + AndroidXPathFactory.DEFAULT_NS_PREFIX +":name", //$NON-NLS-1$
                              source);
    }

}
