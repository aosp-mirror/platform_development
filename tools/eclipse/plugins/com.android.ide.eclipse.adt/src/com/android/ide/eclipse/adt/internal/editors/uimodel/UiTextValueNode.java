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

import com.android.ide.eclipse.adt.internal.editors.descriptors.TextValueDescriptor;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Represents an XML element value in that can be modified using a simple text field
 * in the XML editor's user interface.
 */
public class UiTextValueNode extends UiTextAttributeNode {

    public UiTextValueNode(TextValueDescriptor attributeDescriptor, UiElementNode uiParent) {
        super(attributeDescriptor, uiParent);
    }

    /**
     * Updates the current text field's value when the XML has changed.
     * <p/>
     * The caller doesn't really know if value of the element has changed,
     * so it will call this to refresh the value anyway. The value
     * is only set if it has changed.
     * <p/>
     * This also resets the "dirty" flag.
    */
    @Override
    public void updateValue(Node xml_attribute_node) {
        setCurrentValue(DEFAULT_VALUE);

        // The argument xml_attribute_node is not used here. It should always be
        // null since this is not an attribute. What we want is the "text value" of
        // the parent element, which is actually the first text node of the element.
        
        UiElementNode parent = getUiParent();
        if (parent != null) {
            Node xml_node = parent.getXmlNode();
            if (xml_node != null) {
                for (Node xml_child = xml_node.getFirstChild();
                    xml_child != null;
                    xml_child = xml_child.getNextSibling()) {
                    if (xml_child.getNodeType() == Node.TEXT_NODE) {
                        setCurrentValue(xml_child.getNodeValue());
                        break;
                    }
                }
            }
        }

        if (isValid() && !getTextWidgetValue().equals(getCurrentValue())) {
            try {
                setInInternalTextModification(true);
                setTextWidgetValue(getCurrentValue());
                setDirty(false);
            } finally {
                setInInternalTextModification(false);
            }
        }
    }

    /* (non-java doc)
     * Called by the user interface when the editor is saved or its state changed
     * and the modified "attributes" must be committed (i.e. written) to the XML model.
     */
    @Override
    public void commit() {
        UiElementNode parent = getUiParent();
        if (parent != null && isValid() && isDirty()) {
            // Get (or create) the underlying XML element node that contains the value.
            Node element = parent.prepareCommit();
            if (element != null) {
                String value = getTextWidgetValue();

                // Try to find an existing text child to update.
                boolean updated = false;

                for (Node xml_child = element.getFirstChild();
                        xml_child != null;
                        xml_child = xml_child.getNextSibling()) {
                    if (xml_child.getNodeType() == Node.TEXT_NODE) {
                        xml_child.setNodeValue(value);
                        updated = true;
                        break;
                    }
                }

                // If we didn't find a text child to update, we need to create one.
                if (!updated) {
                    Document doc = element.getOwnerDocument();
                    if (doc != null) {
                        Text text = doc.createTextNode(value);
                        element.appendChild(text);
                    }
                }
                
                setCurrentValue(value);
            }
        }
        setDirty(false);
    }
}
