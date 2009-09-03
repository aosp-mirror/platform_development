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
import com.android.ddmlib.ThreadInfo;
import com.android.ddmlib.AndroidDebugBridge.IClientChangeListener;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
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
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Table;

import java.util.Date;

/**
 * Base class for our information panels.
 */
public class ThreadPanel extends TablePanel {
    
    private final static String PREFS_THREAD_COL_ID = "threadPanel.Col0"; //$NON-NLS-1$
    private final static String PREFS_THREAD_COL_TID = "threadPanel.Col1"; //$NON-NLS-1$
    private final static String PREFS_THREAD_COL_STATUS = "threadPanel.Col2"; //$NON-NLS-1$
    private final static String PREFS_THREAD_COL_UTIME = "threadPanel.Col3"; //$NON-NLS-1$
    private final static String PREFS_THREAD_COL_STIME = "threadPanel.Col4"; //$NON-NLS-1$
    private final static String PREFS_THREAD_COL_NAME = "threadPanel.Col5"; //$NON-NLS-1$
    
    private final static String PREFS_THREAD_SASH = "threadPanel.sash"; //$NON-NLS-1$

    private static final String PREFS_STACK_COL_CLASS = "threadPanel.stack.col0"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_METHOD = "threadPanel.stack.col1"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_FILE = "threadPanel.stack.col2"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_LINE = "threadPanel.stack.col3"; //$NON-NLS-1$
    private static final String PREFS_STACK_COL_NATIVE = "threadPanel.stack.col4"; //$NON-NLS-1$
    
    private Display mDisplay;
    private Composite mBase;
    private Label mNotEnabled;
    private Label mNotSelected;
    
    private Composite mThreadBase;
    private Table mThreadTable;
    private TableViewer mThreadViewer;

    private Composite mStackTraceBase;
    private Button mRefreshStackTraceButton;
    private Label mStackTraceTimeLabel;
    private StackTracePanel mStackTracePanel;
    private Table mStackTraceTable;

    /** Indicates if a timer-based Runnable is current requesting thread updates regularly. */
    private boolean mMustStopRecurringThreadUpdate = false;
    /** Flag to tell the recurring thread update to stop running */
    private boolean mRecurringThreadUpdateRunning = false;

    private Object mLock = new Object();

    private static final String[] THREAD_STATUS = {
        "zombie", "running", "timed-wait", "monitor",
        "wait", "init", "start", "native", "vmwait"
    };
    
