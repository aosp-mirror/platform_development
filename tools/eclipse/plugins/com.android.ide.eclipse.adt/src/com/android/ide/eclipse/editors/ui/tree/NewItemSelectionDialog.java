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
import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.layout.descriptors.ViewElementDescriptor;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

import org.eclipse.core.resources.IFile;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.dialogs.AbstractElementListSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.part.FileEditorInput;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

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
     *  of the same type in the root will be displayed. Can be null. */
    private ElementDescriptor[] mDescriptorFilters;
    /** The key for the {@link #setLastUsedXmlName(Object[])}. It corresponds to the full
     * workspace path of the currently edited file, if this can be computed. This is computed
     * by {@link #getLastUsedXmlName(UiElementNode)}, called from the constructor. */
    private String mLastUsedKey;
    /** A static map of known XML Names used for a given file. The map has full workspace
     * paths as key and XML names as values. */
    private static final Map<String, String> sLastUsedXmlName = new HashMap<String, String>();
    /** The potential XML Name to initially select in the selection dialog. This is computed
     * in the constructor and set by {@link #setInitialSelection(UiElementNode)}. */
    private String mInitialXmlName;

    /**
     * Creates the new item selection dialog.
     * 
     * @param shell The parent shell for the list.
     * @param labelProvider ILabelProvider for the list.
     * @param descriptorFilters The element allows at the root of the tree. Can be null.
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
        
        // Determine the initial selection using a couple heuristics.
        
        // First check if we can get the last used node type for this file.
        // The heuristic is that generally one keeps adding the same kind of items to the
        // same file, so reusing the last used item type makes most sense.
        String xmlName = getLastUsedXmlName(root_node);
        if (xmlName == null) {
            // Another heuristic is to find the most used item and default to that.
            xmlName = getMostUsedXmlName(root_node);
        }
        if (xmlName == null) {
            // Finally the last heuristic is to see if there's an item with a name
            // similar to the edited file name.
            xmlName = getLeafFileName(root_node);
        }
        // Set the potential name. Selecting the right item is done later by setInitialSelection().
        mInitialXmlName = xmlName;
    }

    /**
     * Returns a potential XML name based on the file name.
     * The item name is marked with an asterisk to identify it as a partial match.
     */
    private String getLeafFileName(UiElementNode ui_node) {
        if (ui_node != null) {
            AndroidEditor editor = ui_node.getEditor();
            if (editor != null) {
                IEditorInput editorInput = editor.getEditorInput();
                if (editorInput instanceof FileEditorInput) {
                    IFile f = ((FileEditorInput) editorInput).getFile();
                    if (f != null) {
                        String leafName = f.getFullPath().removeFileExtension().lastSegment();
                        return "*" + leafName; //$NON-NLS-1$
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Given a potential non-null root node, this method looks for the currently edited
     * file path and uses it as a key to retrieve the last used item for this file by this
     * selection dialog. Returns null if nothing can be found, otherwise returns the string
     * name of the item.
     */
    private String getLastUsedXmlName(UiElementNode ui_node) {
        if (ui_node != null) {
            AndroidEditor editor = ui_node.getEditor();
            if (editor != null) {
                IEditorInput editorInput = editor.getEditorInput();
                if (editorInput instanceof FileEditorInput) {
                    IFile f = ((FileEditorInput) editorInput).getFile();
                    if (f != null) {
                        mLastUsedKey = f.getFullPath().toPortableString();
    
                        return sLastUsedXmlName.get(mLastUsedKey);
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Sets the last used item for this selection dialog for this file.
     * @param objects The currently selected items. Only the first one is used if it is an
     *                {@link ElementDescriptor}.
     */
    private void setLastUsedXmlName(Object[] objects) {
        if (mLastUsedKey != null &&
                objects != null &&
                objects.length > 0 &&
                objects[0] instanceof ElementDescriptor) {
            ElementDescriptor desc = (ElementDescriptor) objects[0];
            sLastUsedXmlName.put(mLastUsedKey, desc.getXmlName());
        }
    }

    /**
     * Returns the most used sub-element name, if any, or null.
     */
    private String getMostUsedXmlName(UiElementNode ui_node) {
        if (ui_node != null) {
            TreeMap<String, Integer> counts = new TreeMap<String, Integer>();
            int max = -1;
            
            for (UiElementNode child : ui_node.getUiChildren()) {
                String name = child.getDescriptor().getXmlName();
                Integer i = counts.get(name);
                int count = i == null ? 1 : i.intValue() + 1;
                counts.put(name, count);
                max = Math.max(max, count);
            }

            if (max > 0) {
                // Find first key with this max and return it
                for (Entry<String, Integer> entry : counts.entrySet()) {
                    if (entry.getValue().intValue() == max) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
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
        setLastUsedXmlName(getSelectedElements());
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
        
        // Set the initial selection
        setInitialSelection(mChosenRootNode);
        return contents;
    }
    
    /**
     * Tries to set the initial selection based on the {@link #mInitialXmlName} computed
     * in the constructor. The selection is only set if there's an element descriptor
     * that matches the same exact XML name. When {@link #mInitialXmlName} starts with an
     * asterisk, it means to do a partial case-insensitive match on the start of the
     * strings.
     */
    private void setInitialSelection(UiElementNode rootNode) {
        ElementDescriptor initialElement = null;

        if (mInitialXmlName != null && mInitialXmlName.length() > 0) {
            String name = mInitialXmlName;
            boolean partial = name.startsWith("*");   //$NON-NLS-1$
            if (partial) {
                name = name.substring(1).toLowerCase();
            }
            
            for (ElementDescriptor desc : getAllowedDescriptors(rootNode)) {
                if (!partial && desc.getXmlName().equals(name)) {
                    initialElement = desc;
                    break;
                } else if (partial) {
                    String name2 = desc.getXmlLocalName().toLowerCase();
                    if (name.startsWith(name2) || name2.startsWith(name)) {
                        initialElement = desc;
                        break;
                    }
                }
            }
        }
        
        setSelection(initialElement == null ? null : new ElementDescriptor[] { initialElement });
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
        setListElements(getAllowedDescriptors(ui_node));
    }

    /**
     * Returns the list of {@link ElementDescriptor}s that can be added to the given
     * UI node.
     * 
     * @param ui_node The UI node to which element should be added. Cannot be null.
     * @return A non-null array of {@link ElementDescriptor}. The array might be empty.
     */
    private ElementDescriptor[] getAllowedDescriptors(UiElementNode ui_node) {
        if (ui_node == mLocalRootNode && 
                mDescriptorFilters != null &&
                mDescriptorFilters.length != 0) {
            return mDescriptorFilters;
        } else {
            return ui_node.getDescriptor().getChildren();
        }
    }
}
