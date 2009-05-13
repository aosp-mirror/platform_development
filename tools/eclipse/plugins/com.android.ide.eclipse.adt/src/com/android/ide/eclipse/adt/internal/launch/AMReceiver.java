/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.eclipse.adt.internal.launch;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.MultiLineReceiver;
import com.android.ide.eclipse.adt.AdtPlugin;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Output receiver for am process (Activity Manager)
 * 
 * Monitors adb output for am errors, and retries launch as appropriate. 
 */
public class AMReceiver extends MultiLineReceiver {

    private static final int MAX_ATTEMPT_COUNT = 5;
    private static final Pattern sAmErrorType = Pattern.compile("Error type (\\d+)"); //$NON-NLS-1$

    private final DelayedLaunchInfo mLaunchInfo;
    private final IDevice mDevice;
    private final ILaunchController mLaunchController;

    /**
     * Basic constructor.
     * 
     * @param launchInfo the {@link DelayedLaunchInfo} associated with the am process.
     * @param device the Android device on which the launch is done.
     * @param launchController the {@link ILaunchController} that is managing the launch
     */
    public AMReceiver(DelayedLaunchInfo launchInfo, IDevice device, 
            ILaunchController launchController) {
        mLaunchInfo = launchInfo;
        mDevice = device;
        mLaunchController = launchController;
    }

    /**
     * Monitors the am process for error messages. If an error occurs, will reattempt launch up to
     * <code>MAX_ATTEMPT_COUNT</code> times.
     * 
     * @param lines a portion of the am output
     * 
     * @see MultiLineReceiver#processNewLines(String[])
     */
    @Override
    public void processNewLines(String[] lines) {
        // first we check if one starts with error
        ArrayList<String> array = new ArrayList<String>();
        boolean error = false;
        boolean warning = false;
        for (String s : lines) {
            // ignore empty lines.
            if (s.length() == 0) {
                continue;
            }

            // check for errors that output an error type, if the attempt count is still
            // valid. If not the whole text will be output in the console
            if (mLaunchInfo.getAttemptCount() < MAX_ATTEMPT_COUNT &&
                    mLaunchInfo.isCancelled() == false) {
                Matcher m = sAmErrorType.matcher(s);
                if (m.matches()) {
                    // get the error type
                    int type = Integer.parseInt(m.group(1));

                    final int waitTime = 3;
                    String msg;

                    switch (type) {
                        case 1:
                            /* Intended fall through */
                        case 2:
                            msg = String.format(
                                    "Device not ready. Waiting %1$d seconds before next attempt.",
                                    waitTime);
                            break;
                        case 3:
                            msg = String.format(
                                    "New package not yet registered with the system. Waiting %1$d seconds before next attempt.",
                                    waitTime);
                            break;
                        default:
                            msg = String.format(
                                    "Device not ready (%2$d). Waiting %1$d seconds before next attempt.",
                                    waitTime, type);
                        break;

                    }

                    AdtPlugin.printToConsole(mLaunchInfo.getProject(), msg);

                    // launch another thread, that waits a bit and attempts another launch
                    new Thread("Delayed Launch attempt") {
                        @Override
                        public void run() {
                            try {
                                sleep(waitTime * 1000);
                            } catch (InterruptedException e) {
                                // ignore
                            }

                            mLaunchController.launchApp(mLaunchInfo, mDevice);
                        }
                    }.start();

                    // no need to parse the rest
                    return;
                }
            }

            // check for error if needed
            if (error == false && s.startsWith("Error:")) { //$NON-NLS-1$
                error = true;
            }
            if (warning == false && s.startsWith("Warning:")) { //$NON-NLS-1$
                warning = true;
            }

            // add the line to the list
            array.add("ActivityManager: " + s); //$NON-NLS-1$
        }

        // then we display them in the console
        if (warning || error) {
            AdtPlugin.printErrorToConsole(mLaunchInfo.getProject(), array.toArray());
        } else {
            AdtPlugin.printToConsole(mLaunchInfo.getProject(), array.toArray());
        }

        // if error then we cancel the launch, and remove the delayed info
        if (error) {
            mLaunchController.stopLaunch(mLaunchInfo);
        }
    }

    /**
     * Returns true if launch has been cancelled
     */
    public boolean isCancelled() {
        return mLaunchInfo.isCancelled();
    }
}
