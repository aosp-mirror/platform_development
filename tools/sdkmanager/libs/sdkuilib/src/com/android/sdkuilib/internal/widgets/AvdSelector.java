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

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.internal.avd.AvdManager.AvdInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


/**
 * The AVD selector is a table that is added to the given parent composite.
 * <p/>
 * To use, create it using {@link #AvdSelector(Composite, SelectionMode, IExtraAction)} then
 * call {@link #setSelection(AvdInfo)}, {@link #setSelectionListener(SelectionListener)}
 * and finally use {@link #getSelected()} to retrieve the selection.
 */
public final class AvdSelector {

    private AvdInfo[] mAvds;
    private SelectionListener mSelectionListener;
    private Table mTable;
    private Label mDescription;

    private static int NUM_COL = 2;
    private final SelectionMode mSelectionMode;
    private final IExtraAction mExtraAction;
    private Button mExtraActionButton;

    /** The selection mode, either {@link #SELECT} or {@link #CHECK} */
    public enum SelectionMode {
        /**
         * In the "check" selection mode, checkboxes are displayed on each line
         * and {@link AvdSelector#getSelected()} returns the line that is checked
         * even if it is not the currently selected line. Only one line can
         * be checked at once.
         */
        CHECK,
        /**
         * In the "select" selection mode, there are no checkboxes and
         * {@link AvdSelector#getSelected()} returns the line currently selected.
         * Only one line can be selected at once.
         */
        SELECT
    }

    /**
     * Defines an "extra action" button that can be shown under the AVD Selector.
     */
    public interface IExtraAction {
        /**
         * Label of the button that will be created.
         * This is invoked once when the button is created and cannot be changed later.
         */
        public String label();

        /**
         * This is invoked just after the selection has changed to update the "enabled"
         * state of the action. Implementation should use {@link AvdSelector#getSelected()}.
         */
        public boolean isEnabled();

