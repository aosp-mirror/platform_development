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

package com.android.eventanalyzer;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.ILogOutput;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.log.EventContainer;
import com.android.ddmlib.log.EventLogParser;
import com.android.ddmlib.log.InvalidTypeException;
import com.android.ddmlib.log.LogReceiver;
import com.android.ddmlib.log.LogReceiver.ILogListener;
import com.android.ddmlib.log.LogReceiver.LogEntry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;

/**
 * Connects to a device using ddmlib and analyze its event log.
 */
public class EventAnalyzer implements ILogListener {

    private final static int TAG_ACTIVITY_LAUNCH_TIME = 30009;
    private final static char DATA_SEPARATOR = ',';

    private final static String CVS_EXT = ".csv";
    private final static String TAG_FILE_EXT = ".tag"; //$NON-NLS-1$

    private EventLogParser mParser;
    private TreeMap<String, ArrayList<Long>> mLaunchMap = new TreeMap<String, ArrayList<Long>>();

    String mInputTextFile = null;
    String mInputBinaryFile = null;
    String mInputDevice = null;
    String mInputFolder = null;
    String mAlternateTagFile = null;
    String mOutputFile = null;

    public static void main(String[] args) {
        new EventAnalyzer().run(args);
    }

    private void run(String[] args) {
        if (args.length == 0) {
            printUsageAndQuit();
        }

        int index = 0;
        do {
            String argument = args[index++];

            if ("-s".equals(argument)) {
                checkInputValidity("-s");

                if (index == args.length) {
                    printUsageAndQuit();
                }

                mInputDevice = args[index++];
            } else if ("-fb".equals(argument)) {
                checkInputValidity("-fb");

                if (index == args.length) {
                    printUsageAndQuit();
                }

                mInputBinaryFile = args[index++];
            } else if ("-ft".equals(argument)) {
                checkInputValidity("-ft");

                if (index == args.length) {
                    printUsageAndQuit();
                }

                mInputTextFile = args[index++];
            } else if ("-F".equals(argument)) {
                checkInputValidity("-F");

                if (index == args.length) {
                    printUsageAndQuit();
                }

                mInputFolder = args[index++];
            } else if ("-t".equals(argument)) {
                if (index == args.length) {
                    printUsageAndQuit();
                }

                mAlternateTagFile = args[index++];
            } else {
                // get the filepath and break.
                mOutputFile = argument;

                // should not be any other device.
                if (index < args.length) {
                    printAndExit("Too many arguments!", false /* terminate */);
                }
            }
        } while (index < args.length);

        if ((mInputTextFile == null && mInputBinaryFile == null && mInputFolder == null &&
                mInputDevice == null)) {
            printUsageAndQuit();
        }

        File outputParent = new File(mOutputFile).getParentFile();
        if (outputParent == null || outputParent.isDirectory() == false) {
            printAndExit(String.format("%1$s is not a valid ouput file", mOutputFile),
                    false /* terminate */);
        }

        // redirect the log output to /dev/null
        Log.setLogOutput(new ILogOutput() {
            public void printAndPromptLog(LogLevel logLevel, String tag, String message) {
                // pass
            }

            public void printLog(LogLevel logLevel, String tag, String message) {
                // pass
            }
        });

        try {
            if (mInputBinaryFile != null) {
                parseBinaryLogFile();
            } else if (mInputTextFile != null) {
                parseTextLogFile(mInputTextFile);
            } else if (mInputFolder != null) {
                parseFolder(mInputFolder);
            } else if (mInputDevice != null) {
                parseLogFromDevice();
            }

            // analyze the data gathered by the parser methods
            analyzeData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses a binary event log file located at {@link #mInputBinaryFile}.
     * @throws IOException
     */
    private void parseBinaryLogFile() throws IOException {
        mParser = new EventLogParser();

        String tagFile = mInputBinaryFile + TAG_FILE_EXT;
        if (mParser.init(tagFile) == false) {
            // if we have an alternate location
            if (mAlternateTagFile != null) {
                if (mParser.init(mAlternateTagFile) == false) {
                    printAndExit("Failed to get event tags from " + mAlternateTagFile,
                            false /* terminate*/);
                }
            } else {
                printAndExit("Failed to get event tags from " + tagFile, false /* terminate*/);
            }
        }

        LogReceiver receiver = new LogReceiver(this);

        byte[] buffer = new byte[256];

        FileInputStream fis = new FileInputStream(mInputBinaryFile);

        int count;
        while ((count = fis.read(buffer)) != -1) {
            receiver.parseNewData(buffer, 0, count);
        }
    }

    /**
     * Parse a text Log file.
     * @param filePath the location of the file.
     * @throws IOException
     */
    private void parseTextLogFile(String filePath) throws IOException {
        mParser = new EventLogParser();

        String tagFile = filePath + TAG_FILE_EXT;
        if (mParser.init(tagFile) == false) {
            // if we have an alternate location
            if (mAlternateTagFile != null) {
                if (mParser.init(mAlternateTagFile) == false) {
                    printAndExit("Failed to get event tags from " + mAlternateTagFile,
                            false /* terminate*/);
                }
            } else {
                printAndExit("Failed to get event tags from " + tagFile, false /* terminate*/);
            }
        }

        // read the lines from the file and process them.
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath)));

        String line;
        while ((line = reader.readLine()) != null) {
            processEvent(mParser.parse(line));
        }
    }

