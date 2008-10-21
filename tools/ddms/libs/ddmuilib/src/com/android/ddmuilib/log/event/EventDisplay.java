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

import com.android.ddmlib.Log;
import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventContainer.CompareMethod;
import com.android.ddmlib.log.EventContainer.EventValueType;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.EventValueDescription;
import com.android.ddmlib.log.EventValueDescription.ValueType;
import com.android.ddmlib.log.InvalidTypeException;
import com.android.ddmuilib.DdmUiPreferences;
import com.android.ddmuilib.TableHelper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.experimental.swt.SWTUtils;
import org.jfree.util.ShapeUtilities;

import java.awt.Color;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Represents a custom display of one or more events.
 */
final class EventDisplay {
    
    private final static String DISPLAY_DATA_STORAGE_SEPARATOR = ":"; //$NON-NLS-1$
    private final static String PID_STORAGE_SEPARATOR = ","; //$NON-NLS-1$
    private final static String DESCRIPTOR_STORAGE_SEPARATOR = "$"; //$NON-NLS-1$
    private final static String DESCRIPTOR_DATA_STORAGE_SEPARATOR = "!"; //$NON-NLS-1$
    
    private final static String PREFS_COL_DATE = "EventLogPanel.log.Col1"; //$NON-NLS-1$
    private final static String PREFS_COL_PID = "EventLogPanel.log.Col2"; //$NON-NLS-1$
    private final static String PREFS_COL_EVENTTAG = "EventLogPanel.log.Col3"; //$NON-NLS-1$
    private final static String PREFS_COL_VALUENAME = "EventLogPanel.log.Col4"; //$NON-NLS-1$
    private final static String PREFS_COL_VALUE = "EventLogPanel.log.Col5"; //$NON-NLS-1$
    private final static String PREFS_COL_TYPE = "EventLogPanel.log.Col6"; //$NON-NLS-1$

    private final static String FILTER_VALUE_NULL = "<null>"; //$NON-NLS-1$

    public final static int DISPLAY_TYPE_LOG_ALL = 0;
    public final static int DISPLAY_TYPE_FILTERED_LOG = 1;
    public final static int DISPLAY_TYPE_GRAPH = 2;
    public final static int DISPLAY_TYPE_SYNC = 3;
    public final static int DISPLAY_TYPE_SYNC_HIST = 4;

    private final static int EVENT_CHECK_FAILED = 0;
    private final static int EVENT_CHECK_SAME_TAG = 1;
    private final static int EVENT_CHECK_SAME_VALUE = 2;
    
    interface ILogColumnListener {
        void columnResized(int index, TableColumn sourceColumn);
    }

    /**
     * Describes an event to be displayed. 
     */
    static class OccurrenceDisplayDescriptor {
        
        int eventTag = -1;
        int seriesValueIndex = -1;
        boolean includePid = false;
        int filterValueIndex = -1;
        CompareMethod filterCompareMethod = CompareMethod.EQUAL_TO;
        Object filterValue = null;

        OccurrenceDisplayDescriptor() {
        }

        OccurrenceDisplayDescriptor(OccurrenceDisplayDescriptor descriptor) {
            replaceWith(descriptor);
        }

        OccurrenceDisplayDescriptor(int eventTag) {
            this.eventTag = eventTag;
        }

        OccurrenceDisplayDescriptor(int eventTag, int seriesValueIndex) {
            this.eventTag = eventTag;
            this.seriesValueIndex = seriesValueIndex;
        }

        void replaceWith(OccurrenceDisplayDescriptor descriptor) {
            eventTag = descriptor.eventTag;
            seriesValueIndex = descriptor.seriesValueIndex;
            includePid = descriptor.includePid;
            filterValueIndex = descriptor.filterValueIndex;
            filterCompareMethod = descriptor.filterCompareMethod;
            filterValue = descriptor.filterValue;
        }

        /**
         * Loads the descriptor parameter from a storage string. The storage string must have
         * been generated with {@link #getStorageString()}.
         * @param storageString the storage string
         */
        final void loadFrom(String storageString) {
            String[] values = storageString.split(Pattern.quote(DESCRIPTOR_DATA_STORAGE_SEPARATOR));
            loadFrom(values, 0);
        }
        
        /**
         * Loads the parameters from an array of strings.
         * @param storageStrings the strings representing each parameter.
         * @param index the starting index in the array of strings.
         * @return the new index in the array.
         */
        protected int loadFrom(String[] storageStrings, int index) {
            eventTag = Integer.parseInt(storageStrings[index++]);
            seriesValueIndex = Integer.parseInt(storageStrings[index++]);
            includePid = Boolean.parseBoolean(storageStrings[index++]);
            filterValueIndex = Integer.parseInt(storageStrings[index++]);
            try {
                filterCompareMethod = CompareMethod.valueOf(storageStrings[index++]);
            } catch (IllegalArgumentException e) {
                // if the name does not match any known CompareMethod, we init it to the default one
                filterCompareMethod = CompareMethod.EQUAL_TO;
            }
            String value = storageStrings[index++];
            if (filterValueIndex != -1 && FILTER_VALUE_NULL.equals(value) == false) {
                filterValue = EventValueType.getObjectFromStorageString(value);
            }

            return index;
        }

        /**
         * Returns the storage string for the receiver.
         */
        String getStorageString() {
            StringBuilder sb = new StringBuilder();
            sb.append(eventTag);
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(seriesValueIndex);
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(Boolean.toString(includePid));
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(filterValueIndex);
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(filterCompareMethod.name());
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            if (filterValue != null) {
                String value = EventValueType.getStorageString(filterValue);
                if (value != null) {
                    sb.append(value);
                } else {
                    sb.append(FILTER_VALUE_NULL);
                }
            } else {
                sb.append(FILTER_VALUE_NULL);
            }

            return sb.toString();
        }
    }

    /**
     * Describes an event value to be displayed. 
     */
    static final class ValueDisplayDescriptor extends OccurrenceDisplayDescriptor {
        String valueName;
        int valueIndex = -1;

        ValueDisplayDescriptor() {
            super();
        }

        ValueDisplayDescriptor(ValueDisplayDescriptor descriptor) {
            super();
            replaceWith(descriptor);
        }

        ValueDisplayDescriptor(int eventTag, String valueName, int valueIndex) {
            super(eventTag);
            this.valueName = valueName;
            this.valueIndex = valueIndex;
        }

        ValueDisplayDescriptor(int eventTag, String valueName, int valueIndex,
                int seriesValueIndex) {
            super(eventTag, seriesValueIndex);
            this.valueName = valueName;
            this.valueIndex = valueIndex;
        }

        @Override
        void replaceWith(OccurrenceDisplayDescriptor descriptor) {
            super.replaceWith(descriptor);
            if (descriptor instanceof ValueDisplayDescriptor) {
                ValueDisplayDescriptor valueDescriptor = (ValueDisplayDescriptor)descriptor;
                valueName = valueDescriptor.valueName;
                valueIndex = valueDescriptor.valueIndex;
            }
        }

        /**
         * Loads the parameters from an array of strings.
         * @param storageStrings the strings representing each parameter.
         * @param index the starting index in the array of strings.
         * @return the new index in the array.
         */
        @Override
        protected int loadFrom(String[] storageStrings, int index) {
            index = super.loadFrom(storageStrings, index);
            valueName = storageStrings[index++];
            valueIndex = Integer.parseInt(storageStrings[index++]);
            return index;
        }

        /**
         * Returns the storage string for the receiver.
         */
        @Override
        String getStorageString() {
            String superStorage = super.getStorageString();

            StringBuilder sb = new StringBuilder();
            sb.append(superStorage);
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(valueName);
            sb.append(DESCRIPTOR_DATA_STORAGE_SEPARATOR);
            sb.append(valueIndex);

            return sb.toString();
        }
    }

    /* ==================
     * Event Display parameters.
     * ================== */
    private String mName;
    
    private int mDisplayType = DISPLAY_TYPE_GRAPH;
    private boolean mPidFiltering = false;

