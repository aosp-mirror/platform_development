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

package com.android.bugreport.stacks;

import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parse a vm traces process.
 *
 * The parser can be reused, but is not thread safe.
 */
public class ProcessSnapshotParser {
    public static final Pattern BEGIN_PROCESS_RE = Pattern.compile(
                    "----- pid (\\d+) at (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) -----");
    private static final Pattern END_PROCESS_RE
            = Pattern.compile("----- end \\d+ -----");

    public static final Pattern CMD_LINE_RE = Pattern.compile(
                    "Cmd line: (.*)");

    /**
     * Construct a new parser.
     */
    public ProcessSnapshotParser() {
    }

    /**
     * Parse the given Lines until the beginning of the next process section, or until
     * the end of input is reached.
     */
    public ProcessSnapshot parse(Lines<? extends Line> lines) {
        final ProcessSnapshot result = new ProcessSnapshot();

        final Matcher beginProcessRe = BEGIN_PROCESS_RE.matcher("");
        final Matcher beginUnmanagedThreadRe = ThreadSnapshotParser.BEGIN_UNMANAGED_THREAD_RE
                .matcher("");
        final Matcher beginManagedThreadRe = ThreadSnapshotParser.BEGIN_MANAGED_THREAD_RE
                .matcher("");
        final Matcher beginNotAttachedThreadRe = ThreadSnapshotParser.BEGIN_NOT_ATTACHED_THREAD_RE
                .matcher("");
        final Matcher endProcessRe = END_PROCESS_RE.matcher("");
        final Matcher cmdLineRe = CMD_LINE_RE.matcher("");

        final int STATE_INITIAL = 0;
        final int STATE_THREADS = 1;
        int state = STATE_INITIAL;

        // Preamble
        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;
            if (Utils.matches(beginProcessRe, text)) {
                result.pid = Integer.parseInt(beginProcessRe.group(1));
                result.date = beginProcessRe.group(2);
            } else if (Utils.matches(beginUnmanagedThreadRe, text)) {
                state = STATE_THREADS;
                lines.rewind();
                break;
            } else if (Utils.matches(beginManagedThreadRe, text)) {
                state = STATE_THREADS;
                lines.rewind();
                break;
            } else if (Utils.matches(beginNotAttachedThreadRe, text)) {
                state = STATE_THREADS;
                lines.rewind();
                break;
            } else if (Utils.matches(endProcessRe, text)) {
                break;
            } else if (Utils.matches(cmdLineRe, text)) {
                result.cmdLine = cmdLineRe.group(1);
            } else {
                if (false) {
                    System.out.println("ProcessSnapshotParser Dropping: " + text);
                }
            }
        }
        
        // Thread list
        if (state == STATE_THREADS) {
            while (lines.hasNext()) {
                final Line line = lines.next();
                final String text = line.text;
                if (Utils.matches(beginUnmanagedThreadRe, text)
                        || Utils.matches(beginManagedThreadRe, text)
                        || Utils.matches(beginNotAttachedThreadRe, text)) {
                    lines.rewind();
                    ThreadSnapshotParser parser = new ThreadSnapshotParser(); 
                    final ThreadSnapshot snapshot = parser.parse(lines);
                    if (snapshot != null) {
                        result.threads.add(snapshot);
                    } else {
                        // TODO: Try to backtrack and correct the parsing.
                    }
                } else if (Utils.matches(endProcessRe, text)) {
                    break;
                } else {
                    if (false) {
                        System.out.println("ProcessSnapshotParser STATE_THREADS Dropping: " + text);
                    }
                }
            }
        }

        if (false) {
            System.out.println();
            System.out.println("PROCESS");
            System.out.println("pid=" + result.pid);
            System.out.println("date=" + result.date);
            System.out.println("threads=" + result.threads.size());
            System.out.println("cmdLine=" + result.cmdLine);
        }

        return result;
    }

}

