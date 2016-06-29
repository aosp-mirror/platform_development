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

import com.android.bugreport.anr.Anr;
import com.android.bugreport.logcat.Logcat;
import com.android.bugreport.logcat.LogLine;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.VmTraces;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * Class to represent what we know and understand about a bugreport.
 */
public class Bugreport {
    /**
     * The build id of the device.
     */
    public String buildId;

    /**
     * Timestamp for the beginning of the bugreport.
     */
    public GregorianCalendar startTime;

    /**
     * Timestamp for the beginning of the bugreport.
     */
    public GregorianCalendar endTime;

    /**
     * The information about the first ANR that contained in the bugreport.  If there
     * was a monkey report, this will be that one.  The first ANR is the most likely culprit.
     */
    public Anr anr;

    /**
     * The information about any ANR found in a monkey report.
     */
    public Anr monkeyAnr;

    /**
     * The merged logcat section of a bugreport.
     */
    public Logcat logcat;

    /**
     * The 'SYSTEM LOG' section of a bugreport.
     */
    public Logcat systemLog;

    /**
     * The 'EVENT LOG' section of a bugreport.
     */
    public Logcat eventLog;

    /**
     * The stack traces from the VM TRACES JUST NOW section.
     */
    public VmTraces vmTracesJustNow;

    /**
     * The stack traces from the VM TRACES AT LAST ANR section.
     */
    public VmTraces vmTracesLastAnr;

    /**
     * The logcat lines that have something interesting about them.
     */
    public ArrayList<LogLine> interestingLogLines = new ArrayList<LogLine>();

    /**
     * The set of all known processes.  This is scraped from lots of sources.
     */
    public HashMap<Integer,ProcessInfo> allKnownProcesses = new HashMap<Integer,ProcessInfo>();
}

