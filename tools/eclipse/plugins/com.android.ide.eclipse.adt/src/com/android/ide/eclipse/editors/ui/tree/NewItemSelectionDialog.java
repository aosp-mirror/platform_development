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

package com.android.ide.eclipse.editors.ui.tree;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import java.util.Arrays;

/**
 * A selection dialog to select the type of the new element node to
 * create, either in the application node or the selected sub node.
 */
public class NewItemSelectionDialog extends AbstractElementListSelectionDialog {

    /** The UI node selected in the tree view before creating the new item selection dialog.
     *  Can be null -- which means new items must be created in the root_node. */
    private UiElementNode mSelectedUiNode;
    /** The root node chosen by the user, either root_node or the one passed
     *  to the constructor if not null */
    private UiElementNode mChosenRootNode;
    private UiElementNode mLocalRootNode;
    /** The descriptor of the elements to be displayed as root in this tree view. All elements
     *  of the same type in the root will be displayed. */
    private ElementDescriptor[] mDescriptorFilters;

    /**
     * Creates the new item selection dialog.
     * 
     * @param shell The parent shell for the list.
     * @param labelProvider ILabelProvider for the list.
     * @param descriptorFilters The element allows at the root of the tree
     * @param ui_node The selected node, or null if none is selected.
     * @param root_node The root of the Ui Tree, either the UiDocumentNode or a sub-node.
     */
    public NewItemSelectionDialog(Shell shell, ILabelProvider labelProvider,
            ElementDescriptor[] descriptorFilters,
            UiElementNode ui_node,
            UiElementNode root_node) {
        super(shell, labelProvider);
        mDescriptorFilters = descriptorFilters;
        mLocalRootNode = root_node;

        // Only accept the UI node if it is not the UI root node and it can have children.
        // If the node cannot have children, select its parent as a potential target.
        if (ui_node != null && ui_node != mLocalRootNode) {
            if (ui_node.getDescriptor().hasChildren()) {
                mSelectedUiNode = ui_node;
            } else {
                UiElementNode parent = ui_node.getUiParent();
                if (parent != null && parent != mLocalRootNode) {
                    mSelectedUiNode = parent;
                }
            }
        }
        
        setHelpAvailable(false);
        setMultipleSelection(false);
        
        setValidator(new ISelectionStatusValidator() {
            public IStatus validate(Object[] selection) {
                if (selection.length == 1 && selection[0] instanceof ViewElementDescriptor) {
                    return new Status(IStatus.OK, // severity
                            AdtPlugin.PLUGIN_ID, //plugin id
                            IStatus.OK, // code
                            ((ViewElementDescriptor) selection[0]).getCanonicalClassName(), //msg 
                            null); // exception
                } else if (selection.length == 1 && selection[0] instanceof ElementDescriptor) {
                    return new Status(IStatus.OK, // severity
                            AdtPlugin.PLUGIN_ID, //plugin id
                            IStatus.OK, // code
                            "", //$NON-NLS-1$ // msg
                            null); // exception
                } else {
                    return new Status(IStatus.ERROR, // severity
                            AdtPlugin.PLUGIN_ID, //plugin id
                            IStatus.ERROR, // code
                            "Invalid selection", // msg, translatable 
                            null); // exception
                }
            }
        });
    }

    /**
     * @return The root node selected by the user, either root node or the
     *         one passed to the constructor if not null.
     */
    public UiElementNode getChosenRootNode() {
        return mChosenRootNode;
    }

    /**
     * Internal helper to compute the result. Returns the selection from
     * the list view, if any.
     */
    @Override
    protected void computeResult() {
        setResult(Arrays.asList(getSelectedElements()));
    }

    /**
     * Creates the dialog area.
     * 
     * First add a radio area, which may be either 2 radio controls or
     * just a message area if there's only one choice (the app root node).
     * 
     * Then uses the default from the AbstractElementListSelectionDialog
     * which is to add both a filter text and a filtered list. Adding both
     * is necessary (since the base class accesses both internal directly
     * fields without checking for null pointers.) 
     * 
     * Finally sets the initial selection list.
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite contents = (Composite) super.createDialogArea(parent);

        createRadioControl(contents);
        createFilterText(contents);
        createFilteredList(contents);

        // Initialize the list state.
        // This must be done after the filtered list as been created.
        chooseNode(mChosenRootNode);
        setSelection(getInitialElementSelections().toArray());
        return contents;
    }
    
    /**
     * Creates the message text widget and sets layout data.
     * @param content the parent composite of the message area.
     */
    private Composite createRadioControl(Composite content) {
        
        if (mSelectedUiNode != null) {
            Button radio1 = new Button(content, SWT.RADIO);
            radio1.setText(String.format("Create a new element at the top level, in %1$s.",
                    mLocalRootNode.getShortDescription()));

            Button radio2 = new Button(content, SWT.RADIO);
            radio2.setText(String.format("Create a new element in the selected element, %1$s.",
                    mSelectedUiNode.getBreadcrumbTrailDescription(false /* include_root */)));

            // Set the initial selection before adding the listeners
            // (they can't be run till the filtered list has been created)
            radio1.setSelection(false);
            radio2.setSelection(true);
            mChosenRootNode = mSelectedUiNode;
            
            radio1.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    chooseNode(mLocalRootNode);
                }
            });
            
            radio2.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    chooseNode(mSelectedUiNode);
                }
            });
        } else {
            setMessage(String.format("Create a new element at the top level, in %1$s.",
                    mLocalRootNode.getShortDescription()));
            createMessageArea(content);

            mChosenRootNode = mLocalRootNode;
        }
         
        return content;
    }

    /**
     * Internal helper to remember the root node choosen by the user.
     * It also sets the list view to the adequate list of children that can
     * be added to the chosen root node.
     * 
     * If the chosen root node is mLocalRootNode and a descriptor filter was specified
     * when creating the master-detail part, we use this as the set of nodes that
     * can be created on the root node.
     * 
     * @param ui_node The chosen root node, either mLocalRootNode or
     *                mSelectedUiNode.
     */
    private void chooseNode(UiElementNode ui_node) {
        mChosenRootNode = ui_node;

        if (ui_node == mLocalRootNode && 
                mDescriptorFilters != null &&
                mDescriptorFilters.length != 0) {
            setListElements(mDescriptorFilters);
        } else {
            setListElements(ui_node.getDescriptor().getChildren());
        }
    }
}
