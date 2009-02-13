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

package com.android.ide.eclipse.adt.build;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.project.FixLaunchConfig;
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestHelper;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.ide.eclipse.common.project.XmlErrorHandler.BasicXmlErrorListener;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre Java Compiler.
 * This incremental builder performs 2 tasks:
 * <ul>
 * <li>compiles the resources located in the res/ folder, along with the
 * AndroidManifest.xml file into the R.java class.</li>
 * <li>compiles any .aidl files into a corresponding java file.</li>
 * </ul>
 *
 */
public class PreCompilerBuilder extends BaseBuilder {

    public static final String ID = "com.android.ide.eclipse.adt.PreCompilerBuilder"; //$NON-NLS-1$

    private static final String PROPERTY_PACKAGE = "manifestPackage"; //$NON-NLS-1$

    private static final String PROPERTY_SOURCE_FOLDER =
        "manifestPackageSourceFolder"; //$NON-NLS-1$

    private static final String PROPERTY_COMPILE_RESOURCES = "compileResources"; //$NON-NLS-1$

    static final String PROPERTY_ANDROID_GENERATED = "androidGenerated"; //$NON-NLS-1$
    static final String PROPERTY_ANDROID_CONFLICT = "androidConflict"; //$NON-NLS-1$

    /**
     * Single line aidl error<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     */
    private static Pattern sAidlPattern1 = Pattern.compile("^(.+?):(\\d+):\\s(.+)$"); //$NON-NLS-1$

    /**
     * Compile flag. This is set to true if one of the changed/added/removed
     * file is a resource file. Upon visiting all the delta resources, if
     * this flag is true, then we know we'll have to compile the resources
     * into R.java
     */
    private boolean mCompileResources = false;

    /** List of .aidl files found that are modified or new. */
    private final ArrayList<IFile> mAidlToCompile = new ArrayList<IFile>();

    /** List of .aidl files that have been removed. */
    private final ArrayList<IFile> mAidlToRemove = new ArrayList<IFile>();

    /** cache of the java package defined in the manifest */
    private String mManifestPackage;

    /** Source folder containing the java package defined in the manifest. */
    private IFolder mManifestPackageSourceFolder;

    /**
     * Progress monitor waiting the end of the process to set a persistent value
     * in a file. This is typically used in conjunction with <code>IResource.refresh()</code>,
     * since this call is asysnchronous, and we need to wait for it to finish for the file
     * to be known by eclipse, before we can call <code>resource.setPersistentProperty</code> on
     * a new file.
     */
    private static class RefreshProgressMonitor implements IProgressMonitor {
        private boolean mCancelled = false;
        private IFile mNewFile;
        private IFile mSource;
        private boolean mDoneExecuted = false;
        public RefreshProgressMonitor(IFile newFile, IFile source) {
            mNewFile = newFile;
            mSource = source;
        }

        public void beginTask(String name, int totalWork) {
        }

        public void done() {
            if (mDoneExecuted == false) {
                mDoneExecuted = true;
                if (mNewFile.exists()) {
                    ProjectHelper.saveResourceProperty(mNewFile, PROPERTY_ANDROID_GENERATED,
                            mSource);
                    try {
                        mNewFile.setDerived(true);
                    } catch (CoreException e) {
                        // This really shouldn't happen since we check that the resource exist.
                        // Worst case scenario, the resource isn't marked as derived.
                    }
                }
            }
        }

        public void internalWorked(double work) {
        }

        public boolean isCanceled() {
            return mCancelled;
        }

        public void setCanceled(boolean value) {
            mCancelled = value;
        }

        public void setTaskName(String name) {
        }

        public void subTask(String name) {
        }

        public void worked(int work) {
        }
    }

    /**
     * Progress Monitor setting up to two files as derived once their parent is refreshed.
     * This is used as ProgressMonitor to refresh the R.java/Manifest.java parent (to display
     * the newly created files in the package explorer).
     */
    private static class DerivedProgressMonitor implements IProgressMonitor {
        private boolean mCancelled = false;
        private IFile mFile1;
        private IFile mFile2;
        private boolean mDoneExecuted = false;
        public DerivedProgressMonitor(IFile file1, IFile file2) {
            mFile1 = file1;
            mFile2 = file2;
        }

        public void beginTask(String name, int totalWork) {
        }

