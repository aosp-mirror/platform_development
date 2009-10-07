/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.layoutopt.cli;

import com.android.layoutopt.uix.LayoutAnalyzer;
import com.android.layoutopt.uix.LayoutAnalysis;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

/**
 * Command line utility for the uix library.
 *
 * This is a simple CLI front-end for the uix library, used to
 * analyze and optimize Android layout files.
 */
public class Main {
    /**
     * Main entry point of the application.
     *
     * @param args One mandatory parameter, a path (absolute or relative)
     *             to an Android XML layout file
     */
    public static void main(String[] args) {
        Parameters p = checkParameters(args);
        if (!p.valid) {
            displayHelpMessage();
            exit();
        }

        analyzeFiles(p.files);
    }

    private static void analyzeFiles(File[] files) {
        LayoutAnalyzer analyzer = new LayoutAnalyzer();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".xml")) {
                analyze(analyzer, file);
            } else if (file.isDirectory()) {
                analyzeFiles(file.listFiles());
            }
        }
    }

    private static void analyze(LayoutAnalyzer analyzer, File file) {
        LayoutAnalysis analysis = analyzer.analyze(file);
        System.out.println(analysis.getName());
        for (LayoutAnalysis.Issue issue : analysis.getIssues()) {
            System.out.print(String.format("\t%d:%d ", issue.getStartLine(), issue.getEndLine()));
            System.out.println(issue.getDescription());
        }
    }

    /**
     * Exits the tool.
     */
    private static void exit() {
        System.exit(0);
    }

    /**
     * Displays this tool's help message on the standard output.
     */
    private static void displayHelpMessage() {
        System.out.println("usage: layoutopt <directories/files to analyze>");
    }

    /**
     * Builds a valid Parameters object. Parses the paramters if necessary
     * and checks for errors.
     *
     * @param args The parameters passed from the CLI.
     */
    private static Parameters checkParameters(String[] args) {
        Parameters p = new Parameters();

        if (args.length < 1) {
            p.valid = false;
        } else {
            List<File> files = new ArrayList<File>();
            for (String path : args) {
                File file = new File(path);
                if (file.exists() && (file.isDirectory() || file.getName().endsWith(".xml"))) {
                    files.add(file);
                }
            }
            p.files = files.toArray(new File[files.size()]);
            p.valid = true;
        }

        return p;
    }

    /**
     * Parameters parsed from the CLI.
     */
    private static class Parameters {
        /**
         * True if this list of parameters is valid, false otherwise.
         */
        boolean valid;

        /**
         * Paths (absolute or relative) to the files to be analyzed.
         */
        File[] files;
    }
}
