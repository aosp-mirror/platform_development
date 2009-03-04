/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmuilib.console;


/**
 * Static Console used to ouput messages. By default outputs the message to System.out and
 * System.err, but can receive a IDdmConsole object which will actually do something.
 */
public class DdmConsole {

    private static IDdmConsole mConsole;

    /**
     * Prints a message to the android console.
     * @param message the message to print
     * @param forceDisplay if true, this force the console to be displayed.
     */
    public static void printErrorToConsole(String message) {
        if (mConsole != null) {
            mConsole.printErrorToConsole(message);
        } else {
            System.err.println(message);
        }
    }

    /**
     * Prints several messages to the android console.
     * @param messages the messages to print
     * @param forceDisplay if true, this force the console to be displayed.
     */
    public static void printErrorToConsole(String[] messages) {
        if (mConsole != null) {
            mConsole.printErrorToConsole(messages);
        } else {
            for (String message : messages) {
                System.err.println(message);
            }
        }
    }

    /**
     * Prints a message to the android console.
     * @param message the message to print
     * @param forceDisplay if true, this force the console to be displayed.
     */
    public static void printToConsole(String message) {
        if (mConsole != null) {
            mConsole.printToConsole(message);
        } else {
            System.out.println(message);
        }
    }

    /**
     * Prints several messages to the android console.
     * @param messages the messages to print
     * @param forceDisplay if true, this force the console to be displayed.
     */
    public static void printToConsole(String[] messages) {
        if (mConsole != null) {
            mConsole.printToConsole(messages);
        } else {
            for (String message : messages) {
                System.out.println(message);
            }
        }
    }

    /**
     * Sets a IDdmConsole to override the default behavior of the console
     * @param console The new IDdmConsole
     * **/
    public static void setConsole(IDdmConsole console) {
        mConsole = console;
    }
}
