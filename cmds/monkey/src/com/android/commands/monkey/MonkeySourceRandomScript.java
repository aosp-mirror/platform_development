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

import java.util.ArrayList;
import java.util.Random;

/**
 * Class for generating MonkeyEvents from multiple scripts.
 */
public class MonkeySourceRandomScript implements MonkeyEventSource {
    /** The verbose level of the source (currently not used) */
    private int mVerbose = 0;

    /** The source for the setup script if it exists */
    private MonkeySourceScript mSetupSource = null;

    /** The list of MonkeySourceScript instances to be played in random order */
    private ArrayList<MonkeySourceScript> mScriptSources = new ArrayList<MonkeySourceScript>();

    /** The current source, set to the setup source and then a random script */
    private MonkeySourceScript mCurrentSource = null;

    /** The random number generator */
    private Random mRandom;

    private boolean mRandomizeScript = false;

    private int mScriptCount = 0;

    /**
     * Creates a MonkeySourceRandomScript instance with an additional setup script.
     *
     * @param setupFileName The name of the setup script file on the device.
     * @param scriptFileNames An ArrayList of the names of the script files to be run randomly.
     * @param throttle The amount of time to sleep in ms between events.
     * @param randomizeThrottle Whether to randomize throttle.
     * @param random The random number generator.
     */
    public MonkeySourceRandomScript(String setupFileName, ArrayList<String> scriptFileNames,
            long throttle, boolean randomizeThrottle, Random random, long profileWaitTime,
            long deviceSleepTime, boolean randomizeScript) {
        if (setupFileName != null) {
            mSetupSource = new MonkeySourceScript(random, setupFileName, throttle,
                    randomizeThrottle, profileWaitTime, deviceSleepTime);
            mCurrentSource = mSetupSource;
        }

        for (String fileName: scriptFileNames) {
            mScriptSources.add(new MonkeySourceScript(random, fileName, throttle,
                    randomizeThrottle, profileWaitTime, deviceSleepTime));
        }

        mRandom = random;
        mRandomizeScript = randomizeScript;
    }

    /**
     * Creates a MonkeySourceRandomScript instance without an additional setup script.
     *
     * @param scriptFileNames An ArrayList of the names of the script files to be run randomly.
     * @param throttle The amount of time to sleep in ms between events.
     * @param randomizeThrottle Whether to randomize throttle.
     * @param random The random number generator.
     */
    public MonkeySourceRandomScript(ArrayList<String> scriptFileNames, long throttle,
            boolean randomizeThrottle, Random random, long profileWaitTime, long deviceSleepTime,
            boolean randomizeScript) {
        this(null, scriptFileNames, throttle, randomizeThrottle, random, profileWaitTime,
                deviceSleepTime, randomizeScript);
    }

    /**
     * Gets the next event from the current event source.  If the event source is null, a new
     * script event source is chosen randomly from the list of script sources and the next event is
     * chosen from that.
     *
     * @return The first event in the event queue or null if the end of the file
     *         is reached or if an error is encountered reading the file.
     */
    public MonkeyEvent getNextEvent() {
        if (mCurrentSource == null) {
            int numSources = mScriptSources.size();
            if (numSources == 1) {
                mCurrentSource = mScriptSources.get(0);
            } else if (numSources > 1 ) {
                if (mRandomizeScript) {
                    mCurrentSource = mScriptSources.get(mRandom.nextInt(numSources));
                } else {
                    mCurrentSource = mScriptSources.get(mScriptCount % numSources);
                    mScriptCount++;
                }
            }
        }

        if (mCurrentSource != null) {
            MonkeyEvent nextEvent = mCurrentSource.getNextEvent();
            if (nextEvent == null) {
                mCurrentSource = null;
            }
            return nextEvent;
        }
        return null;
    }

    /**
     * Sets the verbosity for the source as well as all sub event sources.
     *
     * @param verbose The verbose level.
     */
    public void setVerbose(int verbose) {
        mVerbose = verbose;

        if (mSetupSource != null) {
            mSetupSource.setVerbose(verbose);
        }

        for (MonkeySourceScript source: mScriptSources) {
            source.setVerbose(verbose);
        }
    }

    /**
     * Validates that all the underlying event sources are valid
     *
     * @return True if all the script files are valid.
     *
     * @see MonkeySourceScript#validate()
     */
    public boolean validate() {
        if (mSetupSource != null && !mSetupSource.validate()) {
            return false;
        }

        for (MonkeySourceScript source: mScriptSources) {
            if (!source.validate()) {
                return false;
            }
        }

        return true;
    }
}
