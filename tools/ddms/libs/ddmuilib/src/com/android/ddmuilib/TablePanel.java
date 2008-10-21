/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib;

import com.android.ddmuilib.ITableFocusListener.IFocusedTableActivator;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import java.util.Arrays;

/**
 * Base class for panel containing Table that need to support copy-paste-selectAll
 */
public abstract class TablePanel extends ClientDisplayPanel {
    private ITableFocusListener mGlobalListener;

    /**
     * Sets a TableFocusListener which will be notified when one of the tables
     * gets or loses focus.
     *
     * @param listener
     */
    public final void setTableFocusListener(ITableFocusListener listener) {
        // record the global listener, to make sure table created after
        // this call will still be setup.
        mGlobalListener = listener;

        setTableFocusListener();
    }

    /**
     * Sets up the Table of object of the panel to work with the global listener.<br>
     * Default implementation does nothing.
     */
    protected void setTableFocusListener() {

    }

    /**
     * Sets up a Table object to notify the global Table Focus listener when it
     * gets or loses the focus.
     *
     * @param table the Table object.
     * @param colStart
     * @param colEnd
     */
    protected final void addTableToFocusListener(final Table table,
            final int colStart, final int colEnd) {
        // create the activator for this table
        final IFocusedTableActivator activator = new IFocusedTableActivator() {
            public void copy(Clipboard clipboard) {
                int[] selection = table.getSelectionIndices();

                // we need to sort the items to be sure.
                Arrays.sort(selection);

                // all lines must be concatenated.
                StringBuilder sb = new StringBuilder();

                // loop on the selection and output the file.
                for (int i : selection) {
                    TableItem item = table.getItem(i);
                    for (int c = colStart ; c <= colEnd ; c++) {
                        sb.append(item.getText(c));
                        sb.append('\t');
                    }
                    sb.append('\n');
                }

                // now add that to the clipboard if the string has content
                String data = sb.toString();
                if (data != null || data.length() > 0) {
                    clipboard.setContents(
                            new Object[] { data },
                            new Transfer[] { TextTransfer.getInstance() });
                }
            }

            public void selectAll() {
                table.selectAll();
            }
        };

        // add the focus listener on the table to notify the global listener
        table.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                mGlobalListener.focusGained(activator);
            }

            public void focusLost(FocusEvent e) {
                mGlobalListener.focusLost(activator);
            }
        });
    }

    /**
     * Sets up a Table object to notify the global Table Focus listener when it
     * gets or loses the focus.<br>
     * When the copy method is invoked, all columns are put in the clipboard, separated
     * by tabs
     *
     * @param table the Table object.
     */
    protected final void addTableToFocusListener(final Table table) {
        addTableToFocusListener(table, 0, table.getColumnCount()-1);
    }

}
