/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.dumpviewer.pickers;

import com.android.dumpviewer.utils.Exec;
import com.android.dumpviewer.utils.Utils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PackageNamePicker extends PickerActivity {
    // "[^\]]" doesn't seem to work with Android's sed..?
    final String COMMAND =
            "dumpsys package packages | sed -ne 's/^ *Package *\\[\\(.*\\)\\].*/\\1/p'" +
                    " | sort -u 2>&1";

    @Override
    protected String[] getList() {
        final AtomicBoolean timedOut = new AtomicBoolean();
        final AtomicReference<String> message = new AtomicReference<>();

        try {
            return Exec.runForStrings(
                    COMMAND,
                    message::set,
                    () -> timedOut.set(true),
                    (e) -> {throw new RuntimeException(e.getMessage(), e);},
                    5);
        } catch (IOException e) {
            if (timedOut.get()) {
                return new String[]{"Command timed out"};
            }
            Utils.toast(this, "Error: " + e.getMessage());
            return null;
        }
    }
}
