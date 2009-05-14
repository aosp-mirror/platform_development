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

package com.android.ide.eclipse.adt.internal.editors.manifest.pages;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.XmlnsAttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.ui.UiElementPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Application's attributes section part for Application page.
 * <p/>
 * This part is displayed at the top of the application page and displays all the possible
 * attributes of an application node in the AndroidManifest (icon, class name, label, etc.)
 */
final class ApplicationAttributesPart extends UiElementPart {

    /** Listen to changes to the UI node for <application> and updates the UI */
    private AppNodeUpdateListener mAppNodeUpdateListener;
    /** ManagedForm needed to create the UI controls */ 
    private IManagedForm mManagedForm;

    public ApplicationAttributesPart(Composite body, FormToolkit toolkit, ManifestEditor editor,
            UiElementNode applicationUiNode) {
        super(body, toolkit, editor, applicationUiNode,
                "Application Attributes", // section title
                "Defines the attributes specific to the application.", // section description
                Section.TWISTIE | Section.EXPANDED);
    }
    
    /**
     * Changes and refreshes the Application UI node handle by the this part.
     */
    @Override
    public void setUiElementNode(UiElementNode uiElementNode) {
        super.setUiElementNode(uiElementNode);

        createUiAttributes(mManagedForm);
    }

    /* (non-java doc)
     * Create the controls to edit the attributes for the given ElementDescriptor.
     * <p/>
     * This MUST not be called by the constructor. Instead it must be called from
     * <code>initialize</code> (i.e. right after the form part is added to the managed form.)
     * <p/>
     * Derived classes can override this if necessary.
     * 
     * @param managedForm The owner managed form
     */
    @Override
    protected void createFormControls(final IManagedForm managedForm) {
        mManagedForm = managedForm; 
        setTable(createTableLayout(managedForm.getToolkit(), 4 /* numColumns */));

        mAppNodeUpdateListener = new AppNodeUpdateListener();
        getUiElementNode().addUpdateListener(mAppNodeUpdateListener);

        createUiAttributes(mManagedForm);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (getUiElementNode() != null && mAppNodeUpdateListener != null) {
            getUiElementNode().removeUpdateListener(mAppNodeUpdateListener);
            mAppNodeUpdateListener = null;
        }
    }

    @Override
    protected void createUiAttributes(IManagedForm managedForm) {
        Composite table = getTable();
        if (table == null || managedForm == null) {
            return;
        }
        
        // Remove any old UI controls 
        for (Control c : table.getChildren()) {
            c.dispose();
        }
        
        UiElementNode uiElementNode = getUiElementNode(); 
        AttributeDescriptor[] attr_desc_list = uiElementNode.getAttributeDescriptors();

        // Display the attributes in 2 columns:
        // attr 0 | attr 4 
        // attr 1 | attr 5
        // attr 2 | attr 6
        // attr 3 | attr 7
        // that is we have to fill the grid in order 0, 4, 1, 5, 2, 6, 3, 7
        // thus index = i/2 + (i is odd * n/2)
        int n = attr_desc_list.length;
        int n2 = (int) Math.ceil(n / 2.0);
        for (int i = 0; i < n; i++) {
            AttributeDescriptor attr_desc = attr_desc_list[i / 2 + (i & 1) * n2];
            if (attr_desc instanceof XmlnsAttributeDescriptor) {
                // Do not show hidden attributes
                continue;
            }

            UiAttributeNode ui_attr = uiElementNode.findUiAttribute(attr_desc);
            if (ui_attr != null) {
                ui_attr.createUiControl(table, managedForm);
            } else {
                // The XML has an extra attribute which wasn't declared in
                // AndroidManifestDescriptors. This is not a problem, we just ignore it.
                AdtPlugin.log(IStatus.WARNING,
                        "Attribute %1$s not declared in node %2$s, ignored.", //$NON-NLS-1$
                        attr_desc.getXmlLocalName(),
                        uiElementNode.getDescriptor().getXmlName());
            }
        }
        
        if (n == 0) {
            createLabel(table, managedForm.getToolkit(),
                    "No attributes to display, waiting for SDK to finish loading...",
                    null /* tooltip */ );
        }

        // Initialize the enabled/disabled state
        if (mAppNodeUpdateListener != null) {
            mAppNodeUpdateListener.uiElementNodeUpdated(uiElementNode, null /* state, not used */);
        }
        
        // Tell the section that the layout has changed.
        layoutChanged();
    }

    /**
     * This listener synchronizes the UI with the actual presence of the application XML node.
     */
    private class AppNodeUpdateListener implements IUiUpdateListener {        
        public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
            // The UiElementNode for the application XML node always exists, even
            // if there is no corresponding XML node in the XML file.
            //
            // We enable the UI here if the XML node is not null.
            Composite table = getTable();
            boolean exists = (ui_node.getXmlNode() != null);
            if (table != null && table.getEnabled() != exists) {
                table.setEnabled(exists);
                for (Control c : table.getChildren()) {
                    c.setEnabled(exists);
                }
            }
        }
    }
}
