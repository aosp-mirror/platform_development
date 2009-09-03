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

package com.android.ddmuilib;

import com.android.ddmlib.AllocationInfo;
import com.android.ddmlib.Client;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;
import com.android.ddmlib.ClientData.AllocationTrackingStatus;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;

/**
 * Base class for our information panels.
 */
public class AllocationPanel extends TablePanel {

    private final static String PREFS_ALLOC_COL_SIZE = "allocPanel.Col0"; //$NON-NLS-1$
    private final static String PREFS_ALLOC_COL_CLASS = "allocPanel.Col1"; //$NON-NLS-1$
    private final static String PREFS_ALLOC_COL_THREAD = "allocPanel.Col2"; //$NON-NLS-1$
    private final static String PREFS_ALLOC_COL_TRACE_CLASS = "allocPanel.Col3"; //$NON-NLS-1$
    private final static String PREFS_ALLOC_COL_TRACE_METHOD = "allocPanel.Col4"; //$NON-NLS-1$

    private final static String PREFS_ALLOC_SASH = "allocPanel.sash"; //$NON-NLS-1$

    private static final String PREFS_STACK_COL_CLASS = "allocPanel.stack.col0"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_METHOD = "allocPanel.stack.col1"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_FILE = "allocPanel.stack.col2"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_LINE = "allocPanel.stack.col3"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_NATIVE = "allocPanel.stack.col4"; //$NON-NLS-1$

    private Composite mAllocationBase;
    private Table mAllocationTable;
    private TableViewer mAllocationViewer;

    private StackTracePanel mStackTracePanel;
    private Table mStackTraceTable;
    private Button mEnableButton;
    private Button mRequestButton;

