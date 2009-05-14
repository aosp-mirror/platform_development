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
import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.adt.internal.editors.ui.UiElementPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener.UiUpdateState;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Appllication Toogle section part for application page.
 */
final class ApplicationToggle extends UiElementPart {
    
    /** Checkbox indicating whether an application node is present */ 
    private Button mCheckbox;
    /** Listen to changes to the UI node for <application> and updates the checkbox */
    private AppNodeUpdateListener mAppNodeUpdateListener;
    /** Internal flag to know where we're programmatically modifying the checkbox and we want to
     *  avoid triggering the checkbox's callback. */
    public boolean mInternalModification;
    private FormText mTooltipFormText;

    public ApplicationToggle(Composite body, FormToolkit toolkit, ManifestEditor editor,
            UiElementNode applicationUiNode) {
        super(body, toolkit, editor, applicationUiNode,
                "Application Toggle",
                null, /* description */
                Section.TWISTIE | Section.EXPANDED);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (getUiElementNode() != null && mAppNodeUpdateListener != null) {
            getUiElementNode().removeUpdateListener(mAppNodeUpdateListener);
            mAppNodeUpdateListener = null;
        }
    }
    
    /**
     * Changes and refreshes the Application UI node handle by the this part.
     */
    @Override
    public void setUiElementNode(UiElementNode uiElementNode) {
        super.setUiElementNode(uiElementNode);

        updateTooltip();

        // Set the state of the checkbox
        mAppNodeUpdateListener.uiElementNodeUpdated(getUiElementNode(),
                UiUpdateState.CHILDREN_CHANGED);
    }

