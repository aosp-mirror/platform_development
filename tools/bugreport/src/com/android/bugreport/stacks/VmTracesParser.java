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
public class VmTracesParser {

    private final Matcher mBeginProcessRe = ProcessSnapshotParser.BEGIN_PROCESS_RE.matcher("");
    
    /**
     * Construct a new parser.
     */
    public VmTracesParser() {
    }

    /**
     * Do the parsing.
     */
    public VmTraces parse(Lines<? extends Line> lines) {
        final VmTraces result = new VmTraces();

        // Drop any preamble
        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;
            if (Utils.matches(mBeginProcessRe, text)) {
                lines.rewind();
                break;
            }
        }

        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;

            if (Utils.matches(mBeginProcessRe, text)) {
                lines.rewind();
                ProcessSnapshotParser parser = new ProcessSnapshotParser(); 
                final ProcessSnapshot snapshot = parser.parse(lines);
                if (snapshot != null) {
                    result.processes.add(snapshot);
                } else {
                    // TODO: Try to backtrack and correct the parsing.
                }
            } else {
                if (false) {
                    System.out.println("VmTracesParser Dropping: " + text);
                }
            }
        }

        return result;
    }

}