        public void done() {
            if (mDoneExecuted == false) {
                if (mFile1 != null && mFile1.exists()) {
                    mDoneExecuted = true;
                    try {
                        mFile1.setDerived(true);
                    } catch (CoreException e) {
                        // This really shouldn't happen since we check that the resource edit.
                        // Worst case scenario, the resource isn't marked as derived.
                    }
                }
                if (mFile2 != null && mFile2.exists()) {
                    try {
                        mFile2.setDerived(true);
                    } catch (CoreException e) {
                        // This really shouldn't happen since we check that the resource edit.
                        // Worst case scenario, the resource isn't marked as derived.
                    }
                }
            }
        }

        public void internalWorked(double work) {
        }

        public boolean isCanceled() {
            return mCancelled;
        }

        public void setCanceled(boolean value) {
            mCancelled = value;
        }

        public void setTaskName(String name) {
        }

        public void subTask(String name) {
        }

        public void worked(int work) {
        }
    }

    public PreCompilerBuilder() {
        super();
    }
    
    // build() returns a list of project from which this project depends for future compilation.
    @SuppressWarnings("unchecked") //$NON-NLS-1$
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        // First thing we do is go through the resource delta to not
        // lose it if we have to abort the build for any reason.

        // get the project objects
        IProject project = getProject();
        IJavaProject javaProject = JavaCore.create(project);

        // now we need to get the classpath list
        ArrayList<IPath> sourceList = BaseProjectHelper.getSourceClasspaths(javaProject);

        PreCompilerDeltaVisitor dv = null;
        String javaPackage = null;

