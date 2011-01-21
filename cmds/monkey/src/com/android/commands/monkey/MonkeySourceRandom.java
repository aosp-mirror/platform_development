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
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManagerImpl;

import java.util.ArrayList;
import java.util.Random;

/**
 * monkey event queue
 */
public class MonkeySourceRandom implements MonkeyEventSource {
    /** Key events that move around the UI. */
    private static final int[] NAV_KEYS = {
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
    };
    /**
     * Key events that perform major navigation options (so shouldn't be sent
     * as much).
     */
    private static final int[] MAJOR_NAV_KEYS = {
        KeyEvent.KEYCODE_MENU, /*KeyEvent.KEYCODE_SOFT_RIGHT,*/
        KeyEvent.KEYCODE_DPAD_CENTER,
    };
    /** Key events that perform system operations. */
    private static final int[] SYS_KEYS = {
        KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK,
        KeyEvent.KEYCODE_CALL, KeyEvent.KEYCODE_ENDCALL,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_MUTE,
    };
    /** If a physical key exists? */
    private static final boolean[] PHYSICAL_KEY_EXISTS = new boolean[KeyEvent.getMaxKeyCode() + 1];
    static {
        for (int i = 0; i < PHYSICAL_KEY_EXISTS.length; ++i) {
            PHYSICAL_KEY_EXISTS[i] = true;
        }
        // Only examine SYS_KEYS
        for (int i = 0; i < SYS_KEYS.length; ++i) {
            PHYSICAL_KEY_EXISTS[SYS_KEYS[i]] = KeyCharacterMap.deviceHasKey(SYS_KEYS[i]);
        }
    }

    /** Nice names for all key events. */
    private static final String[] KEY_NAMES = {
        "KEYCODE_UNKNOWN",
        "KEYCODE_SOFT_LEFT",
        "KEYCODE_SOFT_RIGHT",
        "KEYCODE_HOME",
        "KEYCODE_BACK",
        "KEYCODE_CALL",
        "KEYCODE_ENDCALL",
        "KEYCODE_0",
        "KEYCODE_1",
        "KEYCODE_2",
        "KEYCODE_3",
        "KEYCODE_4",
        "KEYCODE_5",
        "KEYCODE_6",
        "KEYCODE_7",
        "KEYCODE_8",
        "KEYCODE_9",
        "KEYCODE_STAR",
        "KEYCODE_POUND",
        "KEYCODE_DPAD_UP",
        "KEYCODE_DPAD_DOWN",
        "KEYCODE_DPAD_LEFT",
        "KEYCODE_DPAD_RIGHT",
        "KEYCODE_DPAD_CENTER",
        "KEYCODE_VOLUME_UP",
        "KEYCODE_VOLUME_DOWN",
        "KEYCODE_POWER",
        "KEYCODE_CAMERA",
        "KEYCODE_CLEAR",
        "KEYCODE_A",
        "KEYCODE_B",
        "KEYCODE_C",
        "KEYCODE_D",
        "KEYCODE_E",
        "KEYCODE_F",
        "KEYCODE_G",
        "KEYCODE_H",
        "KEYCODE_I",
        "KEYCODE_J",
        "KEYCODE_K",
        "KEYCODE_L",
        "KEYCODE_M",
        "KEYCODE_N",
        "KEYCODE_O",
        "KEYCODE_P",
        "KEYCODE_Q",
        "KEYCODE_R",
        "KEYCODE_S",
        "KEYCODE_T",
        "KEYCODE_U",
        "KEYCODE_V",
        "KEYCODE_W",
        "KEYCODE_X",
        "KEYCODE_Y",
        "KEYCODE_Z",
        "KEYCODE_COMMA",
        "KEYCODE_PERIOD",
        "KEYCODE_ALT_LEFT",
        "KEYCODE_ALT_RIGHT",
        "KEYCODE_SHIFT_LEFT",
        "KEYCODE_SHIFT_RIGHT",
        "KEYCODE_TAB",
        "KEYCODE_SPACE",
        "KEYCODE_SYM",
        "KEYCODE_EXPLORER",
        "KEYCODE_ENVELOPE",
        "KEYCODE_ENTER",
        "KEYCODE_DEL",
        "KEYCODE_GRAVE",
        "KEYCODE_MINUS",
        "KEYCODE_EQUALS",
        "KEYCODE_LEFT_BRACKET",
        "KEYCODE_RIGHT_BRACKET",
        "KEYCODE_BACKSLASH",
        "KEYCODE_SEMICOLON",
        "KEYCODE_APOSTROPHE",
        "KEYCODE_SLASH",
        "KEYCODE_AT",
        "KEYCODE_NUM",
        "KEYCODE_HEADSETHOOK",
        "KEYCODE_FOCUS",
        "KEYCODE_PLUS",
        "KEYCODE_MENU",
        "KEYCODE_NOTIFICATION",
        "KEYCODE_SEARCH",
        "KEYCODE_PLAYPAUSE",
        "KEYCODE_STOP",
        "KEYCODE_NEXTSONG",
        "KEYCODE_PREVIOUSSONG",
        "KEYCODE_REWIND",
        "KEYCODE_FORWARD",
        "KEYCODE_MUTE",
        "KEYCODE_PAGE_UP",
        "KEYCODE_PAGE_DOWN",
        "KEYCODE_PICTSYMBOLS",
        "KEYCODE_SWITCH_CHARSET",
        "KEYCODE_BUTTON_A",
        "KEYCODE_BUTTON_B",
        "KEYCODE_BUTTON_C",
        "KEYCODE_BUTTON_X",
        "KEYCODE_BUTTON_Y",
        "KEYCODE_BUTTON_Z",
        "KEYCODE_BUTTON_L1",
        "KEYCODE_BUTTON_R1",
        "KEYCODE_BUTTON_L2",
        "KEYCODE_BUTTON_R2",
        "KEYCODE_BUTTON_THUMBL",
        "KEYCODE_BUTTON_THUMBR",
        "KEYCODE_BUTTON_START",
        "KEYCODE_BUTTON_SELECT",
        "KEYCODE_BUTTON_MODE",

        "TAG_LAST_KEYCODE"      // EOL.  used to keep the lists in sync
    };

