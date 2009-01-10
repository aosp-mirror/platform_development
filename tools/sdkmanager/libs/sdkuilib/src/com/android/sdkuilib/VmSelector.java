/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdkuilib;

import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.vm.VmManager.VmInfo;

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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;


/**
 * The VM selector is a table that is added to the given parent composite.
 * <p/>
 * To use, create it using {@link #VmSelector(Composite, VmInfo[], boolean)} then
 * call {@link #setSelection(VmInfo)}, {@link #setSelectionListener(SelectionListener)}
 * and finally use {@link #getFirstSelected()} or {@link #getAllSelected()} to retrieve the
 * selection.
 */
public final class VmSelector {
    
    private VmInfo[] mVms;
    private final boolean mAllowMultipleSelection;
    private SelectionListener mSelectionListener;
    private Table mTable;
    private Label mDescription;

    /**
     * Creates a new SDK Target Selector.
     * 
     * @param parent The parent composite where the selector will be added.
     * @param vms The list of vms. This is <em>not</em> copied, the caller must not modify.
     * @param allowMultipleSelection True if more than one SDK target can be selected at the same
     *        time.
     */
    public VmSelector(Composite parent, VmInfo[] vms, boolean allowMultipleSelection) {
        mVms = vms;

        // Layout has 1 column
        Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout());
        group.setLayoutData(new GridData(GridData.FILL_BOTH));
        group.setFont(parent.getFont());
        
        mAllowMultipleSelection = allowMultipleSelection;
        mTable = new Table(group, SWT.CHECK | SWT.FULL_SELECTION | SWT.SINGLE | SWT.BORDER);
        mTable.setHeaderVisible(true);
        mTable.setLinesVisible(false);

        GridData data = new GridData();
        data.grabExcessVerticalSpace = true;
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        data.verticalAlignment = GridData.FILL;
        mTable.setLayoutData(data);

        mDescription = new Label(group, SWT.WRAP);
        mDescription.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // create the table columns
        final TableColumn column0 = new TableColumn(mTable, SWT.NONE);
        column0.setText("VM Name");
        final TableColumn column1 = new TableColumn(mTable, SWT.NONE);
        column1.setText("Target Name");
        final TableColumn column2 = new TableColumn(mTable, SWT.NONE);
        column2.setText("API Level");
        final TableColumn column3 = new TableColumn(mTable, SWT.NONE);
        column3.setText("SDK");

        adjustColumnsWidth(mTable, column0, column1, column2, column3);
        setupSelectionListener(mTable);
        fillTable(mTable, null /* target filter */);
        setupTooltip(mTable);
    }
    
    /**
     * Sets a new set of VM, with an optional filter.
     * <p/>This must be called from the UI thread.
     * 
     * @param vms The list of vms. This is <em>not</em> copied, the caller must not modify.
     * @param filter An IAndroidTarget. If non-null, only VM whose target are compatible with the
     * filter target will displayed an available for selection.
     */
    public void setVms(VmInfo[] vms, IAndroidTarget filter) {
        mVms = vms;
        fillTable(mTable, filter);
    }

    /**
     * Returns the list of known Vms.
     * <p/>
     * This is not a copy. Callers must <em>not</em> modify this array.
     */
    public VmInfo[] getVms() {
        return mVms;
    }

    /**
     * Sets a selection listener. Set it to null to remove it.
     * The listener will be called <em>after</em> this table processed its selection
     * events so that the caller can see the updated state.
     * <p/>
     * The event's item contains a {@link TableItem}.
     * The {@link TableItem#getData()} contains an {@link IAndroidTarget}.
     * <p/>
     * It is recommended that the caller uses the {@link #getFirstSelected()} and
     * {@link #getAllSelected()} methods instead.
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
    public boolean setSelection(VmInfo target) {
        boolean found = false;
        boolean modified = false;
        for (TableItem i : mTable.getItems()) {
            if ((VmInfo) i.getData() == target) {
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
        
        if (modified && mSelectionListener != null) {
            mSelectionListener.widgetSelected(null);
        }
        
        return found;
    }

    /**
     * Returns all selected items.
     * This is useful when the table is in multiple-selection mode.
     * 
     * @see #getFirstSelected()
     * @return An array of selected items. The list can be empty but not null.
     */
    public VmInfo[] getAllSelected() {
        ArrayList<IAndroidTarget> list = new ArrayList<IAndroidTarget>();
        for (TableItem i : mTable.getItems()) {
            if (i.getChecked()) {
                list.add((IAndroidTarget) i.getData());
            }
        }
        return list.toArray(new VmInfo[list.size()]);
    }

    /**
     * Returns the first selected item.
     * This is useful when the table is in single-selection mode.
     * 
     * @see #getAllSelected()
     * @return The first selected item or null.
     */
    public VmInfo getFirstSelected() {
        for (TableItem i : mTable.getItems()) {
            if (i.getChecked()) {
                return (VmInfo) i.getData();
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
                if (!mAllowMultipleSelection && item.getChecked()) {
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
     * Fills the table with all VM.
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
        if (mVms != null && mVms.length > 0) {
            table.setEnabled(true);
            for (VmInfo vm : mVms) {
                if (filter == null || filter.isCompatibleBaseFor(vm.getTarget())) {
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setData(vm);
                    item.setText(0, vm.getName());
                    IAndroidTarget target = vm.getTarget();
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
            item.setText(1, "No VM available");
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
     * Updates the description label with the path of the item's VM, if any.
     */
    private void updateDescription(TableItem item) {
        if (item != null) {
            Object data = item.getData();
            if (data instanceof VmInfo) {
                String newTooltip = ((VmInfo) data).getPath();
                mDescription.setText(newTooltip == null ? "" : newTooltip);  //$NON-NLS-1$
            }
        }
    }
}
