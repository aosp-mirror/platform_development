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

package com.android.ide.eclipse.adt.internal.editors.descriptors;

import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAbstractTextAttributeNode;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

/**
 * Label provider for {@link UiAbstractTextAttributeNode}.
 */
public class AttributeDescriptorLabelProvider implements ILabelProvider {
    
    private final static AttributeDescriptorLabelProvider sThis =
        new AttributeDescriptorLabelProvider();
    
    public static ILabelProvider getProvider() {
        return sThis;
    }

    public Image getImage(Object element) {
        if (element instanceof UiAbstractTextAttributeNode) {
            UiAbstractTextAttributeNode node = (UiAbstractTextAttributeNode) element;
            if (node.getDescriptor().isDeprecated()) {
                String v = node.getCurrentValue();
                if (v != null && v.length() > 0) {
                    IconFactory factory = IconFactory.getInstance();
                    return factory.getIcon("warning"); //$NON-NLS-1$
                }                
            }
        }

        return null;
    }

    public String getText(Object element) {
        if (element instanceof UiAbstractTextAttributeNode) {
            return ((UiAbstractTextAttributeNode)element).getCurrentValue();
        }

        return null;
    }

    public void addListener(ILabelProviderListener listener) {
        // TODO Auto-generated method stub

    }

    public void dispose() {
        // TODO Auto-generated method stub

    }

    public boolean isLabelProperty(Object element, String property) {
        // TODO Auto-generated method stub
        return false;
    }

    public void removeListener(ILabelProviderListener listener) {
        // TODO Auto-generated method stub

    }

}
