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

package com.android.ide.eclipse.adt.launch;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

/**
 * Launch shortcut to launch debug/run configuration directly.
 */
public class LaunchShortcut implements ILaunchShortcut {


    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(
     * org.eclipse.jface.viewers.ISelection, java.lang.String)
     */
    public void launch(ISelection selection, String mode) {
        if (selection instanceof IStructuredSelection) {

            // get the object and the project from it
            IStructuredSelection structSelect = (IStructuredSelection)selection;
            Object o = structSelect.getFirstElement();

            // get the first (and normally only) element
            if (o instanceof IAdaptable) {
                IResource r = (IResource)((IAdaptable)o).getAdapter(IResource.class);

                // get the project from the resource
                if (r != null) {
                    IProject project = r.getProject();

                    if (project != null)  {
                        // and launch
                        launch(project, mode);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(
     * org.eclipse.ui.IEditorPart, java.lang.String)
     */
    public void launch(IEditorPart editor, String mode) {
        // since we force the shortcut to only work on selection in the
        // package explorer, this will never be called.
    }


    /**
     * Launch a config for the specified project.
     * @param project The project to launch
     * @param mode The launch mode ("debug", "run" or "profile")
     */
    private void launch(IProject project, String mode) {
        // get an existing or new launch configuration
        ILaunchConfiguration config = AndroidLaunchController.getLaunchConfig(project);

        if (config != null) {
            // and launch!
            DebugUITools.launch(config, mode);
        }
    }
}
