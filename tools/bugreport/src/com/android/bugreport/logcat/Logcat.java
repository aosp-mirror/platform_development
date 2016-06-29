/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bugreport.logcat;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Set;

/**
 * Class to represent an android log.
 */
public class Logcat {
    /**
     * The lines contained in this logcat.
     */
    public ArrayList<LogLine> lines = new ArrayList<LogLine>();

    /**
     * Return the lines that match the given log tags and optional log level.
     */
    public ArrayList<LogLine> filter(Set<String> tags, String levels) {
        final ArrayList<LogLine> result = new ArrayList<LogLine>();
        for (LogLine line: lines) {
            if (tags.contains(line.tag) && (levels == null || levels.indexOf(line.level) >= 0)) {
                result.add(line);
            }
        }
        return result;
    }

    /**
     * Return the lines that match the given log tag and optional log level.
     */
    public ArrayList<LogLine> filter(String tag, String levels) {
        final ArrayList<LogLine> result = new ArrayList<LogLine>();
        for (LogLine line: lines) {
            if (tag.equals(line.tag) && (levels == null || levels.indexOf(line.level) >= 0)) {
                result.add(line);
            }
        }
        return result;
    }
}
