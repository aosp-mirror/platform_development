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

import com.android.bugreport.util.ArgParser;

import java.io.File;

/**
 * Class to encapsulate the command line arguments.
 */
public class Options {
    /**
     * The original raw args array.
     */
    public String[] args;

    /**
     * If there was an error parsing, the index into the args array that
     * had the error.
     *
     * Will be -1 if there was no error.
     */
    public int errorIndex;

    /**
     * Human readable description of the problem encountered while parsing.
     *
     * Will be null if there was no error.
     */
    public String errorText;

    /**
     * The bugreport file to parse.
     */
    public File bugreport;

    /**
     * The monkey log file to parse.
     *
     * Will be used instead of searching the logcat for the problem.
     */
    public File monkey;

    /**
     * The logcat file to parse.
     *
     * Will be used instead of the "SYSTEM LOG" section of the bugreport.
     */
    public File logcat;

    /**
     * The html file to output.
     */
    public File html;

    /**
     * Parse the arguments.
     *
     * Always returns an Options object.  It will either be a correctly formed one
     * with all the arguments, or one with nothing set except for the args, errorText
     * and errorIndex fields.
     */
    public static Options parseArgs(String[] args) {
        final Options result = new Options(args);

        String flag;
        final ArgParser argParser = new ArgParser(args);
        while ((flag = argParser.nextFlag()) != null) {
            if ("--monkey".equals(flag)) {
                if (result.monkey != null || !argParser.hasData(1)) {
                    return new Options(args, argParser.pos(),
                            "--monkey flag requires an argument");
                }
                result.monkey = new File(argParser.nextData());
            } else if ("--html".equals(flag)) {
                if (result.html != null || !argParser.hasData(1)) {
                    return new Options(args, argParser.pos(),
                            "--html flag requires an argument");
                }
                result.html = new File(argParser.nextData());
            } else if ("--logcat".equals(flag)) {
                if (result.logcat != null || !argParser.hasData(1)) {
                    return new Options(args, argParser.pos(),
                            "--logcat flag requires an argument");
                }
                result.logcat = new File(argParser.nextData());
            } else {
                return new Options(args, argParser.pos(),
                        "Unknown flag: " + flag);
            }
        }
        if ((!argParser.hasData(1)) || argParser.remaining() != 1) {
            return new Options(args, argParser.pos(),
                    "bugreport file name required");
        }
        result.bugreport = new File(argParser.nextData());

        return result;
    }

    /**
     * Construct a "successful" Options object.
     */
    private Options(String[] args) {
        this.args = args;
        this.errorIndex = -1;
    }

    /**
     * Construct an error Options object.
     */
    private Options(String[] args, int errorIndex, String errorText) {
        this.args = args;
        this.errorIndex = errorIndex;
        this.errorText = errorText;
    }
}



