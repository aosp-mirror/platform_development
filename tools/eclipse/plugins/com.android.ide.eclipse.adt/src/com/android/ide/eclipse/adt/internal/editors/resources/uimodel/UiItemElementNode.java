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

package com.android.ide.eclipse.adt.internal.editors.resources.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.resources.descriptors.ItemElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.resources.descriptors.ResourcesDescriptors;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * {@link UiItemElementNode} is apecial version of {@link UiElementNode} that 
 * customizes the element display to include the item type attribute if present.
 */
public class UiItemElementNode extends UiElementNode {

    /**
     * Creates a new {@link UiElementNode} described by a given {@link ItemElementDescriptor}.
     * 
     * @param elementDescriptor The {@link ItemElementDescriptor} for the XML node. Cannot be null.
     */
    public UiItemElementNode(ItemElementDescriptor elementDescriptor) {
        super(elementDescriptor);
    }

    @Override
    public String getShortDescription() {
        Node xmlNode = getXmlNode();
        if (xmlNode != null && xmlNode instanceof Element && xmlNode.hasAttributes()) {

            Element elem = (Element) xmlNode;
            String type = elem.getAttribute(ResourcesDescriptors.TYPE_ATTR);
            String name = elem.getAttribute(ResourcesDescriptors.NAME_ATTR);
            if (type != null && name != null && type.length() > 0 && name.length() > 0) {
                type = DescriptorsUtils.capitalize(type);
                return String.format("%1$s (%2$s %3$s)", name, type, getDescriptor().getUiName());
            }
        }

        return super.getShortDescription();
    }
}
