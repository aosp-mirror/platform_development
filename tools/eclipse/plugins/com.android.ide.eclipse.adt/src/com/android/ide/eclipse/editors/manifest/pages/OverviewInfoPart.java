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

package com.android.ide.eclipse.editors.manifest.pages;

import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.manifest.ManifestEditor;
import com.android.ide.eclipse.editors.manifest.descriptors.AndroidManifestDescriptors;
import com.android.ide.eclipse.editors.ui.UiElementPart;
import com.android.ide.eclipse.editors.uimodel.UiAttributeNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.w3c.dom.Node;

/**
 * Generic info section part for overview page
 */
final class OverviewInfoPart extends UiElementPart {

    private IManagedForm mManagedForm;

    public OverviewInfoPart(Composite body, FormToolkit toolkit, ManifestEditor editor) {
        super(body, toolkit, editor,
                null,  // uiElementNode
                "General Information", // section title
                "Defines general information about the AndroidManifest.xml", // section description
                Section.TWISTIE | Section.EXPANDED);
    }

    /**
     * Retrieves the UiElementNode that this part will edit. The node must exist
     * and can't be null, by design, because it's a mandatory node.
     */
    private static UiElementNode getManifestUiNode(ManifestEditor editor) {
        AndroidManifestDescriptors manifestDescriptors = editor.getManifestDescriptors();
        if (manifestDescriptors != null) {
            ElementDescriptor desc = manifestDescriptors.getManifestElement();
            if (editor.getUiRootNode().getDescriptor() == desc) {
                return editor.getUiRootNode();
            } else {
                return editor.getUiRootNode().findUiChildNode(desc.getXmlName());
            }
        }
        
        // No manifest descriptor: we have a dummy UiRootNode, so we return that.
        // The editor will be reloaded once we have the proper descriptors anyway.
        return editor.getUiRootNode();
    }

    /**
     * Retrieves the uses-sdk UI node. Since this is a mandatory node, it *always*
     * exists, even if there is no matching XML node.
     */
    private UiElementNode getUsesSdkUiNode(ManifestEditor editor) {
        AndroidManifestDescriptors manifestDescriptors = editor.getManifestDescriptors();
        if (manifestDescriptors != null) {
            ElementDescriptor desc = manifestDescriptors.getUsesSdkElement();
            return editor.getUiRootNode().findUiChildNode(desc.getXmlName());
        }
        
        // No manifest descriptor: we have a dummy UiRootNode, so we return that.
        // The editor will be reloaded once we have the proper descriptors anyway.
        return editor.getUiRootNode();
    }

    /**
     * Overridden in order to capture the current managed form.
     * 
     * {@inheritDoc}
     */
    @Override
    protected void createFormControls(final IManagedForm managedForm) {
        mManagedForm = managedForm; 
        super.createFormControls(managedForm);
    }

    /**
     * Removes any existing Attribute UI widgets and recreate them if the SDK has changed.
     * <p/>
     * This is called by {@link OverviewPage#refreshUiApplicationNode()} when the
     * SDK has changed.
     */
    public void onSdkChanged() {
        createUiAttributes(mManagedForm);
    }
    
    /**
     * Overridden to add the description and the ui attributes of both the
     * manifest and uses-sdk UI nodes.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    protected void fillTable(Composite table, IManagedForm managedForm) {
        int n = 0;

        UiElementNode uiNode = getManifestUiNode(getEditor());
        n += insertUiAttributes(uiNode, table, managedForm);

        uiNode = getUsesSdkUiNode(getEditor());
        n += insertUiAttributes(uiNode, table, managedForm);

        if (n == 0) {
            createLabel(table, managedForm.getToolkit(),
                    "No attributes to display, waiting for SDK to finish loading...",
                    null /* tooltip */ );
        }
        
        layoutChanged();
    }

    /**
     * Overridden to tests whether either the manifest or uses-sdk nodes parts are dirty.
     * <p/>
     * {@inheritDoc}
     * 
     * @return <code>true</code> if the part is dirty, <code>false</code>
     *         otherwise.
     */
    @Override
    public boolean isDirty() {
        boolean dirty = super.isDirty();
        
        if (!dirty) {
            UiElementNode uiNode = getManifestUiNode(getEditor());
            if (uiNode != null) {
                for (UiAttributeNode ui_attr : uiNode.getUiAttributes()) {
                    if (ui_attr.isDirty()) {
                        markDirty();
                        dirty = true;
                        break;
                    }
                }
            }
        }

        if (!dirty) {
            UiElementNode uiNode = getUsesSdkUiNode(getEditor());
            if (uiNode != null) {
                for (UiAttributeNode ui_attr : uiNode.getUiAttributes()) {
                    if (ui_attr.isDirty()) {
                        markDirty();
                        dirty = true;
                        break;
                    }
                }
            }
        }

        return dirty;
    }
    
    /**
     * Overridden to save both the manifest or uses-sdk nodes.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void commit(boolean onSave) {
        final UiElementNode manifestUiNode = getManifestUiNode(getEditor());
        final UiElementNode usesSdkUiNode = getUsesSdkUiNode(getEditor());

        getEditor().editXmlModel(new Runnable() {
            public void run() {
                if (manifestUiNode != null && manifestUiNode.isDirty()) {
                    for (UiAttributeNode ui_attr : manifestUiNode.getUiAttributes()) {
                        ui_attr.commit();
                    }
                }

                if (usesSdkUiNode != null && usesSdkUiNode.isDirty()) {
                    for (UiAttributeNode ui_attr : usesSdkUiNode.getUiAttributes()) {
                        ui_attr.commit();
                    }

                    if (!usesSdkUiNode.isDirty()) {
                        // Remove the <uses-sdk> XML element if it is empty.
                        // Rather than rely on the internal UI state, actually check that the
                        // XML element has no attributes and no child element so that we don't
                        // trash some user-generated content.
                        Node element = usesSdkUiNode.prepareCommit();
                        if (element != null &&
                                !element.hasAttributes() &&
                                !element.hasChildNodes()) {
                            // Important note: we MUST NOT use usesSdkUiNode.deleteXmlNode()
                            // here, as it would clear the UiAttribute list and thus break the
                            // link between the controls and the ui attribute field.
                            // Instead what we want is simply to remove the XML node and let the
                            // UiElementNode node know.
                            Node parent = element.getParentNode();
                            if (parent != null) {
                                parent.removeChild(element);
                                usesSdkUiNode.loadFromXmlNode(null /*xml_node*/);
                            }
                        }
                    }
                }
            }
        });

        // We need to call super's commit after we synchronized the nodes to make sure we
        // reset the dirty flag after all the side effects from committing have occurred.
        super.commit(onSave);
    }
}
