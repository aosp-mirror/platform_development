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

import com.android.bugreport.logcat.LogcatParser;
import com.android.bugreport.stacks.VmTracesParser;
import com.android.bugreport.util.Utils;
import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parses a bugreport text file.  The object can be reused, but can only parse
 * one bugreport at a time (i.e. any single object is not thread-safe).
 */
public class BugreportParser {

    private static final Pattern SECTION_BEGIN = Pattern.compile(
            "------ (.*?)(?: \\((.*)\\)) ------");
    private static final Pattern SECTION_BEGIN_NO_CMD = Pattern.compile(
            "------ ([^(]+) ------");
    private static final Pattern SECTION_END = Pattern.compile(
            "------ (\\d+.\\d+)s was the duration of '(.*?)(?: \\(.*\\))?' ------");

    private final Matcher mSectionBegin = SECTION_BEGIN.matcher("");
    private final Matcher mSectionBeginNoCmd = SECTION_BEGIN_NO_CMD.matcher("");
    private final Matcher mSectionEnd = SECTION_END.matcher("");

    private final HashMap<String,SectionParser> mSectionParsers
            = new HashMap<String,SectionParser>();

    private final MetadataParser mMetadataParser = new MetadataParser();

    private Bugreport mBugreport;

    /**
     * Base class for bugreport section parsers. They self-report which
     * sections they are interested in, and BugreportParser will call them
     * when a section is encountered.  These then call into the other
     * packges' parsers to do the actual parsing.
     */
    private interface SectionParser {

        /**
         * Return the sections that this parser can handle.
         */
        public String[] getSectionNames();

        /**
         * Parse the given lines.  Add any information found to mBugreport.
         */
        public void parse(String section, String command, Lines<? extends Line> lines);
    }
    
    /**
     * Construct the bugreport parser.
     */
    public BugreportParser() {
        // Initialize the section parsers.
        for (SectionParser parser: mParserList) {
            for (String name: parser.getSectionNames()) {
                mSectionParsers.put(name, parser);
            }
        }
    }

    /**
     * Parse the input into a Bugreport object.
     */
    public Bugreport parse(Lines<? extends Line> lines) {
        mBugreport = new Bugreport();
        Matcher m;
        int pos;

        mMetadataParser.setBugreport(mBugreport);

        // Read and parse the preamble -- until the first section beginning
        pos = lines.pos;
        while (lines.hasNext()) {
            final Line line = lines.next();
            if (Utils.matches(mSectionBegin, line.text)) {
                lines.rewind();
                mMetadataParser.parseHeader(lines.copy(pos, lines.pos));
                break;
            }
        }

        // Read each section, and then parse it
        String section = null;
        String command = null;
        while (lines.hasNext()) {
            final Line line = lines.next();
            if ((m = Utils.match(mSectionEnd, line.text)) != null) {
                final int durationMs = (int)(Float.parseFloat(m.group(1)) * 1000);
                final String endSection = m.group(2);
                if (section != null && endSection.equals(section)) {
                    // End of the section
                    parseSection(section, lines.copy(pos, lines.pos-1), command, durationMs);
                    pos = lines.pos; // for the footer
                    section = null;
                } else {
                    if (false) {
                        System.out.println("mismatched end of section=" + section + " endSection="
                                + endSection);
                    }
                    // We missed it. Don't do anything. Keep reading until we find
                    // the right section marker or the beginning of the next section.
                    // Except the last one for the whole bugreport has an extra footer
                    if ("DUMPSTATE".equals(endSection)) {
                        mMetadataParser.parseFooter(lines.copy(pos, lines.pos-1), durationMs);
                    }
                }
            } else if (((m = Utils.match(mSectionBegin, line.text)) != null)
                    || ((m = Utils.match(mSectionBeginNoCmd, line.text)) != null)) {
                // Beginning of the section
                // Clean out any section that wasn't closed propertly (it happens)
                if (section != null) {
                    if (false) {
                        System.out.println("missed end of section " + section);
                    }
                    parseSection(section, lines.copy(pos, lines.pos-1), null, -1);
                }
                section = m.group(1);
                command = (m.groupCount() > 1) ? command = m.group(2) : null;
                pos = lines.pos;
            }
        }

        return mBugreport;
    }

    /**
     * Parse the stuff in the preamble.
     */
    private void parseSection(String section, Lines<? extends Line> lines, String command,
            int durationMs) {
        final SectionParser parser = mSectionParsers.get(section);
        if (parser != null) {
            if (false) {
                System.out.println("Parsing section  '" + section + "' " + lines.size() + " lines");
            }
            parser.parse(section, command, lines);
        } else {
            if (false) {
                System.out.println("Skipping section '" + section + "' " + lines.size() + " lines");
            }
        }
    }

    /**
     * The list of section parsers. Each one handles one or more sections, and adds that
     * stuff to the Bugreport.
     */
    final SectionParser[] mParserList = new SectionParser[] {
        new SectionParser() {
            private LogcatParser mParser = new LogcatParser();

            @Override
            public String[] getSectionNames() {
                return new String[] {
                    "SYSTEM LOG",
                    "EVENT LOG",
                };
            }

            @Override
            public void parse(String section, String command, Lines<? extends Line> lines) {
                if ("SYSTEM LOG".equals(section)) {
                    mBugreport.systemLog = mParser.parse(lines);
                } else if ("EVENT LOG".equals(section)) {
                    mBugreport.eventLog = mParser.parse(lines);
                }
            }
        },

        new SectionParser() {
            private VmTracesParser mParser = new VmTracesParser();

            @Override
            public String[] getSectionNames() {
                return new String[] {
                    "VM TRACES JUST NOW",
                    "VM TRACES AT LAST ANR",
                };
            }

            @Override
            public void parse(String section, String command, Lines<? extends Line> lines) {
                if ("VM TRACES JUST NOW".equals(section)) {
                    mBugreport.vmTracesJustNow = mParser.parse(lines);
                } else if ("VM TRACES AT LAST ANR".equals(section)) {
                    mBugreport.vmTracesLastAnr = mParser.parse(lines);
                }
            }
        },
        
    };

}