    /**
     * Content Provider to display the threads of a client.
     * Expected input is a {@link Client} object.
     */
    private static class ThreadContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Client) {
                return ((Client)inputElement).getClientData().getThreads();
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
     * A Label Provider to use with {@link ThreadContentProvider}. It expects the elements to be
     * of type {@link ThreadInfo}.
     */
    private static class ThreadLabelProvider implements ITableLabelProvider {

        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof ThreadInfo) {
                ThreadInfo thread = (ThreadInfo)element;
                switch (columnIndex) {
                    case 0:
                        return (thread.isDaemon() ? "*" : "") + //$NON-NLS-1$ //$NON-NLS-2$
                            String.valueOf(thread.getThreadId());
                    case 1:
                        return String.valueOf(thread.getTid());
                    case 2:
                        if (thread.getStatus() >= 0 && thread.getStatus() < THREAD_STATUS.length)
                            return THREAD_STATUS[thread.getStatus()];
                        return "unknown";
                    case 3:
                        return String.valueOf(thread.getUtime());
                    case 4:
                        return String.valueOf(thread.getStime());
                    case 5:
                        return thread.getThreadName();
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
        mDisplay = parent.getDisplay();

        final IPreferenceStore store = DdmUiPreferences.getStore();

        mBase = new Composite(parent, SWT.NONE);
        mBase.setLayout(new StackLayout());

        // UI for thread not enabled
        mNotEnabled = new Label(mBase, SWT.CENTER | SWT.WRAP);
        mNotEnabled.setText("Thread updates not enabled for selected client\n"
            + "(use toolbar button to enable)");

        // UI for not client selected
        mNotSelected = new Label(mBase, SWT.CENTER | SWT.WRAP);
        mNotSelected.setText("no client is selected");

        // base composite for selected client with enabled thread update.
        mThreadBase = new Composite(mBase, SWT.NONE);
        mThreadBase.setLayout(new FormLayout());
        
        // table above the sash
        mThreadTable = new Table(mThreadBase, SWT.MULTI | SWT.FULL_SELECTION);
        mThreadTable.setHeaderVisible(true);
        mThreadTable.setLinesVisible(true);

        TableHelper.createTableColumn(
                mThreadTable,
                "ID",
                SWT.RIGHT,
                "888", //$NON-NLS-1$
                PREFS_THREAD_COL_ID, store);

        TableHelper.createTableColumn(
                mThreadTable,
                "Tid",
                SWT.RIGHT,
                "88888", //$NON-NLS-1$
                PREFS_THREAD_COL_TID, store);

        TableHelper.createTableColumn(
                mThreadTable,
                "Status",
                SWT.LEFT,
                "timed-wait", //$NON-NLS-1$
                PREFS_THREAD_COL_STATUS, store);

        TableHelper.createTableColumn(
                mThreadTable,
                "utime",
                SWT.RIGHT,
                "utime", //$NON-NLS-1$
                PREFS_THREAD_COL_UTIME, store);

        TableHelper.createTableColumn(
                mThreadTable,
                "stime",
                SWT.RIGHT,
                "utime", //$NON-NLS-1$
                PREFS_THREAD_COL_STIME, store);

        TableHelper.createTableColumn(
                mThreadTable,
                "Name",
                SWT.LEFT,
                "android.class.ReallyLongClassName.MethodName", //$NON-NLS-1$
                PREFS_THREAD_COL_NAME, store);
        
        mThreadViewer = new TableViewer(mThreadTable);
        mThreadViewer.setContentProvider(new ThreadContentProvider());
        mThreadViewer.setLabelProvider(new ThreadLabelProvider());

        mThreadViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                ThreadInfo selectedThread = getThreadSelection(event.getSelection());
                updateThreadStackTrace(selectedThread);
            }
        });
        mThreadViewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                ThreadInfo selectedThread = getThreadSelection(event.getSelection());
                if (selectedThread != null) {
                    Client client = (Client)mThreadViewer.getInput();
                    
                    if (client != null) {
                        client.requestThreadStackTrace(selectedThread.getThreadId());
                    }
                }
            }
        });
        
        // the separating sash
        final Sash sash = new Sash(mThreadBase, SWT.HORIZONTAL);
        Color darkGray = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);
        sash.setBackground(darkGray);
        
        // the UI below the sash
        mStackTraceBase = new Composite(mThreadBase, SWT.NONE);
        mStackTraceBase.setLayout(new GridLayout(2, false));

        mRefreshStackTraceButton = new Button(mStackTraceBase, SWT.PUSH);
        mRefreshStackTraceButton.setText("Refresh");
        mRefreshStackTraceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ThreadInfo selectedThread = getThreadSelection(null);
                if (selectedThread != null) {
                    Client currentClient = getCurrentClient();
                    if (currentClient != null) {
                        currentClient.requestThreadStackTrace(selectedThread.getThreadId());
                    }
                }
            }
        });
        
        mStackTraceTimeLabel = new Label(mStackTraceBase, SWT.NONE);
        mStackTraceTimeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mStackTracePanel = new StackTracePanel();
        mStackTraceTable = mStackTracePanel.createPanel(mStackTraceBase,
                PREFS_STACK_COL_CLASS,
                PREFS_STACK_COL_METHOD,
                PREFS_STACK_COL_FILE,
                PREFS_STACK_COL_LINE,
                PREFS_STACK_COL_NATIVE,
                store);
        
        GridData gd;
        mStackTraceTable.setLayoutData(gd = new GridData(GridData.FILL_BOTH));
        gd.horizontalSpan = 2;
        
        // now setup the sash.
        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        mThreadTable.setLayoutData(data);

        final FormData sashData = new FormData();
        if (store != null && store.contains(PREFS_THREAD_SASH)) {
            sashData.top = new FormAttachment(0, store.getInt(PREFS_THREAD_SASH));
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
        mStackTraceBase.setLayoutData(data);

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = mThreadBase.getClientArea();
                int bottom = panelRect.height - sashRect.height - 100;
                e.y = Math.max(Math.min(e.y, bottom), 100);
                if (e.y != sashRect.y) {
                    sashData.top = new FormAttachment(0, e.y);
                    store.setValue(PREFS_THREAD_SASH, e.y);
                    mThreadBase.layout();
                }
            }
        });

        ((StackLayout)mBase.getLayout()).topControl = mNotSelected;

        return mBase;
    }
    
    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mThreadTable.setFocus();
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
            if ((changeMask & Client.CHANGE_THREAD_MODE) != 0 ||
                    (changeMask & Client.CHANGE_THREAD_DATA) != 0) {
                try {
                    mThreadTable.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            clientSelected();
                        }
                    });
                } catch (SWTException e) {
                    // widget is disposed, we do nothing
                }
            } else if ((changeMask & Client.CHANGE_THREAD_STACKTRACE) != 0) {
                try {
                    mThreadTable.getDisplay().asyncExec(new Runnable() {
                        public void run() {
                            updateThreadStackCall();
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
        if (mThreadTable.isDisposed()) {
            return;
        }

        Client client = getCurrentClient();
        
        mStackTracePanel.setCurrentClient(client);

        if (client != null) {
            if (!client.isThreadUpdateEnabled()) {
                ((StackLayout)mBase.getLayout()).topControl = mNotEnabled;
                mThreadViewer.setInput(null);

                // if we are currently updating the thread, stop doing it.
                mMustStopRecurringThreadUpdate = true;
            } else {
                ((StackLayout)mBase.getLayout()).topControl = mThreadBase;
                mThreadViewer.setInput(client);

                synchronized (mLock) {
                    // if we're not updating we start the process
                    if (mRecurringThreadUpdateRunning == false) {
                        startRecurringThreadUpdate();
                    } else if (mMustStopRecurringThreadUpdate) {
                        // else if there's a runnable that's still going to get called, lets
                        // simply cancel the stop, and keep going
                        mMustStopRecurringThreadUpdate = false;
                    }
                }
            }
        } else {
            ((StackLayout)mBase.getLayout()).topControl = mNotSelected;
            mThreadViewer.setInput(null);
        }

        mBase.layout();
    }
    
    /**
     * Updates the stack call of the currently selected thread.
     * <p/>
     * This <b>must</b> be called from the UI thread.
     */
    private void updateThreadStackCall() {
        Client client = getCurrentClient();
        if (client != null) {
            // get the current selection in the ThreadTable
            ThreadInfo selectedThread = getThreadSelection(null);
            
            if (selectedThread != null) {
                updateThreadStackTrace(selectedThread);
            } else {
                updateThreadStackTrace(null);
            }
        }
    }
    
    /**
     * updates the stackcall of the specified thread. If <code>null</code> the UI is emptied
     * of current data.
     * @param thread
     */
    private void updateThreadStackTrace(ThreadInfo thread) {
        mStackTracePanel.setViewerInput(thread);
        
        if (thread != null) {
            mRefreshStackTraceButton.setEnabled(true);
            long stackcallTime = thread.getStackCallTime();
            if (stackcallTime != 0) {
                String label = new Date(stackcallTime).toString();
                mStackTraceTimeLabel.setText(label);
            } else {
                mStackTraceTimeLabel.setText(""); //$NON-NLS-1$
            }
        } else {
            mRefreshStackTraceButton.setEnabled(true);
            mStackTraceTimeLabel.setText(""); //$NON-NLS-1$
        }
    }

    @Override
    protected void setTableFocusListener() {
        addTableToFocusListener(mThreadTable);
        addTableToFocusListener(mStackTraceTable);
    }

    /**
     * Initiate recurring events. We use a shorter "initialWait" so we do the
     * first execution sooner. We don't do it immediately because we want to
     * give the clients a chance to get set up.
     */
    private void startRecurringThreadUpdate() {
        mRecurringThreadUpdateRunning = true;
        int initialWait = 1000;

        mDisplay.timerExec(initialWait, new Runnable() {
            public void run() {
                synchronized (mLock) {
                    // lets check we still want updates.
                    if (mMustStopRecurringThreadUpdate == false) {
                        Client client = getCurrentClient();
                        if (client != null) {
                            client.requestThreadUpdate();

                            mDisplay.timerExec(
                                    DdmUiPreferences.getThreadRefreshInterval() * 1000, this);
                        } else {
                            // we don't have a Client, which means the runnable is not
                            // going to be called through the timer. We reset the running flag.
                            mRecurringThreadUpdateRunning = false;
                        }
                    } else {
                        // else actually stops (don't call the timerExec) and reset the flags.
                        mRecurringThreadUpdateRunning = false;
                        mMustStopRecurringThreadUpdate = false;
                    }
                }
            }
        });
    }
    
    /**
     * Returns the current thread selection or <code>null</code> if none is found.
     * If a {@link ISelection} object is specified, the first {@link ThreadInfo} from this selection
     * is returned, otherwise, the <code>ISelection</code> returned by
     * {@link TableViewer#getSelection()} is used.
     * @param selection the {@link ISelection} to use, or <code>null</code>
     */
    private ThreadInfo getThreadSelection(ISelection selection) {
        if (selection == null) {
            selection = mThreadViewer.getSelection();
        }
        
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            Object object = structuredSelection.getFirstElement();
            if (object instanceof ThreadInfo) {
                return (ThreadInfo)object;
            }
        }
        
        return null;
    }

}

