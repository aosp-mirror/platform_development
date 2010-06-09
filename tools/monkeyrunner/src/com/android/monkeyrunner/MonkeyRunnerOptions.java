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
package com.android.monkeyrunner;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonkeyRunnerOptions {
    private static final Logger LOG = Logger.getLogger(MonkeyRunnerOptions.class.getName());
    private static String DEFAULT_MONKEY_SERVER_ADDRESS = "127.0.0.1";
    private static int DEFAULT_MONKEY_PORT = 12345;

    private final int port;
    private final String hostname;
    private final File scriptFile;
    private final String backend;
    private final Collection<File> plugins;
    private final Collection<String> arguments;

    private MonkeyRunnerOptions(String hostname, int port, File scriptFile, String backend,
            Collection<File> plugins, Collection<String> arguments) {
        this.hostname = hostname;
        this.port = port;
        this.scriptFile = scriptFile;
        this.backend = backend;
        this.plugins = plugins;
        this.arguments = arguments;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public File getScriptFile() {
        return scriptFile;
    }

    public String getBackendName() {
        return backend;
    }

    public Collection<File> getPlugins() {
        return plugins;
    }

    public Collection<String> getArguments() {
        return arguments;
    }

    private static void printUsage(String message) {
        System.out.println(message);
        System.out.println("Usage: monkeyrunner [options] SCRIPT_FILE");
        System.out.println("");
        System.out.println("    -s      MonkeyServer IP Address.");
        System.out.println("    -p      MonkeyServer TCP Port.");
        System.out.println("    -v      MonkeyServer Logging level (ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF)");
        System.out.println("");
        System.out.println("");
    }

    /**
     * Process the command-line options
     *
     * @return the parsed options, or null if there was an error.
     */
    public static MonkeyRunnerOptions processOptions(String[] args) {
        // parse command line parameters.
        int index = 0;

        String hostname = DEFAULT_MONKEY_SERVER_ADDRESS;
        File scriptFile = null;
        int port = DEFAULT_MONKEY_PORT;
        String backend = "adb";

        ImmutableList.Builder<File> pluginListBuilder = ImmutableList.builder();
        ImmutableList.Builder<String> argumentBuilder = ImmutableList.builder();
        while (index < args.length) {
            String argument = args[index++];

            if ("-s".equals(argument)) {
                if (index == args.length) {
                    printUsage("Missing Server after -s");
                    return null;
                }
                hostname = args[index++];

            } else if ("-p".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    printUsage("Missing Server port after -p");
                    return null;
                }
                port = Integer.parseInt(args[index++]);

            } else if ("-v".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    printUsage("Missing Log Level after -v");
                    return null;
                }

                Level level = Level.parse(args[index++]);
                LOG.setLevel(level);
                level = LOG.getLevel();
                System.out.println("Log level set to: " + level + "(" + level.intValue() + ").");
                System.out.println("Warning: Log levels below INFO(800) not working currently... parent issues");
            } else if ("-be".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    printUsage("Missing backend name after -be");
                    return null;
                }
                backend = args[index++];
            } else if ("-plugin".equals(argument)) {
                // quick check on the next argument.
                if (index == args.length) {
                    printUsage("Missing plugin path after -plugin");
                    return null;
                }
                File plugin = new File(args[index++]);
                if (!plugin.exists()) {
                    printUsage("Plugin file doesn't exist");
                    return null;
                }

                if (!plugin.canRead()) {
                    printUsage("Can't read plugin file");
                    return null;
                }

                pluginListBuilder.add(plugin);
            } else if (argument.startsWith("-") &&
                // Once we have the scriptfile, the rest of the arguments go to jython.
                scriptFile == null) {
                // we have an unrecognized argument.
                printUsage("Unrecognized argument: " + argument + ".");
                return null;
            } else {
                if (scriptFile == null) {
                    // get the filepath of the script to run.  This will be the last undashed argument.
                    scriptFile = new File(argument);
                    if (!scriptFile.exists()) {
                        printUsage("Can't open specified script file");
                        return null;
                    }
                    if (!scriptFile.canRead()) {
                        printUsage("Can't open specified script file");
                        return null;
                    }
                } else {
                    argumentBuilder.add(argument);
                }
            }
        };

        return new MonkeyRunnerOptions(hostname, port, scriptFile, backend,
                pluginListBuilder.build(), argumentBuilder.build());
    }
}
