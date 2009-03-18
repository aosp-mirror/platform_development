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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DisplayGraph extends EventDisplay {

    public DisplayGraph(String name) {
        super(name);
    }

    /**
     * Resets the display.
     */
    @Override
    void resetUI() {
        Collection<TimeSeriesCollection> datasets = mValueTypeDataSetMap.values();
        for (TimeSeriesCollection dataset : datasets) {
            dataset.removeAllSeries();
        }
        if (mOccurrenceDataSet != null) {
            mOccurrenceDataSet.removeAllSeries();
        }
        mValueDescriptorSeriesMap.clear();
        mOcurrenceDescriptorSeriesMap.clear();
    }

    /**
     * Creates the UI for the event display.
     * @param parent the parent composite.
     * @param logParser the current log parser.
     * @return the created control (which may have children).
     */
    @Override
    public Control createComposite(final Composite parent, EventLogParser logParser,
            final ILogColumnListener listener) {
        String title = getChartTitle(logParser);
        return createCompositeChart(parent, logParser, title);
    }

    /**
     * Adds event to the display.
     */
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
        ArrayList<ValueDisplayDescriptor> valueDescriptors =
                new ArrayList<ValueDisplayDescriptor>();

        ArrayList<OccurrenceDisplayDescriptor> occurrenceDescriptors =
                new ArrayList<OccurrenceDisplayDescriptor>();

        if (filterEvent(event, valueDescriptors, occurrenceDescriptors)) {
            updateChart(event, logParser, valueDescriptors, occurrenceDescriptors);
        }
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
     * Returns a {@link TimeSeriesCollection} for a specific {@link com.android.ddmlib.log.EventValueDescription.ValueType}.
     * If the data set is not yet created, it is first allocated and set up into the
     * {@link org.jfree.chart.JFreeChart} object.
     * @param type the {@link com.android.ddmlib.log.EventValueDescription.ValueType} of the data set.
     * @param accumulateValues
     */
    private TimeSeriesCollection getValueDataset(EventValueDescription.ValueType type, boolean accumulateValues) {
        TimeSeriesCollection dataset = mValueTypeDataSetMap.get(type);
        if (dataset == null) {
            // create the data set and store it in the map
            dataset = new TimeSeriesCollection();
            mValueTypeDataSetMap.put(type, dataset);

            // create the renderer and configure it depending on the ValueType
            AbstractXYItemRenderer renderer;
            if (type == EventValueDescription.ValueType.PERCENT && accumulateValues) {
                renderer = new XYAreaRenderer();
            } else {
                XYLineAndShapeRenderer r = new XYLineAndShapeRenderer();
                r.setBaseShapesVisible(type != EventValueDescription.ValueType.PERCENT);

                renderer = r;
            }

            // set both the dataset and the renderer in the plot object.
            XYPlot xyPlot = mChart.getXYPlot();
            xyPlot.setDataset(mDataSetCount, dataset);
            xyPlot.setRenderer(mDataSetCount, renderer);

            // put a new axis label, and configure it.
            NumberAxis axis = new NumberAxis(type.toString());

            if (type == EventValueDescription.ValueType.PERCENT) {
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
     * Returns the {@link TimeSeriesCollection} for the occurrence display. If the data set is not
     * yet created, it is first allocated and set up into the {@link org.jfree.chart.JFreeChart} object.
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
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_GRAPH;
    }

    /**
     * Sets the current {@link EventLogParser} object.
     */
    @Override
    protected void setNewLogParser(EventLogParser logParser) {
        if (mChart != null) {
            mChart.setTitle(getChartTitle(logParser));
        }
    }
    /**
     * Returns a meaningful chart title based on the value of {@link #mValueDescriptorCheck}.
     *
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
}