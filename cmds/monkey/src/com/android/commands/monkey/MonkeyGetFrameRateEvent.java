/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.app.IActivityManager;
import android.util.Log;
import android.view.IWindowManager;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Events for running a special shell command to capture the frame rate.
 * To run this test, the system property viewancestor.profile_rendering
 * must be set to true to force the currently focused window to render at
 * 60 Hz.
 */
public class MonkeyGetFrameRateEvent extends MonkeyEvent {

    private String GET_FRAMERATE_CMD = "service call SurfaceFlinger 1013";
    private String mStatus;
    private static long mStartTime; // in millisecond
    private static long mEndTime; // in millisecond
    private static float mDuration; // in seconds
    private static String mTestCaseName = null;
    private static int mStartFrameNo;
    private static int mEndFrameNo;

    private static final String TAG = "MonkeyGetFrameRateEvent";
    private static final String LOG_FILE = "/sdcard/avgFrameRateOut.txt";

    private static final Pattern NO_OF_FRAMES_PATTERN =
        Pattern.compile(".*\\(([a-f[A-F][0-9]].*?)\\s.*\\)");

    public MonkeyGetFrameRateEvent(String status, String testCaseName) {
        super(EVENT_TYPE_ACTIVITY);
        mStatus = status;
        mTestCaseName = testCaseName;
    }

    public MonkeyGetFrameRateEvent(String status) {
        super(EVENT_TYPE_ACTIVITY);
        mStatus = status;
    }

    //Calculate the average frame rate
    private float getAverageFrameRate(int totalNumberOfFrame, float duration) {
        float avgFrameRate = 0;
        if (duration > 0) {
            avgFrameRate = (totalNumberOfFrame / duration);
        }
        return avgFrameRate;
    }

    /**
     * Calculate the frame rate and write the output to a file on the SD card.
     */
    private void writeAverageFrameRate() {
        FileWriter writer = null;
        float avgFrameRate;
        int totalNumberOfFrame = 0;
        try {
            writer = new FileWriter(LOG_FILE, true); // true = append
            totalNumberOfFrame = mEndFrameNo - mStartFrameNo;
            avgFrameRate = getAverageFrameRate(totalNumberOfFrame, mDuration);
            writer.write(String.format("%s:%.2f\n",mTestCaseName,avgFrameRate));
            writer.close();
        } catch (IOException e) {
            Log.w(TAG, "Can't write sdcard log file", e);
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e.toString());
            }
        }
    }

    // Parse the output of the surfaceFlinge shell command call
    private String getNumberOfFrames(String input){
        String noOfFrames = null;
        Matcher m = NO_OF_FRAMES_PATTERN.matcher(input);
        if (m.matches()){
            noOfFrames = m.group(1);
        }
        return noOfFrames;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        java.lang.Process p = null;
        BufferedReader result = null;
        try {
            p = Runtime.getRuntime().exec(GET_FRAMERATE_CMD);
            int status = p.waitFor();
            if (status != 0) {
                System.err.println(String.format("// Shell command %s status was %s",
                        GET_FRAMERATE_CMD, status));
            }
            result = new BufferedReader(new InputStreamReader(p.getInputStream()));

            //Only need the first line of the output
            String output = result.readLine();

            if (output != null) {
                if (mStatus == "start") {
                    mStartFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mStartTime = System.currentTimeMillis();
                } else if (mStatus == "end") {
                    mEndFrameNo = Integer.parseInt(getNumberOfFrames(output), 16);
                    mEndTime = System.currentTimeMillis();
                    long diff = mEndTime - mStartTime;
                    mDuration = (float)(diff/1000.0);
                    writeAverageFrameRate();
                }
            }
        } catch (Exception e) {
            System.err.println("// Exception from " + GET_FRAMERATE_CMD + ":");
            System.err.println(e.toString());
        } finally {
            try {
                if (result != null) {
                    result.close();
                }
                if (p != null) {
                    p.destroy();
                }
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
        return MonkeyEvent.INJECT_SUCCESS;
    }
}