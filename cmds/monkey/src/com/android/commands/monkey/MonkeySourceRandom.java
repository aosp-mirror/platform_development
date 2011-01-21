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
import android.graphics.PointF;
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
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
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

    public static final int FACTOR_TOUCH        = 0;
    public static final int FACTOR_MOTION       = 1;
    public static final int FACTOR_PINCHZOOM    = 2;
    public static final int FACTOR_TRACKBALL    = 3;
    public static final int FACTOR_NAV          = 4;
    public static final int FACTOR_MAJORNAV     = 5;
    public static final int FACTOR_SYSOPS       = 6;
    public static final int FACTOR_APPSWITCH    = 7;
    public static final int FACTOR_FLIP         = 8;
    public static final int FACTOR_ANYTHING     = 9;
    public static final int FACTORZ_COUNT       = 10;    // should be last+1

    private static final int GESTURE_TAP = 0;
    private static final int GESTURE_DRAG = 1;
    private static final int GESTURE_PINCH_OR_ZOOM = 2;

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

    public static String getKeyName(int keycode) {
        return KeyEvent.keyCodeToString(keycode);
    }

    /**
     * Looks up the keyCode from a given KEYCODE_NAME.  NOTE: This may
     * be an expensive operation.
     *
     * @param keyName the name of the KEYCODE_VALUE to lookup.
     * @returns the intenger keyCode value, or -1 if not found
     */
    public static int getKeyCode(String keyName) {
        return KeyEvent.keyCodeFromString(keyName);
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
        mFactors[FACTOR_ANYTHING] = 13.0f;
        mFactors[FACTOR_PINCHZOOM] = 2.0f;

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
     * @param gesture The gesture to perform.
     *
     */
    private void generatePointerEvent(Random random, int gesture){
        Display display = WindowManagerImpl.getDefault().getDefaultDisplay();

        PointF p1 = randomPoint(random, display);
        PointF v1 = randomVector(random);

        long downAt = SystemClock.uptimeMillis();

        mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_DOWN)
                .setDownTime(downAt)
                .addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));

        // sometimes we'll move during the touch
        if (gesture == GESTURE_DRAG) {
            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                randomWalk(random, display, p1, v1);

                mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE)
                        .setDownTime(downAt)
                        .addPointer(0, p1.x, p1.y)
                        .setIntermediateNote(true));
            }
        } else if (gesture == GESTURE_PINCH_OR_ZOOM) {
            PointF p2 = randomPoint(random, display);
            PointF v2 = randomVector(random);

            randomWalk(random, display, p1, v1);
            mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_DOWN
                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                    .setDownTime(downAt)
                    .addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y)
                    .setIntermediateNote(true));

            int count = random.nextInt(10);
            for (int i = 0; i < count; i++) {
                randomWalk(random, display, p1, v1);
                randomWalk(random, display, p2, v2);

                mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_MOVE)
                        .setDownTime(downAt)
                        .addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y)
                        .setIntermediateNote(true));
            }

            randomWalk(random, display, p1, v1);
            randomWalk(random, display, p2, v2);
            mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_POINTER_UP
                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT))
                    .setDownTime(downAt)
                    .addPointer(0, p1.x, p1.y).addPointer(1, p2.x, p2.y)
                    .setIntermediateNote(true));
        }

        randomWalk(random, display, p1, v1);
        mQ.addLast(new MonkeyTouchEvent(MotionEvent.ACTION_UP)
                .setDownTime(downAt)
                .addPointer(0, p1.x, p1.y)
                .setIntermediateNote(false));
    }

    private PointF randomPoint(Random random, Display display) {
        return new PointF(random.nextInt(display.getWidth()), random.nextInt(display.getHeight()));
    }

    private PointF randomVector(Random random) {
        return new PointF((random.nextFloat() - 0.5f) * 50, (random.nextFloat() - 0.5f) * 50);
    }

    private void randomWalk(Random random, Display display, PointF point, PointF vector) {
        point.x = (float) Math.max(Math.min(point.x + random.nextFloat() * vector.x,
                display.getWidth()), 0);
        point.y = (float) Math.max(Math.min(point.y + random.nextFloat() * vector.y,
                display.getHeight()), 0);
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
        for (int i = 0; i < 10; ++i) {
            // generate a small random step
            int dX = random.nextInt(10) - 5;
            int dY = random.nextInt(10) - 5;

            mQ.addLast(new MonkeyTrackballEvent(MotionEvent.ACTION_MOVE)
                    .addPointer(0, dX, dY)
                    .setIntermediateNote(i > 0));
        }

        // 10% of trackball moves end with a click
        if (0 == random.nextInt(10)) {
            long downAt = SystemClock.uptimeMillis();

            mQ.addLast(new MonkeyTrackballEvent(MotionEvent.ACTION_DOWN)
                    .setDownTime(downAt)
                    .addPointer(0, 0, 0)
                    .setIntermediateNote(true));

            mQ.addLast(new MonkeyTrackballEvent(MotionEvent.ACTION_UP)
                    .setDownTime(downAt)
                    .addPointer(0, 0, 0)
                    .setIntermediateNote(false));
        }
    }

    /**
     * generate a random event based on mFactor
     */
    private void generateEvents() {
        float cls = mRandom.nextFloat();
        int lastKey = 0;

        if (cls < mFactors[FACTOR_TOUCH]) {
            generatePointerEvent(mRandom, GESTURE_TAP);
            return;
        } else if (cls < mFactors[FACTOR_MOTION]) {
            generatePointerEvent(mRandom, GESTURE_DRAG);
            return;
        } else if (cls < mFactors[FACTOR_PINCHZOOM]) {
            generatePointerEvent(mRandom, GESTURE_PINCH_OR_ZOOM);
            return;
        } else if (cls < mFactors[FACTOR_TRACKBALL]) {
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
