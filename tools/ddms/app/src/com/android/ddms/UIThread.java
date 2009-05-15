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

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.AllocationPanel;
import com.android.ddmuilib.DevicePanel;
import com.android.ddmuilib.DevicePanel.IUiSelectionListener;
import com.android.ddmuilib.EmulatorControlPanel;
import com.android.ddmuilib.HeapPanel;
import com.android.ddmuilib.ITableFocusListener;
import com.android.ddmuilib.ImageHelper;
import com.android.ddmuilib.ImageLoader;
import com.android.ddmuilib.InfoPanel;
import com.android.ddmuilib.NativeHeapPanel;
import com.android.ddmuilib.ScreenShotDialog;
import com.android.ddmuilib.SysinfoPanel;
import com.android.ddmuilib.TablePanel;
import com.android.ddmuilib.ThreadPanel;
import com.android.ddmuilib.actions.ToolItemAction;
import com.android.ddmuilib.explorer.DeviceExplorer;
import com.android.ddmuilib.log.event.EventLogPanel;
import com.android.ddmuilib.logcat.LogColors;
import com.android.ddmuilib.logcat.LogFilter;
import com.android.ddmuilib.logcat.LogPanel;
import com.android.ddmuilib.logcat.LogPanel.ILogFilterStorageManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import java.io.File;
import java.util.ArrayList;

/**
 * This acts as the UI builder. This cannot be its own thread since this prevent using AWT in an
 * SWT application. So this class mainly builds the ui, and manages communication between the panels
 * when {@link IDevice} / {@link Client} selection changes.
 */
public class UIThread implements IUiSelectionListener {
    /*
     * UI tab panel definitions. The constants here must match up with the array
     * indices in mPanels. PANEL_CLIENT_LIST is a "virtual" panel representing
     * the client list.
     */
    public static final int PANEL_CLIENT_LIST = -1;

    public static final int PANEL_INFO = 0;

    public static final int PANEL_THREAD = 1;

    public static final int PANEL_HEAP = 2;

    public static final int PANEL_NATIVE_HEAP = 3;

    private static final int PANEL_ALLOCATIONS = 4;

    private static final int PANEL_SYSINFO = 5;

    private static final int PANEL_COUNT = 6;

    /** Content is setup in the constructor */
    private static TablePanel[] mPanels = new TablePanel[PANEL_COUNT];

    private static final String[] mPanelNames = new String[] {
            "Info", "Threads", "VM Heap", "Native Heap", "Allocation Tracker", "Sysinfo"
    };

    private static final String[] mPanelTips = new String[] {
            "Client information", "Thread status", "VM heap status",
            "Native heap status", "Allocation Tracker", "Sysinfo graphs"
    };

    private static final String PREFERENCE_LOGSASH =
        "logSashLocation"; //$NON-NLS-1$
    private static final String PREFERENCE_SASH =
        "sashLocation"; //$NON-NLS-1$

    private static final String PREFS_COL_TIME =
        "logcat.time"; //$NON-NLS-1$
    private static final String PREFS_COL_LEVEL =
        "logcat.level"; //$NON-NLS-1$
    private static final String PREFS_COL_PID =
        "logcat.pid"; //$NON-NLS-1$
    private static final String PREFS_COL_TAG =
        "logcat.tag"; //$NON-NLS-1$
    private static final String PREFS_COL_MESSAGE =
        "logcat.message"; //$NON-NLS-1$

    private static final String PREFS_FILTERS = "logcat.filter"; //$NON-NLS-1$

    // singleton instance
    private static UIThread mInstance = new UIThread();

    // our display
    private Display mDisplay;

    // the table we show in the left-hand pane
    private DevicePanel mDevicePanel;

    private IDevice mCurrentDevice = null;
    private Client mCurrentClient = null;

    // status line at the bottom of the app window
    private Label mStatusLine;

    // some toolbar items we need to update
    private ToolItem mTBShowThreadUpdates;
    private ToolItem mTBShowHeapUpdates;
    private ToolItem mTBHalt;
    private ToolItem mTBCauseGc;

    private ImageLoader mDdmsImageLoader;
    private ImageLoader mDdmuiLibImageLoader;

    private final class FilterStorage implements ILogFilterStorageManager {

        public LogFilter[] getFilterFromStore() {
            String filterPrefs = PrefsDialog.getStore().getString(
                    PREFS_FILTERS);

            // split in a string per filter
            String[] filters = filterPrefs.split("\\|"); //$NON-NLS-1$

            ArrayList<LogFilter> list =
                new ArrayList<LogFilter>(filters.length);

            for (String f : filters) {
                if (f.length() > 0) {
                    LogFilter logFilter = new LogFilter();
                    if (logFilter.loadFromString(f)) {
                        list.add(logFilter);
                    }
                }
            }

            return list.toArray(new LogFilter[list.size()]);
        }

        public void saveFilters(LogFilter[] filters) {
            StringBuilder sb = new StringBuilder();
            for (LogFilter f : filters) {
                String filterString = f.toString();
                sb.append(filterString);
                sb.append('|');
            }

            PrefsDialog.getStore().setValue(PREFS_FILTERS, sb.toString());
        }

        public boolean requiresDefaultFilter() {
            return true;
        }
    }


    private LogPanel mLogPanel;

    private ToolItemAction mCreateFilterAction;
    private ToolItemAction mDeleteFilterAction;
    private ToolItemAction mEditFilterAction;
    private ToolItemAction mExportAction;
    private ToolItemAction mClearAction;

