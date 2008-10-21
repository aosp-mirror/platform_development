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

package com.android.ide.eclipse.ddms.views;

import com.android.ide.eclipse.ddms.CommonAction;
import com.android.ide.eclipse.ddms.DdmsPlugin;
import com.android.ide.eclipse.ddms.ImageLoader;
import com.android.ide.eclipse.ddms.preferences.PreferenceInitializer;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmuilib.logcat.LogColors;
import com.android.ddmuilib.logcat.LogFilter;
import com.android.ddmuilib.logcat.LogPanel;
import com.android.ddmuilib.logcat.LogPanel.ILogFilterStorageManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;

import java.util.ArrayList;

/**
 * The log cat view displays log output from the current device selection.
 *
 */
public final class LogCatView extends SelectionDependentViewPart {

    public static final String ID =
        "com.android.ide.eclipse.ddms.views.LogCatView"; // $NON-NLS-1$

    private static final String PREFS_COL_TIME =
        DdmsPlugin.PLUGIN_ID + ".logcat.time"; // $NON-NLS-1$
    private static final String PREFS_COL_LEVEL =
        DdmsPlugin.PLUGIN_ID + ".logcat.level"; // $NON-NLS-1$
    private static final String PREFS_COL_PID =
        DdmsPlugin.PLUGIN_ID + ".logcat.pid"; // $NON-NLS-1$
    private static final String PREFS_COL_TAG =
        DdmsPlugin.PLUGIN_ID + ".logcat.tag"; // $NON-NLS-1$
    private static final String PREFS_COL_MESSAGE =
        DdmsPlugin.PLUGIN_ID + ".logcat.message"; // $NON-NLS-1$

    private static final String PREFS_FILTERS =
        DdmsPlugin.PLUGIN_ID + ".logcat.filters"; // $NON-NLS-1$

    private static LogCatView sThis;
    private LogPanel mLogPanel;

    private CommonAction mCreateFilterAction;
    private CommonAction mDeleteFilterAction;
    private CommonAction mEditFilterAction;
    private CommonAction mExportAction;

    private CommonAction[] mLogLevelActions;
    private String[] mLogLevelIcons = {
            "v.png", //$NON-NLS-1S
            "d.png", //$NON-NLS-1S
            "i.png", //$NON-NLS-1S
            "w.png", //$NON-NLS-1S
            "e.png", //$NON-NLS-1S
    };

    private Action mClearAction;

    private Clipboard mClipboard;

    /**
     * An implementation of {@link ILogFilterStorageManager} to bridge to the eclipse preference
     * store, and saves the log filters.
     */
    private final class FilterStorage implements ILogFilterStorageManager {

        public LogFilter[] getFilterFromStore() {
            String filterPrefs = DdmsPlugin.getDefault().getPreferenceStore().getString(
                    PREFS_FILTERS);

            // split in a string per filter
            String[] filters = filterPrefs.split("\\|"); // $NON-NLS-1$

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

            DdmsPlugin.getDefault().getPreferenceStore().setValue(PREFS_FILTERS, sb.toString());
        }

        public boolean requiresDefaultFilter() {
            return true;
        }
    }

    public LogCatView() {
        sThis = this;
        LogPanel.PREFS_TIME = PREFS_COL_TIME;
        LogPanel.PREFS_LEVEL = PREFS_COL_LEVEL;
        LogPanel.PREFS_PID = PREFS_COL_PID;
        LogPanel.PREFS_TAG = PREFS_COL_TAG;
        LogPanel.PREFS_MESSAGE = PREFS_COL_MESSAGE;
    }

    /**
     * Returns the singleton instance.
     */
    public static LogCatView getInstance() {
        return sThis;
    }