    /**
     * Create the controls to edit the attributes for the given ElementDescriptor.
     * <p/>
     * This MUST not be called by the constructor. Instead it must be called from
     * <code>initialize</code> (i.e. right after the form part is added to the managed form.)
     * 
     * @param managedForm The owner managed form
     */
    @Override
    protected void createFormControls(IManagedForm managedForm) {
        FormToolkit toolkit = managedForm.getToolkit();
        Composite table = createTableLayout(toolkit, 1 /* numColumns */);

        mTooltipFormText = createFormText(table, toolkit, true, "<form></form>",
                false /* setupLayoutData */);
        updateTooltip();

        mCheckbox = toolkit.createButton(table,
                "Define an <application> tag in the AndroidManifest.xml",
                SWT.CHECK);
        mCheckbox.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB, TableWrapData.TOP));
        mCheckbox.setSelection(false);
        mCheckbox.addSelectionListener(new CheckboxSelectionListener());

        mAppNodeUpdateListener = new AppNodeUpdateListener();
        getUiElementNode().addUpdateListener(mAppNodeUpdateListener);

        // Initialize the state of the checkbox
        mAppNodeUpdateListener.uiElementNodeUpdated(getUiElementNode(),
                UiUpdateState.CHILDREN_CHANGED);
        
        // Tell the section that the layout has changed.
        layoutChanged();
    }

    /**
     * Updates the application tooltip in the form text.
     * If there is no tooltip, the form text is hidden. 
     */
    private void updateTooltip() {
        boolean isVisible = false;

        String tooltip = getUiElementNode().getDescriptor().getTooltip();
        if (tooltip != null) {
            tooltip = DescriptorsUtils.formatFormText(tooltip,
                    getUiElementNode().getDescriptor(),
                    Sdk.getCurrent().getDocumentationBaseUrl());
    
            mTooltipFormText.setText(tooltip, true /* parseTags */, true /* expandURLs */);
            mTooltipFormText.setImage(DescriptorsUtils.IMAGE_KEY, AdtPlugin.getAndroidLogo());
            mTooltipFormText.addHyperlinkListener(getEditor().createHyperlinkListener());
            isVisible = true;
        }
        
        mTooltipFormText.setVisible(isVisible);
    }

    /**
     * This listener synchronizes the XML application node when the checkbox
     * is changed by the user.
     */
    private class CheckboxSelectionListener extends SelectionAdapter {
        private Node mUndoXmlNode;
        private Node mUndoXmlParent;
        private Node mUndoXmlNextNode;
        private Node mUndoXmlNextElement;
        private Document mUndoXmlDocument;

        @Override
        public void widgetSelected(SelectionEvent e) {
            super.widgetSelected(e);
            if (!mInternalModification && getUiElementNode() != null) {
                getUiElementNode().getEditor().wrapUndoRecording(
                        mCheckbox.getSelection()
                            ? "Create or restore Application node"
                            : "Remove Application node",
                        new Runnable() {
                            public void run() {
                                getUiElementNode().getEditor().editXmlModel(new Runnable() {
                                    public void run() {
                                        if (mCheckbox.getSelection()) {
                                            // The user wants an <application> node.
                                            // Either restore a previous one
                                            // or create a full new one.
                                            boolean create = true;
                                            if (mUndoXmlNode != null) {
                                                create = !restoreApplicationNode();
                                            }
                                            if (create) {
                                                getUiElementNode().createXmlNode();
                                            }
                                        } else {
                                            // Users no longer wants the <application> node.
                                            removeApplicationNode();
                                        }
                                    }
                                });
                            }
                });
            }
        }

        /**
         * Restore a previously "saved" application node.
         * 
         * @return True if the node could be restored, false otherwise.
         */
        private boolean restoreApplicationNode() {
            if (mUndoXmlDocument == null || mUndoXmlNode == null) {
                return false;
            }

            // Validate node references...
            mUndoXmlParent = validateNode(mUndoXmlDocument, mUndoXmlParent);
            mUndoXmlNextNode = validateNode(mUndoXmlDocument, mUndoXmlNextNode);
            mUndoXmlNextElement = validateNode(mUndoXmlDocument, mUndoXmlNextElement);

            if (mUndoXmlParent == null){
                // If the parent node doesn't exist, try to find a new manifest node.
                // If it doesn't exist, create it.
                mUndoXmlParent = getUiElementNode().getUiParent().prepareCommit();
                mUndoXmlNextNode = null;
                mUndoXmlNextElement = null;
            }

            boolean success = false;
            if (mUndoXmlParent != null) {
                // If the parent is still around, reuse the same node.

                // Ideally we want to insert the node before what used to be its next sibling.
                // If that's not possible, we try to insert it before its next sibling element.
                // If that's not possible either, it will be inserted at the end of the parent's.
                Node next = mUndoXmlNextNode;
                if (next == null) {
                    next = mUndoXmlNextElement;
                }
                mUndoXmlParent.insertBefore(mUndoXmlNode, next);
                if (next == null) {
                    Text sep = mUndoXmlDocument.createTextNode("\n");  //$NON-NLS-1$
                    mUndoXmlParent.insertBefore(sep, null);  // insert separator before end tag
                }
                success = true;
            } 

            // Remove internal references to avoid using them twice
            mUndoXmlParent = null;
            mUndoXmlNextNode = null;
            mUndoXmlNextElement = null;
            mUndoXmlNode = null;
            mUndoXmlDocument = null;
            return success;
        }

        /**
         * Validates that the given xml_node is still either the root node or one of its
         * direct descendants. 
         * 
         * @param root_node The root of the node hierarchy to examine.
         * @param xml_node The XML node to find.
         * @return Returns xml_node if it is, otherwise returns null.
         */
        private Node validateNode(Node root_node, Node xml_node) {
            if (root_node == xml_node) {
                return xml_node;
            } else {
                for (Node node = root_node.getFirstChild(); node != null;
                        node = node.getNextSibling()) {
                    if (root_node == xml_node || validateNode(node, xml_node) != null) {
                        return xml_node;
                    }
                }
            }
            return null;
        }

        /**
         * Removes the <application> node from the hierarchy.
         * Before doing that, we try to remember where it was so that we can put it back
         * in the same place.
         */
        private void removeApplicationNode() {
            // Make sure the node actually exists...
            Node xml_node = getUiElementNode().getXmlNode();
            if (xml_node == null) {
                return;
            }

            // Save its parent, next sibling and next element
            mUndoXmlDocument = xml_node.getOwnerDocument();
            mUndoXmlParent = xml_node.getParentNode();
            mUndoXmlNextNode = xml_node.getNextSibling();
            mUndoXmlNextElement = mUndoXmlNextNode;
            while (mUndoXmlNextElement != null &&
                    mUndoXmlNextElement.getNodeType() != Node.ELEMENT_NODE) {
                mUndoXmlNextElement = mUndoXmlNextElement.getNextSibling();
            }

            // Actually remove the node from the hierarchy and keep it here.
            // The returned node looses its parents/siblings pointers.
            mUndoXmlNode = getUiElementNode().deleteXmlNode();
        }
    }

    /**
     * This listener synchronizes the UI (i.e. the checkbox) with the
     * actual presence of the application XML node.
     */
    private class AppNodeUpdateListener implements IUiUpdateListener {        
        public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
            // The UiElementNode for the application XML node always exists, even
            // if there is no corresponding XML node in the XML file.
            //
            // To update the checkbox to reflect the actual state, we just need
            // to check if the XML node is null.
            try {
                mInternalModification = true;
                boolean exists = ui_node.getXmlNode() != null;
                if (mCheckbox.getSelection() != exists) {
                    mCheckbox.setSelection(exists);
                }
            } finally {
                mInternalModification = false;
            }
            
        }
    }
}
