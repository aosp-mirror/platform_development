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

package com.android.ddmuilib.log.event;

import com.android.ddmlib.Client;
import com.android.ddmlib.Device;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.log.LogReceiver.ILogListener;
import com.android.ddmlib.log.LogReceiver.LogEntry;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.IImageLoader;
import com.android.ddmuilib.TablePanel;
import com.android.ddmuilib.actions.ICommonAction;
import com.android.ddmuilib.annotation.UiThread;
import com.android.ddmuilib.annotation.WorkerThread;
import com.android.ddmuilib.log.event.EventDisplay.ILogColumnListener;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Event log viewer
 */
public class EventLogPanel extends TablePanel implements ILogListener,
        ILogColumnListener {

    private final static String TAG_FILE_EXT = ".tag"; //$NON-NLS-1$

    private final static String PREFS_EVENT_DISPLAY = "EventLogPanel.eventDisplay"; //$NON-NLS-1$
    private final static String EVENT_DISPLAY_STORAGE_SEPARATOR = "|"; //$NON-NLS-1$

    static final String PREFS_DISPLAY_WIDTH = "EventLogPanel.width"; //$NON-NLS-1$
    static final String PREFS_DISPLAY_HEIGHT = "EventLogPanel.height"; //$NON-NLS-1$

    private final static int DEFAULT_DISPLAY_WIDTH = 500;
    private final static int DEFAULT_DISPLAY_HEIGHT = 400;

    private IImageLoader mImageLoader;

    private Device mCurrentLoggedDevice;
    private String mCurrentLogFile;
    private LogReceiver mCurrentLogReceiver;
    private EventLogParser mCurrentEventLogParser;

    private Object mLock = new Object();

    /** list of all the events. */
    private final ArrayList<EventContainer> mEvents = new ArrayList<EventContainer>();

    /** list of all the new events, that have yet to be displayed by the ui */
    private final ArrayList<EventContainer> mNewEvents = new ArrayList<EventContainer>();
    /** indicates a pending ui thread display */
    private boolean mPendingDisplay = false;
    
    /** list of all the custom event displays */
    private final ArrayList<EventDisplay> mEventDisplays = new ArrayList<EventDisplay>();

    private final NumberFormat mFormatter = NumberFormat.getInstance();
    private Composite mParent;
    private ScrolledComposite mBottomParentPanel;
    private Composite mBottomPanel;
    private ICommonAction mOptionsAction;
    private ICommonAction mClearAction;
    private ICommonAction mSaveAction;
    private ICommonAction mLoadAction;
    private ICommonAction mImportAction;
    
    /** file containing the current log raw data. */
    private File mTempFile = null;

    public EventLogPanel(IImageLoader imageLoader) {
        super();
        mImageLoader = imageLoader;
        mFormatter.setGroupingUsed(true);
    }

    /**
     * Sets the external actions.
     * <p/>This method sets up the {@link ICommonAction} objects to execute the proper code
     * when triggered by using {@link ICommonAction#setRunnable(Runnable)}.
     * <p/>It will also make sure they are enabled only when possible.
     * @param optionsAction
     * @param clearAction
     * @param saveAction
     * @param loadAction
     * @param importAction
     */
    public void setActions(ICommonAction optionsAction, ICommonAction clearAction,
            ICommonAction saveAction, ICommonAction loadAction, ICommonAction importAction) {
        mOptionsAction = optionsAction;
        mOptionsAction.setRunnable(new Runnable() {
            public void run() {
                openOptionPanel();
            }
        });

        mClearAction = clearAction;
        mClearAction.setRunnable(new Runnable() {
            public void run() {
                clearLog();
            }
        });

        mSaveAction = saveAction;
        mSaveAction.setRunnable(new Runnable() {
            public void run() {
                try {
                    FileDialog fileDialog = new FileDialog(mParent.getShell(), SWT.SAVE);

                    fileDialog.setText("Save Event Log");
                    fileDialog.setFileName("event.log");

                    String fileName = fileDialog.open();
                    if (fileName != null) {
                        saveLog(fileName);
                    }
                } catch (IOException e1) {
                }
            }
        });

        mLoadAction = loadAction;
        mLoadAction.setRunnable(new Runnable() {
            public void run() {
                FileDialog fileDialog = new FileDialog(mParent.getShell(), SWT.OPEN);

                fileDialog.setText("Load Event Log");

                String fileName = fileDialog.open();
                if (fileName != null) {
                    loadLog(fileName);
                }
            }
        });

        mImportAction = importAction;
        mImportAction.setRunnable(new Runnable() {
            public void run() {
                FileDialog fileDialog = new FileDialog(mParent.getShell(), SWT.OPEN);

                fileDialog.setText("Import Bug Report");

                String fileName = fileDialog.open();
                if (fileName != null) {
                    importBugReport(fileName);
                }
            }
        });

        mOptionsAction.setEnabled(false);
        mClearAction.setEnabled(false);
        mSaveAction.setEnabled(false);
    }

    /**
     * Opens the option panel.
     * </p>
     * <b>This must be called from the UI thread</b>
     */
    @UiThread
    public void openOptionPanel() {
        try {
            EventDisplayOptions dialog = new EventDisplayOptions(mImageLoader, mParent.getShell());
            if (dialog.open(mCurrentEventLogParser, mEventDisplays, mEvents)) {
                synchronized (mLock) {
                    // get the new EventDisplay list
                    mEventDisplays.clear();
                    mEventDisplays.addAll(dialog.getEventDisplays());
                    
                    // since the list of EventDisplay changed, we store it.
                    saveEventDisplays();
                    
                    rebuildUi();
                }
            }
        } catch (SWTException e) {
            Log.e("EventLog", e); //$NON-NLS-1$
        }
    }
    
    /**
     * Clears the log.
     * <p/>
     * <b>This must be called from the UI thread</b>
     */
    public void clearLog() {
        try {
            synchronized (mLock) {
                mEvents.clear();
                mNewEvents.clear();
                mPendingDisplay = false;
                for (EventDisplay eventDisplay : mEventDisplays) {
                    eventDisplay.resetUI();
                }
            }
        } catch (SWTException e) {
            Log.e("EventLog", e); //$NON-NLS-1$
        }
    }
    
    /**
     * Saves the content of the event log into a file. The log is saved in the same
     * binary format than on the device.
     * @param filePath
     * @throws IOException
     */
    public void saveLog(String filePath) throws IOException {
        if (mCurrentLoggedDevice != null && mCurrentEventLogParser != null) {
            File destFile = new File(filePath);
            destFile.createNewFile();
            FileInputStream fis = new FileInputStream(mTempFile);
            FileOutputStream fos = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            
            int count;
            
            while ((count = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, count);
            }
            
            fos.close();
            fis.close();
            
            // now we save the tag file
            filePath = filePath + TAG_FILE_EXT;
            mCurrentEventLogParser.saveTags(filePath);
        }
    }

    /**
     * Loads a binary event log (if has associated .tag file) or
     * otherwise loads a textual event log.
     * @param filePath Event log path (and base of potential tag file)
     */
    public void loadLog(String filePath) {
        if ((new File(filePath + TAG_FILE_EXT)).exists()) {
            startEventLogFromFiles(filePath);
        } else {
            try {
                EventLogImporter importer = new EventLogImporter(filePath);
                String[] tags = importer.getTags();
                String[] log = importer.getLog();
                startEventLogFromContent(tags, log);
            } catch (FileNotFoundException e) {
                // If this fails, display the error message from startEventLogFromFiles,
                // and pretend we never tried EventLogImporter
                Log.logAndDisplay(Log.LogLevel.ERROR, "EventLog",
                        String.format("Failure to read %1$s", filePath + TAG_FILE_EXT));
            }

        }
    }
    
    public void importBugReport(String filePath) {
        try {
            BugReportImporter importer = new BugReportImporter(filePath);
            
            String[] tags = importer.getTags();
            String[] log = importer.getLog();
            
            startEventLogFromContent(tags, log);
            
        } catch (FileNotFoundException e) {
            Log.logAndDisplay(LogLevel.ERROR, "Import",
                    "Unable to import bug report: " + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see com.android.ddmuilib.SelectionDependentPanel#clientSelected()
     */
    @Override
    public void clientSelected() {
        // pass
    }

    /* (non-Javadoc)
     * @see com.android.ddmuilib.SelectionDependentPanel#deviceSelected()
     */
    @Override
    public void deviceSelected() {
        startEventLog(getCurrentDevice());
    }
    
    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.AndroidDebugBridge.IClientChangeListener#clientChanged(com.android.ddmlib.Client, int)
     */
    public void clientChanged(Client client, int changeMask) {
        // pass
    }

    /* (non-Javadoc)
     * @see com.android.ddmuilib.Panel#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createControl(Composite parent) {
        mParent = parent;
        mParent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                synchronized (mLock) {
                    if (mCurrentLogReceiver != null) {
                        mCurrentLogReceiver.cancel();
                        mCurrentLogReceiver = null;
                        mCurrentEventLogParser = null;
                        mCurrentLoggedDevice = null;
                        mEventDisplays.clear();
                        mEvents.clear();
                    }
                }
            }
        });

        final IPreferenceStore store = DdmUiPreferences.getStore();

        // init some store stuff
        store.setDefault(PREFS_DISPLAY_WIDTH, DEFAULT_DISPLAY_WIDTH);
        store.setDefault(PREFS_DISPLAY_HEIGHT, DEFAULT_DISPLAY_HEIGHT);
        
        mBottomParentPanel = new ScrolledComposite(parent, SWT.V_SCROLL);
        mBottomParentPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        mBottomParentPanel.setExpandHorizontal(true);
        mBottomParentPanel.setExpandVertical(true);

        mBottomParentPanel.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                if (mBottomPanel != null) {
                    Rectangle r = mBottomParentPanel.getClientArea();
                    mBottomParentPanel.setMinSize(mBottomPanel.computeSize(r.width,
                        SWT.DEFAULT));
                }
            }
        });

        prepareDisplayUi();

        // load the EventDisplay from storage.
        loadEventDisplays();

        // create the ui
        createDisplayUi();
        
        return mBottomParentPanel;
    }

    /* (non-Javadoc)
     * @see com.android.ddmuilib.Panel#postCreation()
     */
    @Override
    protected void postCreation() {
        // pass
    }

    /* (non-Javadoc)
     * @see com.android.ddmuilib.Panel#setFocus()
     */
    @Override
    public void setFocus() {
        mBottomParentPanel.setFocus();
    }
    
    /**
     * Starts a new logcat and set mCurrentLogCat as the current receiver.
     * @param device the device to connect logcat to.
     */
    private void startEventLog(final Device device) {
        if (device == mCurrentLoggedDevice) {
            return;
        }

        // if we have a logcat already running
        if (mCurrentLogReceiver != null) {
            stopEventLog(false);
        }
        mCurrentLoggedDevice = null;
        mCurrentLogFile = null;

        if (device != null) {
            // create a new output receiver
            mCurrentLogReceiver = new LogReceiver(this);

            // start the logcat in a different thread
            new Thread("EventLog")  { //$NON-NLS-1$
                @Override
                public void run() {
                    while (device.isOnline() == false &&
                            mCurrentLogReceiver != null &&
                            mCurrentLogReceiver.isCancelled() == false) {
                        try {
                            sleep(2000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (mCurrentLogReceiver == null || mCurrentLogReceiver.isCancelled()) {
                        // logcat was stopped/cancelled before the device became ready.
                        return;
                    }

                    try {
                        mCurrentLoggedDevice = device;
                        synchronized (mLock) {
                            mCurrentEventLogParser = new EventLogParser();
                            mCurrentEventLogParser.init(device);
                        }
                        
                        // update the event display with the new parser.
                        updateEventDisplays();
                        
                        // prepare the temp file that will contain the raw data
                        mTempFile = File.createTempFile("android-event-", ".log");

                        device.runEventLogService(mCurrentLogReceiver);
                    } catch (Exception e) {
                        Log.e("EventLog", e);
                    } finally {
                    }
                }
            }.start();
        }
    }
    
    private void startEventLogFromFiles(final String fileName) {
        // if we have a logcat already running
        if (mCurrentLogReceiver != null) {
            stopEventLog(false);
        }
        mCurrentLoggedDevice = null;
        mCurrentLogFile = null;

        // create a new output receiver
        mCurrentLogReceiver = new LogReceiver(this);
        
        mSaveAction.setEnabled(false);

        // start the logcat in a different thread
        new Thread("EventLog")  { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    mCurrentLogFile = fileName;
                    synchronized (mLock) {
                        mCurrentEventLogParser = new EventLogParser();
                        if (mCurrentEventLogParser.init(fileName + TAG_FILE_EXT) == false) {
                            mCurrentEventLogParser = null;
                            Log.logAndDisplay(LogLevel.ERROR, "EventLog",
                                    String.format("Failure to read %1$s", fileName + TAG_FILE_EXT));
                            return;
                        }
                    }
                    
                    // update the event display with the new parser.
                    updateEventDisplays();
                    
                    runLocalEventLogService(fileName, mCurrentLogReceiver);
                } catch (Exception e) {
                    Log.e("EventLog", e);
                } finally {
                }
            }
        }.start();
    }

    private void startEventLogFromContent(final String[] tags, final String[] log) {
        // if we have a logcat already running
        if (mCurrentLogReceiver != null) {
            stopEventLog(false);
        }
        mCurrentLoggedDevice = null;
        mCurrentLogFile = null;

        // create a new output receiver
        mCurrentLogReceiver = new LogReceiver(this);
        
        mSaveAction.setEnabled(false);

        // start the logcat in a different thread
        new Thread("EventLog")  { //$NON-NLS-1$
            @Override
            public void run() {
                try {
                    synchronized (mLock) {
                        mCurrentEventLogParser = new EventLogParser();
                        if (mCurrentEventLogParser.init(tags) == false) {
                            mCurrentEventLogParser = null;
                            return;
                        }
                    }
                    
                    // update the event display with the new parser.
                    updateEventDisplays();
                    
                    runLocalEventLogService(log, mCurrentLogReceiver);
                } catch (Exception e) {
                    Log.e("EventLog", e);
                } finally {
                }
            }
        }.start();
    }


    public void stopEventLog(boolean inUiThread) {
        if (mCurrentLogReceiver != null) {
            mCurrentLogReceiver.cancel();

            // when the thread finishes, no one will reference that object
            // and it'll be destroyed
            synchronized (mLock) {
                mCurrentLogReceiver = null;
                mCurrentEventLogParser = null;

                mCurrentLoggedDevice = null;
                mEvents.clear();
                mNewEvents.clear();
                mPendingDisplay = false;
            }

            resetUI(inUiThread);
        }
        
        if (mTempFile != null) {
            mTempFile.delete();
            mTempFile = null;
        }
    }

    private void resetUI(boolean inUiThread) {
        mEvents.clear();

        // the ui is static we just empty it.
        if (inUiThread) {
            resetUiFromUiThread();
        } else {
            try {
                Display d = mBottomParentPanel.getDisplay();

                // run sync as we need to update right now.
                d.syncExec(new Runnable() {
                    public void run() {
                        if (mBottomParentPanel.isDisposed() == false) {
                            resetUiFromUiThread();
                        }
                    }
                });
            } catch (SWTException e) {
                // display is disposed, we're quitting. Do nothing.
            }
        }
    }
    
    private void resetUiFromUiThread() {
        synchronized(mLock) {
            for (EventDisplay eventDisplay : mEventDisplays) {
                eventDisplay.resetUI();
            }
        }
        mOptionsAction.setEnabled(false);
        mClearAction.setEnabled(false);
        mSaveAction.setEnabled(false);
    }

    private void prepareDisplayUi() {
        mBottomPanel = new Composite(mBottomParentPanel, SWT.NONE);
        mBottomParentPanel.setContent(mBottomPanel);
    }

    private void createDisplayUi() {
        RowLayout rowLayout = new RowLayout();
        rowLayout.wrap = true;
        rowLayout.pack = false;
        rowLayout.justify = true;
        rowLayout.fill = true;
        rowLayout.type = SWT.HORIZONTAL;
        mBottomPanel.setLayout(rowLayout);
        
        IPreferenceStore store = DdmUiPreferences.getStore();
        int displayWidth = store.getInt(PREFS_DISPLAY_WIDTH);
        int displayHeight = store.getInt(PREFS_DISPLAY_HEIGHT);
        
        for (EventDisplay eventDisplay : mEventDisplays) {
            Control c = eventDisplay.createComposite(mBottomPanel, mCurrentEventLogParser, this);
            if (c != null) {
                RowData rd = new RowData();
                rd.height = displayHeight;
                rd.width = displayWidth;
                c.setLayoutData(rd);
            }
            
            Table table = eventDisplay.getTable();
            if (table != null) {
                addTableToFocusListener(table);
            }
        }

        mBottomPanel.layout();
        mBottomParentPanel.setMinSize(mBottomPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        mBottomParentPanel.layout();
    }
    
    /**
     * Rebuild the display ui.
     */
    @UiThread
    private void rebuildUi() {
        synchronized (mLock) {
            // we need to rebuild the ui. First we get rid of it.
            mBottomPanel.dispose();
            mBottomPanel = null;
            
            prepareDisplayUi();
            createDisplayUi();
            
            // and fill it
            
            boolean start_event = false;
            synchronized (mNewEvents) {
                mNewEvents.addAll(0, mEvents);
                
                if (mPendingDisplay == false) {
                    mPendingDisplay = true;
                    start_event = true;
                }
            }
            
            if (start_event) {
                scheduleUIEventHandler();
            }
            
            Rectangle r = mBottomParentPanel.getClientArea();
            mBottomParentPanel.setMinSize(mBottomPanel.computeSize(r.width,
                SWT.DEFAULT));
        }
    }


    /**
     * Processes a new {@link LogEntry} by parsing it with {@link EventLogParser} and displaying it.
     * @param entry The new log entry
     * @see LogReceiver.ILogListener#newEntry(LogEntry) 
     */
    @WorkerThread
    public void newEntry(LogEntry entry) {
        synchronized (mLock) {
            if (mCurrentEventLogParser != null) {
                EventContainer event = mCurrentEventLogParser.parse(entry);
                if (event != null) {
                    handleNewEvent(event);
                }
            }
        }
    }
    
    @WorkerThread
    private void handleNewEvent(EventContainer event) {
        // add the event to the generic list
        mEvents.add(event);
        
        // add to the list of events that needs to be displayed, and trigger a
        // new display if needed.
        boolean start_event = false;
        synchronized (mNewEvents) {
            mNewEvents.add(event);
            
            if (mPendingDisplay == false) {
                mPendingDisplay = true;
                start_event = true;
            }
        }
        
        if (start_event == false) {
            // we're done
            return;
        }

        scheduleUIEventHandler();
    }

    /**
     * Schedules the UI thread to execute a {@link Runnable} calling {@link #displayNewEvents()}.
     */
    private void scheduleUIEventHandler() {
        try  {
            Display d = mBottomParentPanel.getDisplay();
            d.asyncExec(new Runnable() {
                public void run() {
                    if (mBottomParentPanel.isDisposed() == false) {
                        if (mCurrentEventLogParser != null) {
                            displayNewEvents();
                        }
                    }
                }
            });
        } catch (SWTException e) {
            // if the ui is disposed, do nothing 
        }
    }

    /**
     * Processes raw data coming from the log service.
     * @see LogReceiver.ILogListener#newData(byte[], int, int)
     */
    public void newData(byte[] data, int offset, int length) {
        if (mTempFile != null) {
            try {
                FileOutputStream fos = new FileOutputStream(mTempFile, true /* append */);
                fos.write(data, offset, length);
                fos.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    }

    @UiThread
    private void displayNewEvents() {
        // never display more than 1,000 events in this loop. We can't do too much in the UI thread.
        int count = 0;

        // prepare the displays
        for (EventDisplay eventDisplay : mEventDisplays) {
            eventDisplay.startMultiEventDisplay();
        }
        
        // display the new events
        EventContainer event = null;
        boolean need_to_reloop = false;
        do {
            // get the next event to display.
            synchronized (mNewEvents) {
                if (mNewEvents.size() > 0) {
                    if (count > 200) {
                        // there are still events to be displayed, but we don't want to hog the
                        // UI thread for too long, so we stop this runnable, but launch a new
                        // one to keep going.
                        need_to_reloop = true;
                        event = null;
                    } else {
                        event = mNewEvents.remove(0);
                        count++;
                    }
                } else {
                    // we're done.
                    event = null;
                    mPendingDisplay = false;
                }
            }

            if (event != null) {
                // notify the event display
                for (EventDisplay eventDisplay : mEventDisplays) {
                    eventDisplay.newEvent(event, mCurrentEventLogParser);
                }
            }
        } while (event != null);

        // we're done displaying events.
        for (EventDisplay eventDisplay : mEventDisplays) {
            eventDisplay.endMultiEventDisplay();
        }
        
        // if needed, ask the UI thread to re-run this method.
        if (need_to_reloop) {
            scheduleUIEventHandler();
        }
    }

    /**
     * Loads the {@link EventDisplay}s from the preference store.
     */
    private void loadEventDisplays() {
        IPreferenceStore store = DdmUiPreferences.getStore();
        String storage = store.getString(PREFS_EVENT_DISPLAY);
        
        if (storage.length() > 0) {
            String[] values = storage.split(Pattern.quote(EVENT_DISPLAY_STORAGE_SEPARATOR));
            
            for (String value : values) {
                EventDisplay eventDisplay = EventDisplay.load(value);
                if (eventDisplay != null) {
                    mEventDisplays.add(eventDisplay);
                }
            }
        }
    }

    /**
     * Saves the {@link EventDisplay}s into the {@link DdmUiPreferences} store.
     */
    private void saveEventDisplays() {
        IPreferenceStore store = DdmUiPreferences.getStore();
        
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        
        for (EventDisplay eventDisplay : mEventDisplays) {
            String storage = eventDisplay.getStorageString();
            if (storage != null) {
                if (first == false) {
                    sb.append(EVENT_DISPLAY_STORAGE_SEPARATOR);
                } else {
                    first = false;
                }
                
                sb.append(storage);
            }
        }

        store.setValue(PREFS_EVENT_DISPLAY, sb.toString());
    }

    /**
     * Updates the {@link EventDisplay} with the new {@link EventLogParser}.
     * <p/>
     * This will run asynchronously in the UI thread.
     */
    @WorkerThread
    private void updateEventDisplays() {
        try {
            Display d = mBottomParentPanel.getDisplay();

            d.asyncExec(new Runnable() {
                public void run() {
                    if (mBottomParentPanel.isDisposed() == false) {
                        for (EventDisplay eventDisplay : mEventDisplays) {
                            eventDisplay.setNewLogParser(mCurrentEventLogParser);
                        }
                        
                        mOptionsAction.setEnabled(true);
                        mClearAction.setEnabled(true);
                        if (mCurrentLogFile == null) {
                            mSaveAction.setEnabled(true);
                        } else {
                            mSaveAction.setEnabled(false);
                        }
                    }
                }
            });
        } catch (SWTException e) {
            // display is disposed: do nothing.
        }
    }

    @UiThread
    public void columnResized(int index, TableColumn sourceColumn) {
        for (EventDisplay eventDisplay : mEventDisplays) {
            eventDisplay.resizeColumn(index, sourceColumn);
        }
    }

    /**
     * Runs an event log service out of a local file.
     * @param fileName the full file name of the local file containing the event log.
     * @param logReceiver the receiver that will handle the log
     * @throws IOException 
     */
    @WorkerThread
    private void runLocalEventLogService(String fileName, LogReceiver logReceiver)
            throws IOException {
        byte[] buffer = new byte[256];
        
        FileInputStream fis = new FileInputStream(fileName);
        
        int count;
        while ((count = fis.read(buffer)) != -1) {
            logReceiver.parseNewData(buffer, 0, count);
        }
    }
    
    @WorkerThread
    private void runLocalEventLogService(String[] log, LogReceiver currentLogReceiver) {
        synchronized (mLock) {
            for (String line : log) {
                EventContainer event = mCurrentEventLogParser.parse(line);
                if (event != null) {
                    handleNewEvent(event);
                }
            }
        }
    }
}
