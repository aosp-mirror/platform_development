/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.android.ide.eclipse.editors.ui.tree;

import com.android.ide.eclipse.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Performs basic actions on an XML tree: add node, remove node, move up/down.
 */
public abstract class UiActions implements ICommitXml {

    public UiActions() {
    }

    //---------------------
    // Actual implementations must override these to provide specific hooks

    /** Returns the UiDocumentNode for the current model. */
    abstract protected UiElementNode getRootNode();

    /** Commits pending data before the XML model is modified. */
    abstract public void commitPendingXmlChanges();

    /**
     * Utility method to select an outline item based on its model node
     * 
     * @param ui_node The node to select. Can be null (in which case nothing should happen)
     */
    abstract protected void selectUiNode(UiElementNode ui_node);

    //---------------------

    /**
     * Called when the "Add..." button next to the tree view is selected.
     * <p/>
     * This simplified version of doAdd does not support descriptor filters and creates
     * a new {@link UiModelTreeLabelProvider} for each call.
     */
    public void doAdd(UiElementNode ui_node, Shell shell) {
        doAdd(ui_node, null /* descriptorFilters */, shell, new UiModelTreeLabelProvider());
    }
    
    /**
     * Called when the "Add..." button next to the tree view is selected.
     * 
     * Displays a selection dialog that lets the user select which kind of node
     * to create, depending on the current selection.
     */
    public void doAdd(UiElementNode ui_node,
            ElementDescriptor[] descriptorFilters,
            Shell shell, ILabelProvider labelProvider) {
        // If the root node is a document with already a root, use it as the root node
        UiElementNode root_node = getRootNode();
        if (root_node instanceof UiDocumentNode && 
                root_node.getUiChildren().size() > 0) {
            root_node = root_node.getUiChildren().get(0);
        }

        NewItemSelectionDialog dlg = new NewItemSelectionDialog(
                shell,
                labelProvider,
                descriptorFilters,
                ui_node, root_node);
        dlg.open();
        Object[] results = dlg.getResult();
        if (results != null && results.length > 0) {
            UiElementNode ui_new = addNewTreeElement(dlg.getChosenRootNode(),
                    (ElementDescriptor) results[0]);

            selectUiNode(ui_new);
        }
    }

    /**
     * Called when the "Remove" button is selected.
     * 
     * If the tree has a selection, remove it.
     * This simply deletes the XML node attached to the UI node: when the XML model fires the
     * update event, the tree will get refreshed.
     */
    public void doRemove(final UiElementNode ui_node, Shell shell) {
        if (MessageDialog.openQuestion(shell,
                "Remove element from Android XML",  // title
                String.format("Do you really want to remove %1$s?",
                        ui_node.getBreadcrumbTrailDescription(false /* include_root */)))) {
            commitPendingXmlChanges();
            getRootNode().getEditor().editXmlModel(new Runnable() {
                public void run() {
                    UiElementNode previous = ui_node.getUiPreviousSibling();
                    UiElementNode parent = ui_node.getUiParent();
                    
                    // delete node
                    ui_node.deleteXmlNode();
                    
                    // try to select the previous sibling or the parent
                    if (previous != null) {
                        selectUiNode(previous);
                    } else if (parent != null) {
                        selectUiNode(parent);
                    }
                }
            });
        }
    }

