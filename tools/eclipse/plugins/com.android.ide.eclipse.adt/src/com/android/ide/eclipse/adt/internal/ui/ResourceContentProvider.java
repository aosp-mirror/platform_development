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

package com.android.ide.eclipse.adt.internal.ui;

import com.android.ide.eclipse.adt.internal.resources.IResourceRepository;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.manager.ConfigurableResourceItem;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider for the Resource Explorer TreeView.
 * Each level of the tree is represented by a different class.
 * <ul>
 * <li>{@link ResourceType}. This represents the list of existing Resource Type present
 * in the resources. This can be matched to the subclasses inside the class <code>R</code>
 * </li>
 * <ul>
 * <li>{@link ResourceItem}. This represents one resource (which can existing in various alternate
 * versions). This is similar to the resource Ids defined as <code>R.sometype.id</code>.
 * </li>
 * <ul>
 * <li>{@link ResourceFile}. (optional) This represents a particular version of the
 * {@link ResourceItem}. It is displayed as a list of resource qualifier.
 * </li>
 * </ul> 
 * </ul> 
 * </ul> 
 * 
 * @see ResourceLabelProvider
 */
public class ResourceContentProvider implements ITreeContentProvider {

    /**
     * The current ProjectResources being displayed.
     */
    private IResourceRepository mResources;
    
    private boolean mFullLevels;
    
   /**
     * Constructs a new content providers for resource display.
     * @param fullLevels if <code>true</code> the content provider will suppport all 3 levels. If
     * <code>false</code>, only two levels are provided.
     */
    public ResourceContentProvider(boolean fullLevels) {
        mFullLevels = fullLevels;
    }

    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof ResourceType) {
            return mResources.getResources((ResourceType)parentElement);
        } else if (mFullLevels && parentElement instanceof ConfigurableResourceItem) {
            return ((ConfigurableResourceItem)parentElement).getSourceFileArray();
        }
        return null;
    }

    public Object getParent(Object element) {
        // pass
        return null;
    }

    public boolean hasChildren(Object element) {
        if (element instanceof ResourceType) {
            return mResources.hasResources((ResourceType)element);
        } else if (mFullLevels && element instanceof ConfigurableResourceItem) {
            return ((ConfigurableResourceItem)element).hasAlternates();
        }
        return false;
    }

    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof IResourceRepository) {
            if ((IResourceRepository)inputElement == mResources) {
                // get the top level resources.
                return mResources.getAvailableResourceTypes();
            }
        }

        return new Object[0];
    }

    public void dispose() {
        // pass
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof IResourceRepository) {
             mResources = (IResourceRepository)newInput;
        }
    }
}
