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
import com.android.ide.eclipse.adt.project.ProjectHelper;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;
import java.util.Map;

/**
 * Resource manager builder whose only purpose is to refresh the resource folder
 * so that the other builder use an up to date version.
 */
public class ResourceManagerBuilder extends BaseBuilder {

    public static final String ID = "com.android.ide.eclipse.adt.ResourceManagerBuilder"; //$NON-NLS-1$

    public ResourceManagerBuilder() {
        super();
    }

    // build() returns a list of project from which this project depends for future compilation.
    @SuppressWarnings("unchecked")
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        // Get the project.
        IProject project = getProject();

        // Clear the project of the generic markers
        BaseBuilder.removeMarkersFromProject(project, AdtConstants.MARKER_ADT);
        
        // check for existing target marker, in which case we abort.
        // (this means: no SDK, no target, or unresolvable target.)
        abortOnBadSetup(project);

        // Check the compiler compliance level, displaying the error message
        // since this is the first builder.
        int res = ProjectHelper.checkCompilerCompliance(project);
        String errorMessage = null;
        switch (res) {
            case ProjectHelper.COMPILER_COMPLIANCE_LEVEL:
                errorMessage = Messages.Requires_Compiler_Compliance_5;
            case ProjectHelper.COMPILER_COMPLIANCE_SOURCE:
                errorMessage = Messages.Requires_Source_Compatibility_5;
            case ProjectHelper.COMPILER_COMPLIANCE_CODEGEN_TARGET:
                errorMessage = Messages.Requires_Class_Compatibility_5;
        }

        if (errorMessage != null) {
            BaseProjectHelper.addMarker(project, AdtConstants.MARKER_ADT, errorMessage,
                    IMarker.SEVERITY_ERROR);
            AdtPlugin.printErrorToConsole(project, errorMessage);
            
            // interrupt the build. The next builders will not run.
            stopBuild(errorMessage);
        }

        // Check that the SDK directory has been setup.
        String osSdkFolder = AdtPlugin.getOsSdkFolder();

        if (osSdkFolder == null || osSdkFolder.length() == 0) {
            AdtPlugin.printErrorToConsole(project, Messages.No_SDK_Setup_Error);
            markProject(AdtConstants.MARKER_ADT, Messages.No_SDK_Setup_Error,
                    IMarker.SEVERITY_ERROR);

            // This interrupts the build. The next builders will not run.
            stopBuild(Messages.No_SDK_Setup_Error);
        }

        // check the project has a target
        IAndroidTarget projectTarget = Sdk.getCurrent().getTarget(project);
        if (projectTarget == null) {
            // no target. marker has been set by the container initializer: exit silently.
            // This interrupts the build. The next builders will not run.
            stopBuild("Project has no target");
        }
        
        // check the 'gen' source folder is present
        boolean hasGenSrcFolder = false; // whether the project has a 'gen' source folder setup
        IJavaProject javaProject = JavaCore.create(project);
        
        IClasspathEntry[] classpaths = javaProject.readRawClasspath();
        if (classpaths != null) {
            for (IClasspathEntry e : classpaths) {
                if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = e.getPath();
                    if (path.segmentCount() == 2 &&
                            path.segment(1).equals(SdkConstants.FD_GEN_SOURCES)) {
                        hasGenSrcFolder = true;
                        break;
                    }
                }
            }
        }

        boolean genFolderPresent = false; // whether the gen folder actually exists
        IResource resource = project.findMember(SdkConstants.FD_GEN_SOURCES);
        genFolderPresent = resource != null && resource.exists();
        
        if (hasGenSrcFolder == false && genFolderPresent) {
            // No source folder setup for 'gen' in the project, but there's already a
            // 'gen' resource (file or folder).
            String message;
            if (resource.getType() == IResource.FOLDER) {
                // folder exists already! This is an error. If the folder had been created
                // by the NewProjectWizard, it'd be a source folder.
                message = String.format("%1$s already exists but is not a source folder. Convert to a source folder or rename it.",
                        resource.getFullPath().toString());
            } else {
                // resource exists but is not a folder.
                message = String.format(
                        "Resource %1$s is in the way. ADT needs a source folder called 'gen' to work. Rename or delete resource.",
                        resource.getFullPath().toString());
            }

            AdtPlugin.printErrorToConsole(project, message);
            markProject(AdtConstants.MARKER_ADT, message, IMarker.SEVERITY_ERROR);

            // This interrupts the build. The next builders will not run.
            stopBuild(message);
        } else if (hasGenSrcFolder == false || genFolderPresent == false) {
            // either there is no 'gen' source folder in the project (older SDK),
            // or the folder does not exist (was deleted, or was a fresh svn checkout maybe.)

            // In case we are migrating from an older SDK, we go through the current source
            // folders and delete the generated Java files.
            ArrayList<IPath> sourceFolders = BaseProjectHelper.getSourceClasspaths(javaProject);
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IPath path : sourceFolders) {
                IResource member = root.findMember(path);
                if (member != null) {
                    removeDerivedResources(member, monitor);
                }
            }

            // create the new source folder, if needed
            IFolder genFolder = project.getFolder(SdkConstants.FD_GEN_SOURCES);
            if (genFolderPresent == false) {
                AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                        "Creating 'gen' source folder for generated Java files");
                genFolder.create(true /* force */, true /* local */,
                        new SubProgressMonitor(monitor, 10));
                genFolder.setDerived(true);
            }
            
            // add it to the source folder list, if needed only (or it will throw)
            if (hasGenSrcFolder == false) {
                IClasspathEntry[] entries = javaProject.getRawClasspath();
                entries = ProjectHelper.addEntryToClasspath(entries,
                        JavaCore.newSourceEntry(genFolder.getFullPath()));
                javaProject.setRawClasspath(entries, new SubProgressMonitor(monitor, 10));
            }
            
            // refresh the whole project
            project.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 10));
        }

        // Check the preference to be sure we are supposed to refresh
        // the folders.
        if (AdtPlugin.getAutoResRefresh()) {
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.Refreshing_Res);

            // refresh the res folder.
            IFolder resFolder = project.getFolder(
                    AndroidConstants.WS_RESOURCES);
            resFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);

            // Also refresh the assets folder to make sure the ApkBuilder
            // will now it's changed and will force a new resource packaging.
            IFolder assetsFolder = project.getFolder(
                    AndroidConstants.WS_ASSETS);
            assetsFolder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }

        return null;
    }
}
