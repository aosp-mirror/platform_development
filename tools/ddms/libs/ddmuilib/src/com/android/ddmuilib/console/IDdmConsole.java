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
 * DDMS console interface.
 */
public interface IDdmConsole {
    /**
     * Prints a message to the android console.
     * @param message the message to print
     */
    public void printErrorToConsole(String message);

    /**
     * Prints several messages to the android console.
     * @param messages the messages to print
     */
    public void printErrorToConsole(String[] messages);

    /**
     * Prints a message to the android console.
     * @param message the message to print
     */
    public void printToConsole(String message);

    /**
     * Prints several messages to the android console.
     * @param messages the messages to print
     */
    public void printToConsole(String[] messages);
}
