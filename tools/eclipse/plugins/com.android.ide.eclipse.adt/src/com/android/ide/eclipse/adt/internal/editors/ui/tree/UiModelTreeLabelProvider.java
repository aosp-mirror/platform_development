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

package com.android.ide.eclipse.adt.internal.editors.ui.tree;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.ui.ErrorImageComposite;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * UiModelTreeLabelProvider is a trivial implementation of {@link ILabelProvider}
 * where elements are expected to derive from {@link UiElementNode} or
 * from {@link ElementDescriptor}.
 * 
 * It is used by both the master tree viewer and by the list in the Add... selection dialog.
 */
public class UiModelTreeLabelProvider implements ILabelProvider {

    public UiModelTreeLabelProvider() {
    }

    /**
     * Returns the element's logo with a fallback on the android logo.
     */
    public Image getImage(Object element) {
        ElementDescriptor desc = null;
        if (element instanceof ElementDescriptor) {
            Image img = ((ElementDescriptor) element).getIcon();
            if (img != null) {
                return img;
            }
        } else if (element instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) element;
            desc = node.getDescriptor();
            if (desc != null) {
                Image img = desc.getIcon();
                if (img != null) {
                    if (node.hasError()) {
                        //TODO: cache image
                        return new ErrorImageComposite(img).createImage();
                    } else {
                        return img;
                    }
                }
            }
        }
        return AdtPlugin.getAndroidLogo();
    }

    /**
     * Uses UiElementNode.shortDescription for the label for this tree item.
     */
    public String getText(Object element) {
        if (element instanceof ElementDescriptor) {
            ElementDescriptor desc = (ElementDescriptor) element;
            return desc.getUiName();
        } else if (element instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) element;
            return node.getShortDescription();
        }
        return element.toString();
    }

    public void addListener(ILabelProviderListener listener) {
        // pass
    }

    public void dispose() {
        // pass
    }

    public boolean isLabelProperty(Object element, String property) {
        // pass
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
        // pass
    }
}


