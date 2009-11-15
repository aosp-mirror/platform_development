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
import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.util.ShapeUtilities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

public class DisplaySync extends SyncCommon {

    // Information to graph for each authority
    private TimePeriodValues mDatasetsSync[];
    private List<String> mTooltipsSync[];
    private CustomXYToolTipGenerator mTooltipGenerators[];
    private TimeSeries mDatasetsSyncTickle[];

    // Dataset of error events to graph
    private TimeSeries mDatasetError;

    public DisplaySync(String name) {
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
        Control composite = createCompositeChart(parent, logParser, "Sync Status");
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

        XYBarRenderer br = new XYBarRenderer();
        mDatasetsSync = new TimePeriodValues[NUM_AUTHS];
        mTooltipsSync = new List[NUM_AUTHS];
        mTooltipGenerators = new CustomXYToolTipGenerator[NUM_AUTHS];

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

            mDatasetsSyncTickle[i] = new TimeSeries(AUTH_NAMES[i] + " tickle",
                    FixedMillisecond.class);
            tsc.addSeries(mDatasetsSyncTickle[i]);
            ls.setSeriesShape(i, ShapeUtilities.createUpTriangle(2.5f));
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
            if (event.mTag == EVENT_TICKLE) {
                int auth = getAuth(event.getValueAsString(0));
                if (auth >= 0) {
                    long msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
                    mDatasetsSyncTickle[auth].addOrUpdate(new FixedMillisecond(msec), -1);
                }
            }
        } catch (InvalidTypeException e) {
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
            } else if (auth == CONTACTS && "g".equals(key)) {
                sb.append("aggregation query: ").append(val).append("\n");
            } else if (auth == CONTACTS && "G".equals(key)) {
                sb.append("aggregation merge: ").append(val).append("\n");
            } else if (auth == CONTACTS && "n".equals(key)) {
                sb.append("num entries: ").append(val).append("\n");
            } else if (auth == CONTACTS && "p".equals(key)) {
                sb.append("photos uploaded from server: ").append(val).append("\n");
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
     * Callback to process a sync event.
     */
    @Override
    void processSyncEvent(EventContainer event, int auth, long startTime, long stopTime,
            String details, boolean newEvent, int syncSource) {
        if (!newEvent) {
            // Details arrived for a previous sync event
            // Remove event before reinserting.
            int lastItem = mDatasetsSync[auth].getItemCount();
            mDatasetsSync[auth].delete(lastItem-1, lastItem-1);
            mTooltipsSync[auth].remove(lastItem-1);
        }
        double height = getHeightFromDetails(details);
        height = height / (stopTime - startTime + 1) * 10000;
        if (height > 30) {
            height = 30;
        }
        mDatasetsSync[auth].add(new SimpleTimePeriod(startTime, stopTime), height);
        mTooltipsSync[auth].add(getTextFromDetails(auth, details, syncSource));
        mTooltipGenerators[auth].addToolTipSeries(mTooltipsSync[auth]);
        if (details.indexOf('x') >= 0 || details.indexOf('X') >= 0) {
            long msec = (long)event.sec * 1000L + (event.nsec / 1000000L);
            mDatasetError.addOrUpdate(new FixedMillisecond(msec), -1);
        }
    }

    /**
     * Gets display type
     *
     * @return display type as an integer
     */
    @Override
    int getDisplayType() {
        return DISPLAY_TYPE_SYNC;
    }
}