    /**
     * Content Provider to display the allocations of a client.
     * Expected input is a {@link Client} object, elements used in the table are of type
     * {@link AllocationInfo}.
     */
    private static class AllocationContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Client) {
                AllocationInfo[] allocs = ((Client)inputElement).getClientData().getAllocations();
                if (allocs != null) {
                    return allocs;
                }
            }

            return new Object[0];
        }

        public void dispose() {
            // pass
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // pass
        }
    }

    /**
     * A Label Provider to use with {@link AllocationContentProvider}. It expects the elements to be
     * of type {@link AllocationInfo}.
     */
    private static class AllocationLabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof AllocationInfo) {
                AllocationInfo alloc = (AllocationInfo)element;
                switch (columnIndex) {
                    case 0:
                        return Integer.toString(alloc.getSize());
                    case 1:
                        return alloc.getAllocatedClass();
                    case 2:
                        return Short.toString(alloc.getThreadId());
                    case 3:
                        StackTraceElement[] traces = alloc.getStackTrace();
                        if (traces.length > 0) {
                            return traces[0].getClassName();
                        }
                        break;
                    case 4:
                        traces = alloc.getStackTrace();
                        if (traces.length > 0) {
                            return traces[0].getMethodName();
                        }
                        break;
                }
            }

            return null;
        }

        public void addListener(ILabelProviderListener listener) {
            // pass
        }

        public void dispose() {
            // pass
        }

        public boolean isLabelProperty(Object element, String property) {
            // pass
            return false;
        }

        public void removeListener(ILabelProviderListener listener) {
            // pass
        }
    }

    /**
     * Create our control(s).
     */
    @Override
    protected Control createControl(Composite parent) {
        final IPreferenceStore store = DdmUiPreferences.getStore();

        // base composite for selected client with enabled thread update.
        mAllocationBase = new Composite(parent, SWT.NONE);
        mAllocationBase.setLayout(new FormLayout());

        // table above the sash
        Composite topParent = new Composite(mAllocationBase, SWT.NONE);
        topParent.setLayout(new GridLayout(2, false));

        mEnableButton = new Button(topParent, SWT.PUSH);
        mEnableButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Client current = getCurrentClient();
                AllocationTrackingStatus status = current.getClientData().getAllocationStatus();
                if (status == AllocationTrackingStatus.ON) {
                    current.enableAllocationTracker(false);
                } else {
                    current.enableAllocationTracker(true);
                }
                current.requestAllocationStatus();
            }
        });

        mRequestButton = new Button(topParent, SWT.PUSH);
        mRequestButton.setText("Get Allocations");
        mRequestButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getCurrentClient().requestAllocationDetails();
            }
        });

        setUpButtons(false /* enabled */, AllocationTrackingStatus.OFF);

        mAllocationTable = new Table(topParent, SWT.MULTI | SWT.FULL_SELECTION);
        GridData gridData;
        mAllocationTable.setLayoutData(gridData = new GridData(GridData.FILL_BOTH));
        gridData.horizontalSpan = 2;
        mAllocationTable.setHeaderVisible(true);
        mAllocationTable.setLinesVisible(true);

        TableHelper.createTableColumn(
                mAllocationTable,
                "Allocation Size",
                SWT.RIGHT,
                "888", //$NON-NLS-1$
                PREFS_ALLOC_COL_SIZE, store);

        TableHelper.createTableColumn(
                mAllocationTable,
                "Allocated Class",
                SWT.LEFT,
                "Allocated Class", //$NON-NLS-1$
                PREFS_ALLOC_COL_CLASS, store);

        TableHelper.createTableColumn(
                mAllocationTable,
                "Thread Id",
                SWT.LEFT,
                "999", //$NON-NLS-1$
                PREFS_ALLOC_COL_THREAD, store);

        TableHelper.createTableColumn(
                mAllocationTable,
                "Allocated in",
                SWT.LEFT,
                "utime", //$NON-NLS-1$
                PREFS_ALLOC_COL_TRACE_CLASS, store);

        TableHelper.createTableColumn(
                mAllocationTable,
                "Allocated in",
                SWT.LEFT,
                "utime", //$NON-NLS-1$
                PREFS_ALLOC_COL_TRACE_METHOD, store);

        mAllocationViewer = new TableViewer(mAllocationTable);
        mAllocationViewer.setContentProvider(new AllocationContentProvider());
        mAllocationViewer.setLabelProvider(new AllocationLabelProvider());

        mAllocationViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                AllocationInfo selectedAlloc = getAllocationSelection(event.getSelection());
                updateAllocationStackTrace(selectedAlloc);
            }
        });

        // the separating sash
        final Sash sash = new Sash(mAllocationBase, SWT.HORIZONTAL);
        Color darkGray = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        sash.setBackground(darkGray);

        // the UI below the sash
        mStackTracePanel = new StackTracePanel();
        mStackTraceTable = mStackTracePanel.createPanel(mAllocationBase,
                PREFS_STACK_COL_CLASS,
                PREFS_STACK_COL_METHOD,
                PREFS_STACK_COL_FILE,
                PREFS_STACK_COL_LINE,
                PREFS_STACK_COL_NATIVE,
                store);

        // now setup the sash.
        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        topParent.setLayoutData(data);

        final FormData sashData = new FormData();
        if (store != null && store.contains(PREFS_ALLOC_SASH)) {
            sashData.top = new FormAttachment(0, store.getInt(PREFS_ALLOC_SASH));
        } else {
            sashData.top = new FormAttachment(50,0); // 50% across
        }
        sashData.left = new FormAttachment(0, 0);
        sashData.right = new FormAttachment(100, 0);
        sash.setLayoutData(sashData);

        data = new FormData();
        data.top = new FormAttachment(sash, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mStackTraceTable.setLayoutData(data);

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = mAllocationBase.getClientArea();
                int bottom = panelRect.height - sashRect.height - 100;
                e.y = Math.max(Math.min(e.y, bottom), 100);
                if (e.y != sashRect.y) {
                    sashData.top = new FormAttachment(0, e.y);
                    store.setValue(PREFS_ALLOC_SASH, e.y);
                    mAllocationBase.layout();
                }
            }
        });

        return mAllocationBase;
    }

    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mAllocationTable.setFocus();
    }

    /**
     * Sent when an existing client information changed.
     * <p/>
     * This is sent from a non UI thread.
     * @param client the updated client.
     * @param changeMask the bit mask describing the changed properties. It can contain
     * any of the following values: {@link Client#CHANGE_INFO}, {@link Client#CHANGE_NAME}
     * {@link Client#CHANGE_DEBUGGER_STATUS}, {@link Client#CHANGE_THREAD_MODE},
     * {@link Client#CHANGE_THREAD_DATA}, {@link Client#CHANGE_HEAP_MODE},
     * {@link Client#CHANGE_HEAP_DATA}, {@link Client#CHANGE_NATIVE_HEAP_DATA}
     *
     * @see IClientChangeListener#clientChanged(Client, int)
     */
    public void clientChanged(final Client client, int changeMask) {
        if (client == getCurrentClient()) {
            if ((changeMask & Client.CHANGE_HEAP_ALLOCATIONS) != 0) {
                try {
                    mAllocationTable.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            mAllocationViewer.refresh();
                            updateAllocationStackCall();
                        }
                    });
                } catch (SWTException e) {
                    // widget is disposed, we do nothing
                }
            } else if ((changeMask & Client.CHANGE_HEAP_ALLOCATION_STATUS) != 0) {
                try {
                    mAllocationTable.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            setUpButtons(true, client.getClientData().getAllocationStatus());
                        }
                    });
                } catch (SWTException e) {
                    // widget is disposed, we do nothing
                }
            }
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}.
     */
    @Override
    public void deviceSelected() {
        // pass
    }

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}.
     */
    @Override
    public void clientSelected() {
        if (mAllocationTable.isDisposed()) {
            return;
        }

        Client client = getCurrentClient();

        mStackTracePanel.setCurrentClient(client);
        mStackTracePanel.setViewerInput(null); // always empty on client selection change.

        if (client != null) {
            setUpButtons(true /* enabled */, client.getClientData().getAllocationStatus());
        } else {
            setUpButtons(false /* enabled */, AllocationTrackingStatus.OFF);
        }

        mAllocationViewer.setInput(client);
    }

    /**
     * Updates the stack call of the currently selected thread.
     * <p/>
     * This <b>must</b> be called from the UI thread.
     */
    private void updateAllocationStackCall() {
        Client client = getCurrentClient();
        if (client != null) {
            // get the current selection in the ThreadTable
            AllocationInfo selectedAlloc = getAllocationSelection(null);

            if (selectedAlloc != null) {
                updateAllocationStackTrace(selectedAlloc);
            } else {
                updateAllocationStackTrace(null);
            }
        }
    }

    /**
     * updates the stackcall of the specified allocation. If <code>null</code> the UI is emptied
     * of current data.
     * @param thread
     */
    private void updateAllocationStackTrace(AllocationInfo alloc) {
        mStackTracePanel.setViewerInput(alloc);
    }

    @Override
    protected void setTableFocusListener() {
        addTableToFocusListener(mAllocationTable);
        addTableToFocusListener(mStackTraceTable);
    }

    /**
     * Returns the current allocation selection or <code>null</code> if none is found.
     * If a {@link ISelection} object is specified, the first {@link AllocationInfo} from this
     * selection is returned, otherwise, the <code>ISelection</code> returned by
     * {@link TableViewer#getSelection()} is used.
     * @param selection the {@link ISelection} to use, or <code>null</code>
     */
    private AllocationInfo getAllocationSelection(ISelection selection) {
        if (selection == null) {
            selection = mAllocationViewer.getSelection();
        }

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            Object object = structuredSelection.getFirstElement();
            if (object instanceof AllocationInfo) {
                return (AllocationInfo)object;
            }
        }

        return null;
    }

    /**
     *
     * @param enabled
     * @param trackingStatus
     */
    private void setUpButtons(boolean enabled, AllocationTrackingStatus trackingStatus) {
        if (enabled) {
            switch (trackingStatus) {
                case UNKNOWN:
                    mEnableButton.setText("?");
                    mEnableButton.setEnabled(false);
                    mRequestButton.setEnabled(false);
                    break;
                case OFF:
                    mEnableButton.setText("Start Tracking");
                    mEnableButton.setEnabled(true);
                    mRequestButton.setEnabled(false);
                    break;
                case ON:
                    mEnableButton.setText("Stop Tracking");
                    mEnableButton.setEnabled(true);
                    mRequestButton.setEnabled(true);
                    break;
            }
        } else {
            mEnableButton.setEnabled(false);
            mRequestButton.setEnabled(false);
            mEnableButton.setText("Start Tracking");
        }
    }
}

