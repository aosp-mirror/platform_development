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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to manipulate Project parameters/properties.
 */
public final class ProjectHelper {
    public final static int COMPILER_COMPLIANCE_OK = 0;
    public final static int COMPILER_COMPLIANCE_LEVEL = 1;
    public final static int COMPILER_COMPLIANCE_SOURCE = 2;
    public final static int COMPILER_COMPLIANCE_CODEGEN_TARGET = 3;

    /**
     * Adds the corresponding source folder to the class path entries.
     *
     * @param entries The class path entries to read. A copy will be returned.
     * @param new_entry The parent source folder to remove.
     * @return A new class path entries array.
     */
    public static IClasspathEntry[] addEntryToClasspath(
            IClasspathEntry[] entries, IClasspathEntry new_entry) {
        int n = entries.length;
        IClasspathEntry[] newEntries = new IClasspathEntry[n + 1];
        System.arraycopy(entries, 0, newEntries, 0, n);
        newEntries[n] = new_entry;
        return newEntries;
    }

    /**
     * Adds the corresponding source folder to the project's class path entries.
     *
     * @param javaProject The java project of which path entries to update.
     * @param new_entry The parent source folder to remove.
     * @throws JavaModelException 
     */
    public static void addEntryToClasspath(
            IJavaProject javaProject, IClasspathEntry new_entry)
            throws JavaModelException {

        IClasspathEntry[] entries = javaProject.getRawClasspath();
        entries = addEntryToClasspath(entries, new_entry);
        javaProject.setRawClasspath(entries, new NullProgressMonitor());
    }

    /**
     * Remove a classpath entry from the array.
     * @param entries The class path entries to read. A copy will be returned
     * @param index The index to remove.
     * @return A new class path entries array.
     */
    public static IClasspathEntry[] removeEntryFromClasspath(
            IClasspathEntry[] entries, int index) {
        int n = entries.length;
        IClasspathEntry[] newEntries = new IClasspathEntry[n-1];

        // copy the entries before index
        System.arraycopy(entries, 0, newEntries, 0, index);

        // copy the entries after index
        System.arraycopy(entries, index + 1, newEntries, index,
                entries.length - index - 1);

        return newEntries;
    }

    /**
     * Converts a OS specific path into a path valid for the java doc location
     * attributes of a project.
     * @param javaDocOSLocation The OS specific path.
     * @return a valid path for the java doc location.
     */
    public static String getJavaDocPath(String javaDocOSLocation) {
        // first thing we do is convert the \ into /
        String javaDoc = javaDocOSLocation.replaceAll("\\\\", //$NON-NLS-1$
                AndroidConstants.WS_SEP);

        // then we add file: at the beginning for unix path, and file:/ for non
        // unix path
        if (javaDoc.startsWith(AndroidConstants.WS_SEP)) {
            return "file:" + javaDoc; //$NON-NLS-1$
        }

        return "file:/" + javaDoc; //$NON-NLS-1$
    }

    /**
     * Look for a specific classpath entry by full path and return its index.
     * @param entries The entry array to search in.
     * @param entryPath The OS specific path of the entry.
     * @param entryKind The kind of the entry. Accepted values are 0
     * (no filter), IClasspathEntry.CPE_LIBRARY, IClasspathEntry.CPE_PROJECT,
     * IClasspathEntry.CPE_SOURCE, IClasspathEntry.CPE_VARIABLE,
     * and IClasspathEntry.CPE_CONTAINER
     * @return the index of the found classpath entry or -1.
     */
    public static int findClasspathEntryByPath(IClasspathEntry[] entries,
            String entryPath, int entryKind) {
        for (int i = 0 ; i < entries.length ; i++) {
            IClasspathEntry entry = entries[i];

            int kind = entry.getEntryKind();

            if (kind == entryKind || entryKind == 0) {
                // get the path
                IPath path = entry.getPath();

                String osPathString = path.toOSString();
                if (osPathString.equals(entryPath)) {
                    return i;
                }
            }
        }

        // not found, return bad index.
        return -1;
    }

