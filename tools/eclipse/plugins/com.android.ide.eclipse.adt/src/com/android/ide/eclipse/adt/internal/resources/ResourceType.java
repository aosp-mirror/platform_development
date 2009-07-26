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

package com.android.ide.eclipse.adt.internal.resources;

/**
 * Enum representing a type of compiled resource.
 */
public enum ResourceType {
    ANIM("anim", "Animation"), //$NON-NLS-1$
    ARRAY("array", "Array", "string-array", "integer-array"), //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-4$
    ATTR("attr", "Attr"), //$NON-NLS-1$
    COLOR("color", "Color"), //$NON-NLS-1$
    DIMEN("dimen", "Dimension"), //$NON-NLS-1$
    DRAWABLE("drawable", "Drawable"), //$NON-NLS-1$
    ID("id", "ID"), //$NON-NLS-1$
    LAYOUT("layout", "Layout"), //$NON-NLS-1$
    MENU("menu", "Menu"), //$NON-NLS-1$
    RAW("raw", "Raw"), //$NON-NLS-1$
    STRING("string", "String"), //$NON-NLS-1$
    STYLE("style", "Style"), //$NON-NLS-1$
    STYLEABLE("styleable", "Styleable"), //$NON-NLS-1$
    XML("xml", "XML"); //$NON-NLS-1$

    private final String mName;
    private final String mDisplayName;
    private final String[] mAlternateXmlNames;

    ResourceType(String name, String displayName, String... alternateXmlNames) {
        mName = name;
        mDisplayName = displayName;
        mAlternateXmlNames = alternateXmlNames;
    }
    
    /**
     * Returns the resource type name, as used by XML files.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns a translated display name for the resource type.
     */
    public String getDisplayName() {
        return mDisplayName;
    }
    
    /**
     * Returns the enum by its name as it appears in the XML or the R class.
     * @param name name of the resource
     * @return the matching {@link ResourceType} or <code>null</code> if no match was found.
     */
    public static ResourceType getEnum(String name) {
        for (ResourceType rType : values()) {
            if (rType.mName.equals(name)) {
                return rType;
            } else if (rType.mAlternateXmlNames != null) {
                // if there are alternate Xml Names, we test those too
                for (String alternate : rType.mAlternateXmlNames) {
                    if (alternate.equals(name)) {
                        return rType;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Returns a formatted string usable in an XML to use the specified {@link ResourceItem}.
     * @param resourceItem The resource item.
     * @param system Whether this is a system resource or a project resource.
     * @return a string in the format @[type]/[name] 
     */
    public String getXmlString(ResourceItem resourceItem, boolean system) {
        if (this == ID && resourceItem instanceof IIdResourceItem) {
            IIdResourceItem idResource = (IIdResourceItem)resourceItem;
            if (idResource.isDeclaredInline()) {
                return (system?"@android:":"@+") + mName + "/" + resourceItem.getName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }

        return (system?"@android:":"@") + mName + "/" + resourceItem.getName(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    
    /**
     * Returns an array with all the names defined by this enum.
     */
    public static String[] getNames() {
        ResourceType[] values = values();
        String[] names = new String[values.length];
        for (int i = values.length - 1; i >= 0; --i) {
            names[i] = values[i].getName();
        }
        return names;
    }
}
