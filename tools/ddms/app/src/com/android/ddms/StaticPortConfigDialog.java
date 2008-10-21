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

package com.android.ddms;

import com.android.ddmuilib.TableHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dialog to configure the static debug ports.
 *
 */
public class StaticPortConfigDialog extends Dialog {

    /** Preference name for the 0th column width */
    private static final String PREFS_DEVICE_COL = "spcd.deviceColumn"; //$NON-NLS-1$

    /** Preference name for the 1st column width */
    private static final String PREFS_APP_COL = "spcd.AppColumn"; //$NON-NLS-1$

    /** Preference name for the 2nd column width */
    private static final String PREFS_PORT_COL = "spcd.PortColumn"; //$NON-NLS-1$

    private static final int COL_DEVICE = 0;
    private static final int COL_APPLICATION = 1;
    private static final int COL_PORT = 2;


    private static final int DLG_WIDTH = 500;
    private static final int DLG_HEIGHT = 300;

    private Shell mShell;
    private Shell mParent;

    private Table mPortTable;

    /**
     * Array containing the list of already used static port to avoid
     * duplication.
     */
    private ArrayList<Integer> mPorts = new ArrayList<Integer>();

    /**
     * Basic constructor.
     * @param parent
     */
    public StaticPortConfigDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.BORDER | SWT.APPLICATION_MODAL);
    }

    /**
     * Open and display the dialog. This method returns only when the
     * user closes the dialog somehow.
     *
     */
    public void open() {
        createUI();

        if (mParent == null || mShell == null) {
            return;
        }

        updateFromStore();

        // Set the dialog size.
        mShell.setMinimumSize(DLG_WIDTH, DLG_HEIGHT);
        Rectangle r = mParent.getBounds();
        // get the center new top left.
        int cx = r.x + r.width/2;
        int x = cx - DLG_WIDTH / 2;
        int cy = r.y + r.height/2;
        int y = cy - DLG_HEIGHT / 2;
        mShell.setBounds(x, y, DLG_WIDTH, DLG_HEIGHT);

        mShell.pack();

        // actually open the dialog
        mShell.open();

        // event loop until the dialog is closed.
        Display display = mParent.getDisplay();
        while (!mShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
    }

    /**
     * Creates the dialog ui.
     */
    private void createUI() {
        mParent = getParent();
        mShell = new Shell(mParent, getStyle());
        mShell.setText("Static Port Configuration");

        mShell.setLayout(new GridLayout(1, true));

        mShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                event.doit = true;
            }
        });

        // center part with the list on the left and the buttons
        // on the right.
        Composite main = new Composite(mShell, SWT.NONE);
        main.setLayoutData(new GridData(GridData.FILL_BOTH));
        main.setLayout(new GridLayout(2, false));

        // left part: list view
        mPortTable = new Table(main, SWT.SINGLE | SWT.FULL_SELECTION);
        mPortTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        mPortTable.setHeaderVisible(true);
        mPortTable.setLinesVisible(true);

        TableHelper.createTableColumn(mPortTable, "Device Serial Number",
                SWT.LEFT, "emulator-5554", //$NON-NLS-1$
                PREFS_DEVICE_COL, PrefsDialog.getStore());

        TableHelper.createTableColumn(mPortTable, "Application Package",
                SWT.LEFT, "com.android.samples.phone", //$NON-NLS-1$
                PREFS_APP_COL, PrefsDialog.getStore());

        TableHelper.createTableColumn(mPortTable, "Debug Port",
                SWT.RIGHT, "Debug Port", //$NON-NLS-1$
                PREFS_PORT_COL, PrefsDialog.getStore());

        // right part: buttons
        Composite buttons = new Composite(main, SWT.NONE);
        buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        buttons.setLayout(new GridLayout(1, true));

        Button newButton = new Button(buttons, SWT.NONE);
        newButton.setText("New...");
        newButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StaticPortEditDialog dlg = new StaticPortEditDialog(mShell,
                        mPorts);
                if (dlg.open()) {
                    // get the text
                    String device = dlg.getDeviceSN();
                    String app = dlg.getAppName();
                    int port = dlg.getPortNumber();

                    // add it to the list
                    addEntry(device, app, port);
                }
            }
        });

        final Button editButton = new Button(buttons, SWT.NONE);
        editButton.setText("Edit...");
        editButton.setEnabled(false);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = mPortTable.getSelectionIndex();
                String oldDeviceName = getDeviceName(index);
                String oldAppName = getAppName(index);
                String oldPortNumber = getPortNumber(index);
                StaticPortEditDialog dlg = new StaticPortEditDialog(mShell,
                        mPorts, oldDeviceName, oldAppName, oldPortNumber);
                if (dlg.open()) {
                    // get the text
                    String deviceName = dlg.getDeviceSN();
                    String app = dlg.getAppName();
                    int port = dlg.getPortNumber();

                    // add it to the list
                    replaceEntry(index, deviceName, app, port);
                }
            }
        });

        final Button deleteButton = new Button(buttons, SWT.NONE);
        deleteButton.setText("Delete");
        deleteButton.setEnabled(false);
        deleteButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = mPortTable.getSelectionIndex();
                removeEntry(index);
            }
        });

        // bottom part with the ok/cancel
        Composite bottomComp = new Composite(mShell, SWT.NONE);
        bottomComp.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_CENTER));
        bottomComp.setLayout(new GridLayout(2, true));

        Button okButton = new Button(bottomComp, SWT.NONE);
        okButton.setText("OK");
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateStore();
                mShell.close();
            }
        });

        Button cancelButton = new Button(bottomComp, SWT.NONE);
        cancelButton.setText("Cancel");
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mShell.close();
            }
        });

        mPortTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // get the selection index
                int index = mPortTable.getSelectionIndex();

                boolean enabled = index != -1;
                editButton.setEnabled(enabled);
                deleteButton.setEnabled(enabled);
            }
        });

        mShell.pack();

    }

    /**
     * Add a new entry in the list.
     * @param deviceName the serial number of the device
     * @param appName java package for the application
     * @param portNumber port number
     */
    private void addEntry(String deviceName, String appName, int portNumber) {
        // create a new item for the table
        TableItem item = new TableItem(mPortTable, SWT.NONE);

        item.setText(COL_DEVICE, deviceName);
        item.setText(COL_APPLICATION, appName);
        item.setText(COL_PORT, Integer.toString(portNumber));

        // add the port to the list of port number used.
        mPorts.add(portNumber);
    }

    /**
     * Remove an entry from the list.
     * @param index The index of the entry to be removed
     */
    private void removeEntry(int index) {
        // remove from the ui
        mPortTable.remove(index);

        // and from the port list.
        mPorts.remove(index);
    }

    /**
     * Replace an entry in the list with new values.
     * @param index The index of the item to be replaced
     * @param deviceName the serial number of the device
     * @param appName The new java package for the application
     * @param portNumber The new port number.
     */
    private void replaceEntry(int index, String deviceName, String appName, int portNumber) {
        // get the table item by index
        TableItem item = mPortTable.getItem(index);

        // set its new value
        item.setText(COL_DEVICE, deviceName);
        item.setText(COL_APPLICATION, appName);
        item.setText(COL_PORT, Integer.toString(portNumber));

        // and replace the port number in the port list.
        mPorts.set(index, portNumber);
    }


    /**
     * Returns the device name for a specific index
     * @param index The index
     * @return the java package name of the application
     */
    private String getDeviceName(int index) {
        TableItem item = mPortTable.getItem(index);
        return item.getText(COL_DEVICE);
    }

    /**
     * Returns the application name for a specific index
     * @param index The index
     * @return the java package name of the application
     */
    private String getAppName(int index) {
        TableItem item = mPortTable.getItem(index);
        return item.getText(COL_APPLICATION);
    }

    /**
     * Returns the port number for a specific index
     * @param index The index
     * @return the port number
     */
    private String getPortNumber(int index) {
        TableItem item = mPortTable.getItem(index);
        return item.getText(COL_PORT);
    }

    /**
     * Updates the ui from the value in the preference store.
     */
    private void updateFromStore() {
        // get the map from the debug port manager
        DebugPortProvider provider = DebugPortProvider.getInstance();
        Map<String, Map<String, Integer>> map = provider.getPortList();

        // we're going to loop on the keys and fill the table.
        Set<String> deviceKeys = map.keySet();

        for (String deviceKey : deviceKeys) {
            Map<String, Integer> deviceMap = map.get(deviceKey);
            if (deviceMap != null) {
                Set<String> appKeys = deviceMap.keySet();

                for (String appKey : appKeys) {
                    Integer port = deviceMap.get(appKey);
                    if (port != null) {
                        addEntry(deviceKey, appKey, port);
                    }
                }
            }
        }
    }

    /**
     * Update the store from the content of the ui.
     */
    private void updateStore() {
        // create a new Map object and fill it.
        HashMap<String, Map<String, Integer>> map = new HashMap<String, Map<String, Integer>>();

        int count = mPortTable.getItemCount();

        for (int i = 0 ; i < count ; i++) {
            TableItem item = mPortTable.getItem(i);
            String deviceName = item.getText(COL_DEVICE);

            Map<String, Integer> deviceMap = map.get(deviceName);
            if (deviceMap == null) {
                deviceMap = new HashMap<String, Integer>();
                map.put(deviceName, deviceMap);
            }

            deviceMap.put(item.getText(COL_APPLICATION), Integer.valueOf(item.getText(COL_PORT)));
        }

        // set it in the store through the debug port manager.
        DebugPortProvider provider = DebugPortProvider.getInstance();
        provider.setPortList(map);
    }
}
