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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;


/**
 * The SDK target selector is a table that is added to the given parent composite.
 * <p/>
 * To use, create it using {@link #SdkTargetSelector(Composite, IAndroidTarget[], boolean)} then
 * call {@link #setSelection(IAndroidTarget)}, {@link #setSelectionListener(SelectionListener)}
 * and finally use {@link #getSelected()} to retrieve the
 * selection.
 */
public class SdkTargetSelector {

    private IAndroidTarget[] mTargets;
    private final boolean mAllowSelection;
    private SelectionListener mSelectionListener;
    private Table mTable;
    private Label mDescription;
    private Composite mInnerGroup;

    /**
     * Creates a new SDK Target Selector.
     *
     * @param parent The parent composite where the selector will be added.
     * @param targets The list of targets. This is <em>not</em> copied, the caller must not modify.
     *                Targets can be null or an empty array, in which case the table is disabled.
     */
    public SdkTargetSelector(Composite parent, IAndroidTarget[] targets) {
        this(parent, targets, true /*allowSelection*/);
    }

    /**
     * Creates a new SDK Target Selector.
     *
     * @param parent The parent composite where the selector will be added.
     * @param targets The list of targets. This is <em>not</em> copied, the caller must not modify.
     *                Targets can be null or an empty array, in which case the table is disabled.
     * @param allowSelection True if selection is enabled.
     */
    public SdkTargetSelector(Composite parent, IAndroidTarget[] targets, boolean allowSelection) {
        // Layout has 1 column
        mInnerGroup = new Composite(parent, SWT.NONE);
        mInnerGroup.setLayout(new GridLayout());
        mInnerGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        mInnerGroup.setFont(parent.getFont());

        mAllowSelection = allowSelection;
        int style = SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION;
        if (allowSelection) {
            style |= SWT.CHECK;
        }
        mTable = new Table(mInnerGroup, style);
        mTable.setHeaderVisible(true);
        mTable.setLinesVisible(false);

        GridData data = new GridData();
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mTable.setLayoutData(data);

        mDescription = new Label(mInnerGroup, SWT.WRAP);
        mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // create the table columns
        final TableColumn column0 = new TableColumn(mTable, SWT.NONE);
        column0.setText("Target Name");
        final TableColumn column1 = new TableColumn(mTable, SWT.NONE);
        column1.setText("Vendor");
        final TableColumn column2 = new TableColumn(mTable, SWT.NONE);
        column2.setText("Platform");
        final TableColumn column3 = new TableColumn(mTable, SWT.NONE);
        column3.setText("API Level");

        adjustColumnsWidth(mTable, column0, column1, column2, column3);
        setupSelectionListener(mTable);
        setTargets(targets);
        setupTooltip(mTable);
    }

    /**
     * Returns the layout data of the inner composite widget that contains the target selector.
     * By default the layout data is set to a {@link GridData} with a {@link GridData#FILL_BOTH}
     * mode.
     * <p/>
     * This can be useful if you want to change the {@link GridData#horizontalSpan} for example.
     */
    public Object getLayoutData() {
        return mInnerGroup.getLayoutData();
    }

    /**
     * Returns the list of known targets.
     * <p/>
     * This is not a copy. Callers must <em>not</em> modify this array.
     */
    public IAndroidTarget[] getTargets() {
        return mTargets;
    }

