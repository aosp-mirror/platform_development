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

package com.android.commands.monkey;

import android.content.ComponentName;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Random;
import android.util.Log;

/**
 * monkey event queue. It takes a script to produce events sample script format:
 *
 * <pre>
 * type= raw events
 * count= 10
 * speed= 1.0
 * start data &gt;&gt;
 * captureDispatchPointer(5109520,5109520,0,230.75429,458.1814,0.20784314,0.06666667,0,0.0,0.0,65539,0)
 * captureDispatchKey(5113146,5113146,0,20,0,0,0,0)
 * captureDispatchFlip(true)
 * ...
 * </pre>
 */
public class MonkeySourceScript implements MonkeyEventSource {
    private int mEventCountInScript = 0; // total number of events in the file

    private int mVerbose = 0;

    private double mSpeed = 1.0;

    private String mScriptFileName;

    private MonkeyEventQueue mQ;

    private static final String HEADER_COUNT = "count=";

    private static final String HEADER_SPEED = "speed=";

    private long mLastRecordedDownTimeKey = 0;

    private long mLastRecordedDownTimeMotion = 0;

    private long mLastExportDownTimeKey = 0;

    private long mLastExportDownTimeMotion = 0;

    private long mLastExportEventTime = -1;

    private long mLastRecordedEventTime = -1;

    private static final boolean THIS_DEBUG = false;

    // a parameter that compensates the difference of real elapsed time and
    // time in theory
    private static final long SLEEP_COMPENSATE_DIFF = 16;

    // maximum number of events that we read at one time
    private static final int MAX_ONE_TIME_READS = 100;

    // event key word in the capture log
    private static final String EVENT_KEYWORD_POINTER = "DispatchPointer";

    private static final String EVENT_KEYWORD_TRACKBALL = "DispatchTrackball";

    private static final String EVENT_KEYWORD_KEY = "DispatchKey";

    private static final String EVENT_KEYWORD_FLIP = "DispatchFlip";

    private static final String EVENT_KEYWORD_KEYPRESS = "DispatchPress";

    private static final String EVENT_KEYWORD_ACTIVITY = "LaunchActivity";

    private static final String EVENT_KEYWORD_INSTRUMENTATION = "LaunchInstrumentation";

    private static final String EVENT_KEYWORD_WAIT = "UserWait";

    private static final String EVENT_KEYWORD_LONGPRESS = "LongPress";

    private static final String EVENT_KEYWORD_POWERLOG = "PowerLog";

    private static final String EVENT_KEYWORD_WRITEPOWERLOG = "WriteLog";

    private static final String EVENT_KEYWORD_RUNCMD = "RunCmd";

    private static final String EVENT_KEYWORD_TAP = "Tap";

    private static final String EVENT_KEYWORD_PROFILE_WAIT = "ProfileWait";

    private static final String EVENT_KEYWORD_DEVICE_WAKEUP = "DeviceWakeUp";

    private static final String EVENT_KEYWORD_INPUT_STRING = "DispatchString";

    private static final String EVENT_KEYWORD_PRESSANDHOLD = "PressAndHold";

    private static final String EVENT_KEYWORD_DRAG = "Drag";

    private static final String EVENT_KEYWORD_PINCH_ZOOM = "PinchZoom";

    private static final String EVENT_KEYWORD_START_FRAMERATE_CAPTURE = "StartCaptureFramerate";

    private static final String EVENT_KEYWORD_END_FRAMERATE_CAPTURE = "EndCaptureFramerate";

    // a line at the end of the header
    private static final String STARTING_DATA_LINE = "start data >>";

    private boolean mFileOpened = false;

    private static int LONGPRESS_WAIT_TIME = 2000; // wait time for the long

    private long mProfileWaitTime = 5000; //Wait time for each user profile

    private long mDeviceSleepTime = 30000; //Device sleep time

    FileInputStream mFStream;

    DataInputStream mInputStream;

    BufferedReader mBufferedReader;

    /**
     * Creates a MonkeySourceScript instance.
     *
     * @param filename The filename of the script (on the device).
     * @param throttle The amount of time in ms to sleep between events.
     */
    public MonkeySourceScript(Random random, String filename, long throttle,
            boolean randomizeThrottle, long profileWaitTime, long deviceSleepTime) {
        mScriptFileName = filename;
        mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
        mProfileWaitTime = profileWaitTime;
        mDeviceSleepTime = deviceSleepTime;
    }

