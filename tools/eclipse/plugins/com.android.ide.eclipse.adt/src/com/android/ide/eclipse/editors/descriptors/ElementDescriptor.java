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
import com.android.ide.eclipse.editors.uimodel.UiElementNode;
import com.android.sdklib.SdkConstants;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link ElementDescriptor} describes the properties expected for a given XML element node.
 * 
 * {@link ElementDescriptor} have an XML name, UI name, a tooltip, an SDK url,
 * an attributes list and a children list.
 * 
 * An UI node can be "mandatory", meaning the UI node is never deleted and it may lack
 * an actual XML node attached. A non-mandatory UI node MUST have an XML node attached
 * and it will cease to exist when the XML node ceases to exist.
 */
public class ElementDescriptor {
    /** The XML element node name. Case sensitive. */
    private String mXmlName;
    /** The XML element name for the user interface, typically capitalized. */
    private String mUiName;
    /** The list of allowed attributes. */
    private AttributeDescriptor[] mAttributes;
    /** The list of allowed children */
    private ElementDescriptor[] mChildren;
    /* An optional tooltip. Can be empty. */
    private String mTooltip;
    /** An optional SKD URL. Can be empty. */
    private String mSdkUrl;
    /** Whether this UI node must always exist (even for empty models). */
    private boolean mMandatory;

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name, UI name,
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
    public ElementDescriptor(String xml_name, String ui_name, String tooltip, String sdk_url,
            AttributeDescriptor[] attributes,
            ElementDescriptor[] children,
            boolean mandatory) {
        mMandatory = mandatory;
        mXmlName = xml_name;
        mUiName = ui_name;
        mTooltip = (tooltip != null && tooltip.length() > 0) ? tooltip : null;
        mSdkUrl = (sdk_url != null && sdk_url.length() > 0) ? sdk_url : null;
        setAttributes(attributes != null ? attributes : new AttributeDescriptor[]{});
        mChildren = children != null ? children : new ElementDescriptor[]{};
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name and children list.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param children The list of allowed children. Can be null or empty.
     * @param mandatory Whether this node must always exist (even for empty models). A mandatory
     *  UI node is never deleted and it may lack an actual XML node attached. A non-mandatory
     *  UI node MUST have an XML node attached and it will cease to exist when the XML node
     *  ceases to exist.
     */
    public ElementDescriptor(String xml_name, ElementDescriptor[] children, boolean mandatory) {
        this(xml_name, prettyName(xml_name), null, null, null, children, mandatory);
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name and children list.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     * @param children The list of allowed children. Can be null or empty.
     */
    public ElementDescriptor(String xml_name, ElementDescriptor[] children) {
        this(xml_name, prettyName(xml_name), null, null, null, children, false);
    }

    /**
     * Constructs a new {@link ElementDescriptor} based on its XML name.
     * The UI name is build by capitalizing the XML name.
     * The UI nodes will be non-mandatory.
     * 
     * @param xml_name The XML element node name. Case sensitive.
     */
    public ElementDescriptor(String xml_name) {
        this(xml_name, prettyName(xml_name), null, null, null, null, false);
    }

    /** Returns whether this node must always exist (even for empty models) */
    public boolean isMandatory() {
        return mMandatory;
    }
    
    /**
     * Returns the XML element node local name (case sensitive)
     */
    public final String getXmlLocalName() {
        int pos = mXmlName.indexOf(':'); 
        if (pos != -1) {
            return mXmlName.substring(pos+1);
        }
        return mXmlName;
    }

    /** Returns the XML element node name. Case sensitive. */
    public String getXmlName() {
        return mXmlName;
    }
    
    /**
     * Returns the namespace of the attribute.
     */
    public final String getNamespace() {
        // For now we hard-code the prefix as being "android"
        if (mXmlName.startsWith("android:")) { //$NON-NLs-1$
            return SdkConstants.NS_RESOURCES;
        }
        
        return ""; //$NON-NLs-1$
    }


    /** Returns the XML element name for the user interface, typically capitalized. */
    public String getUiName() {
        return mUiName;
    }

    /** 
     * Returns an optional icon for the element.
     * <p/>
     * By default this tries to return an icon based on the XML name of the element.
     * If this fails, it tries to return the default Android logo as defined in the
     * plugin. If all fails, it returns null.
     * 
     * @return An icon for this element or null.
     */
    public Image getIcon() {
        IconFactory factory = IconFactory.getInstance();
        int color = hasChildren() ? IconFactory.COLOR_BLUE : IconFactory.COLOR_GREEN;
        int shape = hasChildren() ? IconFactory.SHAPE_RECT : IconFactory.SHAPE_CIRCLE;
        Image icon = factory.getIcon(mXmlName, color, shape);
        return icon != null ? icon : AdtPlugin.getAndroidLogo();
    }

    /** 
     * Returns an optional ImageDescriptor for the element.
     * <p/>
     * By default this tries to return an image based on the XML name of the element.
     * If this fails, it tries to return the default Android logo as defined in the
     * plugin. If all fails, it returns null.
     * 
     * @return An ImageDescriptor for this element or null.
     */
    public ImageDescriptor getImageDescriptor() {
        IconFactory factory = IconFactory.getInstance();
        int color = hasChildren() ? IconFactory.COLOR_BLUE : IconFactory.COLOR_GREEN;
        int shape = hasChildren() ? IconFactory.SHAPE_RECT : IconFactory.SHAPE_CIRCLE;
        ImageDescriptor id = factory.getImageDescriptor(mXmlName, color, shape);
        return id != null ? id : AdtPlugin.getAndroidLogoDesc();
    }

    /* Returns the list of allowed attributes. */
    public AttributeDescriptor[] getAttributes() {
        return mAttributes;
    }
    
    /* Sets the list of allowed attributes. */
    public void setAttributes(AttributeDescriptor[] attributes) {
        mAttributes = attributes;
        for (AttributeDescriptor attribute : attributes) {
            attribute.setParent(this);
        }
    }

    /** Returns the list of allowed children */
    public ElementDescriptor[] getChildren() {
        return mChildren;
    }

    /** @return True if this descriptor has children available */
    public boolean hasChildren() {
        return mChildren.length > 0;
    }

    /** Sets the list of allowed children. */
    public void setChildren(ElementDescriptor[] newChildren) {
        mChildren = newChildren;
    }

    /** Sets the list of allowed children.
     * <p/>
     * This is just a convenience method that converts a Collection into an array and
     * calls {@link #setChildren(ElementDescriptor[])}.
     * <p/>
     * This means a <em>copy</em> of the collection is made. The collection is not
     * stored by the recipient and can thus be altered by the caller.
     */
    public void setChildren(Collection<ElementDescriptor> newChildren) {
        setChildren(newChildren.toArray(new ElementDescriptor[newChildren.size()]));
    }

    /**
     * Returns an optional tooltip. Will be null if not present.
     * <p/>
     * The tooltip is based on the Javadoc of the element and already processed via
     * {@link DescriptorsUtils#formatTooltip(String)} to be displayed right away as
     * a UI tooltip.
     */
    public String getTooltip() {
        return mTooltip;
    }

    /** Returns an optional SKD URL. Will be null if not present. */
    public String getSdkUrl() {
        return mSdkUrl;
    }

    /** Sets the optional tooltip. Can be null or empty. */
    public void setTooltip(String tooltip) {
        mTooltip = tooltip;
    }
    
    /** Sets the optional SDK URL. Can be null or empty. */
    public void setSdkUrl(String sdkUrl) {
        mSdkUrl = sdkUrl;
    }

    /**
     * @return A new {@link UiElementNode} linked to this descriptor.
     */
    public UiElementNode createUiNode() {
        return new UiElementNode(this);
    }
    
    /**
     * Returns the first children of this descriptor that describes the given XML element name. 
     * <p/>
     * In recursive mode, searches the direct children first before descending in the hierarchy.
     * 
     * @return The ElementDescriptor matching the requested XML node element name or null.
     */
    public ElementDescriptor findChildrenDescriptor(String element_name, boolean recursive) {
        return findChildrenDescriptorInternal(element_name, recursive, null);
    }

    private ElementDescriptor findChildrenDescriptorInternal(String element_name,
            boolean recursive,
            Set<ElementDescriptor> visited) {
        if (recursive && visited == null) {
            visited = new HashSet<ElementDescriptor>();
        }

        for (ElementDescriptor e : getChildren()) {
            if (e.getXmlName().equals(element_name)) {
                return e;
            }
        }

        if (visited != null) {
            visited.add(this);
        }

        if (recursive) {
            for (ElementDescriptor e : getChildren()) {
                if (visited != null) {
                    if (!visited.add(e)) {  // Set.add() returns false if element is already present
                        continue;
                    }
                }
                ElementDescriptor f = e.findChildrenDescriptorInternal(element_name,
                        recursive, visited);
                if (f != null) {
                    return f;
                }
            }
        }

        return null;
    }

    /**
     * Utility helper than pretty-formats an XML Name for the UI.
     * This is used by the simplified constructor that takes only an XML element name.
     * 
     * @param xml_name The XML name to convert.
     * @return The XML name with dashes replaced by spaces and capitalized.
     */
    private static String prettyName(String xml_name) {
        char c[] = xml_name.toCharArray();
        if (c.length > 0) {
            c[0] = Character.toUpperCase(c[0]);
        }
        return new String(c).replace("-", " ");  //$NON-NLS-1$  //$NON-NLS-2$
    }

}
