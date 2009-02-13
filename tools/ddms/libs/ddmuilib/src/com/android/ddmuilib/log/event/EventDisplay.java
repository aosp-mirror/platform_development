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
import com.android.ddmlib.log.EventValueDescription.ValueType;
import com.android.ddmlib.log.InvalidTypeException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.experimental.swt.SWTUtils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Represents a custom display of one or more events.
 */
abstract class EventDisplay {

    private final static String DISPLAY_DATA_STORAGE_SEPARATOR = ":"; //$NON-NLS-1$
    private final static String PID_STORAGE_SEPARATOR = ","; //$NON-NLS-1$
    private final static String DESCRIPTOR_STORAGE_SEPARATOR = "$"; //$NON-NLS-1$
    private final static String DESCRIPTOR_DATA_STORAGE_SEPARATOR = "!"; //$NON-NLS-1$

    private final static String FILTER_VALUE_NULL = "<null>"; //$NON-NLS-1$

    public final static int DISPLAY_TYPE_LOG_ALL = 0;
    public final static int DISPLAY_TYPE_FILTERED_LOG = 1;
    public final static int DISPLAY_TYPE_GRAPH = 2;
    public final static int DISPLAY_TYPE_SYNC = 3;
    public final static int DISPLAY_TYPE_SYNC_HIST = 4;
    public final static int DISPLAY_TYPE_SYNC_PERF = 5;

    private final static int EVENT_CHECK_FAILED = 0;
    protected final static int EVENT_CHECK_SAME_TAG = 1;
    protected final static int EVENT_CHECK_SAME_VALUE = 2;

    /**
     * Creates the appropriate EventDisplay subclass.
     *
     * @param type the type of display (DISPLAY_TYPE_LOG_ALL, etc)
     * @param name the name of the display
     * @return the created object
     */
    public static EventDisplay eventDisplayFactory(int type, String name) {
        switch (type) {
            case DISPLAY_TYPE_LOG_ALL:
                return new DisplayLog(name);
            case DISPLAY_TYPE_FILTERED_LOG:
                return new DisplayFilteredLog(name);
            case DISPLAY_TYPE_SYNC:
                return new DisplaySync(name);
            case DISPLAY_TYPE_SYNC_HIST:
                return new DisplaySyncHistogram(name);
            case DISPLAY_TYPE_GRAPH:
                return new DisplayGraph(name);
            case DISPLAY_TYPE_SYNC_PERF:
                return new DisplaySyncPerf(name);
            default:
                throw new InvalidParameterException("Unknown Display Type " + type); //$NON-NLS-1$
        }
    }

    /**
     * Adds event to the display.
     * @param event The event
     * @param logParser The log parser.
     */
    abstract void newEvent(EventContainer event, EventLogParser logParser);

    /**
     * Resets the display.
     */
    abstract void resetUI();

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    abstract int getDisplayType();

    /**
     * Creates the UI for the event display.
     *
     * @param parent    the parent composite.
     * @param logParser the current log parser.
     * @return the created control (which may have children).
     */
    abstract Control createComposite(final Composite parent, EventLogParser logParser,
            final ILogColumnListener listener);

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
         *
         * @param storageString the storage string
         */
        final void loadFrom(String storageString) {
            String[] values = storageString.split(Pattern.quote(DESCRIPTOR_DATA_STORAGE_SEPARATOR));
            loadFrom(values, 0);
        }

        /**
         * Loads the parameters from an array of strings.
         *
         * @param storageStrings the strings representing each parameter.
         * @param index          the starting index in the array of strings.
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
                ValueDisplayDescriptor valueDescriptor = (ValueDisplayDescriptor) descriptor;
                valueName = valueDescriptor.valueName;
                valueIndex = valueDescriptor.valueIndex;
            }
        }

        /**
         * Loads the parameters from an array of strings.
         *
         * @param storageStrings the strings representing each parameter.
         * @param index          the starting index in the array of strings.
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
    protected String mName;

    private boolean mPidFiltering = false;

    private ArrayList<Integer> mPidFilterList = null;

    protected final ArrayList<ValueDisplayDescriptor> mValueDescriptors =
            new ArrayList<ValueDisplayDescriptor>();
    private final ArrayList<OccurrenceDisplayDescriptor> mOccurrenceDescriptors =
            new ArrayList<OccurrenceDisplayDescriptor>();

    /* ==================
     * Event Display members for display purpose.
     * ================== */
    // chart objects
    /**
     * This is a map of (descriptor, map2) where map2 is a map of (pid, chart-series)
     */
    protected final HashMap<ValueDisplayDescriptor, HashMap<Integer, TimeSeries>> mValueDescriptorSeriesMap =
            new HashMap<ValueDisplayDescriptor, HashMap<Integer, TimeSeries>>();
    /**
     * This is a map of (descriptor, map2) where map2 is a map of (pid, chart-series)
     */
    protected final HashMap<OccurrenceDisplayDescriptor, HashMap<Integer, TimeSeries>> mOcurrenceDescriptorSeriesMap =
            new HashMap<OccurrenceDisplayDescriptor, HashMap<Integer, TimeSeries>>();

