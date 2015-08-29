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

import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiDeviceInfo.PortInfo;
import android.os.Bundle;

import com.example.android.common.midi.MidiConstants;

/**
 * Format a MIDI message for printing.
 */
public class MidiPrinter {

    public static final String[] CHANNEL_COMMAND_NAMES = { "NoteOff", "NoteOn",
            "PolyTouch", "Control", "Program", "Pressure", "Bend" };
    public static final String[] SYSTEM_COMMAND_NAMES = { "SysEx", // F0
            "TimeCode",    // F1
            "SongPos",     // F2
            "SongSel",     // F3
            "F4",          // F4
            "F5",          // F5
            "TuneReq",     // F6
            "EndSysex",    // F7
            "TimingClock", // F8
            "F9",          // F9
            "Start",       // FA
            "Continue",    // FB
            "Stop",        // FC
            "FD",          // FD
            "ActiveSensing", // FE
            "Reset"        // FF
    };

    public static String getName(int status) {
        if (status >= 0xF0) {
            int index = status & 0x0F;
            return SYSTEM_COMMAND_NAMES[index];
        } else if (status >= 0x80) {
            int index = (status >> 4) & 0x07;
            return CHANNEL_COMMAND_NAMES[index];
        } else {
            return "data";
        }
    }

    public static String formatBytes(byte[] data, int offset, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(String.format(" %02X", data[offset + i]));
        }
        return sb.toString();
    }

    public static String formatMessage(byte[] data, int offset, int count) {
        StringBuilder sb = new StringBuilder();
        byte statusByte = data[offset++];
        int status = statusByte & 0xFF;
        sb.append(getName(status)).append("(");
        int numData = MidiConstants.getBytesPerMessage(statusByte) - 1;
        if ((status >= 0x80) && (status < 0xF0)) { // channel message
            int channel = status & 0x0F;
            // Add 1 for humans who think channels are numbered 1-16.
            sb.append((channel + 1)).append(", ");
        }
        for (int i = 0; i < numData; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(data[offset++]);
        }
        sb.append(")");
        return sb.toString();
    }

    public static String formatDeviceInfo(MidiDeviceInfo info) {
        StringBuilder sb = new StringBuilder();
        if (info != null) {
            Bundle properties = info.getProperties();
            for (String key : properties.keySet()) {
                Object value = properties.get(key);
                sb.append(key).append(" = ").append(value).append('\n');
            }
            for (PortInfo port : info.getPorts()) {
                sb.append((port.getType() == PortInfo.TYPE_INPUT) ? "input"
                        : "output");
                sb.append("[").append(port.getPortNumber()).append("] = \"").append(port.getName()
                        + "\"\n");
            }
        }
        return sb.toString();
    }
}
