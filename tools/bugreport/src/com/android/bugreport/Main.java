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
import com.android.bugreport.monkey.MonkeyLogParser;
import com.android.bugreport.util.ArgParser;

import java.io.BufferedReader;
import java.io.File;
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
        File bugFile = null;
        File monkeyFile = null;
        File htmlFile = null;

        // Parse args
        String flag;
        final ArgParser argParser = new ArgParser(args);
        while ((flag = argParser.nextFlag()) != null) {
            if ("--monkey".equals(flag)) {
                if (monkeyFile != null || !argParser.hasData(1)) {
                    return usage();
                }
                monkeyFile = new File(argParser.nextData());
            } else if ("--html".equals(flag)) {
                if (htmlFile != null || !argParser.hasData(1)) {
                    return usage();
                }
                htmlFile = new File(argParser.nextData());
            } else {
                return usage();
            }
        }
        if ((!argParser.hasData(1)) || argParser.remaining() != 1) {
            return usage();
        }
        bugFile = new File(argParser.nextData());

        // Run
        return run(bugFile, monkeyFile, htmlFile);
    }

    /**
     * Prints the usage message to stderr and returns 1.
     */
    private static int usage() {
        System.err.println("usage: bugreport --monkey MONKEYLOG --html HTML BUGREPORT\n");
        return 1;
    }

    /**
     * Run the tool with the given files.
     *
     * @return the process exit code.
     */
    public static int run(File bugFile, File monkeyFile, File htmlFile) {
        Bugreport bugreport = null;
        FileReader reader = null;

        // Parse bugreport file
        try {
            reader = new FileReader(bugFile);
            final BugreportParser parser = new BugreportParser();
            bugreport = parser.parse(new BufferedReader(reader));
        } catch (IOException ex) {
            System.err.println("Error reading monkey file: " + bugFile);
            System.err.println("Error: " + ex.getMessage());
            return 1;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                }
            }
        }

        // Also parse the monkey log if we have one. That parser will merge
        // into the Bugreport we already parsed.
        if (monkeyFile != null) {
            try {
                reader = new FileReader(monkeyFile);
                final MonkeyLogParser parser = new MonkeyLogParser();
                parser.parse(bugreport, new BufferedReader(reader));
            } catch (IOException ex) {
                System.err.println("Error reading bugreport file: " + bugFile);
                System.err.println("Error: " + ex.getMessage());
                return 1;
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
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
            renderer.render(htmlFile, bugreport);
        } catch (IOException ex) {
            System.err.println("Error reading output file: " + htmlFile);
            System.err.println("Error: " + ex.getMessage());
            return 1;
        }

        return 0;
    }
}

