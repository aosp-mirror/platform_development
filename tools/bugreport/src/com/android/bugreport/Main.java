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

package com.android.bugreport;

import com.android.bugreport.bugreport.Bugreport;
import com.android.bugreport.bugreport.BugreportParser;
import com.android.bugreport.html.Renderer;
import com.android.bugreport.inspector.Inspector;
import com.android.bugreport.logcat.LogcatParser;
import com.android.bugreport.monkey.MonkeyLogParser;
import com.android.bugreport.util.Lines;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Main entry point.
 */
public class Main {
    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        System.exit(run(args));
    }

    /**
     * Parse the args and run it.
     *
     * @return the process exit code.
     */
    public static int run(String[] args) {
        // Parse args
        final Options options = Options.parseArgs(args);
        if (options.errorIndex >= 0) {
            return usage();
        }

        // Run
        return run(options);
    }

    /**
     * Prints the usage message to stderr and returns 1.
     */
    private static int usage() {
        System.err.println("usage: bugreport --monkey MONKEYLOG --html HTML --logcat SYSTEMLOG"
                + " BUGREPORT\n");
        return 1;
    }

    /**
     * Run the tool with the given files.
     *
     * @return the process exit code.
     */
    public static int run(Options options) {
        Bugreport bugreport = null;

        // Parse bugreport file
        try {
            final BugreportParser parser = new BugreportParser();
            bugreport = parser.parse(Lines.readLines(options.bugreport));
        } catch (IOException ex) {
            System.err.println("Error reading monkey file: " + options.bugreport);
            System.err.println("Error: " + ex.getMessage());
            return 1;
        }

        // Also parse the monkey log if we have one. That parser will merge
        // into the Bugreport we already parsed.
        if (options.monkey != null) {
            try {
                final MonkeyLogParser parser = new MonkeyLogParser();
                parser.parse(bugreport, Lines.readLines(options.monkey));
            } catch (IOException ex) {
                System.err.println("Error reading bugreport file: " + options.bugreport);
                System.err.println("Error: " + ex.getMessage());
                return 1;
            }
        }

        // Also parse the logcat if we have one. That parser will merge
        // into the Bugreport we already parsed.
        if (options.logcat != null) {
            try {
                final LogcatParser parser = new LogcatParser();
                bugreport.logcat = parser.parse(Lines.readLines(options.logcat));
            } catch (IOException ex) {
                System.err.println("Error reading bugreport file: " + options.bugreport);
                System.err.println("Error: " + ex.getMessage());
                return 1;
            }
        }

        // Inspect the Failure and see if we can figure out what's going on.
        // Fills in the additional fields in the Anr object.
        Inspector.inspect(bugreport);

        // For now, since all we do is ANRs, just bail out if there wasn't one.
        if (bugreport.anr == null) {
            System.err.println("No anr!");
            return 0;
        }

        // Write the html
        try {
            Renderer renderer = new Renderer();
            renderer.render(options.html, bugreport);
        } catch (IOException ex) {
            System.err.println("Error reading output file: " + options.html);
            System.err.println("Error: " + ex.getMessage());
            return 1;
        }

        return 0;
    }
}

