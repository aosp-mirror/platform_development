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

package com.android.bugreport.monkey;

import com.android.bugreport.anr.Anr;
import com.android.bugreport.anr.AnrParser;
import com.android.bugreport.bugreport.Bugreport;
import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parser for a monkey log file.
 */
public class MonkeyLogParser {
    private static final Pattern NOT_RESPONDING_RE
            = Pattern.compile("// NOT RESPONDING: \\S+ \\(pid \\d+\\)");
    private static final Pattern ABORTED_RE
            = Pattern.compile("\\*\\* Monkey aborted due to error\\.");
    
    public MonkeyLogParser() {
    }

    /**
     * Parses the monkey file, adding in what's there into an already
     * created bugreport.
     */
    public void parse(Bugreport bugreport, Lines<? extends Line> in) throws IOException {
        // Get the lines
        final Lines<Line> lines = extractAnrLines(in);
        if (!lines.hasNext()) {
            return;
        }

        final Line line = lines.next();
        final String text = line.text;

        // ANRs
        final Matcher anrStart = NOT_RESPONDING_RE.matcher(text);
        if (Utils.matches(anrStart, text)) {
            final AnrParser anrParser = new AnrParser();
            final ArrayList<Anr> anrs = anrParser.parse(lines, true);
            if (anrs.size() >= 1) {
                // Pick the first one.
                bugreport.anr = bugreport.monkeyAnr = anrs.get(0);
            }
        }
    }

    /**
     * Pull out the ANR lines from a monkey log.
     */
    private static Lines<Line> extractAnrLines(Lines<? extends Line> lines) throws IOException {
        final int STATE_INITIAL = 0;
        final int STATE_ANR = 1;
        final int STATE_DONE = 2;

        ArrayList<Line> list = new ArrayList<Line>();

        final Matcher anrStart = NOT_RESPONDING_RE.matcher("");
        final Matcher monkeyEnd = ABORTED_RE.matcher("");

        int state = STATE_INITIAL;
        int lineno = 0;
        while (state != STATE_DONE && lines.hasNext()) {
            final Line line = lines.next();
            lineno++;
            switch (state) {
                case STATE_INITIAL:
                    anrStart.reset(line.text);
                    if (anrStart.matches()) {
                        state = STATE_ANR;
                        list.add(new Line(lineno, line.text));
                    }
                    break;
                case STATE_ANR:
                    monkeyEnd.reset(line.text);
                    if (monkeyEnd.matches()) {
                        state = STATE_DONE;
                    } else {
                        list.add(new Line(lineno, line.text));
                    }
                    break;
                default:
                    break;
            }
        }

        return new Lines<Line>(list);
    }
}
