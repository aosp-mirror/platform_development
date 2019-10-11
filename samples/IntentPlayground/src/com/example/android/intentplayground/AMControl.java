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
package com.example.android.intentplayground;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides a backend for launching activities using the ActivityManager command line.
 */
class AMControl {
    public static final String TAG = "AMControl";

    /**
     * Launches the activity specified by an {@link Intent} in the background.
     * @param intent The intent to launch.
     * @return The output of the "am shell" command.
     */
    public static String launchInBackground(Intent intent) {
        StringBuilder cmd = new StringBuilder("am start -n ");
        ComponentName target = intent.getComponent();
        cmd.append(target.getPackageName()).append("/").append(target.getShortClassName());
        cmd.append(" -f ").append("0x").append(Integer.toHexString(intent.getFlags()));
        cmd.append(" --ez moveToBack true");
        return execCmd(cmd.toString());
    }

    /**
     * Executes a shell command in a separate process.
     * @param cmd The command to execute.
     * @return The output of the command.
     */
    public static String execCmd(String cmd) {
        StringBuilder output = new StringBuilder();
        ProcessBuilder factory = new ProcessBuilder(cmd.split(" "));
        String line;
        int lineCount = 0;
        if (BuildConfig.DEBUG) Log.d(TAG, "Running command " + cmd);
        try {
            Process proc = factory.start();
            // get stdout
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    proc.getInputStream()));
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                lineCount++;
            }
            reader.close();
            // get stderr
            reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
                lineCount++;
            }
            reader.close();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, String.format("Received %d lines from %s:\n %s",
                        lineCount, cmd.split(" ")[0], output.toString()));
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) Log.e(TAG, output.append(e.getMessage()).toString());
            throw new RuntimeException(e);
        }
        return output.toString();
    }
}
