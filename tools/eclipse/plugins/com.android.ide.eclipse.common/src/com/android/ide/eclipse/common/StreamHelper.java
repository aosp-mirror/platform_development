/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ide.eclipse.common;

import org.eclipse.ui.console.MessageConsoleStream;

import java.util.Calendar;

/**
 * Stream helper class.
 */
public class StreamHelper {

    /**
     * Prints messages, associated with a project to the specified stream
     * @param stream The stream to write to
     * @param tag The tag associated to the message. Can be null
     * @param objects The objects to print through their toString() method (or directly for
     * {@link String} objects.
     */
    public static synchronized void printToStream(MessageConsoleStream stream, String tag,
            Object... objects) {
        String dateTag = getMessageTag(tag);

        for (Object obj : objects) {
            stream.print(dateTag);
            if (obj instanceof String) {
                stream.println((String)obj);
            } else {
                stream.println(obj.toString());
            }
        }
    }

    /**
     * Creates a string containing the current date/time, and the tag
     * @param tag The tag associated to the message. Can be null
     * @return The dateTag
     */
    public static String getMessageTag(String tag) {
        Calendar c = Calendar.getInstance();

        if (tag == null) {
            return String.format(Messages.Console_Date_Tag, c);
        }

        return String.format(Messages.Console_Data_Project_Tag, c, tag);
    }
}
