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

package com.android.ide.eclipse.adt.internal.editors.manifest.model;

import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.adt.internal.editors.manifest.descriptors.ManifestElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.sdklib.SdkConstants;

import org.w3c.dom.Element;

/**
 * Represents an XML node that can be modified by the user interface in the XML editor.
 * <p/>
 * Each tree viewer used in the application page's parts needs to keep a model representing
 * each underlying node in the tree. This interface represents the base type for such a node.
 * <p/>
 * Each node acts as an intermediary model between the actual XML model (the real data support)
 * and the tree viewers or the corresponding page parts.
 * <p/>
 * Element nodes don't contain data per se. Their data is contained in their attributes
 * as well as their children's attributes, see {@link UiAttributeNode}.
 * <p/>
 * The structure of a given {@link UiElementNode} is declared by a corresponding
 * {@link ElementDescriptor}.
 */
public final class UiManifestElementNode extends UiElementNode {

    /**
     * Creates a new {@link UiElementNode} described by a given {@link ElementDescriptor}.
     *
     * @param elementDescriptor The {@link ElementDescriptor} for the XML node. Cannot be null.
     */
    public UiManifestElementNode(ManifestElementDescriptor elementDescriptor) {
        super(elementDescriptor);
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
        AndroidTargetData target = getAndroidTarget();
        AndroidManifestDescriptors manifestDescriptors = null;
        if (target != null) {
            manifestDescriptors = target.getManifestDescriptors();
        }

        if (manifestDescriptors != null &&
                getXmlNode() != null &&
                getXmlNode() instanceof Element &&
                getXmlNode().hasAttributes()) {


            // Application and Manifest nodes have a special treatment: they are unique nodes
            // so we don't bother trying to differentiate their strings and we fall back to
            // just using the UI name below.
            ElementDescriptor desc = getDescriptor();
            if (desc != manifestDescriptors.getManifestElement() &&
                    desc != manifestDescriptors.getApplicationElement()) {
                Element elem = (Element) getXmlNode();
                String attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                                  AndroidManifestDescriptors.ANDROID_NAME_ATTR);
                if (attr == null || attr.length() == 0) {
                    attr = elem.getAttributeNS(SdkConstants.NS_RESOURCES,
                                               AndroidManifestDescriptors.ANDROID_LABEL_ATTR);
                }
                if (attr != null && attr.length() > 0) {
                    return String.format("%1$s (%2$s)", attr, getDescriptor().getUiName());
                }
            }
        }

        return String.format("%1$s", getDescriptor().getUiName());
    }
}

