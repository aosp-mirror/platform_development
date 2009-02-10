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

import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.EventValueDescription;
import com.android.ddmlib.log.InvalidTypeException;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.TableHelper;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.ArrayList;
import java.util.Calendar;

public class DisplayLog extends EventDisplay {
    public DisplayLog(String name) {
        super(name);
    }

    private final static String PREFS_COL_DATE = "EventLogPanel.log.Col1"; //$NON-NLS-1$
    private final static String PREFS_COL_PID = "EventLogPanel.log.Col2"; //$NON-NLS-1$
    private final static String PREFS_COL_EVENTTAG = "EventLogPanel.log.Col3"; //$NON-NLS-1$
    private final static String PREFS_COL_VALUENAME = "EventLogPanel.log.Col4"; //$NON-NLS-1$
    private final static String PREFS_COL_VALUE = "EventLogPanel.log.Col5"; //$NON-NLS-1$
    private final static String PREFS_COL_TYPE = "EventLogPanel.log.Col6"; //$NON-NLS-1$

    /**
     * Resets the display.
     */
    @Override
    void resetUI() {
        mLogTable.removeAll();
    }

    /**
     * Adds event to the display.
     */
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
        addToLog(event, logParser);
    }

    /**
     * Creates the UI for the event display.
     *
     * @param parent    the parent composite.
     * @param logParser the current log parser.
     * @return the created control (which may have children).
     */
    @Override
    Control createComposite(Composite parent, EventLogParser logParser, ILogColumnListener listener) {
        return createLogUI(parent, listener);
    }

    /**
     * Adds an {@link EventContainer} to the log.
     *
     * @param event     the event.
     * @param logParser the log parser.
     */
    private void addToLog(EventContainer event, EventLogParser logParser) {
        ScrollBar bar = mLogTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();

        // get the date.
        Calendar c = Calendar.getInstance();
        long msec = (long) event.sec * 1000L;
        c.setTimeInMillis(msec);

        // convert the time into a string
        String date = String.format("%1$tF %1$tT", c);

        String eventName = logParser.getTagMap().get(event.mTag);
        String pidName = Integer.toString(event.pid);

        // get the value description
        EventValueDescription[] valueDescription = logParser.getEventInfoMap().get(event.mTag);
        if (valueDescription != null) {
            for (int i = 0; i < valueDescription.length; i++) {
                EventValueDescription description = valueDescription[i];
                try {
                    String value = event.getValueAsString(i);

                    logValue(date, pidName, eventName, description.getName(), value,
                            description.getEventValueType(), description.getValueType());
                } catch (InvalidTypeException e) {
                    logValue(date, pidName, eventName, description.getName(), e.getMessage(),
                            description.getEventValueType(), description.getValueType());
                }
            }

            // scroll if needed, by showing the last item
            if (scroll) {
                int itemCount = mLogTable.getItemCount();
                if (itemCount > 0) {
                    mLogTable.showItem(mLogTable.getItem(itemCount - 1));
                }
            }
        }
    }

    /**
     * Adds an {@link EventContainer} to the log. Only add the values/occurrences defined by
     * the list of descriptors. If an event is configured to be displayed by value and occurrence,
     * only the values are displayed (as they mark an event occurrence anyway).
     * <p/>This method is only called when at least one of the descriptor list is non empty.
     *
     * @param event
     * @param logParser
     * @param valueDescriptors
     * @param occurrenceDescriptors
     */
    protected void addToLog(EventContainer event, EventLogParser logParser,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {
        ScrollBar bar = mLogTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();

        // get the date.
        Calendar c = Calendar.getInstance();
        long msec = (long) event.sec * 1000L;
        c.setTimeInMillis(msec);

        // convert the time into a string
        String date = String.format("%1$tF %1$tT", c);

        String eventName = logParser.getTagMap().get(event.mTag);
        String pidName = Integer.toString(event.pid);

        if (valueDescriptors.size() > 0) {
            for (ValueDisplayDescriptor descriptor : valueDescriptors) {
                logDescriptor(event, descriptor, date, pidName, eventName, logParser);
            }
        } else {
            // we display the event. Since the StringBuilder contains the header (date, event name,
            // pid) at this point, there isn't anything else to display.
        }

        // scroll if needed, by showing the last item
        if (scroll) {
            int itemCount = mLogTable.getItemCount();
            if (itemCount > 0) {
                mLogTable.showItem(mLogTable.getItem(itemCount - 1));
            }
        }
    }


    /**
     * Logs a value in the ui.
     *
     * @param date
     * @param pid
     * @param event
     * @param valueName
     * @param value
     * @param eventValueType
     * @param valueType
     */
    private void logValue(String date, String pid, String event, String valueName,
            String value, EventContainer.EventValueType eventValueType, EventValueDescription.ValueType valueType) {

        TableItem item = new TableItem(mLogTable, SWT.NONE);
        item.setText(0, date);
        item.setText(1, pid);
        item.setText(2, event);
        item.setText(3, valueName);
        item.setText(4, value);

        String type;
        if (valueType != EventValueDescription.ValueType.NOT_APPLICABLE) {
            type = String.format("%1$s, %2$s", eventValueType.toString(), valueType.toString());
        } else {
            type = eventValueType.toString();
        }

        item.setText(5, type);
    }

    /**
     * Logs a value from an {@link EventContainer} as defined by the {@link ValueDisplayDescriptor}.
     *
     * @param event      the EventContainer
     * @param descriptor the ValueDisplayDescriptor defining which value to display.
     * @param date       the date of the event in a string.
     * @param pidName
     * @param eventName
     * @param logParser
     */
    private void logDescriptor(EventContainer event, ValueDisplayDescriptor descriptor,
            String date, String pidName, String eventName, EventLogParser logParser) {

        String value;
        try {
            value = event.getValueAsString(descriptor.valueIndex);
        } catch (InvalidTypeException e) {
            value = e.getMessage();
        }

        EventValueDescription[] values = logParser.getEventInfoMap().get(event.mTag);

        EventValueDescription valueDescription = values[descriptor.valueIndex];

        logValue(date, pidName, eventName, descriptor.valueName, value,
                valueDescription.getEventValueType(), valueDescription.getValueType());
    }

    /**
     * Creates the UI for a log display.
     *
     * @param parent   the parent {@link Composite}
     * @param listener the {@link ILogColumnListener} to notify on column resize events.
     * @return the top Composite of the UI.
     */
    private Control createLogUI(Composite parent, final ILogColumnListener listener) {
        Composite mainComp = new Composite(parent, SWT.NONE);
        GridLayout gl;
        mainComp.setLayout(gl = new GridLayout(1, false));
        gl.marginHeight = gl.marginWidth = 0;
        mainComp.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                mLogTable = null;
            }
        });

        Label l = new Label(mainComp, SWT.CENTER);
        l.setText(mName);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mLogTable = new Table(mainComp, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL |
                SWT.BORDER);
        mLogTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        IPreferenceStore store = DdmUiPreferences.getStore();

        TableColumn col = TableHelper.createTableColumn(
                mLogTable, "Time",
                SWT.LEFT, "0000-00-00 00:00:00", PREFS_COL_DATE, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(0, (TableColumn) source);
                }
            }
        });

        col = TableHelper.createTableColumn(
                mLogTable, "pid",
                SWT.LEFT, "0000", PREFS_COL_PID, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(1, (TableColumn) source);
                }
            }
        });

        col = TableHelper.createTableColumn(
                mLogTable, "Event",
                SWT.LEFT, "abcdejghijklmno", PREFS_COL_EVENTTAG, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(2, (TableColumn) source);
                }
            }
        });

        col = TableHelper.createTableColumn(
                mLogTable, "Name",
                SWT.LEFT, "Process Name", PREFS_COL_VALUENAME, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(3, (TableColumn) source);
                }
            }
        });

        col = TableHelper.createTableColumn(
                mLogTable, "Value",
                SWT.LEFT, "0000000", PREFS_COL_VALUE, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(4, (TableColumn) source);
                }
            }
        });

        col = TableHelper.createTableColumn(
                mLogTable, "Type",
                SWT.LEFT, "long, seconds", PREFS_COL_TYPE, store); //$NON-NLS-1$
        col.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                Object source = e.getSource();
                if (source instanceof TableColumn) {
                    listener.columnResized(5, (TableColumn) source);
                }
            }
        });

        mLogTable.setHeaderVisible(true);
        mLogTable.setLinesVisible(true);

        return mainComp;
    }

    /**
     * Resizes the <code>index</code>-th column of the log {@link Table} (if applicable).
     * <p/>
     * This does nothing if the <code>Table</code> object is <code>null</code> (because the display
     * type does not use a column) or if the <code>index</code>-th column is in fact the originating
     * column passed as argument.
     *
     * @param index        the index of the column to resize
     * @param sourceColumn the original column that was resize, and on which we need to sync the
     *                     index-th column width.
     */
    @Override
    void resizeColumn(int index, TableColumn sourceColumn) {
        if (mLogTable != null) {
            TableColumn col = mLogTable.getColumn(index);
            if (col != sourceColumn) {
                col.setWidth(sourceColumn.getWidth());
            }
        }
    }

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_LOG_ALL;
    }
}
