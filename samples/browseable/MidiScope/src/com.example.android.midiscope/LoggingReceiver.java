/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.midiscope;

import android.media.midi.MidiReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Convert incoming MIDI messages to a string and write them to a ScopeLogger.
 * Assume that messages have been aligned using a MidiFramer.
 */
public class LoggingReceiver extends MidiReceiver {
    public static final String TAG = "MidiScope";
    private static final long NANOS_PER_SECOND = TimeUnit.SECONDS.toNanos(1);
    private long mStartTime;
    private ScopeLogger mLogger;

    public LoggingReceiver(ScopeLogger logger) {
        mStartTime = System.nanoTime();
        mLogger = logger;
    }

    /*
     * @see android.media.midi.MidiReceiver#onReceive(byte[], int, int, long)
     */
    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        if (timestamp == 0) {
            sb.append(String.format("-----0----: "));
        } else {
            long monoTime = timestamp - mStartTime;
            double seconds = (double) monoTime / NANOS_PER_SECOND;
            sb.append(String.format("%10.3f: ", seconds));
        }
        sb.append(MidiPrinter.formatBytes(data, offset, count));
        sb.append(": ");
        sb.append(MidiPrinter.formatMessage(data, offset, count));
        String text = sb.toString();
        mLogger.log(text);
        Log.i(TAG, text);
    }

}