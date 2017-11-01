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

package com.android.bugreport.cpuinfo;

import com.android.bugreport.util.Utils;
import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Incomplete - Reads a little bit of the cpu usage block printed in the monkey report.
 */
public class CpuUsageParser {
    public static final Pattern CPU_USAGE_RE
            = Pattern.compile("CPU usage from (-?\\d+)ms to (-?\\d+)ms ago \\((.*) to (.*)\\):");
    private static final Pattern TOTAL_RE
            = Pattern.compile(".*TOTAL:.*");
            //= Pattern.compile("(-?\\d*(?:.\\d+)?)% TOTAL: (-?\\d*(?:.\\d+)?)% user \\+ (-?\\d*(?:.\\d+)?)% kernel \\+ (-?\\d*(?:.\\d+)?)% iowait \\+ (-?\\d*(?:.\\d+)?)% softirq");


    public CpuUsageParser() {
    }

    public CpuUsageSnapshot parse(Lines<? extends Line> lines) {
        final CpuUsageSnapshot result = new CpuUsageSnapshot();

        final Matcher cpuUsageRe = CPU_USAGE_RE.matcher("");
        final Matcher totalRe = TOTAL_RE.matcher("");

        while (lines.hasNext()) {
            final Line line = lines.next();
            final String text = line.text;
            if (Utils.matches(cpuUsageRe, text)) {
                // System.out.println("CpuUsageParser cpuUsageRe: " + text);
            } else if (Utils.matches(totalRe, text)) {
                /*
                result.totalPercent = Float.parseFloat(totalRe.group(1));
                result.totalUser = Float.parseFloat(totalRe.group(2));
                result.totalKernel = Float.parseFloat(totalRe.group(3));
                result.totalIoWait = Float.parseFloat(totalRe.group(3));
                result.totalSoftIrq = Float.parseFloat(totalRe.group(3));
                */
                break;
            } else {
                if (false) {
                    System.out.println("CpuUsageParser Dropping: " + text);
                }
            }
        }

        if (false) {
            System.out.println("totalPercent=" + result.totalPercent);
            System.out.println("totalUser=" + result.totalUser);
            System.out.println("totalKernel=" + result.totalKernel);
            System.out.println("totalIoWait=" + result.totalIoWait);
            System.out.println("totalSoftIrq=" + result.totalSoftIrq);
        }

        return result;
    }

}

