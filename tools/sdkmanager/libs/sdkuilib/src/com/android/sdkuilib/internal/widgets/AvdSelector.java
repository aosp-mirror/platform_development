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
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo.AvdStatus;
import com.android.sdkuilib.internal.repository.icons.ImageFactory;

import org.eclipse.jface.dialogs.MessageDialog;
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

import java.util.ArrayList;


/**
 * The AVD selector is a table that is added to the given parent composite.
 * <p/>
 * To use, create it using {@link #AvdSelector(Composite, AvdManager, DisplayMode)} then
 * call {@link #setSelection(AvdInfo)}, {@link #setSelectionListener(SelectionListener)}
 * and finally use {@link #getSelected()} to retrieve the selection.
 */
public final class AvdSelector {
    private SelectionListener mSelectionListener;
    private Table mTable;

    private static int NUM_COL = 2;
    private final DisplayMode mDisplayMode;
    private Button mManagerButton;
    private IAvdFilter mTargetFilter;
    private AvdManager mManager;
    private Image mOkImage;
    private Image mBrokenImage;
    private ImageFactory mIconFactory;

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
     * @param manager the AVD manager.
     * @param filter When non-null, will allow filtering the AVDs to display.
     * @param extraAction When non-null, displays an extra action button.
     * @param displayMode The display mode ({@link DisplayMode}).
     */
    public AvdSelector(Composite parent,
            AvdManager manager,
            IAvdFilter filter,
            DisplayMode displayMode) {
        mManager = manager;
        mTargetFilter = filter;
        mDisplayMode = displayMode;

        // Layout has 2 columns
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl;
        group.setLayout(gl = new GridLayout(NUM_COL, false /*makeColumnsEqualWidth*/));
        gl.marginHeight = gl.marginWidth = 0;
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());
        group.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent arg0) {
                mIconFactory.dispose();
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
            Button newButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            newButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            newButton.setText("New...");

            Button deleteButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            deleteButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            deleteButton.setText("Delete");
            deleteButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    onDelete();
                }
            });

            Label l = new Label(buttons, SWT.SEPARATOR | SWT.HORIZONTAL);
            l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        Button infoButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        infoButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        infoButton.setText("Details...");
        infoButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                onDetails();
            }
        });

        Composite padding = new Composite(buttons, SWT.NONE);
        padding.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        Button refreshButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
        refreshButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        refreshButton.setText("Resfresh");
        refreshButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                refresh(true);
            }
        });

        if (displayMode != DisplayMode.MANAGER) {
            mManagerButton = new Button(buttons, SWT.PUSH | SWT.FLAT);
            mManagerButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            mManagerButton.setText("Manager...");
            mManagerButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                }
            });
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

        // get some bitmaps.
        mIconFactory = new ImageFactory(parent.getDisplay());
        mOkImage = mIconFactory.getImageByName("accept_icon16.png");
        mBrokenImage = mIconFactory.getImageByName("reject_icon16.png");

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
    public AvdSelector(Composite parent, AvdManager manager,
            DisplayMode displayMode) {
        this(parent, manager, (IAvdFilter)null /* filter */, displayMode);
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
            AvdManager manager,
            IAndroidTarget filter,
            DisplayMode displayMode) {
        this(parent, manager, new TargetBasedFilter(filter), displayMode);
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
     * @throws AndroidLocationException if reload the AVD failed.
     * @return false if the reloading failed. This is always true if <var>reload</var> is
     * <code>false</code>.
     */
    public boolean refresh(boolean reload) {
        if (reload) {
            try {
                mManager.reloadAvds();
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
        mManager = manager;
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

        return found;
    }

    /**
     * Returns the currently selected item.
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
        mTable.setEnabled(enabled);
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

                //whether or not we display details. default: true when not in SIMPLE_CHECK mode.
                boolean showDetails = mDisplayMode != DisplayMode.SIMPLE_CHECK;

                if (mSelectionListener != null) {
                    mSelectionListener.widgetDefaultSelected(e);
                    showDetails &= e.doit; // enforce false in SIMPLE_CHECK
                }

                if (showDetails) {
                    onDetails();
                }
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
        if (mManager != null) {
            if (mDisplayMode == DisplayMode.MANAGER) {
                avds = mManager.getAllAvds();
            } else {
                avds = mManager.getValidAvds();
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
                        item.setText(2, target.getApiVersionName());
                        item.setText(3, Integer.toString(target.getApiVersionNumber()));
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

    private void onDetails() {
        final AvdInfo avdInfo = getSelected();

        AvdDetailsDialog dlg = new AvdDetailsDialog(mTable.getShell(), avdInfo);
        dlg.open();
    }

    private void onDelete() {
        final AvdInfo avdInfo = getSelected();

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
        boolean success = mManager.deleteAvd(avdInfo, log);

        // display the result
        log.displayResult(success);

        if (success) {
            refresh(false);
        }
    }

    /**
     * Collects all log from the AVD action and displays it in a dialog.
     */
    private class SdkLog implements ISdkLog {

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
                final StringBuilder sb = new StringBuilder(mMessage + "\n");
                for (String msg : logMessages) {
                    sb.append('\n');
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