    /**
     * Resets the globals used to timeshift events.
     */
    private void resetValue() {
        mLastRecordedDownTimeKey = 0;
        mLastRecordedDownTimeMotion = 0;
        mLastRecordedEventTime = -1;
        mLastExportDownTimeKey = 0;
        mLastExportDownTimeMotion = 0;
        mLastExportEventTime = -1;
    }

    /**
     * Reads the header of the script file.
     *
     * @return True if the file header could be parsed, and false otherwise.
     * @throws IOException If there was an error reading the file.
     */
    private boolean readHeader() throws IOException {
        mFileOpened = true;

        mFStream = new FileInputStream(mScriptFileName);
        mInputStream = new DataInputStream(mFStream);
        mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));

        String line;

        while ((line = mBufferedReader.readLine()) != null) {
            line = line.trim();

            if (line.indexOf(HEADER_COUNT) >= 0) {
                try {
                    String value = line.substring(HEADER_COUNT.length() + 1).trim();
                    mEventCountInScript = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.err.println(e);
                    return false;
                }
            } else if (line.indexOf(HEADER_SPEED) >= 0) {
                try {
                    String value = line.substring(HEADER_COUNT.length() + 1).trim();
                    mSpeed = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    System.err.println(e);
                    return false;
                }
            } else if (line.indexOf(STARTING_DATA_LINE) >= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Reads a number of lines and passes the lines to be processed.
     *
     * @return The number of lines read.
     * @throws IOException If there was an error reading the file.
     */
    private int readLines() throws IOException {
        String line;
        for (int i = 0; i < MAX_ONE_TIME_READS; i++) {
            line = mBufferedReader.readLine();
            if (line == null) {
                return i;
            }
            line.trim();
            processLine(line);
        }
        return MAX_ONE_TIME_READS;
    }




    /**
     * Creates an event and adds it to the event queue. If the parameters are
     * not understood, they are ignored and no events are added.
     *
     * @param s The entire string from the script file.
     * @param args An array of arguments extracted from the script file line.
     */
    private void handleEvent(String s, String[] args) {
        // Handle key event
        if (s.indexOf(EVENT_KEYWORD_KEY) >= 0 && args.length == 8) {
            try {
                System.out.println(" old key\n");
                long downTime = Long.parseLong(args[0]);
                long eventTime = Long.parseLong(args[1]);
                int action = Integer.parseInt(args[2]);
                int code = Integer.parseInt(args[3]);
                int repeat = Integer.parseInt(args[4]);
                int metaState = Integer.parseInt(args[5]);
                int device = Integer.parseInt(args[6]);
                int scancode = Integer.parseInt(args[7]);

                MonkeyKeyEvent e = new MonkeyKeyEvent(downTime, eventTime, action, code, repeat,
                        metaState, device, scancode);
                System.out.println(" Key code " + code + "\n");

                mQ.addLast(e);
                System.out.println("Added key up \n");
            } catch (NumberFormatException e) {
            }
            return;
        }

        // Handle trackball or pointer events
        if ((s.indexOf(EVENT_KEYWORD_POINTER) >= 0 || s.indexOf(EVENT_KEYWORD_TRACKBALL) >= 0)
                && args.length == 12) {
            try {
                long downTime = Long.parseLong(args[0]);
                long eventTime = Long.parseLong(args[1]);
                int action = Integer.parseInt(args[2]);
                float x = Float.parseFloat(args[3]);
                float y = Float.parseFloat(args[4]);
                float pressure = Float.parseFloat(args[5]);
                float size = Float.parseFloat(args[6]);
                int metaState = Integer.parseInt(args[7]);
                float xPrecision = Float.parseFloat(args[8]);
                float yPrecision = Float.parseFloat(args[9]);
                int device = Integer.parseInt(args[10]);
                int edgeFlags = Integer.parseInt(args[11]);

                MonkeyMotionEvent e;
                if (s.indexOf("Pointer") > 0) {
                    e = new MonkeyTouchEvent(action);
                } else {
                    e = new MonkeyTrackballEvent(action);
                }

                e.setDownTime(downTime)
                        .setEventTime(eventTime)
                        .setMetaState(metaState)
                        .setPrecision(xPrecision, yPrecision)
                        .setDeviceId(device)
                        .setEdgeFlags(edgeFlags)
                        .addPointer(0, x, y, pressure, size);
                mQ.addLast(e);
            } catch (NumberFormatException e) {
            }
            return;
        }

        // Handle tap event
        if ((s.indexOf(EVENT_KEYWORD_TAP) >= 0) && args.length >= 2) {
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                long tapDuration = 0;
                if (args.length == 3) {
                    tapDuration = Long.parseLong(args[2]);
                }

                // Set the default parameters
                long downTime = SystemClock.uptimeMillis();
                MonkeyMotionEvent e1 = new MonkeyTouchEvent(MotionEvent.ACTION_DOWN)
                        .setDownTime(downTime)
                        .setEventTime(downTime)
                        .addPointer(0, x, y, 1, 5);
                mQ.addLast(e1);
                if (tapDuration > 0){
                    mQ.addLast(new MonkeyWaitEvent(tapDuration));
                }
                MonkeyMotionEvent e2 = new MonkeyTouchEvent(MotionEvent.ACTION_UP)
                        .setDownTime(downTime)
                        .setEventTime(downTime)
                        .addPointer(0, x, y, 1, 5);
                mQ.addLast(e2);
            } catch (NumberFormatException e) {
                System.err.println("// " + e.toString());
            }
            return;
        }

        //Handle the press and hold
        if ((s.indexOf(EVENT_KEYWORD_PRESSANDHOLD) >= 0) && args.length == 3) {
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                long pressDuration = Long.parseLong(args[2]);

                // Set the default parameters
                long downTime = SystemClock.uptimeMillis();

                MonkeyMotionEvent e1 = new MonkeyTouchEvent(MotionEvent.ACTION_DOWN)
                        .setDownTime(downTime)
                        .setEventTime(downTime)
                        .addPointer(0, x, y, 1, 5);
                MonkeyWaitEvent e2 = new MonkeyWaitEvent(pressDuration);
                MonkeyMotionEvent e3 = new MonkeyTouchEvent(MotionEvent.ACTION_UP)
                        .setDownTime(downTime + pressDuration)
                        .setEventTime(downTime + pressDuration)
                        .addPointer(0, x, y, 1, 5);
                mQ.addLast(e1);
                mQ.addLast(e2);
                mQ.addLast(e2);

            } catch (NumberFormatException e) {
                System.err.println("// " + e.toString());
            }
            return;
        }

        // Handle drag event
        if ((s.indexOf(EVENT_KEYWORD_DRAG) >= 0) && args.length == 5) {
            float xStart = Float.parseFloat(args[0]);
            float yStart = Float.parseFloat(args[1]);
            float xEnd = Float.parseFloat(args[2]);
            float yEnd = Float.parseFloat(args[3]);
            int stepCount = Integer.parseInt(args[4]);

            float x = xStart;
            float y = yStart;
            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            if (stepCount > 0) {
                float xStep = (xEnd - xStart) / stepCount;
                float yStep = (yEnd - yStart) / stepCount;

                MonkeyMotionEvent e =
                        new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downTime)
                                .setEventTime(eventTime).addPointer(0, x, y, 1, 5);
                mQ.addLast(e);

                for (int i = 0; i < stepCount; ++i) {
                    x += xStep;
                    y += yStep;
                    eventTime = SystemClock.uptimeMillis();
                    e = new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downTime)
                        .setEventTime(eventTime).addPointer(0, x, y, 1, 5);
                    mQ.addLast(e);
                }

                eventTime = SystemClock.uptimeMillis();
                e = new MonkeyTouchEvent(MotionEvent.ACTION_UP).setDownTime(downTime)
                    .setEventTime(eventTime).addPointer(0, x, y, 1, 5);
                mQ.addLast(e);
            }
        }

        // Handle pinch or zoom action
        if ((s.indexOf(EVENT_KEYWORD_PINCH_ZOOM) >= 0) && args.length == 9) {
            //Parse the parameters
            float pt1xStart = Float.parseFloat(args[0]);
            float pt1yStart = Float.parseFloat(args[1]);
            float pt1xEnd = Float.parseFloat(args[2]);
            float pt1yEnd = Float.parseFloat(args[3]);

            float pt2xStart = Float.parseFloat(args[4]);
            float pt2yStart = Float.parseFloat(args[5]);
            float pt2xEnd = Float.parseFloat(args[6]);
            float pt2yEnd = Float.parseFloat(args[7]);

            int stepCount = Integer.parseInt(args[8]);

            float x1 = pt1xStart;
            float y1 = pt1yStart;
            float x2 = pt2xStart;
            float y2 = pt2yStart;

            long downTime = SystemClock.uptimeMillis();
            long eventTime = SystemClock.uptimeMillis();

            if (stepCount > 0) {
                float pt1xStep = (pt1xEnd - pt1xStart) / stepCount;
                float pt1yStep = (pt1yEnd - pt1yStart) / stepCount;

                float pt2xStep = (pt2xEnd - pt2xStart) / stepCount;
                float pt2yStep = (pt2yEnd - pt2yStart) / stepCount;

                mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN).setDownTime(downTime)
                        .setEventTime(eventTime).addPointer(0, x1, y1, 1, 5));

                mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_DOWN
                        | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT)).setDownTime(downTime)
                        .addPointer(0, x1, y1).addPointer(1, x2, y2).setIntermediateNote(true));

                for (int i = 0; i < stepCount; ++i) {
                    x1 += pt1xStep;
                    y1 += pt1yStep;
                    x2 += pt2xStep;
                    y2 += pt2yStep;

                    eventTime = SystemClock.uptimeMillis();
                    mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE).setDownTime(downTime)
                            .setEventTime(eventTime).addPointer(0, x1, y1, 1, 5).addPointer(1, x2,
                                    y2, 1, 5));
                }
                eventTime = SystemClock.uptimeMillis();
                mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_UP)
                        .setDownTime(downTime).setEventTime(eventTime).addPointer(0, x1, y1)
                        .addPointer(1, x2, y2));
            }
        }

        // Handle flip events
        if (s.indexOf(EVENT_KEYWORD_FLIP) >= 0 && args.length == 1) {
            boolean keyboardOpen = Boolean.parseBoolean(args[0]);
            MonkeyFlipEvent e = new MonkeyFlipEvent(keyboardOpen);
            mQ.addLast(e);
        }

        // Handle launch events
        if (s.indexOf(EVENT_KEYWORD_ACTIVITY) >= 0 && args.length >= 2) {
            String pkg_name = args[0];
            String cl_name = args[1];
            long alarmTime = 0;

            ComponentName mApp = new ComponentName(pkg_name, cl_name);

            if (args.length > 2) {
                try {
                    alarmTime = Long.parseLong(args[2]);
                } catch (NumberFormatException e) {
                    System.err.println("// " + e.toString());
                    return;
                }
            }

            if (args.length == 2) {
                MonkeyActivityEvent e = new MonkeyActivityEvent(mApp);
                mQ.addLast(e);
            } else {
                MonkeyActivityEvent e = new MonkeyActivityEvent(mApp, alarmTime);
                mQ.addLast(e);
            }
            return;
        }

        //Handle the device wake up event
        if (s.indexOf(EVENT_KEYWORD_DEVICE_WAKEUP) >= 0){
            String pkg_name = "com.google.android.powerutil";
            String cl_name = "com.google.android.powerutil.WakeUpScreen";
            long deviceSleepTime = mDeviceSleepTime;

            //Start the wakeUpScreen test activity to turn off the screen.
            ComponentName mApp = new ComponentName(pkg_name, cl_name);
            mQ.addLast(new MonkeyActivityEvent(mApp, deviceSleepTime));

            //inject the special key for the wakeUpScreen test activity.
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_0));
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_0));

            //Add the wait event after the device sleep event so that the monkey
            //can continue after the device wake up.
            mQ.addLast(new MonkeyWaitEvent(deviceSleepTime + 3000));

            //Insert the menu key to unlock the screen
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU));
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU));

            //Insert the back key to dismiss the test activity
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
            mQ.addLast(new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));

            return;
        }

       // Handle launch instrumentation events
        if (s.indexOf(EVENT_KEYWORD_INSTRUMENTATION) >= 0 && args.length == 2) {
            String test_name = args[0];
            String runner_name = args[1];
            MonkeyInstrumentationEvent e = new MonkeyInstrumentationEvent(test_name, runner_name);
            mQ.addLast(e);
            return;
        }

        // Handle wait events
        if (s.indexOf(EVENT_KEYWORD_WAIT) >= 0 && args.length == 1) {
            try {
                long sleeptime = Integer.parseInt(args[0]);
                MonkeyWaitEvent e = new MonkeyWaitEvent(sleeptime);
                mQ.addLast(e);
            } catch (NumberFormatException e) {
            }
            return;
        }


        // Handle the profile wait time
        if (s.indexOf(EVENT_KEYWORD_PROFILE_WAIT) >= 0) {
            MonkeyWaitEvent e = new MonkeyWaitEvent(mProfileWaitTime);
            mQ.addLast(e);
            return;
        }

        // Handle keypress events
        if (s.indexOf(EVENT_KEYWORD_KEYPRESS) >= 0 && args.length == 1) {
            String key_name = args[0];
            int keyCode = MonkeySourceRandom.getKeyCode(key_name);
            MonkeyKeyEvent e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            mQ.addLast(e);
            e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, keyCode);
            mQ.addLast(e);
            return;
        }

        // Handle longpress events
        if (s.indexOf(EVENT_KEYWORD_LONGPRESS) >= 0) {
            MonkeyKeyEvent e;
            e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER);
            mQ.addLast(e);
            MonkeyWaitEvent we = new MonkeyWaitEvent(LONGPRESS_WAIT_TIME);
            mQ.addLast(we);
            e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER);
            mQ.addLast(e);
        }

        //The power log event is mainly for the automated power framework
        if (s.indexOf(EVENT_KEYWORD_POWERLOG) >= 0 && args.length > 0) {
            String power_log_type = args[0];
            String test_case_status;

            if (args.length == 1){
                MonkeyPowerEvent e = new MonkeyPowerEvent(power_log_type);
                mQ.addLast(e);
            } else if (args.length == 2){
                test_case_status = args[1];
                MonkeyPowerEvent e = new MonkeyPowerEvent(power_log_type, test_case_status);
                mQ.addLast(e);
            }
        }

        //Write power log to sdcard
        if (s.indexOf(EVENT_KEYWORD_WRITEPOWERLOG) >= 0) {
            MonkeyPowerEvent e = new MonkeyPowerEvent();
            mQ.addLast(e);
        }

      //Run the shell command
        if (s.indexOf(EVENT_KEYWORD_RUNCMD) >= 0 && args.length == 1) {
            String cmd = args[0];
            MonkeyCommandEvent e = new MonkeyCommandEvent(cmd);
            mQ.addLast(e);
        }

        //Input the string through the shell command
        if (s.indexOf(EVENT_KEYWORD_INPUT_STRING) >= 0 && args.length == 1) {
            String input = args[0];
            String cmd = "input text " + input;
            MonkeyCommandEvent e = new MonkeyCommandEvent(cmd);
            mQ.addLast(e);
            return;
        }

        if (s.indexOf(EVENT_KEYWORD_START_FRAMERATE_CAPTURE) >= 0) {
            MonkeyGetFrameRateEvent e = new MonkeyGetFrameRateEvent("start");
            mQ.addLast(e);
            return;
        }

        if (s.indexOf(EVENT_KEYWORD_END_FRAMERATE_CAPTURE) >= 0 && args.length == 1) {
            String input = args[0];
            MonkeyGetFrameRateEvent e = new MonkeyGetFrameRateEvent("end", input);
            mQ.addLast(e);
            return;
        }



    }

    /**
     * Extracts an event and a list of arguments from a line. If the line does
     * not match the format required, it is ignored.
     *
     * @param line A string in the form {@code cmd(arg1,arg2,arg3)}.
     */
    private void processLine(String line) {
        int index1 = line.indexOf('(');
        int index2 = line.indexOf(')');

        if (index1 < 0 || index2 < 0) {
            return;
        }

        String[] args = line.substring(index1 + 1, index2).split(",");

        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
        }

        handleEvent(line, args);
    }

    /**
     * Closes the script file.
     *
     * @throws IOException If there was an error closing the file.
     */
    private void closeFile() throws IOException {
        mFileOpened = false;

        try {
            mFStream.close();
            mInputStream.close();
        } catch (NullPointerException e) {
            // File was never opened so it can't be closed.
        }
    }

    /**
     * Read next batch of events from the script file into the event queue.
     * Checks if the script is open and then reads the next MAX_ONE_TIME_READS
     * events or reads until the end of the file. If no events are read, then
     * the script is closed.
     *
     * @throws IOException If there was an error reading the file.
     */
    private void readNextBatch() throws IOException {
        int linesRead = 0;

        if (THIS_DEBUG) {
            System.out.println("readNextBatch(): reading next batch of events");
        }

        if (!mFileOpened) {
            resetValue();
            readHeader();
        }

        linesRead = readLines();

        if (linesRead == 0) {
            closeFile();
        }
    }

    /**
     * Sleep for a period of given time. Used to introduce latency between
     * events.
     *
     * @param time The amount of time to sleep in ms
     */
    private void needSleep(long time) {
        if (time < 1) {
            return;
        }
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Checks if the file can be opened and if the header is valid.
     *
     * @return True if the file exists and the header is valid, false otherwise.
     */
    public boolean validate() {
        boolean validHeader;
        try {
            validHeader = readHeader();
            closeFile();
        } catch (IOException e) {
            return false;
        }

        if (mVerbose > 0) {
            System.out.println("Replaying " + mEventCountInScript + " events with speed " + mSpeed);
        }
        return validHeader;
    }

    public void setVerbose(int verbose) {
        mVerbose = verbose;
    }

    /**
     * Adjust key downtime and eventtime according to both recorded values and
     * current system time.
     *
     * @param e A KeyEvent
     */
    private void adjustKeyEventTime(MonkeyKeyEvent e) {
        if (e.getEventTime() < 0) {
            return;
        }
        long thisDownTime = 0;
        long thisEventTime = 0;
        long expectedDelay = 0;

        if (mLastRecordedEventTime <= 0) {
            // first time event
            thisDownTime = SystemClock.uptimeMillis();
            thisEventTime = thisDownTime;
        } else {
            if (e.getDownTime() != mLastRecordedDownTimeKey) {
                thisDownTime = e.getDownTime();
            } else {
                thisDownTime = mLastExportDownTimeKey;
            }
            expectedDelay = (long) ((e.getEventTime() - mLastRecordedEventTime) * mSpeed);
            thisEventTime = mLastExportEventTime + expectedDelay;
            // add sleep to simulate everything in recording
            needSleep(expectedDelay - SLEEP_COMPENSATE_DIFF);
        }
        mLastRecordedDownTimeKey = e.getDownTime();
        mLastRecordedEventTime = e.getEventTime();
        e.setDownTime(thisDownTime);
        e.setEventTime(thisEventTime);
        mLastExportDownTimeKey = thisDownTime;
        mLastExportEventTime = thisEventTime;
    }

    /**
     * Adjust motion downtime and eventtime according to current system time.
     *
     * @param e A KeyEvent
     */
    private void adjustMotionEventTime(MonkeyMotionEvent e) {
        long updatedDownTime = 0;

        if (e.getEventTime() < 0) {
            return;
        }      
        updatedDownTime = SystemClock.uptimeMillis();
        e.setDownTime(updatedDownTime);
        e.setEventTime(updatedDownTime);
    }

    /**
     * Gets the next event to be injected from the script. If the event queue is
     * empty, reads the next n events from the script into the queue, where n is
     * the lesser of the number of remaining events and the value specified by
     * MAX_ONE_TIME_READS. If the end of the file is reached, no events are
     * added to the queue and null is returned.
     *
     * @return The first event in the event queue or null if the end of the file
     *         is reached or if an error is encountered reading the file.
     */
    public MonkeyEvent getNextEvent() {
        long recordedEventTime = -1;
        MonkeyEvent ev;

        if (mQ.isEmpty()) {
            try {
                readNextBatch();
            } catch (IOException e) {
                return null;
            }
        }

        try {
            ev = mQ.getFirst();
            mQ.removeFirst();
        } catch (NoSuchElementException e) {
            return null;
        }

        if (ev.getEventType() == MonkeyEvent.EVENT_TYPE_KEY) {
            adjustKeyEventTime((MonkeyKeyEvent) ev);
        } else if (ev.getEventType() == MonkeyEvent.EVENT_TYPE_TOUCH
                || ev.getEventType() == MonkeyEvent.EVENT_TYPE_TRACKBALL) {
            adjustMotionEventTime((MonkeyMotionEvent) ev);
        }
        return ev;
    }
}