    public static final int FACTOR_TOUCH        = 0;
    public static final int FACTOR_MOTION       = 1;
    public static final int FACTOR_TRACKBALL    = 2;
    public static final int FACTOR_NAV          = 3;
    public static final int FACTOR_MAJORNAV     = 4;
    public static final int FACTOR_SYSOPS       = 5;
    public static final int FACTOR_APPSWITCH    = 6;
    public static final int FACTOR_FLIP         = 7;
    public static final int FACTOR_ANYTHING     = 8;
    public static final int FACTORZ_COUNT       = 9;    // should be last+1


    /** percentages for each type of event.  These will be remapped to working
     * values after we read any optional values.
     **/
    private float[] mFactors = new float[FACTORZ_COUNT];
    private ArrayList<ComponentName> mMainApps;
    private int mEventCount = 0;  //total number of events generated so far
    private MonkeyEventQueue mQ;
    private Random mRandom;
    private int mVerbose = 0;
    private long mThrottle = 0;

    private boolean mKeyboardOpen = false;

    /**
     * @return the last name in the key list
     */
    public static String getLastKeyName() {
        return KEY_NAMES[KeyEvent.getMaxKeyCode() + 1];
    }

    public static String getKeyName(int keycode) {
        return KEY_NAMES[keycode];
    }

    /**
     * Looks up the keyCode from a given KEYCODE_NAME.  NOTE: This may
     * be an expensive operation.
     *
     * @param keyName the name of the KEYCODE_VALUE to lookup.
     * @returns the intenger keyCode value, or -1 if not found
     */
    public static int getKeyCode(String keyName) {
        for (int x = 0; x < KEY_NAMES.length; x++) {
            if (KEY_NAMES[x].equals(keyName)) {
                return x;
            }
        }
        return -1;
    }

    public MonkeySourceRandom(Random random, ArrayList<ComponentName> MainApps,
            long throttle, boolean randomizeThrottle) {
        // default values for random distributions
        // note, these are straight percentages, to match user input (cmd line args)
        // but they will be converted to 0..1 values before the main loop runs.
        mFactors[FACTOR_TOUCH] = 15.0f;
        mFactors[FACTOR_MOTION] = 10.0f;
        mFactors[FACTOR_TRACKBALL] = 15.0f;
        mFactors[FACTOR_NAV] = 25.0f;
        mFactors[FACTOR_MAJORNAV] = 15.0f;
        mFactors[FACTOR_SYSOPS] = 2.0f;
        mFactors[FACTOR_APPSWITCH] = 2.0f;
        mFactors[FACTOR_FLIP] = 1.0f;
        mFactors[FACTOR_ANYTHING] = 15.0f;

        mRandom = random;
        mMainApps = MainApps;
        mQ = new MonkeyEventQueue(random, throttle, randomizeThrottle);
    }

