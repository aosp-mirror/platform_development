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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.DocumentDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener.UiUpdateState;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Represents an XML document node that can be modified by the user interface in the XML editor.
 * <p/>
 * The structure of a given {@link UiDocumentNode} is declared by a corresponding
 * {@link DocumentDescriptor}.
 */
public class UiDocumentNode extends UiElementNode {
    
    /**
     * Creates a new {@link UiDocumentNode} described by a given {@link DocumentDescriptor}.
     * 
     * @param documentDescriptor The {@link DocumentDescriptor} for the XML node. Cannot be null.
     */
    public UiDocumentNode(DocumentDescriptor documentDescriptor) {
        super(documentDescriptor);
    }

    /**
     * Computes a short string describing the UI node suitable for tree views.
     * Uses the element's attribute "android:name" if present, or the "android:label" one
     * followed by the element's name.
     * 
     * @return A short string describing the UI node suitable for tree views.
     */
    @Override
    public String getShortDescription() {
        return "Document"; //$NON-NLS-1$
    }
    
    /**
     * Computes a "breadcrumb trail" description for this node.
     * 
     * @param include_root Whether to include the root (e.g. "Manifest") or not. Has no effect
     *                     when called on the root node itself.
     * @return The "breadcrumb trail" description for this node.
     */
    @Override
    public String getBreadcrumbTrailDescription(boolean include_root) {
        return "Document"; //$NON-NLS-1$
    }
    
    /**
     * This method throws an exception when attempted to assign a parent, since XML documents
     * cannot have a parent. It is OK to assign null.
     */
    @Override
    protected void setUiParent(UiElementNode parent) {
        if (parent != null) {
            // DEBUG. Change to log warning.
            throw new UnsupportedOperationException("Documents can't have UI parents"); //$NON-NLS-1$
        }
        super.setUiParent(null);
    }

    /**
     * Populate this element node with all values from the given XML node.
     * 
     * This fails if the given XML node has a different element name -- it won't change the
     * type of this ui node.
     * 
     * This method can be both used for populating values the first time and updating values
     * after the XML model changed.
     * 
     * @param xml_node The XML node to mirror
     * @return Returns true if the XML structure has changed (nodes added, removed or replaced)
     */
    @Override
    public boolean loadFromXmlNode(Node xml_node) {
        boolean structure_changed = (getXmlDocument() != xml_node);
        setXmlDocument((Document) xml_node);
        structure_changed |= super.loadFromXmlNode(xml_node);
        if (structure_changed) {
            invokeUiUpdateListeners(UiUpdateState.CHILDREN_CHANGED);
        }
        return structure_changed;
    }
    
    /**
     * This method throws an exception if there is no underlying XML document.
     * <p/>
     * XML documents cannot be created per se -- they are a by-product of the StructuredEditor
     * XML parser.
     * 
     * @return The current value of getXmlDocument().
     */
    @Override
    public Node createXmlNode() {
        if (getXmlDocument() == null) {
            // By design, a document node cannot be created, it is owned by the XML parser.
            // By "design" this should never happen since the XML parser always creates an XML
            // document container, even for an empty file.
            throw new UnsupportedOperationException("Documents cannot be created"); //$NON-NLS-1$
        }
        return getXmlDocument();
    }

    /**
     * This method throws an exception and does not even try to delete the XML document.
     * <p/>
     * XML documents cannot be deleted per se -- they are a by-product of the StructuredEditor
     * XML parser.
     * 
     * @return The removed node or null if it didn't exist in the firtst place. 
     */
    @Override
    public Node deleteXmlNode() {
        // DEBUG. Change to log warning.
        throw new UnsupportedOperationException("Documents cannot be deleted"); //$NON-NLS-1$
    }
}

