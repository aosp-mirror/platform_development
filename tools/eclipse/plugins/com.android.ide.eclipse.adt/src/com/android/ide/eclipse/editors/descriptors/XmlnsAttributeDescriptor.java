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

package com.android.ide.eclipse.editors.descriptors;

import com.android.ide.eclipse.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;


/**
 * Describes an XMLNS attribute that is hidden.
 * <p/>
 * Such an attribute has no user interface and no corresponding {@link UiAttributeNode}.
 * It also has a single constant default value.
 * <p/>
 * When loading an XML, we'll ignore this attribute.
 * However when writing a new XML, we should always write this attribute.
 * <p/>
 * Currently this is used for the xmlns:android attribute in the manifest element.
 */
public final class XmlnsAttributeDescriptor extends AttributeDescriptor {

    /**
     * URI of the reserved "xmlns"  prefix, as described in
     * http://www.w3.org/TR/2004/REC-DOM-Level-3-Core-20040407/namespaces-algorithms.html#normalizeDocumentAlgo
     */
    public final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/"; //$NON-NLS-1$ 
    
    private String mValue;

    
    public XmlnsAttributeDescriptor(String defaultPrefix, String value) {
        super(defaultPrefix, XMLNS_URI);
        mValue = value;
    }

    /**
     * Returns the value of this specialized attribute descriptor, which is the URI associated
     * to the declared namespace prefix.
     */
    public String getValue() {
        return mValue;
    }

    /**
     * Returns the "xmlns" prefix that is always used by this node for its namespace URI.
     * This is defined by the XML specification.
     */
    public String getXmlNsPrefix() {
        return "xmlns"; //$NON-NLS-1$
    }
    
    /**
     * Returns the fully-qualified attribute name, namely "xmlns:xxx" where xxx is
     * the defaultPrefix passed in the constructor.
     */
    public String getXmlNsName() {
        return getXmlNsPrefix() + ":" + getXmlLocalName(); //$NON-NLS-1$
    }
    
    /**
     * @return Always returns null. {@link XmlnsAttributeDescriptor} has no user interface.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return null;
    }
}
