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

package com.android.ddmuilib;

import com.android.ddmlib.Client;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.Log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Displays system information graphs obtained from a bugreport file or device.
 */
public class SysinfoPanel extends TablePanel implements IShellOutputReceiver {

    // UI components
    private Label mLabel;
    private Button mFetchButton;
    private Combo mDisplayMode;

    private DefaultPieDataset mDataset;

    // The bugreport file to process
    private File mDataFile;

    // To get output from adb commands
    private FileOutputStream mTempStream;

    // Selects the current display: MODE_CPU, etc.
    private int mMode = 0;

    private static final int MODE_CPU = 0;
    private static final int MODE_ALARM = 1;
    private static final int MODE_WAKELOCK = 2;
    private static final int MODE_MEMINFO = 3;
    private static final int MODE_SYNC = 4;

    // argument to dumpsys; section in the bugreport holding the data
    private static final String BUGREPORT_SECTION[] = {"cpuinfo", "alarm",
            "batteryinfo", "MEMORY INFO", "content"};

    private static final String DUMP_COMMAND[] = {"dumpsys cpuinfo",
            "dumpsys alarm", "dumpsys batteryinfo", "cat /proc/meminfo ; procrank",
            "dumpsys content"};

    private static final String CAPTIONS[] = {"CPU load", "Alarms",
            "Wakelocks", "Memory usage", "Sync"};

    /**
     * Generates the dataset to display.
     *
     * @param file The bugreport file to process.
     */
    public void generateDataset(File file) {
        mDataset.clear();
        mLabel.setText("");
        if (file == null) {
            return;
        }
        try {
            BufferedReader br = getBugreportReader(file);
            if (mMode == MODE_CPU) {
                readCpuDataset(br);
            } else if (mMode == MODE_ALARM) {
                readAlarmDataset(br);
            } else if (mMode == MODE_WAKELOCK) {
                readWakelockDataset(br);
            } else if (mMode == MODE_MEMINFO) {
                readMeminfoDataset(br);
            } else if (mMode == MODE_SYNC) {
                readSyncDataset(br);
            }
        } catch (IOException e) {
            Log.e("DDMS", e);
        }
    }

    /**
     * Sent when a new device is selected. The new device can be accessed with
     * {@link #getCurrentDevice()}
     */
    @Override
    public void deviceSelected() {
        if (getCurrentDevice() != null) {
            mFetchButton.setEnabled(true);
            loadFromDevice();
        } else {
            mFetchButton.setEnabled(false);
        }
    }

    /**
     * Sent when a new client is selected. The new client can be accessed with
     * {@link #getCurrentClient()}.
     */
    @Override
    public void clientSelected() {
    }

    /**
     * Sets the focus to the proper control inside the panel.
     */
    @Override
    public void setFocus() {
        mDisplayMode.setFocus();
    }

    /**
     * Fetches a new bugreport from the device and updates the display.
     * Fetching is asynchronous.  See also addOutput, flush, and isCancelled.
     */
    private void loadFromDevice() {
        try {
            initShellOutputBuffer();
            if (mMode == MODE_MEMINFO) {
                // Hack to add bugreport-style section header for meminfo
                mTempStream.write("------ MEMORY INFO ------\n".getBytes());
            }
            getCurrentDevice().executeShellCommand(
                    DUMP_COMMAND[mMode], this);
        } catch (IOException e) {
            Log.e("DDMS", e);
        }
    }

    /**
     * Initializes temporary output file for executeShellCommand().
     *
     * @throws IOException on file error
     */
    void initShellOutputBuffer() throws IOException {
        mDataFile = File.createTempFile("ddmsfile", ".txt");
        mDataFile.deleteOnExit();
        mTempStream = new FileOutputStream(mDataFile);
    }

