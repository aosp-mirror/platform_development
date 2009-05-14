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

import com.android.ide.eclipse.adt.internal.resources.IIdResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.manager.ConfigurableResourceItem;
import com.android.ide.eclipse.adt.internal.resources.manager.IdResourceItem;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceFile;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Label provider for the Resource Explorer TreeView.
 * Each level of the tree is represented by a different class.
 * <ul>
 * <li>{@link ResourceType}. This represents the list of existing Resource Type present
 * in the resources. This can be matched to the subclasses inside the class <code>R</code>
 * </li>
 * <ul>
 * <li>{@link ResourceItem}. This represents one resource. The actual type can be
 * {@link ConfigurableResourceItem} (which can exist in various alternate versions),
 * or {@link IdResourceItem}.
 * This is similar to the resource Ids defined as <code>R.sometype.id</code>.
 * </li>
 * <ul>
 * <li>{@link ResourceFile}. This represents a particular version of the {@link ResourceItem}.
 * It is displayed as a list of resource qualifier.
 * </li>
 * </ul> 
 * </ul> 
 * </ul> 
 * 
 * @see ResourceContentProvider
 */
public class ResourceLabelProvider implements ILabelProvider, ITableLabelProvider {
    private Image mWarningImage;
    
    public ResourceLabelProvider() {
        mWarningImage = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(
                ISharedImages.IMG_OBJS_WARN_TSK).createImage();
    }

    /**
     * @see #getColumnImage(Object, int)
     */
    public Image getImage(Object element) {
        // pass
        return null;
    }

    /**
     * @see #getColumnText(Object, int)
     */
    public String getText(Object element) {
        return getColumnText(element, 0);
    }

    public void addListener(ILabelProviderListener listener) {
        // pass
    }

    public void dispose() {
        mWarningImage.dispose();
    }

    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
        // pass
    }

    public Image getColumnImage(Object element, int columnIndex) {
        if (columnIndex == 1) {
            if (element instanceof ConfigurableResourceItem) {
                ConfigurableResourceItem item = (ConfigurableResourceItem)element;
                if (item.hasDefault() == false) {
                    return mWarningImage;
                }
            }
        }
        return null;
    }

    public String getColumnText(Object element, int columnIndex) {
        switch (columnIndex) {
            case 0:
                if (element instanceof ResourceType) {
                    return ((ResourceType)element).getDisplayName();
                } else if (element instanceof ResourceItem) {
                    return ((ResourceItem)element).getName();
                } else if (element instanceof ResourceFile) {
                    return ((ResourceFile)element).getFolder().getConfiguration().toDisplayString();
                }
                break;
            case 1:
                if (element instanceof ConfigurableResourceItem) {
                    ConfigurableResourceItem item = (ConfigurableResourceItem)element;
                    int count = item.getAlternateCount();
                    if (count > 0) {
                        if (item.hasDefault()) {
                            count++;
                        }
                        return String.format("%1$d version(s)", count);
                    }
                } else if (element instanceof IIdResourceItem) {
                    IIdResourceItem idResource = (IIdResourceItem)element;
                    if (idResource.isDeclaredInline()) {
                        return "Declared inline";
                    }
                }
                return null;
        }
        return null;
    }
}
