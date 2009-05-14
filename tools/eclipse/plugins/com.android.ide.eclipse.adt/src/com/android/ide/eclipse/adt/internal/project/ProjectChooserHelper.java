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

package com.android.ide.eclipse.adt.internal.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * Helper class to deal with displaying a project choosing dialog that lists only the
 * projects with the Android nature.
 */
public class ProjectChooserHelper {

    private final Shell mParentShell;

    /**
     * List of current android projects. Since the dialog is modal, we'll just get
     * the list once on-demand.
     */
    private IJavaProject[] mAndroidProjects;

    public ProjectChooserHelper(Shell parentShell) {
        mParentShell = parentShell;
    }
    /**
     * Displays a project chooser dialog which lists all available projects with the Android nature.
     * <p/>
     * The list of project is built from Android flagged projects currently opened in the workspace.
     *
     * @param projectName If non null and not empty, represents the name of an Android project
     *                    that will be selected by default.
     * @return the project chosen by the user in the dialog, or null if the dialog was canceled.
     */
    public IJavaProject chooseJavaProject(String projectName) {
        ILabelProvider labelProvider = new JavaElementLabelProvider(
                JavaElementLabelProvider.SHOW_DEFAULT);
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                mParentShell, labelProvider);
        dialog.setTitle("Project Selection");
        dialog.setMessage("Select a project to constrain your search.");

        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);

        // set the elements in the dialog. These are opened android projects.
        dialog.setElements(getAndroidProjects(javaModel));

        // look for the project matching the given project name
        IJavaProject javaProject = null;
        if (projectName != null && projectName.length() > 0) {
            javaProject = javaModel.getJavaProject(projectName);
        }

        // if we found it, we set the initial selection in the dialog to this one.
        if (javaProject != null) {
            dialog.setInitialSelections(new Object[] { javaProject });
        }

        // open the dialog and return the object selected if OK was clicked, or null otherwise
        if (dialog.open() == Window.OK) {
            return (IJavaProject) dialog.getFirstResult();
        }
        return null;
    }
    
    /**
     * Returns the list of Android projects.
     * <p/>
     * Because this list can be time consuming, this class caches the list of project.
     * It is recommended to call this method instead of
     * {@link BaseProjectHelper#getAndroidProjects()}.
     * 
     * @param javaModel the java model. Can be null.
     */
    public IJavaProject[] getAndroidProjects(IJavaModel javaModel) {
        if (mAndroidProjects == null) {
            if (javaModel == null) {
                mAndroidProjects = BaseProjectHelper.getAndroidProjects();
            } else {
                mAndroidProjects = BaseProjectHelper.getAndroidProjects(javaModel);
            }
        }
        
        return mAndroidProjects;
    }
    
    /**
     * Helper method to get the Android project with the given name
     * 
     * @param projectName the name of the project to find
     * @return the {@link IProject} for the Android project. <code>null</code> if not found.
     */
    public IProject getAndroidProject(String projectName) {
        IProject iproject = null;
        IJavaProject[] javaProjects = getAndroidProjects(null);
        if (javaProjects != null) {
            for (IJavaProject javaProject : javaProjects) {
                if (javaProject.getElementName().equals(projectName)) {
                    iproject = javaProject.getProject();
                    break;
                }
            }
        }    
        return iproject;
    }
}
