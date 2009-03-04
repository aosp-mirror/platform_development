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

package com.android.ide.eclipse.adt.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import java.util.Iterator;

/**
 * Action to fix the project properties:
 * <ul>
 * <li>Make sure the framework archive is present with the link to the java
 * doc</li>
 * </ul>
 */
public class FixProjectAction implements IObjectActionDelegate {

    private ISelection mSelection;

    /**
     * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
     */
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void run(IAction action) {
        if (mSelection instanceof IStructuredSelection) {

            for (Iterator<?> it = ((IStructuredSelection) mSelection).iterator();
                    it.hasNext();) {
                Object element = it.next();
                IProject project = null;
                if (element instanceof IProject) {
                    project = (IProject) element;
                } else if (element instanceof IAdaptable) {
                    project = (IProject) ((IAdaptable) element)
                            .getAdapter(IProject.class);
                }
                if (project != null) {
                    fixProject(project);
                }
            }
        }
    }

    public void selectionChanged(IAction action, ISelection selection) {
        this.mSelection = selection;
    }

    private void fixProject(final IProject project) {
        new Job("Fix Project Properties") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    if (monitor != null) {
                        monitor.beginTask("Fix Project Properties", 6);
                    }

                    ProjectHelper.fixProject(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }
                    
                    // fix the nature order to have the proper project icon
                    ProjectHelper.fixProjectNatureOrder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    // now we fix the builders
                    AndroidNature.configureResourceManagerBuilder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    AndroidNature.configurePreBuilder(project);
                    if (monitor != null) {
                        monitor.worked(1);
                    }

                    AndroidNature.configureApkBuilder(project);
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

    /**
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        // pass
    }

}