    private ArrayList<Integer> mPidFilterList = null;
    
    private final ArrayList<ValueDisplayDescriptor> mValueDescriptors =
        new ArrayList<ValueDisplayDescriptor>(); 
    private final ArrayList<OccurrenceDisplayDescriptor> mOccurrenceDescriptors =
        new ArrayList<OccurrenceDisplayDescriptor>();

    /* ==================
     * Event Display members for display purpose.
     * ================== */
    // chart objects
    /** This is a map of (descriptor, map2) where map2 is a map of (pid, chart-series) */
    private final HashMap<ValueDisplayDescriptor, HashMap<Integer, TimeSeries>> mValueDescriptorSeriesMap =
        new HashMap<ValueDisplayDescriptor, HashMap<Integer, TimeSeries>>();
    /** This is a map of (descriptor, map2) where map2 is a map of (pid, chart-series) */
    private final HashMap<OccurrenceDisplayDescriptor, HashMap<Integer, TimeSeries>> mOcurrenceDescriptorSeriesMap =
        new HashMap<OccurrenceDisplayDescriptor, HashMap<Integer, TimeSeries>>();

    /** This is a map of (ValueType, dataset) */
    private final HashMap<ValueType, TimeSeriesCollection> mValueTypeDataSetMap =
        new HashMap<ValueType, TimeSeriesCollection>();

    private JFreeChart mChart;
    private TimeSeriesCollection mOccurrenceDataSet;
    private int mDataSetCount;
    private ChartComposite mChartComposite;
    private long mMaximumChartItemAge = -1;
    private long mHistWidth = 1;

    // log objects.
    private Table mLogTable;

    /* ==================
     * Misc data.
     * ================== */
    private int mValueDescriptorCheck = EVENT_CHECK_FAILED;

    /**
     * Loads a new {@link EventDisplay} from a storage string. The string must have been created
     * with {@link #getStorageString()}.
     * @param storageString the storage string
     * @return a new {@link EventDisplay} or null if the load failed.
     */
    static EventDisplay load(String storageString) {
        EventDisplay ed = new EventDisplay();
        if (ed.loadFrom(storageString)) {
            return ed;
        }

        return null;
    }

    EventDisplay(String name) {
        mName = name;
    }

    /**
     * Builds an {@link EventDisplay}.
     * @param name the name of the display
     * @param displayType the display type: {@link #DISPLAY_TYPE_GRAPH} or
     * {@value #DISPLAY_TYPE_FILTERED_LOG}.
     * @param filterByPid the flag indicating whether to filter by pid.
     */
    EventDisplay(String name, int displayType, boolean filterByPid) {
        mName = name;
        mDisplayType = displayType;
        mPidFiltering = filterByPid;
    }

    EventDisplay(EventDisplay from) {
        mName = from.mName;
        mDisplayType = from.mDisplayType;
        mPidFiltering = from.mPidFiltering;
        mMaximumChartItemAge = from.mMaximumChartItemAge;
        mHistWidth = from.mHistWidth;

        if (from.mPidFilterList != null) {
            mPidFilterList = new ArrayList<Integer>();
            mPidFilterList.addAll(from.mPidFilterList);
        }

        for (ValueDisplayDescriptor desc : from.mValueDescriptors) {
            mValueDescriptors.add(new ValueDisplayDescriptor(desc));
        }
        mValueDescriptorCheck = from.mValueDescriptorCheck;

        for (OccurrenceDisplayDescriptor desc : from.mOccurrenceDescriptors) {
            mOccurrenceDescriptors.add(new OccurrenceDisplayDescriptor(desc));
        }
    }

    /**
     * Returns the parameters of the receiver as a single String for storage.
     */
    String getStorageString() {
        StringBuilder sb = new StringBuilder();

        sb.append(mName);
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(mDisplayType);
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(Boolean.toString(mPidFiltering));
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(getPidStorageString());
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(getDescriptorStorageString(mValueDescriptors));
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(getDescriptorStorageString(mOccurrenceDescriptors));
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(mMaximumChartItemAge);
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(mHistWidth);
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);

