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

import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

/**
 * {@link DocumentDescriptor} describes the properties expected for an XML document node.
 * 
 * Compared to ElementDescriptor, {@link DocumentDescriptor} does not have XML name nor UI name,
 * tooltip, SDK url and attributes list.
 * <p/>
 * It has a children list which represent all the possible roots of the document.
 * <p/>
 * The document nodes are "mandatory", meaning the UI node is never deleted and it may lack
 * an actual XML node attached.
 */
public class DocumentDescriptor extends ElementDescriptor {

    /**
     * Constructs a new {@link DocumentDescriptor} based on its XML name and children list.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     * <p/>
     * The XML name is never shown in the UI directly. It is however used when an icon
     * needs to be found for the node.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param children The list of allowed children. Can be null or empty.
     */
    public DocumentDescriptor(String xml_name, ElementDescriptor[] children) {
        super(xml_name, children, true /* mandatory */);
    }

    /**
     * @return A new {@link UiElementNode} linked to this descriptor.
     */
    @Override
    public UiElementNode createUiNode() {
        return new UiDocumentNode(this);
    }
}
