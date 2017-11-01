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

package com.android.bugreport.anr;

import com.android.bugreport.cpuinfo.CpuUsageParser;
import com.android.bugreport.cpuinfo.CpuUsageSnapshot;
import com.android.bugreport.util.Utils;
import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.ProcessSnapshotParser;
import com.android.bugreport.stacks.VmTraces;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parse an anr block from a monkey log.
 *
 * The parser can be reused, but is not thread safe.
 */
public class AnrParser {
    private static final Pattern PROC_NAME_RE
            = Pattern.compile("ANR in (\\S+) \\((\\S+)/(\\S+)\\)");
    private static final Pattern PID_RE
            = Pattern.compile("PID: (\\d+)");
    private static final Pattern REASON_RE
            = Pattern.compile("Reason: (.*)");
    private static final Pattern BLANK_RE
            = Pattern.compile("\\s+");

    /**
     * Construct a new parser.
     */
    public AnrParser() {
    }

    /**
     * Do the parsing.
     */
    public ArrayList<Anr> parse(Lines<? extends Line> lines, boolean tryTraces) {
        final ArrayList<Anr> results = new ArrayList<Anr>();
        Anr anr = null;

        final Matcher procNameRe = PROC_NAME_RE.matcher("");
        final Matcher pidRe = PID_RE.matcher("");
        final Matcher reasonRe = REASON_RE.matcher("");
        final Matcher cpuUsageRe = CpuUsageParser.CPU_USAGE_RE.matcher("");
        final Matcher beginProcessRe = ProcessSnapshotParser.BEGIN_PROCESS_RE.matcher("");

        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;
            if (Utils.matches(procNameRe, text)) {
                anr = new Anr();
                anr.vmTraces = new VmTraces();
                results.add(anr);
                anr.processName = procNameRe.group(1);
                anr.componentPackage = procNameRe.group(2);
                anr.componentClass = procNameRe.group(3);
            } else if (Utils.matches(pidRe, text)) {
                if (anr != null) {
                    anr.pid = Integer.parseInt(pidRe.group(1));
                }
            } else if (Utils.matches(reasonRe, text)) {
                if (anr != null) {
                    anr.reason = reasonRe.group(1);
                }
            } else if (Utils.matches(cpuUsageRe, text)) {
                if (anr != null) {
                    lines.rewind();
                    CpuUsageParser parser = new CpuUsageParser(); 
                    final CpuUsageSnapshot snapshot = parser.parse(lines);
                    if (snapshot != null) {
                        anr.cpuUsages.add(snapshot);
                    } else {
                        // TODO: Try to backtrack and correct the parsing.
                    }
                }
            } else if (Utils.matches(beginProcessRe, text)) {
                if (tryTraces && anr != null) {
                    lines.rewind();
                    ProcessSnapshotParser parser = new ProcessSnapshotParser(); 
                    final ProcessSnapshot snapshot = parser.parse(lines);
                    if (snapshot != null) {
                        anr.vmTraces.processes.add(snapshot);
                    } else {
                        // TODO: Try to backtrack and correct the parsing.
                    }
                }
            } else {
                // Unknown
                //
                // TODO: These lines:
                //      Load: 16.37 / 7.19 / 2.73
                //      procrank:
                if (false) {
                    System.out.println("AnrParser Dropping: " + text);
                }
            }
        }

        if (false) {
            for (Anr item: results) {
                System.out.println("ANR");
                System.out.println("  processName=" + item.processName);
                System.out.println("  componentPackage=" + item.componentPackage);
                System.out.println("  componentClass=" + item.componentClass);
                System.out.println("  pid=" + item.pid);
                System.out.println("  reason=" + item.reason);
            }
        }

        return results;
    }

}

