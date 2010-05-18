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

/**
 * Shell Output Receiver that captures shell output into a String for
 * later retrieval.
 */
public class CommandOutputCapture implements IShellOutputReceiver {
    private final StringBuilder builder = new StringBuilder();

    public void flush() { }

    public boolean isCancelled() {
        return false;
    }

    public void addOutput(byte[] data, int offset, int length) {
        String message = new String(data, offset, length);
        builder.append(message);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
