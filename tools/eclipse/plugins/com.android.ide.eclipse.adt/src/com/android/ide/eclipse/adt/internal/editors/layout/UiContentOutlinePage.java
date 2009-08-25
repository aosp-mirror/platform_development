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


package com.android.ide.eclipse.adt.internal.editors.layout;

import com.android.ide.eclipse.adt.internal.editors.IconFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiDocumentTreeEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementTreeEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiElementTreeEditPartFactory;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiLayoutTreeEditPart;
import com.android.ide.eclipse.adt.internal.editors.layout.parts.UiViewTreeEditPart;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.CopyCutAction;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.PasteAction;
import com.android.ide.eclipse.adt.internal.editors.ui.tree.UiActions;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiDocumentNode;
import com.android.ide.eclipse.adt.internal.editors.uimodel.UiElementNode;
import com.android.ide.eclipse.adt.internal.ui.EclipseUiHelper;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of the {@link ContentOutlinePage} to display {@link UiElementNode}.
 *
 * @since GLE1
 */
class UiContentOutlinePage extends ContentOutlinePage {

    private GraphicalLayoutEditor mEditor;

    private Action mAddAction;
    private Action mDeleteAction;
    private Action mUpAction;
    private Action mDownAction;

    private UiOutlineActions mUiActions = new UiOutlineActions();

