/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

public class ProfileView extends Composite implements Observer {
    
    private TreeViewer mTreeViewer;
    private Text mSearchBox;
    private SelectionController mSelectionController;
    private ProfileProvider mProfileProvider;
    private Color mColorNoMatch;
    private Color mColorMatch;
    private MethodData mCurrentHighlightedMethod;

    public ProfileView(Composite parent, TraceReader reader,
            SelectionController selectController) {
        super(parent, SWT.NONE);
        setLayout(new GridLayout(1, false));
        this.mSelectionController = selectController;
        mSelectionController.addObserver(this);

        // Add a tree viewer at the top
        mTreeViewer = new TreeViewer(this, SWT.MULTI | SWT.NONE);
        mTreeViewer.setUseHashlookup(true);
        mProfileProvider = reader.getProfileProvider();
        mProfileProvider.setTreeViewer(mTreeViewer);
        SelectionAdapter listener = mProfileProvider.getColumnListener();
        final Tree tree = mTreeViewer.getTree();
        tree.setHeaderVisible(true);
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Get the column names from the ProfileProvider
        String[] columnNames = mProfileProvider.getColumnNames();
        int[] columnWidths = mProfileProvider.getColumnWidths();
        int[] columnAlignments = mProfileProvider.getColumnAlignments();
        for (int ii = 0; ii < columnWidths.length; ++ii) {
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            column.setText(columnNames[ii]);
            column.setWidth(columnWidths[ii]);
            column.setMoveable(true);
            column.addSelectionListener(listener);
            column.setAlignment(columnAlignments[ii]);
        }

        // Add a listener to the tree so that we can make the row
        // height smaller.
        tree.addListener(SWT.MeasureItem, new Listener() {
            public void handleEvent(Event event) {
                int fontHeight = event.gc.getFontMetrics().getHeight();
                event.height = fontHeight;
            }
        });

        mTreeViewer.setContentProvider(mProfileProvider);
        mTreeViewer.setLabelProvider(mProfileProvider.getLabelProvider());
        mTreeViewer.setInput(mProfileProvider.getRoot());

        // Create another composite to hold the label and text box
        Composite composite = new Composite(this, SWT.NONE);
        composite.setLayout(new GridLayout(2, false));
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Add a label for the search box
        Label label = new Label(composite, SWT.NONE);
        label.setText("Find:");

        // Add a text box for searching for method names
        mSearchBox = new Text(composite, SWT.BORDER);
        mSearchBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Display display = getDisplay();
        mColorNoMatch = new Color(display, 255, 200, 200);
        mColorMatch = mSearchBox.getBackground();

        mSearchBox.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent ev) {
                String query = mSearchBox.getText();
                if (query.length() == 0)
                    return;
                findName(query);
            }
        });

        // Add a key listener to the text box so that we can clear
        // the text box if the user presses <ESC>.
        mSearchBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.keyCode == SWT.ESC) {
                    mSearchBox.setText("");
                } else if (event.keyCode == SWT.CR) {
                    String query = mSearchBox.getText();
                    if (query.length() == 0)
                        return;
                    findNextName(query);
                }
            }
        });

        // Also add a key listener to the tree viewer so that the
        // user can just start typing anywhere in the tree view.
        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (event.keyCode == SWT.ESC) {
                    mSearchBox.setText("");
                } else if (event.keyCode == SWT.BS) {
                    // Erase the last character from the search box
                    String text = mSearchBox.getText();
                    int len = text.length();
                    String chopped;
                    if (len <= 1)
                        chopped = "";
                    else
                        chopped = text.substring(0, len - 1);
                    mSearchBox.setText(chopped);
                } else if (event.keyCode == SWT.CR) {
                    String query = mSearchBox.getText();
                    if (query.length() == 0)
                        return;
                    findNextName(query);
                } else {
                    // Append the given character to the search box
                    String str = String.valueOf(event.character);
                    mSearchBox.append(str);
                }
                event.doit = false;
            }
        });

        // Add a selection listener to the tree so that the user can click
        // on a method that is a child or parent and jump to that method.
        mTreeViewer
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    public void selectionChanged(SelectionChangedEvent ev) {
                        ISelection sel = ev.getSelection();
                        if (sel.isEmpty())
                            return;
                        if (sel instanceof IStructuredSelection) {
                            IStructuredSelection selection = (IStructuredSelection) sel;
                            Object element = selection.getFirstElement();
                            if (element == null)
                                return;
                            if (element instanceof MethodData) {
                                MethodData md = (MethodData) element;
                                highlightMethod(md, true);
                            }
                            if (element instanceof ProfileData) {
                                MethodData md = ((ProfileData) element)
                                        .getMethodData();
                                highlightMethod(md, true);
                            }
                        }
                    }
                });
        
        // Add a tree listener so that we can expand the parents and children
        // of a method when a method is expanded.
        mTreeViewer.addTreeListener(new ITreeViewerListener() {
            public void treeExpanded(TreeExpansionEvent event) {
                Object element = event.getElement();
                if (element instanceof MethodData) {
                    MethodData md = (MethodData) element;
                    expandNode(md);
                }
            }
            public void treeCollapsed(TreeExpansionEvent event) {
            }
        });

        tree.addListener(SWT.MouseDown, new Listener() {
            public void handleEvent(Event event) {
                Point point = new Point(event.x, event.y);
                TreeItem treeItem = tree.getItem(point);
                MethodData md = mProfileProvider.findMatchingTreeItem(treeItem);
                if (md == null)
                    return;
                ArrayList<Selection> selections = new ArrayList<Selection>();
                selections.add(Selection.highlight("MethodData", md));
                mSelectionController.change(selections, "ProfileView");
            }
        });
    }

    private void findName(String query) {
        MethodData md = mProfileProvider.findMatchingName(query);
        selectMethod(md);
    }

    private void findNextName(String query) {
        MethodData md = mProfileProvider.findNextMatchingName(query);
        selectMethod(md);
    }

    private void selectMethod(MethodData md) {
        if (md == null) {
            mSearchBox.setBackground(mColorNoMatch);
            return;
        }
        mSearchBox.setBackground(mColorMatch);
        highlightMethod(md, false);
    }

    public void update(Observable objservable, Object arg) {
        // Ignore updates from myself
        if (arg == "ProfileView")
            return;
        // System.out.printf("profileview update from %s\n", arg);
        ArrayList<Selection> selections;
        selections = mSelectionController.getSelections();
        for (Selection selection : selections) {
            Selection.Action action = selection.getAction();
            if (action != Selection.Action.Highlight)
                continue;
            String name = selection.getName();
            if (name == "MethodData") {
                MethodData md = (MethodData) selection.getValue();
                highlightMethod(md, true);
                return;
            }
            if (name == "Call") {
                Call call = (Call) selection.getValue();
                MethodData md = call.mMethodData;
                highlightMethod(md, true);
                return;
            }
        }
    }

    private void highlightMethod(MethodData md, boolean clearSearch) {
        if (md == null)
            return;
        // Avoid an infinite recursion
        if (md == mCurrentHighlightedMethod)
            return;
        if (clearSearch) {
            mSearchBox.setText("");
            mSearchBox.setBackground(mColorMatch);
        }
        mCurrentHighlightedMethod = md;
        mTreeViewer.collapseAll();
        // Expand this node and its children
        expandNode(md);
        StructuredSelection sel = new StructuredSelection(md);
        mTreeViewer.setSelection(sel, true);
        Tree tree = mTreeViewer.getTree();
        TreeItem[] items = tree.getSelection();
        tree.setTopItem(items[0]);
        // workaround a Mac bug by adding showItem().
        tree.showItem(items[0]);
    }

    private void expandNode(MethodData md) {
        ProfileNode[] nodes = md.getProfileNodes();
        mTreeViewer.setExpandedState(md, true);
        // Also expand the "Parents" and "Children" nodes.
        for (ProfileNode node : nodes) {
            if (node.isRecursive() == false)
                mTreeViewer.setExpandedState(node, true);
        }
    }
}
