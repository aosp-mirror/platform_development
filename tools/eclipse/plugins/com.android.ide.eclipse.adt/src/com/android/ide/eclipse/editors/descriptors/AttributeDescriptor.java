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

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.editors.IconFactory;
import com.android.ide.eclipse.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.sdklib.SdkConstants;

import org.eclipse.swt.graphics.Image;

/**
 * {@link AttributeDescriptor} describes an XML attribute with its XML attribute name.
 * <p/>
 * An attribute descriptor also knows which UI node should be instantiated to represent
 * this particular attribute (e.g. text field, icon chooser, class selector, etc.)
 * Some attributes may be hidden and have no user interface at all.
 * <p/>
 * This is an abstract class. Derived classes must implement data description and return
 * the correct UiAttributeNode-derived class.
 */
public abstract class AttributeDescriptor {
    private String mXmlLocalName;
    private ElementDescriptor mParent;
    private final String mNsUri;
    private boolean mDeprecated;
    
    /**
     * Creates a new {@link AttributeDescriptor}
     * 
     * @param xmlLocalName The XML name of the attribute (case sensitive)
     * @param nsUri The URI of the attribute. Can be null if attribute has no namespace.
     *              See {@link SdkConstants#NS_RESOURCES} for a common value.
     */
    public AttributeDescriptor(String xmlLocalName, String nsUri) {
        mXmlLocalName = xmlLocalName;
        mNsUri = nsUri;
    }

    /**
     * Returns the XML local name of the attribute (case sensitive)
     */
    public final String getXmlLocalName() {
        return mXmlLocalName;
    }
    
    public final String getNamespaceUri() {
        return mNsUri;
    }
    
    final void setParent(ElementDescriptor parent) {
        mParent = parent;
    }
    
    public final ElementDescriptor getParent() {
        return mParent;
    }

    public void setDeprecated(boolean isDeprecated) {
        mDeprecated = isDeprecated;
    }
    
    public boolean isDeprecated() {
        return mDeprecated;
    }

    /** 
     * Returns an optional icon for the attribute.
     * <p/>
     * By default this tries to return an icon based on the XML name of the attribute.
     * If this fails, it tries to return the default Android logo as defined in the
     * plugin. If all fails, it returns null.
     * 
     * @return An icon for this element or null.
     */
    public Image getIcon() {
        IconFactory factory = IconFactory.getInstance();
        Image icon;
        icon = factory.getIcon(getXmlLocalName(), IconFactory.COLOR_RED, IconFactory.SHAPE_CIRCLE);
        return icon != null ? icon : AdtPlugin.getAndroidLogo();
    }
    
    /**
     * @param uiParent The {@link UiElementNode} parent of this UI attribute.
     * @return A new {@link UiAttributeNode} linked to this descriptor or null if this
     *         attribute has no user interface.
     */
    public abstract UiAttributeNode createUiNode(UiElementNode uiParent);
}    
