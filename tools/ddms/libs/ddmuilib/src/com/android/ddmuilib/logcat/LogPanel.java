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

package com.android.ddmuilib.logcat;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.IImageLoader;
import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.SelectionDependentPanel;
import com.android.ddmuilib.TableHelper;
import com.android.ddmuilib.ITableFocusListener.IFocusedTableActivator;
import com.android.ddmuilib.actions.ICommonAction;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogPanel extends SelectionDependentPanel {

    private static final int STRING_BUFFER_LENGTH = 10000;

    /** no filtering. Only one tab with everything. */
    public static final int FILTER_NONE = 0;
    /** manual mode for filter. all filters are manually created. */
    public static final int FILTER_MANUAL = 1;
    /** automatic mode for filter (pid mode).
     * All filters are automatically created. */
    public static final int FILTER_AUTO_PID = 2;
    /** automatic mode for filter (tag mode).
     * All filters are automatically created. */
    public static final int FILTER_AUTO_TAG = 3;
    /** Manual filtering mode + new filter for debug app, if needed */
    public static final int FILTER_DEBUG = 4;

    public static final int COLUMN_MODE_MANUAL = 0;
    public static final int COLUMN_MODE_AUTO = 1;

    public static String PREFS_TIME;
    public static String PREFS_LEVEL;
    public static String PREFS_PID;
    public static String PREFS_TAG;
    public static String PREFS_MESSAGE;

    /**
     * This pattern is meant to parse the first line of a log message with the option
     * 'logcat -v long'. The first line represents the date, tag, severity, etc.. while the
     * following lines are the message (can be several line).<br>
     * This first line looks something like<br>
     * <code>"[ 00-00 00:00:00.000 &lt;pid&gt;:0x&lt;???&gt; &lt;severity&gt;/&lt;tag&gt;]"</code>
     * <br>
     * Note: severity is one of V, D, I, W, or EM<br>
     * Note: the fraction of second value can have any number of digit.
     * Note the tag should be trim as it may have spaces at the end.
     */
    private static Pattern sLogPattern = Pattern.compile(
            "^\\[\\s(\\d\\d-\\d\\d\\s\\d\\d:\\d\\d:\\d\\d\\.\\d+)" + //$NON-NLS-1$
            "\\s+(\\d*):(0x[0-9a-fA-F]+)\\s([VDIWE])/(.*)\\]$"); //$NON-NLS-1$

    /**
     * Interface for Storage Filter manager. Implementation of this interface
     * provide a custom way to archive an reload filters.
     */
    public interface ILogFilterStorageManager {

        public LogFilter[] getFilterFromStore();

        public void saveFilters(LogFilter[] filters);

        public boolean requiresDefaultFilter();
    }

    private Composite mParent;
    private IPreferenceStore mStore;

    /** top object in the view */
    private TabFolder mFolders;

    private LogColors mColors;

    private ILogFilterStorageManager mFilterStorage;

    private LogCatOuputReceiver mCurrentLogCat;

    /**
     * Circular buffer containing the logcat output. This is unfiltered.
     * The valid content goes from <code>mBufferStart</code> to
     * <code>mBufferEnd - 1</code>. Therefore its number of item is
     * <code>mBufferEnd - mBufferStart</code>.
     */
    private LogMessage[] mBuffer = new LogMessage[STRING_BUFFER_LENGTH];

    /** Represents the oldest message in the buffer */
    private int mBufferStart = -1;

    /**
     * Represents the next usable item in the buffer to receive new message.
     * This can be equal to mBufferStart, but when used mBufferStart will be
     * incremented as well.
     */
    private int mBufferEnd = -1;

    /** Filter list */
    private LogFilter[] mFilters;

    /** Default filter */
    private LogFilter mDefaultFilter;

    /** Current filter being displayed */
    private LogFilter mCurrentFilter;

    /** Filtering mode */
    private int mFilterMode = FILTER_NONE;

    /** Device currently running logcat */
    private IDevice mCurrentLoggedDevice = null;

    private ICommonAction mDeleteFilterAction;
    private ICommonAction mEditFilterAction;

    private ICommonAction[] mLogLevelActions;

    /** message data, separated from content for multi line messages */
    protected static class LogMessageInfo {
        public LogLevel logLevel;
        public int pid;
        public String pidString;
        public String tag;
        public String time;
    }

    /** pointer to the latest LogMessageInfo. this is used for multi line
     * log message, to reuse the info regarding level, pid, etc...
     */
    private LogMessageInfo mLastMessageInfo = null;

    private boolean mPendingAsyncRefresh = false;

    /** loader for the images. the implementation will varie between standalone
     * app and eclipse plugin app and eclipse plugin. */
    private IImageLoader mImageLoader;

    private String mDefaultLogSave;

    private int mColumnMode = COLUMN_MODE_MANUAL;
    private Font mDisplayFont;

    private ITableFocusListener mGlobalListener;

    /** message data, separated from content for multi line messages */
    protected static class LogMessage {
        public LogMessageInfo data;
        public String msg;

        @Override
        public String toString() {
            return data.time + ": " //$NON-NLS-1$
                + data.logLevel + "/" //$NON-NLS-1$
                + data.tag + "(" //$NON-NLS-1$
                + data.pidString + "): " //$NON-NLS-1$
                + msg;
        }
    }

    /**
     * objects able to receive the output of a remote shell command,
     * specifically a logcat command in this case
     */
    private final class LogCatOuputReceiver extends MultiLineReceiver {

        public boolean isCancelled = false;

        public LogCatOuputReceiver() {
            super();

            setTrimLine(false);
        }

        @Override
        public void processNewLines(String[] lines) {
            if (isCancelled == false) {
                processLogLines(lines);
            }
        }

        public boolean isCancelled() {
            return isCancelled;
        }
    }

    /**
     * Parser class for the output of a "ps" shell command executed on a device.
     * This class looks for a specific pid to find the process name from it.
     * Once found, the name is used to update a filter and a tab object
     *
     */
    private class PsOutputReceiver extends MultiLineReceiver {

        private LogFilter mFilter;

        private TabItem mTabItem;

        private int mPid;

        /** set to true when we've found the pid we're looking for */
        private boolean mDone = false;

        PsOutputReceiver(int pid, LogFilter filter, TabItem tabItem) {
            mPid = pid;
            mFilter = filter;
            mTabItem = tabItem;
        }

        public boolean isCancelled() {
            return mDone;
        }

        @Override
        public void processNewLines(String[] lines) {
            for (String line : lines) {
                if (line.startsWith("USER")) { //$NON-NLS-1$
                    continue;
                }
                // get the pid.
                int index = line.indexOf(' ');
                if (index == -1) {
                    continue;
                }
                // look for the next non blank char
                index++;
                while (line.charAt(index) == ' ') {
                    index++;
                }

                // this is the start of the pid.
                // look for the end.
                int index2 = line.indexOf(' ', index);

                // get the line
                String pidStr = line.substring(index, index2);
                int pid = Integer.parseInt(pidStr);
                if (pid != mPid) {
                    continue;
                } else {
                    // get the process name
                    index = line.lastIndexOf(' ');
                    final String name = line.substring(index + 1);

                    mFilter.setName(name);

                    // update the tab
                    Display d = mFolders.getDisplay();
                    d.asyncExec(new Runnable() {
                       public void run() {
                           mTabItem.setText(name);
                       }
                    });

                    // we're done with this ps.
                    mDone = true;
                    return;
                }
            }
        }

    }


    /**
     * Create the log view with some default parameters
     * @param imageLoader the image loader.
     * @param colors The display color object
     * @param filterStorage the storage for user defined filters.
     * @param mode The filtering mode
     */
    public LogPanel(IImageLoader imageLoader, LogColors colors,
            ILogFilterStorageManager filterStorage, int mode) {
        mImageLoader = imageLoader;
        mColors = colors;
        mFilterMode = mode;
        mFilterStorage = filterStorage;
        mStore = DdmUiPreferences.getStore();
    }

    public void setActions(ICommonAction deleteAction, ICommonAction editAction,
            ICommonAction[] logLevelActions) {
        mDeleteFilterAction = deleteAction;
        mEditFilterAction = editAction;
        mLogLevelActions = logLevelActions;
    }

    /**
     * Sets the column mode. Must be called before creatUI
     * @param mode the column mode. Valid values are COLUMN_MOD_MANUAL and
     *  COLUMN_MODE_AUTO
     */
    public void setColumnMode(int mode) {
        mColumnMode  = mode;
    }

    /**
     * Sets the display font.
     * @param font The display font.
     */
    public void setFont(Font font) {
        mDisplayFont = font;

        if (mFilters != null) {
            for (LogFilter f : mFilters) {
                Table table = f.getTable();
                if (table != null) {
                    table.setFont(font);
                }
            }
        }

        if (mDefaultFilter != null) {
            Table table = mDefaultFilter.getTable();
            if (table != null) {
                table.setFont(font);
            }
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed
     * with {@link #getCurrentDevice()}.
     */
    @Override
    public void deviceSelected() {
        startLogCat(getCurrentDevice());
    }

    /**
     * Sent when a new client is selected. The new client can be accessed
     * with {@link #getCurrentClient()}.
     */
    @Override
    public void clientSelected() {
        // pass
    }


    /**
     * Creates a control capable of displaying some information.  This is
     * called once, when the application is initializing, from the UI thread.
     */
    @Override
    protected Control createControl(Composite parent) {
        mParent = parent;

        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(1, false));

        // create the tab folder
        mFolders = new TabFolder(top, SWT.NONE);
        mFolders.setLayoutData(new GridData(GridData.FILL_BOTH));
        mFolders.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentFilter != null) {
                    mCurrentFilter.setSelectedState(false);
                }
                mCurrentFilter = getCurrentFilter();
                mCurrentFilter.setSelectedState(true);
                updateColumns(mCurrentFilter.getTable());
                if (mCurrentFilter.getTempFilterStatus()) {
                    initFilter(mCurrentFilter);
                }
                selectionChanged(mCurrentFilter);
            }
        });


        Composite bottom = new Composite(top, SWT.NONE);
        bottom.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        bottom.setLayout(new GridLayout(3, false));

        Label label = new Label(bottom, SWT.NONE);
        label.setText("Filter:");

        final Text filterText = new Text(bottom, SWT.SINGLE | SWT.BORDER);
        filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filterText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateFilteringWith(filterText.getText());
            }
        });

        /*
        Button addFilterBtn = new Button(bottom, SWT.NONE);
        addFilterBtn.setImage(mImageLoader.loadImage("add.png", //$NON-NLS-1$
                addFilterBtn.getDisplay()));
        */

        // get the filters
        createFilters();

        // for each filter, create a tab.
        int index = 0;

        if (mDefaultFilter != null) {
            createTab(mDefaultFilter, index++, false);
        }

        if (mFilters != null) {
            for (LogFilter f : mFilters) {
                createTab(f, index++, false);
            }
        }

        return top;
    }

    @Override
    protected void postCreation() {
        // pass
    }

    /**
     * Sets the focus to the proper object.
     */
    @Override
    public void setFocus() {
        mFolders.setFocus();
    }


    /**
     * Starts a new logcat and set mCurrentLogCat as the current receiver.
     * @param device the device to connect logcat to.
     */
    public void startLogCat(final IDevice device) {
        if (device == mCurrentLoggedDevice) {
            return;
        }

        // if we have a logcat already running
        if (mCurrentLoggedDevice != null) {
            stopLogCat(false);
            mCurrentLoggedDevice = null;
        }

        resetUI(false);

        if (device != null) {
            // create a new output receiver
            mCurrentLogCat = new LogCatOuputReceiver();

            // start the logcat in a different thread
            new Thread("Logcat")  { //$NON-NLS-1$
                @Override
                public void run() {

                    while (device.isOnline() == false &&
                            mCurrentLogCat != null &&
                            mCurrentLogCat.isCancelled == false) {
                        try {
                            sleep(2000);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    if (mCurrentLogCat == null || mCurrentLogCat.isCancelled) {
                        // logcat was stopped/cancelled before the device became ready.
                        return;
                    }

                    try {
                        mCurrentLoggedDevice = device;
                        device.executeShellCommand("logcat -v long", mCurrentLogCat); //$NON-NLS-1$
                    } catch (Exception e) {
                        Log.e("Logcat", e);
                    } finally {
                        // at this point the command is terminated.
                        mCurrentLogCat = null;
                        mCurrentLoggedDevice = null;
                    }
                }
            }.start();
        }
    }

    /** Stop the current logcat */
    public void stopLogCat(boolean inUiThread) {
        if (mCurrentLogCat != null) {
            mCurrentLogCat.isCancelled = true;

            // when the thread finishes, no one will reference that object
            // and it'll be destroyed
            mCurrentLogCat = null;

            // reset the content buffer
            for (int i = 0 ; i < STRING_BUFFER_LENGTH; i++) {
                mBuffer[i] = null;
            }

            // because it's a circular buffer, it's hard to know if
            // the array is empty with both start/end at 0 or if it's full
            // with both start/end at 0 as well. So to mean empty, we use -1
            mBufferStart = -1;
            mBufferEnd = -1;

            resetFilters();
            resetUI(inUiThread);
        }
    }

    /**
     * Adds a new Filter. This methods displays the UI to create the filter
     * and set up its parameters.<br>
     * <b>MUST</b> be called from the ui thread.
     *
     */
    public void addFilter() {
        EditFilterDialog dlg = new EditFilterDialog(mImageLoader,
                mFolders.getShell());
        if (dlg.open()) {
            synchronized (mBuffer) {
                // get the new filter in the array
                LogFilter filter = dlg.getFilter();
                addFilterToArray(filter);

                int index = mFilters.length - 1;
                if (mDefaultFilter != null) {
                    index++;
                }

                if (false) {

                    for (LogFilter f : mFilters) {
                        if (f.uiReady()) {
                            f.dispose();
                        }
                    }
                    if (mDefaultFilter != null && mDefaultFilter.uiReady()) {
                        mDefaultFilter.dispose();
                    }

                    // for each filter, create a tab.
                    int i = 0;
                    if (mFilters != null) {
                        for (LogFilter f : mFilters) {
                            createTab(f, i++, true);
                        }
                    }
                    if (mDefaultFilter != null) {
                        createTab(mDefaultFilter, i++, true);
                    }
                } else {

                    // create ui for the filter.
                    createTab(filter, index, true);

                    // reset the default as it shouldn't contain the content of
                    // this new filter.
                    if (mDefaultFilter != null) {
                        initDefaultFilter();
                    }
                }

                // select the new filter
                if (mCurrentFilter != null) {
                    mCurrentFilter.setSelectedState(false);
                }
                mFolders.setSelection(index);
                filter.setSelectedState(true);
                mCurrentFilter = filter;

                selectionChanged(filter);

                // finally we update the filtering mode if needed
                if (mFilterMode == FILTER_NONE) {
                    mFilterMode = FILTER_MANUAL;
                }

                mFilterStorage.saveFilters(mFilters);

            }
        }
    }

    /**
     * Edits the current filter. The method displays the UI to edit the filter.
     */
    public void editFilter() {
        if (mCurrentFilter != null && mCurrentFilter != mDefaultFilter) {
            EditFilterDialog dlg = new EditFilterDialog(mImageLoader,
                    mFolders.getShell(),
                    mCurrentFilter);
            if (dlg.open()) {
                synchronized (mBuffer) {
                    // at this point the filter has been updated.
                    // so we update its content
                    initFilter(mCurrentFilter);

                    // and the content of the "other" filter as well.
                    if (mDefaultFilter != null) {
                        initDefaultFilter();
                    }

                    mFilterStorage.saveFilters(mFilters);
                }
            }
        }
    }

    /**
     * Deletes the current filter.
     */
    public void deleteFilter() {
        synchronized (mBuffer) {
            if (mCurrentFilter != null && mCurrentFilter != mDefaultFilter) {
                // remove the filter from the list
                removeFilterFromArray(mCurrentFilter);
                mCurrentFilter.dispose();

                // select the new filter
                mFolders.setSelection(0);
                if (mFilters.length > 0) {
                    mCurrentFilter = mFilters[0];
                } else {
                    mCurrentFilter = mDefaultFilter;
                }

                selectionChanged(mCurrentFilter);

                // update the content of the "other" filter to include what was filtered out
                // by the deleted filter.
                if (mDefaultFilter != null) {
                    initDefaultFilter();
                }

                mFilterStorage.saveFilters(mFilters);
            }
        }
    }

    /**
     * saves the current selection in a text file.
     * @return false if the saving failed.
     */
    public boolean save() {
        synchronized (mBuffer) {
            FileDialog dlg = new FileDialog(mParent.getShell(), SWT.SAVE);
            String fileName;

            dlg.setText("Save log...");
            dlg.setFileName("log.txt");
            String defaultPath = mDefaultLogSave;
            if (defaultPath == null) {
                defaultPath = System.getProperty("user.home"); //$NON-NLS-1$
            }
            dlg.setFilterPath(defaultPath);
            dlg.setFilterNames(new String[] {
                "Text Files (*.txt)"
            });
            dlg.setFilterExtensions(new String[] {
                "*.txt"
            });

            fileName = dlg.open();
            if (fileName != null) {
                mDefaultLogSave = dlg.getFilterPath();

                // get the current table and its selection
                Table currentTable = mCurrentFilter.getTable();

                int[] selection = currentTable.getSelectionIndices();

                // we need to sort the items to be sure.
                Arrays.sort(selection);

                // loop on the selection and output the file.
                try {
                    FileWriter writer = new FileWriter(fileName);

                    for (int i : selection) {
                        TableItem item = currentTable.getItem(i);
                        LogMessage msg = (LogMessage)item.getData();
                        String line = msg.toString();
                        writer.write(line);
                        writer.write('\n');
                    }
                    writer.flush();

                } catch (IOException e) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Empty the current circular buffer.
     */
    public void clear() {
        synchronized (mBuffer) {
            for (int i = 0 ; i < STRING_BUFFER_LENGTH; i++) {
                mBuffer[i] = null;
            }

            mBufferStart = -1;
            mBufferEnd = -1;

            // now we clear the existing filters
            for (LogFilter filter : mFilters) {
                filter.clear();
            }

            // and the default one
            if (mDefaultFilter != null) {
                mDefaultFilter.clear();
            }
        }
    }

    /**
     * Copies the current selection of the current filter as multiline text.
     *
     * @param clipboard The clipboard to place the copied content.
     */
    public void copy(Clipboard clipboard) {
        // get the current table and its selection
        Table currentTable = mCurrentFilter.getTable();

        copyTable(clipboard, currentTable);
    }

    /**
     * Selects all lines.
     */
    public void selectAll() {
        Table currentTable = mCurrentFilter.getTable();
        currentTable.selectAll();
    }

    /**
     * Sets a TableFocusListener which will be notified when one of the tables
     * gets or loses focus.
     *
     * @param listener
     */
    public void setTableFocusListener(ITableFocusListener listener) {
        // record the global listener, to make sure table created after
        // this call will still be setup.
        mGlobalListener = listener;

        // now we setup the existing filters
        for (LogFilter filter : mFilters) {
            Table table = filter.getTable();

            addTableToFocusListener(table);
        }

        // and the default one
        if (mDefaultFilter != null) {
            addTableToFocusListener(mDefaultFilter.getTable());
        }
    }

    /**
     * Sets up a Table object to notify the global Table Focus listener when it
     * gets or loses the focus.
     *
     * @param table the Table object.
     */
    private void addTableToFocusListener(final Table table) {
        // create the activator for this table
        final IFocusedTableActivator activator = new IFocusedTableActivator() {
            public void copy(Clipboard clipboard) {
                copyTable(clipboard, table);
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
     * Copies the current selection of a Table into the provided Clipboard, as
     * multi-line text.
     *
     * @param clipboard The clipboard to place the copied content.
     * @param table The table to copy from.
     */
    private static void copyTable(Clipboard clipboard, Table table) {
        int[] selection = table.getSelectionIndices();

        // we need to sort the items to be sure.
        Arrays.sort(selection);

        // all lines must be concatenated.
        StringBuilder sb = new StringBuilder();

        // loop on the selection and output the file.
        for (int i : selection) {
            TableItem item = table.getItem(i);
            LogMessage msg = (LogMessage)item.getData();
            String line = msg.toString();
            sb.append(line);
            sb.append('\n');
        }

        // now add that to the clipboard
        clipboard.setContents(new Object[] {
            sb.toString()
        }, new Transfer[] {
            TextTransfer.getInstance()
        });
    }

    /**
     * Sets the log level for the current filter, but does not save it.
     * @param i
     */
    public void setCurrentFilterLogLevel(int i) {
        LogFilter filter = getCurrentFilter();

        filter.setLogLevel(i);

        initFilter(filter);
    }

    /**
     * Creates a new tab in the folderTab item. Must be called from the ui
     *      thread.
     * @param filter The filter associated with the tab.
     * @param index the index of the tab. if -1, the tab will be added at the
     *          end.
     * @param fillTable If true the table is filled with the current content of
     *          the buffer.
     * @return The TabItem object that was created.
     */
    private TabItem createTab(LogFilter filter, int index, boolean fillTable) {
        synchronized (mBuffer) {
            TabItem item = null;
            if (index != -1) {
                item = new TabItem(mFolders, SWT.NONE, index);
            } else {
                item = new TabItem(mFolders, SWT.NONE);
            }
            item.setText(filter.getName());

            // set the control (the parent is the TabFolder item, always)
            Composite top = new Composite(mFolders, SWT.NONE);
            item.setControl(top);

            top.setLayout(new FillLayout());

            // create the ui, first the table
            final Table t = new Table(top, SWT.MULTI | SWT.FULL_SELECTION);

            if (mDisplayFont != null) {
                t.setFont(mDisplayFont);
            }

            // give the ui objects to the filters.
            filter.setWidgets(item, t);

            t.setHeaderVisible(true);
            t.setLinesVisible(false);

            if (mGlobalListener != null) {
                addTableToFocusListener(t);
            }

            // create a controllistener that will handle the resizing of all the
            // columns (except the last) and of the table itself.
            ControlListener listener = null;
            if (mColumnMode == COLUMN_MODE_AUTO) {
                listener = new ControlListener() {
                    public void controlMoved(ControlEvent e) {
                    }

                    public void controlResized(ControlEvent e) {
                        Rectangle r = t.getClientArea();

                        // get the size of all but the last column
                        int total = t.getColumn(0).getWidth();
                        total += t.getColumn(1).getWidth();
                        total += t.getColumn(2).getWidth();
                        total += t.getColumn(3).getWidth();

                        if (r.width > total) {
                            t.getColumn(4).setWidth(r.width-total);
                        }
                    }
                };

                t.addControlListener(listener);
            }

            // then its column
            TableColumn col = TableHelper.createTableColumn(t, "Time", SWT.LEFT,
                    "00-00 00:00:00", //$NON-NLS-1$
                    PREFS_TIME, mStore);
            if (mColumnMode == COLUMN_MODE_AUTO) {
                col.addControlListener(listener);
            }

            col = TableHelper.createTableColumn(t, "", SWT.CENTER,
                    "D", //$NON-NLS-1$
                    PREFS_LEVEL, mStore);
            if (mColumnMode == COLUMN_MODE_AUTO) {
                col.addControlListener(listener);
            }

            col = TableHelper.createTableColumn(t, "pid", SWT.LEFT,
                    "9999", //$NON-NLS-1$
                    PREFS_PID, mStore);
            if (mColumnMode == COLUMN_MODE_AUTO) {
                col.addControlListener(listener);
            }

            col = TableHelper.createTableColumn(t, "tag", SWT.LEFT,
                    "abcdefgh",  //$NON-NLS-1$
                    PREFS_TAG, mStore);
            if (mColumnMode == COLUMN_MODE_AUTO) {
                col.addControlListener(listener);
            }

            col = TableHelper.createTableColumn(t, "Message", SWT.LEFT,
                    "abcdefghijklmnopqrstuvwxyz0123456789",  //$NON-NLS-1$
                    PREFS_MESSAGE, mStore);
            if (mColumnMode == COLUMN_MODE_AUTO) {
                // instead of listening on resize for the last column, we make
                // it non resizable.
                col.setResizable(false);
            }

            if (fillTable) {
                initFilter(filter);
            }
            return item;
        }
    }

    protected void updateColumns(Table table) {
        if (table != null) {
            int index = 0;
            TableColumn col;

            col = table.getColumn(index++);
            col.setWidth(mStore.getInt(PREFS_TIME));

            col = table.getColumn(index++);
            col.setWidth(mStore.getInt(PREFS_LEVEL));

            col = table.getColumn(index++);
            col.setWidth(mStore.getInt(PREFS_PID));

            col = table.getColumn(index++);
            col.setWidth(mStore.getInt(PREFS_TAG));

            col = table.getColumn(index++);
            col.setWidth(mStore.getInt(PREFS_MESSAGE));
        }
    }

    public void resetUI(boolean inUiThread) {
        if (mFilterMode == FILTER_AUTO_PID || mFilterMode == FILTER_AUTO_TAG) {
            if (inUiThread) {
                mFolders.dispose();
                mParent.pack(true);
                createControl(mParent);
            } else {
                Display d = mFolders.getDisplay();

                // run sync as we need to update right now.
                d.syncExec(new Runnable() {
                    public void run() {
                        mFolders.dispose();
                        mParent.pack(true);
                        createControl(mParent);
                    }
                });
            }
        } else  {
            // the ui is static we just empty it.
            if (mFolders.isDisposed() == false) {
                if (inUiThread) {
                    emptyTables();
                } else {
                    Display d = mFolders.getDisplay();

                    // run sync as we need to update right now.
                    d.syncExec(new Runnable() {
                        public void run() {
                            if (mFolders.isDisposed() == false) {
                                emptyTables();
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Process new Log lines coming from {@link LogCatOuputReceiver}.
     * @param lines the new lines
     */
    protected void processLogLines(String[] lines) {
        // WARNING: this will not work if the string contains more line than
        // the buffer holds.

        if (lines.length > STRING_BUFFER_LENGTH) {
            Log.e("LogCat", "Receiving more lines than STRING_BUFFER_LENGTH");
        }

        // parse the lines and create LogMessage that are stored in a temporary list
        final ArrayList<LogMessage> newMessages = new ArrayList<LogMessage>();

        synchronized (mBuffer) {
            for (String line : lines) {
                // ignore empty lines.
                if (line.length() > 0) {
                    // check for header lines.
                    Matcher matcher = sLogPattern.matcher(line);
                    if (matcher.matches()) {
                        // this is a header line, parse the header and keep it around.
                        mLastMessageInfo = new LogMessageInfo();

                        mLastMessageInfo.time = matcher.group(1);
                        mLastMessageInfo.pidString = matcher.group(2);
                        mLastMessageInfo.pid = Integer.valueOf(mLastMessageInfo.pidString);
                        mLastMessageInfo.logLevel = LogLevel.getByLetterString(matcher.group(4));
                        mLastMessageInfo.tag = matcher.group(5).trim();
                    } else {
                        // This is not a header line.
                        // Create a new LogMessage and process it.
                        LogMessage mc = new LogMessage();

                        if (mLastMessageInfo == null) {
                            // The first line of output wasn't preceded
                            // by a header line; make something up so
                            // that users of mc.data don't NPE.
                            mLastMessageInfo = new LogMessageInfo();
                            mLastMessageInfo.time = "??-?? ??:??:??.???"; //$NON-NLS1$
                            mLastMessageInfo.pidString = "<unknown>"; //$NON-NLS1$
                            mLastMessageInfo.pid = 0;
                            mLastMessageInfo.logLevel = LogLevel.INFO;
                            mLastMessageInfo.tag = "<unknown>"; //$NON-NLS1$
                        }

                        // If someone printed a log message with
                        // embedded '\n' characters, there will
                        // one header line followed by multiple text lines.
                        // Use the last header that we saw.
                        mc.data = mLastMessageInfo;

                        // tabs seem to display as only 1 tab so we replace the leading tabs
                        // by 4 spaces.
                        mc.msg = line.replaceAll("\t", "    "); //$NON-NLS-1$ //$NON-NLS-2$

                        // process the new LogMessage.
                        processNewMessage(mc);

                        // store the new LogMessage
                        newMessages.add(mc);
                    }
                }
            }

            // if we don't have a pending Runnable that will do the refresh, we ask the Display
            // to run one in the UI thread.
            if (mPendingAsyncRefresh == false) {
                mPendingAsyncRefresh = true;

                try {
                    Display display = mFolders.getDisplay();

                    // run in sync because this will update the buffer start/end indices
                    display.asyncExec(new Runnable() {
                        public void run() {
                            asyncRefresh();
                        }
                    });
                } catch (SWTException e) {
                    // display is disposed, we're probably quitting. Let's stop.
                    stopLogCat(false);
                }
            }
        }
    }

    /**
     * Refreshes the UI with new messages.
     */
    private void asyncRefresh() {
        if (mFolders.isDisposed() == false) {
            synchronized (mBuffer) {
                try {
                    // the circular buffer has been updated, let have the filter flush their
                    // display with the new messages.
                    if (mFilters != null) {
                        for (LogFilter f : mFilters) {
                            f.flush();
                        }
                    }

                    if (mDefaultFilter != null) {
                        mDefaultFilter.flush();
                    }
                } finally {
                    // the pending refresh is done.
                    mPendingAsyncRefresh = false;
                }
            }
        } else {
            stopLogCat(true);
        }
    }

    /**
     * Processes a new Message.
     * <p/>This adds the new message to the buffer, and gives it to the existing filters.
     * @param newMessage
     */
    private void processNewMessage(LogMessage newMessage) {
        // if we are in auto filtering mode, make sure we have
        // a filter for this
        if (mFilterMode == FILTER_AUTO_PID ||
                mFilterMode == FILTER_AUTO_TAG) {
           checkFilter(newMessage.data);
        }

        // compute the index where the message goes.
        // was the buffer empty?
        int messageIndex = -1;
        if (mBufferStart == -1) {
            messageIndex = mBufferStart = 0;
            mBufferEnd = 1;
        } else {
            messageIndex = mBufferEnd;

            // check we aren't overwriting start
            if (mBufferEnd == mBufferStart) {
                mBufferStart = (mBufferStart + 1) % STRING_BUFFER_LENGTH;
            }

            // increment the next usable slot index
            mBufferEnd = (mBufferEnd + 1) % STRING_BUFFER_LENGTH;
        }

        LogMessage oldMessage = null;

        // record the message that was there before
        if (mBuffer[messageIndex] != null) {
            oldMessage = mBuffer[messageIndex];
        }

        // then add the new one
        mBuffer[messageIndex] = newMessage;

        // give the new message to every filters.
        boolean filtered = false;
        if (mFilters != null) {
            for (LogFilter f : mFilters) {
                filtered |= f.addMessage(newMessage, oldMessage);
            }
        }
        if (filtered == false && mDefaultFilter != null) {
            mDefaultFilter.addMessage(newMessage, oldMessage);
        }
    }

    private void createFilters() {
        if (mFilterMode == FILTER_DEBUG || mFilterMode == FILTER_MANUAL) {
            // unarchive the filters.
            mFilters = mFilterStorage.getFilterFromStore();

            // set the colors
            if (mFilters != null) {
                for (LogFilter f : mFilters) {
                    f.setColors(mColors);
                }
            }

            if (mFilterStorage.requiresDefaultFilter()) {
                mDefaultFilter = new LogFilter("Log");
                mDefaultFilter.setColors(mColors);
                mDefaultFilter.setSupportsDelete(false);
                mDefaultFilter.setSupportsEdit(false);
            }
        } else if (mFilterMode == FILTER_NONE) {
            // if the filtering mode is "none", we create a single filter that
            // will receive all
            mDefaultFilter = new LogFilter("Log");
            mDefaultFilter.setColors(mColors);
            mDefaultFilter.setSupportsDelete(false);
            mDefaultFilter.setSupportsEdit(false);
        }
    }

    /** Checks if there's an automatic filter for this md and if not
     * adds the filter and the ui.
     * This must be called from the UI!
     * @param md
     * @return true if the filter existed already
     */
    private boolean checkFilter(final LogMessageInfo md) {
        if (true)
            return true;
        // look for a filter that matches the pid
        if (mFilterMode == FILTER_AUTO_PID) {
            for (LogFilter f : mFilters) {
                if (f.getPidFilter() == md.pid) {
                    return true;
                }
            }
        } else if (mFilterMode == FILTER_AUTO_TAG) {
            for (LogFilter f : mFilters) {
                if (f.getTagFilter().equals(md.tag)) {
                    return true;
                }
            }
        }

        // if we reach this point, no filter was found.
        // create a filter with a temporary name of the pid
        final LogFilter newFilter = new LogFilter(md.pidString);
        String name = null;
        if (mFilterMode == FILTER_AUTO_PID) {
            newFilter.setPidMode(md.pid);

            // ask the monitor thread if it knows the pid.
            name = mCurrentLoggedDevice.getClientName(md.pid);
        } else {
            newFilter.setTagMode(md.tag);
            name = md.tag;
        }
        addFilterToArray(newFilter);

        final String fname = name;

        // create the tabitem
        final TabItem newTabItem = createTab(newFilter, -1, true);

        // if the name is unknown
        if (fname == null) {
            // we need to find the process running under that pid.
            // launch a thread do a ps on the device
            new Thread("remote PS") { //$NON-NLS-1$
                @Override
                public void run() {
                    // create the receiver
                    PsOutputReceiver psor = new PsOutputReceiver(md.pid,
                            newFilter, newTabItem);

                    // execute ps
                    try {
                        mCurrentLoggedDevice.executeShellCommand("ps", psor); //$NON-NLS-1$
                    } catch (IOException e) {
                        // hmm...
                    }
                }
            }.start();
        }

        return false;
    }

    /**
     * Adds a new filter to the current filter array, and set its colors
     * @param newFilter The filter to add
     */
    private void addFilterToArray(LogFilter newFilter) {
        // set the colors
        newFilter.setColors(mColors);

        // add it to the array.
        if (mFilters != null && mFilters.length > 0) {
            LogFilter[] newFilters = new LogFilter[mFilters.length+1];
            System.arraycopy(mFilters, 0, newFilters, 0, mFilters.length);
            newFilters[mFilters.length] = newFilter;
            mFilters = newFilters;
        } else {
            mFilters = new LogFilter[1];
            mFilters[0] = newFilter;
        }
    }

    private void removeFilterFromArray(LogFilter oldFilter) {
        // look for the index
        int index = -1;
        for (int i = 0 ; i < mFilters.length ; i++) {
            if (mFilters[i] == oldFilter) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            LogFilter[] newFilters = new LogFilter[mFilters.length-1];
            System.arraycopy(mFilters, 0, newFilters, 0, index);
            System.arraycopy(mFilters, index + 1, newFilters, index,
                    newFilters.length-index);
            mFilters = newFilters;
        }
    }

    /**
     * Initialize the filter with already existing buffer.
     * @param filter
     */
    private void initFilter(LogFilter filter) {
        // is it empty
        if (filter.uiReady() == false) {
            return;
        }

        if (filter == mDefaultFilter) {
            initDefaultFilter();
            return;
        }

        filter.clear();

        if (mBufferStart != -1) {
            int max = mBufferEnd;
            if (mBufferEnd < mBufferStart) {
                max += STRING_BUFFER_LENGTH;
            }

            for (int i = mBufferStart; i < max; i++) {
                int realItemIndex = i % STRING_BUFFER_LENGTH;

                filter.addMessage(mBuffer[realItemIndex], null /* old message */);
            }
        }

        filter.flush();
        filter.resetTempFilteringStatus();
    }

    /**
     * Refill the default filter. Not to be called directly.
     * @see initFilter()
     */
    private void initDefaultFilter() {
        mDefaultFilter.clear();

        if (mBufferStart != -1) {
            int max = mBufferEnd;
            if (mBufferEnd < mBufferStart) {
                max += STRING_BUFFER_LENGTH;
            }

            for (int i = mBufferStart; i < max; i++) {
                int realItemIndex = i % STRING_BUFFER_LENGTH;
                LogMessage msg = mBuffer[realItemIndex];

                // first we check that the other filters don't take this message
                boolean filtered = false;
                for (LogFilter f : mFilters) {
                    filtered |= f.accept(msg);
                }

                if (filtered == false) {
                    mDefaultFilter.addMessage(msg, null /* old message */);
                }
            }
        }

        mDefaultFilter.flush();
        mDefaultFilter.resetTempFilteringStatus();
    }

    /**
     * Reset the filters, to handle change in device in automatic filter mode
     */
    private void resetFilters() {
        // if we are in automatic mode, then we need to rmove the current
        // filter.
        if (mFilterMode == FILTER_AUTO_PID || mFilterMode == FILTER_AUTO_TAG) {
            mFilters = null;

            // recreate the filters.
            createFilters();
        }
    }


    private LogFilter getCurrentFilter() {
        int index = mFolders.getSelectionIndex();

        // if mFilters is null or index is invalid, we return the default
        // filter. It doesn't matter if that one is null as well, since we
        // would return null anyway.
        if (index == 0 || mFilters == null) {
            return mDefaultFilter;
        }

        return mFilters[index-1];
    }


    private void emptyTables() {
        for (LogFilter f : mFilters) {
            f.getTable().removeAll();
        }

        if (mDefaultFilter != null) {
            mDefaultFilter.getTable().removeAll();
        }
    }

    protected void updateFilteringWith(String text) {
        synchronized (mBuffer) {
            // reset the temp filtering for all the filters
            for (LogFilter f : mFilters) {
                f.resetTempFiltering();
            }
            if (mDefaultFilter != null) {
                mDefaultFilter.resetTempFiltering();
            }

            // now we need to figure out the new temp filtering
            // split each word
            String[] segments = text.split(" "); //$NON-NLS-1$

            ArrayList<String> keywords = new ArrayList<String>(segments.length);

            // loop and look for temp id/tag
            int tempPid = -1;
            String tempTag = null;
            for (int i = 0 ; i < segments.length; i++) {
                String s = segments[i];
                if (tempPid == -1 && s.startsWith("pid:")) { //$NON-NLS-1$
                    // get the pid
                    String[] seg = s.split(":"); //$NON-NLS-1$
                    if (seg.length == 2) {
                        if (seg[1].matches("^[0-9]*$")) { //$NON-NLS-1$
                            tempPid = Integer.valueOf(seg[1]);
                        }
                    }
                } else if (tempTag == null && s.startsWith("tag:")) { //$NON-NLS-1$
                    String seg[] = segments[i].split(":"); //$NON-NLS-1$
                    if (seg.length == 2) {
                        tempTag = seg[1];
                    }
                } else {
                    keywords.add(s);
                }
            }

            // set the temp filtering in the filters
            if (tempPid != -1 || tempTag != null || keywords.size() > 0) {
                String[] keywordsArray = keywords.toArray(
                        new String[keywords.size()]);

                for (LogFilter f : mFilters) {
                    if (tempPid != -1) {
                        f.setTempPidFiltering(tempPid);
                    }
                    if (tempTag != null) {
                        f.setTempTagFiltering(tempTag);
                    }
                    f.setTempKeywordFiltering(keywordsArray);
                }

                if (mDefaultFilter != null) {
                    if (tempPid != -1) {
                        mDefaultFilter.setTempPidFiltering(tempPid);
                    }
                    if (tempTag != null) {
                        mDefaultFilter.setTempTagFiltering(tempTag);
                    }
                    mDefaultFilter.setTempKeywordFiltering(keywordsArray);

                }
            }

            initFilter(mCurrentFilter);
        }
    }

    /**
     * Called when the current filter selection changes.
     * @param selectedFilter
     */
    private void selectionChanged(LogFilter selectedFilter) {
        if (mLogLevelActions != null) {
            // get the log level
            int level = selectedFilter.getLogLevel();
            for (int i = 0 ; i < mLogLevelActions.length; i++) {
                ICommonAction a = mLogLevelActions[i];
                if (i == level - 2) {
                    a.setChecked(true);
                } else {
                    a.setChecked(false);
                }
            }
        }

        if (mDeleteFilterAction != null) {
            mDeleteFilterAction.setEnabled(selectedFilter.supportsDelete());
        }
        if (mEditFilterAction != null) {
            mEditFilterAction.setEnabled(selectedFilter.supportsEdit());
        }
    }

    public String getSelectedErrorLineMessage() {
        Table table = mCurrentFilter.getTable();
        int[] selection = table.getSelectionIndices();

        if (selection.length == 1) {
            TableItem item = table.getItem(selection[0]);
            LogMessage msg = (LogMessage)item.getData();
            if (msg.data.logLevel == LogLevel.ERROR || msg.data.logLevel == LogLevel.WARN)
                return msg.msg;
        }
        return null;
    }
}
