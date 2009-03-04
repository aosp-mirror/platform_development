/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.text.DecimalFormat;

// This should be a singleton.
public class TraceUnits {

    private TimeScale mTimeScale = TimeScale.MicroSeconds;
    private double mScale = 1.0;
    DecimalFormat mFormatter = new DecimalFormat();

    public double getScaledValue(long value) {
        return value * mScale;
    }

    public double getScaledValue(double value) {
        return value * mScale;
    }

    public String valueOf(long value) {
        return valueOf((double) value);
    }

    public String valueOf(double value) {
        String pattern;
        double scaled = value * mScale;
        if ((int) scaled == scaled)
            pattern = "###,###";
        else
            pattern = "###,###.###";
        mFormatter.applyPattern(pattern);
        return mFormatter.format(scaled);
    }

    public String labelledString(double value) {
        String units = label();
        String num = valueOf(value);
        return String.format("%s: %s", units, num);
    }

    public String labelledString(long value) {
        return labelledString((double) value);
    }

    public String label() {
        if (mScale == 1.0)
            return "usec";
        if (mScale == 0.001)
            return "msec";
        if (mScale == 0.000001)
            return "sec";
        return null;
    }

    public void setTimeScale(TimeScale val) {
        mTimeScale = val;
        switch (val) {
        case Seconds:
            mScale = 0.000001;
            break;
        case MilliSeconds:
            mScale = 0.001;
            break;
        case MicroSeconds:
            mScale = 1.0;
            break;
        }
    }

    public TimeScale getTimeScale() {
        return mTimeScale;
    }

    public enum TimeScale {
        Seconds, MilliSeconds, MicroSeconds
    };
}