    /**
     * Adds output to the temp file. IShellOutputReceiver method. Called by
     * executeShellCommand().
     */
    public void addOutput(byte[] data, int offset, int length) {
        try {
            mTempStream.write(data, offset, length);
        }
        catch (IOException e) {
            Log.e("DDMS", e);
        }
    }

    /**
     * Processes output from shell command. IShellOutputReceiver method. The
     * output is passed to generateDataset(). Called by executeShellCommand() on
     * completion.
     */
    public void flush() {
        if (mTempStream != null) {
            try {
                mTempStream.close();
                generateDataset(mDataFile);
                mTempStream = null;
                mDataFile = null;
            } catch (IOException e) {
                Log.e("DDMS", e);
            }
        }
    }

    /**
     * IShellOutputReceiver method.
     *
     * @return false - don't cancel
     */
    public boolean isCancelled() {
        return false;
    }

    /**
     * Create our controls for the UI panel.
     */
    @Override
    protected Control createControl(Composite parent) {
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout(1, false));
        top.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite buttons = new Composite(top, SWT.NONE);
        buttons.setLayout(new RowLayout());

        mDisplayMode = new Combo(buttons, SWT.PUSH);
        for (String mode : CAPTIONS) {
            mDisplayMode.add(mode);
        }
        mDisplayMode.select(mMode);
        mDisplayMode.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mMode = mDisplayMode.getSelectionIndex();
                if (mDataFile != null) {
                    generateDataset(mDataFile);
                } else if (getCurrentDevice() != null) {
                    loadFromDevice();
                }
            }
        });

        final Button loadButton = new Button(buttons, SWT.PUSH);
        loadButton.setText("Load from File");
        loadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                FileDialog fileDialog = new FileDialog(loadButton.getShell(),
                        SWT.OPEN);
                fileDialog.setText("Load bugreport");
                String filename = fileDialog.open();
                if (filename != null) {
                    mDataFile = new File(filename);
                    generateDataset(mDataFile);
                }
            }
        });

        mFetchButton = new Button(buttons, SWT.PUSH);
        mFetchButton.setText("Update from Device");
        mFetchButton.setEnabled(false);
        mFetchButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                loadFromDevice();
            }
        });

        mLabel = new Label(top, SWT.NONE);
        mLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mDataset = new DefaultPieDataset();
        JFreeChart chart = ChartFactory.createPieChart("", mDataset, false
                /* legend */, true/* tooltips */, false /* urls */);

        ChartComposite chartComposite = new ChartComposite(top,
                SWT.BORDER, chart,
                ChartComposite.DEFAULT_HEIGHT,
                ChartComposite.DEFAULT_HEIGHT,
                ChartComposite.DEFAULT_MINIMUM_DRAW_WIDTH,
                ChartComposite.DEFAULT_MINIMUM_DRAW_HEIGHT,
                3000,
                // max draw width. We don't want it to zoom, so we put a big number
                3000,
                // max draw height. We don't want it to zoom, so we put a big number
                true,  // off-screen buffer
                true,  // properties
                true,  // save
                true,  // print
                false,  // zoom
                true);
        chartComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        return top;
    }

    public void clientChanged(final Client client, int changeMask) {
        // Don't care
    }

    /**
     * Helper to open a bugreport and skip to the specified section.
     *
     * @param file File to open
     * @return Reader to bugreport file
     * @throws java.io.IOException on file error
     */
    private BufferedReader getBugreportReader(File file) throws
            IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        // Skip over the unwanted bugreport sections
        while (true) {
            String line = br.readLine();
            if (line == null) {
                Log.d("DDMS", "Service not found " + line);
                break;
            }
            if ((line.startsWith("DUMP OF SERVICE ") || line.startsWith("-----")) &&
                    line.indexOf(BUGREPORT_SECTION[mMode]) > 0) {
                break;
            }
        }
        return br;
    }

    /**
     * Parse the time string generated by BatteryStats.
     * A typical new-format string is "11d 13h 45m 39s 999ms".
     * A typical old-format string is "12.3 sec".
     * @return time in ms
     */
    private static long parseTimeMs(String s) {
        long total = 0;
        // Matches a single component e.g. "12.3 sec" or "45ms"
        Pattern p = Pattern.compile("([\\d\\.]+)\\s*([a-z]+)");
        Matcher m = p.matcher(s);
        while (m.find()) {
            String label = m.group(2);
            if ("sec".equals(label)) {
                // Backwards compatibility with old time format
                total += (long) (Double.parseDouble(m.group(1)) * 1000);
                continue;
            }
            long value = Integer.parseInt(m.group(1));
            if ("d".equals(label)) {
                total += value * 24 * 60 * 60 * 1000;
            } else if ("h".equals(label)) {
                total += value * 60 * 60 * 1000;
            } else if ("m".equals(label)) {
                total += value * 60 * 1000;
            } else if ("s".equals(label)) {
                total += value * 1000;
            } else if ("ms".equals(label)) {
                total += value;
            }
        }
        return total;
    }
    /**
     * Processes wakelock information from bugreport. Updates mDataset with the
     * new data.
     *
     * @param br Reader providing the content
     * @throws IOException if error reading file
     */
    void readWakelockDataset(BufferedReader br) throws IOException {
        Pattern lockPattern = Pattern.compile("Wake lock (\\S+): (.+) partial");
        Pattern totalPattern = Pattern.compile("Total: (.+) uptime");
        double total = 0;
        boolean inCurrent = false;

        while (true) {
            String line = br.readLine();
            if (line == null || line.startsWith("DUMP OF SERVICE")) {
                // Done, or moved on to the next service
                break;
            }
            if (line.startsWith("Current Battery Usage Statistics")) {
                inCurrent = true;
            } else if (inCurrent) {
                Matcher m = lockPattern.matcher(line);
                if (m.find()) {
                    double value = parseTimeMs(m.group(2)) / 1000.;
                    mDataset.setValue(m.group(1), value);
                    total -= value;
                } else {
                    m = totalPattern.matcher(line);
                    if (m.find()) {
                        total += parseTimeMs(m.group(1)) / 1000.;
                    }
                }
            }
        }
        if (total > 0) {
            mDataset.setValue("Unlocked", total);
        }
    }

    /**
     * Processes alarm information from bugreport. Updates mDataset with the new
     * data.
     *
     * @param br Reader providing the content
     * @throws IOException if error reading file
     */
    void readAlarmDataset(BufferedReader br) throws IOException {
        Pattern pattern = Pattern
                .compile("(\\d+) alarms: Intent .*\\.([^. ]+) flags");

        while (true) {
            String line = br.readLine();
            if (line == null || line.startsWith("DUMP OF SERVICE")) {
                // Done, or moved on to the next service
                break;
            }
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                long count = Long.parseLong(m.group(1));
                String name = m.group(2);
                mDataset.setValue(name, count);
            }
        }
    }

    /**
     * Processes cpu load information from bugreport. Updates mDataset with the
     * new data.
     *
     * @param br Reader providing the content
     * @throws IOException if error reading file
     */
    void readCpuDataset(BufferedReader br) throws IOException {
        Pattern pattern = Pattern
                .compile("(\\S+): (\\S+)% = (.+)% user . (.+)% kernel");

        while (true) {
            String line = br.readLine();
            if (line == null || line.startsWith("DUMP OF SERVICE")) {
                // Done, or moved on to the next service
                break;
            }
            if (line.startsWith("Load:")) {
                mLabel.setText(line);
                continue;
            }
            Matcher m = pattern.matcher(line);
            if (m.find()) {
                String name = m.group(1);
                long both = Long.parseLong(m.group(2));
                long user = Long.parseLong(m.group(3));
                long kernel = Long.parseLong(m.group(4));
                if ("TOTAL".equals(name)) {
                    if (both < 100) {
                        mDataset.setValue("Idle", (100 - both));
                    }
                } else {
                    // Try to make graphs more useful even with rounding;
                    // log often has 0% user + 0% kernel = 1% total
                    // We arbitrarily give extra to kernel
                    if (user > 0) {
                        mDataset.setValue(name + " (user)", user);
                    }
                    if (kernel > 0) {
                        mDataset.setValue(name + " (kernel)" , both - user);
                    }
                    if (user == 0 && kernel == 0 && both > 0) {
                        mDataset.setValue(name, both);
                    }
                }
            }
        }
    }

    /**
     * Processes meminfo information from bugreport. Updates mDataset with the
     * new data.
     *
     * @param br Reader providing the content
     * @throws IOException if error reading file
     */
    void readMeminfoDataset(BufferedReader br) throws IOException {
        Pattern valuePattern = Pattern.compile("(\\d+) kB");
        long total = 0;
        long other = 0;
        mLabel.setText("PSS in kB");        

        // Scan meminfo
        while (true) {
            String line = br.readLine();
            if (line == null) {
                // End of file
                break;
            }
            Matcher m = valuePattern.matcher(line);
            if (m.find()) {
                long kb = Long.parseLong(m.group(1));
                if (line.startsWith("MemTotal")) {
                    total = kb;
                } else if (line.startsWith("MemFree")) {
                    mDataset.setValue("Free", kb);
                    total -= kb;
                } else if (line.startsWith("Slab")) {
                    mDataset.setValue("Slab", kb);
                    total -= kb;
                } else if (line.startsWith("PageTables")) {
                    mDataset.setValue("PageTables", kb);
                    total -= kb;
                } else if (line.startsWith("Buffers") && kb > 0) {
                    mDataset.setValue("Buffers", kb);
                    total -= kb;
                } else if (line.startsWith("Inactive")) {
                    mDataset.setValue("Inactive", kb);
                    total -= kb;
                } else if (line.startsWith("MemFree")) {
                    mDataset.setValue("Free", kb);
                    total -= kb;
                }
            } else {
                break;
            }
        }
        // Scan procrank
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }
            if (line.indexOf("PROCRANK") >= 0 || line.indexOf("PID") >= 0) {
                // procrank header
                continue;
            }
            if  (line.indexOf("----") >= 0) {
                //end of procrank section
                break;
            }
            // Extract pss field from procrank output
            long pss = Long.parseLong(line.substring(23, 31).trim());
            String cmdline = line.substring(43).trim().replace("/system/bin/", "");
            // Arbitrary minimum size to display
            if (pss > 2000) {
                mDataset.setValue(cmdline, pss);
            } else {
                other += pss;
            }
            total -= pss;
        }
        mDataset.setValue("Other", other);
        mDataset.setValue("Unknown", total);
    }

    /**
     * Processes sync information from bugreport. Updates mDataset with the new
     * data.
     *
     * @param br Reader providing the content
     * @throws IOException if error reading file
     */
    void readSyncDataset(BufferedReader br) throws IOException {
        while (true) {
            String line = br.readLine();
            if (line == null || line.startsWith("DUMP OF SERVICE")) {
                // Done, or moved on to the next service
                break;
            }
            if (line.startsWith(" |") && line.length() > 70) {
                String authority = line.substring(3, 18).trim();
                String duration = line.substring(61, 70).trim();
                // Duration is MM:SS or HH:MM:SS (DateUtils.formatElapsedTime)
                String durParts[] = duration.split(":");
                if (durParts.length == 2) {
                    long dur = Long.parseLong(durParts[0]) * 60 + Long
                            .parseLong(durParts[1]);
                    mDataset.setValue(authority, dur);
                } else if (duration.length() == 3) {
                    long dur = Long.parseLong(durParts[0]) * 3600
                            + Long.parseLong(durParts[1]) * 60 + Long
                            .parseLong(durParts[2]);
                    mDataset.setValue(authority, dur);
                }
            }
        }
    }
}