    public UiContentOutlinePage(GraphicalLayoutEditor editor, final EditPartViewer viewer) {
        super(viewer);
        mEditor = editor;
        IconFactory factory = IconFactory.getInstance();

        mAddAction = new Action("Add...") {
            @Override
            public void run() {
                List<UiElementNode> nodes = getModelSelections();
                UiElementNode node = nodes != null && nodes.size() > 0 ? nodes.get(0) : null;

                mUiActions.doAdd(node, viewer.getControl().getShell());
            }
        };
        mAddAction.setToolTipText("Adds a new element.");
        mAddAction.setImageDescriptor(factory.getImageDescriptor("add")); //$NON-NLS-1$

        mDeleteAction = new Action("Remove...") {
            @Override
            public void run() {
                List<UiElementNode> nodes = getModelSelections();

                mUiActions.doRemove(nodes, viewer.getControl().getShell());
            }
        };
        mDeleteAction.setToolTipText("Removes an existing selected element.");
        mDeleteAction.setImageDescriptor(factory.getImageDescriptor("delete")); //$NON-NLS-1$

        mUpAction = new Action("Up") {
            @Override
            public void run() {
                List<UiElementNode> nodes = getModelSelections();

                mUiActions.doUp(nodes);
            }
        };
        mUpAction.setToolTipText("Moves the selected element up");
        mUpAction.setImageDescriptor(factory.getImageDescriptor("up")); //$NON-NLS-1$

        mDownAction = new Action("Down") {
            @Override
            public void run() {
                List<UiElementNode> nodes = getModelSelections();

                mUiActions.doDown(nodes);
            }
        };
        mDownAction.setToolTipText("Moves the selected element down");
        mDownAction.setImageDescriptor(factory.getImageDescriptor("down")); //$NON-NLS-1$

        // all actions disabled by default.
        mAddAction.setEnabled(false);
        mDeleteAction.setEnabled(false);
        mUpAction.setEnabled(false);
        mDownAction.setEnabled(false);

        addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ISelection selection = event.getSelection();

                // the selection is never empty. The least it'll contain is the
                // UiDocumentTreeEditPart object.
                if (selection instanceof StructuredSelection) {
                    StructuredSelection structSel = (StructuredSelection)selection;

                    if (structSel.size() == 1 &&
                            structSel.getFirstElement() instanceof UiDocumentTreeEditPart) {
                        mDeleteAction.setEnabled(false);
                        mUpAction.setEnabled(false);
                        mDownAction.setEnabled(false);
                    } else {
                        mDeleteAction.setEnabled(true);
                        mUpAction.setEnabled(true);
                        mDownAction.setEnabled(true);
                    }

                    // the "add" button is always enabled, in order to be able to set the
                    // initial root node
                    mAddAction.setEnabled(true);
                }
            }
        });
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(Composite parent) {
        // create outline viewer page
        getViewer().createControl(parent);

        // configure outline viewer
        getViewer().setEditPartFactory(new UiElementTreeEditPartFactory());

        setupOutline();
        setupContextMenu();
        setupTooltip();
        setupDoubleClick();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.part.Page#setActionBars(org.eclipse.ui.IActionBars)
     *
     * Called automatically after createControl
     */
    @Override
    public void setActionBars(IActionBars actionBars) {
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(mAddAction);
        toolBarManager.add(mDeleteAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mUpAction);
        toolBarManager.add(mDownAction);

        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(mAddAction);
        menuManager.add(mDeleteAction);
        menuManager.add(new Separator());
        menuManager.add(mUpAction);
        menuManager.add(mDownAction);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#dispose()
     */
    @Override
    public void dispose() {
        breakConnectionWithEditor();

        // dispose
        super.dispose();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#getControl()
     */
    @Override
    public Control getControl() {
        return getViewer().getControl();
    }

    void setNewEditor(GraphicalLayoutEditor editor) {
        mEditor = editor;
        setupOutline();
    }

    void breakConnectionWithEditor() {
        // unhook outline viewer
        mEditor.getSelectionSynchronizer().removeViewer(getViewer());
    }

    private void setupOutline() {

        getViewer().setEditDomain(mEditor.getEditDomain());

        // hook outline viewer
        mEditor.getSelectionSynchronizer().addViewer(getViewer());

        // initialize outline viewer with model
        getViewer().setContents(mEditor.getModel());
    }

    private void setupContextMenu() {
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
               List<UiElementNode> selected = getModelSelections();

               if (selected != null) {
                   doCreateMenuAction(manager, selected);
                   return;
               }
               doCreateMenuAction(manager, null /* ui_node */);
            }
        });
        Control control = getControl();
        Menu contextMenu = menuManager.createContextMenu(control);
        control.setMenu(contextMenu);
    }

    /**
     * Adds the menu actions to the context menu when the given UI node is selected in
     * the tree view.
     *
     * @param manager The context menu manager
     * @param selected The UI node selected in the tree. Can be null, in which case the root
     *                is to be modified.
     */
    private void doCreateMenuAction(IMenuManager manager, List<UiElementNode> selected) {

        if (selected != null) {
            boolean hasXml = false;
            for (UiElementNode uiNode : selected) {
                if (uiNode.getXmlNode() != null) {
                    hasXml = true;
                    break;
                }
            }

            if (hasXml) {
                manager.add(new CopyCutAction(mEditor.getLayoutEditor(), mEditor.getClipboard(),
                        null, selected, true /* cut */));
                manager.add(new CopyCutAction(mEditor.getLayoutEditor(), mEditor.getClipboard(),
                        null, selected, false /* cut */));

                // Can't paste with more than one element selected (the selection is the target)
                if (selected.size() <= 1) {
                    // Paste is not valid if it would add a second element on a terminal element
                    // which parent is a document -- an XML document can only have one child. This
                    // means paste is valid if the current UI node can have children or if the parent
                    // is not a document.
                    UiElementNode ui_root = selected.get(0).getUiRoot();
                    if (ui_root.getDescriptor().hasChildren() ||
                            !(ui_root.getUiParent() instanceof UiDocumentNode)) {
                        manager.add(new PasteAction(mEditor.getLayoutEditor(),
                                mEditor.getClipboard(),
                                selected.get(0)));
                    }
                }
                manager.add(new Separator());
            }
        }

        // Append "add" and "remove" actions. They do the same thing as the add/remove
        // buttons on the side.
        //
        // "Add" makes sense only if there's 0 or 1 item selected since the
        // one selected item becomes the target.
        if (selected == null || selected.size() <= 1) {
            manager.add(mAddAction);
        }

        if (selected != null) {
            manager.add(mDeleteAction);
            manager.add(new Separator());

            manager.add(mUpAction);
            manager.add(mDownAction);
        }

        if (selected != null && selected.size() == 1) {
            manager.add(new Separator());

            Action propertiesAction = new Action("Properties") {
                @Override
                public void run() {
                    EclipseUiHelper.showView(EclipseUiHelper.PROPERTY_SHEET_VIEW_ID,
                            true /* activate */);
                }
            };
            propertiesAction.setToolTipText("Displays properties of the selected element.");
            manager.add(propertiesAction);
        }
    }

    /**
     * Updates the outline view with the model of the {@link IGraphicalLayoutEditor}.
     * <p/>
     * This attemps to preserve the selection, if any.
     */
    public void reloadModel() {
        // Attemps to preserve the UiNode selection, if any
        List<UiElementNode> uiNodes = null;
        try {
            // get current selection using the model rather than the edit part as
            // reloading the content may change the actual edit part.
            uiNodes = getModelSelections();

            // perform the update
            getViewer().setContents(mEditor.getModel());

        } finally {
            // restore selection
            if (uiNodes != null) {
                setModelSelection(uiNodes.get(0));
            }
        }
    }

    /**
     * Returns the currently selected element, if any, in the viewer.
     * This returns the viewer's elements (i.e. an {@link UiElementTreeEditPart})
     * and not the underlying model node.
     * <p/>
     * When there is no actual selection, this might still return the root node,
     * which is of type {@link UiDocumentTreeEditPart}.
     */
    @SuppressWarnings("unchecked")
    private List<UiElementTreeEditPart> getViewerSelections() {
        ISelection selection = getSelection();
        if (selection instanceof StructuredSelection) {
            StructuredSelection structuredSelection = (StructuredSelection)selection;

            if (structuredSelection.size() > 0) {
                ArrayList<UiElementTreeEditPart> selected = new ArrayList<UiElementTreeEditPart>();

                for (Iterator it = structuredSelection.iterator(); it.hasNext(); ) {
                    Object selectedObj = it.next();

                    if (selectedObj instanceof UiElementTreeEditPart) {
                        selected.add((UiElementTreeEditPart) selectedObj);
                    }
                }

                return selected.size() > 0 ? selected : null;
            }
        }

        return null;
    }

    /**
     * Returns the currently selected model element, which is either an
     * {@link UiViewTreeEditPart} or an {@link UiLayoutTreeEditPart}.
     * <p/>
     * Returns null if there is no selection or if the implicit root is "selected"
     * (which actually represents the lack of a real element selection.)
     */
    private List<UiElementNode> getModelSelections() {

        List<UiElementTreeEditPart> parts = getViewerSelections();

        if (parts != null) {
            ArrayList<UiElementNode> selected = new ArrayList<UiElementNode>();

            for (UiElementTreeEditPart part : parts) {
                if (part instanceof UiViewTreeEditPart || part instanceof UiLayoutTreeEditPart) {
                    selected.add((UiElementNode) part.getModel());
                }
            }

            return selected.size() > 0 ? selected : null;
        }

        return null;
    }

    /**
     * Selects the corresponding edit part in the tree viewer.
     */
    private void setViewerSelection(UiElementTreeEditPart selectedPart) {
        if (selectedPart != null && !(selectedPart instanceof UiDocumentTreeEditPart)) {
            LinkedList<UiElementTreeEditPart> segments = new LinkedList<UiElementTreeEditPart>();
            for (UiElementTreeEditPart part = selectedPart;
                    !(part instanceof UiDocumentTreeEditPart);
                    part = (UiElementTreeEditPart) part.getParent()) {
                segments.add(0, part);
            }
            setSelection(new TreeSelection(new TreePath(segments.toArray())));
        }
    }

    /**
     * Selects the corresponding model element in the tree viewer.
     */
    private void setModelSelection(UiElementNode uiNodeToSelect) {
        if (uiNodeToSelect != null) {

            // find an edit part that has the requested model element
            UiElementTreeEditPart part = findPartForModel(
                    (UiElementTreeEditPart) getViewer().getContents(),
                    uiNodeToSelect);

            // if we found a part, select it and reveal it
            if (part != null) {
                setViewerSelection(part);
                getViewer().reveal(part);
            }
        }
    }

    /**
     * Utility method that tries to find an edit part that matches a given model UI node.
     *
     * @param rootPart The root of the viewer edit parts
     * @param uiNode The UI node model to find
     * @return The part that matches the model or null if it's not in the sub tree.
     */
    private UiElementTreeEditPart findPartForModel(UiElementTreeEditPart rootPart,
            UiElementNode uiNode) {
        if (rootPart.getModel() == uiNode) {
            return rootPart;
        }

        for (Object part : rootPart.getChildren()) {
            if (part instanceof UiElementTreeEditPart) {
                UiElementTreeEditPart found = findPartForModel(
                        (UiElementTreeEditPart) part, uiNode);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    /**
     * Sets up a custom tooltip when hovering over tree items.
     * <p/>
     * The tooltip will display the element's javadoc, if any, or the item's getText otherwise.
     */
    private void setupTooltip() {
        final Tree tree = (Tree) getControl();

        /*
         * Reference:
         * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet125.java?view=markup
         */

        final Listener listener = new Listener() {
            Shell tip = null;
            Label label  = null;

            public void handleEvent(Event event) {
                switch(event.type) {
                case SWT.Dispose:
                case SWT.KeyDown:
                case SWT.MouseExit:
                case SWT.MouseDown:
                case SWT.MouseMove:
                    if (tip != null) {
                        tip.dispose();
                        tip = null;
                        label = null;
                    }
                    break;
                case SWT.MouseHover:
                    if (tip != null) {
                        tip.dispose();
                        tip = null;
                        label = null;
                    }

                    String tooltip = null;

                    TreeItem item = tree.getItem(new Point(event.x, event.y));
                    if (item != null) {
                        Object data = item.getData();
                        if (data instanceof UiElementTreeEditPart) {
                            Object model = ((UiElementTreeEditPart) data).getModel();
                            if (model instanceof UiElementNode) {
                                tooltip = ((UiElementNode) model).getDescriptor().getTooltip();
                            }
                        }

                        if (tooltip == null) {
                            tooltip = item.getText();
                        } else {
                            tooltip = item.getText() + ":\r" + tooltip;
                        }
                    }


                    if (tooltip != null) {
                        Shell shell = tree.getShell();
                        Display display = tree.getDisplay();

                        tip = new Shell(shell, SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
                        tip.setBackground(display .getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                        FillLayout layout = new FillLayout();
                        layout.marginWidth = 2;
                        tip.setLayout(layout);
                        label = new Label(tip, SWT.NONE);
                        label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
                        label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                        label.setData("_TABLEITEM", item);
                        label.setText(tooltip);
                        label.addListener(SWT.MouseExit, this);
                        label.addListener(SWT.MouseDown, this);
                        Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
                        Rectangle rect = item.getBounds(0);
                        Point pt = tree.toDisplay(rect.x, rect.y);
                        tip.setBounds(pt.x, pt.y, size.x, size.y);
                        tip.setVisible(true);
                    }
                }
            }
        };

        tree.addListener(SWT.Dispose, listener);
        tree.addListener(SWT.KeyDown, listener);
        tree.addListener(SWT.MouseMove, listener);
        tree.addListener(SWT.MouseHover, listener);
    }

    /**
     * Sets up double-click action on the tree.
     * <p/>
     * By default, double-click (a.k.a. "default selection") on a valid list item will
     * show the property view.
     */
    private void setupDoubleClick() {
        final Tree tree = (Tree) getControl();

        tree.addListener(SWT.DefaultSelection, new Listener() {
            public void handleEvent(Event event) {
                EclipseUiHelper.showView(EclipseUiHelper.PROPERTY_SHEET_VIEW_ID,
                        true /* activate */);
            }
        });
    }

    // ---------------

    private class UiOutlineActions extends UiActions {

        @Override
        protected UiDocumentNode getRootNode() {
            return mEditor.getModel(); // this is LayoutEditor.getUiRootNode()
        }

        // Select the new item
        @Override
        protected void selectUiNode(UiElementNode uiNodeToSelect) {
            setModelSelection(uiNodeToSelect);
        }

        @Override
        public void commitPendingXmlChanges() {
            // Pass. There is nothing to commit before the XML is changed here.
        }

    }
}