        return sb.toString();
    }

    void setName(String name) {
        mName = name;
    }
    
    String getName() {
        return mName;
    }
    
    void setDisplayType(int value) {
        mDisplayType = value;
    }
    
    int getDisplayType() {
        return mDisplayType;
    }
    
    void setPidFiltering(boolean filterByPid) {
        mPidFiltering = filterByPid;
    }
    
    boolean getPidFiltering() {
        return mPidFiltering;
    }
    
    void setPidFilterList(ArrayList<Integer> pids) {
        if (mPidFiltering == false) {
            new InvalidParameterException();
        }
        
        mPidFilterList = pids;
    }
    
    ArrayList<Integer> getPidFilterList() {
        return mPidFilterList;
    }

    void addPidFiler(int pid) {
        if (mPidFiltering == false) {
            new InvalidParameterException();
        }
        
        if (mPidFilterList == null) {
            mPidFilterList = new ArrayList<Integer>();
        }
        
        mPidFilterList.add(pid);
    }

    /**
     * Returns an iterator to the list of {@link ValueDisplayDescriptor}.
     */
    Iterator<ValueDisplayDescriptor> getValueDescriptors() {
        return mValueDescriptors.iterator();
    }

    /**
     * Update checks on the descriptors. Must be called whenever a descriptor is modified outside
     * of this class.
     */
    void updateValueDescriptorCheck() {
        mValueDescriptorCheck = checkDescriptors();
    }

    /**
     * Returns an iterator to the list of {@link OccurrenceDisplayDescriptor}.
     */
    Iterator<OccurrenceDisplayDescriptor> getOccurrenceDescriptors() {
        return mOccurrenceDescriptors.iterator();
    }
    
    /**
     * Adds a descriptor. This can be a {@link OccurrenceDisplayDescriptor} or a 
     * {@link ValueDisplayDescriptor}.
     * @param descriptor the descriptor to be added.
     */
    void addDescriptor(OccurrenceDisplayDescriptor descriptor) {
        if (descriptor instanceof ValueDisplayDescriptor) {
            mValueDescriptors.add((ValueDisplayDescriptor)descriptor);
            mValueDescriptorCheck = checkDescriptors();
        } else {
            mOccurrenceDescriptors.add(descriptor);
        }
    }
    
    /**
     * Returns a descriptor by index and class (extending {@link OccurrenceDisplayDescriptor}).
     * @param descriptorClass the class of the descriptor to return.
     * @param index the index of the descriptor to return.
     * @return either a {@link OccurrenceDisplayDescriptor} or a {@link ValueDisplayDescriptor}
     * or <code>null</code> if <code>descriptorClass</code> is another class.
     */
    OccurrenceDisplayDescriptor getDescriptor(
            Class<? extends OccurrenceDisplayDescriptor> descriptorClass, int index) {

        if (descriptorClass == OccurrenceDisplayDescriptor.class) {
            return mOccurrenceDescriptors.get(index);
        } else if (descriptorClass == ValueDisplayDescriptor.class) {
            return mValueDescriptors.get(index);
        }
        
        return null;
    }
    
    /**
     * Removes a descriptor based on its class and index.
     * @param descriptorClass the class of the descriptor.
     * @param index the index of the descriptor to be removed.
     */
    void removeDescriptor(Class<? extends OccurrenceDisplayDescriptor> descriptorClass, int index) {
        if (descriptorClass == OccurrenceDisplayDescriptor.class) {
            mOccurrenceDescriptors.remove(index);
        } else if (descriptorClass == ValueDisplayDescriptor.class) {
            mValueDescriptors.remove(index);
            mValueDescriptorCheck = checkDescriptors();
        }
    }

    /**
     * Creates the UI for the event display.
     * @param parent the parent composite.
     * @param logParser the current log parser.
     * @return the created control (which may have children).
     */
    Control createComposite(final Composite parent, EventLogParser logParser,
            final ILogColumnListener listener) {
        switch (mDisplayType) {
            case DISPLAY_TYPE_LOG_ALL:
                // intended fall-through
            case DISPLAY_TYPE_FILTERED_LOG:
                return createLogUI(parent, listener);
            case DISPLAY_TYPE_GRAPH:
                // intended fall-through
            case DISPLAY_TYPE_SYNC:
                // intended fall-through
            case DISPLAY_TYPE_SYNC_HIST:
                String title = getChartTitle(logParser);
                mChart = ChartFactory.createTimeSeriesChart(
                        null,
                        null /* timeAxisLabel */,
                        null /* valueAxisLabel */,
                        null, /* dataset. set below */
                        true /* legend */,
                        false /* tooltips */,
                        false /* urls */);
                
                // get the font to make a proper title. We need to convert the swt font,
                // into an awt font.
                Font f = parent.getFont();
                FontData[] fData = f.getFontData();
                
                // event though on Mac OS there could be more than one fontData, we'll only use
                // the first one.
                FontData firstFontData = fData[0];

                java.awt.Font awtFont = SWTUtils.toAwtFont(parent.getDisplay(),
                        firstFontData, true /* ensureSameSize */);

                if (mDisplayType == DISPLAY_TYPE_SYNC) {
                    title = "Sync Status";
                } else if (mDisplayType == DISPLAY_TYPE_SYNC_HIST) {
                    title = "Sync Histogram";
                }

                mChart.setTitle(new TextTitle(title, awtFont));
                
                final XYPlot xyPlot = mChart.getXYPlot();
                xyPlot.setRangeCrosshairVisible(true);
                xyPlot.setRangeCrosshairLockedOnData(true);
                xyPlot.setDomainCrosshairVisible(true);
                xyPlot.setDomainCrosshairLockedOnData(true);

                mChart.addChangeListener(new ChartChangeListener() {
                    public void chartChanged(ChartChangeEvent event) {
                        ChartChangeEventType type = event.getType();
                        if (type == ChartChangeEventType.GENERAL) {
                            // because the value we need (rangeCrosshair and domainCrosshair) are
                            // updated on the draw, but the notification happens before the draw,
                            // we process the click in a future runnable!
                            parent.getDisplay().asyncExec(new Runnable() {
                                public void run() {
                                    processClick(xyPlot);
                                }
                            });
                        }
                    }
                });

                mChartComposite = new ChartComposite(parent, SWT.BORDER, mChart,
                        ChartComposite.DEFAULT_WIDTH,
                        ChartComposite.DEFAULT_HEIGHT,
                        ChartComposite.DEFAULT_MINIMUM_DRAW_WIDTH,
                        ChartComposite.DEFAULT_MINIMUM_DRAW_HEIGHT,
                        3000, // max draw width. We don't want it to zoom, so we put a big number
                        3000, // max draw height. We don't want it to zoom, so we put a big number
                        true,  // off-screen buffer
                        true,  // properties
                        true,  // save
                        true,  // print
                        true,  // zoom
                        true);   // tooltips

                mChartComposite.addDisposeListener(new DisposeListener() {
                    public void widgetDisposed(DisposeEvent e) {
                        mValueTypeDataSetMap.clear();
                        mDataSetCount = 0;
                        mOccurrenceDataSet = null;
                        mChart = null;
                        mChartComposite = null;
                        mValueDescriptorSeriesMap.clear();
                        mOcurrenceDescriptorSeriesMap.clear();
                    }
                });

                if (mDisplayType == DISPLAY_TYPE_SYNC) {
                    initSyncDisplay();
                } else if (mDisplayType == DISPLAY_TYPE_SYNC_HIST) {
                    initSyncHistDisplay();
                }

                return mChartComposite;
            default:
                    throw new InvalidParameterException("Unknown Display Type"); //$NON-NLS-1$
        }
    }

    private void processClick(XYPlot xyPlot) {
        double rangeValue = xyPlot.getRangeCrosshairValue();
        if (rangeValue != 0) { 
            double domainValue = xyPlot.getDomainCrosshairValue();
            
            Millisecond msec = new Millisecond(new Date((long)domainValue));
            
            // look for values in the dataset that contains data at this TimePeriod
            Set<ValueDisplayDescriptor> descKeys = mValueDescriptorSeriesMap.keySet();
         
            for (ValueDisplayDescriptor descKey : descKeys) {
                HashMap<Integer, TimeSeries> map = mValueDescriptorSeriesMap.get(descKey);
                
                Set<Integer> pidKeys = map.keySet();
                
                for (Integer pidKey : pidKeys) {
                    TimeSeries series = map.get(pidKey);
                    
                    Number value = series.getValue(msec);
                    if (value != null) {
                        // found a match. lets check against the actual value.
                        if (value.doubleValue() == rangeValue) {
                            
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the UI for a log display.
     * @param parent the parent {@link Composite}
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
                    listener.columnResized(0, (TableColumn)source);
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
                    listener.columnResized(1, (TableColumn)source);
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
                    listener.columnResized(2, (TableColumn)source);
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
                    listener.columnResized(3, (TableColumn)source);
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
                    listener.columnResized(4, (TableColumn)source);
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
                    listener.columnResized(5, (TableColumn)source);
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
     * @param index the index of the column to resize
     * @param sourceColumn the original column that was resize, and on which we need to sync the
     * index-th column width.
     */
    void resizeColumn(int index, TableColumn sourceColumn) {
        if (mLogTable != null) {
            TableColumn col = mLogTable.getColumn(index);
            if (col != sourceColumn) {
                col.setWidth(sourceColumn.getWidth());
            }
        }
    }
    
    /**
     * Sets the current {@link EventLogParser} object.
     */
    void setNewLogParser(EventLogParser logParser) {
        if (mDisplayType == DISPLAY_TYPE_GRAPH) {
            if (mChart != null) {
                mChart.setTitle(getChartTitle(logParser));
            }
        }
    }
    
    void resetUI() {
        switch (mDisplayType) {
            case DISPLAY_TYPE_LOG_ALL:
                // intended fall-through
            case DISPLAY_TYPE_FILTERED_LOG:
                mLogTable.removeAll();
                break;
            case DISPLAY_TYPE_SYNC:
                initSyncDisplay();
                break;
            case DISPLAY_TYPE_SYNC_HIST:
                initSyncHistDisplay();
                break;
            case DISPLAY_TYPE_GRAPH:
                Collection<TimeSeriesCollection> datasets = mValueTypeDataSetMap.values();
                for (TimeSeriesCollection dataset : datasets) {
                    dataset.removeAllSeries();
                }
                if (mOccurrenceDataSet != null) {
                    mOccurrenceDataSet.removeAllSeries();
                }
                mValueDescriptorSeriesMap.clear();
                mOcurrenceDescriptorSeriesMap.clear();
                break;
            default:
                throw new InvalidParameterException("Unknown Display Type"); //$NON-NLS-1$
        }
    }
    
    /**
     * Prepares the {@link EventDisplay} for a multi event display.
     */
    void startMultiEventDisplay() {
        if (mLogTable != null) {
            mLogTable.setRedraw(false);
        }
    }

    /**
     * Finalizes the {@link EventDisplay} after a multi event display.
     */
    void endMultiEventDisplay() {
        if (mLogTable != null) {
            mLogTable.setRedraw(true);
        }
    }

    /**
     * Processes a new event. This must be called from the ui thread.
     * @param event the event to process.
     */
    void newEvent(EventContainer event, EventLogParser logParser) {
        ArrayList<ValueDisplayDescriptor> valueDescriptors =
            new ArrayList<ValueDisplayDescriptor>();

        ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors =
            new ArrayList<OccurrenceDisplayDescriptor>();

        if (filterEvent(event, valueDescriptors, occurrenceDescriptors)) {
            switch (mDisplayType) {
                case DISPLAY_TYPE_LOG_ALL:
                    addToLog(event, logParser);
                    break;
                case DISPLAY_TYPE_FILTERED_LOG:
                    addToLog(event, logParser, valueDescriptors, occurrenceDescriptors);
                    break;
                case DISPLAY_TYPE_SYNC:
                    updateSyncDisplay(event);
                    break;
                case DISPLAY_TYPE_SYNC_HIST:
                    updateSyncHistDisplay(event);
                    break;
                case DISPLAY_TYPE_GRAPH:
                    updateChart(event, logParser, valueDescriptors, occurrenceDescriptors);
                    break;
                default:
                    throw new InvalidParameterException("Unknown Display Type"); //$NON-NLS-1$
            }
        }
    }
    
    /**
     * Returns the {@link Table} object used to display events, if any.
     * @return a Table object or <code>null</code>.
     */
    Table getTable() {
        return mLogTable;
    }

    /** Private constructor used for loading from storage */
    private EventDisplay() {
        // nothing to be done here.
    }
    
    /**
     * Loads the {@link EventDisplay} parameters from the storage string.
     * @param storageString the string containing the parameters.
     */
    private boolean loadFrom(String storageString) {
        if (storageString.length() > 0) {
            // the storage string is separated by ':'
            String[] values = storageString.split(Pattern.quote(DISPLAY_DATA_STORAGE_SEPARATOR));
    
            try {
                int index = 0;
    
                mName = values[index++];
                mDisplayType = Integer.parseInt(values[index++]);
                mPidFiltering = Boolean.parseBoolean(values[index++]);

                // because empty sections are removed by String.split(), we have to check
                // the index for those.
                if (index < values.length) {
                    loadPidFilters(values[index++]);
                }
    
                if (index < values.length) {
                    loadValueDescriptors(values[index++]);
                }
    
                if (index < values.length) {
                    loadOccurrenceDescriptors(values[index++]);
                }
                
                updateValueDescriptorCheck();
                
                if (index < values.length) {
                    mMaximumChartItemAge = Long.parseLong(values[index++]);
                }

                if (index < values.length) {
                    mHistWidth = Long.parseLong(values[index++]);
                }
    
                return true;
            } catch (RuntimeException re) {
                // we'll return false below.
                Log.e("ddms", re);
            }
        }
        
        return false;
    }
    
    private String getPidStorageString() {
        if (mPidFilterList != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Integer i : mPidFilterList) {
                if (first == false) {
                    sb.append(PID_STORAGE_SEPARATOR);
                } else {
                    first = false;
                }
                sb.append(i);
            }
            
            return sb.toString();
        }
        return ""; //$NON-NLS-1$
    }

    
    private void loadPidFilters(String storageString) {
        if (storageString.length() > 0) {
            String[] values = storageString.split(Pattern.quote(PID_STORAGE_SEPARATOR));
            
            for (String value : values) {
                if (mPidFilterList == null) {
                    mPidFilterList = new ArrayList<Integer>();
                }
                mPidFilterList.add(Integer.parseInt(value));
            }
        }
    }
    
    private String getDescriptorStorageString(
            ArrayList<? extends OccurrenceDisplayDescriptor> descriptorList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (OccurrenceDisplayDescriptor descriptor : descriptorList) {
            if (first == false) {
                sb.append(DESCRIPTOR_STORAGE_SEPARATOR);
            } else {
                first = false;
            }
            sb.append(descriptor.getStorageString());
        }
        
        return sb.toString();
    }

    private void loadOccurrenceDescriptors(String storageString) {
        if (storageString.length() == 0) {
            return;
        }

        String[] values = storageString.split(Pattern.quote(DESCRIPTOR_STORAGE_SEPARATOR));
        
        for (String value : values) {
            OccurrenceDisplayDescriptor desc = new OccurrenceDisplayDescriptor();
            desc.loadFrom(value);
            mOccurrenceDescriptors.add(desc);
        }
    }
    
    private void loadValueDescriptors(String storageString) {
        if (storageString.length() == 0) {
            return;
        }

        String[] values = storageString.split(Pattern.quote(DESCRIPTOR_STORAGE_SEPARATOR));
        
        for (String value : values) {
            ValueDisplayDescriptor desc = new ValueDisplayDescriptor();
            desc.loadFrom(value);
            mValueDescriptors.add(desc);
        }
    }

    /**
     * Returns the {@link TimeSeriesCollection} for the occurrence display. If the data set is not
     * yet created, it is first allocated and set up into the {@link JFreeChart} object.
     */
    private TimeSeriesCollection getOccurrenceDataSet() {
        if (mOccurrenceDataSet == null) {
            mOccurrenceDataSet = new TimeSeriesCollection();

            XYPlot xyPlot = mChart.getXYPlot();
            xyPlot.setDataset(mDataSetCount, mOccurrenceDataSet);

            OccurrenceRenderer renderer = new OccurrenceRenderer();
            renderer.setBaseShapesVisible(false);
            xyPlot.setRenderer(mDataSetCount, renderer);
            
            mDataSetCount++;
        }
        
        return mOccurrenceDataSet;
    }
    
    /**
     * Returns a {@link TimeSeriesCollection} for a specific {@link ValueType}.
     * If the data set is not yet created, it is first allocated and set up into the
     * {@link JFreeChart} object.
     * @param type the {@link ValueType} of the data set.
     * @param accumulateValues 
     */
    private TimeSeriesCollection getValueDataset(ValueType type, boolean accumulateValues) {
        TimeSeriesCollection dataset = mValueTypeDataSetMap.get(type);
        if (dataset == null) {
            // create the data set and store it in the map
            dataset = new TimeSeriesCollection();
            mValueTypeDataSetMap.put(type, dataset);

            // create the renderer and configure it depending on the ValueType
            AbstractXYItemRenderer renderer;
            if (type == ValueType.PERCENT && accumulateValues) {
                renderer = new XYAreaRenderer();
            } else {
                XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
                r.setBaseShapesVisible(type != ValueType.PERCENT);
                
                renderer = r;
            }

            // set both the dataset and the renderer in the plot object.
            XYPlot xyPlot = mChart.getXYPlot();
            xyPlot.setDataset(mDataSetCount, dataset);
            xyPlot.setRenderer(mDataSetCount, renderer);
            
            // put a new axis label, and configure it.
            NumberAxis axis = new NumberAxis(type.toString());
            
            if (type == ValueType.PERCENT) {
                // force percent range to be (0,100) fixed.
                axis.setAutoRange(false);
                axis.setRange(0., 100.);
            }

            // for the index, we ignore the occurrence dataset
            int count = mDataSetCount;
            if (mOccurrenceDataSet != null) {
                count--;
            }

            xyPlot.setRangeAxis(count, axis);
            if ((count % 2) == 0) {
                xyPlot.setRangeAxisLocation(count, AxisLocation.BOTTOM_OR_LEFT);
            } else {
                xyPlot.setRangeAxisLocation(count, AxisLocation.TOP_OR_RIGHT);
            }
            
            // now we link the dataset and the axis
            xyPlot.mapDatasetToRangeAxis(mDataSetCount, count);
            
            mDataSetCount++;
        }

        return dataset;
    }


    /**
     * Updates the chart with the {@link EventContainer} by adding the values/occurrences defined
     * by the {@link ValueDisplayDescriptor} and {@link OccurrenceDisplayDescriptor} objects from
     * the two lists.
     * <p/>This method is only called when at least one of the descriptor list is non empty.
     * @param event
     * @param logParser
     * @param valueDescriptors
     * @param occurrenceDescriptors
     */
    private void updateChart(EventContainer event, EventLogParser logParser,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {
        Map<Integer, String> tagMap = logParser.getTagMap();

        Millisecond millisecondTime = null;
        long msec = -1;
        
        // If the event container is a cpu container (tag == 2721), and there is no descriptor
        // for the total CPU load, then we do accumulate all the values.
        boolean accumulateValues = false;
        double accumulatedValue = 0;
        
        if (event.mTag == 2721) {
            accumulateValues = true;
            for (ValueDisplayDescriptor descriptor : valueDescriptors) {
                accumulateValues &= (descriptor.valueIndex != 0); 
            }
        }
        
        for (ValueDisplayDescriptor descriptor : valueDescriptors) {
            try {
                // get the hashmap for this descriptor
                HashMap<Integer, TimeSeries> map = mValueDescriptorSeriesMap.get(descriptor);

                // if it's not there yet, we create it.
                if (map == null) {
                    map = new HashMap<Integer, TimeSeries>();
                    mValueDescriptorSeriesMap.put(descriptor, map);
                }
                
                // get the TimeSeries for this pid
                TimeSeries timeSeries = map.get(event.pid);

                // if it doesn't exist yet, we create it
                if (timeSeries == null) {
                    // get the series name
                    String seriesFullName = null;
                    String seriesLabel = getSeriesLabel(event, descriptor);

                    switch (mValueDescriptorCheck) {
                        case EVENT_CHECK_SAME_TAG:
                            seriesFullName = String.format("%1$s / %2$s", seriesLabel,
                                    descriptor.valueName);
                            break;
                        case EVENT_CHECK_SAME_VALUE:
                            seriesFullName = String.format("%1$s", seriesLabel);
                            break;
                        default:
                            seriesFullName = String.format("%1$s / %2$s: %3$s", seriesLabel,
                                    tagMap.get(descriptor.eventTag),
                                    descriptor.valueName);
                            break;
                    }

                    // get the data set for this ValueType
                    TimeSeriesCollection dataset = getValueDataset(
                            logParser.getEventInfoMap().get(event.mTag)[descriptor.valueIndex]
                                                                        .getValueType(),
                            accumulateValues);

                    // create the series
                    timeSeries = new TimeSeries(seriesFullName, Millisecond.class);
                    if (mMaximumChartItemAge != -1) {
                        timeSeries.setMaximumItemAge(mMaximumChartItemAge * 1000);
                    }
                    
                    dataset.addSeries(timeSeries);
                    
                    // add it to the map.
                    map.put(event.pid, timeSeries);
                }
                
                // update the timeSeries.
    
                // get the value from the event
                double value = event.getValueAsDouble(descriptor.valueIndex);
                
                // accumulate the values if needed.
                if (accumulateValues) {
                    accumulatedValue += value;
                    value = accumulatedValue;
                }
                
                // get the time
                if (millisecondTime == null) {
                    msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    millisecondTime = new Millisecond(new Date(msec));
                }

                // add the value to the time series
                timeSeries.addOrUpdate(millisecondTime, value);
            } catch (InvalidTypeException e) {
                // just ignore this descriptor if there's a type mismatch
            }
        }

        for (OccurrenceDisplayDescriptor descriptor : occurrenceDescriptors) {
            try {
                // get the hashmap for this descriptor
                HashMap<Integer, TimeSeries> map = mOcurrenceDescriptorSeriesMap.get(descriptor);
    
                // if it's not there yet, we create it.
                if (map == null) {
                    map = new HashMap<Integer, TimeSeries>();
                    mOcurrenceDescriptorSeriesMap.put(descriptor, map);
                }

                // get the TimeSeries for this pid
                TimeSeries timeSeries = map.get(event.pid);

                // if it doesn't exist yet, we create it.
                if (timeSeries == null) {
                    String seriesLabel = getSeriesLabel(event, descriptor);
    
                    String seriesFullName = String.format("[%1$s:%2$s]",
                            tagMap.get(descriptor.eventTag), seriesLabel);
    
                    timeSeries = new TimeSeries(seriesFullName, Millisecond.class);
                    if (mMaximumChartItemAge != -1) {
                        timeSeries.setMaximumItemAge(mMaximumChartItemAge);
                    }

                    getOccurrenceDataSet().addSeries(timeSeries);
    
                    map.put(event.pid, timeSeries);
                }
                
                // update the series
                
                // get the time
                if (millisecondTime == null) {
                    msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    millisecondTime = new Millisecond(new Date(msec));
                }
    
                // add the value to the time series
                timeSeries.addOrUpdate(millisecondTime, 0); // the value is unused
            } catch (InvalidTypeException e) {
                // just ignore this descriptor if there's a type mismatch
            }
        }
        
        // go through all the series and remove old values.
        if (msec != -1 && mMaximumChartItemAge != -1) {
            Collection<HashMap<Integer, TimeSeries>> pidMapValues =
                mValueDescriptorSeriesMap.values();

            for (HashMap<Integer, TimeSeries> pidMapValue : pidMapValues) {
                Collection<TimeSeries> seriesCollection = pidMapValue.values();
                
                for (TimeSeries timeSeries : seriesCollection) {
                    timeSeries.removeAgedItems(msec, true);
                }
            }
    
            pidMapValues = mOcurrenceDescriptorSeriesMap.values();
            for (HashMap<Integer, TimeSeries> pidMapValue : pidMapValues) {
                Collection<TimeSeries> seriesCollection = pidMapValue.values();
                
                for (TimeSeries timeSeries : seriesCollection) {
                    timeSeries.removeAgedItems(msec, true);
                }
            }
        }
    }

    /**
     * Return the series label for this event. This only contains the pid information.
     * @param event the {@link EventContainer}
     * @param descriptor the {@link OccurrenceDisplayDescriptor}
     * @return the series label.
     * @throws InvalidTypeException
     */
    private String getSeriesLabel(EventContainer event, OccurrenceDisplayDescriptor descriptor)
            throws InvalidTypeException {
        if (descriptor.seriesValueIndex != -1) {
            if (descriptor.includePid == false) {
                return event.getValueAsString(descriptor.seriesValueIndex);
            } else {
                return String.format("%1$s (%2$d)",
                        event.getValueAsString(descriptor.seriesValueIndex), event.pid);
            }
        }

        return Integer.toString(event.pid);
    }

    /**
     * Fills a list with {@link OccurrenceDisplayDescriptor} (or a subclass of it) from another
     * list if they are configured to display the {@link EventContainer}
     * @param event the event container
     * @param fullList the list with all the descriptors.
     * @param outList the list to fill.
     */
    @SuppressWarnings("unchecked")
    private void getDescriptors(EventContainer event,
            ArrayList<? extends OccurrenceDisplayDescriptor> fullList,
            ArrayList outList) {
        for (OccurrenceDisplayDescriptor descriptor : fullList) {
            try {
                // first check the event tag.
                if (descriptor.eventTag == event.mTag) {
                    // now check if we have a filter on a value
                    if (descriptor.filterValueIndex == -1 ||
                            event.testValue(descriptor.filterValueIndex, descriptor.filterValue,
                                    descriptor.filterCompareMethod)) {
                        outList.add(descriptor);
                    }
                }
            } catch (InvalidTypeException ite) {
                // if the filter for the descriptor was incorrect, we ignore the descriptor.
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                // if the index was wrong (the event content may have changed since we setup the
                // display), we do nothing but log the error
                Log.e("Event Log", String.format(
                        "ArrayIndexOutOfBoundsException occured when checking %1$d-th value of event %2$d", //$NON-NLS-1$
                        descriptor.filterValueIndex, descriptor.eventTag));
            }
        }
    }
    
    /**
     * Adds an {@link EventContainer} to the log.
     * @param event the event.
     * @param logParser the log parser.
     */
    private void addToLog(EventContainer event, EventLogParser logParser) {
        ScrollBar bar = mLogTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();
        
        // get the date.
        Calendar c = Calendar.getInstance();
        long msec = (long)event.sec * 1000L;
        c.setTimeInMillis(msec);
        
        // convert the time into a string
        String date = String.format("%1$tF %1$tT", c);

        String eventName = logParser.getTagMap().get(event.mTag);
        String pidName = Integer.toString(event.pid);
        
        // get the value description
        EventValueDescription[] valueDescription = logParser.getEventInfoMap().get(event.mTag);
        if (valueDescription != null) {
            for (int i = 0 ; i < valueDescription.length ; i++) {
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
                    mLogTable.showItem(mLogTable.getItem(itemCount-1));
                }
            }
        }
    }

    /**
     * Adds an {@link EventContainer} to the log. Only add the values/occurrences defined by
     * the list of descriptors. If an event is configured to be displayed by value and occurrence,
     * only the values are displayed (as they mark an event occurrence anyway).
     * <p/>This method is only called when at least one of the descriptor list is non empty.
     * @param event
     * @param logParser
     * @param valueDescriptors
     * @param occurrenceDescriptors
     */
    private void addToLog(EventContainer event, EventLogParser logParser,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {
        ScrollBar bar = mLogTable.getVerticalBar();
        boolean scroll = bar.getMaximum() == bar.getSelection() + bar.getThumb();

        // get the date.
        Calendar c = Calendar.getInstance();
        long msec = (long)event.sec * 1000L;
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
                mLogTable.showItem(mLogTable.getItem(itemCount-1));
            }
        }
    }

    /**
     * Logs a value from an {@link EventContainer} as defined by the {@link ValueDisplayDescriptor}.
     * @param event the EventContainer
     * @param descriptor the ValueDisplayDescriptor defining which value to display.
     * @param date the date of the event in a string.
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
     * Logs a value in the ui.
     * @param date
     * @param pid
     * @param event
     * @param valueName
     * @param value
     * @param eventValueType
     * @param valueType
     */
    private void logValue(String date, String pid, String event, String valueName,
            String value, EventValueType eventValueType, ValueType valueType) {
        
        TableItem item = new TableItem(mLogTable, SWT.NONE);
        item.setText(0, date);
        item.setText(1, pid);
        item.setText(2, event);
        item.setText(3, valueName);
        item.setText(4, value);
        
        String type;
        if (valueType != ValueType.NOT_APPLICABLE) {
            type = String.format("%1$s, %2$s", eventValueType.toString(), valueType.toString());
        } else {
            type = eventValueType.toString();
        }

        item.setText(5, type);
    }

    /**
     * Show the current value(s) of an {@link EventContainer}. The values to show are defined by
     * the {@link ValueDisplayDescriptor}s and {@link OccurrenceDisplayDescriptor}s passed in the
     * two lists.
     * @param event
     * @param logParser
     * @param valueDescriptors
     * @param occurrenceDescriptors
     */
    private void showCurrent(EventContainer event, EventLogParser logParser,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {
        // TODO Auto-generated method stub
    }

    // Values from data/etc/event-log-tags
    final int EVENT_SYNC = 2720;
    final int EVENT_TICKLE = 2742;
    final int EVENT_SYNC_DETAILS = 2743;

    /**
     * Filters the {@link EventContainer}, and fills two list of {@link ValueDisplayDescriptor}
     * and {@link OccurrenceDisplayDescriptor} configured to display the event.
     * @param event
     * @param valueDescriptors
     * @param occurrenceDescriptors
     * @return true if the event should be displayed.
     */
    private boolean filterEvent(EventContainer event,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {

        if (mDisplayType == DISPLAY_TYPE_LOG_ALL) {
            return true;
        }

        if (mDisplayType == DISPLAY_TYPE_SYNC || mDisplayType == DISPLAY_TYPE_SYNC_HIST) {
            if (event.mTag == EVENT_SYNC || event.mTag == EVENT_TICKLE ||
                    event.mTag == EVENT_SYNC_DETAILS) {
                return true;
            } else {
                return false;
            }
        }

        // test the pid first (if needed)
        if (mPidFiltering && mPidFilterList != null) {
            boolean found = false;
            for (int pid : mPidFilterList) {
                if (pid == event.pid) {
                    found = true;
                    break;
                }
            }
            
            if (found == false) {
                return false;
            }
        }

        // now get the list of matching descriptors
        getDescriptors(event, mValueDescriptors, valueDescriptors);
        getDescriptors(event, mOccurrenceDescriptors, occurrenceDescriptors);

        // and return whether there is at least one match in either list.
        return (valueDescriptors.size() > 0 || occurrenceDescriptors.size() > 0);
    }

    /**
     * Checks all the {@link ValueDisplayDescriptor} for similarity.
     * If all the event values are from the same tag, the method will return EVENT_CHECK_SAME_TAG.
     * If all the event/value are the same, the method will return EVENT_CHECK_SAME_VALUE
     * @return
     */
    private int checkDescriptors() {
        if (mValueDescriptors.size() < 2) {
            return EVENT_CHECK_SAME_VALUE;
        }

        int tag = -1;
        int index = -1;
        for (ValueDisplayDescriptor display : mValueDescriptors) {
            if (tag == -1) {
                tag = display.eventTag;
                index = display.valueIndex;
            } else {
                if (tag != display.eventTag) {
                    return EVENT_CHECK_FAILED;
                } else {
                    if (index != -1) {
                        if (index != display.valueIndex) {
                            index = -1;
                        }
                    }
                }
            }
        }

        if (index == -1) {
            return EVENT_CHECK_SAME_TAG;
        }
        
        return EVENT_CHECK_SAME_VALUE;
    }
    
    /**
     * Returns a meaningful chart title based on the value of {@link #mValueDescriptorCheck}.
     * @param logParser the logParser.
     * @return the chart title.
     */
    private String getChartTitle(EventLogParser logParser) {
        if (mValueDescriptors.size() > 0) {
            String chartDesc = null;
            switch (mValueDescriptorCheck) {
                case EVENT_CHECK_SAME_TAG:
                    if (logParser != null) {
                        chartDesc = logParser.getTagMap().get(mValueDescriptors.get(0).eventTag);
                    }
                    break;
                case EVENT_CHECK_SAME_VALUE:
                    if (logParser != null) {
                        chartDesc = String.format("%1$s / %2$s",
                                logParser.getTagMap().get(mValueDescriptors.get(0).eventTag),
                                mValueDescriptors.get(0).valueName);
                    }
                    break;
            }
            
            if (chartDesc != null) {
                return String.format("%1$s - %2$s", mName, chartDesc);
            }
        }

        return mName;
    }

    /**
     * Resets the time limit on the chart to be infinite.
     */
    void resetChartTimeLimit() {
        mMaximumChartItemAge = -1;
    }

    /**
     * Sets the time limit on the charts.
     * @param timeLimit the time limit in seconds.
     */
    void setChartTimeLimit(long timeLimit) {
        mMaximumChartItemAge = timeLimit;
    }
    
    long getChartTimeLimit() {
        return mMaximumChartItemAge;
    }

    /**
     * Resets the histogram width
     */
    void resetHistWidth() {
        mHistWidth = 1;
    }

    /**
     * Sets the histogram width
     * @param histWidth the width in hours
     */
    void setHistWidth(long histWidth) {
        mHistWidth = histWidth;
    }
    
    long getHistWidth() {
        return mHistWidth;
    }

    // Implementation of the Sync display
    // TODO: DISPLAY_TYPE_LOG, DISPLAY_TYPE_GRAPH, and DISPLAY_TYPE_SYNC should be subclasses
    // of EventDisplay.java

    private static final int CALENDAR = 0;
    private static final int GMAIL = 1;
    private static final int FEEDS = 2;
    private static final int CONTACTS = 3;
    private static final int ERRORS = 4;
    private static final int NUM_AUTHS = (CONTACTS+1);
    private static final String AUTH_NAMES[] = {"Calendar", "Gmail", "Feeds", "Contacts", "Errors"};
    private static final Color AUTH_COLORS[] = {Color.MAGENTA,  Color.GREEN, Color.BLUE, Color.ORANGE, Color.RED};

    // Information to graph for each authority
    private TimePeriodValues mDatasetsSync[];
    private List<String> mTooltipsSync[];
    private CustomXYToolTipGenerator mTooltipGenerators[];
    private TimeSeries mDatasetsSyncTickle[];

    // Dataset of error events to graph
    private TimeSeries mDatasetError;

    /**
     * Initialize the Plot and series data for the sync display.
     */
    void initSyncDisplay() {
        XYPlot xyPlot = mChart.getXYPlot();

        XYBarRenderer br = new XYBarRenderer();
        mDatasetsSync = new TimePeriodValues[NUM_AUTHS];
        mTooltipsSync = new List[NUM_AUTHS];
        mTooltipGenerators = new CustomXYToolTipGenerator[NUM_AUTHS];
        mLastDetails = "";

        TimePeriodValuesCollection tpvc = new TimePeriodValuesCollection();
        xyPlot.setDataset(tpvc);
        xyPlot.setRenderer(0, br);

        XYLineAndShapeRenderer ls = new XYLineAndShapeRenderer();
        ls.setBaseLinesVisible(false);
        mDatasetsSyncTickle = new TimeSeries[NUM_AUTHS];
        TimeSeriesCollection tsc = new TimeSeriesCollection();
        xyPlot.setDataset(1, tsc);
        xyPlot.setRenderer(1, ls);

        mDatasetError = new TimeSeries("Errors", FixedMillisecond.class);
        xyPlot.setDataset(2, new TimeSeriesCollection(mDatasetError));
        XYLineAndShapeRenderer errls = new XYLineAndShapeRenderer();
        errls.setBaseLinesVisible(false);
        errls.setSeriesPaint(0, Color.RED);        
        xyPlot.setRenderer(2, errls);

        for (int i = 0; i < NUM_AUTHS; i++) {
            br.setSeriesPaint(i, AUTH_COLORS[i]);
            ls.setSeriesPaint(i, AUTH_COLORS[i]);
            mDatasetsSync[i] = new TimePeriodValues(AUTH_NAMES[i]);
            tpvc.addSeries(mDatasetsSync[i]);
            mTooltipsSync[i] = new ArrayList<String>();
            mTooltipGenerators[i] = new CustomXYToolTipGenerator();
            br.setSeriesToolTipGenerator(i, mTooltipGenerators[i]);
            mTooltipGenerators[i].addToolTipSeries(mTooltipsSync[i]);

            mDatasetsSyncTickle[i] = new TimeSeries(AUTH_NAMES[i] + " tickle", FixedMillisecond.class);
            tsc.addSeries(mDatasetsSyncTickle[i]);
            ls.setSeriesShape(i, ShapeUtilities.createUpTriangle(2.5f));
        }
    }

    // State information while processing the event stream
    private int mLastState; // 0 if event started, 1 if event stopped
    private long mLastStartTime; // ms
    private long mLastStopTime; //ms
    private String mLastDetails;
    private int mLastEvent; // server, poll, etc

    /**
     * Updates the display with a new event.  This is the main entry point for
     * each event.  This method has the logic to tie together the start event,
     * stop event, and details event into one graph item.  Note that the details
     * can happen before or after the stop event.
     * @param event The event
     */
    private void updateSyncDisplay(EventContainer event) {
        try {
            if (event.mTag == EVENT_SYNC) {
                int state = Integer.parseInt(event.getValueAsString(1));
                if (state == 0) { // start
                    mLastStartTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    mLastState = 0;
                    mLastEvent = Integer.parseInt(event.getValueAsString(2));
                    mLastDetails = "";
                } else if (state == 1) { // stop
                    if (mLastState == 0) {
                        mLastStopTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                        if (mLastStartTime == 0) {
                            // Log starts with a stop event
                            mLastStartTime = mLastStopTime;
                        }
                        addEvent(event);
                        mLastState = 1;
                    }
                }
            } else if (event.mTag == EVENT_TICKLE) {
                int auth = getAuth(event.getValueAsString(0));
                if (auth >= 0) {
                    long msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    mDatasetsSyncTickle[auth].addOrUpdate(new FixedMillisecond(msec), -1);
                }
            } else if (event.mTag == EVENT_SYNC_DETAILS) {
                int auth = getAuth(event.getValueAsString(0));
                mLastDetails = event.getValueAsString(3);
                if (mLastState != 0) { // Not inside event
                    long updateTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    if (updateTime - mLastStopTime <= 250) {
                        // Got details within 250ms after event, so delete and re-insert
                        // Details later than 250ms (arbitrary) are discarded as probably
                        // unrelated.
                        int lastItem = mDatasetsSync[auth].getItemCount();
                        mDatasetsSync[auth].delete(lastItem-1, lastItem-1);
                        mTooltipsSync[auth].remove(lastItem-1);
                        addEvent(event);
                    }
                }
            }
        } catch (InvalidTypeException e) {
        }
    }

    /**
     * Convert authority name to auth number.
     * @param authname "calendar", etc.
     * @return number series number associated with the authority
     */
    private int getAuth(String authname) throws InvalidTypeException {
        if ("calendar".equals(authname) || "cl".equals(authname)) {
            return CALENDAR;
        } else if ("contacts".equals(authname) || "cp".equals(authname)) {
            return CONTACTS;
        } else if ("subscribedfeeds".equals(authname)) {
            return FEEDS;
        } else if ("gmail-ls".equals(authname) || "mail".equals(authname)) {
            return GMAIL;
        } else if ("gmail-live".equals(authname)) {
            return GMAIL;
        } else if ("unknown".equals(authname)) {
            return -1; // Unknown tickles; discard
        } else {
            throw new InvalidTypeException("Unknown authname " + authname);
        }
    }

    /**
     * Generate the height for an event.
     * Height is somewhat arbitrarily the count of "things" that happened
     * during the sync.
     * When network traffic measurements are available, code should be modified
     * to use that instead.
     * @param details The details string associated with the event
     * @return The height in arbirary units (0-100)
     */
    private int getHeightFromDetails(String details) {
        if (details == null) {
            return 1; // Arbitrary
        }
        int total = 0;
        String parts[] = details.split("[a-zA-Z]");
        for (String part : parts) {
            if ("".equals(part)) continue;
            total += Integer.parseInt(part);
        }
        if (total == 0) {
            total = 1;
        }
        return total;
    }

    /**
     * Generates the tooltips text for an event.
     * This method decodes the cryptic details string.
     * @param auth The authority associated with the event
     * @param details The details string
     * @param eventSource server, poll, etc.
     * @return The text to display in the tooltips
     */
    private String getTextFromDetails(int auth, String details, int eventSource) {

        StringBuffer sb = new StringBuffer();
        sb.append(AUTH_NAMES[auth]).append(": \n");

        Scanner scanner = new Scanner(details);
        Pattern charPat = Pattern.compile("[a-zA-Z]");
        Pattern numPat = Pattern.compile("[0-9]+");
        while (scanner.hasNext()) {
            String key = scanner.findInLine(charPat);
            int val = Integer.parseInt(scanner.findInLine(numPat));
            if (auth == GMAIL && "M".equals(key)) {
                sb.append("messages from server: ").append(val).append("\n");
            } else if (auth == GMAIL && "L".equals(key)) {
                sb.append("labels from server: ").append(val).append("\n");
            } else if (auth == GMAIL && "C".equals(key)) {
                sb.append("check conversation requests from server: ").append(val).append("\n");
            } else if (auth == GMAIL && "A".equals(key)) {
                sb.append("attachments from server: ").append(val).append("\n");
            } else if (auth == GMAIL && "U".equals(key)) {
                sb.append("op updates from server: ").append(val).append("\n");
            } else if (auth == GMAIL && "u".equals(key)) {
                sb.append("op updates to server: ").append(val).append("\n");
            } else if (auth == GMAIL && "S".equals(key)) {
                sb.append("send/receive cycles: ").append(val).append("\n");
            } else if ("Q".equals(key)) {
                sb.append("queries to server: ").append(val).append("\n");
            } else if ("E".equals(key)) {
                sb.append("entries from server: ").append(val).append("\n");
            } else if ("u".equals(key)) {
                sb.append("updates from client: ").append(val).append("\n");
            } else if ("i".equals(key)) {
                sb.append("inserts from client: ").append(val).append("\n");
            } else if ("d".equals(key)) {
                sb.append("deletes from client: ").append(val).append("\n");
            } else if ("f".equals(key)) {
                sb.append("full sync requested\n");
            } else if ("r".equals(key)) {
                sb.append("partial sync unavailable\n");
            } else if ("X".equals(key)) {
                sb.append("hard error\n");
            } else if ("e".equals(key)) {
                sb.append("number of parse exceptions: ").append(val).append("\n");
            } else if ("c".equals(key)) {
                sb.append("number of conflicts: ").append(val).append("\n");
            } else if ("a".equals(key)) {
                sb.append("number of auth exceptions: ").append(val).append("\n");
            } else if ("D".equals(key)) {
                sb.append("too many deletions\n");
            } else if ("R".equals(key)) {
                sb.append("too many retries: ").append(val).append("\n");
            } else if ("b".equals(key)) {
                sb.append("database error\n");
            } else if ("x".equals(key)) {
                sb.append("soft error\n");
            } else if ("l".equals(key)) {
                sb.append("sync already in progress\n");
            } else if ("I".equals(key)) {
                sb.append("io exception\n");
            } else if (auth == CONTACTS && "p".equals(key)) {
                sb.append("photos uploaded from client: ").append(val).append("\n");
            } else if (auth == CONTACTS && "P".equals(key)) {
                sb.append("photos downloaded from server: ").append(val).append("\n");
            } else if (auth == CALENDAR && "F".equals(key)) {
                sb.append("server refresh\n");
            } else if (auth == CALENDAR && "s".equals(key)) {
                sb.append("server diffs fetched\n");
            } else {
                sb.append(key).append("=").append(val);
            }
        }
        if (eventSource == 0) {
            sb.append("(server)");
        } else if (eventSource == 1) {
            sb.append("(local)");
        } else if (eventSource == 2) {
            sb.append("(poll)");
        } else if (eventSource == 3) {
            sb.append("(user)");
        }
        return sb.toString();
    }

    /**
     * Helper to add an event to the data series.
     * Also updates error series if appropriate (x or X in details).
     * @param event The event
     */
    private void addEvent(EventContainer event) {
        try {
            int auth = getAuth(event.getValueAsString(0));
            double height = getHeightFromDetails(mLastDetails);
            height = height / (mLastStopTime - mLastStartTime + 1) * 10000;
            if (height > 30) {
                height = 30;
            }
            mDatasetsSync[auth].add(new SimpleTimePeriod(mLastStartTime, mLastStopTime), height);
            mTooltipsSync[auth].add(getTextFromDetails(auth, mLastDetails,
                    mLastEvent));
            mTooltipGenerators[auth].addToolTipSeries(mTooltipsSync[auth]);
            if (mLastDetails.indexOf('x') >= 0 || mLastDetails.indexOf('X') >= 0) {
                long msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
                mDatasetError.addOrUpdate(new FixedMillisecond(msec), -1);
            }
        } catch (InvalidTypeException e) {
            e.printStackTrace();
        }
    }

    // Implementation of the Sync Histogram display

    // Information to graph for each authority
    private TimePeriodValues mDatasetsSyncHist[];

    /**
     * Initialize the Plot and series data for the sync display.
     */
    void initSyncHistDisplay() {
        XYPlot xyPlot = mChart.getXYPlot();

        AbstractXYItemRenderer br = new XYBarRenderer();
        mDatasetsSyncHist = new TimePeriodValues[NUM_AUTHS+1];
        mLastDetails = "";
        mTimePeriodMap = new HashMap[NUM_AUTHS + 1];

        TimePeriodValuesCollection tpvc = new TimePeriodValuesCollection();
        xyPlot.setDataset(tpvc);
        xyPlot.setRenderer(br);

        for (int i = 0; i < NUM_AUTHS + 1; i++) {
            br.setSeriesPaint(i, AUTH_COLORS[i]);
            mDatasetsSyncHist[i] = new TimePeriodValues(AUTH_NAMES[i]);
            tpvc.addSeries(mDatasetsSyncHist[i]);
            mTimePeriodMap[i] = new HashMap<SimpleTimePeriod, Integer>();

        }
    }

    /**
     * Updates the display with a new event.  This is the main entry point for
     * each event.  This method has the logic to tie together the start event,
     * stop event, and details event into one graph item.  Note that the details
     * can happen before or after the stop event.
     * @param event The event
     */
    private void updateSyncHistDisplay(EventContainer event) {
        try {
            if (event.mTag == EVENT_SYNC) {
                int state = Integer.parseInt(event.getValueAsString(1));
                if (state == 0) { // start
                    mLastStartTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    mLastState = 0;
                    mLastEvent = Integer.parseInt(event.getValueAsString(2));
                    mLastDetails = "";
                } else if (state == 1) { // stop
                    if (mLastState == 0) {
                        mLastStopTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                        if (mLastStartTime == 0) {
                            // Log starts with a stop event
                            mLastStartTime = mLastStopTime;
                        }
                        int auth = getAuth(event.getValueAsString(0));
                        if (mLastDetails.indexOf('x') >= 0 || mLastDetails.indexOf('X') >= 0) {
                            auth = ERRORS;
                        }
                        double delta = (mLastStopTime - mLastStartTime) * 100. / 1000 / 3600; // Percent of hour
                        addHistEvent(event, auth, delta);
                        mLastState = 1;
                    }
                }
            } else if (event.mTag == EVENT_SYNC_DETAILS) {
                int auth = getAuth(event.getValueAsString(0));
                mLastDetails = event.getValueAsString(3);
                if (mLastState != 0) { // Not inside event
                    long updateTime = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    if (updateTime - mLastStopTime <= 250) {
                        // Got details within 250ms after event, so delete and re-insert
                        // Details later than 250ms (arbitrary) are discarded as probably
                        // unrelated.
                        //int lastItem = mDatasetsSync[auth].getItemCount();
                        //addHistEvent(event);
                        if (mLastDetails.indexOf('x') >= 0 || mLastDetails.indexOf('X') >= 0) {
                            // Item turns out to be in error, so transfer time from old auth to error.

                            double delta = (mLastStopTime - mLastStartTime) * 100. / 1000 / 3600; // Percent of hour
                            addHistEvent(event, auth, -delta);
                            addHistEvent(event, ERRORS, delta);
                        }
                    }
                }
            }
        } catch (InvalidTypeException e) {
        }
    }

    /**
     * Helper to add an event to the data series.
     * Also updates error series if appropriate (x or X in details).
     * @param event The event
     * @param auth
     * @param value
     */
    private void addHistEvent(EventContainer event, int auth, double value) {
        SimpleTimePeriod hour = getTimePeriod(mLastStopTime, mHistWidth);

        // Loop over all datasets to do the stacking.
        for (int i = auth; i <= ERRORS; i++) {
            addToPeriod(mDatasetsSyncHist, i, hour, value);
        }
    }

    Map<SimpleTimePeriod, Integer> mTimePeriodMap[];

    private void addToPeriod(TimePeriodValues tpv[], int auth, SimpleTimePeriod period, double value) {
        int index;
        if (mTimePeriodMap[auth].containsKey(period)) {
            index = mTimePeriodMap[auth].get(period);
            double oldValue = tpv[auth].getValue(index).doubleValue();
            tpv[auth].update(index, oldValue + value);
        } else {
            index = tpv[auth].getItemCount();
            mTimePeriodMap[auth].put(period, index);
            tpv[auth].add(period, value);
        }
    }

    /**
     * Creates a multiple-hour time period for the histogram.
     * @param time Time in milliseconds.
     * @param numHoursWide: should divide into a day.
     * @return SimpleTimePeriod covering the number of hours and containing time.
     */
    private SimpleTimePeriod getTimePeriod(long time, long numHoursWide) {
        Date date = new Date(time);
        TimeZone zone = RegularTimePeriod.DEFAULT_TIME_ZONE;
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTime(date);
        long hoursOfYear = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.DAY_OF_YEAR) * 24;
        int year = calendar.get(Calendar.YEAR);
        hoursOfYear = (hoursOfYear / numHoursWide) * numHoursWide;
        calendar.clear();
        calendar.set(year, 0, 1, 0, 0); // Jan 1
        long start = calendar.getTimeInMillis() + hoursOfYear * 3600 * 1000;
        return new SimpleTimePeriod(start, start + numHoursWide * 3600 * 1000);
    }
}