        /**
         * Run the action, invoked when the button is clicked.
         *
         * The caller's action is responsible for reloading the AVD list
         * using {@link AvdSelector#setAvds(AvdInfo[], IAndroidTarget)}.
         */
        public void run();
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}, filtered
     * by a {@link IAndroidTarget}.
     * <p/>Only the {@link AvdInfo} able to run application developed for the given
     * {@link IAndroidTarget} will be displayed.
     *
     * @param parent The parent composite where the selector will be added.
     * @param avds The list of AVDs. This is <em>not</em> copied, the caller must not modify.
     *             It can be null.
     * @param filter When non-null, will display only the AVDs matching this target.
     * @param extraAction When non-null, displays an extra action button.
     * @param selectionMode One of {@link SelectionMode#SELECT} or {@link SelectionMode#CHECK}
     */
    public AvdSelector(Composite parent,
            AvdInfo[] avds,
            IAndroidTarget filter,
            IExtraAction extraAction,
            SelectionMode selectionMode) {
        mAvds = avds;
        mExtraAction = extraAction;
        mSelectionMode = selectionMode;

        // Layout has 2 columns
        Composite group = new Composite(parent, SWT.NONE);
        GridLayout gl;
        group.setLayout(gl = new GridLayout(NUM_COL, false /*makeColumnsEqualWidth*/));
        gl.marginHeight = gl.marginWidth = 0;
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());

        int style = SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER;
        if (selectionMode == SelectionMode.CHECK) {
            style |= SWT.CHECK;
        }
        mTable = new Table(group, style);
        mTable.setHeaderVisible(true);
        mTable.setLinesVisible(false);
        setTableHeightHint(0);

        mDescription = new Label(group, SWT.WRAP);
        mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (extraAction != null) {
            mExtraActionButton = new Button(group, SWT.PUSH);
            mExtraActionButton.setText(extraAction.label());
            mExtraActionButton.setEnabled(extraAction.isEnabled());
            mExtraActionButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    super.widgetSelected(e);
                    mExtraAction.run();
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

        adjustColumnsWidth(mTable, column0, column1, column2, column3);
        setupSelectionListener(mTable);
        fillTable(mTable, filter);
        setupTooltip(mTable);
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}.
     *
     * @param parent The parent composite where the selector will be added.
     * @param avds The list of AVDs. This is <em>not</em> copied, the caller must not modify.
     *             It can be null.
     * @param extraAction When non-null, displays an extra action button.
     * @param selectionMode One of {@link SelectionMode#SELECT} or {@link SelectionMode#CHECK}
     */
    public AvdSelector(Composite parent,
            AvdInfo[] avds,
            IExtraAction extraAction,
            SelectionMode selectionMode) {
        this(parent, avds, null /* filter */, extraAction, selectionMode);
    }

    /**
     * Creates a new SDK Target Selector, and fills it with a list of {@link AvdInfo}.
     *
     * @param parent The parent composite where the selector will be added.
     * @param extraAction When non-null, displays an extra action button.
     * @param selectionMode One of {@link SelectionMode#SELECT} or {@link SelectionMode#CHECK}
     */
    public AvdSelector(Composite parent,
            SelectionMode selectionMode,
            IExtraAction extraAction) {
        this(parent, null /*avds*/, null /* filter */, extraAction, selectionMode);
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
        data.horizontalSpan = NUM_COL;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mTable.setLayoutData(data);
    }

    /**
     * Sets a new set of AVD, with an optional filter.
     * Tries to keep the selection.
     * <p/>
     * This must be called from the UI thread.
     *
     *
     * @param avds The list of AVDs. This is <em>not</em> copied, the caller must not modify.
     *             It can be null.
     * @param filter An IAndroidTarget. If non-null, only AVD whose target are compatible with the
     * filter target will displayed an available for selection.
     */
    public void setAvds(AvdInfo[] avds, IAndroidTarget filter) {

        AvdInfo selected = getSelected();

        mAvds = avds;
        fillTable(mTable, filter);

        setSelection(selected);
    }

    /**
     * Returns the list of known AVDs.
     * <p/>
     * This is not a copy. Callers must <em>not</em> modify this array.
     */
    public AvdInfo[] getAvds() {
        return mAvds;
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
            if (mSelectionMode == SelectionMode.SELECT) {
                if ((AvdInfo) i.getData() == target) {
                    found = true;
                    if (index != selIndex) {
                        mTable.setSelection(index);
                        modified = true;
                    }
                    break;
                }

                index++;

            } else if (mSelectionMode == SelectionMode.CHECK){
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
            }
        }

        if (modified && mSelectionListener != null) {
            mSelectionListener.widgetSelected(null);
        }

        if (mExtraAction != null && mExtraActionButton != null) {
            mExtraActionButton.setEnabled(mExtraAction.isEnabled());
        }

        return found;
    }

    /**
     * Returns the currently selected item.
     *
     * @return The currently selected item or null.
     */
    public AvdInfo getSelected() {
        if (mSelectionMode == SelectionMode.SELECT) {
            int selIndex = mTable.getSelectionIndex();
            if (selIndex >= 0) {
                return (AvdInfo) mTable.getItem(selIndex).getData();
            }

        } else if (mSelectionMode == SelectionMode.CHECK) {
            for (TableItem i : mTable.getItems()) {
                if (i.getChecked()) {
                    return (AvdInfo) i.getData();
                }
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
        mDescription.setEnabled(enabled);
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
                column0.setWidth(r.width * 30 / 100); // 30%
                column1.setWidth(r.width * 45 / 100); // 45%
                column2.setWidth(r.width * 10 / 100); // 10%
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
                    updateDescription(i);
                }

                if (mSelectionListener != null) {
                    mSelectionListener.widgetSelected(e);
                }

                if (mExtraAction != null && mExtraActionButton != null) {
                    mExtraActionButton.setEnabled(mExtraAction.isEnabled());
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
                    if (mSelectionMode == SelectionMode.CHECK) {
                        i.setChecked(true);
                    }
                    enforceSingleSelection(i);
                    updateDescription(i);
                }

                if (mSelectionListener != null) {
                    mSelectionListener.widgetDefaultSelected(e);
                }

                if (mExtraAction != null && mExtraActionButton != null) {
                    mExtraActionButton.setEnabled(mExtraAction.isEnabled());
                }
            }

            /**
             * To ensure single selection, uncheck all other items when this one is selected.
             * This makes the chekboxes act as radio buttons.
             */
            private void enforceSingleSelection(TableItem item) {
                if (mSelectionMode == SelectionMode.SELECT) {
                    // pass

                } else if (mSelectionMode == SelectionMode.CHECK) {
                    if (item.getChecked()) {
                        Table parentTable = item.getParent();
                        for (TableItem i2 : parentTable.getItems()) {
                            if (i2 != item && i2.getChecked()) {
                                i2.setChecked(false);
                            }
                        }
                    }
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
    private void fillTable(final Table table, IAndroidTarget filter) {
        table.removeAll();
        if (mAvds != null && mAvds.length > 0) {
            table.setEnabled(true);
            for (AvdInfo avd : mAvds) {
                if (filter == null || filter.isCompatibleBaseFor(avd.getTarget())) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setData(avd);
                    item.setText(0, avd.getName());
                    IAndroidTarget target = avd.getTarget();
                    item.setText(1, target.getFullName());
                    item.setText(2, target.getApiVersionName());
                    item.setText(3, Integer.toString(target.getApiVersionNumber()));
                }
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
     * Sets up a tooltip that displays the current item description.
     * <p/>
     * Displaying a tooltip over the table looks kind of odd here. Instead we actually
     * display the description in a label under the table.
     */
    private void setupTooltip(final Table table) {
        /*
         * Reference:
         * http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet125.java?view=markup
         */

        final Listener listener = new Listener() {
            public void handleEvent(Event event) {

                switch(event.type) {
                case SWT.KeyDown:
                case SWT.MouseExit:
                case SWT.MouseDown:
                    return;

                case SWT.MouseHover:
                    updateDescription(table.getItem(new Point(event.x, event.y)));
                    break;

                case SWT.Selection:
                    if (event.item instanceof TableItem) {
                        updateDescription((TableItem) event.item);
                    }
                    break;

                default:
                    return;
                }

            }
        };

        table.addListener(SWT.Dispose, listener);
        table.addListener(SWT.KeyDown, listener);
        table.addListener(SWT.MouseMove, listener);
        table.addListener(SWT.MouseHover, listener);
    }

    /**
     * Updates the description label with the path of the item's AVD, if any.
     */
    private void updateDescription(TableItem item) {
        if (item != null) {
            Object data = item.getData();
            if (data instanceof AvdInfo) {
                String newTooltip = ((AvdInfo) data).getPath();
                mDescription.setText(newTooltip == null ? "" : newTooltip);  //$NON-NLS-1$
            }
        }
    }
}
