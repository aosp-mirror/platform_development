/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import com.android.sdkstats.SdkStatsService;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Properties;

public class MainWindow extends ApplicationWindow {

    private final static String PING_NAME = "Traceview";
    private final static String PING_VERSION = "1.0";

    private TraceReader mReader;
    private String mTraceName;

    // A global cache of string names.
    public static HashMap<String, String> sStringCache = new HashMap<String, String>();

    public MainWindow(String traceName, TraceReader reader) {
        super(null);
        mReader = reader;
        mTraceName = traceName;
    }

    public void run() {
        setBlockOnOpen(true);
        open();
        Display.getCurrent().dispose();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Traceview: " + mTraceName);
        shell.setBounds(100, 10, 1282, 900);
    }

    @Override
    protected Control createContents(Composite parent) {
        ColorController.assignMethodColors(parent.getDisplay(), mReader.getMethods());
        SelectionController selectionController = new SelectionController();

        GridLayout gridLayout = new GridLayout(1, false);
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        parent.setLayout(gridLayout);

        Display display = parent.getDisplay();
        Color darkGray = display.getSystemColor(SWT.COLOR_DARK_GRAY);

        // Create a sash form to separate the timeline view (on top)
        // and the profile view (on bottom)
        SashForm sashForm1 = new SashForm(parent, SWT.VERTICAL);
        sashForm1.setBackground(darkGray);
        sashForm1.SASH_WIDTH = 3;
        GridData data = new GridData(GridData.FILL_BOTH);
        sashForm1.setLayoutData(data);

        // Create the timeline view
        new TimeLineView(sashForm1, mReader, selectionController);

        // Create the profile view
        new ProfileView(sashForm1, mReader, selectionController);
        return sashForm1;
    }

    /**
     * Convert the old two-file format into the current concatenated one.
     *
     * @param base Base path of the two files, i.e. base.key and base.data
     * @return Path to a temporary file that will be deleted on exit.
     * @throws IOException
     */
    private static String makeTempTraceFile(String base) throws IOException {
        // Make a temporary file that will go away on exit and prepare to
        // write into it.
        File temp = File.createTempFile(base, ".trace");
        temp.deleteOnExit();
        FileChannel dstChannel = new FileOutputStream(temp).getChannel();

        // First copy the contents of the key file into our temp file.
        FileChannel srcChannel = new FileInputStream(base + ".key").getChannel();
        long size = dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();

        // Then concatenate the data file.
        srcChannel = new FileInputStream(base + ".data").getChannel();
        dstChannel.transferFrom(srcChannel, size, srcChannel.size());

        // Clean up.
        srcChannel.close();
        dstChannel.close();

        // Return the path of the temp file.
        return temp.getPath();
    }

    /**
     * Returns the tools revision number.
     */
    private static String getRevision() {
        Properties p = new Properties();
        try{
            String toolsdir = System.getProperty("com.android.traceview.toolsdir"); //$NON-NLS-1$
            File sourceProp;
            if (toolsdir == null || toolsdir.length() == 0) {
                sourceProp = new File("source.properties"); //$NON-NLS-1$
            } else {
                sourceProp = new File(toolsdir, "source.properties"); //$NON-NLS-1$
            }
            p.load(new FileInputStream(sourceProp));
            String revision = p.getProperty("Pkg.Revision"); //$NON-NLS-1$
            if (revision != null && revision.length() > 0) {
                return revision;
            }
        } catch (FileNotFoundException e) {
            // couldn't find the file? don't ping.
        } catch (IOException e) {
            // couldn't find the file? don't ping.
        }

        return null;
    }


    public static void main(String[] args) {
        TraceReader reader = null;
        boolean regression = false;

        // ping the usage server

        String revision = getRevision();
        if (revision != null) {
            SdkStatsService.ping(PING_NAME, revision, null);
        }

        // Process command line arguments
        int argc = 0;
        int len = args.length;
        while (argc < len) {
            String arg = args[argc];
            if (arg.charAt(0) != '-') {
                break;
            }
            if (arg.equals("-r")) {
                regression = true;
            } else {
                break;
            }
            argc++;
        }
        if (argc != len - 1) {
            System.out.printf("Usage: java %s [-r] trace%n", MainWindow.class.getName());
            System.out.printf("  -r   regression only%n");
            return;
        }

        String traceName = args[len - 1];
        File file = new File(traceName);
        if (file.exists() && file.isDirectory()) {
            System.out.printf("Qemu trace files not supported yet.\n");
            System.exit(1);
            // reader = new QtraceReader(traceName);
        } else {
            // If the filename as given doesn't exist...
            if (!file.exists()) {
                // Try appending .trace.
                if (new File(traceName + ".trace").exists()) {
                    traceName = traceName + ".trace";
                // Next, see if it is the old two-file trace.
                } else if (new File(traceName + ".data").exists()
                    && new File(traceName + ".key").exists()) {
                    try {
                        traceName = makeTempTraceFile(traceName);
                    } catch (IOException e) {
                        System.err.printf("cannot convert old trace file '%s'\n", traceName);
                        System.exit(1);
                    }
                // Otherwise, give up.
                } else {
                    System.err.printf("trace file '%s' not found\n", traceName);
                    System.exit(1);
                }
            }

            reader = new DmTraceReader(traceName, regression);
        }
        reader.getTraceUnits().setTimeScale(TraceUnits.TimeScale.MilliSeconds);
        new MainWindow(traceName, reader).run();
    }
}