        if (kind == FULL_BUILD) {
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.Start_Full_Pre_Compiler);
            mCompileResources = true;
            buildAidlCompilationList(project, sourceList);
        } else {
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.Start_Inc_Pre_Compiler);

            // Go through the resources and see if something changed.
            // Even if the mCompileResources flag is true from a previously aborted
            // build, we need to go through the Resource delta to get a possible
            // list of aidl files to compile/remove.
            IResourceDelta delta = getDelta(project);
            if (delta == null) {
                mCompileResources = true;
                buildAidlCompilationList(project, sourceList);
            } else {
                dv = new PreCompilerDeltaVisitor(this, sourceList);
                delta.accept(dv);

                // record the state
                mCompileResources |= dv.getCompileResources();
                
                // handle aidl modification
                if (dv.getFullAidlRecompilation()) {
                    buildAidlCompilationList(project, sourceList);
                } else {
                    mergeAidlFileModifications(dv.getAidlToCompile(),
                            dv.getAidlToRemove());
                }
                
                // get the java package from the visitor
                javaPackage = dv.getManifestPackage();
            }
        }

        // store the build status in the persistent storage
        saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES , mCompileResources);
        // TODO also needs to store the list of aidl to compile/remove

        // At this point we have stored what needs to be build, so we can
        // do some high level test and abort if needed.
        abortOnBadSetup(project);
        
        // if there was some XML errors, we just return w/o doing
        // anything since we've put some markers in the files anyway.
        if (dv != null && dv.mXmlError) {
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.Xml_Error);

            // This interrupts the build. The next builders will not run.
            stopBuild(Messages.Xml_Error);
        }


        // get the manifest file
        IFile manifest = AndroidManifestHelper.getManifest(project);

        if (manifest == null) {
            String msg = String.format(Messages.s_File_Missing,
                    AndroidConstants.FN_ANDROID_MANIFEST);
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project, msg);
            markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

            // This interrupts the build. The next builders will not run.
            stopBuild(msg);
        }

        // lets check the XML of the manifest first, if that hasn't been done by the
        // resource delta visitor yet.
        if (dv == null || dv.getCheckedManifestXml() == false) {
            BasicXmlErrorListener errorListener = new BasicXmlErrorListener();
            AndroidManifestParser parser = BaseProjectHelper.parseManifestForError(manifest,
                    errorListener);
            
            if (errorListener.mHasXmlError == true) {
                // there was an error in the manifest, its file has been marked,
                // by the XmlErrorHandler.
                // We return;
                String msg = String.format(Messages.s_Contains_Xml_Error,
                        AndroidConstants.FN_ANDROID_MANIFEST);
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project, msg);

                // This interrupts the build. The next builders will not run.
                stopBuild(msg);
            }
            
            // get the java package from the parser
            javaPackage = parser.getPackage();
        }

        if (javaPackage == null || javaPackage.length() == 0) {
            // looks like the AndroidManifest file isn't valid.
            String msg = String.format(Messages.s_Doesnt_Declare_Package_Error,
                    AndroidConstants.FN_ANDROID_MANIFEST);
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    msg);

            // This interrupts the build. The next builders will not run.
            stopBuild(msg);
        }

        // at this point we have the java package. We need to make sure it's not a different package
        // than the previous one that were built.
        if (javaPackage.equals(mManifestPackage) == false) {
            // The manifest package has changed, the user may want to update
            // the launch configuration
            if (mManifestPackage != null) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        Messages.Checking_Package_Change);

                FixLaunchConfig flc = new FixLaunchConfig(project, mManifestPackage, javaPackage);
                flc.start();
            }

            // now we delete the generated classes from their previous location
            deleteObsoleteGeneratedClass(AndroidConstants.FN_RESOURCE_CLASS,
                    mManifestPackageSourceFolder, mManifestPackage);
            deleteObsoleteGeneratedClass(AndroidConstants.FN_MANIFEST_CLASS,
                    mManifestPackageSourceFolder, mManifestPackage);

            // record the new manifest package, and save it.
            mManifestPackage = javaPackage;
            saveProjectStringProperty(PROPERTY_PACKAGE, mManifestPackage);
        }

        if (mCompileResources) {
            // we need to figure out where to store the R class.
            // get the parent folder for R.java and update mManifestPackageSourceFolder
            IFolder packageFolder = getManifestPackageFolder(project, sourceList);

            // at this point, either we have found the package or not.
            // if we haven't well it's time to tell the user and abort
            if (mManifestPackageSourceFolder == null) {
                // mark the manifest file
                String message = String.format(Messages.Package_s_Doesnt_Exist_Error,
                        mManifestPackage);
                BaseProjectHelper.addMarker(manifest, AndroidConstants.MARKER_AAPT, message,
                        IMarker.SEVERITY_ERROR);
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project, message);

                // abort
                // This interrupts the build. The next builders will not run.
                stopBuild(message);
            }


            // found the folder in which to write the stuff

            // get the resource folder
            IFolder resFolder = project.getFolder(AndroidConstants.WS_RESOURCES);

            // get the file system path
            IPath outputLocation = mManifestPackageSourceFolder.getLocation();
            IPath resLocation = resFolder.getLocation();
            IPath manifestLocation = manifest.getLocation();

            // those locations have to exist for us to do something!
            if (outputLocation != null && resLocation != null
                    && manifestLocation != null) {
                String osOutputPath = outputLocation.toOSString();
                String osResPath = resLocation.toOSString();
                String osManifestPath = manifestLocation.toOSString();

                IAndroidTarget projectTarget = Sdk.getCurrent().getTarget(project);

                // remove the aapt markers
                removeMarkersFromFile(manifest, AndroidConstants.MARKER_AAPT);
                removeMarkersFromContainer(resFolder, AndroidConstants.MARKER_AAPT);

                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        Messages.Preparing_Generated_Files);

                // since the R.java file may be already existing in read-only
                // mode we need to make it readable so that aapt can overwrite
                // it
                IFile rJavaFile = packageFolder.getFile(AndroidConstants.FN_RESOURCE_CLASS);
                prepareFileForExternalModification(rJavaFile);

                // do the same for the Manifest.java class
                IFile manifestJavaFile = packageFolder.getFile(AndroidConstants.FN_MANIFEST_CLASS);
                prepareFileForExternalModification(manifestJavaFile);

                // we actually need to delete the manifest.java as it may become empty and in this
                // case aapt doesn't generate an empty one, but instead doesn't touch it.
                manifestJavaFile.delete(true, null);

                // launch aapt: create the command line
                ArrayList<String> array = new ArrayList<String>();
                array.add(AdtPlugin.getOsAbsoluteAapt());
                array.add("package"); //$NON-NLS-1$
                array.add("-m"); //$NON-NLS-1$
                if (AdtPlugin.getBuildVerbosity() == AdtConstants.BUILD_VERBOSE) {
                    array.add("-v"); //$NON-NLS-1$
                }
                array.add("-J"); //$NON-NLS-1$
                array.add(osOutputPath);
                array.add("-M"); //$NON-NLS-1$
                array.add(osManifestPath);
                array.add("-S"); //$NON-NLS-1$
                array.add(osResPath);
                array.add("-I"); //$NON-NLS-1$
                array.add(projectTarget.getPath(IAndroidTarget.ANDROID_JAR));

                if (AdtPlugin.getBuildVerbosity() == AdtConstants.BUILD_VERBOSE) {
                    StringBuilder sb = new StringBuilder();
                    for (String c : array) {
                        sb.append(c);
                        sb.append(' ');
                    }
                    String cmd_line = sb.toString();
                    AdtPlugin.printToConsole(project, cmd_line);
                }

                // launch
                int execError = 1;
                try {
                    // launch the command line process
                    Process process = Runtime.getRuntime().exec(
                            array.toArray(new String[array.size()]));

                    // list to store each line of stderr
                    ArrayList<String> results = new ArrayList<String>();

                    // get the output and return code from the process
                    execError = grabProcessOutput(process, results);

                    // attempt to parse the error output
                    boolean parsingError = parseAaptOutput(results, project);

                    // if we couldn't parse the output we display it in the console.
                    if (parsingError) {
                        if (execError != 0) {
                            AdtPlugin.printErrorToConsole(project, results.toArray());
                        } else {
                            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_NORMAL,
                                    project, results.toArray());
                        }
                    }

                    if (execError != 0) {
                        // if the exec failed, and we couldn't parse the error output (and therefore
                        // not all files that should have been marked, were marked), we put a
                        // generic marker on the project and abort.
                        if (parsingError) {
                            markProject(AdtConstants.MARKER_ADT, Messages.Unparsed_AAPT_Errors,
                                    IMarker.SEVERITY_ERROR);
                        }

                        AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                                Messages.AAPT_Error);

                        // abort if exec failed.
                        // This interrupts the build. The next builders will not run.
                        stopBuild(Messages.AAPT_Error);
                    }
                } catch (IOException e1) {
                    // something happen while executing the process,
                    // mark the project and exit
                    String msg = String.format(Messages.AAPT_Exec_Error, array.get(0));
                    markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                    // This interrupts the build. The next builders will not run.
                    stopBuild(msg);
                } catch (InterruptedException e) {
                    // we got interrupted waiting for the process to end...
                    // mark the project and exit
                    String msg = String.format(Messages.AAPT_Exec_Error, array.get(0));
                    markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                    // This interrupts the build. The next builders will not run.
                    stopBuild(msg);
                }

                // if the return code was OK, we refresh the folder that
                // contains R.java to force a java recompile.
                if (execError == 0) {
                    // now set the R.java/Manifest.java file as read only.
                    finishJavaFilesAfterExternalModification(rJavaFile, manifestJavaFile);

                    // build has been done. reset the state of the builder
                    mCompileResources = false;

                    // and store it
                    saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, mCompileResources);
                }
            }
        } else {
            // nothing to do
        }

        // now handle the aidl stuff.
        // look for a preprocessed aidl file
        IResource projectAidl = project.findMember("project.aidl"); //$NON-NLS-1$
        String folderAidlPath = null;
        if (projectAidl != null && projectAidl.exists()) {
            folderAidlPath = projectAidl.getLocation().toOSString();
        }
        boolean aidlStatus = handleAidl(sourceList, folderAidlPath, monitor);

        if (aidlStatus == false && mCompileResources == false) {
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.Nothing_To_Compile);
        }

        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);

        AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, getProject(),
                Messages.Removing_Generated_Classes);

        // check if we have the R.java info already.
        if (mManifestPackageSourceFolder != null && mManifestPackage != null) {
            deleteObsoleteGeneratedClass(AndroidConstants.FN_RESOURCE_CLASS,
                    mManifestPackageSourceFolder, mManifestPackage);
            deleteObsoleteGeneratedClass(AndroidConstants.FN_MANIFEST_CLASS,
                    mManifestPackageSourceFolder, mManifestPackage);
        }
        
        // FIXME: delete all java generated from aidl.
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();

        // load the previous IFolder and java package.
        mManifestPackage = loadProjectStringProperty(PROPERTY_PACKAGE);
        IResource resource = loadProjectResourceProperty(PROPERTY_SOURCE_FOLDER);
        if (resource instanceof IFolder) {
            mManifestPackageSourceFolder = (IFolder)resource;
        }

        // Load the current compile flag. We ask for true if not found to force a
        // recompile.
        mCompileResources = loadProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, true);
    }

    /**
     * Delete the a generated java class associated with the specified java package.
     * @param filename Name of the generated file to remove.
     * @param sourceFolder The source Folder containing the old java package.
     * @param javaPackage the old java package
     */
    private void deleteObsoleteGeneratedClass(String filename, IFolder sourceFolder,
            String javaPackage) {
        if (sourceFolder == null || javaPackage == null) {
            return;
        }

        // convert the java package into path
        String[] segments = javaPackage.split(AndroidConstants.RE_DOT);

        StringBuilder path = new StringBuilder();
        for (String s : segments) {
           path.append(AndroidConstants.WS_SEP_CHAR);
           path.append(s);
        }

        // appends the name of the generated file
        path.append(AndroidConstants.WS_SEP_CHAR);
        path.append(filename);

        Path iPath = new Path(path.toString());

        // Find a matching resource object.
        IResource javaFile = sourceFolder.findMember(iPath);
        if (javaFile != null && javaFile.exists() && javaFile.getType() == IResource.FILE) {
            try {
                // remove the read-only tag
                prepareFileForExternalModification((IFile)javaFile);

                // delete
                javaFile.delete(true, null);

                // refresh parent
                javaFile.getParent().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

            } catch (CoreException e) {
                // failed to delete it, the user will have to delete it manually.
                String message = String.format(Messages.Delete_Obsolete_Error, path);
                IProject project = getProject();
                AdtPlugin.printErrorToConsole(project, message);
                AdtPlugin.printErrorToConsole(project, e.getMessage());
            }
        }
    }

    /**
     * Looks for the folder containing the package defined in the manifest. It looks in the
     * list of source folders for the one containing folders matching the package defined in the
     * manifest (from the field <code>mManifestPackage</code>). It returns the final folder, which
     * will contain the R class, and update the field <code>mManifestPackageSourceFolder</code>
     * to be the source folder containing the full package.
     * @param project The project.
     * @param sourceList The list of source folders for the project.
     * @return the package that will contain the R class or null if the folder was not found.
     * @throws CoreException
     */
    private IFolder getManifestPackageFolder(IProject project, ArrayList<IPath> sourceList)
            throws CoreException {
        // split the package in segments
        String[] packageSegments = mManifestPackage.split(AndroidConstants.RE_DOT);

        // we look for 2 folders.
        // 1. The source folder that contains the full java package.
        // we will store the folder in the field mJavaSourceFolder, for reuse during
        IFolder manifestPackageSourceFolder = null;
        // subsequent builds. This is the folder we will give to aapt.
        // 2. The folder actually containing the R.java files. We need this one to do a refresh
        IFolder packageFolder = null;

        for (IPath iPath : sourceList) {
            int packageSegmentIndex = 0;

            // the path is relative to the workspace. We ignore the first segment,
            // when getting the resource from the IProject object.
            IResource classpathEntry = project.getFolder(iPath.removeFirstSegments(1));

            if (classpathEntry instanceof IFolder) {
                IFolder classpathFolder = (IFolder)classpathEntry;
                IFolder folder = classpathFolder;

                boolean failed = false;
                while (failed == false
                        && packageSegmentIndex < packageSegments.length) {

                    // loop on that folder content looking for folders
                    // that match the package
                    // defined in AndroidManifest.xml

                    // get the folder content
                    IResource[] content = folder.members();

                    // this is the segment we look for
                    String segment = packageSegments[packageSegmentIndex];

                    // did we find it at this level
                    boolean found = false;

                    for (IResource r : content) {
                        // look for the java package segment
                        if (r instanceof IFolder) {
                            if (r.getName().equals(segment)) {
                                // we need to skip to the next one
                                folder = (IFolder)r;
                                packageSegmentIndex++;
                                found = true;
                                break;
                            }
                        }
                    }

                    // if we didn't find it at this level we just fail.
                    if (found == false) {
                        failed = true;
                    }
                }

                // if we didn't fail then we found it. no point in
                // looping through the rest
                // or the classpathEntry
                if (failed == false) {
                    // save the target folder reference
                    manifestPackageSourceFolder = classpathFolder;
                    packageFolder = folder;
                    break;
                }
            }
        }

        // save the location of the folder into the persistent storage
        if (manifestPackageSourceFolder != mManifestPackageSourceFolder) {
            mManifestPackageSourceFolder = manifestPackageSourceFolder;
            saveProjectResourceProperty(PROPERTY_SOURCE_FOLDER, mManifestPackageSourceFolder);
        }
        return packageFolder;
    }

    /**
     * Compiles aidl files into java. This will also removes old java files
     * created from aidl files that are now gone.
     * @param sourceFolders the list of source folders, relative to the workspace.
     * @param folderAidlPath 
     * @param monitor the projess monitor
     * @returns true if it did something
     * @throws CoreException
     */
    private boolean handleAidl(ArrayList<IPath> sourceFolders, String folderAidlPath,
            IProgressMonitor monitor) throws CoreException {
        if (mAidlToCompile.size() == 0 && mAidlToRemove.size() == 0) {
            return false;
        }
        

        // create the command line
        String[] command = new String[4 + sourceFolders.size() + (folderAidlPath != null ? 1 : 0)];
        int index = 0;
        int aidlIndex;
        command[index++] = AdtPlugin.getOsAbsoluteAidl();
        command[aidlIndex = index++] = "-p"; //$NON-NLS-1$
        if (folderAidlPath != null) {
            command[index++] = "-p" + folderAidlPath; //$NON-NLS-1$
        }
        
        // since the path are relative to the workspace and not the project itself, we need
        // the workspace root.
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot(); 
        for (IPath p : sourceFolders) {
            IFolder f = wsRoot.getFolder(p);
            command[index++] = "-I" + f.getLocation().toOSString(); //$NON-NLS-1$
        }

        // list of files that have failed compilation.
        ArrayList<IFile> stillNeedCompilation = new ArrayList<IFile>();

        // if an aidl file is being removed before we managed to compile it, it'll be in
        // both list. We *need* to remove it from the compile list or it'll never go away.
        for (IFile aidlFile : mAidlToRemove) {
            int pos = mAidlToCompile.indexOf(aidlFile);
            if (pos != -1) {
                mAidlToCompile.remove(pos);
            }
        }
        
        // loop until we've compile them all
        for (IFile aidlFile : mAidlToCompile) {
            // Remove the AIDL error markers from the aidl file
            removeMarkersFromFile(aidlFile, AndroidConstants.MARKER_AIDL);

            // get the path
            IPath iPath = aidlFile.getLocation();
            String osPath = iPath.toOSString();

            // get the parent container
            IContainer parentContainer = aidlFile.getParent();

            // replace the extension in both the full path and the
            // last segment
            String osJavaPath = osPath.replaceAll(AndroidConstants.RE_AIDL_EXT,
                    AndroidConstants.DOT_JAVA);
            String javaName = aidlFile.getName().replaceAll(AndroidConstants.RE_AIDL_EXT,
                    AndroidConstants.DOT_JAVA);

            // check if we can compile it, or if there is a conflict with a java file
            boolean conflict = ProjectHelper.loadBooleanProperty(aidlFile,
                    PROPERTY_ANDROID_CONFLICT, false);
            if (conflict) {
                String msg = String.format(Messages.AIDL_Java_Conflict, javaName,
                        aidlFile.getName());

                // put a marker
                BaseProjectHelper.addMarker(aidlFile, AndroidConstants.MARKER_AIDL, msg,
                        IMarker.SEVERITY_ERROR);

                // output an error
                AdtPlugin.printErrorToConsole(getProject(), msg);

                stillNeedCompilation.add(aidlFile);

                // move on to next file
                continue;
            }

            // get the resource for the java file.
            Path javaIPath = new Path(javaName);
            IFile javaFile = parentContainer.getFile(javaIPath);

            // if the file was read-only, this will make it readable.
            prepareFileForExternalModification(javaFile);

            // finish to set the command line.
            command[aidlIndex] = "-p" + Sdk.getCurrent().getTarget(aidlFile.getProject()).getPath(
                    IAndroidTarget.ANDROID_AIDL); //$NON-NLS-1$
            command[index] = osPath;
            command[index + 1] = osJavaPath;

            // launch the process
            if (execAidl(command, aidlFile) == false) {
                // aidl failed. File should be marked. We add the file to the list
                // of file that will need compilation again.
                stillNeedCompilation.add(aidlFile);

                // and we move on to the next one.
                continue;
            } else {
                // since the exec worked, we refresh the parent, and set the
                // file as read only.
                finishFileAfterExternalModification(javaFile, aidlFile);
            }
        }

        // change the list to only contains the file that have failed compilation
        mAidlToCompile.clear();
        mAidlToCompile.addAll(stillNeedCompilation);

        // Remove the java files created from aidl files that have been removed.
        for (IFile aidlFile : mAidlToRemove) {
            // make the java filename
            String javaName = aidlFile.getName().replaceAll(
                    AndroidConstants.RE_AIDL_EXT,
                    AndroidConstants.DOT_JAVA);

            // get the parent container
            IContainer ic = aidlFile.getParent();

            // and get the IFile corresponding to the java file.
            IFile javaFile = ic.getFile(new Path(javaName));
            if (javaFile != null && javaFile.exists() ) {
                // check if this java file has a persistent data marking it as generated by
                // the builder.
                // While we put the aidl path as a resource, internally it's all string anyway.
                // We use loadStringProperty, because loadResourceProperty tries to match
                // the string value (a path in this case) with an existing resource, but
                // the aidl file was deleted, so it would return null, even though the property
                // existed.
                String aidlPath = ProjectHelper.loadStringProperty(javaFile,
                        PROPERTY_ANDROID_GENERATED);

                if (aidlPath != null) {
                    // This confirms the java file was generated by the builder,
                    // we can delete the aidlFile.
                    javaFile.delete(true, null);

                    // Refresh parent.
                    ic.refreshLocal(IResource.DEPTH_ONE, monitor);
                }
            }
        }
        mAidlToRemove.clear();

        return true;
    }

    /**
     * Execute the aidl command line, parse the output, and mark the aidl file
     * with any reported errors.
     * @param command the String array containing the command line to execute.
     * @param file The IFile object representing the aidl file being
     *      compiled.
     * @return false if the exec failed, and build needs to be aborted.
     */
    private boolean execAidl(String[] command, IFile file) {
        // do the exec
        try {
            Process p = Runtime.getRuntime().exec(command);

            // list to store each line of stderr
            ArrayList<String> results = new ArrayList<String>();

            // get the output and return code from the process
            int result = grabProcessOutput(p, results);

            // attempt to parse the error output
            boolean error = parseAidlOutput(results, file);

            // If the process failed and we couldn't parse the output
            // we pring a message, mark the project and exit
            if (result != 0 && error == true) {
                // display the message in the console.
                AdtPlugin.printErrorToConsole(getProject(), results.toArray());

                // mark the project and exit
                markProject(AdtConstants.MARKER_ADT, Messages.Unparsed_AIDL_Errors,
                        IMarker.SEVERITY_ERROR);
                return false;
            }
        } catch (IOException e) {
            // mark the project and exit
            String msg = String.format(Messages.AIDL_Exec_Error, command[0]);
            markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);
            return false;
        } catch (InterruptedException e) {
            // mark the project and exit
            String msg = String.format(Messages.AIDL_Exec_Error, command[0]);
            markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);
            return false;
        }

        return true;
    }

    /**
     * Goes through the build paths and fills the list of aidl files to compile
     * ({@link #mAidlToCompile}).
     * @param project The project.
     * @param buildPaths The list of build paths.
     */
    private void buildAidlCompilationList(IProject project,
            ArrayList<IPath> buildPaths) {
        for (IPath p : buildPaths) {
            // Because the path contains the name of the project as well, we
            // need to remove it, to access the final folder.
            String[] segments = p.segments();
            IContainer folder = project;
            for (int i = 1; i < segments.length; i++) {
                IResource r = folder.findMember(segments[i]);
                if (r != null && r.exists() &&
                        r.getType() == IResource.FOLDER) {
                    folder = (IContainer)r;
                } else {
                    // hmm looks like the build path is corrupted/wrong.
                    // reset and break
                    folder = project;
                    break;
                }
            }

            // did we ge a folder?
            if (folder != project) {
                // then we scan!
                scanContainerForAidl(folder);
            }
        }
    }

    /**
     * Scans a container and fills the list of aidl files to compile.
     * @param container The container to scan.
     */
    private void scanContainerForAidl(IContainer container) {
        try {
            IResource[] members = container.members();
            for (IResource r : members) {
                // get the type of the resource
               switch (r.getType()) {
                   case IResource.FILE:
                       // if this a file, check that the file actually exist
                       // and that it's an aidl file
                       if (r.exists() &&
                               AndroidConstants.EXT_AIDL.equalsIgnoreCase(r.getFileExtension())) {
                           mAidlToCompile.add((IFile)r);
                       }
                       break;
                   case IResource.FOLDER:
                       // recursively go through children
                       scanContainerForAidl((IFolder)r);
                       break;
                   default:
                       // this would mean it's a project or the workspace root
                       // which is unlikely to happen. we do nothing
                       break;
               }
            }
        } catch (CoreException e) {
            // Couldn't get the members list for some reason. Just return.
        }
    }


    /**
     * Parse the output of aidl and mark the file with any errors.
     * @param lines The output to parse.
     * @param file The file to mark with error.
     * @return true if the parsing failed, false if success.
     */
    private boolean parseAidlOutput(ArrayList<String> lines, IFile file) {
        // nothing to parse? just return false;
        if (lines.size() == 0) {
            return false;
        }

        Matcher m;

        for (int i = 0; i < lines.size(); i++) {
            String p = lines.get(i);

            m = sAidlPattern1.matcher(p);
            if (m.matches()) {
                // we can ignore group 1 which is the location since we already
                // have a IFile object representing the aidl file.
                String lineStr = m.group(2);
                String msg = m.group(3);

                // get the line number
                int line = 0;
                try {
                    line = Integer.parseInt(lineStr);
                } catch (NumberFormatException e) {
                    // looks like the string we extracted wasn't a valid
                    // file number. Parsing failed and we return true
                    return true;
                }

                // mark the file
                BaseProjectHelper.addMarker(file, AndroidConstants.MARKER_AIDL, msg, line,
                        IMarker.SEVERITY_ERROR);

                // success, go to the next line
                continue;
            }

            // invalid line format, flag as error, and bail
            return true;
        }

        return false;
    }

    /**
     * Merge the current list of aidl file to compile/remove with the new one.
     * @param toCompile List of file to compile
     * @param toRemove List of file to remove
     */
    private void mergeAidlFileModifications(ArrayList<IFile> toCompile,
            ArrayList<IFile> toRemove) {

        // loop through the new toRemove list, and add it to the old one,
        // plus remove any file that was still to compile and that are now
        // removed
        for (IFile r : toRemove) {
            if (mAidlToRemove.indexOf(r) == -1) {
                mAidlToRemove.add(r);
            }

            int index = mAidlToCompile.indexOf(r);
            if (index != -1) {
                mAidlToCompile.remove(index);
            }
        }

        // now loop through the new files to compile and add it to the list.
        // Also look for them in the remove list, this would mean that they
        // were removed, then added back, and we shouldn't remove them, just
        // recompile them.
        for (IFile r : toCompile) {
            if (mAidlToCompile.indexOf(r) == -1) {
                mAidlToCompile.add(r);
            }

            int index = mAidlToRemove.indexOf(r);
            if (index != -1) {
                mAidlToRemove.remove(index);
            }
        }
    }

    /**
     * Prepare an already existing file for modification. File generated from
     * command line processed are marked as read-only. This method prepares
     * them (mark them as read-write) before the command line process is
     * started. A check is made to be sure the file exists.
     * @param file The IResource object for the file to prepare.
     * @throws CoreException
     */
    private void prepareFileForExternalModification(IFile file)
            throws CoreException {
        // file may not exist yet, so we check that.
        if (file != null && file.exists()) {
            // get the attributes.
            ResourceAttributes ra = file.getResourceAttributes();
            if (ra != null) {
                // change the attributes
                ra.setReadOnly(false);

                // set the new attributes in the file.
                file.setResourceAttributes(ra);
            }
        }
    }

    /**
     * Finish a file created/modified by an outside command line process.
     * The file is marked as modified by Android, and the parent folder is refreshed, so that,
     * in case the file didn't exist beforehand, the file appears in the package explorer.
     * @param rFile The R file to "finish".
     * @param manifestFile The manifest file to "finish".
     * @throws CoreException
     */
    private void finishJavaFilesAfterExternalModification(IFile rFile, IFile manifestFile)
            throws CoreException {
        IContainer parent = rFile.getParent();

        IProgressMonitor monitor = new DerivedProgressMonitor(rFile, manifestFile);

        // refresh the parent node in the package explorer. Once this is done the custom progress
        // monitor will mark them as derived.
        parent.refreshLocal(IResource.DEPTH_ONE, monitor);
    }

    /**
     * Finish a file created/modified by an outside command line process.
     * The file is marked as modified by Android, and the parent folder is refreshed, so that,
     * in case the file didn't exist beforehand, the file appears in the package explorer.
     * @param file The file to "finish".
     * @param aidlFile The AIDL file to "finish".
     * @throws CoreException
     */
    private void finishFileAfterExternalModification(IFile file, IFile aidlFile)
            throws CoreException {
        IContainer parent = file.getParent();

        // we need to add a link to the aidl file.
        // We need to wait for the refresh of the parent to be done, so we'll do
        // it in the monitor. This will also set the file as derived.
        IProgressMonitor monitor = new RefreshProgressMonitor(file, aidlFile);

        // refresh the parent node in the package explorer.
        parent.refreshLocal(IResource.DEPTH_ONE, monitor);
    }
}
