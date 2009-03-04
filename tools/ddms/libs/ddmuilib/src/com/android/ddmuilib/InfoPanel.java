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

import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * Display client info in a two-column format.
 */
public class InfoPanel extends TablePanel {
    private Table mTable;
    private TableColumn mCol2;

    private static final String mLabels[] = {
        "DDM-aware?",
        "App description:",
        "VM version:",
        "Process ID:",
    };
    private static final int ENT_DDM_AWARE = 0;
    private static final int ENT_APP_DESCR = 1;
    private static final int ENT_VM_VERSION = 2;
    private static final int ENT_PROCESS_ID = 3;

    /**
     * Create our control(s).
     */
    @Override
    protected Control createControl(Composite parent) {
        mTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION);
        mTable.setHeaderVisible(false);
        mTable.setLinesVisible(false);

        TableColumn col1 = new TableColumn(mTable, SWT.RIGHT);
        col1.setText("name");
        mCol2 = new TableColumn(mTable, SWT.LEFT);
        mCol2.setText("PlaceHolderContentForWidth");

        TableItem item;
        for (int i = 0; i < mLabels.length; i++) {
            item = new TableItem(mTable, SWT.NONE);
            item.setText(0, mLabels[i]);
            item.setText(1, "-");
        }

        col1.pack();
        mCol2.pack();

        return mTable;
    }
    
    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mTable.setFocus();
    }


    /**
     * Sent when an existing client information changed.
     * <p/>
     * This is sent from a non UI thread.
     * @param client the updated client.
     * @param changeMask the bit mask describing the changed properties. It can contain
     * any of the following values: {@link Client#CHANGE_PORT}, {@link Client#CHANGE_NAME}
     * {@link Client#CHANGE_DEBUGGER_INTEREST}, {@link Client#CHANGE_THREAD_MODE},
     * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
     * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
     *
     * @see IClientChangeListener#clientChanged(Client, int)
     */
    public void clientChanged(final Client client, int changeMask) {
        if (client == getCurrentClient()) {
            if ((changeMask & Client.CHANGE_INFO) == Client.CHANGE_INFO) {
                if (mTable.isDisposed())
                    return;

                mTable.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        clientSelected();
                    }
                });
            }
        }
    }


    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}
     */
    @Override
    public void deviceSelected() {
        // pass
    }

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}
     */
    @Override
    public void clientSelected() {
        if (mTable.isDisposed())
            return;

        Client client = getCurrentClient();

        if (client == null) {
            for (int i = 0; i < mLabels.length; i++) {
                TableItem item = mTable.getItem(i);
                item.setText(1, "-");
            }
        } else {
            TableItem item;
            String clientDescription, vmIdentifier, isDdmAware,
                pid;

            ClientData cd = client.getClientData();
            synchronized (cd) {
                clientDescription = (cd.getClientDescription() != null) ?
                        cd.getClientDescription() : "?";
                vmIdentifier = (cd.getVmIdentifier() != null) ?
                        cd.getVmIdentifier() : "?";
                isDdmAware = cd.isDdmAware() ?
                        "yes" : "no";
                pid = (cd.getPid() != 0) ?
                        String.valueOf(cd.getPid()) : "?";
            }

            item = mTable.getItem(ENT_APP_DESCR);
            item.setText(1, clientDescription);
            item = mTable.getItem(ENT_VM_VERSION);
            item.setText(1, vmIdentifier);
            item = mTable.getItem(ENT_DDM_AWARE);
            item.setText(1, isDdmAware);
            item = mTable.getItem(ENT_PROCESS_ID);
            item.setText(1, pid);
        }

        mCol2.pack();

        //Log.i("ddms", "InfoPanel: changed " + client);
    }

    @Override
    protected void setTableFocusListener() {
        addTableToFocusListener(mTable);
    }
}

