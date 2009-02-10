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
import com.android.ddmlib.log.InvalidTypeException;
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

public class DisplaySyncHistogram extends EventDisplay {


    // State information while processing the event stream
    protected int mLastState; // 0 if event started, 1 if event stopped
    protected long mLastStartTime; // ms
    protected long mLastStopTime; //ms
    protected String mLastDetails;
    protected int mLastEvent; // server, poll, etc

    public DisplaySyncHistogram(String name) {
        super(name);
    }

    /**
     * Resets the display.
     */
    @Override
    void resetUI() {
        initSyncHistogramDisplay();
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
        initSyncHistogramDisplay();
        return composite;
    }

    // Information to graph for each authority
    private TimePeriodValues mDatasetsSyncHist[];

    /**
     * Initializes the display.
     */
    private void initSyncHistogramDisplay() {
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
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
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
