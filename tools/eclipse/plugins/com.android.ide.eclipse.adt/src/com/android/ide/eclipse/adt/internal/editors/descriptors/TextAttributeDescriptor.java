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

import com.android.ide.eclipse.adt.internal.editors.ui.TextValueCellEditor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiTextAttributeNode;
import com.android.sdklib.SdkConstants;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.views.properties.IPropertyDescriptor;


/**
 * Describes a textual XML attribute.
 * <p/>
 * Such an attribute has a tooltip and would typically be displayed by
 * {@link UiTextAttributeNode} using a label widget and text field.
 * <p/>
 * This is the "default" kind of attribute. If in doubt, use this.
 */
public class TextAttributeDescriptor extends AttributeDescriptor implements IPropertyDescriptor {
    private String mUiName;
    private String mTooltip;
    
    /**
     * Creates a new {@link TextAttributeDescriptor}
     * 
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param tooltip A non-empty tooltip string or null
     */
    public TextAttributeDescriptor(String xmlLocalName, String uiName,
            String nsUri, String tooltip) {
        super(xmlLocalName, nsUri);
        mUiName = uiName;
        mTooltip = (tooltip != null && tooltip.length() > 0) ? tooltip : null;
    }

    /**
     * @return The UI name of the attribute. Cannot be an empty string and cannot be null.
     */
    public final String getUiName() {
        return mUiName;
    }

    /**
     * The tooltip string is either null or a non-empty string.
     * <p/>
     * The tooltip is based on the Javadoc of the attribute and already processed via
     * {@link DescriptorsUtils#formatTooltip(String)} to be displayed right away as
     * a UI tooltip.
     * <p/>
     * An empty string is converted to null, to match the behavior of setToolTipText() in
     * {@link Control}.
     * 
     * @return A non-empty tooltip string or null
     */
    public final String getTooltip() {
        return mTooltip;
    }
    
    /**
     * @return A new {@link UiTextAttributeNode} linked to this descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiTextAttributeNode(this, uiParent);
    }
    
    // ------- IPropertyDescriptor Methods

    public CellEditor createPropertyEditor(Composite parent) {
        return new TextValueCellEditor(parent);
    }

    public String getCategory() {
        if (isDeprecated()) {
            return "Deprecated";
        }

        ElementDescriptor parent = getParent();
        if (parent != null) {
            return parent.getUiName();
        }

        return null;
    }

    public String getDescription() {
        return mTooltip;
    }

    public String getDisplayName() {
        return mUiName;
    }

    public String[] getFilterFlags() {
        return null;
    }

    public Object getHelpContextIds() {
        return null;
    }

    public Object getId() {
        return this;
    }

    public ILabelProvider getLabelProvider() {
        return AttributeDescriptorLabelProvider.getProvider();
    }

    public boolean isCompatibleWith(IPropertyDescriptor anotherProperty) {
        return anotherProperty == this;
    }
}
