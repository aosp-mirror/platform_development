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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;

import org.w3c.dom.Node;

/**
 * Represents an XML attribute in that can be modified using a simple text field
 * in the XML editor's user interface.
 * <p/>
 * The XML attribute has no default value. When unset, the text field is blank.
 * When updating the XML, if the field is empty, the attribute will be removed
 * from the XML element.  
 * <p/>
 * See {@link UiAttributeNode} for more information.
 */
public abstract class UiAbstractTextAttributeNode extends UiAttributeNode
    implements IUiSettableAttributeNode {

    protected static final String DEFAULT_VALUE = "";  //$NON-NLS-1$

    /** Prevent internal listener from firing when internally modifying the text */
    private boolean mInternalTextModification;
    /** Last value read from the XML model. Cannot be null. */
    private String mCurrentValue = DEFAULT_VALUE;

    public UiAbstractTextAttributeNode(AttributeDescriptor attributeDescriptor,
            UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }
    
    /** Returns the current value of the node. */
    @Override
    public final String getCurrentValue() {
        return mCurrentValue;
    }
    
    /** Sets the current value of the node. Cannot be null (use an empty string). */
    public final void setCurrentValue(String value) {
        mCurrentValue = value;
    }
    
    /** Returns if the attribute node is valid, and its UI has been created. */
    public abstract boolean isValid();

    /** Returns the text value present in the UI. */
    public abstract String getTextWidgetValue();
    
    /** Sets the text value to be displayed in the UI. */
    public abstract void setTextWidgetValue(String value);
    

    /**
     * Updates the current text field's value when the XML has changed.
     * <p/>
     * The caller doesn't really know if attributes have changed,
     * so it will call this to refresh the attribute anyway. The value
     * is only set if it has changed.
     * <p/>
     * This also resets the "dirty" flag.
    */
    @Override
    public void updateValue(Node xml_attribute_node) {
        mCurrentValue = DEFAULT_VALUE;
        if (xml_attribute_node != null) {
            mCurrentValue = xml_attribute_node.getNodeValue();
        }

        if (isValid() && !getTextWidgetValue().equals(mCurrentValue)) {
            try {
                mInternalTextModification = true;
                setTextWidgetValue(mCurrentValue);
                setDirty(false);
            } finally {
                mInternalTextModification = false;
            }
        }
    }

    /* (non-java doc)
     * Called by the user interface when the editor is saved or its state changed
     * and the modified attributes must be committed (i.e. written) to the XML model.
     */
    @Override
    public void commit() {
        UiElementNode parent = getUiParent();
        if (parent != null && isValid() && isDirty()) {
            String value = getTextWidgetValue();
            if (parent.commitAttributeToXml(this, value)) {
                mCurrentValue = value;
                setDirty(false);
            }
        }
    }
    
    protected final boolean isInInternalTextModification() {
        return mInternalTextModification;
    }
    
    protected final void setInInternalTextModification(boolean internalTextModification) {
        mInternalTextModification = internalTextModification;
    }
}
