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

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import java.util.ArrayList;

/**
 * UiModelTreeContentProvider is a trivial implementation of {@link ITreeContentProvider}
 * where elements are expected to be instances of {@link UiElementNode}.
 */
class UiModelTreeContentProvider implements ITreeContentProvider {
    
    /** The descriptor of the elements to be displayed as root in this tree view. All elements
     *  of the same type in the root will be displayed. */
    private ElementDescriptor[] mDescriptorFilters;
    /** The uiRootNode of the model. */
    private final UiElementNode mUiRootNode;

    public UiModelTreeContentProvider(UiElementNode uiRootNode,
            ElementDescriptor[] descriptorFilters) {
        mUiRootNode = uiRootNode;
        mDescriptorFilters = descriptorFilters;
    }
    
    /* (non-java doc)
     * Returns all the UI node children of the given element or null if not the right kind
     * of object. */
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) parentElement;
            return node.getUiChildren().toArray();
        }
        return null;
    }

    /* (non-java doc)
     * Returns the parent of a given UI node or null if it's a root node or it's not the
     * right kind of node. */
    public Object getParent(Object element) {
        if (element instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) element;
            return node.getUiParent();
        }
        return null;
    }

    /* (non-java doc)
     * Returns true if the UI node has any UI children nodes. */
    public boolean hasChildren(Object element) {
        if (element instanceof UiElementNode) {
            UiElementNode node = (UiElementNode) element;
            return node.getUiChildren().size() > 0;
        }
        return false;
    }

    /* (non-java doc)
     * Get root elements for the tree. These are all the UI nodes that
     * match the filter descriptor in the current root node.
     * <p/>
     * Although not documented, it seems this method should not return null.
     * At worse, it should return new Object[0].
     * <p/>
     * inputElement is not currently used. The root node and the filter are given
     * by the enclosing class.
     */
    public Object[] getElements(Object inputElement) {
        ArrayList<UiElementNode> roots = new ArrayList<UiElementNode>();
        if (mUiRootNode != null) {
            for (UiElementNode ui_node : mUiRootNode.getUiChildren()) {
                if (mDescriptorFilters == null || mDescriptorFilters.length == 0) {
                    roots.add(ui_node);
                } else {
                    for (ElementDescriptor filter : mDescriptorFilters) {
                        if (ui_node.getDescriptor() == filter) {
                            roots.add(ui_node);
                        }
                    }
                }
            }
        }
        
        return roots.toArray();
    }

    public void dispose() {
        // pass
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // pass
    }
}

