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
package com.android.dumpviewer.utils;

import android.util.Log;

import com.android.dumpviewer.DumpActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Exec {
    public static final String TAG = DumpActivity.TAG;

    private Exec() {
    }

    public static InputStream runForStream(
            String commandLine,
            Consumer<String> messenger,
            Runnable onCanceled,
            Consumer<Exception> onException,
            int timeoutSeconds
            )
            throws IOException {
        messenger.accept("Running: " + commandLine);
        Log.v(TAG, "Running: " + commandLine);

        final Process p = Runtime.getRuntime().exec(
                new String[]{"/system/bin/sh", "-c", commandLine});
        final InputStream in = p.getInputStream();

        final AtomicReference<Throwable> th = new AtomicReference<>();
        new Thread(() -> {
            try {
                Log.v(TAG, "Waiting for process: " + p);
                final boolean success = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                if (success) {
                    Log.v(TAG, String.format("Command %s finished with code %d", commandLine,
                            p.exitValue()));
                } else {
                    messenger.accept(String.format("Command %s timed out", commandLine));
                    onCanceled.run();
                    try {
                        p.destroyForcibly();
                        in.close();
                    } catch (Exception ignore) {
                    }
                }
            } catch (Exception e) {
                th.set(e);
            }
        }).start();

        return in;
    }

    public static String[] runForStrings(
            String commandLine,
            Consumer<String> messenger,
            Runnable onCanceled,
            Consumer<Exception> onException,
            int timeoutSeconds)
            throws IOException {
        final ArrayList<String> ret = new ArrayList<>(128);
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(
                runForStream(commandLine, messenger, onCanceled, onException, timeoutSeconds)))) {
            while (true) {
                String line = rd.readLine();
                if (line == null) {
                    break;
                }
                ret.add(line);
            }
        }
        return ret.toArray(new String[ret.size()]);
    }
}
