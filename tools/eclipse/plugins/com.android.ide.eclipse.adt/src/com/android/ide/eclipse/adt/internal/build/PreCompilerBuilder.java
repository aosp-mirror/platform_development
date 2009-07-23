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

package com.android.ide.eclipse.adt.internal.build;

import com.android.ide.eclipse.adt.AdtConstants;
import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;
import com.android.ide.eclipse.adt.internal.project.FixLaunchConfig;
import com.android.ide.eclipse.adt.internal.project.XmlErrorHandler.BasicXmlErrorListener;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.xml.ManifestConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
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

    private static final String PROPERTY_COMPILE_RESOURCES = "compileResources"; //$NON-NLS-1$
    private static final String PROPERTY_COMPILE_AIDL = "compileAidl"; //$NON-NLS-1$

    /**
     * Single line aidl error<br>
     * "&lt;path&gt;:&lt;line&gt;: &lt;error&gt;"
     * or
     * "&lt;path&gt;:&lt;line&gt; &lt;error&gt;"
     */
    private static Pattern sAidlPattern1 = Pattern.compile("^(.+?):(\\d+):?\\s(.+)$"); //$NON-NLS-1$

    /**
     * Data to temporarly store aidl source file information
     */
    static class AidlData {
        IFile aidlFile;
        IFolder sourceFolder;

        AidlData(IFolder sourceFolder, IFile aidlFile) {
            this.sourceFolder = sourceFolder;
            this.aidlFile = aidlFile;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof AidlData) {
                AidlData file = (AidlData)obj;
                return aidlFile.equals(file.aidlFile) && sourceFolder.equals(file.sourceFolder);
            }

            return false;
        }
    }

    /**
     * Resource Compile flag. This flag is reset to false after each successful compilation, and
     * stored in the project persistent properties. This allows the builder to remember its state
     * when the project is closed/opened.
     */
    private boolean mMustCompileResources = false;

    /** List of .aidl files found that are modified or new. */
    private final ArrayList<AidlData> mAidlToCompile = new ArrayList<AidlData>();

    /** List of .aidl files that have been removed. */
    private final ArrayList<AidlData> mAidlToRemove = new ArrayList<AidlData>();

    /** cache of the java package defined in the manifest */
    private String mManifestPackage;

    /** Output folder for generated Java File. Created on the Builder init
     * @see #startupOnInitialize()
     */
    private IFolder mGenFolder;

    /**
     * Progress monitor used at the end of every build to refresh the content of the 'gen' folder
     * and set the generated files as derived.
     */
    private DerivedProgressMonitor mDerivedProgressMonitor;

    /**
     * Progress monitor waiting the end of the process to set a persistent value
     * in a file. This is typically used in conjunction with <code>IResource.refresh()</code>,
     * since this call is asysnchronous, and we need to wait for it to finish for the file
     * to be known by eclipse, before we can call <code>resource.setPersistentProperty</code> on
     * a new file.
     */
    private static class DerivedProgressMonitor implements IProgressMonitor {
        private boolean mCancelled = false;
        private final ArrayList<IFile> mFileList = new ArrayList<IFile>();
        private boolean mDone = false;
        public DerivedProgressMonitor() {
        }

        void addFile(IFile file) {
            mFileList.add(file);
        }

        void reset() {
            mFileList.clear();
            mDone = false;
        }

        public void beginTask(String name, int totalWork) {
        }

        public void done() {
            if (mDone == false) {
                mDone = true;
                for (IFile file : mFileList) {
                    if (file.exists()) {
                        try {
                            file.setDerived(true);
                        } catch (CoreException e) {
                            // This really shouldn't happen since we check that the resource exist.
                            // Worst case scenario, the resource isn't marked as derived.
                        }
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
    @SuppressWarnings("unchecked")
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        try {
            mDerivedProgressMonitor.reset();

            // First thing we do is go through the resource delta to not
            // lose it if we have to abort the build for any reason.

            // get the project objects
            IProject project = getProject();

            // Top level check to make sure the build can move forward.
            abortOnBadSetup(project);

            IJavaProject javaProject = JavaCore.create(project);
            IAndroidTarget projectTarget = Sdk.getCurrent().getTarget(project);

            // now we need to get the classpath list
            ArrayList<IPath> sourceFolderPathList = BaseProjectHelper.getSourceClasspaths(
                    javaProject);

            PreCompilerDeltaVisitor dv = null;
            String javaPackage = null;
            String minSdkVersion = null;

            if (kind == FULL_BUILD) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        Messages.Start_Full_Pre_Compiler);
                mMustCompileResources = true;
                buildAidlCompilationList(project, sourceFolderPathList);
            } else {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        Messages.Start_Inc_Pre_Compiler);

                // Go through the resources and see if something changed.
                // Even if the mCompileResources flag is true from a previously aborted
                // build, we need to go through the Resource delta to get a possible
                // list of aidl files to compile/remove.
                IResourceDelta delta = getDelta(project);
                if (delta == null) {
                    mMustCompileResources = true;
                    buildAidlCompilationList(project, sourceFolderPathList);
                } else {
                    dv = new PreCompilerDeltaVisitor(this, sourceFolderPathList);
                    delta.accept(dv);

                    // record the state
                    mMustCompileResources |= dv.getCompileResources();

                    if (dv.getForceAidlCompile()) {
                        buildAidlCompilationList(project, sourceFolderPathList);
                    } else {
                        // handle aidl modification, and update mMustCompileAidl
                        mergeAidlFileModifications(dv.getAidlToCompile(),
                                dv.getAidlToRemove());
                    }

                    // get the java package from the visitor
                    javaPackage = dv.getManifestPackage();
                    minSdkVersion = dv.getMinSdkVersion();
                }
            }

            // store the build status in the persistent storage
            saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES , mMustCompileResources);

            // if there was some XML errors, we just return w/o doing
            // anything since we've put some markers in the files anyway.
            if (dv != null && dv.mXmlError) {
                AdtPlugin.printErrorToConsole(project, Messages.Xml_Error);

                // This interrupts the build. The next builders will not run.
                stopBuild(Messages.Xml_Error);
            }


            // get the manifest file
            IFile manifest = AndroidManifestParser.getManifest(project);

            if (manifest == null) {
                String msg = String.format(Messages.s_File_Missing,
                        AndroidConstants.FN_ANDROID_MANIFEST);
                AdtPlugin.printErrorToConsole(project, msg);
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
                minSdkVersion = parser.getApiLevelRequirement();
            }

            if (minSdkVersion != null) {
                int minSdkValue = -1;
                try {
                    minSdkValue = Integer.parseInt(minSdkVersion);
                } catch (NumberFormatException e) {
                    // it's ok, it means minSdkVersion contains a (hopefully) valid codename.
                }

                AndroidVersion projectVersion = projectTarget.getVersion();

                if (minSdkValue != -1) {
                    String codename = projectVersion.getCodename();
                    if (codename != null) {
                        // integer minSdk when the target is a preview => fatal error
                        String msg = String.format(
                                "Platform %1$s is a preview and requires appication manifests to set %2$s to '%3$s'",
                                codename, ManifestConstants.ATTRIBUTE_MIN_SDK_VERSION,
                                codename);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.addMarker(manifest, AdtConstants.MARKER_ADT, msg,
                                IMarker.SEVERITY_ERROR);
                        stopBuild(msg);
                    } else if (minSdkValue < projectVersion.getApiLevel()) {
                        // integer minSdk is not high enough for the target => warning
                        String msg = String.format(
                                "Manifest min SDK version (%1$d) is lower than project target API level (%2$d)",
                                minSdkVersion, projectVersion.getApiLevel());
                        AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project, msg);
                        BaseProjectHelper.addMarker(manifest, AdtConstants.MARKER_ADT, msg,
                                IMarker.SEVERITY_WARNING);
                    }
                } else {
                    // looks like the min sdk is a codename, check it matches the codename
                    // of the platform
                    String codename = projectVersion.getCodename();
                    if (codename == null) {
                        // platform is not a preview => fatal error
                        String msg = String.format(
                                "Manifest attribute '%1$s' is set to '%2$s'. Integer is expected.",
                                ManifestConstants.ATTRIBUTE_MIN_SDK_VERSION, codename);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.addMarker(manifest, AdtConstants.MARKER_ADT, msg,
                                IMarker.SEVERITY_ERROR);
                        stopBuild(msg);
                    } else if (codename.equals(minSdkVersion) == false) {
                        // platform and manifest codenames don't match => fatal error.
                        String msg = String.format(
                                "Value of manifest attribute '%1$s' does not match platform codename '%2$s'",
                                ManifestConstants.ATTRIBUTE_MIN_SDK_VERSION, codename);
                        AdtPlugin.printErrorToConsole(project, msg);
                        BaseProjectHelper.addMarker(manifest, AdtConstants.MARKER_ADT, msg,
                                IMarker.SEVERITY_ERROR);
                        stopBuild(msg);
                    }
                }
            }

            if (javaPackage == null || javaPackage.length() == 0) {
                // looks like the AndroidManifest file isn't valid.
                String msg = String.format(Messages.s_Doesnt_Declare_Package_Error,
                        AndroidConstants.FN_ANDROID_MANIFEST);
                AdtPlugin.printErrorToConsole(project, msg);
                markProject(AdtConstants.MARKER_ADT, msg, IMarker.SEVERITY_ERROR);

                // This interrupts the build. The next builders will not run.
                stopBuild(msg);
            }

            // at this point we have the java package. We need to make sure it's not a different
            // package than the previous one that were built.
            if (javaPackage.equals(mManifestPackage) == false) {
                // The manifest package has changed, the user may want to update
                // the launch configuration
                if (mManifestPackage != null) {
                    AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                            Messages.Checking_Package_Change);

                    FixLaunchConfig flc = new FixLaunchConfig(project, mManifestPackage,
                            javaPackage);
                    flc.start();
                }

                // now we delete the generated classes from their previous location
                deleteObsoleteGeneratedClass(AndroidConstants.FN_RESOURCE_CLASS,
                        mManifestPackage);
                deleteObsoleteGeneratedClass(AndroidConstants.FN_MANIFEST_CLASS,
                        mManifestPackage);

                // record the new manifest package, and save it.
                mManifestPackage = javaPackage;
                saveProjectStringProperty(PROPERTY_PACKAGE, mManifestPackage);
            }

            if (mMustCompileResources) {
                // we need to figure out where to store the R class.
                // get the parent folder for R.java and update mManifestPackageSourceFolder
                IFolder packageFolder = getGenManifestPackageFolder(project);

                // get the resource folder
                IFolder resFolder = project.getFolder(AndroidConstants.WS_RESOURCES);

                // get the file system path
                IPath outputLocation = mGenFolder.getLocation();
                IPath resLocation = resFolder.getLocation();
                IPath manifestLocation = manifest.getLocation();

                // those locations have to exist for us to do something!
                if (outputLocation != null && resLocation != null
                        && manifestLocation != null) {
                    String osOutputPath = outputLocation.toOSString();
                    String osResPath = resLocation.toOSString();
                    String osManifestPath = manifestLocation.toOSString();

                    // remove the aapt markers
                    removeMarkersFromFile(manifest, AndroidConstants.MARKER_AAPT_COMPILE);
                    removeMarkersFromContainer(resFolder, AndroidConstants.MARKER_AAPT_COMPILE);

                    AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                            Messages.Preparing_Generated_Files);

                    // since the R.java file may be already existing in read-only
                    // mode we need to make it readable so that aapt can overwrite
                    // it
                    IFile rJavaFile = packageFolder.getFile(AndroidConstants.FN_RESOURCE_CLASS);

                    // do the same for the Manifest.java class
                    IFile manifestJavaFile = packageFolder.getFile(
                            AndroidConstants.FN_MANIFEST_CLASS);

                    // we actually need to delete the manifest.java as it may become empty and
                    // in this case aapt doesn't generate an empty one, but instead doesn't
                    // touch it.
                    manifestJavaFile.delete(true, null);

                    // launch aapt: create the command line
                    ArrayList<String> array = new ArrayList<String>();
                    array.add(projectTarget.getPath(IAndroidTarget.AAPT));
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
                            // if the exec failed, and we couldn't parse the error output
                            // (and therefore not all files that should have been marked,
                            // were marked), we put a generic marker on the project and abort.
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
                        // now add the R.java/Manifest.java to the list of file to be marked
                        // as derived.
                        mDerivedProgressMonitor.addFile(rJavaFile);
                        mDerivedProgressMonitor.addFile(manifestJavaFile);

                        // build has been done. reset the state of the builder
                        mMustCompileResources = false;

                        // and store it
                        saveProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES,
                                mMustCompileResources);
                    }
                }
            } else {
                // nothing to do
            }

            // now handle the aidl stuff.
            boolean aidlStatus = handleAidl(projectTarget, sourceFolderPathList, monitor);

            if (aidlStatus == false && mMustCompileResources == false) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        Messages.Nothing_To_Compile);
            }
        } finally {
            // refresh the 'gen' source folder. Once this is done with the custom progress
            // monitor to mark all new files as derived
            mGenFolder.refreshLocal(IResource.DEPTH_INFINITE, mDerivedProgressMonitor);
        }

        return null;
    }

    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        super.clean(monitor);

        AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, getProject(),
                Messages.Removing_Generated_Classes);

        // remove all the derived resources from the 'gen' source folder.
        removeDerivedResources(mGenFolder, monitor);
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();

        mDerivedProgressMonitor = new DerivedProgressMonitor();

        IProject project = getProject();

        // load the previous IFolder and java package.
        mManifestPackage = loadProjectStringProperty(PROPERTY_PACKAGE);

        // get the source folder in which all the Java files are created
        mGenFolder = project.getFolder(SdkConstants.FD_GEN_SOURCES);

        // Load the current compile flags. We ask for true if not found to force a
        // recompile.
        mMustCompileResources = loadProjectBooleanProperty(PROPERTY_COMPILE_RESOURCES, true);
        boolean mustCompileAidl = loadProjectBooleanProperty(PROPERTY_COMPILE_AIDL, true);

        // if we stored that we have to compile some aidl, we build the list that will compile them
        // all
        if (mustCompileAidl) {
            IJavaProject javaProject = JavaCore.create(project);
            ArrayList<IPath> sourceFolderPathList = BaseProjectHelper.getSourceClasspaths(
                    javaProject);

            buildAidlCompilationList(project, sourceFolderPathList);
        }
    }

    /**
     * Delete the a generated java class associated with the specified java package.
     * @param filename Name of the generated file to remove.
     * @param javaPackage the old java package
     */
    private void deleteObsoleteGeneratedClass(String filename, String javaPackage) {
        if (javaPackage == null) {
            return;
        }

        IPath packagePath = getJavaPackagePath(javaPackage);
        IPath iPath = packagePath.append(filename);

        // Find a matching resource object.
        IResource javaFile = mGenFolder.findMember(iPath);
        if (javaFile != null && javaFile.exists() && javaFile.getType() == IResource.FILE) {
            try {
                // delete
                javaFile.delete(true, null);

                // refresh parent
                javaFile.getParent().refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());

            } catch (CoreException e) {
                // failed to delete it, the user will have to delete it manually.
                String message = String.format(Messages.Delete_Obsolete_Error,
                        javaFile.getFullPath());
                IProject project = getProject();
                AdtPlugin.printErrorToConsole(project, message);
                AdtPlugin.printErrorToConsole(project, e.getMessage());
            }
        }
    }

    /**
     * Creates a relative {@link IPath} from a java package.
     * @param javaPackageName the java package.
     */
    private IPath getJavaPackagePath(String javaPackageName) {
        // convert the java package into path
        String[] segments = javaPackageName.split(AndroidConstants.RE_DOT);

        StringBuilder path = new StringBuilder();
        for (String s : segments) {
           path.append(AndroidConstants.WS_SEP_CHAR);
           path.append(s);
        }

        return new Path(path.toString());
    }

    /**
     * Returns an {@link IFolder} (located inside the 'gen' source folder), that matches the
     * package defined in the manifest. This {@link IFolder} may not actually exist
     * (aapt will create it anyway).
     * @param project The project.
     * @return the {@link IFolder} that will contain the R class or null if the folder was not found.
     * @throws CoreException
     */
    private IFolder getGenManifestPackageFolder(IProject project)
            throws CoreException {
        // get the path for the package
        IPath packagePath = getJavaPackagePath(mManifestPackage);

        // get a folder for this path under the 'gen' source folder, and return it.
        // This IFolder may not reference an actual existing folder.
        return mGenFolder.getFolder(packagePath);
    }

    /**
     * Compiles aidl files into java. This will also removes old java files
     * created from aidl files that are now gone.
     * @param projectTarget Target of the project
     * @param sourceFolders the list of source folders, relative to the workspace.
     * @param monitor the projess monitor
     * @returns true if it did something
     * @throws CoreException
     */
    private boolean handleAidl(IAndroidTarget projectTarget, ArrayList<IPath> sourceFolders,
            IProgressMonitor monitor) throws CoreException {
        if (mAidlToCompile.size() == 0 && mAidlToRemove.size() == 0) {
            return false;
        }

        // create the command line
        String[] command = new String[4 + sourceFolders.size()];
        int index = 0;
        command[index++] = projectTarget.getPath(IAndroidTarget.AIDL);
        command[index++] = "-p" + Sdk.getCurrent().getTarget(getProject()).getPath( //$NON-NLS-1$
                IAndroidTarget.ANDROID_AIDL);

        // since the path are relative to the workspace and not the project itself, we need
        // the workspace root.
        IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (IPath p : sourceFolders) {
            IFolder f = wsRoot.getFolder(p);
            command[index++] = "-I" + f.getLocation().toOSString(); //$NON-NLS-1$
        }

        // list of files that have failed compilation.
        ArrayList<AidlData> stillNeedCompilation = new ArrayList<AidlData>();

        // if an aidl file is being removed before we managed to compile it, it'll be in
        // both list. We *need* to remove it from the compile list or it'll never go away.
        for (AidlData aidlFile : mAidlToRemove) {
            int pos = mAidlToCompile.indexOf(aidlFile);
            if (pos != -1) {
                mAidlToCompile.remove(pos);
            }
        }

        // loop until we've compile them all
        for (AidlData aidlData : mAidlToCompile) {
            // Remove the AIDL error markers from the aidl file
            removeMarkersFromFile(aidlData.aidlFile, AndroidConstants.MARKER_AIDL);

            // get the path of the source file.
            IPath sourcePath = aidlData.aidlFile.getLocation();
            String osSourcePath = sourcePath.toOSString();

            IFile javaFile = getGenDestinationFile(aidlData, true /*createFolders*/, monitor);

            // finish to set the command line.
            command[index] = osSourcePath;
            command[index + 1] = javaFile.getLocation().toOSString();

            // launch the process
            if (execAidl(command, aidlData.aidlFile) == false) {
                // aidl failed. File should be marked. We add the file to the list
                // of file that will need compilation again.
                stillNeedCompilation.add(aidlData);

                // and we move on to the next one.
                continue;
            } else {
                // make sure the file will be marked as derived once we refresh the 'gen' source
                // folder.
                mDerivedProgressMonitor.addFile(javaFile);
            }
        }

        // change the list to only contains the file that have failed compilation
        mAidlToCompile.clear();
        mAidlToCompile.addAll(stillNeedCompilation);

        // Remove the java files created from aidl files that have been removed.
        for (AidlData aidlData : mAidlToRemove) {
            IFile javaFile = getGenDestinationFile(aidlData, false /*createFolders*/, monitor);
            if (javaFile.exists()) {
                // This confirms the java file was generated by the builder,
                // we can delete the aidlFile.
                javaFile.delete(true, null);

                // Refresh parent.
                javaFile.getParent().refreshLocal(IResource.DEPTH_ONE, monitor);
            }
        }

        mAidlToRemove.clear();

        // store the build state. If there are any files that failed to compile, we will
        // force a full aidl compile on the next project open. (unless a full compilation succeed
        // before the project is closed/re-opened.)
        // TODO: Optimize by saving only the files that need compilation
        saveProjectBooleanProperty(PROPERTY_COMPILE_AIDL , mAidlToCompile.size() > 0);

        return true;
    }

    /**
     * Returns the {@link IFile} handle to the destination file for a given aild source file
     * ({@link AidlData}).
     * @param aidlData the data for the aidl source file.
     * @param createFolders whether or not the parent folder of the destination should be created
     * if it does not exist.
     * @param monitor the progress monitor
     * @return the handle to the destination file.
     * @throws CoreException
     */
    private IFile getGenDestinationFile(AidlData aidlData, boolean createFolders,
            IProgressMonitor monitor) throws CoreException {
        // build the destination folder path.
        // Use the path of the source file, except for the path leading to its source folder,
        // and for the last segment which is the filename.
        int segmentToSourceFolderCount = aidlData.sourceFolder.getFullPath().segmentCount();
        IPath packagePath = aidlData.aidlFile.getFullPath().removeFirstSegments(
                segmentToSourceFolderCount).removeLastSegments(1);
        Path destinationPath = new Path(packagePath.toString());

        // get an IFolder for this path. It's relative to the 'gen' folder already
        IFolder destinationFolder = mGenFolder.getFolder(destinationPath);

        // create it if needed.
        if (destinationFolder.exists() == false && createFolders) {
            createFolder(destinationFolder, monitor);
        }

        // Build the Java file name from the aidl name.
        String javaName = aidlData.aidlFile.getName().replaceAll(AndroidConstants.RE_AIDL_EXT,
                AndroidConstants.DOT_JAVA);

        // get the resource for the java file.
        IFile javaFile = destinationFolder.getFile(javaName);
        return javaFile;
    }

    /**
     * Creates the destination folder. Because
     * {@link IFolder#create(boolean, boolean, IProgressMonitor)} only works if the parent folder
     * already exists, this goes and ensure that all the parent folders actually exist, or it
     * creates them as well.
     * @param destinationFolder The folder to create
     * @param monitor the {@link IProgressMonitor},
     * @throws CoreException
     */
    private void createFolder(IFolder destinationFolder, IProgressMonitor monitor)
            throws CoreException {

        // check the parent exist and create if necessary.
        IContainer parent = destinationFolder.getParent();
        if (parent.getType() == IResource.FOLDER && parent.exists() == false) {
            createFolder((IFolder)parent, monitor);
        }

        // create the folder.
        destinationFolder.create(true /*force*/, true /*local*/,
                new SubProgressMonitor(monitor, 10));
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
     * @param sourceFolderPathList The list of source folder paths.
     */
    private void buildAidlCompilationList(IProject project,
            ArrayList<IPath> sourceFolderPathList) {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for (IPath sourceFolderPath : sourceFolderPathList) {
            IFolder sourceFolder = root.getFolder(sourceFolderPath);
            // we don't look in the 'gen' source folder as there will be no source in there.
            if (sourceFolder.exists() && sourceFolder.equals(mGenFolder) == false) {
                scanFolderForAidl(sourceFolder, sourceFolder);
            }
        }
    }

    /**
     * Scans a folder and fills the list of aidl files to compile.
     * @param sourceFolder the root source folder.
     * @param folder The folder to scan.
     */
    private void scanFolderForAidl(IFolder sourceFolder, IFolder folder) {
        try {
            IResource[] members = folder.members();
            for (IResource r : members) {
                // get the type of the resource
               switch (r.getType()) {
                   case IResource.FILE:
                       // if this a file, check that the file actually exist
                       // and that it's an aidl file
                       if (r.exists() &&
                               AndroidConstants.EXT_AIDL.equalsIgnoreCase(r.getFileExtension())) {
                           mAidlToCompile.add(new AidlData(sourceFolder, (IFile)r));
                       }
                       break;
                   case IResource.FOLDER:
                       // recursively go through children
                       scanFolderForAidl(sourceFolder, (IFolder)r);
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
    private void mergeAidlFileModifications(ArrayList<AidlData> toCompile,
            ArrayList<AidlData> toRemove) {
        // loop through the new toRemove list, and add it to the old one,
        // plus remove any file that was still to compile and that are now
        // removed
        for (AidlData r : toRemove) {
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
        for (AidlData r : toCompile) {
            if (mAidlToCompile.indexOf(r) == -1) {
                mAidlToCompile.add(r);
            }

            int index = mAidlToRemove.indexOf(r);
            if (index != -1) {
                mAidlToRemove.remove(index);
            }
        }
    }
}
