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

package com.android.ide.eclipse.adt.internal.actions;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.project.ProjectHelper;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import java.util.Iterator;

/**
 * Converts a project created with the activity creator into an
 * Android project.
 */
public class ConvertToAndroidAction implements IObjectActionDelegate {

    private ISelection mSelection;

    /*
     * (non-Javadoc)
     * 
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
        // pass
    }

    /*
     * (non-Javadoc)
     * 
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {
            for (Iterator<?> it = ((IStructuredSelection)mSelection).iterator(); it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject)element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject)((IAdaptable)element).getAdapter(IProject.class);
                }
                if (project != null) {
                    convertProject(project);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        this.mSelection = selection;
    }

    /**
     * Toggles sample nature on a project
     * 
     * @param project to have sample nature added or removed
     */
    private void convertProject(final IProject project) {
        new Job("Convert Project") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (monitor != null) {
                        monitor.beginTask(String.format(
                                "Convert %1$s to Android", project.getName()), 5);
                    }

                    IProjectDescription description = project.getDescription();
                    String[] natures = description.getNatureIds();

                    // check if the project already has the android nature.
                    for (int i = 0; i < natures.length; ++i) {
                        if (AndroidConstants.NATURE.equals(natures[i])) {
                            // we shouldn't be here as the visibility of the item
                            // is dependent on the project.
                            return new Status(Status.WARNING, AdtPlugin.PLUGIN_ID,
                                    "Project is already an Android project");
                        }
                    }

                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    String[] newNatures = new String[natures.length + 1];
                    System.arraycopy(natures, 0, newNatures, 1, natures.length);
                    newNatures[0] = AndroidConstants.NATURE;

                    // set the new nature list in the project
                    description.setNatureIds(newNatures);
                    project.setDescription(description, null);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    // Fix the classpath entries.
                    // get a java project
                    IJavaProject javaProject = JavaCore.create(project);
                    ProjectHelper.fixProjectClasspathEntries(javaProject);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    return Status.OK_STATUS;
                } catch (JavaModelException e) {
                    return e.getJavaModelStatus();
                } catch (CoreException e) {
                    return e.getStatus();
                } finally {
                    if (monitor != null) {
                        monitor.done();
                    }
                }
            }
        }.schedule();
    }
}
