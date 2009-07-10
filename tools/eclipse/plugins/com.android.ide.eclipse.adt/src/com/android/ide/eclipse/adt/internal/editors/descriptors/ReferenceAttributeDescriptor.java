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

import com.android.ide.eclipse.adt.internal.editors.ui.ResourceValueCellEditor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiResourceAttributeNode;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.sdklib.SdkConstants;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;

/**
 * Describes an XML attribute displayed containing a value or a reference to a resource.
 * It is displayed by a {@link UiResourceAttributeNode}.
 */
public final class ReferenceAttributeDescriptor extends TextAttributeDescriptor {

    /** The {@link ResourceType} that this reference attribute can accept. It can be null,
     * in which case any reference type can be used. */
    private ResourceType mResourceType;

    /**
     * Creates a reference attributes that can contain any type of resources.
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param tooltip A non-empty tooltip string or null
     */
    public ReferenceAttributeDescriptor(String xmlLocalName, String uiName, String nsUri,
            String tooltip) {
        super(xmlLocalName, uiName, nsUri, tooltip);
    }

    /**
     * Creates a reference attributes that can contain a reference to a specific
     * {@link ResourceType}.
     * @param resourceType The specific {@link ResourceType} that this reference attribute supports.
     * It can be <code>null</code>, in which case, all resource types are supported.
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param uiName The UI name of the attribute. Cannot be an empty string and cannot be null.
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     * @param tooltip A non-empty tooltip string or null
     */
    public ReferenceAttributeDescriptor(ResourceType resourceType,
            String xmlLocalName, String uiName, String nsUri,
            String tooltip) {
        super(xmlLocalName, uiName, nsUri, tooltip);
        mResourceType = resourceType;
    }


    /** Returns the {@link ResourceType} that this reference attribute can accept.
     * It can be null, in which case any reference type can be used. */
    public ResourceType getResourceType() {
        return mResourceType;
    }

    /**
     * @return A new {@link UiResourceAttributeNode} linked to this reference descriptor.
     */
    @Override
    public UiAttributeNode createUiNode(UiElementNode uiParent) {
        return new UiResourceAttributeNode(mResourceType, this, uiParent);
    }

    // ------- IPropertyDescriptor Methods

    @Override
    public CellEditor createPropertyEditor(Composite parent) {
        return new ResourceValueCellEditor(parent);
    }

}