    /**
     * This is a map of (ValueType, dataset)
     */
    protected final HashMap<ValueType, TimeSeriesCollection> mValueTypeDataSetMap =
            new HashMap<ValueType, TimeSeriesCollection>();

    protected JFreeChart mChart;
    protected TimeSeriesCollection mOccurrenceDataSet;
    protected int mDataSetCount;
    private ChartComposite mChartComposite;
    protected long mMaximumChartItemAge = -1;
    protected long mHistWidth = 1;

    // log objects.
    protected Table mLogTable;

    /* ==================
     * Misc data.
     * ================== */
    protected int mValueDescriptorCheck = EVENT_CHECK_FAILED;

    EventDisplay(String name) {
        mName = name;
    }

    static EventDisplay clone(EventDisplay from) {
        EventDisplay ed = eventDisplayFactory(from.getDisplayType(), from.getName());
        ed.mName = from.mName;
        ed.mPidFiltering = from.mPidFiltering;
        ed.mMaximumChartItemAge = from.mMaximumChartItemAge;
        ed.mHistWidth = from.mHistWidth;

        if (from.mPidFilterList != null) {
            ed.mPidFilterList = new ArrayList<Integer>();
            ed.mPidFilterList.addAll(from.mPidFilterList);
        }

        for (ValueDisplayDescriptor desc : from.mValueDescriptors) {
            ed.mValueDescriptors.add(new ValueDisplayDescriptor(desc));
        }
        ed.mValueDescriptorCheck = from.mValueDescriptorCheck;

        for (OccurrenceDisplayDescriptor desc : from.mOccurrenceDescriptors) {
            ed.mOccurrenceDescriptors.add(new OccurrenceDisplayDescriptor(desc));
        }
        return ed;
    }

