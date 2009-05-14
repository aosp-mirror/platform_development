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

package com.android.ide.eclipse.adt.internal.editors.layout.uimodel;

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.sdk.AndroidTargetData;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IProject;

import java.util.List;

/**
 * Specialized version of {@link UiElementNode} for the {@link ViewElementDescriptor}s.
 */
public class UiViewElementNode extends UiElementNode {

    private AttributeDescriptor[] mCachedAttributeDescriptors;

    public UiViewElementNode(ViewElementDescriptor elementDescriptor) {
        super(elementDescriptor);
    }

    /**
     * Returns an AttributeDescriptor array that depends on the current UiParent.
     * <p/>
     * The array merges both "direct" attributes with the descriptor layout attributes.
     * The array instance is cached and cleared if the UiParent is changed.
     */
    @Override
    public AttributeDescriptor[] getAttributeDescriptors() {
        if (mCachedAttributeDescriptors != null) {
            return mCachedAttributeDescriptors;
        }

        UiElementNode ui_parent = getUiParent();
        AttributeDescriptor[] direct_attrs = super.getAttributeDescriptors();
        mCachedAttributeDescriptors = direct_attrs;

        AttributeDescriptor[] layout_attrs = null;
        boolean need_xmlns = false;

        if (ui_parent instanceof UiDocumentNode) {
            // Limitation: right now the layout behaves as if everything was
            // owned by a FrameLayout.
            // TODO replace by something user-configurable.

            List<ElementDescriptor> layoutDescriptors = null;
            IProject project = getEditor().getProject();
            if (project != null) {
                Sdk currentSdk = Sdk.getCurrent();
                IAndroidTarget target = currentSdk.getTarget(project);
                if (target != null) {
                    AndroidTargetData data = currentSdk.getTargetData(target);
                    layoutDescriptors = data.getLayoutDescriptors().getLayoutDescriptors();
                }
            }
            
            if (layoutDescriptors != null) {
                for (ElementDescriptor desc : layoutDescriptors) {
                    if (desc instanceof ViewElementDescriptor &&
                            desc.getXmlName().equals(AndroidConstants.CLASS_NAME_FRAMELAYOUT)) {
                        layout_attrs = ((ViewElementDescriptor) desc).getLayoutAttributes();
                        need_xmlns = true;
                        break;
                    }
                }
            }
        } else if (ui_parent instanceof UiViewElementNode){
            layout_attrs =
                ((ViewElementDescriptor) ui_parent.getDescriptor()).getLayoutAttributes();
        }

        if (layout_attrs == null || layout_attrs.length == 0) {
            return mCachedAttributeDescriptors;
        }

        mCachedAttributeDescriptors =
            new AttributeDescriptor[direct_attrs.length +
                                    layout_attrs.length +
                                    (need_xmlns ? 1 : 0)];
        System.arraycopy(direct_attrs, 0,
                mCachedAttributeDescriptors, 0,
                direct_attrs.length);
        System.arraycopy(layout_attrs, 0,
                mCachedAttributeDescriptors, direct_attrs.length,
                layout_attrs.length);
        if (need_xmlns) {
            AttributeDescriptor desc = new XmlnsAttributeDescriptor(
                    "android",  //$NON-NLS-1$
                    SdkConstants.NS_RESOURCES);
            mCachedAttributeDescriptors[direct_attrs.length + layout_attrs.length] = desc;
        }

        return mCachedAttributeDescriptors;
    }
    
    /**
     * Sets the parent of this UI node.
     * <p/>
     * Also removes the cached AttributeDescriptor array that depends on the current UiParent.
     */
    @Override
    protected void setUiParent(UiElementNode parent) {
        super.setUiParent(parent);
        mCachedAttributeDescriptors = null;
    }
}
