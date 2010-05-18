/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.monkeyrunner.adb;

import com.android.ddmlib.IShellOutputReceiver;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shell Output Receiver that sends shell output to a Logger.
 */
public class LoggingOutputReceiver implements IShellOutputReceiver {
    private final Logger log;
    private final Level level;

    public LoggingOutputReceiver(Logger log, Level level) {
        this.log = log;
        this.level = level;
    }

    public void addOutput(byte[] data, int offset, int length) {
        String message = new String(data, offset, length);
        for (String line : message.split("\n")) {
            log.log(level, line);
        }
    }

    public void flush() { }

    public boolean isCancelled() {
        return false;
    }
}
