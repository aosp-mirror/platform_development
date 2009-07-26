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

package com.android.ide.eclipse.adt.internal.editors.resources.descriptors;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.resources.uimodel.UiItemElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

/**
 * {@link ItemElementDescriptor} is a special version of {@link ElementDescriptor} that
 * uses a specialized {@link UiItemElementNode} for display.
 */
public class ItemElementDescriptor extends ElementDescriptor {

    /**
     * Constructs a new {@link ItemElementDescriptor} based on its XML name, UI name,
     * tooltip, SDK url, attributes list, children list and mandatory.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param ui_name The XML element name for the user interface, typically capitalized.
     * @param tooltip An optional tooltip. Can be null or empty.
     * @param sdk_url An optional SKD URL. Can be null or empty.
     * @param attributes The list of allowed attributes. Can be null or empty.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ItemElementDescriptor(String xml_name, String ui_name,
            String tooltip, String sdk_url, AttributeDescriptor[] attributes,
            ElementDescriptor[] children, boolean mandatory) {
        super(xml_name, ui_name, tooltip, sdk_url, attributes, children, mandatory);
    }

    @Override
    public UiElementNode createUiNode() {
        return new UiItemElementNode(this);
    }
}
