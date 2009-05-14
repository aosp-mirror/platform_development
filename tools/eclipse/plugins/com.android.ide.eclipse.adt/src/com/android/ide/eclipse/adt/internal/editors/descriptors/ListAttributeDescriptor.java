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

package com.android.ide.eclipse.adt.internal.editors.descriptors;

import com.android.ide.eclipse.adt.internal.editors.ui.ListValueCellEditor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiListAttributeNode;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Describes a text attribute that can contains some predefined values.
 * It is displayed by a {@link UiListAttributeNode}.
 */
public class ListAttributeDescriptor extends TextAttributeDescriptor {

    private String[] mValues = null;
    
    /**
     * Creates a new {@link ListAttributeDescriptor} which automatically gets its
     * values from the FrameworkResourceManager.
     */
    public ListAttributeDescriptor(String xmlLocalName, String uiName, String nsUri,
            String tooltip) {
        super(xmlLocalName, uiName, nsUri, tooltip);
    }

     /**
     * Creates a new {@link ListAttributeDescriptor} which uses the provided values.
     */
    public ListAttributeDescriptor(String xmlLocalName, String uiName, String nsUri, 
            String tooltip, String[] values) {
        super(xmlLocalName, uiName, nsUri, tooltip);
        mValues = values;
    }
   
    public String[] getValues() {
        return mValues;
    }

    /**
     * @return A new {@link UiListAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiListAttributeNode(this, uiParent);
    }
    
    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new ListValueCellEditor(parent);
    }
}
