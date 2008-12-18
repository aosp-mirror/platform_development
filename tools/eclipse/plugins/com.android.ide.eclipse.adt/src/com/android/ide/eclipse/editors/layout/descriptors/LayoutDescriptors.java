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

package com.android.ide.eclipse.editors.layout.descriptors;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.resources.ViewClassInfo;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo.AttributeInfo;
import com.android.ide.eclipse.common.resources.ViewClassInfo.LayoutParamsInfo;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.DocumentDescriptor;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.editors.descriptors.SeparatorAttributeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Complete description of the layout structure.
 */
public final class LayoutDescriptors implements IDescriptorProvider {

    // Public attributes names, attributes descriptors and elements descriptors
    public static final String ID_ATTR = "id"; //$NON-NLS-1$

    /** The document descriptor. Contains all layouts and views linked together. */
    private DocumentDescriptor mDescriptor =
        new DocumentDescriptor("layout_doc", null); //$NON-NLS-1$

    /** The list of all known ViewLayout descriptors. */
    private ArrayList<ElementDescriptor> mLayoutDescriptors = new ArrayList<ElementDescriptor>();

    /** Read-Only list of View Descriptors. */
    private List<ElementDescriptor> mROLayoutDescriptors;

    /** The list of all known View (not ViewLayout) descriptors. */
    private ArrayList<ElementDescriptor> mViewDescriptors = new ArrayList<ElementDescriptor>();
    
    /** Read-Only list of View Descriptors. */
    private List<ElementDescriptor> mROViewDescriptors;
    
    /** @return the document descriptor. Contains all layouts and views linked together. */
    public DocumentDescriptor getDescriptor() {
        return mDescriptor;
    }
    
    /** @return The read-only list of all known ViewLayout descriptors. */
    public List<ElementDescriptor> getLayoutDescriptors() {
        return mROLayoutDescriptors;
    }
    
    /** @return The read-only list of all known View (not ViewLayout) descriptors. */
    public List<ElementDescriptor> getViewDescriptors() {
        return mROViewDescriptors;
    }
    
    public ElementDescriptor[] getRootElementDescriptors() {
        return mDescriptor.getChildren();
    }

    /**
     * Updates the document descriptor.
     * <p/>
     * It first computes the new children of the descriptor and then update them
     * all at once.
     * <p/> 
     *  TODO: differentiate groups from views in the tree UI? => rely on icons
     * <p/> 
     * 
     * @param views The list of views in the framework.
     * @param layouts The list of layouts in the framework.
     */
    public synchronized void updateDescriptors(ViewClassInfo[] views, ViewClassInfo[] layouts) {
        ArrayList<ElementDescriptor> newViews = new ArrayList<ElementDescriptor>();
        if (views != null) {
            for (ViewClassInfo info : views) {
                ElementDescriptor desc = convertView(info);
                newViews.add(desc);
            }
        }

        ArrayList<ElementDescriptor> newLayouts = new ArrayList<ElementDescriptor>();
        if (layouts != null) {
            for (ViewClassInfo info : layouts) {
                ElementDescriptor desc = convertView(info);
                newLayouts.add(desc);
            }
        }

        ArrayList<ElementDescriptor> newDescriptors = new ArrayList<ElementDescriptor>();
        newDescriptors.addAll(newLayouts);
        newDescriptors.addAll(newViews);
        ElementDescriptor[] newArray = newDescriptors.toArray(
                new ElementDescriptor[newDescriptors.size()]);

        // Link all layouts to everything else here.. recursively
        for (ElementDescriptor layoutDesc : newLayouts) {
            layoutDesc.setChildren(newArray);
        }

        mViewDescriptors = newViews;
        mLayoutDescriptors  = newLayouts;
        mDescriptor.setChildren(newArray);
        
        mROLayoutDescriptors = Collections.unmodifiableList(mLayoutDescriptors);
        mROViewDescriptors = Collections.unmodifiableList(mViewDescriptors);
    }

    /**
     * Creates an element descriptor from a given {@link ViewClassInfo}.
     */
    private ElementDescriptor convertView(ViewClassInfo info) {
        String xml_name = info.getShortClassName();
        String tooltip = info.getJavaDoc();
        
        // Process all View attributes
        ArrayList<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
        DescriptorsUtils.appendAttributes(attributes,
                null, // elementName
                AndroidConstants.NS_RESOURCES,
                info.getAttributes(),
                null, // requiredAttributes
                null /* overrides */);
        
        for (ViewClassInfo link = info.getSuperClass();
                link != null;
                link = link.getSuperClass()) {
            AttributeInfo[] attrList = link.getAttributes();
            if (attrList.length > 0) {
                attributes.add(new SeparatorAttributeDescriptor(
                        String.format("Attributes from %1$s", link.getShortClassName()))); 
                DescriptorsUtils.appendAttributes(attributes,
                        null, // elementName
                        AndroidConstants.NS_RESOURCES,
                        attrList,
                        null, // requiredAttributes
                        null /* overrides */);
            }
        }
        
        // Process all LayoutParams attributes
        ArrayList<AttributeDescriptor> layoutAttributes = new ArrayList<AttributeDescriptor>();
        LayoutParamsInfo layoutParams = info.getLayoutData();

        for(; layoutParams != null; layoutParams = layoutParams.getSuperClass()) {
            boolean need_separator = true;
            for (AttributeInfo attr_info : layoutParams.getAttributes()) {
                if (DescriptorsUtils.containsAttribute(layoutAttributes,
                        AndroidConstants.NS_RESOURCES, attr_info)) {
                    continue;
                }
                if (need_separator) {
                    String title;
                    if (layoutParams.getShortClassName().equals(
                            AndroidConstants.CLASS_LAYOUTPARAMS)) {
                        title = String.format("Layout Attributes from %1$s",
                                    layoutParams.getViewLayoutClass().getShortClassName());
                    } else {
                        title = String.format("Layout Attributes from %1$s (%2$s)",
                                layoutParams.getViewLayoutClass().getShortClassName(),
                                layoutParams.getShortClassName());
                    }
                    layoutAttributes.add(new SeparatorAttributeDescriptor(title));
                    need_separator = false;
                }
                DescriptorsUtils.appendAttribute(layoutAttributes,
                        null, // elementName
                        AndroidConstants.NS_RESOURCES,
                        attr_info,
                        false, // required
                        null /* overrides */);
            }
        }

        return new ViewElementDescriptor(xml_name,
                xml_name, // ui_name
                info.getCanonicalClassName(),
                tooltip,
                null, // sdk_url
                attributes.toArray(new AttributeDescriptor[attributes.size()]),
                layoutAttributes.toArray(new AttributeDescriptor[layoutAttributes.size()]),
                null, // children
                false /* mandatory */);
    }

}