    /**
     * Look for a specific classpath entry for file name only and return its
     *  index.
     * @param entries The entry array to search in.
     * @param entryName The filename of the entry.
     * @param entryKind The kind of the entry. Accepted values are 0
     * (no filter), IClasspathEntry.CPE_LIBRARY, IClasspathEntry.CPE_PROJECT,
     * IClasspathEntry.CPE_SOURCE, IClasspathEntry.CPE_VARIABLE,
     * and IClasspathEntry.CPE_CONTAINER
     * @param startIndex Index where to start the search
     * @return the index of the found classpath entry or -1.
     */
    public static int findClasspathEntryByName(IClasspathEntry[] entries,
            String entryName, int entryKind, int startIndex) {
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int i = startIndex ; i < entries.length ; i++) {
            IClasspathEntry entry = entries[i];

            int kind = entry.getEntryKind();

            if (kind == entryKind || entryKind == 0) {
                // get the path
                IPath path = entry.getPath();
                String name = path.segment(path.segmentCount()-1);

                if (name.equals(entryName)) {
                    return i;
                }
            }
        }

        // not found, return bad index.
        return -1;
    }

    /**
     * Fix the project. This checks the SDK location.
     * @param project The project to fix.
     * @throws JavaModelException
     */
    public static void fixProject(IProject project) throws JavaModelException {
        if (AdtPlugin.getOsSdkFolder().length() == 0) {
            AdtPlugin.printToConsole(project, "Unknown SDK Location, project not fixed.");
            return;
        }
        
        // get a java project
        IJavaProject javaProject = JavaCore.create(project);
        fixProjectClasspathEntries(javaProject);
    }

    /**
     * Fix the project classpath entries. The method ensures that:
     * <ul>
     * <li>The project does not reference any old android.zip/android.jar archive.</li>
     * <li>The project does not use its output folder as a sourc folder.</li>
     * <li>The project does not reference a desktop JRE</li>
     * <li>The project references the AndroidClasspathContainer.
     * </ul>
     * @param javaProject The project to fix.
     * @throws JavaModelException
     */
    public static void fixProjectClasspathEntries(IJavaProject javaProject)
            throws JavaModelException {

        // get the project classpath
        IClasspathEntry[] entries = javaProject.getRawClasspath();
        IClasspathEntry[] oldEntries = entries;

        // check if the JRE is set as library
        int jreIndex = ProjectHelper.findClasspathEntryByPath(entries, JavaRuntime.JRE_CONTAINER,
                IClasspathEntry.CPE_CONTAINER);
        if (jreIndex != -1) {
            // the project has a JRE included, we remove it
            entries = ProjectHelper.removeEntryFromClasspath(entries, jreIndex);
        }

        // get the output folder
        IPath outputFolder = javaProject.getOutputLocation();
        
        boolean foundContainer = false;

        for (int i = 0 ; i < entries.length ;) {
            // get the entry and kind
            IClasspathEntry entry = entries[i];
            int kind = entry.getEntryKind();

            if (kind == IClasspathEntry.CPE_SOURCE) {
                IPath path = entry.getPath();
                
                if (path.equals(outputFolder)) {
                    entries = ProjectHelper.removeEntryFromClasspath(entries, i);
                    
                    // continue, to skip the i++;
                    continue;
                }
            } else if (kind == IClasspathEntry.CPE_CONTAINER) {
                if (AndroidClasspathContainerInitializer.checkPath(entry.getPath())) {
                    foundContainer = true;
                }
            }
            
            i++;
        }

        // if the framework container is not there, we add it
        if (foundContainer == false) {
            // add the android container to the array
            entries = ProjectHelper.addEntryToClasspath(entries,
                    AndroidClasspathContainerInitializer.getContainerEntry());
        }

        // set the new list of entries to the project
        if (entries != oldEntries) {
            javaProject.setRawClasspath(entries, new NullProgressMonitor());
        }

        // If needed, check and fix compiler compliance and source compatibility
        ProjectHelper.checkAndFixCompilerCompliance(javaProject);
    }
    
    
    /**
     * Checks the project compiler compliance level is supported.
     * @param javaProject The project to check
     * @return <ul>
     * <li><code>COMPILER_COMPLIANCE_OK</code> if the project is properly configured</li>
     * <li><code>COMPILER_COMPLIANCE_LEVEL</code> for unsupported compiler level</li>
     * <li><code>COMPILER_COMPLIANCE_SOURCE</code> for unsupported source compatibility</li>
     * <li><code>COMPILER_COMPLIANCE_CODEGEN_TARGET</code> for unsupported .class format</li>
     * </ul>
     */
    public static final int checkCompilerCompliance(IJavaProject javaProject) {
        // get the project compliance level option
        String compliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);

        // check it against a list of valid compliance level strings.
        if (checkCompliance(compliance) == false) {
            // if we didn't find the proper compliance level, we return an error
            return COMPILER_COMPLIANCE_LEVEL;
        }

        // otherwise we check source compatibility
        String source = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);

        // check it against a list of valid compliance level strings.
        if (checkCompliance(source) == false) {
            // if we didn't find the proper compliance level, we return an error
            return COMPILER_COMPLIANCE_SOURCE;
        }

        // otherwise check codegen level
        String codeGen = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

        // check it against a list of valid compliance level strings.
        if (checkCompliance(codeGen) == false) {
            // if we didn't find the proper compliance level, we return an error
            return COMPILER_COMPLIANCE_CODEGEN_TARGET;
        }

        return COMPILER_COMPLIANCE_OK;
    }

    /**
     * Checks the project compiler compliance level is supported.
     * @param project The project to check
     * @return <ul>
     * <li><code>COMPILER_COMPLIANCE_OK</code> if the project is properly configured</li>
     * <li><code>COMPILER_COMPLIANCE_LEVEL</code> for unsupported compiler level</li>
     * <li><code>COMPILER_COMPLIANCE_SOURCE</code> for unsupported source compatibility</li>
     * <li><code>COMPILER_COMPLIANCE_CODEGEN_TARGET</code> for unsupported .class format</li>
     * </ul>
     */
    public static final int checkCompilerCompliance(IProject project) {
        // get the java project from the IProject resource object
        IJavaProject javaProject = JavaCore.create(project);

        // check and return the result.
        return checkCompilerCompliance(javaProject);
    }


    /**
     * Checks, and fixes if needed, the compiler compliance level, and the source compatibility
     * level
     * @param project The project to check and fix.
     */
    public static final void checkAndFixCompilerCompliance(IProject project) {
        // get the java project from the IProject resource object
        IJavaProject javaProject = JavaCore.create(project);

        // Now we check the compiler compliance level and make sure it is valid
        checkAndFixCompilerCompliance(javaProject);
    }

    /**
     * Checks, and fixes if needed, the compiler compliance level, and the source compatibility
     * level
     * @param javaProject The Java project to check and fix.
     */
    public static final void checkAndFixCompilerCompliance(IJavaProject javaProject) {
        if (checkCompilerCompliance(javaProject) != COMPILER_COMPLIANCE_OK) {
            // setup the preferred compiler compliance level.
            javaProject.setOption(JavaCore.COMPILER_COMPLIANCE,
                    AndroidConstants.COMPILER_COMPLIANCE_PREFERRED);
            javaProject.setOption(JavaCore.COMPILER_SOURCE,
                    AndroidConstants.COMPILER_COMPLIANCE_PREFERRED);
            javaProject.setOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM,
                    AndroidConstants.COMPILER_COMPLIANCE_PREFERRED);

            // clean the project to make sure we recompile
            try {
                javaProject.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD,
                        new NullProgressMonitor());
            } catch (CoreException e) {
                AdtPlugin.printErrorToConsole(javaProject.getProject(),
                        "Project compiler settings changed. Clean your project.");
            }
        }
    }

    /**
     * Returns a {@link IProject} by its running application name, as it returned by the AVD.
     * <p/>
     * <var>applicationName</var> will in most case be the package declared in the manifest, but
     * can, in some cases, be a custom process name declared in the manifest, in the
     * <code>application</code>, <code>activity</code>, <code>receiver</code>, or
     * <code>service</code> nodes.
     * @param applicationName The application name.
     * @return a project or <code>null</code> if no matching project were found.
     */
    public static IProject findAndroidProjectByAppName(String applicationName) {
        // Get the list of project for the current workspace
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] projects = workspace.getRoot().getProjects();
        
        // look for a project that matches the packageName of the app
        // we're trying to debug
        for (IProject p : projects) {
            if (p.isOpen()) {
                try {
                    if (p.hasNature(AndroidConstants.NATURE) == false) {
                        // ignore non android projects
                        continue;
                    }
                } catch (CoreException e) {
                    // failed to get the nature? skip project.
                    continue;
                }

                // check that there is indeed a manifest file.
                IFile manifestFile = AndroidManifestParser.getManifest(p);
                if (manifestFile == null) {
                    // no file? skip this project.
                    continue;
                }

                AndroidManifestParser parser = null;
                try {
                    parser = AndroidManifestParser.parseForData(manifestFile);
                } catch (CoreException e) {
                    // ignore, handled below.
                }
                if (parser == null) {
                    // skip this project.
                    continue;
                }

                String manifestPackage = parser.getPackage();

                if (manifestPackage != null && manifestPackage.equals(applicationName)) {
                    // this is the project we were looking for!
                    return p;
                } else {
                    // if the package and application name don't match,
                    // we look for other possible process names declared in the manifest.
                    String[] processes = parser.getProcesses();
                    for (String process : processes) {
                        if (process.equals(applicationName)) {
                            return p;
                        }
                    }
                }
            }
        }

        return null;

    }

    public static void fixProjectNatureOrder(IProject project) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();
        
        // if the android nature is not the first one, we reorder them
        if (AndroidConstants.NATURE.equals(natures[0]) == false) {
            // look for the index
            for (int i = 0 ; i < natures.length ; i++) {
                if (AndroidConstants.NATURE.equals(natures[i])) {
                    // if we try to just reorder the array in one pass, this doesn't do 
                    // anything. I guess JDT check that we are actually adding/removing nature.
                    // So, first we'll remove the android nature, and then add it back.

                    // remove the android nature
                    removeNature(project, AndroidConstants.NATURE);
                    
                    // now add it back at the first index.
                    description = project.getDescription();
                    natures = description.getNatureIds();
                    
                    String[] newNatures = new String[natures.length + 1];

                    // first one is android
                    newNatures[0] = AndroidConstants.NATURE;
                    
                    // next the rest that was before the android nature
                    System.arraycopy(natures, 0, newNatures, 1, natures.length);
                    
                    // set the new natures
                    description.setNatureIds(newNatures);
                    project.setDescription(description, null);

                    // and stop
                    break;
                }
            }
        }
    }


    /**
     * Removes a specific nature from a project.
     * @param project The project to remove the nature from.
     * @param nature The nature id to remove.
     * @throws CoreException
     */
    public static void removeNature(IProject project, String nature) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] natures = description.getNatureIds();

        // check if the project already has the android nature.
        for (int i = 0; i < natures.length; ++i) {
            if (nature.equals(natures[i])) {
                String[] newNatures = new String[natures.length - 1];
                if (i > 0) {
                    System.arraycopy(natures, 0, newNatures, 0, i);
                }
                System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
                description.setNatureIds(newNatures);
                project.setDescription(description, null);

                return;
            }
        }

    }

    /**
     * Returns if the project has error level markers.
     * @param includeReferencedProjects flag to also test the referenced projects.
     * @throws CoreException
     */
    public static boolean hasError(IProject project, boolean includeReferencedProjects)
    throws CoreException {
        IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
        if (markers != null && markers.length > 0) {
            // the project has marker(s). even though they are "problem" we
            // don't know their severity. so we loop on them and figure if they
            // are warnings or errors
            for (IMarker m : markers) {
                int s = m.getAttribute(IMarker.SEVERITY, -1);
                if (s == IMarker.SEVERITY_ERROR) {
                    return true;
                }
            }
        }
        
        // test the referenced projects if needed.
        if (includeReferencedProjects) {
            IProject[] projects = getReferencedProjects(project);
            
            for (IProject p : projects) {
                if (hasError(p, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Saves a String property into the persistent storage of a resource.
     * @param resource The resource into which the string value is saved.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    public static boolean saveStringProperty(IResource resource, String propertyName,
            String value) {
        QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID, propertyName);

        try {
            resource.setPersistentProperty(qname, value);
        } catch (CoreException e) {
            return false;
        }

        return true;
    }

    /**
     * Loads a String property from the persistent storage of a resource.
     * @param resource The resource from which the string value is loaded.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @return the property value or null if it was not found.
     */
    public static String loadStringProperty(IResource resource, String propertyName) {
        QualifiedName qname = new QualifiedName(AdtPlugin.PLUGIN_ID, propertyName);

        try {
            String value = resource.getPersistentProperty(qname);
            return value;
        } catch (CoreException e) {
            return null;
        }
    }

    /**
     * Saves a property into the persistent storage of a resource.
     * @param resource The resource into which the boolean value is saved.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value the value to save
     * @return true if the save succeeded.
     */
    public static boolean saveBooleanProperty(IResource resource, String propertyName,
            boolean value) {
        return saveStringProperty(resource, propertyName, Boolean.toString(value));
    }

    /**
     * Loads a boolean property from the persistent storage of the project.
     * @param resource The resource from which the boolean value is loaded.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param defaultValue The default value to return if the property was not found.
     * @return the property value or the default value if the property was not found.
     */
    public static boolean loadBooleanProperty(IResource resource, String propertyName,
            boolean defaultValue) {
        String value = loadStringProperty(resource, propertyName);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        return defaultValue;
    }

    /**
     * Saves the path of a resource into the persistent storate of the project.
     * @param resource The resource into which the resource path is saved.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @param value The resource to save. It's its path that is actually stored. If null, an
     *      empty string is stored.
     * @return true if the save succeeded
     */
    public static boolean saveResourceProperty(IResource resource, String propertyName,
            IResource value) {
        if (value != null) {
            IPath iPath = value.getProjectRelativePath();
            return saveStringProperty(resource, propertyName, iPath.toString());
        }

        return saveStringProperty(resource, propertyName, ""); //$NON-NLS-1$
    }

    /**
     * Loads the path of a resource from the persistent storage of the project, and returns the
     * corresponding IResource object, if it exists in the same project as <code>resource</code>.
     * @param resource The resource from which the resource path is loaded.
     * @param propertyName the name of the property. The id of the plugin is added to this string.
     * @return The corresponding IResource object (or children interface) or null
     */
    public static IResource loadResourceProperty(IResource resource, String propertyName) {
        String value = loadStringProperty(resource, propertyName);

        if (value != null && value.length() > 0) {
            return resource.getProject().findMember(value);
        }

        return null;
    }
    
    /**
     * Returns the list of referenced project that are opened and Java projects.
     * @param project
     * @return list of opened referenced java project.
     * @throws CoreException
     */
    public static IProject[] getReferencedProjects(IProject project) throws CoreException {
        IProject[] projects = project.getReferencedProjects();
        
        ArrayList<IProject> list = new ArrayList<IProject>();
        
        for (IProject p : projects) {
            if (p.isOpen() && p.hasNature(JavaCore.NATURE_ID)) {
                list.add(p);
            }
        }

        return list.toArray(new IProject[list.size()]);
    }


    /**
     * Checks a Java project compiler level option against a list of supported versions.
     * @param optionValue the Compiler level option.
     * @return true if the option value is supproted.
     */
    private static boolean checkCompliance(String optionValue) {
        for (String s : AndroidConstants.COMPILER_COMPLIANCE) {
            if (s != null && s.equals(optionValue)) {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Returns the apk filename for the given project
     * @param project The project.
     * @param config An optional config name. Can be null.
     */
    public static String getApkFilename(IProject project, String config) {
        if (config != null) {
            return project.getName() + "-" + config + AndroidConstants.DOT_ANDROID_PACKAGE; //$NON-NLS-1$ 
        }
        
        return project.getName() + AndroidConstants.DOT_ANDROID_PACKAGE;
    }

    /**
     * Find the list of projects on which this JavaProject is dependent on at the compilation level.
     * 
     * @param javaProject Java project that we are looking for the dependencies.
     * @return A list of Java projects for which javaProject depend on.
     * @throws JavaModelException
     */
    public static List<IJavaProject> getAndroidProjectDependencies(IJavaProject javaProject) 
        throws JavaModelException {
        String[] requiredProjectNames = javaProject.getRequiredProjectNames();
    
        // Go from java project name to JavaProject name
        IJavaModel javaModel = javaProject.getJavaModel();
    
        // loop through all dependent projects and keep only those that are Android projects
        List<IJavaProject> projectList = new ArrayList<IJavaProject>(requiredProjectNames.length);
        for (String javaProjectName : requiredProjectNames) {
            IJavaProject androidJavaProject = javaModel.getJavaProject(javaProjectName);
            
            //Verify that the project has also the Android Nature
            try {
                if (!androidJavaProject.getProject().hasNature(AndroidConstants.NATURE)) {
                    continue;
                }
            } catch (CoreException e) {
                continue;
            }
            
            projectList.add(androidJavaProject);
        }
        
        return projectList;
    }

    /**
     * Returns the android package file as an IFile object for the specified
     * project.
     * @param project The project
     * @return The android package as an IFile object or null if not found.
     */
    public static IFile getApplicationPackage(IProject project) {
        // get the output folder
        IFolder outputLocation = BaseProjectHelper.getOutputFolder(project);
    
        if (outputLocation == null) {
            AdtPlugin.printErrorToConsole(project,
                    "Failed to get the output location of the project. Check build path properties"
                    );
            return null;
        }
        
    
        // get the package path
        String packageName = project.getName() + AndroidConstants.DOT_ANDROID_PACKAGE;
        IResource r = outputLocation.findMember(packageName);
    
        // check the package is present
        if (r instanceof IFile && r.exists()) {
            return (IFile)r;
        }
    
        String msg = String.format("Could not find %1$s!", packageName);
        AdtPlugin.printErrorToConsole(project, msg);
    
        return null;
    }
}