    private ToolItemAction[] mLogLevelActions;
    private String[] mLogLevelIcons = {
            "v.png", //$NON-NLS-1S
            "d.png", //$NON-NLS-1S
            "i.png", //$NON-NLS-1S
            "w.png", //$NON-NLS-1S
            "e.png", //$NON-NLS-1S
    };

    protected Clipboard mClipboard;

    private MenuItem mCopyMenuItem;

    private MenuItem mSelectAllMenuItem;

    private TableFocusListener mTableListener;

    private DeviceExplorer mExplorer = null;
    private Shell mExplorerShell = null;

    private EmulatorControlPanel mEmulatorPanel;

    private EventLogPanel mEventLogPanel;

    private class TableFocusListener implements ITableFocusListener {

        private IFocusedTableActivator mCurrentActivator;

        public void focusGained(IFocusedTableActivator activator) {
            mCurrentActivator = activator;
            if (mCopyMenuItem.isDisposed() == false) {
                mCopyMenuItem.setEnabled(true);
                mSelectAllMenuItem.setEnabled(true);
            }
        }

        public void focusLost(IFocusedTableActivator activator) {
            // if we move from one table to another, it's unclear
            // if the old table lose its focus before the new
            // one gets the focus, so we need to check.
            if (activator == mCurrentActivator) {
                activator = null;
                if (mCopyMenuItem.isDisposed() == false) {
                    mCopyMenuItem.setEnabled(false);
                    mSelectAllMenuItem.setEnabled(false);
                }
            }
        }

        public void copy(Clipboard clipboard) {
            if (mCurrentActivator != null) {
                mCurrentActivator.copy(clipboard);
            }
        }

        public void selectAll() {
            if (mCurrentActivator != null) {
                mCurrentActivator.selectAll();
            }
        }

    }


    /**
     * Generic constructor.
     */
    private UIThread() {
        mPanels[PANEL_INFO] = new InfoPanel();
        mPanels[PANEL_THREAD] = new ThreadPanel();
        mPanels[PANEL_HEAP] = new HeapPanel();
        if (PrefsDialog.getStore().getBoolean(PrefsDialog.SHOW_NATIVE_HEAP)) {
            mPanels[PANEL_NATIVE_HEAP] = new NativeHeapPanel();
        } else {
            mPanels[PANEL_NATIVE_HEAP] = null;
        }
        mPanels[PANEL_ALLOCATIONS] = new AllocationPanel();
        mPanels[PANEL_SYSINFO] = new SysinfoPanel();
    }

    /**
     * Get singleton instance of the UI thread.
     */
    public static UIThread getInstance() {
        return mInstance;
    }

    /**
     * Return the Display. Don't try this unless you're in the UI thread.
     */
    public Display getDisplay() {
        return mDisplay;
    }

    public void asyncExec(Runnable r) {
        if (mDisplay != null && mDisplay.isDisposed() == false) {
            mDisplay.asyncExec(r);
        }
    }

    /** returns the IPreferenceStore */
    public IPreferenceStore getStore() {
        return PrefsDialog.getStore();
    }

    /**
     * Create SWT objects and drive the user interface event loop.
     */
    public void runUI() {
        Display.setAppName("ddms");
        mDisplay = new Display();
        Shell shell = new Shell(mDisplay);

        // create the image loaders for DDMS and DDMUILIB
        mDdmsImageLoader = new ImageLoader(this.getClass());
        mDdmuiLibImageLoader = new ImageLoader(DevicePanel.class);

        shell.setImage(ImageHelper.loadImage(mDdmsImageLoader, mDisplay,
                "ddms-icon.png", //$NON-NLS-1$
                100, 50, null));

        Log.setLogOutput(new ILogOutput() {
            public void printAndPromptLog(final LogLevel logLevel, final String tag,
                    final String message) {
                Log.printLog(logLevel, tag, message);
                // dialog box only run in UI thread..
                mDisplay.asyncExec(new Runnable() {
                    public void run() {
                        Shell shell = mDisplay.getActiveShell();
                        if (logLevel == LogLevel.ERROR) {
                            MessageDialog.openError(shell, tag, message);
                        } else {
                            MessageDialog.openWarning(shell, tag, message);
                        }
                    }
                });
            }

            public void printLog(LogLevel logLevel, String tag, String message) {
                Log.printLog(logLevel, tag, message);
            }
        });

        // [try to] ensure ADB is running
        String adbLocation = System.getProperty("com.android.ddms.bindir"); //$NON-NLS-1$
        if (adbLocation != null && adbLocation.length() != 0) {
            adbLocation += File.separator + "adb"; //$NON-NLS-1$
        } else {
            adbLocation = "adb"; //$NON-NLS-1$
        }

        AndroidDebugBridge.init(true /* debugger support */);
        AndroidDebugBridge.createBridge(adbLocation, true /* forceNewBridge */);

        shell.setText("Dalvik Debug Monitor");
        setConfirmClose(shell);
        createMenus(shell);
        createWidgets(shell);

        shell.pack();
        setSizeAndPosition(shell);
        shell.open();

        Log.d("ddms", "UI is up");

        while (!shell.isDisposed()) {
            if (!mDisplay.readAndDispatch())
                mDisplay.sleep();
        }
        mLogPanel.stopLogCat(true);

        mDevicePanel.dispose();
        for (TablePanel panel : mPanels) {
            if (panel != null) {
                panel.dispose();
            }
        }

        mDisplay.dispose();
        Log.d("ddms", "UI is down");
    }

