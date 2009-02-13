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
import com.android.ide.eclipse.adt.sdk.LoadStatus;
import com.android.ide.eclipse.adt.sdk.Sdk;
import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.BaseProjectHelper;
import com.android.sdklib.IAndroidTarget;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

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
    @SuppressWarnings("unchecked") //$NON-NLS-1$
    @Override
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
            throws CoreException {
        // Get the project.
        IProject project = getProject();

        // Clear the project of the generic markers
        BaseBuilder.removeMarkersFromProject(project, AdtConstants.MARKER_ADT);

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
            AdtPlugin.printBuildToConsole(AdtConstants.BUILD_VERBOSE, project,
                    Messages.No_SDK_Setup_Error);
            markProject(AdtConstants.MARKER_ADT, Messages.No_SDK_Setup_Error,
                    IMarker.SEVERITY_ERROR);

            // This interrupts the build. The next builders will not run.
            stopBuild(Messages.No_SDK_Setup_Error);
        }

        // check if we have finished loading the SDK.
        if (AdtPlugin.getDefault().getSdkLoadStatus() != LoadStatus.LOADED) {
            // we exit silently
            // This interrupts the build. The next builders will not run.
            stopBuild("SDK is not loaded yet");
        }
        
        // check the project has a target
        IAndroidTarget projectTarget = Sdk.getCurrent().getTarget(project);
        if (projectTarget == null) {
            // no target. marker has been set by the container initializer: exit silently.
            // This interrupts the build. The next builders will not run.
            stopBuild("Project has no target");
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
