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

package com.android.ide.eclipse.adt.internal.editors.manifest.descriptors;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.model.UiManifestElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

/**
 * {@link ManifestElementDescriptor} describes an XML element node, with its
 * element name, its possible attributes, its possible child elements but also
 * its display name and tooltip.
 * 
 * This {@link ElementDescriptor} is specialized to create {@link UiManifestElementNode} UI nodes.
 */
public class ManifestElementDescriptor extends ElementDescriptor {

    /**
     * Constructs a new {@link ManifestElementDescriptor}.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models).
     */
    public ManifestElementDescriptor(String xml_name, String ui_name, String tooltip, String sdk_url,
            AttributeDescriptor[] attributes,
            ElementDescriptor[] children,
            boolean mandatory) {
        super(xml_name, ui_name, tooltip, sdk_url, attributes, children, mandatory);
    }

    /**
     * Constructs a new {@link ManifestElementDescriptor}.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     */
    public ManifestElementDescriptor(String xml_name, String ui_name, String tooltip, String sdk_url,
            AttributeDescriptor[] attributes,
            ElementDescriptor[] children) {
        super(xml_name, ui_name, tooltip, sdk_url, attributes, children, false);
    }

    /**
     * This is a shortcut for
     * ManifestElementDescriptor(xml_name, xml_name.capitalize(), null, null, null, children).
     * This constructor is mostly used for unit tests.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     */
    public ManifestElementDescriptor(String xml_name, ElementDescriptor[] children) {
        super(xml_name, children);
    }

    /**
     * This is a shortcut for
     * ManifestElementDescriptor(xml_name, xml_name.capitalize(), null, null, null, null).
     * This constructor is mostly used for unit tests.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     */
    public ManifestElementDescriptor(String xml_name) {
        super(xml_name, null);
    }

    /**
     * @return A new {@link UiElementNode} linked to this descriptor.
     */
    @Override
    public UiElementNode createUiNode() {
        return new UiManifestElementNode(this);
    }
}