    /**
     * Set the size and position of the main window from the preference, and
     * setup listeners for control events (resize/move of the window)
     */
    private void setSizeAndPosition(final Shell shell) {
        shell.setMinimumSize(400, 200);

        // get the x/y and w/h from the prefs
        PreferenceStore prefs = PrefsDialog.getStore();
        int x = prefs.getInt(PrefsDialog.SHELL_X);
        int y = prefs.getInt(PrefsDialog.SHELL_Y);
        int w = prefs.getInt(PrefsDialog.SHELL_WIDTH);
        int h = prefs.getInt(PrefsDialog.SHELL_HEIGHT);

        // check that we're not out of the display area
        Rectangle rect = mDisplay.getClientArea();
        // first check the width/height
        if (w > rect.width) {
            w = rect.width;
            prefs.setValue(PrefsDialog.SHELL_WIDTH, rect.width);
        }
        if (h > rect.height) {
            h = rect.height;
            prefs.setValue(PrefsDialog.SHELL_HEIGHT, rect.height);
        }
        // then check x. Make sure the left corner is in the screen
        if (x < rect.x) {
            x = rect.x;
            prefs.setValue(PrefsDialog.SHELL_X, rect.x);
        } else if (x >= rect.x + rect.width) {
            x = rect.x + rect.width - w;
            prefs.setValue(PrefsDialog.SHELL_X, rect.x);
        }
        // then check y. Make sure the left corner is in the screen
        if (y < rect.y) {
            y = rect.y;
            prefs.setValue(PrefsDialog.SHELL_Y, rect.y);
        } else if (y >= rect.y + rect.height) {
            y = rect.y + rect.height - h;
            prefs.setValue(PrefsDialog.SHELL_Y, rect.y);
        }

        // now we can set the location/size
        shell.setBounds(x, y, w, h);

        // add listener for resize/move
        shell.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {
                // get the new x/y
                Rectangle rect = shell.getBounds();
                // store in pref file
                PreferenceStore prefs = PrefsDialog.getStore();
                prefs.setValue(PrefsDialog.SHELL_X, rect.x);
                prefs.setValue(PrefsDialog.SHELL_Y, rect.y);
            }

            public void controlResized(ControlEvent e) {
                // get the new w/h
                Rectangle rect = shell.getBounds();
                // store in pref file
                PreferenceStore prefs = PrefsDialog.getStore();
                prefs.setValue(PrefsDialog.SHELL_WIDTH, rect.width);
                prefs.setValue(PrefsDialog.SHELL_HEIGHT, rect.height);
            }
        });
    }

    /**
     * Set the size and position of the file explorer window from the
     * preference, and setup listeners for control events (resize/move of
     * the window)
     */
    private void setExplorerSizeAndPosition(final Shell shell) {
        shell.setMinimumSize(400, 200);

        // get the x/y and w/h from the prefs
        PreferenceStore prefs = PrefsDialog.getStore();
        int x = prefs.getInt(PrefsDialog.EXPLORER_SHELL_X);
        int y = prefs.getInt(PrefsDialog.EXPLORER_SHELL_Y);
        int w = prefs.getInt(PrefsDialog.EXPLORER_SHELL_WIDTH);
        int h = prefs.getInt(PrefsDialog.EXPLORER_SHELL_HEIGHT);

        // check that we're not out of the display area
        Rectangle rect = mDisplay.getClientArea();
        // first check the width/height
        if (w > rect.width) {
            w = rect.width;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_WIDTH, rect.width);
        }
        if (h > rect.height) {
            h = rect.height;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_HEIGHT, rect.height);
        }
        // then check x. Make sure the left corner is in the screen
        if (x < rect.x) {
            x = rect.x;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_X, rect.x);
        } else if (x >= rect.x + rect.width) {
            x = rect.x + rect.width - w;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_X, rect.x);
        }
        // then check y. Make sure the left corner is in the screen
        if (y < rect.y) {
            y = rect.y;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_Y, rect.y);
        } else if (y >= rect.y + rect.height) {
            y = rect.y + rect.height - h;
            prefs.setValue(PrefsDialog.EXPLORER_SHELL_Y, rect.y);
        }

        // now we can set the location/size
        shell.setBounds(x, y, w, h);

        // add listener for resize/move
        shell.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {
                // get the new x/y
                Rectangle rect = shell.getBounds();
                // store in pref file
                PreferenceStore prefs = PrefsDialog.getStore();
                prefs.setValue(PrefsDialog.EXPLORER_SHELL_X, rect.x);
                prefs.setValue(PrefsDialog.EXPLORER_SHELL_Y, rect.y);
            }

            public void controlResized(ControlEvent e) {
                // get the new w/h
                Rectangle rect = shell.getBounds();
                // store in pref file
                PreferenceStore prefs = PrefsDialog.getStore();
                prefs.setValue(PrefsDialog.EXPLORER_SHELL_WIDTH, rect.width);
                prefs.setValue(PrefsDialog.EXPLORER_SHELL_HEIGHT, rect.height);
            }
        });
    }

    /*
     * Set the confirm-before-close dialog. TODO: enable/disable in prefs. TODO:
     * is there any point in having this?
     */
    private void setConfirmClose(final Shell shell) {
        if (true)
            return;

        shell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
                MessageBox msgBox = new MessageBox(shell, style);
                msgBox.setText("Confirm...");
                msgBox.setMessage("Close DDM?");
                event.doit = (msgBox.open() == SWT.YES);
            }
        });
    }

    /*
     * Create the menu bar and items.
     */
    private void createMenus(final Shell shell) {
        // create menu bar
        Menu menuBar = new Menu(shell, SWT.BAR);

        // create top-level items
        MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
        fileItem.setText("&File");
        MenuItem editItem = new MenuItem(menuBar, SWT.CASCADE);
        editItem.setText("&Edit");
        MenuItem actionItem = new MenuItem(menuBar, SWT.CASCADE);
        actionItem.setText("&Actions");
        MenuItem deviceItem = new MenuItem(menuBar, SWT.CASCADE);
        deviceItem.setText("&Device");
        MenuItem helpItem = new MenuItem(menuBar, SWT.CASCADE);
        helpItem.setText("&Help");

        // create top-level menus
        Menu fileMenu = new Menu(menuBar);
        fileItem.setMenu(fileMenu);
        Menu editMenu = new Menu(menuBar);
        editItem.setMenu(editMenu);
        Menu actionMenu = new Menu(menuBar);
        actionItem.setMenu(actionMenu);
        Menu deviceMenu = new Menu(menuBar);
        deviceItem.setMenu(deviceMenu);
        Menu helpMenu = new Menu(menuBar);
        helpItem.setMenu(helpMenu);

        MenuItem item;

        // create File menu items
        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Preferences...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PrefsDialog.run(shell);
            }
        });

        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("&Static Port Configuration...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StaticPortConfigDialog dlg = new StaticPortConfigDialog(shell);
                dlg.open();
            }
        });

        new MenuItem(fileMenu, SWT.SEPARATOR);

        item = new MenuItem(fileMenu, SWT.NONE);
        item.setText("E&xit\tCtrl-Q");
        item.setAccelerator('Q' | SWT.CONTROL);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });

        // create edit menu items
        mCopyMenuItem = new MenuItem(editMenu, SWT.NONE);
        mCopyMenuItem.setText("&Copy\tCtrl-C");
        mCopyMenuItem.setAccelerator('C' | SWT.COMMAND);
        mCopyMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.copy(mClipboard);
            }
        });

        new MenuItem(editMenu, SWT.SEPARATOR);

        mSelectAllMenuItem = new MenuItem(editMenu, SWT.NONE);
        mSelectAllMenuItem.setText("Select &All\tCtrl-A");
        mSelectAllMenuItem.setAccelerator('A' | SWT.COMMAND);
        mSelectAllMenuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mTableListener.selectAll();
            }
        });

        // create Action menu items
        // TODO: this should come with a confirmation dialog
        final MenuItem actionHaltItem = new MenuItem(actionMenu, SWT.NONE);
        actionHaltItem.setText("&Halt VM");
        actionHaltItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mDevicePanel.killSelectedClient();
            }
        });

        final MenuItem actionCauseGcItem = new MenuItem(actionMenu, SWT.NONE);
        actionCauseGcItem.setText("Cause &GC");
        actionCauseGcItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mDevicePanel.forceGcOnSelectedClient();
            }
        });

        // configure Action items based on current state
        actionMenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                actionHaltItem.setEnabled(mTBHalt.getEnabled());
                actionCauseGcItem.setEnabled(mTBCauseGc.getEnabled());
            }
        });

        // create Device menu items
        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("&Screen capture...\tCTrl-S");
        item.setAccelerator('S' | SWT.CONTROL);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentDevice != null) {
                    ScreenShotDialog dlg = new ScreenShotDialog(shell);
                    dlg.open(mCurrentDevice);
                }
            }
        });

        new MenuItem(deviceMenu, SWT.SEPARATOR);

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("File Explorer...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createFileExplorer();
            }
        });

        new MenuItem(deviceMenu, SWT.SEPARATOR);

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("Show &process status...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCommandDialog dlg;
                dlg = new DeviceCommandDialog("ps -x", "ps-x.txt", shell);
                dlg.open(mCurrentDevice);
            }
        });

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("Dump &device state...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCommandDialog dlg;
                dlg = new DeviceCommandDialog("/system/bin/dumpstate /proc/self/fd/0",
                        "device-state.txt", shell);
                dlg.open(mCurrentDevice);
            }
        });

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("Dump &app state...");
        item.setEnabled(false);
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCommandDialog dlg;
                dlg = new DeviceCommandDialog("dumpsys", "app-state.txt", shell);
                dlg.open(mCurrentDevice);
            }
        });

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("Dump &radio state...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCommandDialog dlg;
                dlg = new DeviceCommandDialog(
                        "cat /data/logs/radio.4 /data/logs/radio.3"
                        + " /data/logs/radio.2 /data/logs/radio.1"
                        + " /data/logs/radio",
                        "radio-state.txt", shell);
                dlg.open(mCurrentDevice);
            }
        });

        item = new MenuItem(deviceMenu, SWT.NONE);
        item.setText("Run &logcat...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DeviceCommandDialog dlg;
                dlg = new DeviceCommandDialog("logcat '*:d jdwp:w'", "log.txt",
                        shell);
                dlg.open(mCurrentDevice);
            }
        });

        // create Help menu items
        item = new MenuItem(helpMenu, SWT.NONE);
        item.setText("&Contents...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int style = SWT.APPLICATION_MODAL | SWT.OK;
                MessageBox msgBox = new MessageBox(shell, style);
                msgBox.setText("Help!");
                msgBox.setMessage("Help wanted.");
                msgBox.open();
            }
        });

        new MenuItem(helpMenu, SWT.SEPARATOR);

        item = new MenuItem(helpMenu, SWT.NONE);
        item.setText("&About...");
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                AboutDialog dlg = new AboutDialog(shell);
                dlg.open();
            }
        });

        // tell the shell to use this menu
        shell.setMenuBar(menuBar);
    }

    /*
     * Create the widgets in the main application window. The basic layout is a
     * two-panel sash, with a scrolling list of VMs on the left and detailed
     * output for a single VM on the right.
     */
    private void createWidgets(final Shell shell) {
        Color darkGray = shell.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY);

        /*
         * Create three areas: tool bar, split panels, status line
         */
        shell.setLayout(new GridLayout(1, false));

        // 1. panel area
        final Composite panelArea = new Composite(shell, SWT.BORDER);

        // make the panel area absorb all space
        panelArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        // 2. status line.
        mStatusLine = new Label(shell, SWT.NONE);

        // make status line extend all the way across
        mStatusLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mStatusLine.setText("Initializing...");

        /*
         * Configure the split-panel area.
         */
        final PreferenceStore prefs = PrefsDialog.getStore();

        Composite topPanel = new Composite(panelArea, SWT.NONE);
        final Sash sash = new Sash(panelArea, SWT.HORIZONTAL);
        sash.setBackground(darkGray);
        Composite bottomPanel = new Composite(panelArea, SWT.NONE);

        panelArea.setLayout(new FormLayout());

        createTopPanel(topPanel, darkGray);
        createBottomPanel(bottomPanel);

        // form layout data
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(sash, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        topPanel.setLayoutData(data);

        final FormData sashData = new FormData();
        if (prefs != null && prefs.contains(PREFERENCE_LOGSASH)) {
            sashData.top = new FormAttachment(0, prefs.getInt(
                    PREFERENCE_LOGSASH));
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
        bottomPanel.setLayoutData(data);

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = panelArea.getClientArea();
                int bottom = panelRect.height - sashRect.height - 100;
                e.y = Math.max(Math.min(e.y, bottom), 100);
                if (e.y != sashRect.y) {
                    sashData.top = new FormAttachment(0, e.y);
                    prefs.setValue(PREFERENCE_LOGSASH, e.y);
                    panelArea.layout();
                }
            }
        });

        // add a global focus listener for all the tables
        mTableListener = new TableFocusListener();

        // now set up the listener in the various panels
        mLogPanel.setTableFocusListener(mTableListener);
        mEventLogPanel.setTableFocusListener(mTableListener);
        for (TablePanel p : mPanels) {
            if (p != null) {
                p.setTableFocusListener(mTableListener);
            }
        }

        mStatusLine.setText("");
    }

    /*
     * Populate the tool bar.
     */
    private void createDevicePanelToolBar(ToolBar toolBar) {
        Display display = toolBar.getDisplay();

        // add "show thread updates" button
        mTBShowThreadUpdates = new ToolItem(toolBar, SWT.CHECK);
        mTBShowThreadUpdates.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, display,
                DevicePanel.ICON_THREAD, DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mTBShowThreadUpdates.setToolTipText("Show thread updates");
        mTBShowThreadUpdates.setEnabled(false);
        mTBShowThreadUpdates.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentClient != null) {
                    // boolean status = ((ToolItem)e.item).getSelection();
                    // invert previous state
                    boolean enable = !mCurrentClient.isThreadUpdateEnabled();

                    mCurrentClient.setThreadUpdateEnabled(enable);
                } else {
                    e.doit = false; // this has no effect?
                }
            }
        });

        // add "show heap updates" button
        mTBShowHeapUpdates = new ToolItem(toolBar, SWT.CHECK);
        mTBShowHeapUpdates.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, display,
                DevicePanel.ICON_HEAP, DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mTBShowHeapUpdates.setToolTipText("Show heap updates");
        mTBShowHeapUpdates.setEnabled(false);
        mTBShowHeapUpdates.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (mCurrentClient != null) {
                    // boolean status = ((ToolItem)e.item).getSelection();
                    // invert previous state
                    boolean enable = !mCurrentClient.isHeapUpdateEnabled();
                    mCurrentClient.setHeapUpdateEnabled(enable);
                } else {
                    e.doit = false; // this has no effect?
                }
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR);

        // add "kill VM" button; need to make this visually distinct from
        // the status update buttons
        mTBHalt = new ToolItem(toolBar, SWT.PUSH);
        mTBHalt.setToolTipText("Halt the target VM");
        mTBHalt.setEnabled(false);
        mTBHalt.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, display,
                DevicePanel.ICON_HALT, DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mTBHalt.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mDevicePanel.killSelectedClient();
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR);

        // add "cause GC" button
        mTBCauseGc = new ToolItem(toolBar, SWT.PUSH);
        mTBCauseGc.setToolTipText("Cause an immediate GC in the target VM");
        mTBCauseGc.setEnabled(false);
        mTBCauseGc.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, display,
                DevicePanel.ICON_GC, DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mTBCauseGc.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mDevicePanel.forceGcOnSelectedClient();
            }
        });

       toolBar.pack();
    }

    private void createTopPanel(final Composite comp, Color darkGray) {
        final PreferenceStore prefs = PrefsDialog.getStore();

        comp.setLayout(new FormLayout());

        Composite leftPanel = new Composite(comp, SWT.NONE);
        final Sash sash = new Sash(comp, SWT.VERTICAL);
        sash.setBackground(darkGray);
        Composite rightPanel = new Composite(comp, SWT.NONE);

        createLeftPanel(leftPanel);
        createRightPanel(rightPanel);

        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(sash, 0);
        leftPanel.setLayoutData(data);

        final FormData sashData = new FormData();
        sashData.top = new FormAttachment(0, 0);
        sashData.bottom = new FormAttachment(100, 0);
        if (prefs != null && prefs.contains(PREFERENCE_SASH)) {
            sashData.left = new FormAttachment(0, prefs.getInt(
                    PREFERENCE_SASH));
        } else {
            // position the sash 380 from the right instead of x% (done by using
            // FormAttachment(x, 0)) in order to keep the sash at the same
            // position
            // from the left when the window is resized.
            // 380px is just enough to display the left table with no horizontal
            // scrollbar with the default font.
            sashData.left = new FormAttachment(0, 380);
        }
        sash.setLayoutData(sashData);

        data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.bottom = new FormAttachment(100, 0);
        data.left = new FormAttachment(sash, 0);
        data.right = new FormAttachment(100, 0);
        rightPanel.setLayoutData(data);

        final int minPanelWidth = 60;

        // allow resizes, but cap at minPanelWidth
        sash.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
                Rectangle sashRect = sash.getBounds();
                Rectangle panelRect = comp.getClientArea();
                int right = panelRect.width - sashRect.width - minPanelWidth;
                e.x = Math.max(Math.min(e.x, right), minPanelWidth);
                if (e.x != sashRect.x) {
                    sashData.left = new FormAttachment(0, e.x);
                    prefs.setValue(PREFERENCE_SASH, e.x);
                    comp.layout();
                }
            }
        });
    }

    private void createBottomPanel(final Composite comp) {
        final PreferenceStore prefs = PrefsDialog.getStore();

        // create clipboard
        Display display = comp.getDisplay();
        mClipboard = new Clipboard(display);

        LogColors colors = new LogColors();

        colors.infoColor = new Color(display, 0, 127, 0);
        colors.debugColor = new Color(display, 0, 0, 127);
        colors.errorColor = new Color(display, 255, 0, 0);
        colors.warningColor = new Color(display, 255, 127, 0);
        colors.verboseColor = new Color(display, 0, 0, 0);

        // set the preferences names
        LogPanel.PREFS_TIME = PREFS_COL_TIME;
        LogPanel.PREFS_LEVEL = PREFS_COL_LEVEL;
        LogPanel.PREFS_PID = PREFS_COL_PID;
        LogPanel.PREFS_TAG = PREFS_COL_TAG;
        LogPanel.PREFS_MESSAGE = PREFS_COL_MESSAGE;

        comp.setLayout(new GridLayout(1, false));

        ToolBar toolBar = new ToolBar(comp, SWT.HORIZONTAL);

        mCreateFilterAction = new ToolItemAction(toolBar, SWT.PUSH);
        mCreateFilterAction.item.setToolTipText("Create Filter");
        mCreateFilterAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                "add.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mCreateFilterAction.item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogPanel.addFilter();
            }
        });

        mEditFilterAction = new ToolItemAction(toolBar, SWT.PUSH);
        mEditFilterAction.item.setToolTipText("Edit Filter");
        mEditFilterAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                "edit.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mEditFilterAction.item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogPanel.editFilter();
            }
        });

        mDeleteFilterAction = new ToolItemAction(toolBar, SWT.PUSH);
        mDeleteFilterAction.item.setToolTipText("Delete Filter");
        mDeleteFilterAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                "delete.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mDeleteFilterAction.item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogPanel.deleteFilter();
            }
        });


        new ToolItem(toolBar, SWT.SEPARATOR);

        LogLevel[] levels = LogLevel.values();
        mLogLevelActions = new ToolItemAction[mLogLevelIcons.length];
        for (int i = 0 ; i < mLogLevelActions.length; i++) {
            String name = levels[i].getStringValue();
            final ToolItemAction newAction = new ToolItemAction(toolBar, SWT.CHECK);
            mLogLevelActions[i] = newAction;
            //newAction.item.setText(name);
            newAction.item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    // disable the other actions and record current index
                    for (int i = 0 ; i < mLogLevelActions.length; i++) {
                        ToolItemAction a = mLogLevelActions[i];
                        if (a == newAction) {
                            a.setChecked(true);

                            // set the log level
                            mLogPanel.setCurrentFilterLogLevel(i+2);
                        } else {
                            a.setChecked(false);
                        }
                    }
                }
            });

            newAction.item.setToolTipText(name);
            newAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                    mLogLevelIcons[i],
                    DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        }

        new ToolItem(toolBar, SWT.SEPARATOR);

        mClearAction = new ToolItemAction(toolBar, SWT.PUSH);
        mClearAction.item.setToolTipText("Clear Log");

        mClearAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                "clear.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mClearAction.item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogPanel.clear();
            }
        });

        new ToolItem(toolBar, SWT.SEPARATOR);

        mExportAction = new ToolItemAction(toolBar, SWT.PUSH);
        mExportAction.item.setToolTipText("Export Selection As Text...");
        mExportAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, mDisplay,
                "save.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));
        mExportAction.item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mLogPanel.save();
            }
        });


        toolBar.pack();

        // now create the log view
        mLogPanel = new LogPanel(new ImageLoader(LogPanel.class), colors, new FilterStorage(),
                LogPanel.FILTER_MANUAL);

        mLogPanel.setActions(mDeleteFilterAction, mEditFilterAction, mLogLevelActions);

        String colMode = prefs.getString(PrefsDialog.LOGCAT_COLUMN_MODE);
        if (PrefsDialog.LOGCAT_COLUMN_MODE_AUTO.equals(colMode)) {
            mLogPanel.setColumnMode(LogPanel.COLUMN_MODE_AUTO);
        }

        String fontStr = PrefsDialog.getStore().getString(PrefsDialog.LOGCAT_FONT);
        if (fontStr != null) {
            try {
                FontData fdat = new FontData(fontStr);
                mLogPanel.setFont(new Font(display, fdat));
            } catch (IllegalArgumentException e) {
                // Looks like fontStr isn't a valid font representation.
                // We do nothing in this case, the logcat view will use the default font.
            } catch (SWTError e2) {
                // Looks like the Font() constructor failed.
                // We do nothing in this case, the logcat view will use the default font.
            }
        }

        mLogPanel.createPanel(comp);

        // and start the logcat
        mLogPanel.startLogCat(mCurrentDevice);
    }

    /*
     * Create the contents of the left panel: a table of VMs.
     */
    private void createLeftPanel(final Composite comp) {
        comp.setLayout(new GridLayout(1, false));
        ToolBar toolBar = new ToolBar(comp, SWT.HORIZONTAL | SWT.RIGHT | SWT.WRAP);
        toolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createDevicePanelToolBar(toolBar);

        Composite c = new Composite(comp, SWT.NONE);
        c.setLayoutData(new GridData(GridData.FILL_BOTH));

        mDevicePanel = new DevicePanel(new ImageLoader(DevicePanel.class), true /* showPorts */);
        mDevicePanel.createPanel(c);

        // add ourselves to the device panel selection listener
        mDevicePanel.addSelectionListener(this);
    }

    /*
     * Create the contents of the right panel: tabs with VM information.
     */
    private void createRightPanel(final Composite comp) {
        TabItem item;
        TabFolder tabFolder;

        comp.setLayout(new FillLayout());

        tabFolder = new TabFolder(comp, SWT.NONE);

        for (int i = 0; i < mPanels.length; i++) {
            if (mPanels[i] != null) {
                item = new TabItem(tabFolder, SWT.NONE);
                item.setText(mPanelNames[i]);
                item.setToolTipText(mPanelTips[i]);
                item.setControl(mPanels[i].createPanel(tabFolder));
            }
        }

        // add the emulator control panel to the folders.
        item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Emulator Control");
        item.setToolTipText("Emulator Control Panel");
        mEmulatorPanel = new EmulatorControlPanel(mDdmuiLibImageLoader);
        item.setControl(mEmulatorPanel.createPanel(tabFolder));

        // add the event log panel to the folders.
        item = new TabItem(tabFolder, SWT.NONE);
        item.setText("Event Log");
        item.setToolTipText("Event Log");

        // create the composite that will hold the toolbar and the event log panel.
        Composite eventLogTopComposite = new Composite(tabFolder, SWT.NONE);
        item.setControl(eventLogTopComposite);
        eventLogTopComposite.setLayout(new GridLayout(1, false));

        // create the toolbar and the actions
        ToolBar toolbar = new ToolBar(eventLogTopComposite, SWT.HORIZONTAL);
        toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolItemAction optionsAction = new ToolItemAction(toolbar, SWT.PUSH);
        optionsAction.item.setToolTipText("Opens the options panel");
        optionsAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, comp.getDisplay(),
                "edit.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));

        ToolItemAction clearAction = new ToolItemAction(toolbar, SWT.PUSH);
        clearAction.item.setToolTipText("Clears the event log");
        clearAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, comp.getDisplay(),
                "clear.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));

        new ToolItem(toolbar, SWT.SEPARATOR);

        ToolItemAction saveAction = new ToolItemAction(toolbar, SWT.PUSH);
        saveAction.item.setToolTipText("Saves the event log");
        saveAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, comp.getDisplay(),
                "save.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));

        ToolItemAction loadAction = new ToolItemAction(toolbar, SWT.PUSH);
        loadAction.item.setToolTipText("Loads an event log");
        loadAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, comp.getDisplay(),
                "load.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));

        ToolItemAction importBugAction = new ToolItemAction(toolbar, SWT.PUSH);
        importBugAction.item.setToolTipText("Imports a bug report");
        importBugAction.item.setImage(ImageHelper.loadImage(mDdmuiLibImageLoader, comp.getDisplay(),
                "importBug.png", //$NON-NLS-1$
                DevicePanel.ICON_WIDTH, DevicePanel.ICON_WIDTH, null));

        // create the event log panel
        mEventLogPanel = new EventLogPanel(mDdmuiLibImageLoader);

        // set the external actions
        mEventLogPanel.setActions(optionsAction, clearAction, saveAction, loadAction,
                importBugAction);

        // create the panel
        mEventLogPanel.createPanel(eventLogTopComposite);
    }

    private void createFileExplorer() {
        if (mExplorer == null) {
            mExplorerShell = new Shell(mDisplay);

            // create the ui
            mExplorerShell.setLayout(new GridLayout(1, false));

            // toolbar + action
            ToolBar toolBar = new ToolBar(mExplorerShell, SWT.HORIZONTAL);
            toolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            ToolItemAction pullAction = new ToolItemAction(toolBar, SWT.PUSH);
            pullAction.item.setToolTipText("Pull File from Device");
            pullAction.item.setImage(mDdmuiLibImageLoader.loadImage("pull.png", mDisplay)); //$NON-NLS-1$

            ToolItemAction pushAction = new ToolItemAction(toolBar, SWT.PUSH);
            pushAction.item.setToolTipText("Push file onto Device");
            pushAction.item.setImage(mDdmuiLibImageLoader.loadImage("push.png", mDisplay)); //$NON-NLS-1$

            ToolItemAction deleteAction = new ToolItemAction(toolBar, SWT.PUSH);
            deleteAction.item.setToolTipText("Delete");
            deleteAction.item.setImage(mDdmuiLibImageLoader.loadImage("delete.png", mDisplay)); //$NON-NLS-1$

            // device explorer
            mExplorer = new DeviceExplorer();

            mExplorer.setImages(mDdmuiLibImageLoader.loadImage("file.png", mDisplay), //$NON-NLS-1$
                    mDdmuiLibImageLoader.loadImage("folder.png", mDisplay), //$NON-NLS-1$
                    mDdmuiLibImageLoader.loadImage("android.png", mDisplay), //$NON-NLS-1$
                    null);
            mExplorer.setActions(pushAction, pullAction, deleteAction);

            pullAction.item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mExplorer.pullSelection();
                }
            });
            pullAction.setEnabled(false);

            pushAction.item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mExplorer.pushIntoSelection();
                }
            });
            pushAction.setEnabled(false);

            deleteAction.item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mExplorer.deleteSelection();
                }
            });
            deleteAction.setEnabled(false);

            Composite parent = new Composite(mExplorerShell, SWT.NONE);
            parent.setLayoutData(new GridData(GridData.FILL_BOTH));

            mExplorer.createPanel(parent);
            mExplorer.switchDevice(mCurrentDevice);

            mExplorerShell.addShellListener(new ShellListener() {
                public void shellActivated(ShellEvent e) {
                    // pass
                }

                public void shellClosed(ShellEvent e) {
                    mExplorer = null;
                    mExplorerShell = null;
                }

                public void shellDeactivated(ShellEvent e) {
                    // pass
                }

                public void shellDeiconified(ShellEvent e) {
                    // pass
                }

                public void shellIconified(ShellEvent e) {
                    // pass
                }
            });

            mExplorerShell.pack();
            setExplorerSizeAndPosition(mExplorerShell);
            mExplorerShell.open();
        } else {
            if (mExplorerShell != null) {
                mExplorerShell.forceActive();
            }
        }
    }

    /**
     * Set the status line. TODO: make this a stack, so we can safely have
     * multiple things trying to set it all at once. Also specify an expiration?
     */
    public void setStatusLine(final String str) {
        try {
            mDisplay.asyncExec(new Runnable() {
                public void run() {
                    doSetStatusLine(str);
                }
            });
        } catch (SWTException swte) {
            if (!mDisplay.isDisposed())
                throw swte;
        }
    }

    private void doSetStatusLine(String str) {
        if (mStatusLine.isDisposed())
            return;

        if (!mStatusLine.getText().equals(str)) {
            mStatusLine.setText(str);

            // try { Thread.sleep(100); }
            // catch (InterruptedException ie) {}
        }
    }

    public void displayError(final String msg) {
        try {
            mDisplay.syncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(mDisplay.getActiveShell(), "Error",
                            msg);
                }
            });
        } catch (SWTException swte) {
            if (!mDisplay.isDisposed())
                throw swte;
        }
    }

    private void enableButtons() {
        if (mCurrentClient != null) {
            mTBShowThreadUpdates.setSelection(mCurrentClient.isThreadUpdateEnabled());
            mTBShowThreadUpdates.setEnabled(true);
            mTBShowHeapUpdates.setSelection(mCurrentClient.isHeapUpdateEnabled());
            mTBShowHeapUpdates.setEnabled(true);
            mTBHalt.setEnabled(true);
            mTBCauseGc.setEnabled(true);
        } else {
            // list is empty, disable these
            mTBShowThreadUpdates.setSelection(false);
            mTBShowThreadUpdates.setEnabled(false);
            mTBShowHeapUpdates.setSelection(false);
            mTBShowHeapUpdates.setEnabled(false);
            mTBHalt.setEnabled(false);
            mTBCauseGc.setEnabled(false);
        }
    }

    /**
     * Sent when a new {@link IDevice} and {@link Client} are selected.
     * @param selectedDevice the selected device. If null, no devices are selected.
     * @param selectedClient The selected client. If null, no clients are selected.
     *
     * @see IUiSelectionListener
     */
    public void selectionChanged(IDevice selectedDevice, Client selectedClient) {
        if (mCurrentDevice != selectedDevice) {
            mCurrentDevice = selectedDevice;
            for (TablePanel panel : mPanels) {
                if (panel != null) {
                    panel.deviceSelected(mCurrentDevice);
                }
            }

            mEmulatorPanel.deviceSelected(mCurrentDevice);
            mLogPanel.deviceSelected(mCurrentDevice);
            if (mEventLogPanel != null) {
                mEventLogPanel.deviceSelected(mCurrentDevice);
            }

            if (mExplorer != null) {
                mExplorer.switchDevice(mCurrentDevice);
            }
        }

        if (mCurrentClient != selectedClient) {
            AndroidDebugBridge.getBridge().setSelectedClient(selectedClient);
            mCurrentClient = selectedClient;
            for (TablePanel panel : mPanels) {
                if (panel != null) {
                    panel.clientSelected(mCurrentClient);
                }
            }

            enableButtons();
        }
    }
}