    /**
     * Sets the display font.
     * @param font The font.
     */
    public static void setFont(Font font) {
        if (sThis != null && sThis.mLogPanel != null) {
            sThis.mLogPanel.setFont(font);
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        Display d = parent.getDisplay();
        LogColors colors = new LogColors();

        ImageLoader loader = DdmsPlugin.getImageLoader();

        colors.infoColor = new Color(d, 0, 127, 0);
        colors.debugColor = new Color(d, 0, 0, 127);
        colors.errorColor = new Color(d, 255, 0, 0);
        colors.warningColor = new Color(d, 255, 127, 0);
        colors.verboseColor = new Color(d, 0, 0, 0);

        mCreateFilterAction = new CommonAction("Create Filter") {
            @Override
            public void run() {
                mLogPanel.addFilter();
            }
        };
        mCreateFilterAction.setToolTipText("Create Filter");
        mCreateFilterAction.setImageDescriptor(loader
                .loadDescriptor("add.png")); // $NON-NLS-1$

        mEditFilterAction = new CommonAction("Edit Filter") {
            @Override
            public void run() {
                mLogPanel.editFilter();
            }
        };
        mEditFilterAction.setToolTipText("Edit Filter");
        mEditFilterAction.setImageDescriptor(loader
                .loadDescriptor("edit.png")); // $NON-NLS-1$

        mDeleteFilterAction = new CommonAction("Delete Filter") {
            @Override
            public void run() {
                mLogPanel.deleteFilter();
            }
        };
        mDeleteFilterAction.setToolTipText("Delete Filter");
        mDeleteFilterAction.setImageDescriptor(loader
                .loadDescriptor("delete.png")); // $NON-NLS-1$

        mExportAction = new CommonAction("Export Selection As Text...") {
            @Override
            public void run() {
                mLogPanel.save();
            }
        };
        mExportAction.setToolTipText("Export Selection As Text...");
        mExportAction.setImageDescriptor(loader.loadDescriptor("save.png")); // $NON-NLS-1$

        LogLevel[] levels = LogLevel.values();
        mLogLevelActions = new CommonAction[mLogLevelIcons.length];
        for (int i = 0 ; i < mLogLevelActions.length; i++) {
            String name = levels[i].getStringValue();
            mLogLevelActions[i] = new CommonAction(name, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    // disable the other actions and record current index
                    for (int i = 0 ; i < mLogLevelActions.length; i++) {
                        Action a = mLogLevelActions[i];
                        if (a == this) {
                            a.setChecked(true);

                            // set the log level
                            mLogPanel.setCurrentFilterLogLevel(i+2);
                        } else {
                            a.setChecked(false);
                        }
                    }
                }
            };

            mLogLevelActions[i].setToolTipText(name);
            mLogLevelActions[i].setImageDescriptor(loader.loadDescriptor(mLogLevelIcons[i]));
        }

        mClearAction = new Action("Clear Log") {
            @Override
            public void run() {
                mLogPanel.clear();
            }
        };
        mClearAction.setImageDescriptor(loader
                .loadDescriptor("clear.png")); // $NON-NLS-1$


        // now create the log view
        mLogPanel = new LogPanel(loader, colors, new FilterStorage(), LogPanel.FILTER_MANUAL);
        mLogPanel.setActions(mDeleteFilterAction, mEditFilterAction, mLogLevelActions);

        // get the font
        String fontStr = DdmsPlugin.getDefault().getPreferenceStore().getString(
                PreferenceInitializer.ATTR_LOGCAT_FONT);
        if (fontStr != null) {
            FontData data = new FontData(fontStr);

            if (fontStr != null) {
                mLogPanel.setFont(new Font(parent.getDisplay(), data));
            }
        }

        mLogPanel.createPanel(parent);
        setSelectionDependentPanel(mLogPanel);

        // place the actions.
        placeActions();

        // setup the copy action
        mClipboard = new Clipboard(d);
        IActionBars actionBars = getViewSite().getActionBars();
        actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), new Action("Copy") {
            @Override
            public void run() {
                mLogPanel.copy(mClipboard);
            }
        });

        // setup the select all action
        actionBars.setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(),
                new Action("Select All") {
            @Override
            public void run() {
                mLogPanel.selectAll();
            }
        });
    }

    @Override
    public void dispose() {
        mLogPanel.stopLogCat(true);
        mClipboard.dispose();
    }

    @Override
    public void setFocus() {
        mLogPanel.setFocus();
    }

    /**
     * Place the actions in the ui.
     */
    private void placeActions() {
        IActionBars actionBars = getViewSite().getActionBars();

        // first in the menu
        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(mCreateFilterAction);
        menuManager.add(mEditFilterAction);
        menuManager.add(mDeleteFilterAction);
        menuManager.add(new Separator());
        menuManager.add(mClearAction);
        menuManager.add(new Separator());
        menuManager.add(mExportAction);

        // and then in the toolbar
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        for (CommonAction a : mLogLevelActions) {
            toolBarManager.add(a);
        }
        toolBarManager.add(new Separator());
        toolBarManager.add(mCreateFilterAction);
        toolBarManager.add(mEditFilterAction);
        toolBarManager.add(mDeleteFilterAction);
        toolBarManager.add(new Separator());
        toolBarManager.add(mClearAction);
    }
 }

