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


package com.android.ide.eclipse.adt.internal.editors.ui.tree;

import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.List;

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
     * @param uiNode The node to select. Can be null (in which case nothing should happen)
     */
    abstract protected void selectUiNode(UiElementNode uiNode);

    //---------------------

    /**
     * Called when the "Add..." button next to the tree view is selected.
     * <p/>
     * This simplified version of doAdd does not support descriptor filters and creates
     * a new {@link UiModelTreeLabelProvider} for each call.
     */
    public void doAdd(UiElementNode uiNode, Shell shell) {
        doAdd(uiNode, null /* descriptorFilters */, shell, new UiModelTreeLabelProvider());
    }
    
    /**
     * Called when the "Add..." button next to the tree view is selected.
     * 
     * Displays a selection dialog that lets the user select which kind of node
     * to create, depending on the current selection.
     */
    public void doAdd(UiElementNode uiNode,
            ElementDescriptor[] descriptorFilters,
            Shell shell, ILabelProvider labelProvider) {
        // If the root node is a document with already a root, use it as the root node
        UiElementNode rootNode = getRootNode();
        if (rootNode instanceof UiDocumentNode && rootNode.getUiChildren().size() > 0) {
            rootNode = rootNode.getUiChildren().get(0);
        }

        NewItemSelectionDialog dlg = new NewItemSelectionDialog(
                shell,
                labelProvider,
                descriptorFilters,
                uiNode, rootNode);
        dlg.open();
        Object[] results = dlg.getResult();
        if (results != null && results.length > 0) {
            addElement(dlg.getChosenRootNode(), null, (ElementDescriptor) results[0],
                    true /*updateLayout*/);
        }
    }

    /**
     * Adds a new XML element based on the {@link ElementDescriptor} to the given parent
     * {@link UiElementNode}, and then select it.
     * <p/>
     * If the parent is a document root which already contains a root element, the inner
     * root element is used as the actual parent. This ensure you can't create a broken
     * XML file with more than one root element.
     * <p/>
     * If a sibling is given and that sibling has the same parent, the new node is added
     * right after that sibling. Otherwise the new node is added at the end of the parent
     * child list.
     * 
     * @param uiParent An existing UI node or null to add to the tree root
     * @param uiSibling An existing UI node before which to insert the new node. Can be null.
     * @param descriptor The descriptor of the element to add
     * @param updateLayout True if layout attributes should be set
     * @return The new {@link UiElementNode} or null.
     */
    public UiElementNode addElement(UiElementNode uiParent,
            UiElementNode uiSibling,
            ElementDescriptor descriptor,
            boolean updateLayout) {
        if (uiParent instanceof UiDocumentNode && uiParent.getUiChildren().size() > 0) {
            uiParent = uiParent.getUiChildren().get(0);
        }
        if (uiSibling != null && uiSibling.getUiParent() != uiParent) {
            uiSibling = null;
        }

        UiElementNode uiNew = addNewTreeElement(uiParent, uiSibling, descriptor, updateLayout);
        selectUiNode(uiNew);
        
        return uiNew;
    }

    /**
     * Called when the "Remove" button is selected.
     * 
     * If the tree has a selection, remove it.
     * This simply deletes the XML node attached to the UI node: when the XML model fires the
     * update event, the tree will get refreshed.
     */
    public void doRemove(final List<UiElementNode> nodes, Shell shell) {
        
        if (nodes == null || nodes.size() == 0) {
            return;
        }
        
        final int len = nodes.size();
        
        StringBuilder sb = new StringBuilder();
        for (UiElementNode node : nodes) {
            sb.append("\n- "); //$NON-NLS-1$
            sb.append(node.getBreadcrumbTrailDescription(false /* include_root */));
        }
        
        if (MessageDialog.openQuestion(shell,
                len > 1 ? "Remove elements from Android XML"  // title
                        : "Remove element from Android XML",
                String.format("Do you really want to remove %1$s?", sb.toString()))) {
            commitPendingXmlChanges();
            getRootNode().getEditor().editXmlModel(new Runnable() {
                public void run() {
                    UiElementNode previous = null;
                    UiElementNode parent = null;

                    for (int i = len - 1; i >= 0; i--) {
                        UiElementNode node = nodes.get(i);
                        previous = node.getUiPreviousSibling();
                        parent = node.getUiParent();
                        
                        // delete node
                        node.deleteXmlNode();
                    }
                    
                    // try to select the last previous sibling or the last parent
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
    public void doUp(final List<UiElementNode> nodes) {
        if (nodes == null || nodes.size() < 1) {
            return;
        }
        
        final Node[] select_xml_node = { null };
        UiElementNode last_node = null;
        UiElementNode search_root = null;
        
        for (int i = 0; i < nodes.size(); i++) {
            final UiElementNode node = last_node = nodes.get(i);
            
            // the node will move either up to its parent or grand-parent
            search_root = node.getUiParent();
            if (search_root != null && search_root.getUiParent() != null) {
                search_root = search_root.getUiParent();
            }
    
            commitPendingXmlChanges();
            getRootNode().getEditor().editXmlModel(new Runnable() {
                public void run() {
                    Node xml_node = node.getXmlNode();
                    if (xml_node != null) {
                        Node xml_parent = xml_node.getParentNode();
                        if (xml_parent != null) {
                            UiElementNode ui_prev = node.getUiPreviousSibling();
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
        }

        if (select_xml_node[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(last_node);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (search_root == null) {
                search_root = last_node.getUiRoot();
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
    public void doDown(final List<UiElementNode> nodes) {
        if (nodes == null || nodes.size() < 1) {
            return;
        }
        
        final Node[] select_xml_node = { null };
        UiElementNode last_node = null;
        UiElementNode search_root = null;

        for (int i = nodes.size() - 1; i >= 0; i--) {
            final UiElementNode node = last_node = nodes.get(i);
            // the node will move either down to its parent or grand-parent
            search_root = node.getUiParent();
            if (search_root != null && search_root.getUiParent() != null) {
                search_root = search_root.getUiParent();
            }
    
            commitPendingXmlChanges();
            getRootNode().getEditor().editXmlModel(new Runnable() {
                public void run() {
                    Node xml_node = node.getXmlNode();
                    if (xml_node != null) {
                        Node xml_parent = xml_node.getParentNode();
                        if (xml_parent != null) {
                            UiElementNode uiNext = node.getUiNextSibling();
                            if (uiNext != null && uiNext.getXmlNode() != null) {
                                // This node is not the last one of the parent, so it can be
                                // removed and then inserted after its next sibling.
                                // If the next sibling is a node that can have children, though,
                                // then the node is inserted as the first child.
                                Node xml_next = uiNext.getXmlNode();
                                if (uiNext.getDescriptor().hasChildren()) {
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
        }

        if (select_xml_node[0] == null) {
            // The XML node has not been moved, we can just select the same UI node
            selectUiNode(last_node);
        } else {
            // The XML node has moved. At this point the UI model has been reloaded
            // and the XML node has been affected to a new UI node. Find that new UI
            // node and select it.
            if (search_root == null) {
                search_root = last_node.getUiRoot();
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
     * @param uiParent An existing UI node or null to add to the tree root
     * @param uiSibling An existing UI node to insert right before. Can be null. 
     * @param descriptor The descriptor of the element to add
     * @param updateLayout True if layout attributes should be set
     * @return The {@link UiElementNode} that has been added to the UI tree.
     */
    private UiElementNode addNewTreeElement(UiElementNode uiParent,
            final UiElementNode uiSibling,
            ElementDescriptor descriptor,
            final boolean updateLayout) {
        commitPendingXmlChanges();
        
        int index = 0;
        for (UiElementNode uiChild : uiParent.getUiChildren()) {
            if (uiChild == uiSibling) {
                break;
            }
            index++;
        }
        
        final UiElementNode uiNew = uiParent.insertNewUiChild(index, descriptor);
        UiElementNode rootNode = getRootNode();

        rootNode.getEditor().editXmlModel(new Runnable() {
            public void run() {
                DescriptorsUtils.setDefaultLayoutAttributes(uiNew, updateLayout);
                Node xmlNode = uiNew.createXmlNode();
            }
        });
        return uiNew;
    }
}
