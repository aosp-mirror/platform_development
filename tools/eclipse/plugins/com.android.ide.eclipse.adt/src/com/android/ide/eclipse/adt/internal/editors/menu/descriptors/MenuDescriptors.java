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

package com.android.ide.eclipse.adt.internal.editors.menu.descriptors;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.resources.DeclareStyleableInfo;
import com.android.sdklib.SdkConstants;

import java.util.ArrayList;
import java.util.Map;


/**
 * Complete description of the menu structure.
 */
public final class MenuDescriptors implements IDescriptorProvider {

    public static final String MENU_ROOT_ELEMENT = "menu"; //$NON-NLS-1$

    /** The root element descriptor. */
    private ElementDescriptor mDescriptor = null;

    /** @return the root descriptor. */
    public ElementDescriptor getDescriptor() {
        return mDescriptor;
    }
    
    public ElementDescriptor[] getRootElementDescriptors() {
        return mDescriptor.getChildren();
    }
    
    /**
     * Updates the document descriptor.
     * <p/>
     * It first computes the new children of the descriptor and then updates them
     * all at once.
     * 
     * @param styleMap The map style => attributes from the attrs.xml file
     */
    public synchronized void updateDescriptors(Map<String, DeclareStyleableInfo> styleMap) {

        // There are 3 elements: menu, item and group.
        // The root element MUST be a menu.
        // A top menu can contain items or group:
        //  - top groups can contain top items
        //  - top items can contain sub-menus
        // A sub menu can contains sub items or sub groups:
        //  - sub groups can contain sub items
        //  - sub items cannot contain anything
        
        if (mDescriptor == null) {
            mDescriptor = createElement(styleMap,
                MENU_ROOT_ELEMENT, // xmlName
                "Menu", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                null, // childrenElements,
                true /* mandatory */);
        }

        // -- sub menu can have sub_items, sub_groups but not sub_menus

        ElementDescriptor sub_item = createElement(styleMap,
                "item", // xmlName //$NON-NLS-1$
                "Item", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                null, // childrenElements,
                false /* mandatory */);

        ElementDescriptor sub_group = createElement(styleMap,
                "group", // xmlName //$NON-NLS-1$
                "Group", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                new ElementDescriptor[] { sub_item }, // childrenElements,
                false /* mandatory */);

        ElementDescriptor sub_menu = createElement(styleMap,
                MENU_ROOT_ELEMENT, // xmlName //$NON-NLS-1$
                "Sub-Menu", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                new ElementDescriptor[] { sub_item, sub_group }, // childrenElements,
                true /* mandatory */);

        // -- top menu can have all top groups and top items (which can have sub menus)

        ElementDescriptor top_item = createElement(styleMap,
                "item", // xmlName //$NON-NLS-1$
                "Item", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                new ElementDescriptor[] { sub_menu }, // childrenElements,
                false /* mandatory */);

        ElementDescriptor top_group = createElement(styleMap,
                "group", // xmlName //$NON-NLS-1$
                "Group", // uiName,
                null, // TODO SDK URL
                null, // extraAttribute
                new ElementDescriptor[] { top_item }, // childrenElements,
                false /* mandatory */);

        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor("android", //$NON-NLS-1$
                SdkConstants.NS_RESOURCES); 

        updateElement(mDescriptor, styleMap, "Menu", xmlns); //$NON-NLS-1$
        mDescriptor.setChildren(new ElementDescriptor[] { top_item, top_group });
    }

    /**
     * Returns a new ElementDescriptor constructed from the information given here
     * and the javadoc & attributes extracted from the style map if any.
     */
    private ElementDescriptor createElement(
            Map<String, DeclareStyleableInfo> styleMap, 
            String xmlName, String uiName, String sdkUrl,
            AttributeDescriptor extraAttribute,
            ElementDescriptor[] childrenElements, boolean mandatory) {

        ElementDescriptor element = new ElementDescriptor(xmlName, uiName, null, sdkUrl,
                null, childrenElements, mandatory);

        return updateElement(element, styleMap,
                getStyleName(xmlName),
                extraAttribute);
    }
    
    /**
     * Updates an ElementDescriptor with the javadoc & attributes extracted from the style
     * map if any.
     */
    private ElementDescriptor updateElement(ElementDescriptor element,
            Map<String, DeclareStyleableInfo> styleMap,
            String styleName,
            AttributeDescriptor extraAttribute) {
        ArrayList<AttributeDescriptor> descs = new ArrayList<AttributeDescriptor>();

        DeclareStyleableInfo style = styleMap != null ? styleMap.get(styleName) : null;
        if (style != null) {
            DescriptorsUtils.appendAttributes(descs,
                    null,   // elementName
                    SdkConstants.NS_RESOURCES,
                    style.getAttributes(),
                    null,   // requiredAttributes
                    null);  // overrides
            element.setTooltip(style.getJavaDoc());
        }

        if (extraAttribute != null) {
            descs.add(extraAttribute);
        }

        element.setAttributes(descs.toArray(new AttributeDescriptor[descs.size()]));
        return element;
    }

    /**
     * Returns the style name (i.e. the <declare-styleable> name found in attrs.xml)
     * for a given XML element name.
     * <p/>
     * The rule is that all elements have for style name:
     * - their xml name capitalized
     * - a "Menu" prefix, except for <menu> itself which is just "Menu".
     */
    private String getStyleName(String xmlName) {
        String styleName = DescriptorsUtils.capitalize(xmlName);

        // This is NOT the UI Name but the expected internal style name
        final String MENU_STYLE_BASE_NAME = "Menu"; //$NON-NLS-1$
        
        if (!styleName.equals(MENU_STYLE_BASE_NAME)) {        
            styleName = MENU_STYLE_BASE_NAME + styleName;
        }
        return styleName;
    }
}