    /**
     * Changes the targets of the SDK Target Selector.
     *
     * @param targets The list of targets. This is <em>not</em> copied, the caller must not modify.
     */
    public void setTargets(IAndroidTarget[] targets) {
        mTargets = targets;
        fillTable(mTable);
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
     * @param target the target to be selection
     * @return true if the target could be selected, false otherwise.
     */
    public boolean setSelection(IAndroidTarget target) {
        if (!mAllowSelection) {
            return false;
        }

        boolean found = false;
        boolean modified = false;

        if (mTable != null && !mTable.isDisposed()) {
            for (TableItem i : mTable.getItems()) {
                if ((IAndroidTarget) i.getData() == target) {
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

        return found;
    }

    /**
     * Returns the selected item.
     *
     * @return The selected item or null.
     */
    public IAndroidTarget getSelected() {
        if (mTable == null || mTable.isDisposed()) {
            return null;
        }

        for (TableItem i : mTable.getItems()) {
            if (i.getChecked()) {
                return (IAndroidTarget) i.getData();
            }
        }
        return null;
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
                column2.setWidth(r.width * 15 / 100); // 15%
                column3.setWidth(r.width * 10 / 100); // 10%
            }
        });
    }


    /**
     * Creates a selection listener that will check or uncheck the whole line when
     * double-clicked (aka "the default selection").
     */
    private void setupSelectionListener(final Table table) {
        if (!mAllowSelection) {
            return;
        }

        // Add a selection listener that will check/uncheck items when they are double-clicked
        table.addSelectionListener(new SelectionListener() {
            /** Default selection means double-click on "most" platforms */
            public void widgetDefaultSelected(SelectionEvent e) {
                if (e.item instanceof TableItem) {
                    TableItem i = (TableItem) e.item;
                    i.setChecked(!i.getChecked());
                    enforceSingleSelection(i);
                    updateDescription(i);
                }

                if (mSelectionListener != null) {
                    mSelectionListener.widgetDefaultSelected(e);
                }
            }

            public void widgetSelected(SelectionEvent e) {
                if (e.item instanceof TableItem) {
                    TableItem i = (TableItem) e.item;
                    enforceSingleSelection(i);
                    updateDescription(i);
                }

                if (mSelectionListener != null) {
                    mSelectionListener.widgetSelected(e);
                }
            }

            /**
             * If we're not in multiple selection mode, uncheck all other
             * items when this one is selected.
             */
            private void enforceSingleSelection(TableItem item) {
                if (item.getChecked()) {
                    Table parentTable = item.getParent();
                    for (TableItem i2 : parentTable.getItems()) {
                        if (i2 != item && i2.getChecked()) {
                            i2.setChecked(false);
                        }
                    }
                }
            }
        });
    }


    /**
     * Fills the table with all SDK targets.
     * The table columns are:
     * <ul>
     * <li>column 0: sdk name
     * <li>column 1: sdk vendor
     * <li>column 2: sdk api name
     * <li>column 3: sdk version
     * </ul>
     */
    private void fillTable(final Table table) {

        if (table == null || table.isDisposed()) {
            return;
        }

        table.removeAll();

        if (mTargets != null && mTargets.length > 0) {
            table.setEnabled(true);
            for (IAndroidTarget target : mTargets) {
                TableItem item = new TableItem(table, SWT.NONE);
                item.setData(target);
                item.setText(0, target.getName());
                item.setText(1, target.getVendor());
                item.setText(2, target.getVersionName());
                item.setText(3, target.getVersion().getApiString());
            }
        } else {
            table.setEnabled(false);
            TableItem item = new TableItem(table, SWT.NONE);
            item.setData(null);
            item.setText(0, "--");
            item.setText(1, "No target available");
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

        if (table == null || table.isDisposed()) {
            return;
        }

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
     * Updates the description label with the description of the item's android target, if any.
     */
    private void updateDescription(TableItem item) {
        if (item != null) {
            Object data = item.getData();
            if (data instanceof IAndroidTarget) {
                String newTooltip = ((IAndroidTarget) data).getDescription();
                mDescription.setText(newTooltip == null ? "" : newTooltip);  //$NON-NLS-1$
            }
        }
    }

    /** Enables or disables the controls. */
    public void setEnabled(boolean enabled) {
        if (mInnerGroup != null && mTable != null && !mTable.isDisposed()) {
            enableControl(mInnerGroup, enabled);
        }
    }

    /** Enables or disables controls; recursive for composite controls. */
    private void enableControl(Control c, boolean enabled) {
        c.setEnabled(enabled);
        if (c instanceof Composite)
        for (Control c2 : ((Composite) c).getChildren()) {
            enableControl(c2, enabled);
        }
    }

}
