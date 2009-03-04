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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DisplaySyncHistogram extends SyncCommon {

    Map<SimpleTimePeriod, Integer> mTimePeriodMap[];

    // Information to graph for each authority
    private TimePeriodValues mDatasetsSyncHist[];

    public DisplaySyncHistogram(String name) {
        super(name);
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
        Control composite = createCompositeChart(parent, logParser, "Sync Histogram");
        resetUI();
        return composite;
    }

    /**
     * Resets the display.
     */
    @Override
    void resetUI() {
        super.resetUI();
        XYPlot xyPlot = mChart.getXYPlot();

        AbstractXYItemRenderer br = new XYBarRenderer();
        mDatasetsSyncHist = new TimePeriodValues[NUM_AUTHS+1];
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
     * Callback to process a sync event.
     *
     * @param event      The sync event
     * @param startTime Start time (ms) of events
     * @param stopTime Stop time (ms) of events
     * @param details Details associated with the event.
     * @param newEvent True if this event is a new sync event.  False if this event
     * @param syncSource
     */
    @Override
    void processSyncEvent(EventContainer event, int auth, long startTime, long stopTime,
            String details, boolean newEvent, int syncSource) {
        if (newEvent) {
            if (details.indexOf('x') >= 0 || details.indexOf('X') >= 0) {
                auth = ERRORS;
            }
            double delta = (stopTime - startTime) * 100. / 1000 / 3600; // Percent of hour
            addHistEvent(0, auth, delta);
        } else {
            // sync_details arrived for an event that has already been graphed.
            if (details.indexOf('x') >= 0 || details.indexOf('X') >= 0) {
                // Item turns out to be in error, so transfer time from old auth to error.
                double delta = (stopTime - startTime) * 100. / 1000 / 3600; // Percent of hour
                addHistEvent(0, auth, -delta);
                addHistEvent(0, ERRORS, delta);
            }
        }
    }

    /**
     * Helper to add an event to the data series.
     * Also updates error series if appropriate (x or X in details).
     * @param stopTime Time event ends
     * @param auth Sync authority
     * @param value Value to graph for event
     */
    private void addHistEvent(long stopTime, int auth, double value) {
        SimpleTimePeriod hour = getTimePeriod(stopTime, mHistWidth);

        // Loop over all datasets to do the stacking.
        for (int i = auth; i <= ERRORS; i++) {
            addToPeriod(mDatasetsSyncHist, i, hour, value);
        }
    }

    private void addToPeriod(TimePeriodValues tpv[], int auth, SimpleTimePeriod period,
            double value) {
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
        long hoursOfYear = calendar.get(Calendar.HOUR_OF_DAY) +
                calendar.get(Calendar.DAY_OF_YEAR) * 24;
        int year = calendar.get(Calendar.YEAR);
        hoursOfYear = (hoursOfYear / numHoursWide) * numHoursWide;
        calendar.clear();
        calendar.set(year, 0, 1, 0, 0); // Jan 1
        long start = calendar.getTimeInMillis() + hoursOfYear * 3600 * 1000;
        return new SimpleTimePeriod(start, start + numHoursWide * 3600 * 1000);
    }

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_SYNC_HIST;
    }
}
