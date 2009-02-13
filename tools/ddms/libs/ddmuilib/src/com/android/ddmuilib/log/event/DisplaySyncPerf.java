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

package com.android.ddmuilib.log.event;

import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.InvalidTypeException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class DisplaySyncPerf extends SyncCommon {

    CustomXYToolTipGenerator mTooltipGenerator;
    List mTooltips[];

    // The series number for each graphed item.
    // sync authorities are 0-3
    private static final int DB_QUERY = 4;
    private static final int DB_WRITE = 5;
    private static final int HTTP_NETWORK = 6;
    private static final int HTTP_PROCESSING = 7;
    private static final int NUM_SERIES = (HTTP_PROCESSING + 1);
    private static final String SERIES_NAMES[] = {"Calendar", "Gmail", "Feeds", "Contacts",
            "DB Query", "DB Write", "HTTP Response", "HTTP Processing",};
    private static final Color SERIES_COLORS[] = {Color.MAGENTA, Color.GREEN, Color.BLUE,
            Color.ORANGE, Color.RED, Color.CYAN, Color.PINK, Color.DARK_GRAY};
    private static final double SERIES_YCOORD[] = {0, 0, 0, 0, 1, 1, 2, 2};

    // Values from data/etc/event-log-tags
    private static final int EVENT_DB_OPERATION = 52000;
    private static final int EVENT_HTTP_STATS = 52001;
    // op types for EVENT_DB_OPERATION
    final int EVENT_DB_QUERY = 0;
    final int EVENT_DB_WRITE = 1;

    // Information to graph for each authority
    private TimePeriodValues mDatasets[];

    /**
     * TimePeriodValuesCollection that supports Y intervals.  This allows the
     * creation of "floating" bars, rather than bars rooted to the axis.
     */
    class YIntervalTimePeriodValuesCollection extends TimePeriodValuesCollection {
        /** default serial UID */
        private static final long serialVersionUID = 1L;

        private double yheight;

        /**
         * Constructs a collection of bars with a fixed Y height.
         *
         * @param yheight The height of the bars.
         */
        YIntervalTimePeriodValuesCollection(double yheight) {
            this.yheight = yheight;
        }

        /**
         * Returns ending Y value that is a fixed amount greater than the starting value.
         *
         * @param series the series (zero-based index).
         * @param item   the item (zero-based index).
         * @return The ending Y value for the specified series and item.
         */
        @Override
        public Number getEndY(int series, int item) {
            return getY(series, item).doubleValue() + yheight;
        }
    }

    /**
     * Constructs a graph of network and database stats.
     *
     * @param name The name of this graph in the graph list.
     */
    public DisplaySyncPerf(String name) {
        super(name);
    }

    /**
     * Creates the UI for the event display.
     *
     * @param parent    the parent composite.
     * @param logParser the current log parser.
     * @return the created control (which may have children).
     */
    @Override
    public Control createComposite(final Composite parent, EventLogParser logParser,
            final ILogColumnListener listener) {
        Control composite = createCompositeChart(parent, logParser, "Sync Performance");
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
        xyPlot.getRangeAxis().setVisible(false);
        mTooltipGenerator = new CustomXYToolTipGenerator();
        mTooltips = new List[NUM_SERIES];

        XYBarRenderer br = new XYBarRenderer();
        br.setUseYInterval(true);
        mDatasets = new TimePeriodValues[NUM_SERIES];

        TimePeriodValuesCollection tpvc = new YIntervalTimePeriodValuesCollection(1);
        xyPlot.setDataset(tpvc);
        xyPlot.setRenderer(br);

        for (int i = 0; i < NUM_SERIES; i++) {
            br.setSeriesPaint(i, SERIES_COLORS[i]);
            mDatasets[i] = new TimePeriodValues(SERIES_NAMES[i]);
            tpvc.addSeries(mDatasets[i]);
            mTooltips[i] = new ArrayList<String>();
            mTooltipGenerator.addToolTipSeries(mTooltips[i]);
            br.setSeriesToolTipGenerator(i, mTooltipGenerator);
        }
    }

    /**
     * Updates the display with a new event.
     *
     * @param event     The event
     * @param logParser The parser providing the event.
     */
    @Override
    void newEvent(EventContainer event, EventLogParser logParser) {
        super.newEvent(event, logParser); // Handle sync operation
        try {
            if (event.mTag == EVENT_DB_OPERATION) {
                // 52000 db_operation (name|3),(op_type|1|5),(time|2|3)
                String tip = event.getValueAsString(0);
                long endTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                int opType = Integer.parseInt(event.getValueAsString(1));
                long duration = Long.parseLong(event.getValueAsString(2));

                if (opType == EVENT_DB_QUERY) {
                    mDatasets[DB_QUERY].add(new SimpleTimePeriod(endTime - duration, endTime),
                            SERIES_YCOORD[DB_QUERY]);
                    mTooltips[DB_QUERY].add(tip);
                } else if (opType == EVENT_DB_WRITE) {
                    mDatasets[DB_WRITE].add(new SimpleTimePeriod(endTime - duration, endTime),
                            SERIES_YCOORD[DB_WRITE]);
                    mTooltips[DB_WRITE].add(tip);
                }
            } else if (event.mTag == EVENT_HTTP_STATS) {
                // 52001 http_stats (useragent|3),(response|2|3),(processing|2|3),(tx|1|2),(rx|1|2)
                String tip = event.getValueAsString(0) + ", tx:" + event.getValueAsString(3) +
                        ", rx: " + event.getValueAsString(4);
                long endTime = (long) event.sec * 1000L + (event.nsec / 1000000L);
                long netEndTime = endTime - Long.parseLong(event.getValueAsString(2));
                long netStartTime = netEndTime - Long.parseLong(event.getValueAsString(1));
                mDatasets[HTTP_NETWORK].add(new SimpleTimePeriod(netStartTime, netEndTime),
                        SERIES_YCOORD[HTTP_NETWORK]);
                mDatasets[HTTP_PROCESSING].add(new SimpleTimePeriod(netEndTime, endTime),
                        SERIES_YCOORD[HTTP_PROCESSING]);
                mTooltips[HTTP_NETWORK].add(tip);
                mTooltips[HTTP_PROCESSING].add(tip);
            }
        } catch (InvalidTypeException e) {
        }
    }

    /**
     * Callback from super.newEvent to process a sync event.
     *
     * @param event      The sync event
     * @param startTime  Start time (ms) of events
     * @param stopTime   Stop time (ms) of events
     * @param details    Details associated with the event.
     * @param newEvent   True if this event is a new sync event.  False if this event
     * @param syncSource
     */
    @Override
    void processSyncEvent(EventContainer event, int auth, long startTime, long stopTime,
            String details, boolean newEvent, int syncSource) {
        if (newEvent) {
            mDatasets[auth].add(new SimpleTimePeriod(startTime, stopTime), SERIES_YCOORD[auth]);
        }
    }

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_SYNC_PERF;
    }
}
