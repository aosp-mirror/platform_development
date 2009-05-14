/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.adt.internal.editors.layout;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.views.properties.PropertySheetEntry;
import org.eclipse.ui.views.properties.PropertySheetPage;

/**
 * A customized property sheet page for the graphical layout editor.
 * <p/>
 * Currently it just provides a custom tooltip to display attributes javadocs.
 */
public class UiPropertySheetPage extends PropertySheetPage {

    
    public UiPropertySheetPage() {
        super();
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        
        setupTooltip();
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
                        if (data instanceof PropertySheetEntry) {
                            tooltip = ((PropertySheetEntry) data).getDescription();
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

}
