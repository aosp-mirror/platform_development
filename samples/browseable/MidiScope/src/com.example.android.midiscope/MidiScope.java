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

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;

import com.example.android.common.midi.MidiFramer;

import java.io.IOException;

/**
 * Virtual MIDI Device that logs messages to a ScopeLogger.
 */

public class MidiScope extends MidiDeviceService {

    private static ScopeLogger mScopeLogger;
    private MidiReceiver mInputReceiver = new MyReceiver();
    private static MidiFramer mDeviceFramer;

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mInputReceiver };
    }

    public static ScopeLogger getScopeLogger() {
        return mScopeLogger;
    }

    public static void setScopeLogger(ScopeLogger logger) {
        if (logger != null) {
            // Receiver that prints the messages.
            LoggingReceiver loggingReceiver = new LoggingReceiver(logger);
            mDeviceFramer = new MidiFramer(loggingReceiver);
        }
        mScopeLogger = logger;
    }

    private static class MyReceiver extends MidiReceiver {
        @Override
        public void onSend(byte[] data, int offset, int count,
                long timestamp) throws IOException {
            if (mScopeLogger != null) {
                // Send raw data to be parsed into discrete messages.
                mDeviceFramer.send(data, offset, count, timestamp);
            }
        }
    }

    /**
     * This will get called when clients connect or disconnect.
     * Log device information.
     */
    @Override
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
        if (mScopeLogger != null) {
            if (status.isInputPortOpen(0)) {
                mScopeLogger.log("=== connected ===");
                String text = MidiPrinter.formatDeviceInfo(
                        status.getDeviceInfo());
                mScopeLogger.log(text);
            } else {
                mScopeLogger.log("--- disconnected ---");
            }
        }
    }
}
