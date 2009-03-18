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

package com.android.ide.eclipse.editors.xml.descriptors;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo;
import com.android.ide.eclipse.common.resources.ViewClassInfo;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo.AttributeInfo;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.DocumentDescriptor;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.editors.descriptors.SeparatorAttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.editors.layout.descriptors.ViewElementDescriptor;
import com.android.sdklib.SdkConstants;

import java.util.ArrayList;
import java.util.Map;


/**
 * Description of the /res/xml structure.
 * Currently supports the <searchable> and <preferences> root nodes.
 */
public final class XmlDescriptors implements IDescriptorProvider {

    // Public attributes names, attributes descriptors and elements descriptors referenced
    // elsewhere.
    public static final String PREF_KEY_ATTR = "key"; //$NON-NLS-1$

    /** The root document descriptor for both searchable and preferences. */
    private DocumentDescriptor mDescriptor = new DocumentDescriptor("xml_doc", null /* children */); //$NON-NLS-1$ 

    /** The root document descriptor for searchable. */
    private DocumentDescriptor mSearchDescriptor = new DocumentDescriptor("xml_doc", null /* children */); //$NON-NLS-1$ 

    /** The root document descriptor for preferences. */
    private DocumentDescriptor mPrefDescriptor = new DocumentDescriptor("xml_doc", null /* children */); //$NON-NLS-1$ 

    /** The root document descriptor for widget provider. */
    private DocumentDescriptor mAppWidgetDescriptor = new DocumentDescriptor("xml_doc", null /* children */); //$NON-NLS-1$ 

    /** @return the root descriptor for both searchable and preferences. */
    public DocumentDescriptor getDescriptor() {
        return mDescriptor;
    }
    
    public ElementDescriptor[] getRootElementDescriptors() {
        return mDescriptor.getChildren();
    }
    
    /** @return the root descriptor for searchable. */
    public DocumentDescriptor getSearchableDescriptor() {
        return mSearchDescriptor;
    }
    
    /** @return the root descriptor for preferences. */
    public DocumentDescriptor getPreferencesDescriptor() {
        return mPrefDescriptor;
    }
    
    /** @return the root descriptor for widget providers. */
    public DocumentDescriptor getAppWidgetDescriptor() {
        return mAppWidgetDescriptor;
    }
    
    public IDescriptorProvider getSearchableProvider() {
        return new IDescriptorProvider() {
            public ElementDescriptor getDescriptor() {
                return mSearchDescriptor;
            }

            public ElementDescriptor[] getRootElementDescriptors() {
                return mSearchDescriptor.getChildren();
            }
        };
    }

    public IDescriptorProvider getPreferencesProvider() {
        return new IDescriptorProvider() {
            public ElementDescriptor getDescriptor() {
                return mPrefDescriptor;
            }

            public ElementDescriptor[] getRootElementDescriptors() {
                return mPrefDescriptor.getChildren();
            }
        };
    }

    public IDescriptorProvider getAppWidgetProvider() {
        return new IDescriptorProvider() {
            public ElementDescriptor getDescriptor() {
                return mAppWidgetDescriptor;
            }

            public ElementDescriptor[] getRootElementDescriptors() {
                return mAppWidgetDescriptor.getChildren();
            }
        };
    }

    /**
     * Updates the document descriptor.
     * <p/>
     * It first computes the new children of the descriptor and then updates them
     * all at once.
     * 
     * @param searchableStyleMap The map style=>attributes for <searchable> from the attrs.xml file
     * @param appWidgetStyleMap The map style=>attributes for <appwidget-provider> from the attrs.xml file
     * @param prefs The list of non-group preference descriptions 
     * @param prefGroups The list of preference group descriptions
     */
    public synchronized void updateDescriptors(
            Map<String, DeclareStyleableInfo> searchableStyleMap,
            Map<String, DeclareStyleableInfo> appWidgetStyleMap,
            ViewClassInfo[] prefs, ViewClassInfo[] prefGroups) {

        XmlnsAttributeDescriptor xmlns = new XmlnsAttributeDescriptor(
                "android", //$NON-NLS-1$
                SdkConstants.NS_RESOURCES); 

        ElementDescriptor searchable = createSearchable(searchableStyleMap, xmlns);
        ElementDescriptor appWidget = createAppWidgetProviderInfo(appWidgetStyleMap, xmlns);
        ElementDescriptor preferences = createPreference(prefs, prefGroups, xmlns);
        ArrayList<ElementDescriptor> list =  new ArrayList<ElementDescriptor>();
        if (searchable != null) {
            list.add(searchable);
            mSearchDescriptor.setChildren(new ElementDescriptor[]{ searchable });
        }
        if (appWidget != null) {
            list.add(appWidget);
            mAppWidgetDescriptor.setChildren(new ElementDescriptor[]{ appWidget });
        }
        if (preferences != null) {
            list.add(preferences);
            mPrefDescriptor.setChildren(new ElementDescriptor[]{ preferences });
        }

        if (list.size() > 0) {
            mDescriptor.setChildren(list.toArray(new ElementDescriptor[list.size()]));
        }
    }

    //-------------------------
    // Creation of <searchable>
    //-------------------------
    
