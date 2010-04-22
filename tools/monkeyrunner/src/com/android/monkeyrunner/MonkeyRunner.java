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
package com.android.monkeyrunner;

import com.android.ddmlib.NullOutputReceiver;

import java.io.IOException;

/**
 * This is the main interface class into the jython bindings.
 */
public class MonkeyRunner {
    /* package */ static MonkeyManager MONKEY_MANANGER;

    /**
     * This is where we would put any standard init stuff, if we had any.
     */
    public static void start_script() throws IOException {
    }

    /**
     * This is a house cleaning routine to run after finishing a script.
     * Puts the monkey server in a known state and closes the recording.
     */
    public static void end_script() throws IOException {
        MONKEY_MANANGER.done();
    }

    /** This is a method for scripts to launch an activity on the device
     *
     * @param name The name of the activity to launch
     */
    public static void launch_activity(String name) throws IOException {
        System.out.println("Launching: " + name);
        // We're single threaded, so don't worry about the thead-safety.
        MONKEY_MANANGER.getDevice().executeShellCommand("am start -a android.intent.action.MAIN -n "
                + name, new NullOutputReceiver());
    }

    /**
     * Grabs the current state of the screen stores it as a png
     *
     * @param tag filename or tag descriptor of the screenshot
     */
    public static MonkeyImage grabscreen() throws IOException {
        return new MonkeyImage(MONKEY_MANANGER.getDevice().getScreenshot());
    }

    /**
     * Sleeper method for script to call
     *
     * @param msec msecs to sleep for
     */
    public static void sleep(int msec) throws IOException {
        try {
            Thread.sleep(msec);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tap function for scripts to call at a particular x and y location
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public static boolean tap(int x, int y) throws IOException {
        return MONKEY_MANANGER.tap(x, y);
    }

    /**
     * Press function for scripts to call on a particular button or key
     *
     * @param key key to press
     */
    public static boolean press(String key) throws IOException {
        return press(key, true);
    }

    /**
     * Press function for scripts to call on a particular button or key
     *
     * @param key key to press
     * @param print whether to send output to user
     */
    private static boolean press(String key, boolean print) throws IOException {
        return MONKEY_MANANGER.press(key);
    }

    /**
     * dpad down function
     */
    public static boolean down() throws IOException {
        return press("dpad_down");
    }

    /**
     * dpad up function
     */
    public static boolean up() throws IOException {
        return press("dpad_up");
    }

    /**
     * Function to type text on the device
     *
     * @param text text to type
     */
    public static boolean type(String text) throws IOException {
        return MONKEY_MANANGER.type(text);
    }
}
