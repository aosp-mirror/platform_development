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
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo;
import com.android.ide.eclipse.common.resources.ViewClassInfo;
import com.android.ide.eclipse.common.resources.DeclareStyleableInfo.AttributeInfo;
import com.android.ide.eclipse.common.resources.ViewClassInfo.LayoutParamsInfo;
import com.android.ide.eclipse.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.DocumentDescriptor;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.descriptors.IDescriptorProvider;
import com.android.ide.eclipse.editors.descriptors.SeparatorAttributeDescriptor;
import com.android.sdklib.SdkConstants;

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
    private DocumentDescriptor mRootDescriptor =
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
        return mRootDescriptor;
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
        return mRootDescriptor.getChildren();
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

        // Create <include> as a synthetic regular view.
        // Note: ViewStub is already described by attrs.xml
        insertInclude(newViews);

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

        // Link all layouts to everything else here.. recursively
        for (ElementDescriptor layoutDesc : newLayouts) {
            layoutDesc.setChildren(newDescriptors);
        }

        // The <merge> tag can only be a root tag, so it is added at the end.
        // It gets everything else as children but it is not made a child itself.
        ElementDescriptor mergeTag = createMerge(newLayouts);
        mergeTag.setChildren(newDescriptors);  // mergeTag makes a copy of the list
        newDescriptors.add(mergeTag);
        newLayouts.add(mergeTag);

        mViewDescriptors = newViews;
        mLayoutDescriptors  = newLayouts;
        mRootDescriptor.setChildren(newDescriptors);
        
        mROLayoutDescriptors = Collections.unmodifiableList(mLayoutDescriptors);
        mROViewDescriptors = Collections.unmodifiableList(mViewDescriptors);
    }

    /**
     * Creates an element descriptor from a given {@link ViewClassInfo}.
     */
    private ElementDescriptor convertView(ViewClassInfo info) {
        String xml_name = info.getShortClassName();
        String tooltip = info.getJavaDoc();
        
        ArrayList<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
        
        // All views and groups have an implicit "style" attribute which is a reference.
        AttributeInfo styleInfo = new DeclareStyleableInfo.AttributeInfo(
                "style",    //$NON-NLS-1$ xmlLocalName
                new DeclareStyleableInfo.AttributeInfo.Format[] {
                        DeclareStyleableInfo.AttributeInfo.Format.REFERENCE
                    });
        styleInfo.setJavaDoc("A reference to a custom style"); //tooltip
        DescriptorsUtils.appendAttribute(attributes,
                "style",    //$NON-NLS-1$
                null,       //nsUri
                styleInfo,
                false,      //required
                null);      // overrides
        
        // Process all View attributes
        DescriptorsUtils.appendAttributes(attributes,
                null, // elementName
                SdkConstants.NS_RESOURCES,
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
                        SdkConstants.NS_RESOURCES,
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
                        SdkConstants.NS_RESOURCES, attr_info)) {
                    continue;
                }
                if (need_separator) {
                    String title;
                    if (layoutParams.getShortClassName().equals(
                            AndroidConstants.CLASS_NAME_LAYOUTPARAMS)) {
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
                        SdkConstants.NS_RESOURCES,
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

    /**
     * Creates a new <include> descriptor and adds it to the list of view descriptors.
     * 
     * @param knownViews A list of view descriptors being populated. Also used to find the
     *   View descriptor and extract its layout attributes.
     */
    private void insertInclude(ArrayList<ElementDescriptor> knownViews) {
        String xml_name = "include";  //$NON-NLS-1$

        // Create the include custom attributes
        ArrayList<AttributeDescriptor> attributes = new ArrayList<AttributeDescriptor>();
        
        // Note that the "layout" attribute does NOT have the Android namespace
        DescriptorsUtils.appendAttribute(attributes,
                null, //elementXmlName
                null, //nsUri
                new AttributeInfo(
                        "layout",       //$NON-NLS-1$
                        new AttributeInfo.Format[] { AttributeInfo.Format.REFERENCE }
                        ),
                true,  //required
                null); //overrides

        DescriptorsUtils.appendAttribute(attributes,
                null, //elementXmlName
                SdkConstants.NS_RESOURCES, //nsUri
                new AttributeInfo(
                        "id",           //$NON-NLS-1$
                        new AttributeInfo.Format[] { AttributeInfo.Format.REFERENCE }
                        ),
                true,  //required
                null); //overrides

        // Find View and inherit all its layout attributes
        AttributeDescriptor[] viewLayoutAttribs = findViewLayoutAttributes(
                AndroidConstants.CLASS_VIEW, knownViews);

        // Create the include descriptor
        ViewElementDescriptor desc = new ViewElementDescriptor(xml_name,  // xml_name
                xml_name, // ui_name
                null,     // canonical class name, we don't have one
                "Lets you statically include XML layouts inside other XML layouts.",  // tooltip
                null, // sdk_url
                attributes.toArray(new AttributeDescriptor[attributes.size()]),
                viewLayoutAttribs,  // layout attributes
                null, // children
                false /* mandatory */);
        
        knownViews.add(desc);
    }

    /**
     * Creates and return a new <merge> descriptor.
     * @param knownLayouts  A list of all known layout view descriptors, used to find the
     *   FrameLayout descriptor and extract its layout attributes.
     */
    private ElementDescriptor createMerge(ArrayList<ElementDescriptor> knownLayouts) {
        String xml_name = "merge";  //$NON-NLS-1$

        // Find View and inherit all its layout attributes
        AttributeDescriptor[] viewLayoutAttribs = findViewLayoutAttributes(
                AndroidConstants.CLASS_FRAMELAYOUT, knownLayouts);

        // Create the include descriptor
        ViewElementDescriptor desc = new ViewElementDescriptor(xml_name,  // xml_name
                xml_name, // ui_name
                null,     // canonical class name, we don't have one
                "A root tag useful for XML layouts inflated using a ViewStub.",  // tooltip
                null,  // sdk_url
                null,  // attributes
                viewLayoutAttribs,  // layout attributes
                null,  // children
                false  /* mandatory */);

        return desc;
    }

    /**
     * Finds the descriptor and retrieves all its layout attributes.
     */
    private AttributeDescriptor[] findViewLayoutAttributes(
            String viewFqcn,
            ArrayList<ElementDescriptor> knownViews) {

        for (ElementDescriptor desc : knownViews) {
            if (desc instanceof ViewElementDescriptor) {
                ViewElementDescriptor viewDesc = (ViewElementDescriptor) desc;
                if (viewFqcn.equals(viewDesc.getCanonicalClassName())) {
                    return viewDesc.getLayoutAttributes();
                }
            }
        }
        
        return null;
    }
}
