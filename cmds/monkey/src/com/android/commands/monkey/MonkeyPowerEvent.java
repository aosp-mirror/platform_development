/*
 * Copyright (C) 2010 The Android Open Source Project
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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import android.app.IActivityManager;
import android.content.ContentValues;
import android.util.Log;
import android.view.IWindowManager;
import android.os.Build;


/**
 * Special events for power measurement.
 */
public class MonkeyPowerEvent extends MonkeyEvent {

    //Parameter for the power test runner
    private static final String TAG = "PowerTester";
    private static final String LOG_FILE = "/sdcard/autotester.log";
    private static ArrayList<ContentValues> mLogEvents = new ArrayList<ContentValues>();
    private static final String TEST_SEQ_BEGIN = "AUTOTEST_SEQUENCE_BEGIN";
    private static final String TEST_STARTED = "AUTOTEST_TEST_BEGIN";
    private static final String TEST_DELAY_STARTED = "AUTOTEST_TEST_BEGIN_DELAY";
    private static final String TEST_ENDED = "AUTOTEST_TEST_SUCCESS";
    private static final String TEST_IDLE_ENDED = "AUTOTEST_IDLE_SUCCESS";
    private static long mTestStartTime;

    private String mPowerLogTag;
    private String mTestResult;

    //10 secs for the screen to trun off after the usb notification
    private static final long USB_DELAY_TIME = 10000;

    public MonkeyPowerEvent(String powerLogTag, String powerTestResult) {
        super(EVENT_TYPE_ACTIVITY);
        mPowerLogTag = powerLogTag;
        mTestResult = powerTestResult;
    }

    public MonkeyPowerEvent(String powerLogTag) {
        super(EVENT_TYPE_ACTIVITY);
        mPowerLogTag = powerLogTag;
        mTestResult = null;
    }

    public MonkeyPowerEvent() {
        super(EVENT_TYPE_ACTIVITY);
        mPowerLogTag = null;
        mTestResult = null;
    }

    /**
     * Buffer an event to be logged later.
     */
    private void bufferLogEvent(String tag, String value) {
        long tagTime = System.currentTimeMillis();
        // Record the test start time
        if (tag.compareTo(TEST_STARTED) == 0) {
            mTestStartTime = tagTime;
        } else if (tag.compareTo(TEST_IDLE_ENDED) == 0) {
            long lagTime = Long.parseLong(value);
            tagTime = mTestStartTime + lagTime;
            tag = TEST_ENDED;
        } else if (tag.compareTo(TEST_DELAY_STARTED) == 0) {
            mTestStartTime = tagTime + USB_DELAY_TIME;
            tagTime = mTestStartTime;
            tag = TEST_STARTED;
        }

        ContentValues event = new ContentValues();
        event.put("date", tagTime);
        event.put("tag", tag);

        if (value != null) {
            event.put("value", value);
        }
        mLogEvents.add(event);
    }

    /**
     * Write the accumulated log events to a file on the SD card.
     */
    private void writeLogEvents() {
        ContentValues[] events;

        events = mLogEvents.toArray(new ContentValues[0]);
        mLogEvents.clear();
        FileWriter writer = null;
        try {
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < events.length; ++i) {
                ContentValues event = events[i];
                buffer.append(MonkeyUtils.toCalendarTime(event.getAsLong("date")));
                buffer.append(event.getAsString("tag"));
                if (event.containsKey("value")) {
                    String value = event.getAsString("value");
                    buffer.append(" ");
                    buffer.append(value.replace('\n', '/'));
                }
                buffer.append("\n");
            }
            writer = new FileWriter(LOG_FILE, true); // true = append
            writer.write(buffer.toString());
        } catch (IOException e) {
            Log.w(TAG, "Can't write sdcard log file", e);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (mPowerLogTag != null) {
            if (mPowerLogTag.compareTo(TEST_SEQ_BEGIN) == 0) {
                bufferLogEvent(mPowerLogTag, Build.FINGERPRINT);
            } else if (mTestResult != null) {
                bufferLogEvent(mPowerLogTag, mTestResult);
            }
        } else {
            writeLogEvents();
        }
        return MonkeyEvent.INJECT_SUCCESS;
    }
}
