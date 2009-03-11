/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdkuilib;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The APK Configuration widget is a table that is added to the given parent composite.
 * <p/>
 * To use, create it using {@link #ApkConfigWidget(Composite)} then
 * call {@link #fillTable(Map)} to set the initial list of configurations.
 */
public class ApkConfigWidget {
    private final static int INDEX_NAME = 0;
    private final static int INDEX_FILTER = 1;
    
    private Table mApkConfigTable;
    private Button mEditButton;
    private Button mDelButton;

    public ApkConfigWidget(final Composite parent) {
        final Composite apkConfigComp = new Composite(parent, SWT.NONE);
        apkConfigComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        apkConfigComp.setLayout(new GridLayout(2, false));
        
        mApkConfigTable = new Table(apkConfigComp, SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
        mApkConfigTable.setHeaderVisible(true);
        mApkConfigTable.setLinesVisible(true);

        GridData data = new GridData();
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mApkConfigTable.setLayoutData(data);

        // create the table columns
        final TableColumn column0 = new TableColumn(mApkConfigTable, SWT.NONE);
        column0.setText("Name");
        column0.setWidth(100);
        final TableColumn column1 = new TableColumn(mApkConfigTable, SWT.NONE);
        column1.setText("Configuration");
        column1.setWidth(100);

        Composite buttonComp = new Composite(apkConfigComp, SWT.NONE);
        buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        GridLayout gl;
        buttonComp.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;

        Button newButton = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
        newButton.setText("New...");
        newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mEditButton = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
        mEditButton.setText("Edit...");
        mEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mDelButton = new Button(buttonComp, SWT.PUSH | SWT.FLAT);
        mDelButton.setText("Delete");
        mDelButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ApkConfigEditDialog dlg = new ApkConfigEditDialog(null /*name*/, null /*filter*/,
                        apkConfigComp.getShell());
                if (dlg.open() == Dialog.OK) {
                    TableItem item = new TableItem(mApkConfigTable, SWT.NONE);
                    item.setText(INDEX_NAME, dlg.getName());
                    item.setText(INDEX_FILTER, dlg.getFilter());
                    
                    onSelectionChanged();
                }
            }
        });
        
        mEditButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the current selection (single mode so we don't care about any item beyond
                // index 0).
                TableItem[] items = mApkConfigTable.getSelection();
                if (items.length != 0) {
                    ApkConfigEditDialog dlg = new ApkConfigEditDialog(
                            items[0].getText(INDEX_NAME), items[0].getText(INDEX_FILTER),
                            apkConfigComp.getShell());
                    if (dlg.open() == Dialog.OK) {
                        items[0].setText(INDEX_NAME, dlg.getName());
                        items[0].setText(INDEX_FILTER, dlg.getFilter());
                    }
                }
            }
        });
        
        mDelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the current selection (single mode so we don't care about any item beyond
                // index 0).
                int[] indices = mApkConfigTable.getSelectionIndices();
                if (indices.length != 0) {
                    TableItem item = mApkConfigTable.getItem(indices[0]);
                    if (MessageDialog.openQuestion(parent.getShell(),
                            "Apk Configuration deletion",
                            String.format(
                                    "Are you sure you want to delete configuration '%1$s'?",
                                    item.getText(INDEX_NAME)))) {
                        // delete the item.
                        mApkConfigTable.remove(indices[0]);
                        
                        onSelectionChanged();
                    }
                }
            }
        });
        
        // Add a listener to resize the column to the full width of the table
        mApkConfigTable.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = mApkConfigTable.getClientArea();
                column0.setWidth(r.width * 30 / 100); // 30%  
                column1.setWidth(r.width * 70 / 100); // 70%
            }
        });
        
        // add a selection listener on the table, to enable/disable buttons.
        mApkConfigTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onSelectionChanged();
            }
        });
    }
    
    public void fillTable(Map<String, String> apkConfigMap) {
        // get the names in a list so that we can sort them.
        if (apkConfigMap != null) {
            Set<String> keys = apkConfigMap.keySet();
            String[] keyArray = keys.toArray(new String[keys.size()]);
            Arrays.sort(keyArray);
            
            for (String key : keyArray) {
                TableItem item = new TableItem(mApkConfigTable, SWT.NONE);
                item.setText(INDEX_NAME, key);
                item.setText(INDEX_FILTER, apkConfigMap.get(key));
            }
        }
        
        onSelectionChanged();
    }

    public Map<String, String> getApkConfigs() {
        // go through all the items from the table and fill a new map
        HashMap<String, String> map = new HashMap<String, String>();
        
        TableItem[] items = mApkConfigTable.getItems();
        for (TableItem item : items) {
            map.put(item.getText(INDEX_NAME), item.getText(INDEX_FILTER));
        }

        return map;
    }
    
    /**
     * Handles table selection changes.
     */
    private void onSelectionChanged() {
        if (mApkConfigTable.getSelectionCount() > 0) {
            mEditButton.setEnabled(true);
            mDelButton.setEnabled(true);
        } else {
            mEditButton.setEnabled(false);
            mDelButton.setEnabled(false);
        }
    }
}