    /**
     * Adjust the percentages (after applying user values) and then normalize to a 0..1 scale.
     */
    private boolean adjustEventFactors() {
        // go through all values and compute totals for user & default values
        float userSum = 0.0f;
        float defaultSum = 0.0f;
        int defaultCount = 0;
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            if (mFactors[i] <= 0.0f) {   // user values are zero or negative
                userSum -= mFactors[i];
            } else {
                defaultSum += mFactors[i];
                ++defaultCount;
            }
        }

        // if the user request was > 100%, reject it
        if (userSum > 100.0f) {
            System.err.println("** Event weights > 100%");
            return false;
        }

        // if the user specified all of the weights, then they need to be 100%
        if (defaultCount == 0 && (userSum < 99.9f || userSum > 100.1f)) {
            System.err.println("** Event weights != 100%");
            return false;
        }

        // compute the adjustment necessary
        float defaultsTarget = (100.0f - userSum);
        float defaultsAdjustment = defaultsTarget / defaultSum;

        // fix all values, by adjusting defaults, or flipping user values back to >0
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            if (mFactors[i] <= 0.0f) {   // user values are zero or negative
                mFactors[i] = -mFactors[i];
            } else {
                mFactors[i] *= defaultsAdjustment;
            }
        }

        // if verbose, show factors
        if (mVerbose > 0) {
            System.out.println("// Event percentages:");
            for (int i = 0; i < FACTORZ_COUNT; ++i) {
                System.out.println("//   " + i + ": " + mFactors[i] + "%");
            }
        }

        if (!validateKeys()) {
            return false;
        }

        // finally, normalize and convert to running sum
        float sum = 0.0f;
        for (int i = 0; i < FACTORZ_COUNT; ++i) {
            sum += mFactors[i] / 100.0f;
            mFactors[i] = sum;
        }
        return true;
    }

    private static boolean validateKeyCategory(String catName, int[] keys, float factor) {
        if (factor < 0.1f) {
            return true;
        }
        for (int i = 0; i < keys.length; ++i) {
            if (PHYSICAL_KEY_EXISTS[keys[i]]) {
                return true;
            }
        }
        System.err.println("** " + catName + " has no physical keys but with factor " + factor + "%.");
        return false;
    }

    /**
     * See if any key exists for non-zero factors.
     */
    private boolean validateKeys() {
        return validateKeyCategory("NAV_KEYS", NAV_KEYS, mFactors[FACTOR_NAV])
            && validateKeyCategory("MAJOR_NAV_KEYS", MAJOR_NAV_KEYS, mFactors[FACTOR_MAJORNAV])
            && validateKeyCategory("SYS_KEYS", SYS_KEYS, mFactors[FACTOR_SYSOPS]);
    }

    /**
     * set the factors
     *
     * @param factors percentages for each type of event
     */
    public void setFactors(float factors[]) {
        int c = FACTORZ_COUNT;
        if (factors.length < c) {
            c = factors.length;
        }
        for (int i = 0; i < c; i++)
            mFactors[i] = factors[i];
    }

    public void setFactors(int index, float v) {
        mFactors[index] = v;
    }

    /**
     * Generates a random motion event. This method counts a down, move, and up as multiple events.
     *
     * TODO:  Test & fix the selectors when non-zero percentages
     * TODO:  Longpress.
     * TODO:  Fling.
     * TODO:  Meta state
     * TODO:  More useful than the random walk here would be to pick a single random direction
     * and distance, and divvy it up into a random number of segments.  (This would serve to
     * generate fling gestures, which are important).
     *
     * @param random Random number source for positioning
     * @param motionEvent If false, touch/release.  If true, touch/move/release.
     *
     */
    private void generateMotionEvent(Random random, boolean motionEvent){

        Display display = WindowManagerImpl.getDefault().getDefaultDisplay();

        float x = Math.abs(random.nextInt() % display.getWidth());
        float y = Math.abs(random.nextInt() % display.getHeight());
        long downAt = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        if (downAt == -1) {
            downAt = eventTime;
        }

        MonkeyMotionEvent e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                downAt, MotionEvent.ACTION_DOWN, x, y, 0);
        e.setIntermediateNote(false);
        mQ.addLast(e);

        // sometimes we'll move during the touch
        if (motionEvent) {
            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                // generate some slop in the up event
                x = (x + (random.nextInt() % 10)) % display.getWidth();
                y = (y + (random.nextInt() % 10)) % display.getHeight();

                e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                        downAt, MotionEvent.ACTION_MOVE, x, y, 0);
                e.setIntermediateNote(true);
                mQ.addLast(e);
            }
        }

        // TODO generate some slop in the up event
        e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_POINTER,
                downAt, MotionEvent.ACTION_UP, x, y, 0);
        e.setIntermediateNote(false);
        mQ.addLast(e);
    }

    /**
     * Generates a random trackball event. This consists of a sequence of small moves, followed by
     * an optional single click.
     *
     * TODO:  Longpress.
     * TODO:  Meta state
     * TODO:  Parameterize the % clicked
     * TODO:  More useful than the random walk here would be to pick a single random direction
     * and distance, and divvy it up into a random number of segments.  (This would serve to
     * generate fling gestures, which are important).
     *
     * @param random Random number source for positioning
     *
     */
    private void generateTrackballEvent(Random random) {
        Display display = WindowManagerImpl.getDefault().getDefaultDisplay();

        boolean drop = false;
        int count = random.nextInt(10);
        MonkeyMotionEvent e;
        for (int i = 0; i < 10; ++i) {
            // generate a small random step
            int dX = random.nextInt(10) - 5;
            int dY = random.nextInt(10) - 5;


            e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_TRACKBALL, -1,
                    MotionEvent.ACTION_MOVE, dX, dY, 0);
            e.setIntermediateNote(i > 0);
            mQ.addLast(e);
        }

        // 10% of trackball moves end with a click
        if (0 == random.nextInt(10)) {
            long downAt = SystemClock.uptimeMillis();


            e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_TRACKBALL, downAt,
                    MotionEvent.ACTION_DOWN, 0, 0, 0);
            e.setIntermediateNote(true);
            mQ.addLast(e);


            e = new MonkeyMotionEvent(MonkeyEvent.EVENT_TYPE_TRACKBALL, downAt,
                    MotionEvent.ACTION_UP, 0, 0, 0);
            e.setIntermediateNote(false);
            mQ.addLast(e);
        }
    }

    /**
     * generate a random event based on mFactor
     */
    private void generateEvents() {
        float cls = mRandom.nextFloat();
        int lastKey = 0;

        boolean touchEvent = cls < mFactors[FACTOR_TOUCH];
        boolean motionEvent = !touchEvent && (cls < mFactors[FACTOR_MOTION]);
        if (touchEvent || motionEvent) {
            generateMotionEvent(mRandom, motionEvent);
            return;
        }

        if (cls < mFactors[FACTOR_TRACKBALL]) {
            generateTrackballEvent(mRandom);
            return;
        }

        // The remaining event categories are injected as key events
        for (;;) {
            if (cls < mFactors[FACTOR_NAV]) {
                lastKey = NAV_KEYS[mRandom.nextInt(NAV_KEYS.length)];
            } else if (cls < mFactors[FACTOR_MAJORNAV]) {
                lastKey = MAJOR_NAV_KEYS[mRandom.nextInt(MAJOR_NAV_KEYS.length)];
            } else if (cls < mFactors[FACTOR_SYSOPS]) {
                lastKey = SYS_KEYS[mRandom.nextInt(SYS_KEYS.length)];
            } else if (cls < mFactors[FACTOR_APPSWITCH]) {
                MonkeyActivityEvent e = new MonkeyActivityEvent(mMainApps.get(
                        mRandom.nextInt(mMainApps.size())));
                mQ.addLast(e);
                return;
            } else if (cls < mFactors[FACTOR_FLIP]) {
                MonkeyFlipEvent e = new MonkeyFlipEvent(mKeyboardOpen);
                mKeyboardOpen = !mKeyboardOpen;
                mQ.addLast(e);
                return;
            } else {
                lastKey = 1 + mRandom.nextInt(KeyEvent.getMaxKeyCode() - 1);
            }

            if (lastKey != KeyEvent.KEYCODE_POWER
                    && lastKey != KeyEvent.KEYCODE_ENDCALL
                    && PHYSICAL_KEY_EXISTS[lastKey]) {
                break;
            }
        }

        MonkeyKeyEvent e = new MonkeyKeyEvent(KeyEvent.ACTION_DOWN, lastKey);
        mQ.addLast(e);

        e = new MonkeyKeyEvent(KeyEvent.ACTION_UP, lastKey);
        mQ.addLast(e);
    }

    public boolean validate() {
        //check factors
        return adjustEventFactors();
    }

    public void setVerbose(int verbose) {
        mVerbose = verbose;
    }

    /**
     * generate an activity event
     */
    public void generateActivity() {
        MonkeyActivityEvent e = new MonkeyActivityEvent(mMainApps.get(
                mRandom.nextInt(mMainApps.size())));
        mQ.addLast(e);
    }

    /**
     * if the queue is empty, we generate events first
     * @return the first event in the queue
     */
    public MonkeyEvent getNextEvent() {
        if (mQ.isEmpty()) {
            generateEvents();
        }
        mEventCount++;
        MonkeyEvent e = mQ.getFirst();
        mQ.removeFirst();
        return e;
    }
}