    /**
     * Called when the "Up" button is selected.
     * <p/>
     * If the tree has a selection, move it up, either in the child list or as the last child
     * of the previous parent.
     */
    public void doUp(final UiElementNode ui_node) {
        final Node[] select_xml_node = { null };
        // the node will move either up to its parent or grand-parent
        UiElementNode search_root = ui_node.getUiParent();
        if (search_root != null && search_root.getUiParent() != null) {
            search_root = search_root.getUiParent();
        }

        commitPendingXmlChanges();
        getRootNode().getEditor().editXmlModel(new Runnable() {
            public void run() {
                Node xml_node = ui_node.getXmlNode();
                if (xml_node != null) {
                    Node xml_parent = xml_node.getParentNode();
                    if (xml_parent != null) {
                        UiElementNode ui_prev = ui_node.getUiPreviousSibling();
                        if (ui_prev != null && ui_prev.getXmlNode() != null) {
                            // This node is not the first one of the parent, so it can be
                            // removed and then inserted before its previous sibling.
                            // If the previous sibling can have children, though, then it
                            // is inserted at the end of the children list.
                            Node xml_prev = ui_prev.getXmlNode();
                            if (ui_prev.getDescriptor().hasChildren()) {
                                xml_prev.appendChild(xml_parent.removeChild(xml_node));
                                select_xml_node[0] = xml_node;
                            } else {
                                xml_parent.insertBefore(
                                        xml_parent.removeChild(xml_node),
                                        xml_prev);
                                select_xml_node[0] = xml_node;
                            }
                        } else if (!(xml_parent instanceof Document) &&
                                xml_parent.getParentNode() != null &&
                                !(xml_parent.getParentNode() instanceof Document)) {
                            // If the node is the first one of the child list of its
                            // parent, move it up in the hierarchy as previous sibling
                            // to the parent. This is only possible if the parent of the
                            // parent is not a document.
                            Node grand_parent = xml_parent.getParentNode();
                            grand_parent.insertBefore(xml_parent.removeChild(xml_node),
                                    xml_parent);
                            select_xml_node[0] = xml_node;
                        }
                    }
                }
            }
        });

        if (select_xml_node[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(ui_node);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (search_root == null) {
                search_root = ui_node.getUiRoot();
            }
            if (search_root != null) {
                selectUiNode(search_root.findXmlNode(select_xml_node[0]));
            }
        }
    }

    /**
     * Called when the "Down" button is selected.
     * 
     * If the tree has a selection, move it down, either in the same child list or as the
     * first child of the next parent.
     */
    public void doDown(final UiElementNode ui_node) {
        final Node[] select_xml_node = { null };
        // the node will move either down to its parent or grand-parent
        UiElementNode search_root = ui_node.getUiParent();
        if (search_root != null && search_root.getUiParent() != null) {
            search_root = search_root.getUiParent();
        }

        commitPendingXmlChanges();
        getRootNode().getEditor().editXmlModel(new Runnable() {
            public void run() {
                Node xml_node = ui_node.getXmlNode();
                if (xml_node != null) {
                    Node xml_parent = xml_node.getParentNode();
                    if (xml_parent != null) {
                        UiElementNode ui_next = ui_node.getUiNextSibling();
                        if (ui_next != null && ui_next.getXmlNode() != null) {
                            // This node is not the last one of the parent, so it can be
                            // removed and then inserted after its next sibling.
                            // If the next sibling is a node that can have children, though,
                            // then the node is inserted as the first child.
                            Node xml_next = ui_next.getXmlNode();
                            if (ui_next.getDescriptor().hasChildren()) {
                                // Note: insertBefore works as append if the ref node is
                                // null, i.e. when the node doesn't have children yet.
                                xml_next.insertBefore(xml_parent.removeChild(xml_node),
                                        xml_next.getFirstChild());
                                select_xml_node[0] = xml_node;
                            } else {
                                // Insert "before after next" ;-)
                                xml_parent.insertBefore(xml_parent.removeChild(xml_node),
                                        xml_next.getNextSibling());
                                select_xml_node[0] = xml_node;
                            }
                        } else if (!(xml_parent instanceof Document) &&
                                xml_parent.getParentNode() != null &&
                                !(xml_parent.getParentNode() instanceof Document)) {
                            // This node is the last node of its parent.
                            // If neither the parent nor the grandparent is a document,
                            // then the node can be insert right after the parent.
                            Node grand_parent = xml_parent.getParentNode();
                            grand_parent.insertBefore(xml_parent.removeChild(xml_node),
                                    xml_parent.getNextSibling());
                            select_xml_node[0] = xml_node;
                        }
                    }
                }
            }
        });

        if (select_xml_node[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(ui_node);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (search_root == null) {
                search_root = ui_node.getUiRoot();
            }
            if (search_root != null) {
                selectUiNode(search_root.findXmlNode(select_xml_node[0]));
            }
        }
    }

    //---------------------
    
    /**
     * Adds a new element of the given descriptor's type to the given UI parent node.
     * 
     * This actually creates the corresponding XML node in the XML model, which in turn
     * will refresh the current tree view.
     *  
     * @param ui_parent An existing UI node or null to add to the tree root
     * @param elementDescriptor The descriptor of the element to add
     * @return The {@link UiElementNode} that has been added to the UI tree.
     */
    private UiElementNode addNewTreeElement(UiElementNode ui_parent,
            ElementDescriptor elementDescriptor) {
        commitPendingXmlChanges();
        final UiElementNode ui_new = ui_parent.appendNewUiChild(elementDescriptor);
        UiElementNode root_node = getRootNode();

        root_node.getEditor().editXmlModel(new Runnable() {
            public void run() {
                DescriptorsUtils.setDefaultLayoutAttributes(ui_new);
                ui_new.createXmlNode();
            }
        });
        return ui_new;
    }
}
