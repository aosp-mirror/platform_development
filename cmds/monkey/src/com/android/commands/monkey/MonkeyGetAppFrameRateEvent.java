/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.os.Environment;
import android.util.Log;
import android.view.IWindowManager;

import java.lang.Process;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Events for running a special shell command to capture the frame rate for a given app. To run
 * this test, the system property viewancestor.profile_rendering must be set to
 * true to force the currently focused window to render at 60 Hz.
 */
public class MonkeyGetAppFrameRateEvent extends MonkeyEvent {

    private String GET_APP_FRAMERATE_TMPL = "dumpsys gfxinfo %s";
    private String mStatus;
    private static long sStartTime; // in millisecond
    private static long sEndTime; // in millisecond
    private static float sDuration; // in seconds
    private static String sActivityName = null;
    private static String sTestCaseName = null;
    private static int sStartFrameNo;
    private static int sEndFrameNo;

    private static final String TAG = "MonkeyGetAppFrameRateEvent";
    private static final String LOG_FILE = new File(Environment.getExternalStorageDirectory(),
            "avgAppFrameRateOut.txt").getAbsolutePath();
    private static final Pattern NO_OF_FRAMES_PATTERN =
            Pattern.compile(".* ([0-9]*) frames rendered");

    public MonkeyGetAppFrameRateEvent(String status, String activityName, String testCaseName) {
        super(EVENT_TYPE_ACTIVITY);
        mStatus = status;
        sActivityName = activityName;
        sTestCaseName = testCaseName;
    }

    public MonkeyGetAppFrameRateEvent(String status, String activityName) {
        super(EVENT_TYPE_ACTIVITY);
        mStatus = status;
        sActivityName = activityName;
    }

    public MonkeyGetAppFrameRateEvent(String status) {
        super(EVENT_TYPE_ACTIVITY);
        mStatus = status;
    }

    // Calculate the average frame rate
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
            Log.w(TAG, "file: " +LOG_FILE);
            writer = new FileWriter(LOG_FILE, true); // true = append
            totalNumberOfFrame = sEndFrameNo - sStartFrameNo;
            avgFrameRate = getAverageFrameRate(totalNumberOfFrame, sDuration);
            writer.write(String.format("%s:%.2f\n", sTestCaseName, avgFrameRate));
        } catch (IOException e) {
            Log.w(TAG, "Can't write sdcard log file", e);
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException e) {
                Log.e(TAG, "IOException " + e.toString());
            }
        }
    }

    // Parse the output of the dumpsys shell command call
    private String getNumberOfFrames(BufferedReader reader) throws IOException {
        String noOfFrames = null;
        String line = null;
        while((line = reader.readLine()) != null) {
            Matcher m = NO_OF_FRAMES_PATTERN.matcher(line);
            if (m.matches()) {
                noOfFrames = m.group(1);
                break;
            }
        }
        return noOfFrames;
    }

    @Override
    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        Process p = null;
        BufferedReader result = null;
        String cmd = String.format(GET_APP_FRAMERATE_TMPL, sActivityName);
        try {
            p = Runtime.getRuntime().exec(cmd);
            int status = p.waitFor();
            if (status != 0) {
                System.err.println(String.format("// Shell command %s status was %s",
                        cmd, status));
            }
            result = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String output = getNumberOfFrames(result);

            if (output != null) {
                if ("start".equals(mStatus)) {
                    sStartFrameNo = Integer.parseInt(output);
                    sStartTime = System.currentTimeMillis();
                } else if ("end".equals(mStatus)) {
                    sEndFrameNo = Integer.parseInt(output);
                    sEndTime = System.currentTimeMillis();
                    long diff = sEndTime - sStartTime;
                    sDuration = (float) (diff / 1000.0);
                    writeAverageFrameRate();
                }
            }
        } catch (Exception e) {
            System.err.println("// Exception from " + cmd + ":");
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
