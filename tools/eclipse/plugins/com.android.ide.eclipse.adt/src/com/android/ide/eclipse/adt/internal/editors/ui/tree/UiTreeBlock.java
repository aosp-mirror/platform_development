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

package com.android.ide.eclipse.adt.internal.editors.ui.tree;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.editors.AndroidEditor;
import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper;
import com.android.ide.eclipse.adt.internal.editors.ui.SectionHelper.ManifestSectionPart;
import com.android.ide.eclipse.adt.internal.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.sdk.Sdk.ITargetChangeListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * {@link UiTreeBlock} is a {@link MasterDetailsBlock} which displays a tree view for
 * a specific set of {@link UiElementNode}.
 * <p/>
 * For a given UI element node, the tree view displays all first-level children that
 * match a given type (given by an {@link ElementDescriptor}. All children from these
 * nodes are also displayed.
 * <p/>
 * In the middle next to the tree are some controls to add or delete tree nodes.
 * On the left is a details part that displays all the visible UI attributes for a given
 * selected UI element node.
 */
public final class UiTreeBlock extends MasterDetailsBlock implements ICommitXml {

    /** Height hint for the tree view. Helps the grid layout resize properly on smaller screens. */
    private static final int TREE_HEIGHT_HINT = 50;

    /** Container editor */
    AndroidEditor mEditor;
    /** The root {@link UiElementNode} which contains all the elements that are to be 
     *  manipulated by this tree view. In general this is the manifest UI node. */
    private UiElementNode mUiRootNode;
    /** The descriptor of the elements to be displayed as root in this tree view. All elements
     *  of the same type in the root will be displayed. */
    private ElementDescriptor[] mDescriptorFilters;
    /** The title for the master-detail part (displayed on the top "tab" on top of the tree) */
    private String mTitle;
    /** The description for the master-detail part (displayed on top of the tree view) */
    private String mDescription;
    /** The master-detail part, composed of a main tree and an auxiliary detail part */
    private ManifestSectionPart mMasterPart;
    /** The tree viewer in the master-detail part */
    private TreeViewer mTreeViewer;
    /** The "add" button for the tree view */ 
    private Button mAddButton;
    /** The "remove" button for the tree view */
    private Button mRemoveButton;
    /** The "up" button for the tree view */
    private Button mUpButton;
    /** The "down" button for the tree view */
    private Button mDownButton;
    /** The Managed Form used to create the master part */
    private IManagedForm mManagedForm;
    /** Reference to the details part of the tree master block. */
    private DetailsPart mDetailsPart;
    /** Reference to the clipboard for copy-paste */
    private Clipboard mClipboard;
    /** Listener to refresh the tree viewer when the parent's node has been updated */
    private IUiUpdateListener mUiRefreshListener;
    /** Listener to enable/disable the UI based on the application node's presence */
    private IUiUpdateListener mUiEnableListener;
    /** An adapter/wrapper to use the add/remove/up/down tree edit actions. */
    private UiTreeActions mUiTreeActions;
    /**
     * True if the root node can be created on-demand (i.e. as needed as
     * soon as children exist). False if an external entity controls the existence of the
     * root node. In practise, this is false for the manifest application page (the actual
     * "application" node is managed by the ApplicationToggle part) whereas it is true
     * for all other tree pages.
     */
    private final boolean mAutoCreateRoot;


    /**
     * Creates a new {@link MasterDetailsBlock} that will display all UI nodes matching the
     * given filter in the given root node.
     * 
     * @param editor The parent manifest editor.
     * @param uiRootNode The root {@link UiElementNode} which contains all the elements that are
     *        to be manipulated by this tree view. In general this is the manifest UI node or the
     *        application UI node. This cannot be null.
     * @param autoCreateRoot True if the root node can be created on-demand (i.e. as needed as
     *        soon as children exist). False if an external entity controls the existence of the
     *        root node. In practise, this is false for the manifest application page (the actual
     *        "application" node is managed by the ApplicationToggle part) whereas it is true
     *        for all other tree pages.
     * @param descriptorFilters A list of descriptors of the elements to be displayed as root in
     *        this tree view. Use null or an empty list to accept any kind of node.
     * @param title Title for the section
     * @param description Description for the section
     */
    public UiTreeBlock(AndroidEditor editor,
            UiElementNode uiRootNode,
            boolean autoCreateRoot,
            ElementDescriptor[] descriptorFilters,
            String title,
            String description) {
        mEditor = editor;
        mUiRootNode = uiRootNode;
        mAutoCreateRoot = autoCreateRoot;
        mDescriptorFilters = descriptorFilters;
        mTitle = title;
        mDescription = description;
    }
    
    /** @returns The container editor */
    AndroidEditor getEditor() {
        return mEditor;
    }
    
    /** @returns The reference to the clipboard for copy-paste */
    Clipboard getClipboard() {
        return mClipboard;
    }
    
    /** @returns The master-detail part, composed of a main tree and an auxiliary detail part */
    ManifestSectionPart getMasterPart() {
        return mMasterPart;
    }

    /**
     * Returns the {@link UiElementNode} for the current model.
     * <p/>
     * This is used by the content provider attached to {@link #mTreeViewer} since
     * the uiRootNode changes after each call to
     * {@link #changeRootAndDescriptors(UiElementNode, ElementDescriptor[], boolean)}. 
     */
    public UiElementNode getRootNode() {
        return mUiRootNode;
    }

    @Override
    protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
        FormToolkit toolkit = managedForm.getToolkit();

        mManagedForm = managedForm;
        mMasterPart = new ManifestSectionPart(parent, toolkit);
        Section section = mMasterPart.getSection();
        section.setText(mTitle);
        section.setDescription(mDescription);
        section.setLayout(new GridLayout());
        section.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite grid = SectionHelper.createGridLayout(section, toolkit, 2);

        Tree tree = createTreeViewer(toolkit, grid, managedForm);
        createButtons(toolkit, grid);
        createTreeContextMenu(tree);
        createSectionActions(section, toolkit);
    }

    private void createSectionActions(Section section, FormToolkit toolkit) {
        ToolBarManager manager = new ToolBarManager(SWT.FLAT);
        manager.removeAll();
        
        ToolBar toolbar = manager.createControl(section);        
        section.setTextClient(toolbar);
        
        ElementDescriptor[] descs = mDescriptorFilters;
        if (descs == null && mUiRootNode != null) {
            descs = mUiRootNode.getDescriptor().getChildren();
        }
        
        if (descs != null && descs.length > 1) {
            for (ElementDescriptor desc : descs) {
                manager.add(new DescriptorFilterAction(desc));
            }
        }
        
        manager.add(new TreeSortAction());

        manager.update(true /*force*/);
    }

    /**
     * Creates the tree and its viewer
     * @return The tree control
     */
    private Tree createTreeViewer(FormToolkit toolkit, Composite grid,
            final IManagedForm managedForm) {
        // Note: we *could* use a FilteredTree instead of the Tree+TreeViewer here.
        // However the class must be adapted to create an adapted toolkit tree.
        final Tree tree = toolkit.createTree(grid, SWT.MULTI);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = AndroidEditor.TEXT_WIDTH_HINT;
        gd.heightHint = TREE_HEIGHT_HINT;
        tree.setLayoutData(gd);

        mTreeViewer = new TreeViewer(tree);
        mTreeViewer.setContentProvider(new UiModelTreeContentProvider(mUiRootNode, mDescriptorFilters));
        mTreeViewer.setLabelProvider(new UiModelTreeLabelProvider());
        mTreeViewer.setInput("unused"); //$NON-NLS-1$

        // Create a listener that reacts to selections on the tree viewer.
        // When a selection is made, ask the managed form to propagate an event to
        // all parts in the managed form.
        // This is picked up by UiElementDetail.selectionChanged().
        mTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                managedForm.fireSelectionChanged(mMasterPart, event.getSelection());
                adjustTreeButtons(event.getSelection());
            }
        });
        
        // Create three listeners:
        // - One to refresh the tree viewer when the parent's node has been updated
        // - One to refresh the tree viewer when the framework resources have changed
        // - One to enable/disable the UI based on the application node's presence.
        mUiRefreshListener = new IUiUpdateListener() {
            public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
                mTreeViewer.refresh();
            }
        };
        
        mUiEnableListener = new IUiUpdateListener() {
            public void uiElementNodeUpdated(UiElementNode ui_node, UiUpdateState state) {
                // The UiElementNode for the application XML node always exists, even
                // if there is no corresponding XML node in the XML file.
                //
                // Normally, we enable the UI here if the XML node is not null.
                //
                // However if mAutoCreateRoot is true, the root node will be created on-demand
                // so the tree/block is always enabled.
                boolean exists = mAutoCreateRoot || (ui_node.getXmlNode() != null);
                if (mMasterPart != null) {
                    Section section = mMasterPart.getSection();
                    if (section.getEnabled() != exists) {
                        section.setEnabled(exists);
                        for (Control c : section.getChildren()) {
                            c.setEnabled(exists);
                        }
                    }
                }
            }
        };

        /** Listener to update the root node if the target of the file is changed because of a
         * SDK location change or a project target change */
        final ITargetChangeListener targetListener = new ITargetChangeListener() {
            public void onProjectTargetChange(IProject changedProject) {
                if (changedProject == mEditor.getProject()) {
                    onTargetsLoaded();
                }
            }

            public void onTargetsLoaded() {
                // If a details part has been created, we need to "refresh" it too.
                if (mDetailsPart != null) {
                    // The details part does not directly expose access to its internal
                    // page book. Instead it is possible to resize the page book to 0 and then
                    // back to its original value, which has the side effect of removing all
                    // existing cached pages.
                    int limit = mDetailsPart.getPageLimit();
                    mDetailsPart.setPageLimit(0);
                    mDetailsPart.setPageLimit(limit);
                }
                // Refresh the tree, preserving the selection if possible.
                mTreeViewer.refresh();
            }
        };

        // Setup the listeners
        changeRootAndDescriptors(mUiRootNode, mDescriptorFilters, false /* refresh */);

        // Listen on resource framework changes to refresh the tree
        AdtPlugin.getDefault().addTargetListener(targetListener);

        // Remove listeners when the tree widget gets disposed.
        tree.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                UiElementNode node = mUiRootNode.getUiParent() != null ?
                                        mUiRootNode.getUiParent() :
                                        mUiRootNode;

                node.removeUpdateListener(mUiRefreshListener);
                mUiRootNode.removeUpdateListener(mUiEnableListener);

                AdtPlugin.getDefault().removeTargetListener(targetListener);
                if (mClipboard != null) {
                    mClipboard.dispose();
                    mClipboard = null;
                }
            }
        });
        
        // Get a new clipboard reference. It is disposed when the tree is disposed.
        mClipboard = new Clipboard(tree.getDisplay());

        return tree;
    }

    /**
     * Changes the UI root node and the descriptor filters of the tree.
     * <p/>
     * This removes the listeners attached to the old root node and reattaches them to the
     * new one.
     * 
     * @param uiRootNode The root {@link UiElementNode} which contains all the elements that are
     *        to be manipulated by this tree view. In general this is the manifest UI node or the
     *        application UI node. This cannot be null.
     * @param descriptorFilters A list of descriptors of the elements to be displayed as root in
     *        this tree view. Use null or an empty list to accept any kind of node.
     * @param forceRefresh If tree, forces the tree to refresh
     */
    public void changeRootAndDescriptors(UiElementNode uiRootNode,
            ElementDescriptor[] descriptorFilters, boolean forceRefresh) {
        UiElementNode node;

        // Remove previous listeners if any
        if (mUiRootNode != null) {
            node = mUiRootNode.getUiParent() != null ? mUiRootNode.getUiParent() : mUiRootNode;
            node.removeUpdateListener(mUiRefreshListener);
            mUiRootNode.removeUpdateListener(mUiEnableListener);
        }
        
        mUiRootNode = uiRootNode;
        mDescriptorFilters = descriptorFilters;

        mTreeViewer.setContentProvider(new UiModelTreeContentProvider(mUiRootNode, mDescriptorFilters));

        // Listen on structural changes on the root node of the tree
        // If the node has a parent, listen on the parent instead.
        node = mUiRootNode.getUiParent() != null ? mUiRootNode.getUiParent() : mUiRootNode;
        node.addUpdateListener(mUiRefreshListener);
        
        // Use the root node to listen to its presence.
        mUiRootNode.addUpdateListener(mUiEnableListener);

        // Initialize the enabled/disabled state
        mUiEnableListener.uiElementNodeUpdated(mUiRootNode, null /* state, not used */);
        
        if (forceRefresh) {
            mTreeViewer.refresh();
        }

        createSectionActions(mMasterPart.getSection(), mManagedForm.getToolkit());
    }

    /**
     * Creates the buttons next to the tree.
     */
    private void createButtons(FormToolkit toolkit, Composite grid) {
        
        mUiTreeActions = new UiTreeActions();
        
        Composite button_grid = SectionHelper.createGridLayout(grid, toolkit, 1);
        button_grid.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        mAddButton = toolkit.createButton(button_grid, "Add...", SWT.PUSH);
        SectionHelper.addControlTooltip(mAddButton, "Adds a new element.");
        mAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL |
                GridData.VERTICAL_ALIGN_BEGINNING));

        mAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                doTreeAdd();
            }
        });
        
        mRemoveButton = toolkit.createButton(button_grid, "Remove...", SWT.PUSH);
        SectionHelper.addControlTooltip(mRemoveButton, "Removes an existing selected element.");
        mRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        mRemoveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                doTreeRemove();
            }
        });
        
        mUpButton = toolkit.createButton(button_grid, "Up", SWT.PUSH);
        SectionHelper.addControlTooltip(mRemoveButton, "Moves the selected element up.");
        mUpButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        mUpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                doTreeUp();
            }
        });

        mDownButton = toolkit.createButton(button_grid, "Down", SWT.PUSH);
        SectionHelper.addControlTooltip(mRemoveButton, "Moves the selected element down.");
        mDownButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        mDownButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                doTreeDown();
            }
        });

        adjustTreeButtons(TreeSelection.EMPTY);
    }

    private void createTreeContextMenu(Tree tree) {
        MenuManager menuManager = new MenuManager();
        menuManager.setRemoveAllWhenShown(true);
        menuManager.addMenuListener(new IMenuListener() {
            /**
             * The menu is about to be shown. The menu manager has already been
             * requested to remove any existing menu item. This method gets the
             * tree selection and if it is of the appropriate type it re-creates
             * the necessary actions.
             */
           public void menuAboutToShow(IMenuManager manager) {
               ISelection selection = mTreeViewer.getSelection();
               if (!selection.isEmpty() && selection instanceof ITreeSelection) {
                   ArrayList<UiElementNode> selected = filterSelection((ITreeSelection) selection);
                   doCreateMenuAction(manager, selected);
                   return;
               }
               doCreateMenuAction(manager, null /* ui_node */);
            } 
        });
        Menu contextMenu = menuManager.createContextMenu(tree);
        tree.setMenu(contextMenu);
    }

    /**
     * Adds the menu actions to the context menu when the given UI node is selected in
     * the tree view.
     * 
     * @param manager The context menu manager
     * @param selected The UI nodes selected in the tree. Can be null, in which case the root
     *                is to be modified.
     */
    private void doCreateMenuAction(IMenuManager manager, ArrayList<UiElementNode> selected) {
        if (selected != null) {
            boolean hasXml = false;
            for (UiElementNode uiNode : selected) {
                if (uiNode.getXmlNode() != null) {
                    hasXml = true;
                    break;
                }
            }

            if (hasXml) {
                manager.add(new CopyCutAction(getEditor(), getClipboard(),
                        null, selected, true /* cut */));
                manager.add(new CopyCutAction(getEditor(), getClipboard(),
                        null, selected, false /* cut */));

                // Can't paste with more than one element selected (the selection is the target)
                if (selected.size() <= 1) {
                    // Paste is not valid if it would add a second element on a terminal element
                    // which parent is a document -- an XML document can only have one child. This
                    // means paste is valid if the current UI node can have children or if the
                    // parent is not a document.
                    UiElementNode ui_root = selected.get(0).getUiRoot();
                    if (ui_root.getDescriptor().hasChildren() ||
                            !(ui_root.getUiParent() instanceof UiDocumentNode)) {
                        manager.add(new PasteAction(getEditor(), getClipboard(), selected.get(0)));
                    }
                }
                manager.add(new Separator());
            }
        }

        // Append "add" and "remove" actions. They do the same thing as the add/remove
        // buttons on the side.
        Action action;
        IconFactory factory = IconFactory.getInstance();

        // "Add" makes sense only if there's 0 or 1 item selected since the
        // one selected item becomes the target.
        if (selected == null || selected.size() <= 1) {
            manager.add(new Action("Add...", factory.getImageDescriptor("add")) { //$NON-NLS-1$
                @Override
                public void run() {
                    super.run();
                    doTreeAdd();
                }
            });
        }

        if (selected != null) {
            if (selected != null) {
                manager.add(new Action("Remove", factory.getImageDescriptor("delete")) { //$NON-NLS-1$
                    @Override
                    public void run() {
                        super.run();
                        doTreeRemove();
                    }
                });
            }
            manager.add(new Separator());
            
            manager.add(new Action("Up", factory.getImageDescriptor("up")) { //$NON-NLS-1$
                @Override
                public void run() {
                    super.run();
                    doTreeUp();
                }
            });
            manager.add(new Action("Down", factory.getImageDescriptor("down")) { //$NON-NLS-1$
                @Override
                public void run() {
                    super.run();
                    doTreeDown();
                }
            });
        }
    }

    
    /**
     * This is called by the tree when a selection is made.
     * It enables/disables the buttons associated with the tree depending on the current
     * selection.
     *
     * @param selection The current tree selection (same as mTreeViewer.getSelection())
     */
    private void adjustTreeButtons(ISelection selection) {
        mRemoveButton.setEnabled(!selection.isEmpty() && selection instanceof ITreeSelection);
        mUpButton.setEnabled(!selection.isEmpty() && selection instanceof ITreeSelection);
        mDownButton.setEnabled(!selection.isEmpty() && selection instanceof ITreeSelection);
    }

    /**
     * An adapter/wrapper to use the add/remove/up/down tree edit actions.
     */
    private class UiTreeActions extends UiActions {
        @Override
        protected UiElementNode getRootNode() {
            return mUiRootNode;
        }

        @Override
        protected void selectUiNode(UiElementNode uiNodeToSelect) {
            // Select the new item
            if (uiNodeToSelect != null) {
                LinkedList<UiElementNode> segments = new LinkedList<UiElementNode>();
                for (UiElementNode ui_node = uiNodeToSelect; ui_node != mUiRootNode;
                        ui_node = ui_node.getUiParent()) {
                    segments.add(0, ui_node);
                }
                if (segments.size() > 0) {
                    mTreeViewer.setSelection(new TreeSelection(new TreePath(segments.toArray())));
                } else {
                    mTreeViewer.setSelection(null);
                }
            }
        }

        @Override
        public void commitPendingXmlChanges() {
            commitManagedForm();
        }
    }

    /**
     * Filters an ITreeSelection to only keep the {@link UiElementNode}s (in case there's
     * something else in there).
     * 
     * @return A new list of {@link UiElementNode} with at least one item or null.
     */
    @SuppressWarnings("unchecked")
    private ArrayList<UiElementNode> filterSelection(ITreeSelection selection) {
        ArrayList<UiElementNode> selected = new ArrayList<UiElementNode>();
        
        for (Iterator it = selection.iterator(); it.hasNext(); ) {
            Object selectedObj = it.next();
        
            if (selectedObj instanceof UiElementNode) {
                selected.add((UiElementNode) selectedObj);
            }
        }

        return selected.size() > 0 ? selected : null;
    }

    /**
     * Called when the "Add..." button next to the tree view is selected.
     * 
     * Displays a selection dialog that lets the user select which kind of node
     * to create, depending on the current selection.
     */
    private void doTreeAdd() {
        UiElementNode ui_node = mUiRootNode;
        ISelection selection = mTreeViewer.getSelection();
        if (!selection.isEmpty() && selection instanceof ITreeSelection) {
            ITreeSelection tree_selection = (ITreeSelection) selection;
            Object first = tree_selection.getFirstElement();
            if (first != null && first instanceof UiElementNode) {
                ui_node = (UiElementNode) first;
            }
        }

        mUiTreeActions.doAdd(
                ui_node,
                mDescriptorFilters,
                mTreeViewer.getControl().getShell(),
                (ILabelProvider) mTreeViewer.getLabelProvider());
    }

    /**
     * Called when the "Remove" button is selected.
     * 
     * If the tree has a selection, remove it.
     * This simply deletes the XML node attached to the UI node: when the XML model fires the
     * update event, the tree will get refreshed.
     */
    protected void doTreeRemove() {
        ISelection selection = mTreeViewer.getSelection();
        if (!selection.isEmpty() && selection instanceof ITreeSelection) {
            ArrayList<UiElementNode> selected = filterSelection((ITreeSelection) selection);
            mUiTreeActions.doRemove(selected, mTreeViewer.getControl().getShell());
        }
    }

    /**
     * Called when the "Up" button is selected.
     * <p/>
     * If the tree has a selection, move it up, either in the child list or as the last child
     * of the previous parent.
     */
    protected void doTreeUp() {
        ISelection selection = mTreeViewer.getSelection();
        if (!selection.isEmpty() && selection instanceof ITreeSelection) {
            ArrayList<UiElementNode> selected = filterSelection((ITreeSelection) selection);
            mUiTreeActions.doUp(selected);
        }
    }
    
    /**
     * Called when the "Down" button is selected.
     * 
     * If the tree has a selection, move it down, either in the same child list or as the
     * first child of the next parent.
     */
    protected void doTreeDown() {
        ISelection selection = mTreeViewer.getSelection();
        if (!selection.isEmpty() && selection instanceof ITreeSelection) {
            ArrayList<UiElementNode> selected = filterSelection((ITreeSelection) selection);
            mUiTreeActions.doDown(selected);
        }
    }

    /**
     * Commits the current managed form (the one associated with our master part).
     * As a side effect, this will commit the current UiElementDetails page.
     */
    void commitManagedForm() {
        if (mManagedForm != null) {
            mManagedForm.commit(false /* onSave */);
        }
    }

    /* Implements ICommitXml for CopyCutAction */
    public void commitPendingXmlChanges() {
        commitManagedForm();
    }

    @Override
    protected void createToolBarActions(IManagedForm managedForm) {
        // Pass. Not used, toolbar actions are defined by createSectionActions().
    }

    @Override
    protected void registerPages(DetailsPart detailsPart) {
        // Keep a reference on the details part (the super class doesn't provide a getter
        // for it.)
        mDetailsPart = detailsPart;
        
        // The page selection mechanism does not use pages registered by association with
        // a node class. Instead it uses a custom details page provider that provides a
        // new UiElementDetail instance for each node instance. A limit of 5 pages is
        // then set (the value is arbitrary but should be reasonable) for the internal
        // page book.
        detailsPart.setPageLimit(5);
        
        final UiTreeBlock tree = this;
        
        detailsPart.setPageProvider(new IDetailsPageProvider() {
            public IDetailsPage getPage(Object key) {
                if (key instanceof UiElementNode) {
                    return new UiElementDetail(tree);
                }
                return null;
            }

            public Object getPageKey(Object object) {
                return object;  // use node object as key
            }
        });
    }

    /**
     * An alphabetic sort action for the tree viewer.
     */
    private class TreeSortAction extends Action {
        
        private ViewerComparator mComparator;

        public TreeSortAction() {
            super("Sorts elements alphabetically.", AS_CHECK_BOX);
            setImageDescriptor(IconFactory.getInstance().getImageDescriptor("az_sort")); //$NON-NLS-1$
 
            if (mTreeViewer != null) {
                boolean is_sorted = mTreeViewer.getComparator() != null;
                setChecked(is_sorted);
            }
        }

        /**
         * Called when the button is selected. Toggles the tree viewer comparator.
         */
        @Override
        public void run() {
            if (mTreeViewer == null) {
                notifyResult(false /*success*/);
                return;
            }

            ViewerComparator comp = mTreeViewer.getComparator();
            if (comp != null) {
                // Tree is currently sorted.
                // Save currently comparator and remove it
                mComparator = comp;
                mTreeViewer.setComparator(null);
            } else {
                // Tree is not currently sorted.
                // Reuse or add a new comparator.
                if (mComparator == null) {
                    mComparator = new ViewerComparator();
                }
                mTreeViewer.setComparator(mComparator);
            }
            
            notifyResult(true /*success*/);
        }
    }

    /**
     * A filter on descriptor for the tree viewer.
     * <p/>
     * The tree viewer will contain many of these actions and only one can be enabled at a
     * given time. When no action is selected, everything is displayed.
     * <p/>
     * Since "radio"-like actions do not allow for unselecting all of them, we manually
     * handle the exclusive radio button-like property: when an action is selected, it manually
     * removes all other actions as needed.
     */
    private class DescriptorFilterAction extends Action {

        private final ElementDescriptor mDescriptor;
        private ViewerFilter mFilter;
        
        public DescriptorFilterAction(ElementDescriptor descriptor) {
            super(String.format("Displays only %1$s elements.", descriptor.getUiName()),
                    AS_CHECK_BOX);
            
            mDescriptor = descriptor;
            setImageDescriptor(descriptor.getImageDescriptor());
        }

        /**
         * Called when the button is selected.
         * <p/>
         * Find any existing {@link DescriptorFilter}s and remove them. Install ours.
         */
        @Override
        public void run() {
            super.run();
            
            if (isChecked()) {
                if (mFilter == null) {
                    // create filter when required
                    mFilter = new DescriptorFilter(this);
                }

                // we add our filter first, otherwise the UI might show the full list
                mTreeViewer.addFilter(mFilter);

                // Then remove the any other filters except ours. There should be at most
                // one other filter, since that's how the actions are made to look like
                // exclusive radio buttons.
                for (ViewerFilter filter : mTreeViewer.getFilters()) {
                    if (filter instanceof DescriptorFilter && filter != mFilter) {
                        DescriptorFilterAction action = ((DescriptorFilter) filter).getAction();
                        action.setChecked(false);
                        mTreeViewer.removeFilter(filter);
                    }
                }
            } else if (mFilter != null){
                mTreeViewer.removeFilter(mFilter);
            }
        }

        /**
         * Filters the tree viewer for the given descriptor.
         * <p/>
         * The filter is linked to the action so that an action can iterate through the list
         * of filters and un-select the actions.
         */
        private class DescriptorFilter extends ViewerFilter {

            private final DescriptorFilterAction mAction;

            public DescriptorFilter(DescriptorFilterAction action) {
                mAction = action;
            }
            
            public DescriptorFilterAction getAction() {
                return mAction;
            }

            /**
             * Returns true if an element should be displayed, that if the element or
             * any of its parent matches the requested descriptor.
             */
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                while (element instanceof UiElementNode) {
                    UiElementNode uiNode = (UiElementNode)element;
                    if (uiNode.getDescriptor() == mDescriptor) {
                        return true;
                    }
                    element = uiNode.getUiParent();
                }
                return false;
            }
        }
    }
    
}