    /**
     * Returns the new ElementDescriptor for <searchable>
     */
    private ElementDescriptor createSearchable(
            Map<String, DeclareStyleableInfo> searchableStyleMap,
            XmlnsAttributeDescriptor xmlns) {

        ElementDescriptor action_key = createElement(searchableStyleMap,
                "SearchableActionKey", //$NON-NLS-1$ styleName
                "actionkey", //$NON-NLS-1$ xmlName
                "Action Key", // uiName
                null, // sdk url
                null, // extraAttribute
                null, // childrenElements
                false /* mandatory */ );

        ElementDescriptor searchable = createElement(searchableStyleMap,
                "Searchable", //$NON-NLS-1$ styleName
                "searchable", //$NON-NLS-1$ xmlName
                "Searchable", // uiName
                null, // sdk url
                xmlns, // extraAttribute
                new ElementDescriptor[] { action_key }, // childrenElements
                false /* mandatory */ );
        return searchable;
    }
    
    /**
     * Returns the new ElementDescriptor for <appwidget-provider>
     */
    private ElementDescriptor createAppWidgetProviderInfo(
            Map<String, DeclareStyleableInfo> appWidgetStyleMap,
            XmlnsAttributeDescriptor xmlns) {

        if (appWidgetStyleMap == null) {
            return null;
        }
        
        ElementDescriptor appWidget = createElement(appWidgetStyleMap,
                "AppWidgetProviderInfo", //$NON-NLS-1$ styleName
                "appwidget-provider", //$NON-NLS-1$ xmlName
                "AppWidget Provider", // uiName
                null, // sdk url
                xmlns, // extraAttribute
                null, // childrenElements
                false /* mandatory */ );
        return appWidget;
    }

    /**
     * Returns a new ElementDescriptor constructed from the information given here
     * and the javadoc & attributes extracted from the style map if any.
     */
    private ElementDescriptor createElement(
            Map<String, DeclareStyleableInfo> styleMap, String styleName,
            String xmlName, String uiName, String sdkUrl,
            AttributeDescriptor extraAttribute,
            ElementDescriptor[] childrenElements, boolean mandatory) {

        ElementDescriptor element = new ElementDescriptor(xmlName, uiName, null, sdkUrl,
                null, childrenElements, mandatory);

        return updateElement(element, styleMap, styleName, extraAttribute);
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

    //--------------------------
    // Creation of <Preferences>
    //--------------------------

    /**
     * Returns the new ElementDescriptor for <Preferences>
     */
    private ElementDescriptor createPreference(ViewClassInfo[] prefs,
            ViewClassInfo[] prefGroups, XmlnsAttributeDescriptor xmlns) {

        ArrayList<ElementDescriptor> newPrefs = new ArrayList<ElementDescriptor>();
        if (prefs != null) {
            for (ViewClassInfo info : prefs) {
                ElementDescriptor desc = convertPref(info);
                newPrefs.add(desc);
            }
        }

        ElementDescriptor topPreferences = null;
        
        ArrayList<ElementDescriptor> newGroups = new ArrayList<ElementDescriptor>();
        if (prefGroups != null) {
            for (ViewClassInfo info : prefGroups) {
                ElementDescriptor desc = convertPref(info);
                newGroups.add(desc);
                
                if (info.getCanonicalClassName() == AndroidConstants.CLASS_PREFERENCES) {
                    topPreferences = desc;
                }
            }
        }

        ArrayList<ElementDescriptor> everything = new ArrayList<ElementDescriptor>();
        everything.addAll(newGroups);
        everything.addAll(newPrefs);
        ElementDescriptor[] newArray = everything.toArray(new ElementDescriptor[everything.size()]);

        // Link all groups to everything else here.. recursively
        for (ElementDescriptor layoutDesc : newGroups) {
            layoutDesc.setChildren(newArray);
        }

        // The "top" element to be returned corresponds to the class "Preferences".
        // Its descriptor has already been created. However the root one also needs
        // the hidden xmlns:android definition..
        if (topPreferences != null) {
            AttributeDescriptor[] attrs = topPreferences.getAttributes();
            AttributeDescriptor[] newAttrs = new AttributeDescriptor[attrs.length + 1];
            System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
            newAttrs[attrs.length] = xmlns;
            return new ElementDescriptor(
                    topPreferences.getXmlName(),
                    topPreferences.getUiName(),
                    topPreferences.getTooltip(),
                    topPreferences.getSdkUrl(),
                    newAttrs,
                    topPreferences.getChildren(),
                    false /* mandatory */);
        } else {
            return null;
        }
    }

    /**
     * Creates an element descriptor from a given {@link ViewClassInfo}.
     */
    private ElementDescriptor convertPref(ViewClassInfo info) {
        String xml_name = info.getShortClassName();
        String tooltip = info.getJavaDoc();
        
        // Process all Preference attributes
        ArrayList<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
        DescriptorsUtils.appendAttributes(attributes,
                null,   // elementName
                SdkConstants.NS_RESOURCES,
                info.getAttributes(),
                null,   // requiredAttributes
                null);  // overrides
        
        for (ViewClassInfo link = info.getSuperClass();
                link != null;
                link = link.getSuperClass()) {
            AttributeInfo[] attrList = link.getAttributes();
            if (attrList.length > 0) {
                attributes.add(new SeparatorAttributeDescriptor(
                        String.format("Attributes from %1$s", link.getShortClassName()))); 
                DescriptorsUtils.appendAttributes(attributes,
                        null,   // elementName
                        SdkConstants.NS_RESOURCES,
                        attrList,
                        null,   // requiredAttributes
                        null);  // overrides
            }
        }

        return new ViewElementDescriptor(xml_name,
                xml_name, // ui_name
                info.getCanonicalClassName(),
                tooltip,
                null, // sdk_url
                attributes.toArray(new AttributeDescriptor[attributes.size()]),
                null,
                null, // children
                false /* mandatory */);
    }
}