    private void parseLogFromDevice() throws IOException {
        // init the lib
        AndroidDebugBridge.init(false /* debugger support */);

        try {
            AndroidDebugBridge bridge = AndroidDebugBridge.createBridge();

            // we can't just ask for the device list right away, as the internal thread getting
            // them from ADB may not be done getting the first list.
            // Since we don't really want getDevices() to be blocking, we wait here manually.
            int count = 0;
            while (bridge.hasInitialDeviceList() == false) {
                try {
                    Thread.sleep(100);
                    count++;
                } catch (InterruptedException e) {
                    // pass
                }

                // let's not wait > 10 sec.
                if (count > 100) {
                    printAndExit("Timeout getting device list!", true /* terminate*/);
                }
            }

            // now get the devices
            IDevice[] devices = bridge.getDevices();

            for (IDevice device : devices) {
                if (device.getSerialNumber().equals(mInputDevice)) {
                    grabLogFrom(device);
                    return;
                }
            }

            System.err.println("Could not find " + mInputDevice);
        } finally {
            AndroidDebugBridge.terminate();
        }
    }

    /**
     * Parses the log files located in the folder, and its sub-folders.
     * @param folderPath the path to the folder.
     */
    private void parseFolder(String folderPath) {
        File f = new File(folderPath);
        if (f.isDirectory() == false) {
            printAndExit(String.format("%1$s is not a valid folder", folderPath),
                    false /* terminate */);
        }

        String[] files = f.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".tag") == false;
            }
        });

        for (String file : files) {
            try {
                f = new File(folderPath + File.separator + file);
                if (f.isDirectory()) {
                    parseFolder(f.getAbsolutePath());
                } else {
                    parseTextLogFile(f.getAbsolutePath());
                }
            } catch (IOException e) {
                // ignore this file.
            }
        }
    }

    private void grabLogFrom(IDevice device) throws IOException {
        mParser = new EventLogParser();
        if (mParser.init(device) == false) {
            printAndExit("Failed to get event-log-tags from " + device.getSerialNumber(),
                    true /* terminate*/);
        }

        LogReceiver receiver = new LogReceiver(this);

        device.runEventLogService(receiver);
    }

    /**
     * Analyze the data and writes it to {@link #mOutputFile}
     * @throws IOException
     */
    private void analyzeData() throws IOException {
        BufferedWriter writer = null;
        try {
            // make sure the file name has the proper extension.
            if (mOutputFile.toLowerCase().endsWith(CVS_EXT) == false) {
                mOutputFile = mOutputFile + CVS_EXT;
            }

            writer = new BufferedWriter(new FileWriter(mOutputFile));
            StringBuilder builder = new StringBuilder();

            // write the list of launch start. One column per activity.
            Set<String> activities = mLaunchMap.keySet();

            // write the column headers.
            for (String activity : activities) {
                builder.append(activity).append(DATA_SEPARATOR);
            }
            writer.write(builder.append('\n').toString());

            // loop on the activities and write their values.
            boolean moreValues = true;
            int index = 0;
            while (moreValues) {
                moreValues = false;
                builder.setLength(0);

                for (String activity : activities) {
                    // get the activity list.
                    ArrayList<Long> list = mLaunchMap.get(activity);
                    if (index < list.size()) {
                        moreValues = true;
                        builder.append(list.get(index).longValue()).append(DATA_SEPARATOR);
                    } else {
                        builder.append(DATA_SEPARATOR);
                    }
                }

                // write the line.
                if (moreValues) {
                    writer.write(builder.append('\n').toString());
                }

                index++;
            }

            // write per-activity stats.
            for (String activity : activities) {
                builder.setLength(0);
                builder.append(activity).append(DATA_SEPARATOR);

                // get the activity list.
                ArrayList<Long> list = mLaunchMap.get(activity);

                // sort the list
                Collections.sort(list);

                // write min/max
                builder.append(list.get(0).longValue()).append(DATA_SEPARATOR);
                builder.append(list.get(list.size()-1).longValue()).append(DATA_SEPARATOR);

                // write median value
                builder.append(list.get(list.size()/2).longValue()).append(DATA_SEPARATOR);

                // compute and write average
                long total = 0; // despite being encoded on a long, the values are low enough that
                                // a Long should be enough to compute the total
                for (Long value : list) {
                    total += value.longValue();
                }
                builder.append(total / list.size()).append(DATA_SEPARATOR);

                // finally write the data.
                writer.write(builder.append('\n').toString());
            }
        } finally {
            writer.close();
        }
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.log.LogReceiver.ILogListener#newData(byte[], int, int)
     */
    public void newData(byte[] data, int offset, int length) {
        // we ignore raw data. New entries are processed in #newEntry(LogEntry)
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.log.LogReceiver.ILogListener#newEntry(com.android.ddmlib.log.LogReceiver.LogEntry)
     */
    public void newEntry(LogEntry entry) {
        // parse and process the entry data.
        processEvent(mParser.parse(entry));
    }

    private void processEvent(EventContainer event) {
        if (event != null && event.mTag == TAG_ACTIVITY_LAUNCH_TIME) {
            // get the activity name
            try {
                String name = event.getValueAsString(0);

                // get the launch time
                Object value = event.getValue(1);
                if (value instanceof Long) {
                    addLaunchTime(name, (Long)value);
                }

            } catch (InvalidTypeException e) {
                // Couldn't get the name as a string...
                // Ignore this event.
            }
        }
    }

    private void addLaunchTime(String name, Long value) {
        ArrayList<Long> list = mLaunchMap.get(name);

        if (list == null) {
            list = new ArrayList<Long>();
            mLaunchMap.put(name, list);
        }

        list.add(value);
    }

    private void checkInputValidity(String option) {
        if (mInputTextFile != null || mInputBinaryFile != null) {
            printAndExit(String.format("ERROR: %1$s cannot be used with an input file.", option),
                    false /* terminate */);
        } else if (mInputFolder != null) {
            printAndExit(String.format("ERROR: %1$s cannot be used with an input file.", option),
                    false /* terminate */);
        } else if (mInputDevice != null) {
            printAndExit(String.format("ERROR: %1$s cannot be used with an input device serial number.",
                    option), false /* terminate */);
        }
    }

    private static void printUsageAndQuit() {
        // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
        System.out.println("Usage:");
        System.out.println("   eventanalyzer [-t <TAG_FILE>] <SOURCE> <OUTPUT>");
        System.out.println("");
        System.out.println("Possible sources:");
        System.out.println("   -fb <file>    The path to a binary event log, gathered by dumpeventlog");
        System.out.println("   -ft <file>    The path to a text event log, gathered by adb logcat -b events");
        System.out.println("   -F <folder>   The path to a folder containing multiple text log files.");
        System.out.println("   -s <serial>   The serial number of the Device to grab the event log from.");
        System.out.println("Options:");
        System.out.println("   -t <file>     The path to tag file to use in case the one associated with");
        System.out.println("                 the source is missing");

        System.exit(1);
    }


    private static void printAndExit(String message, boolean terminate) {
        System.out.println(message);
        if (terminate) {
            AndroidDebugBridge.terminate();
        }
        System.exit(1);
    }
}
