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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;

/**
 * A {@link ILabelDecorator} associated with an org.eclipse.ui.decorators extension.
 * This is used to add android icons in some special folders in the package explorer.
 */
public class FolderDecorator implements ILightweightLabelDecorator {
    
    private ImageDescriptor mDescriptor;

    public FolderDecorator() {
        mDescriptor = AdtPlugin.getImageDescriptor("/icons/android_project.png"); //$NON-NLS-1$
    }

    public void decorate(Object element, IDecoration decoration) {
        if (element instanceof IFolder) {
            IFolder folder = (IFolder)element;
            
            // get the project and make sure this is an android project
            IProject project = folder.getProject();

            try {
                if (project.hasNature(AndroidConstants.NATURE)) {
                    // check the folder is directly under the project.
                    if (folder.getParent().getType() == IResource.PROJECT) {
                        String name = folder.getName();
                        if (name.equals(SdkConstants.FD_ASSETS)) {
                            doDecoration(decoration, null);
                        } else if (name.equals(SdkConstants.FD_RESOURCES)) {
                            doDecoration(decoration, null);
                        } else if (name.equals(SdkConstants.FD_GEN_SOURCES)) {
                            doDecoration(decoration, " [Generated Java Files]");
                      } else if (name.equals(SdkConstants.FD_NATIVE_LIBS)) {
                          doDecoration(decoration, null);
                        }
                    }
                }
            } catch (CoreException e) {
                // log the error
                AdtPlugin.log(e, "Unable to get nature of project '%s'.", project.getName());
            }
        }
    }
    
    public void doDecoration(IDecoration decoration, String suffix) {
        decoration.addOverlay(mDescriptor, IDecoration.TOP_LEFT);

        if (suffix != null) {
            decoration.addSuffix(suffix);

            // this is broken as it changes the color of the whole text, not only of the decoration.
            // TODO: figure out how to change the color of the decoration only.
//            ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
//            ColorRegistry registry = theme.getColorRegistry();
//            decoration.setForegroundColor(
//                    registry.get("org.eclipse.jdt.ui.ColoredLabels.decorations")); //$NON-NLS-1$
        }

    }

    public boolean isLabelProperty(Object element, String property) {
        // Property change do not affect the label
        return false;
    }

    public void addListener(ILabelProviderListener listener) {
        // No state change will affect the rendering.
    }

    public void removeListener(ILabelProviderListener listener) {
        // No state change will affect the rendering.
    }

    public void dispose() {
        // nothing to dispose
    }
}