    /**
     * Returns the parameters of the receiver as a single String for storage.
     */
    String getStorageString() {
        StringBuilder sb = new StringBuilder();

        sb.append(mName);
        sb.append(DISPLAY_DATA_STORAGE_SEPARATOR);
        sb.append(getDisplayType());
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
     *
     * @param descriptor the descriptor to be added.
     */
    void addDescriptor(OccurrenceDisplayDescriptor descriptor) {
        if (descriptor instanceof ValueDisplayDescriptor) {
            mValueDescriptors.add((ValueDisplayDescriptor) descriptor);
            mValueDescriptorCheck = checkDescriptors();
        } else {
            mOccurrenceDescriptors.add(descriptor);
        }
    }

    /**
     * Returns a descriptor by index and class (extending {@link OccurrenceDisplayDescriptor}).
     *
     * @param descriptorClass the class of the descriptor to return.
     * @param index           the index of the descriptor to return.
     * @return either a {@link OccurrenceDisplayDescriptor} or a {@link ValueDisplayDescriptor}
     *         or <code>null</code> if <code>descriptorClass</code> is another class.
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
     *
     * @param descriptorClass the class of the descriptor.
     * @param index           the index of the descriptor to be removed.
     */
    void removeDescriptor(Class<? extends OccurrenceDisplayDescriptor> descriptorClass, int index) {
        if (descriptorClass == OccurrenceDisplayDescriptor.class) {
            mOccurrenceDescriptors.remove(index);
        } else if (descriptorClass == ValueDisplayDescriptor.class) {
            mValueDescriptors.remove(index);
            mValueDescriptorCheck = checkDescriptors();
        }
    }

    Control createCompositeChart(final Composite parent, EventLogParser logParser,
            String title) {
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

        return mChartComposite;

    }

    private void processClick(XYPlot xyPlot) {
        double rangeValue = xyPlot.getRangeCrosshairValue();
        if (rangeValue != 0) {
            double domainValue = xyPlot.getDomainCrosshairValue();

            Millisecond msec = new Millisecond(new Date((long) domainValue));

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
     * Resizes the <code>index</code>-th column of the log {@link Table} (if applicable).
     * Subclasses can override if necessary.
     * <p/>
     * This does nothing if the <code>Table</code> object is <code>null</code> (because the display
     * type does not use a column) or if the <code>index</code>-th column is in fact the originating
     * column passed as argument.
     *
     * @param index        the index of the column to resize
     * @param sourceColumn the original column that was resize, and on which we need to sync the
     *                     index-th column width.
     */
    void resizeColumn(int index, TableColumn sourceColumn) {
    }

    /**
     * Sets the current {@link EventLogParser} object.
     * Subclasses can override if necessary.
     */
    protected void setNewLogParser(EventLogParser logParser) {
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
     * Returns the {@link Table} object used to display events, if any.
     *
     * @return a Table object or <code>null</code>.
     */
    Table getTable() {
        return mLogTable;
    }

    /**
     * Loads a new {@link EventDisplay} from a storage string. The string must have been created
     * with {@link #getStorageString()}.
     *
     * @param storageString the storage string
     * @return a new {@link EventDisplay} or null if the load failed.
     */
    static EventDisplay load(String storageString) {
        if (storageString.length() > 0) {
            // the storage string is separated by ':'
            String[] values = storageString.split(Pattern.quote(DISPLAY_DATA_STORAGE_SEPARATOR));

            try {
                int index = 0;

                String name = values[index++];
                int displayType = Integer.parseInt(values[index++]);
                boolean pidFiltering = Boolean.parseBoolean(values[index++]);

                EventDisplay ed = eventDisplayFactory(displayType, name);
                ed.setPidFiltering(pidFiltering);

                // because empty sections are removed by String.split(), we have to check
                // the index for those.
                if (index < values.length) {
                    ed.loadPidFilters(values[index++]);
                }

                if (index < values.length) {
                    ed.loadValueDescriptors(values[index++]);
                }

                if (index < values.length) {
                    ed.loadOccurrenceDescriptors(values[index++]);
                }

                ed.updateValueDescriptorCheck();

                if (index < values.length) {
                    ed.mMaximumChartItemAge = Long.parseLong(values[index++]);
                }

                if (index < values.length) {
                    ed.mHistWidth = Long.parseLong(values[index++]);
                }

                return ed;
            } catch (RuntimeException re) {
                // we'll return null below.
                Log.e("ddms", re);
            }
        }

        return null;
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
     * Fills a list with {@link OccurrenceDisplayDescriptor} (or a subclass of it) from another
     * list if they are configured to display the {@link EventContainer}
     *
     * @param event    the event container
     * @param fullList the list with all the descriptors.
     * @param outList  the list to fill.
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
     * Filters the {@link com.android.ddmlib.log.EventContainer}, and fills two list of {@link com.android.ddmuilib.log.event.EventDisplay.ValueDisplayDescriptor}
     * and {@link com.android.ddmuilib.log.event.EventDisplay.OccurrenceDisplayDescriptor} configured to display the event.
     *
     * @param event
     * @param valueDescriptors
     * @param occurrenceDescriptors
     * @return true if the event should be displayed.
     */

    protected boolean filterEvent(EventContainer event,
            ArrayList<ValueDisplayDescriptor> valueDescriptors,
            ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors) {

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
     *
     * @return flag as described above
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
     * Resets the time limit on the chart to be infinite.
     */
    void resetChartTimeLimit() {
        mMaximumChartItemAge = -1;
    }

    /**
     * Sets the time limit on the charts.
     *
     * @param timeLimit the time limit in seconds.
     */
    void setChartTimeLimit(long timeLimit) {
        mMaximumChartItemAge = timeLimit;
    }

    long getChartTimeLimit() {
        return mMaximumChartItemAge;
    }

    /**
     * m
     * Resets the histogram width
     */
    void resetHistWidth() {
        mHistWidth = 1;
    }

    /**
     * Sets the histogram width
     *
     * @param histWidth the width in hours
     */
    void setHistWidth(long histWidth) {
        mHistWidth = histWidth;
    }

    long getHistWidth() {
        return mHistWidth;
    }
}
