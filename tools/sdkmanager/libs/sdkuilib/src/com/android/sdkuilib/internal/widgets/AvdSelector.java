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

package com.android.sdkuilib.internal.widgets;

import com.android.prefs.AndroidLocation.AndroidLocationException;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISdkLog;
import com.android.sdklib.NullSdkLog;
import com.android.sdklib.SdkConstants;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo.AvdStatus;
import com.android.sdklib.internal.repository.ITask;
import com.android.sdklib.internal.repository.ITaskMonitor;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;
import com.android.sdkuilib.internal.tasks.ProgressTask;
import com.android.sdkuilib.repository.UpdaterWindow;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * The AVD selector is a table that is added to the given parent composite.
 * <p/>
 * After using one of the constructors, call {@link #setSelection(AvdInfo)},
 * {@link #setSelectionListener(SelectionListener)} and finally use
 * {@link #getSelected()} to retrieve the selection.
 */
public final class AvdSelector {
    private static int NUM_COL = 2;

    private final DisplayMode mDisplayMode;

    private AvdManager mAvdManager;
    private final String mOsSdkPath;

    private Table mTable;
    private Button mDeleteButton;
    private Button mDetailsButton;
    private Button mNewButton;
    private Button mRefreshButton;
    private Button mManagerButton;
    private Button mRepairButton;
    private Button mStartButton;

    private SelectionListener mSelectionListener;
    private IAvdFilter mTargetFilter;

    /** Defaults to true. Changed by the {@link #setEnabled(boolean)} method to represent the
     * "global" enabled state on this composite. */
    private boolean mIsEnabled = true;

    private ImageFactory mImageFactory;
    private Image mOkImage;
    private Image mBrokenImage;


    /**
     * The display mode of the AVD Selector.
     */
    public static enum DisplayMode {
        /**
         * Manager mode. Invalid AVDs are displayed. Buttons to create/delete AVDs
         */
        MANAGER,

        /**
         * Non manager mode. Only valid AVDs are displayed. Cannot create/delete AVDs, but
         * there is a button to open the AVD Manager.
         * In the "check" selection mode, checkboxes are displayed on each line
         * and {@link AvdSelector#getSelected()} returns the line that is checked
         * even if it is not the currently selected line. Only one line can
         * be checked at once.
         */
        SIMPLE_CHECK,

        /**
         * Non manager mode. Only valid AVDs are displayed. Cannot create/delete AVDs, but
         * there is a button to open the AVD Manager.
         * In the "select" selection mode, there are no checkboxes and
         * {@link AvdSelector#getSelected()} returns the line currently selected.
         * Only one line can be selected at once.
         */
        SIMPLE_SELECTION,
    }

    /**
     * A filter to control the whether or not an AVD should be displayed by the AVD Selector.
     */
    public interface IAvdFilter {
        /**
         * Called before {@link #accept(AvdInfo)} is called for any AVD.
         */
        void prepare();

        /**
         * Called to decided whether an AVD should be displayed.
         * @param avd the AVD to test.
         * @return true if the AVD should be displayed.
         */
        boolean accept(AvdInfo avd);

        /**
         * Called after {@link #accept(AvdInfo)} has been called on all the AVDs.
         */
        void cleanup();
    }

    /**
     * Internal implementation of {@link IAvdFilter} to filter out the AVDs that are not
     * running an image compatible with a specific target.
     */
    private final static class TargetBasedFilter implements IAvdFilter {
        private final IAndroidTarget mTarget;

        TargetBasedFilter(IAndroidTarget target) {
            mTarget = target;
        }

        public void prepare() {
            // nothing to prepare
        }

        public boolean accept(AvdInfo avd) {
            if (avd != null) {
                return mTarget.isCompatibleBaseFor(avd.getTarget());
            }

            return false;
        }

        public void cleanup() {
            // nothing to clean up
        }
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}, filtered
     * by a {@link IAndroidTarget}.
     * <p/>Only the {@link AvdInfo} able to run application developed for the given
     * {@link IAndroidTarget} will be displayed.
     *
     * @param parent The parent composite where the selector will be added.
     * @param osSdkPath The SDK root path. When not null, enables the start button to start
     *                  an emulator on a given AVD.
     * @param manager the AVD manager.
     * @param filter When non-null, will allow filtering the AVDs to display.
     * @param displayMode The display mode ({@link DisplayMode}).
     *
     * TODO: pass an ISdkLog and use it when reloading, starting the emulator, etc.
     */
    public AvdSelector(Composite parent,
            String osSdkPath,
            AvdManager manager,
            IAvdFilter filter,
            DisplayMode displayMode) {
        mOsSdkPath = osSdkPath;
        mAvdManager = manager;
        mTargetFilter = filter;
        mDisplayMode = displayMode;

        // get some bitmaps.
        mImageFactory = new ImageFactory(parent.getDisplay());
        mOkImage = mImageFactory.getImageByName("accept_icon16.png");
        mBrokenImage = mImageFactory.getImageByName("reject_icon16.png");

        // Layout has 2 columns
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl;
        group.setLayout(gl = new GridLayout(NUM_COL, false /*makeColumnsEqualWidth*/));
        gl.marginHeight = gl.marginWidth = 0;
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());
        group.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent arg0) {
                mImageFactory.dispose();
            }
        });

        int style = SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER;
        if (displayMode == DisplayMode.SIMPLE_CHECK) {
            style |= SWT.CHECK;
        }
        mTable = new Table(group, style);
        mTable.setHeaderVisible(true);
        mTable.setLinesVisible(false);
        setTableHeightHint(0);

        Composite buttons = new Composite(group, SWT.NONE);
        buttons.setLayout(gl = new GridLayout(1, false /*makeColumnsEqualWidth*/));
        gl.marginHeight = gl.marginWidth = 0;
        buttons.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        buttons.setFont(group.getFont());

        if (displayMode == DisplayMode.MANAGER) {
            mNewButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            mNewButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mNewButton.setText("New...");
            mNewButton.setToolTipText("Creates a new AVD.");
            mNewButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    onNew();
                }
            });

            mDeleteButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            mDeleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mDeleteButton.setText("Delete...");
            mDeleteButton.setToolTipText("Deletes the selected AVD.");
            mDeleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    onDelete();
                }
            });

            mRepairButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            mRepairButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mRepairButton.setText("Repair...");
            mRepairButton.setToolTipText("Repairs the selected AVD.");
            mRepairButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    onRepair();
                }
            });

            Label l = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
            l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        mDetailsButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mDetailsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mDetailsButton.setText("Details...");
        mDetailsButton.setToolTipText("Diplays details of the selected AVD.");
        mDetailsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onDetails();
            }
        });

        mStartButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mStartButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mStartButton.setText("Start...");
        mStartButton.setToolTipText("Starts the selected AVD.");
        mStartButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onStart();
            }
        });

        Composite padding = new Composite(buttons, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        mRefreshButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        mRefreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mRefreshButton.setText("Refresh");
        mRefreshButton.setToolTipText("Reloads the list of AVD.\nUse this if you create AVDs from the command line.");
        mRefreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                refresh(true);
            }
        });

        if (displayMode != DisplayMode.MANAGER) {
            mManagerButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            mManagerButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mManagerButton.setText("Manager...");
            mManagerButton.setToolTipText("Launches the AVD manager.");
            mManagerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    onManager();
                }
            });
        } else {
            Composite legend = new Composite(group, SWT.NONE);
            legend.setLayout(gl = new GridLayout(2, false /*makeColumnsEqualWidth*/));
            gl.marginHeight = gl.marginWidth = 0;
            legend.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false,
                    NUM_COL, 1));
            legend.setFont(group.getFont());

            new Label(legend, SWT.NONE).setImage(mOkImage);
            new Label(legend, SWT.NONE).setText("A valid Android Virtual Device.");
            new Label(legend, SWT.NONE).setImage(mBrokenImage);
            new Label(legend, SWT.NONE).setText(
                    "An Android Virtual Device that failed to load. Click 'Details' to see the error.");
        }

        // create the table columns
        final TableColumn column0 = new TableColumn(mTable, SWT.NONE);
        column0.setText("AVD Name");
        final TableColumn column1 = new TableColumn(mTable, SWT.NONE);
        column1.setText("Target Name");
        final TableColumn column2 = new TableColumn(mTable, SWT.NONE);
        column2.setText("Platform");
        final TableColumn column3 = new TableColumn(mTable, SWT.NONE);
        column3.setText("API Level");

        adjustColumnsWidth(mTable, column0, column1, column2, column3);
        setupSelectionListener(mTable);
        fillTable(mTable);
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}.
     *
     * @param parent The parent composite where the selector will be added.
     * @param manager the AVD manager.
     * @param displayMode The display mode ({@link DisplayMode}).
     */
    public AvdSelector(Composite parent,
            String osSdkPath,
            AvdManager manager,
            DisplayMode displayMode) {
        this(parent, osSdkPath, manager, (IAvdFilter)null /* filter */, displayMode);
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}, filtered
     * by an {@link IAndroidTarget}.
     * <p/>Only the {@link AvdInfo} able to run applications developed for the given
     * {@link IAndroidTarget} will be displayed.
     *
     * @param parent The parent composite where the selector will be added.
     * @param manager the AVD manager.
     * @param filter Only shows the AVDs matching this target (must not be null).
     * @param displayMode The display mode ({@link DisplayMode}).
     */
    public AvdSelector(Composite parent,
            String osSdkPath,
            AvdManager manager,
            IAndroidTarget filter,
            DisplayMode displayMode) {
        this(parent, osSdkPath, manager, new TargetBasedFilter(filter), displayMode);
    }
    /**
     * Sets the table grid layout data.
     *
     * @param heightHint If > 0, the height hint is set to the requested value.
     */
    public void setTableHeightHint(int heightHint) {
        GridData data = new GridData();
        if (heightHint > 0) {
            data.heightHint = heightHint;
        }
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mTable.setLayoutData(data);
    }

    /**
     * Refresh the display of Android Virtual Devices.
     * Tries to keep the selection.
     * <p/>
     * This must be called from the UI thread.
     *
     * @param reload if true, the AVD manager will reload the AVD from the disk.
     * @return false if the reloading failed. This is always true if <var>reload</var> is
     * <code>false</code>.
     */
    public boolean refresh(boolean reload) {
        if (reload) {
            try {
                mAvdManager.reloadAvds(NullSdkLog.getLogger());
            } catch (AndroidLocationException e) {
                return false;
            }
        }

        AvdInfo selected = getSelected();

        fillTable(mTable);

        setSelection(selected);

        return true;
    }

    /**
     * Sets a new AVD manager
     * This does not refresh the display. Call {@link #refresh(boolean)} to do so.
     * @param manager the AVD manager.
     */
    public void setManager(AvdManager manager) {
        mAvdManager = manager;
    }

    /**
     * Sets a new AVD filter.
     * This does not refresh the display. Call {@link #refresh(boolean)} to do so.
     * @param filter An IAvdFilter. If non-null, this will filter out the AVD to not display.
     */
    public void setFilter(IAvdFilter filter) {
        mTargetFilter = filter;
    }

    /**
     * Sets a new Android Target-based AVD filter.
     * This does not refresh the display. Call {@link #refresh(boolean)} to do so.
     * @param target An IAndroidTarget. If non-null, only AVD whose target are compatible with the
     * filter target will displayed an available for selection.
     */
    public void setFilter(IAndroidTarget target) {
        if (target != null) {
            mTargetFilter = new TargetBasedFilter(target);
        } else {
            mTargetFilter = null;
        }
    }

    /**
     * Sets a selection listener. Set it to null to remove it.
     * The listener will be called <em>after</em> this table processed its selection
     * events so that the caller can see the updated state.
     * <p/>
     * The event's item contains a {@link TableItem}.
     * The {@link TableItem#getData()} contains an {@link IAndroidTarget}.
     * <p/>
     * It is recommended that the caller uses the {@link #getSelected()} method instead.
     * <p/>
     * The default behavior for double click (when not in {@link DisplayMode#SIMPLE_CHECK}) is to
     * display the details of the selected AVD.<br>
     * To disable it (when you provide your own double click action), set
     * {@link SelectionEvent#doit} to false in
     * {@link SelectionListener#widgetDefaultSelected(SelectionEvent)}
     *
     * @param selectionListener The new listener or null to remove it.
     */
    public void setSelectionListener(SelectionListener selectionListener) {
        mSelectionListener = selectionListener;
    }

    /**
     * Sets the current target selection.
     * <p/>
     * If the selection is actually changed, this will invoke the selection listener
     * (if any) with a null event.
     *
     * @param target the target to be selected. Use null to deselect everything.
     * @return true if the target could be selected, false otherwise.
     */
    public boolean setSelection(AvdInfo target) {
        boolean found = false;
        boolean modified = false;

        int selIndex = mTable.getSelectionIndex();
        int index = 0;
        for (TableItem i : mTable.getItems()) {
            if (mDisplayMode == DisplayMode.SIMPLE_CHECK) {
                if ((AvdInfo) i.getData() == target) {
                    found = true;
                    if (!i.getChecked()) {
                        modified = true;
                        i.setChecked(true);
                    }
                } else if (i.getChecked()) {
                    modified = true;
                    i.setChecked(false);
                }
            } else {
                if ((AvdInfo) i.getData() == target) {
                    found = true;
                    if (index != selIndex) {
                        mTable.setSelection(index);
                        modified = true;
                    }
                    break;
                }

                index++;
            }
        }

        if (modified && mSelectionListener != null) {
            mSelectionListener.widgetSelected(null);
        }

        enableActionButtons();

        return found;
    }

    /**
     * Returns the currently selected item. In {@link DisplayMode#SIMPLE_CHECK} mode this will
     * return the {@link AvdInfo} that is checked instead of the list selection.
     *
     * @return The currently selected item or null.
     */
    public AvdInfo getSelected() {
        if (mDisplayMode == DisplayMode.SIMPLE_CHECK) {
            for (TableItem i : mTable.getItems()) {
                if (i.getChecked()) {
                    return (AvdInfo) i.getData();
                }
            }
        } else {
            int selIndex = mTable.getSelectionIndex();
            if (selIndex >= 0) {
                return (AvdInfo) mTable.getItem(selIndex).getData();
            }
        }

        return null;
    }

    /**
     * Enables the receiver if the argument is true, and disables it otherwise.
     * A disabled control is typically not selectable from the user interface
     * and draws with an inactive or "grayed" look.
     *
     * @param enabled the new enabled state.
     */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;

        mTable.setEnabled(mIsEnabled);
        mRefreshButton.setEnabled(mIsEnabled);

        if (mNewButton != null) {
            mNewButton.setEnabled(mIsEnabled);
        }
        if (mManagerButton != null) {
            mManagerButton.setEnabled(mIsEnabled);
        }

        enableActionButtons();
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    /**
     * Adds a listener to adjust the columns width when the parent is resized.
     * <p/>
     * If we need something more fancy, we might want to use this:
     * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet77.java?view=co
     */
    private void adjustColumnsWidth(final Table table,
            final TableColumn column0,
            final TableColumn column1,
            final TableColumn column2,
            final TableColumn column3) {
        // Add a listener to resize the column to the full width of the table
        table.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Rectangle r = table.getClientArea();
                column0.setWidth(r.width * 25 / 100); // 25%
                column1.setWidth(r.width * 45 / 100); // 45%
                column2.setWidth(r.width * 15 / 100); // 15%
                column3.setWidth(r.width * 15 / 100); // 15%
            }
        });
    }

    /**
     * Creates a selection listener that will check or uncheck the whole line when
     * double-clicked (aka "the default selection").
     */
    private void setupSelectionListener(final Table table) {
        // Add a selection listener that will check/uncheck items when they are double-clicked
        table.addSelectionListener(new SelectionListener() {

            /**
             * Handles single-click selection on the table.
             * {@inheritDoc}
             */
            public void widgetSelected(SelectionEvent e) {
                if (e.item instanceof TableItem) {
                    TableItem i = (TableItem) e.item;
                    enforceSingleSelection(i);
                }

                if (mSelectionListener != null) {
                    mSelectionListener.widgetSelected(e);
                }

                enableActionButtons();
            }

            /**
             * Handles double-click selection on the table.
             * Note that the single-click handler will probably already have been called.
             *
             * On double-click, <em>always</em> check the table item.
             *
             * {@inheritDoc}
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.item instanceof TableItem) {
                    TableItem i = (TableItem) e.item;
                    if (mDisplayMode == DisplayMode.SIMPLE_CHECK) {
                        i.setChecked(true);
                    }
                    enforceSingleSelection(i);

                }

                // whether or not we display details. default: true when not in SIMPLE_CHECK mode.
                boolean showDetails = mDisplayMode != DisplayMode.SIMPLE_CHECK;

                if (mSelectionListener != null) {
                    mSelectionListener.widgetDefaultSelected(e);
                    showDetails &= e.doit; // enforce false in SIMPLE_CHECK
                }

                if (showDetails) {
                    onDetails();
                }

                enableActionButtons();
            }

            /**
             * To ensure single selection, uncheck all other items when this one is selected.
             * This makes the chekboxes act as radio buttons.
             */
            private void enforceSingleSelection(TableItem item) {
                if (mDisplayMode == DisplayMode.SIMPLE_CHECK) {
                    if (item.getChecked()) {
                        Table parentTable = item.getParent();
                        for (TableItem i2 : parentTable.getItems()) {
                            if (i2 != item && i2.getChecked()) {
                                i2.setChecked(false);
                            }
                        }
                    }
                } else {
                    // pass
                }
            }
        });
    }

    /**
     * Fills the table with all AVD.
     * The table columns are:
     * <ul>
     * <li>column 0: sdk name
     * <li>column 1: sdk vendor
     * <li>column 2: sdk api name
     * <li>column 3: sdk version
     * </ul>
     */
    private void fillTable(final Table table) {
        table.removeAll();

        // get the AVDs
        AvdInfo avds[] = null;
        if (mAvdManager != null) {
            if (mDisplayMode == DisplayMode.MANAGER) {
                avds = mAvdManager.getAllAvds();
            } else {
                avds = mAvdManager.getValidAvds();
            }
        }

        if (avds != null && avds.length > 0) {
            table.setEnabled(true);

            if (mTargetFilter != null) {
                mTargetFilter.prepare();
            }

            for (AvdInfo avd : avds) {
                if (mTargetFilter == null || mTargetFilter.accept(avd)) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setData(avd);
                    item.setText(0, avd.getName());
                    if (mDisplayMode == DisplayMode.MANAGER) {
                        item.setImage(0, avd.getStatus() == AvdStatus.OK ? mOkImage : mBrokenImage);
                    }
                    IAndroidTarget target = avd.getTarget();
                    if (target != null) {
                        item.setText(1, target.getFullName());
                        item.setText(2, target.getVersionName());
                        item.setText(3, target.getVersion().getApiString());
                    } else {
                        item.setText(1, "?");
                        item.setText(2, "?");
                        item.setText(3, "?");
                    }
                }
            }

            if (mTargetFilter != null) {
                mTargetFilter.cleanup();
            }
        }

        if (table.getItemCount() == 0) {
            table.setEnabled(false);
            TableItem item = new TableItem(table, SWT.NONE);
            item.setData(null);
            item.setText(0, "--");
            item.setText(1, "No AVD available");
            item.setText(2, "--");
            item.setText(3, "--");
        }
    }

    /**
     * Returns the currently selected AVD in the table.
     * <p/>
     * Unlike {@link #getSelected()} this will always return the item being selected
     * in the list, ignoring the check boxes state in {@link DisplayMode#SIMPLE_CHECK} mode.
     */
    private AvdInfo getTableSelection() {
        int selIndex = mTable.getSelectionIndex();
        if (selIndex >= 0) {
            return (AvdInfo) mTable.getItem(selIndex).getData();
        }

        return null;
    }

    /**
     * Updates the enable state of the Details, Start, Delete and Update buttons.
     */
    private void enableActionButtons() {
        if (mIsEnabled == false) {
            mDetailsButton.setEnabled(false);
            mStartButton.setEnabled(false);

            if (mDeleteButton != null) {
                mDeleteButton.setEnabled(false);
            }
            if (mRepairButton != null) {
                mRepairButton.setEnabled(false);
            }
        } else {
            AvdInfo selection = getTableSelection();
            boolean hasSelection = selection != null;

            mDetailsButton.setEnabled(hasSelection);
            mStartButton.setEnabled(mOsSdkPath != null &&
                    hasSelection &&
                    selection.getStatus() == AvdStatus.OK);

            if (mDeleteButton != null) {
                mDeleteButton.setEnabled(hasSelection);
            }
            if (mRepairButton != null) {
                mRepairButton.setEnabled(hasSelection &&
                        selection.getStatus() == AvdStatus.ERROR_IMAGE_DIR);
            }
        }
    }

    private void onNew() {
        AvdCreationDialog dlg = new AvdCreationDialog(mTable.getShell(), mAvdManager,
                mImageFactory);
        if (dlg.open() == Window.OK) {
            refresh(false /*reload*/);
        }
    }

    private void onDetails() {
        final AvdInfo avdInfo = getTableSelection();

        AvdDetailsDialog dlg = new AvdDetailsDialog(mTable.getShell(), avdInfo);
        dlg.open();
    }

    private void onDelete() {
        final AvdInfo avdInfo = getTableSelection();

        // get the current Display
        final Display display = mTable.getDisplay();

        // Confirm you want to delete this AVD
        final boolean[] result = new boolean[1];
        display.syncExec(new Runnable() {
            public void run() {
                Shell shell = display.getActiveShell();
                result[0] = MessageDialog.openQuestion(shell,
                        "Delete Android Virtual Device",
                        String.format(
                                "Please confirm that you want to delete the Android Virtual Device named '%s'. This operation cannot be reverted.",
                                avdInfo.getName()));
            }
        });

        if (result[0] == false) {
            return;
        }

        // log for this action.
        SdkLog log = new SdkLog(
                String.format("Result of deleting AVD '%s':", avdInfo.getName()),
                display);

        // delete the AVD
        boolean success = mAvdManager.deleteAvd(avdInfo, log);

        // display the result
        log.displayResult(success);

        if (success) {
            refresh(false /*reload*/);
        }
    }

    /**
     * Repairs the selected AVD.
     * <p/>
     * For now this only supports fixing the wrong value in image.sysdir.*
     */
    private void onRepair() {
        final AvdInfo avdInfo = getTableSelection();

        // get the current Display
        final Display display = mTable.getDisplay();

        // log for this action.
        SdkLog log = new SdkLog(
                String.format("Result of updating AVD '%s':", avdInfo.getName()),
                display);

        // delete the AVD
        try {
            mAvdManager.updateAvd(avdInfo, log);

            // display the result
            log.displayResult(true /* success */);

            refresh(false /*reload*/);
        } catch (IOException e) {
            log.error(e, null);
            log.displayResult(false /* success */);
        }
    }

    private void onManager() {
        UpdaterWindow window = new UpdaterWindow(
                mTable.getShell(),
                null /*sdk log*/,
                mAvdManager.getSdkManager().getLocation(),
                false /*userCanChangeSdkRoot*/);
        window.open();
        refresh(true /*reload*/); // UpdaterWindow uses its own AVD manager so this one must reload.
    }

    private void onStart() {
        AvdInfo avdInfo = getTableSelection();

        if (avdInfo == null || mOsSdkPath == null) {
            return;
        }

        String path = mOsSdkPath +
            File.separator +
            SdkConstants.OS_SDK_TOOLS_FOLDER +
            SdkConstants.FN_EMULATOR;

        final String avdName = avdInfo.getName();

        // build the command line based on the available parameters.
        ArrayList<String> list = new ArrayList<String>();
        list.add(path);
        list.add("-avd");   //$NON-NLS-1$
        list.add(avdName);

        // convert the list into an array for the call to exec.
        final String[] command = list.toArray(new String[list.size()]);

        // launch the emulator
        new ProgressTask(mTable.getShell(),
                "Starting Android Emulator",
                new ITask() {
                    public void run(ITaskMonitor monitor) {
                        try {
                            monitor.setDescription("Starting emulator for AVD '%1$s'", avdName);
                            int n = 10;
                            monitor.setProgressMax(n);
                            Process process = Runtime.getRuntime().exec(command);
                            grabEmulatorOutput(process, monitor);

                            // This small wait prevents the dialog from closing too fast:
                            // When it works, the emulator returns immediately, even if no UI
                            // is shown yet. And when it fails (because the AVD is locked/running)
                            // if we don't have a wait we don't capture the error for some reason.
                            for (int i = 0; i < n; i++) {
                                try {
                                    Thread.sleep(100);
                                    monitor.incProgress(1);
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                            }
                        } catch (IOException e) {
                            monitor.setResult("Failed to start emulator: %1$s", e.getMessage());
                        }
                    }
        });

    }

    /**
     * Get the stderr/stdout outputs of a process and return when the process is done.
     * Both <b>must</b> be read or the process will block on windows.
     * @param process The process to get the output from.
     * @param monitor An {@link ISdkLog} to capture errors.
     */
    private void grabEmulatorOutput(final Process process, final ITaskMonitor monitor) {
        // read the lines as they come. if null is returned, it's because the process finished
        new Thread("emu-stderr") { //$NON-NLS-1$
            @Override
            public void run() {
                // create a buffer to read the stderr output
                InputStreamReader is = new InputStreamReader(process.getErrorStream());
                BufferedReader errReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = errReader.readLine();
                        if (line != null) {
                            monitor.setResult("%1$s", line);    //$NON-NLS-1$
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }.start();

        new Thread("emu-stdout") { //$NON-NLS-1$
            @Override
            public void run() {
                InputStreamReader is = new InputStreamReader(process.getInputStream());
                BufferedReader outReader = new BufferedReader(is);

                try {
                    while (true) {
                        String line = outReader.readLine();
                        if (line != null) {
                            monitor.setResult("%1$s", line);    //$NON-NLS-1$
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }.start();
    }

    /**
     * Collects all log from the AVD action and displays it in a dialog.
     */
    static class SdkLog implements ISdkLog {

        final ArrayList<String> logMessages = new ArrayList<String>();
        private final String mMessage;
        private final Display mDisplay;

        public SdkLog(String message, Display display) {
            mMessage = message;
            mDisplay = display;
        }

        public void error(Throwable throwable, String errorFormat, Object... arg) {
            if (errorFormat != null) {
                logMessages.add(String.format("Error: " + errorFormat, arg));
            }

            if (throwable != null) {
                logMessages.add(throwable.getMessage());
            }
        }

        public void warning(String warningFormat, Object... arg) {
            logMessages.add(String.format("Warning: " + warningFormat, arg));
        }

        public void printf(String msgFormat, Object... arg) {
            logMessages.add(String.format(msgFormat, arg));
        }

        /**
         * Displays the log if anything was captured.
         */
        public void displayResult(final boolean success) {
            if (logMessages.size() > 0) {
                final StringBuilder sb = new StringBuilder(mMessage + "\n\n");
                for (String msg : logMessages) {
                    sb.append(msg);
                }

                // display the message
                // dialog box only run in ui thread..
                mDisplay.asyncExec(new Runnable() {
                    public void run() {
                        Shell shell = mDisplay.getActiveShell();
                        if (success) {
                            MessageDialog.openInformation(shell, "Android Virtual Devices Manager",
                                    sb.toString());
                        } else {
                            MessageDialog.openError(shell, "Android Virtual Devices Manager",
                                    sb.toString());

                        }
                    }
                });
            }
        }
    }
}
