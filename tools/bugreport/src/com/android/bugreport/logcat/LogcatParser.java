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

import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.util.Utils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parses a stream of text as a logcat.
 */
public class LogcatParser {

    public static final Pattern BUFFER_BEGIN_RE = Pattern.compile(
            "--------- beginning of (.*)");
    private static final Pattern LOG_LINE_RE = Pattern.compile(
            "(" + Utils.DATE_TIME_MS_PATTERN
                + "\\s+(\\d+)\\s+(\\d+)\\s+(.)\\s+)(.*?):\\s(.*)");

    private final Matcher mBufferBeginRe = BUFFER_BEGIN_RE.matcher("");
    private final Matcher mLogLineRe = LOG_LINE_RE.matcher("");

    /**
     * Constructor
     */
    public LogcatParser() {
    }

    /**
     * Parse the logcat lines, returning a Logcat object.
     */
    public Logcat parse(Lines<? extends Line> lines) {
        final Logcat result = new Logcat();

        Matcher m;
        int lineno = 0;

        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;

            if ((m = Utils.match(mBufferBeginRe, text)) != null) {
                // Beginning of buffer marker
                final LogLine ll = new LogLine();

                ll.lineno = lineno++;
                ll.rawText = text;
                ll.bufferBegin = m.group(1);

                result.lines.add(ll);
            } else if ((m = Utils.match(mLogLineRe, text)) != null) {
                // Matched line
                final LogLine ll = new LogLine();

                ll.lineno = lineno++;
                ll.rawText = text;
                ll.header = m.group(1);
                ll.time = Utils.parseCalendar(m, 2, true);
                ll.pid = Integer.parseInt(m.group(9));
                ll.tid = Integer.parseInt(m.group(10));
                ll.level = m.group(11).charAt(0);
                ll.tag = m.group(12);
                ll.text = m.group(13);

                result.lines.add(ll);

                if (false) {
                    System.out.println("LogLine: time=" + ll.time + " pid=" + ll.pid
                            + " tid=" + ll.tid + " level=" + ll.level + " tag=" + ll.tag
                            + " text=" + ll.text);
                }
            } else {
                if (false) {
                    System.out.println("\nUNMATCHED: [" + text + "]");
                }
            }
        }

        return result;
    }

}
