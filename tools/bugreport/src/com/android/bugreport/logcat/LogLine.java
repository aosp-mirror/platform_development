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

import com.android.bugreport.bugreport.ProcessInfo;
import com.android.bugreport.bugreport.ThreadInfo;
import com.android.bugreport.util.Line;

import java.util.ArrayList;
import java.util.GregorianCalendar;

/**
 * A log line.
 */
public class LogLine extends Line {

    /**
     * The raw text of the log.
     */
    public String rawText;

    /**
     * If this is set, this line is the beginning of a log buffer. All the following
     * fields will be null / unset.
     */
    public String bufferBegin;

    /**
     * The raw text of everything up to the tag.
     */
    public String header;

    /**
     * The timestamp of the event. In UTC even though the device might not have been.
     */
    public GregorianCalendar time;

    /**
     * The process that emitted the log.
     */
    public int pid = -1;

    /**
     * The thread that emitted the log.
     */
    public int tid = -1;

    /**
     * The log level. One of EWIDV.
     */
    public char level;

    /**
     * The log tag.
     */
    public String tag;

    /**
     * If this log was taken during the period when the app was unresponsive
     * preceeding an anr.
     */
    public boolean regionAnr;

    /**
     * If a bugreport was being taken during this log line
     */
    public boolean regionBugreport;

    /**
     * The process associated with this log line.
     *
     * TODO: Use the full list of processes from all sources, not just the one from the ANR.
     */
    public ProcessInfo process;

    /**
     * The thread associated with this log line.
     *
     * TODO: Use the full list of processes from all sources, not just the one from the ANR.
     */
    public ThreadInfo thread;
}

