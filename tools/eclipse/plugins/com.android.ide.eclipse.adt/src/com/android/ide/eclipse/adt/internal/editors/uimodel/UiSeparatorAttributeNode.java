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

package com.android.ide.eclipse.adt.internal.editors.uimodel;

import com.android.ide.eclipse.adt.internal.editors.descriptors.AttributeDescriptor;
import com.android.ide.eclipse.adt.internal.editors.descriptors.SeparatorAttributeDescriptor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.w3c.dom.Node;

/**
 * {@link UiSeparatorAttributeNode} does not represent any real attribute.
 * <p/>
 * It is used to separate groups of attributes visually.
 */
public class UiSeparatorAttributeNode extends UiAttributeNode {

    /** Creates a new {@link UiAttributeNode} linked to a specific {@link AttributeDescriptor} */
    public UiSeparatorAttributeNode(SeparatorAttributeDescriptor attrDesc,
            UiElementNode uiParent) {
        super(attrDesc, uiParent);
    }

    /** Returns the current value of the node. */
    @Override
    public String getCurrentValue() {
        // There is no value here.
        return null;
    }

    /**
     * Sets whether the attribute is dirty and also notifies the editor some part's dirty
     * flag as changed.
     * <p/>
     * Subclasses should set the to true as a result of user interaction with the widgets in
     * the section and then should set to false when the commit() method completed.
     */
    @Override
    public void setDirty(boolean isDirty) {
        // This is never dirty.
    }
    
    /**
     * Called once by the parent user interface to creates the necessary
     * user interface to edit this attribute.
     * <p/>
     * This method can be called more than once in the life cycle of an UI node,
     * typically when the UI is part of a master-detail tree, as pages are swapped.
     * 
     * @param parent The composite where to create the user interface.
     * @param managedForm The managed form owning this part.
     */
    @Override
    public void createUiControl(Composite parent, IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        Composite row = toolkit.createComposite(parent);
        
        TableWrapData twd = new TableWrapData(TableWrapData.FILL_GRAB);
        if (parent.getLayout() instanceof TableWrapLayout) {
            twd.colspan = ((TableWrapLayout) parent.getLayout()).numColumns;
        }
        row.setLayoutData(twd);
        row.setLayout(new GridLayout(3, false /* equal width */));

        Label sep = toolkit.createSeparator(row, SWT.HORIZONTAL);
        GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
        gd.widthHint = 16;
        sep.setLayoutData(gd);

        Label label = toolkit.createLabel(row, getDescriptor().getXmlLocalName());
        label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        sep = toolkit.createSeparator(row, SWT.HORIZONTAL);
        sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }
    
    /**
     * No completion values for this UI attribute.
     * 
     * {@inheritDoc}
     */
    @Override
    public String[] getPossibleValues(String prefix) {
        return null;
    }
    
    /**
     * Called when the XML is being loaded or has changed to
     * update the value held by this user interface attribute node.
     * <p/>
     * The XML Node <em>may</em> be null, which denotes that the attribute is not
     * specified in the XML model. In general, this means the "default" value of the
     * attribute should be used.
     * <p/>
     * The caller doesn't really know if attributes have changed,
     * so it will call this to refresh the attribute anyway. It's up to the
     * UI implementation to minimize refreshes.
     * 
     * @param xml_attribute_node
     */
    @Override
    public void updateValue(Node xml_attribute_node) {
        // No value to update.
    }

    /**
     * Called by the user interface when the editor is saved or its state changed
     * and the modified attributes must be committed (i.e. written) to the XML model.
     * <p/>
     * Important behaviors:
     * <ul>
     * <li>The caller *must* have called IStructuredModel.aboutToChangeModel before.
     *     The implemented methods must assume it is safe to modify the XML model.
     * <li>On success, the implementation *must* call setDirty(false).
     * <li>On failure, the implementation can fail with an exception, which
     *     is trapped and logged by the caller, or do nothing, whichever is more
     *     appropriate.
     * </ul>
     */
    @Override
    public void commit() {
        // No value to commit.
    }
}
