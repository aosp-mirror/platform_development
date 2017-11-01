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

package com.android.bugreport.bugreport;

import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.util.Utils;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parse the header and footer of the bugreport.
 */
public class MetadataParser {

    private static final Pattern DUMPSTATE_LINE_RE = Pattern.compile(
            "== dumpstate: " + Utils.DATE_TIME_PATTERN);
    private static final Pattern HEADER_LINE_RE = Pattern.compile(
            "([^:]+): (.*)");

    private final Matcher mDumpstateLineRe = DUMPSTATE_LINE_RE.matcher("");
    private final Matcher mHeaderLineRe = HEADER_LINE_RE.matcher("");

    private Bugreport mBugreport;

    /**
     * Constructor
     */
    public MetadataParser() {
    }

    /**
     * Set the Bugreport that we're working on.
     */
    public void setBugreport(Bugreport bugreport) {
        mBugreport = bugreport;
    }

    /**
     * Parse the preamble.
     */
    public void parseHeader(Lines<? extends Line> lines) {
        Matcher m;
        int lineno = 0;

        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;

            if ((m = Utils.match(mDumpstateLineRe, text)) != null) {
                mBugreport.startTime = Utils.parseCalendar(m, 1, false);
            } else if ((m = Utils.match(mHeaderLineRe, text)) != null) {
                final String key = m.group(1);
                final String value = m.group(2);
                if ("Build".equals(key)) {
                    mBugreport.buildId = value;
                }
            }
        }
        
    }

    /**
     * Parse the footer.
     */
    public void parseFooter(Lines<? extends Line> lines, int durationMs) {
        if (mBugreport.startTime != null) {
            mBugreport.endTime = (GregorianCalendar)mBugreport.startTime.clone();
            mBugreport.endTime.add(Calendar.MILLISECOND, durationMs);
        }
    }
}

