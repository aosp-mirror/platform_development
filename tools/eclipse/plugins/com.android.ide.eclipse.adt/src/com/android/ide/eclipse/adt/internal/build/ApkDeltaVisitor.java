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

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.build.BaseBuilder.BaseDeltaVisitor;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.util.ArrayList;

/**
 * Delta resource visitor looking for changes that will trigger a new packaging of an Android
 * application.
 * <p/>
 * This looks for the following changes:
 * <ul>
 * <li>Any change to the AndroidManifest.xml file</li>
 * <li>Any change inside the assets/ folder</li>
 * <li>Any file change inside the res/ folder</li>
 * <li>Any .class file change inside the output folder</li>
 * <li>Any change to the classes.dex inside the output folder</li>
 * <li>Any change to the packaged resources file inside the output folder</li>
 * <li>Any change to a non java/aidl file inside the source folders</li>
 * <li>Any change to .so file inside the lib (native library) folder</li>
 * </ul>
 */
public class ApkDeltaVisitor extends BaseDeltaVisitor
        implements IResourceDeltaVisitor {

    /**
     * compile flag. This is set to true if one of the changed/added/removed
     * file is a .class file. Upon visiting all the delta resources, if this
     * flag is true, then we know we'll have to make the "classes.dex" file.
     */
    private boolean mConvertToDex = false;

    /**
     * compile flag. This is set to true if one of the changed/added/removed
     * file is a resource file. Upon visiting all the delta resources, if
     * this flag is true, then we know we'll have to make the intermediate
     * apk file.
     */
    private boolean mPackageResources = false;
    
    /**
     * Final package flag. This is set to true if one of the changed/added/removed
     * file is a non java file (or aidl) in the resource folder. Upon visiting all the
     * delta resources, if this flag is true, then we know we'll have to make the final
     * package.
     */
    private boolean mMakeFinalPackage = false;

    /** List of source folders. */
    private ArrayList<IPath> mSourceFolders;

    private IPath mOutputPath;

    private IPath mAssetPath;

    private IPath mResPath;

    private IPath mLibFolder;

    /**
     * Builds the object with a specified output folder.
     * @param builder the xml builder using this object to visit the
     *  resource delta.
     * @param sourceFolders the list of source folders for the project, relative to the workspace.
     * @param outputfolder the output folder of the project.
     */
    public ApkDeltaVisitor(BaseBuilder builder, ArrayList<IPath> sourceFolders,
            IFolder outputfolder) {
        super(builder);
        mSourceFolders = sourceFolders;
        
        if (outputfolder != null) {
            mOutputPath = outputfolder.getFullPath();
        }
        
        IResource assetFolder = builder.getProject().findMember(SdkConstants.FD_ASSETS);
        if (assetFolder != null) {
            mAssetPath = assetFolder.getFullPath();
        }

        IResource resFolder = builder.getProject().findMember(SdkConstants.FD_RESOURCES);
        if (resFolder != null) {
            mResPath = resFolder.getFullPath();
        }
        
        IResource libFolder = builder.getProject().findMember(SdkConstants.FD_NATIVE_LIBS);
        if (libFolder != null) {
            mLibFolder = libFolder.getFullPath();
        }
    }

    public boolean getConvertToDex() {
        return mConvertToDex;
    }

    public boolean getPackageResources() {
        return mPackageResources;
    }
    
    public boolean getMakeFinalPackage() {
        return mMakeFinalPackage;
    }

    /**
     * {@inheritDoc}
     * @throws CoreException 
     *
     * @see org.eclipse.core.resources.IResourceDeltaVisitor
     *      #visit(org.eclipse.core.resources.IResourceDelta)
     */
    public boolean visit(IResourceDelta delta) throws CoreException {
        // if all flags are true, we can stop going through the resource delta.
        if (mConvertToDex && mPackageResources && mMakeFinalPackage) {
            return false;
        }

        // we are only going to look for changes in res/, src/ and in
        // AndroidManifest.xml since the delta visitor goes through the main
        // folder before its childre we can check when the path segment
        // count is 2 (format will be /$Project/folder) and make sure we are
        // processing res/, src/ or AndroidManifest.xml
        IResource resource = delta.getResource();
        IPath path = resource.getFullPath();
        String[] pathSegments = path.segments();
        int type = resource.getType();

        // since the delta visitor also visits the root we return true if
        // segments.length = 1
        if (pathSegments.length == 1) {
            return true;
        }

        // check the manifest.
        if (pathSegments.length == 2 &&
                AndroidConstants.FN_ANDROID_MANIFEST.equalsIgnoreCase(pathSegments[1])) {
            // if the manifest changed we have to repackage the
            // resources.
            mPackageResources = true;
            mMakeFinalPackage = true;

            // we don't want to go to the children, not like they are
            // any for this resource anyway.
            return false;
        }
        
        // check the other folders.
        if (mOutputPath != null && mOutputPath.isPrefixOf(path)) {
            // a resource changed inside the output folder.
            if (type == IResource.FILE) {
                // just check this is a .class file. Any modification will
                // trigger a change in the classes.dex file
                String ext = resource.getFileExtension();
                if (AndroidConstants.EXT_CLASS.equalsIgnoreCase(ext)) {
                    mConvertToDex = true;
                    mMakeFinalPackage = true;
    
                    // no need to check the children, as we are in a package
                    // and there can only be subpackage children containing
                    // only .class files
                    return false;
                }

                // check for a few files directly in the output folder and force
                // rebuild if they have been deleted.
                if (delta.getKind() == IResourceDelta.REMOVED) {
                    IPath parentPath = path.removeLastSegments(1);
                    if (mOutputPath.equals(parentPath)) {
                        String resourceName = resource.getName();
                        // check if classes.dex was removed
                        if (resourceName.equalsIgnoreCase(AndroidConstants.FN_CLASSES_DEX)) {
                            mConvertToDex = true;
                            mMakeFinalPackage = true;
                        } else if (resourceName.equalsIgnoreCase(
                                AndroidConstants.FN_RESOURCES_AP_) ||
                                AndroidConstants.PATTERN_RESOURCES_S_AP_.matcher(
                                        resourceName).matches()) {
                            // or if the default resources.ap_ or a configured version
                            // (resources-###.ap_) was removed.
                            mPackageResources = true;
                            mMakeFinalPackage = true;
                        }
                    }
                }
            }

            // if this is a folder, we only go visit it if we don't already know
            // that we need to convert to dex already.
            return mConvertToDex == false;
        } else if (mResPath != null && mResPath.isPrefixOf(path)) {
            // in the res folder we are looking for any file modification
            // (we don't care about folder being added/removed, only content
            // is important)
            if (type == IResource.FILE) {
                mPackageResources = true;
                mMakeFinalPackage = true;
                return false;
            }

            // for folders, return true only if we don't already know we have to
            // package the resources.
            return mPackageResources == false;
        } else if (mAssetPath != null && mAssetPath.isPrefixOf(path)) {
            // this is the assets folder that was modified.
            // we don't care what content was changed. All we care
            // about is that something changed inside. No need to visit
            // the children even.
            mPackageResources = true;
            mMakeFinalPackage = true;
            return false;
        } else if (mLibFolder != null && mLibFolder.isPrefixOf(path)) {
            // inside the native library folder. Test if the changed resource is a .so file.
            if (type == IResource.FILE &&
                    path.getFileExtension().equalsIgnoreCase(AndroidConstants.EXT_NATIVE_LIB)) {
                mMakeFinalPackage = true;
                return false; // return false for file.
            }

            // for folders, return true only if we don't already know we have to make the
            // final package.
            return mMakeFinalPackage == false;
        } else {
            // we are in a folder that is neither the resource folders, nor the output.
            // check against all the source folders, unless we already know we need to do
            // the final package.
            // This could be a source folder or a folder leading to a source folder.
            // However we only check this if we don't already know that we need to build the
            // package anyway
            if (mMakeFinalPackage == false) {
                for (IPath sourcePath : mSourceFolders) {
                    if (sourcePath.isPrefixOf(path)) {
                        // In the source folders, we are looking for any kind of
                        // modification related to file that are not java files.
                        // Also excluded are aidl files, and package.html files
                        if (type == IResource.FOLDER) {
                            // always visit the subfolders, unless the folder is not to be included
                            return ApkBuilder.checkFolderForPackaging((IFolder)resource);
                        } else if (type == IResource.FILE) {
                            if (ApkBuilder.checkFileForPackaging((IFile)resource)) {
                                mMakeFinalPackage = true;
                            }

                            return false;
                        }
                        
                    }
                }
            }
        }
        
        // if the folder is not inside one of the folders we are interested in (res, assets, output,
        // source folders), it could be a folder leading to them, so we return true.
        return true;
    }
}
