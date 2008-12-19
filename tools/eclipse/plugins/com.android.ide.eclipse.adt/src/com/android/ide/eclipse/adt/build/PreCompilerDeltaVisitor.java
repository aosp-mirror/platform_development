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
import com.android.ide.eclipse.adt.build.BaseBuilder.BaseDeltaVisitor;
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestParser;
import com.android.ide.eclipse.common.project.BaseProjectHelper;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import java.util.ArrayList;

/**
 * Resource Delta visitor for the pre-compiler.
 */
class PreCompilerDeltaVisitor extends BaseDeltaVisitor implements
        IResourceDeltaVisitor {

    // Result fields.
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
    
    /** Aidl forced recompilation flag. This is set to true if project.aidl is modified. */
    private boolean mFullAidlCompilation = false;

    /** Manifest check/parsing flag. */
    private boolean mCheckedManifestXml = false;

    /** Application Pacakge, gathered from the parsing of the manifest */
    private String mJavaPackage = null;

    // Internal usage fields.
    /**
     * In Resource folder flag. This allows us to know if we're in the
     * resource folder.
     */
    private boolean mInRes = false;

    /**
     * In Source folder flag. This allows us to know if we're in a source
     * folder.
     */
    private boolean mInSrc = false;

    /** List of source folders. */
    private ArrayList<IPath> mSourceFolders;


    public PreCompilerDeltaVisitor(BaseBuilder builder, ArrayList<IPath> sourceFolders) {
        super(builder);
        mSourceFolders = sourceFolders;
    }

    public boolean getCompileResources() {
        return mCompileResources;
    }

    public ArrayList<IFile> getAidlToCompile() {
        return mAidlToCompile;
    }

    public ArrayList<IFile> getAidlToRemove() {
        return mAidlToRemove;
    }
    
    public boolean getFullAidlRecompilation() {
        return mFullAidlCompilation;
    }

    /**
     * Returns whether the manifest file was parsed/checked for error during the resource delta
     * visiting.
     */
    public boolean getCheckedManifestXml() {
        return mCheckedManifestXml;
    }
    
    /**
     * Returns the manifest package if the manifest was checked/parsed.
     * <p/>
     * This can return null in two cases:
     * <ul>
     * <li>The manifest was not part of the resource change delta, and the manifest was
     * not checked/parsed ({@link #getCheckedManifestXml()} returns <code>false</code>)</li>
     * <li>The manifest was parsed ({@link #getCheckedManifestXml()} returns <code>true</code>),
     * but the package declaration is missing</li>
     * </ul>
     * @return the manifest package or null.
     */
    public String getManifestPackage() {
        return mJavaPackage;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IResourceDeltaVisitor
     *      #visit(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException {
        // we are only going to look for changes in res/, source folders and in
        // AndroidManifest.xml since the delta visitor goes through the main
        // folder before its children we can check when the path segment
        // count is 2 (format will be /$Project/folder) and make sure we are
        // processing res/, source folders or AndroidManifest.xml

        IResource resource = delta.getResource();
        IPath path = resource.getFullPath();
        String[] segments = path.segments();

        // since the delta visitor also visits the root we return true if
        // segments.length = 1
        if (segments.length == 1) {
            return true;
        } else if (segments.length == 2) {
            // if we are at an item directly under the root directory,
            // then we are not yet in a source or resource folder
            mInRes = mInSrc = false;

            if (AndroidConstants.FD_RESOURCES.equalsIgnoreCase(segments[1])) {
                // this is the resource folder that was modified. we want to
                // see its content.

                // since we're going to visit its children next, we set the
                // flag
                mInRes = true;
                mInSrc = false;
                return true;
            } else if (AndroidConstants.FN_ANDROID_MANIFEST.equalsIgnoreCase(segments[1])) {
                // any change in the manifest could trigger a new R.java
                // class, so we don't need to check the delta kind
                if (delta.getKind() != IResourceDelta.REMOVED) {
                    // parse the manifest for errors
                    AndroidManifestParser parser = BaseProjectHelper.parseManifestForError(
                            (IFile)resource, this);
                    
                    if (parser != null) {
                        mJavaPackage = parser.getPackage();
                    }

                    mCheckedManifestXml = true;
                }
                mCompileResources = true;

                // we don't want to go to the children, not like they are
                // any for this resource anyway.
                return false;
            } else if (AndroidConstants.FN_PROJECT_AIDL.equalsIgnoreCase(segments[1])) {
                // need to force recompilation of all the aidl files
                mFullAidlCompilation = true;
            }
        }

        // at this point we can either be in the source folder or in the
        // resource folder or in a different folder that contains a source
        // folder.
        // This is due to not all source folder being src/. Some could be
        // something/somethingelse/src/

        // so first we test if we already know we are in a source or
        // resource folder.

        if (mInSrc) {
            // if we are in the res folder, we are looking for the following changes:
            // - added/removed/modified aidl files.
            // - missing R.java file

            // if the resource is a folder, we just go straight to the children
            if (resource.getType() == IResource.FOLDER) {
                return true;
            }

            if (resource.getType() != IResource.FILE) {
                return false;
            }
            IFile file = (IFile)resource;

            // get the modification kind
            int kind = delta.getKind();

            if (kind == IResourceDelta.ADDED) {
                // we only care about added files (inside the source folders), if they
                // are aidl files.

                // get the extension of the resource
                String ext = resource.getFileExtension();

                if (AndroidConstants.EXT_AIDL.equalsIgnoreCase(ext)) {
                    // look for an already existing matching java file
                    String javaName = resource.getName().replaceAll(
                            AndroidConstants.RE_AIDL_EXT,
                            AndroidConstants.DOT_JAVA);

                    // get the parent container
                    IContainer ic = resource.getParent();

                    IFile javaFile = ic.getFile(new Path(javaName));
                    if (javaFile != null && javaFile.exists()) {
                        // check if that file was generated by the plugin. Normally those files are
                        // deleted automatically, but it's better to check.
                        String aidlPath = ProjectHelper.loadStringProperty(javaFile,
                                PreCompilerBuilder.PROPERTY_ANDROID_GENERATED);
                        if (aidlPath == null) {
                            // mark the aidl file that it cannot be compile just yet
                            ProjectHelper.saveBooleanProperty(file,
                                    PreCompilerBuilder.PROPERTY_ANDROID_CONFLICT, true);
                        }

                        // we add it anyway so that we can try to compile it at every compilation
                        // until the conflict is fixed.
                        mAidlToCompile.add(file);

                    } else {
                        // the java file doesn't exist, we can safely add the file to the list
                        // of files to compile.
                        mAidlToCompile.add(file);
                    }
                }

                return false;
            }

            // get the filename
            String fileName = segments[segments.length - 1];

            boolean outputMessage = false;

            // Special case of R.java/Manifest.java.
            // FIXME: This does not check the package. Any modification of R.java/Manifest.java in another project will trigger a new recompilation of the resources.
            if (AndroidConstants.FN_RESOURCE_CLASS.equals(fileName) ||
                    AndroidConstants.FN_MANIFEST_CLASS.equals(fileName)) {
                // if it was removed, there's a possibility that it was removed due to a
                // package change, or an aidl that was removed, but the only thing
                // that will happen is that we'll have an extra build. Not much of a problem.
                mCompileResources = true;

                // we want a warning
                outputMessage = true;
            } else {

                // get the extension of the resource
                String ext = resource.getFileExtension();

                if (AndroidConstants.EXT_AIDL.equalsIgnoreCase(ext)) {
                    if (kind == IResourceDelta.REMOVED) {
                        mAidlToRemove.add(file);
                    } else {
                        mAidlToCompile.add(file);
                    }
                } else {
                    if (kind == IResourceDelta.REMOVED) {
                        // the file has been removed. we need to check it's a java file and that
                        // there's a matching aidl file. We can't check its persistent storage
                        // anymore.
                        if (AndroidConstants.EXT_JAVA.equalsIgnoreCase(ext)) {
                            String aidlFile = resource.getName().replaceAll(
                                    AndroidConstants.RE_JAVA_EXT,
                                    AndroidConstants.DOT_AIDL);

                            // get the parent container
                            IContainer ic = resource.getParent();

                            IFile f = ic.getFile(new Path(aidlFile));
                            if (f != null && f.exists() ) {
                                // make sure that the aidl file is not in conflict anymore, in
                                // case the java file was not generated by us.
                                if (ProjectHelper.loadBooleanProperty(f,
                                        PreCompilerBuilder.PROPERTY_ANDROID_CONFLICT, false)) {
                                    ProjectHelper.saveBooleanProperty(f,
                                            PreCompilerBuilder.PROPERTY_ANDROID_CONFLICT, false);
                                } else {
                                    outputMessage = true;
                                }
                                mAidlToCompile.add(f);
                            }
                        }
                    } else {
                        // check if it's an android generated java file.
                        IResource aidlSource = ProjectHelper.loadResourceProperty(
                                file, PreCompilerBuilder.PROPERTY_ANDROID_GENERATED);

                        if (aidlSource != null && aidlSource.exists() &&
                                aidlSource.getType() == IResource.FILE) {
                            // it looks like this was a java file created from an aidl file.
                            // we need to add the aidl file to the list of aidl file to compile
                            mAidlToCompile.add((IFile)aidlSource);
                            outputMessage = true;
                        }
                    }
                }
            }

            if (outputMessage) {
                if (kind == IResourceDelta.REMOVED) {
                    // We pring an error just so that it's red, but it's just a warning really.
                    String msg = String.format(Messages.s_Removed_Recreating_s, fileName);
                    AdtPlugin.printErrorToConsole(mBuilder.getProject(), msg);
                } else if (kind == IResourceDelta.CHANGED) {
                    // the file was modified manually! we can't allow it.
                    String msg = String.format(Messages.s_Modified_Manually_Recreating_s, fileName);
                    AdtPlugin.printErrorToConsole(mBuilder.getProject(), msg);
                }
            }

            // no children.
            return false;
        } else if (mInRes) {
            // if we are in the res folder, we are looking for the following
            // changes:
            // - added/removed/modified xml files.
            // - added/removed files of any other type

            // if the resource is a folder, we just go straight to the
            // children
            if (resource.getType() == IResource.FOLDER) {
                return true;
            }

            // get the extension of the resource
            String ext = resource.getFileExtension();
            int kind = delta.getKind();

            String p = resource.getProjectRelativePath().toString();
            String message = null;
            switch (kind) {
                case IResourceDelta.CHANGED:
                    // display verbose message
                    message = String.format(Messages.s_Modified_Recreating_s, p,
                            AndroidConstants.FN_RESOURCE_CLASS);
                    break;
                case IResourceDelta.ADDED:
                    // display verbose message
                    message = String.format(Messages.Added_s_s_Needs_Updating, p,
                            AndroidConstants.FN_RESOURCE_CLASS);
                    break;
                case IResourceDelta.REMOVED:
                    // display verbose message
                    message = String.format(Messages.s_Removed_s_Needs_Updating, p,
                            AndroidConstants.FN_RESOURCE_CLASS);
                    break;
            }
            if (message != null) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE,
                        mBuilder.getProject(), message);
            }

            if (AndroidConstants.EXT_XML.equalsIgnoreCase(ext)) {
                if (kind != IResourceDelta.REMOVED) {
                    // check xml Validity
                    mBuilder.checkXML(resource, this);
                }

                // if we are going through this resource, it was modified
                // somehow.
                // we don't care if it was an added/removed/changed event
                mCompileResources = true;
                return false;
            } else {
                // this is a non xml resource.
                if (kind == IResourceDelta.ADDED
                        || kind == IResourceDelta.REMOVED) {
                    mCompileResources = true;
                    return false;
                }
            }
        } else if (resource instanceof IFolder) {
            // in this case we may be inside a folder that contains a source
            // folder.
            String[] sourceFolderSegments = findMatchingSourceFolder(mSourceFolders, segments);
            if (sourceFolderSegments != null) {
                // we have a match!
                mInRes = false;

                // Check if the current folder is actually a source folder
                if (sourceFolderSegments.length == segments.length) {
                    mInSrc = true;
                }
                
                // and return true to visit the content, no matter what
                return true;
            }

            // if we're here, we are visiting another folder
            // like /$Project/bin/ for instance (we get notified for changes
            // in .class!)
            // This could also be another source folder and we have found
            // R.java in a previous source folder
            // We don't want to visit its children
            return false;
        }

        return false;
    }
}
