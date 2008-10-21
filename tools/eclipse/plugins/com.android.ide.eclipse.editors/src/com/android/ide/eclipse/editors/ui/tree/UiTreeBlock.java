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

import com.android.ide.eclipse.editors.AndroidEditor;
import com.android.ide.eclipse.editors.EditorsPlugin;
import com.android.ide.eclipse.editors.IconFactory;
import com.android.ide.eclipse.editors.descriptors.ElementDescriptor;
import com.android.ide.eclipse.editors.ui.SectionHelper;
import com.android.ide.eclipse.editors.ui.SectionHelper.ManifestSectionPart;
import com.android.ide.eclipse.editors.uimodel.IUiUpdateListener;
import com.android.ide.eclipse.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.editors.uimodel.UiElementNode;

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

    private UiTreeActions mUiTreeActions;

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
        ToolBar toolbar = manager.createControl(section);        
        section.setTextClient(toolbar);
        
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
        final Tree tree = toolkit.createTree(grid, SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = AndroidEditor.TEXT_WIDTH_HINT;
        gd.heightHint = TREE_HEIGHT_HINT;
        tree.setLayoutData(gd);

        mTreeViewer = new TreeViewer(tree);
        mTreeViewer.setContentProvider(new UiModelTreeContentProvider(
                mUiRootNode, mDescriptorFilters));
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

        final Runnable resourceRefreshListener = new Runnable() {
            public void run() {
                // If a details part has been created, we need to "refresh" it too.
                if (mDetailsPart != null) {
                    // The details part does not directly expose access to its internal
                    // page book. Instead it is possible to resize the page book to 0 and then
                    // back to its original value, which as the side effect of removing all
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
        EditorsPlugin.getDefault().addResourceChangedListener(resourceRefreshListener);

        // Remove listeners when the tree widget gets disposed.
        tree.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                UiElementNode node = mUiRootNode.getUiParent() != null ?
                                        mUiRootNode.getUiParent() :
                                        mUiRootNode;

                node.removeUpdateListener(mUiRefreshListener);
                mUiRootNode.removeUpdateListener(mUiEnableListener);

                EditorsPlugin.getDefault().removeResourceChangedListener(resourceRefreshListener);
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
                   ITreeSelection tree_selection = (ITreeSelection) selection;
                   Object first = tree_selection.getFirstElement();
                   if (first != null && first instanceof UiElementNode) {
                       UiElementNode ui_node = (UiElementNode) first;
                       doCreateMenuAction(manager, ui_node);
                       return;
                   }
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
     * @param ui_node The UI node selected in the tree. Can be null, in which case the root
     *                is to be modified.
     */
    private void doCreateMenuAction(IMenuManager manager, UiElementNode ui_node) {
        Action action;
        
        if (ui_node != null && ui_node.getXmlNode() != null) {
            manager.add(new CopyCutAction(getEditor(), getClipboard(),
                    this, ui_node, true /* cut */));
            manager.add(new CopyCutAction(getEditor(), getClipboard(),
                    this, ui_node, false /* cut */));
            // Paste is not valid if it would add a second element on a terminal element
            // which parent is a document -- an XML document can only have one child. This
            // means paste is valid if the current UI node can have children or if the parent
            // is not a document.
            if (mUiRootNode.getDescriptor().hasChildren() ||
                    !(mUiRootNode.getUiParent() instanceof UiDocumentNode)) {
                manager.add(new PasteAction(getEditor(), getClipboard(), ui_node));
            }
            manager.add(new Separator());
        }

        // Append "add" and "remove" actions. They do the same thing as the add/remove
        // buttons on the side.
        
        manager.add(new Action("Add...", EditorsPlugin.getAndroidLogoDesc()) {
            @Override
            public void run() {
                super.run();
                doTreeAdd();
            }
        });
        
        if (ui_node != null) {
            manager.add(new Action("Remove", EditorsPlugin.getAndroidLogoDesc()) {
                @Override
                public void run() {
                    super.run();
                    doTreeRemove();
                }
            });
        }
        
        manager.add(new Separator());

        if (ui_node != null) {
            manager.add(new Action("Up", EditorsPlugin.getAndroidLogoDesc()) {
                @Override
                public void run() {
                    super.run();
                    doTreeUp();
                }
            });
        }
        
        if (ui_node != null) {
            manager.add(new Action("Down", EditorsPlugin.getAndroidLogoDesc()) {
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
                mTreeViewer.setSelection(new TreeSelection(new TreePath(segments.toArray())));
            }
        }

        @Override
        public void commitPendingXmlChanges() {
            commitManagedForm();
        }
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
            ITreeSelection tree_selection = (ITreeSelection) selection;
            Object first = tree_selection.getFirstElement();
            if (first instanceof UiElementNode) {
                final UiElementNode ui_node = (UiElementNode) first;

                mUiTreeActions.doRemove(ui_node, mTreeViewer.getControl().getShell());
            }
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
            ITreeSelection tree_selection = (ITreeSelection) selection;
            Object first = tree_selection.getFirstElement();
            if (first instanceof UiElementNode) {
                final UiElementNode ui_node = (UiElementNode) first;

                mUiTreeActions.doUp(ui_node);
            }
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
            ITreeSelection tree_selection = (ITreeSelection) selection;
            Object first = tree_selection.getFirstElement();
            if (first instanceof UiElementNode) {
                final UiElementNode ui_node = (UiElementNode) first;

                mUiTreeActions.doDown(ui_node);
            }
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
        // pass
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
     * Adds a sort action to the tree viewer.
     */
    private class TreeSortAction extends Action {
        
        private ViewerComparator mComparator;

        public TreeSortAction() {
            setToolTipText("Sorts elements alphabetically.");
            setImageDescriptor(IconFactory.getInstance().getImageDescriptor("az_sort")); //$NON-NLS-1$
 
            if (mTreeViewer != null) {
                boolean is_sorted = mTreeViewer.getComparator() != null;
                setChecked(is_sorted);
            }
        }

        /**
         * Called when the button is selected. Toggle the tree viewer comparator.
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
}
