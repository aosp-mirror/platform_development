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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.XmlErrorHandler.XmlErrorListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.actions.OpenJavaPerspectiveAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import java.util.ArrayList;

/**
 * Utility methods to manipulate projects.
 */
public final class BaseProjectHelper {

    public static final String TEST_CLASS_OK = null;

    /**
     * returns a list of source classpath for a specified project
     * @param javaProject
     * @return a list of path relative to the workspace root.
     */
    public static ArrayList<IPath> getSourceClasspaths(IJavaProject javaProject) {
        ArrayList<IPath> sourceList = new ArrayList<IPath>();
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    sourceList.add(e.getPath());
                }
            }
        }
        return sourceList;
    }

    /**
     * Adds a marker to a file on a specific line. This methods catches thrown
     * {@link CoreException}, and returns null instead.
     * @param file the file to be marked
     * @param markerId The id of the marker to add.
     * @param message the message associated with the mark
     * @param lineNumber the line number where to put the mark. If line is < 1, it puts the marker
     * on line 1.
     * @param severity the severity of the marker.
     * @return the IMarker that was added or null if it failed to add one.
     */
    public final static IMarker addMarker(IResource file, String markerId,
            String message, int lineNumber, int severity) {
        try {
            IMarker marker = file.createMarker(markerId);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.SEVERITY, severity);
            if (lineNumber < 1) {
                lineNumber = 1;
            }
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);

            // on Windows, when adding a marker to a project, it takes a refresh for the marker
            // to show. In order to fix this we're forcing a refresh of elements receiving
            // markers (and only the element, not its children), to force the marker display.
            file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

            return marker;
        } catch (CoreException e) {
            AdtPlugin.log(e, "Failed to add marker '%1$s' to '%2$s'", //$NON-NLS-1$
                    markerId, file.getFullPath());
        }
        
        return null;
    }

    /**
     * Adds a marker to a resource. This methods catches thrown {@link CoreException},
     * and returns null instead.
     * @param resource the file to be marked
     * @param markerId The id of the marker to add.
     * @param message the message associated with the mark
     * @param severity the severity of the marker.
     * @return the IMarker that was added or null if it failed to add one.
     */
    public final static IMarker addMarker(IResource resource, String markerId,
            String message, int severity) {
        try {
            IMarker marker = resource.createMarker(markerId);
            marker.setAttribute(IMarker.MESSAGE, message);
            marker.setAttribute(IMarker.SEVERITY, severity);

            // on Windows, when adding a marker to a project, it takes a refresh for the marker
            // to show. In order to fix this we're forcing a refresh of elements receiving
            // markers (and only the element, not its children), to force the marker display.
            resource.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

            return marker;
        } catch (CoreException e) {
            AdtPlugin.log(e, "Failed to add marker '%1$s' to '%2$s'", //$NON-NLS-1$
                    markerId, resource.getFullPath());
        }
        
        return null;
    }

    /**
     * Adds a marker to a resource. This method does not catch {@link CoreException} and instead
     * throw them.
     * @param resource the file to be marked
     * @param markerId The id of the marker to add.
     * @param message the message associated with the mark
     * @param lineNumber the line number where to put the mark if != -1.
     * @param severity the severity of the marker.
     * @param priority the priority of the marker
     * @return the IMarker that was added.
     * @throws CoreException 
     */
    public final static IMarker addMarker(IResource resource, String markerId,
            String message, int lineNumber, int severity, int priority) throws CoreException {
        IMarker marker = resource.createMarker(markerId);
        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.SEVERITY, severity);
        if (lineNumber != -1) {
            marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
        }
        marker.setAttribute(IMarker.PRIORITY, priority);

        // on Windows, when adding a marker to a project, it takes a refresh for the marker
        // to show. In order to fix this we're forcing a refresh of elements receiving
        // markers (and only the element, not its children), to force the marker display.
        resource.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());

        return marker;
    }

    /**
     * Tests that a class name is valid for usage in the manifest.
     * <p/>
     * This tests the class existence, that it can be instantiated (ie it must not be abstract,
     * nor non static if enclosed), and that it extends the proper super class (not necessarily
     * directly)
     * @param javaProject the {@link IJavaProject} containing the class.
     * @param className the fully qualified name of the class to test.
     * @param superClassName the fully qualified name of the expected super class.
     * @param testVisibility if <code>true</code>, the method will check the visibility of the class
     * or of its constructors.
     * @return {@link #TEST_CLASS_OK} or an error message.
     */
    public final static String testClassForManifest(IJavaProject javaProject, String className,
            String superClassName, boolean testVisibility) {
        try {
            // replace $ by .
            String javaClassName = className.replaceAll("\\$", "\\."); //$NON-NLS-1$ //$NON-NLS-2$

            // look for the IType object for this class
            IType type = javaProject.findType(javaClassName);
            if (type != null && type.exists()) {
                // test that the class is not abstract
                int flags = type.getFlags();
                if (Flags.isAbstract(flags)) {
                    return String.format("%1$s is abstract", className);
                }
                
                // test whether the class is public or not.
                if (testVisibility && Flags.isPublic(flags) == false) {
                    // if its not public, it may have a public default constructor,
                    // which would then be fine.
                    IMethod basicConstructor = type.getMethod(type.getElementName(), new String[0]);
                    if (basicConstructor != null && basicConstructor.exists()) {
                        int constructFlags = basicConstructor.getFlags();
                        if (Flags.isPublic(constructFlags) == false) {
                            return String.format(
                                    "%1$s or its default constructor must be public for the system to be able to instantiate it",
                                    className);
                        }
                    } else {
                        return String.format(
                                "%1$s must be public, or the system will not be able to instantiate it.",
                                className);
                    }
                }

                // If it's enclosed, test that it's static. If its declaring class is enclosed
                // as well, test that it is also static, and public.
                IType declaringType = type;
                do {
                    IType tmpType = declaringType.getDeclaringType();
                    if (tmpType != null) {
                        if (tmpType.exists()) {
                            flags = declaringType.getFlags();
                            if (Flags.isStatic(flags) == false) {
                                return String.format("%1$s is enclosed, but not static",
                                        declaringType.getFullyQualifiedName());
                            }
                            
                            flags = tmpType.getFlags();
                            if (testVisibility && Flags.isPublic(flags) == false) {
                                return String.format("%1$s is not public",
                                        tmpType.getFullyQualifiedName());
                            }
                        } else {
                            // if it doesn't exist, we need to exit so we may as well mark it null.
                            tmpType = null;
                        }
                    }
                    declaringType = tmpType;
                } while (declaringType != null);

                // test the class inherit from the specified super class.
                // get the type hierarchy
                ITypeHierarchy hierarchy = type.newSupertypeHierarchy(new NullProgressMonitor());
                
                // if the super class is not the reference class, it may inherit from
                // it so we get its supertype. At some point it will be null and we
                // will stop
                IType superType = type;
                boolean foundProperSuperClass = false;
                while ((superType = hierarchy.getSuperclass(superType)) != null &&
                        superType.exists()) {
                    if (superClassName.equals(superType.getFullyQualifiedName())) {
                        foundProperSuperClass = true;
                    }
                }
                
                // didn't find the proper superclass? return false.
                if (foundProperSuperClass == false) {
                    return String.format("%1$s does not extend %2$s", className, superClassName);
                }
                
                return TEST_CLASS_OK;
            } else {
                return String.format("Class %1$s does not exist", className);
            }
        } catch (JavaModelException e) {
            return String.format("%1$s: %2$s", className, e.getMessage());
        }
    }
    
    /**
     * Parses the manifest file for errors.
     * <p/>
     * This starts by removing the current XML marker, and then parses the xml for errors, both
     * of XML type and of Android type (checking validity of class files).
     * @param manifestFile
     * @param errorListener
     * @throws CoreException
     */
    public static AndroidManifestParser parseManifestForError(IFile manifestFile,
            XmlErrorListener errorListener) throws CoreException {
        // remove previous markers
        if (manifestFile.exists()) {
            manifestFile.deleteMarkers(AndroidConstants.MARKER_XML, true, IResource.DEPTH_ZERO);
            manifestFile.deleteMarkers(AndroidConstants.MARKER_ANDROID, true, IResource.DEPTH_ZERO);
        }
        
        // and parse
        return AndroidManifestParser.parseForError(
                BaseProjectHelper.getJavaProject(manifestFile.getProject()),
                manifestFile, errorListener);
    }

    /**
     * Returns the {@link IJavaProject} for a {@link IProject} object.
     * <p/>
     * This checks if the project has the Java Nature first.
     * @param project
     * @return the IJavaProject or null if the project couldn't be created or if the project
     * does not have the Java Nature.
     * @throws CoreException
     */
    public static IJavaProject getJavaProject(IProject project) throws CoreException {
        if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
            return JavaCore.create(project);
        }
        return null;
    }
    
    /**
     * Reveals a specific line in the source file defining a specified class,
     * for a specific project.
     * @param project
     * @param className
     * @param line
     */
    public static void revealSource(IProject project, String className, int line) {
        // in case the type is enclosed, we need to replace the $ with .
        className = className.replaceAll("\\$", "\\."); //$NON-NLS-1$ //$NON-NLS2$

        // get the java project
        IJavaProject javaProject = JavaCore.create(project);
        
        try {
            // look for the IType matching the class name.
            IType result = javaProject.findType(className);
            if (result != null && result.exists()) {
                // before we show the type in an editor window, we make sure the current
                // workbench page has an editor area (typically the ddms perspective doesn't).
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
                IWorkbenchPage page = window.getActivePage();
                if (page.isEditorAreaVisible() == false) {
                    // no editor area? we open the java perspective.
                    new OpenJavaPerspectiveAction().run();
                }
                
                IEditorPart editor = JavaUI.openInEditor(result);
                if (editor instanceof ITextEditor) {
                    // get the text editor that was just opened.
                    ITextEditor textEditor = (ITextEditor)editor;
                    
                    IEditorInput input = textEditor.getEditorInput();
                    
                    // get the location of the line to show.
                    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
                    IDocument document = documentProvider.getDocument(input);
                    IRegion lineInfo = document.getLineInformation(line - 1);
                    
                    // select and reveal the line.
                    textEditor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
                }
            }
        } catch (JavaModelException e) {
        } catch (PartInitException e) {
        } catch (BadLocationException e) {
        }
    }
    
    /**
     * Returns the list of android-flagged projects. This list contains projects that are opened
     * in the workspace and that are flagged as android project (through the android nature)
     * @return an array of IJavaProject, which can be empty if no projects match.
     */
    public static IJavaProject[] getAndroidProjects() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);

        return getAndroidProjects(javaModel);
    }

    /**
     * Returns the list of android-flagged projects for the specified java Model.
     * This list contains projects that are opened in the workspace and that are flagged as android
     * project (through the android nature)
     * @param javaModel the Java Model object corresponding for the current workspace root.
     * @return an array of IJavaProject, which can be empty if no projects match.
     */
    public static IJavaProject[] getAndroidProjects(IJavaModel javaModel) {
        // get the java projects
        IJavaProject[] javaProjectList = null;
        try {
            javaProjectList  = javaModel.getJavaProjects();
        }
        catch (JavaModelException jme) {
            return new IJavaProject[0];
        }

        // temp list to build the android project array
        ArrayList<IJavaProject> androidProjectList = new ArrayList<IJavaProject>();

        // loop through the projects and add the android flagged projects to the temp list.
        for (IJavaProject javaProject : javaProjectList) {
            // get the workspace project object
            IProject project = javaProject.getProject();

            // check if it's an android project based on its nature
            try {
                if (project.hasNature(AndroidConstants.NATURE)) {
                    androidProjectList.add(javaProject);
                }
            } catch (CoreException e) {
                // this exception, thrown by IProject.hasNature(), means the project either doesn't
                // exist or isn't opened. So, in any case we just skip it (the exception will
                // bypass the ArrayList.add()
            }
        }

        // return the android projects list.
        return androidProjectList.toArray(new IJavaProject[androidProjectList.size()]);
    }
    
    /**
     * Returns the {@link IFolder} representing the output for the project.
     * <p>
     * The project must be a java project and be opened, or the method will return null.
     * @param project the {@link IProject}
     * @return an IFolder item or null.
     */
    public final static IFolder getOutputFolder(IProject project) {
        try {
            if (project.isOpen() && project.hasNature(JavaCore.NATURE_ID)) {
                // get a java project from the normal project object
                IJavaProject javaProject = JavaCore.create(project);
    
                IPath path = javaProject.getOutputLocation();
                IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
                IResource outputResource = wsRoot.findMember(path);
                if (outputResource != null && outputResource.getType() == IResource.FOLDER) {
                    return (IFolder)outputResource;
                }
            }
        } catch (JavaModelException e) {
            // Let's do nothing and return null
        } catch (CoreException e) {
            // Let's do nothing and return null
        }
        return null;
    }
}
