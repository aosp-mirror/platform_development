/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.apkbuilder;

import com.android.apkbuilder.internal.ApkBuilderImpl;

import java.io.FileNotFoundException;


/**
 * Command line APK builder with signing support.
 */
public final class ApkBuilder {

    public final static class WrongOptionException extends Exception {
        private static final long serialVersionUID = 1L;

        public WrongOptionException(String message) {
            super(message);
        }
    }

    public final static class ApkCreationException extends Exception {
        private static final long serialVersionUID = 1L;

        public ApkCreationException(String message) {
            super(message);
        }

        public ApkCreationException(Throwable throwable) {
            super(throwable);
        }

    }

    /**
     * Main method. This is meant to be called from the command line through an exec.
     * <p/>WARNING: this will call {@link System#exit(int)} if anything goes wrong.
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        try {
            new ApkBuilderImpl().run(args);
        } catch (WrongOptionException e) {
            printUsageAndQuit();
        } catch (FileNotFoundException e) {
            printAndExit(e.getMessage());
        } catch (ApkCreationException e) {
            printAndExit(e.getMessage());
        }
    }

    /**
     * API entry point similar to the {@link #main(String[])} method.
     * <p/>Unlike {@link #main(String[])}, this will not call {@link System#exit(int)} and instead
     * will throw exceptions.
     * @param args command line arguments.
     * @throws WrongOptionException if the command line arguments are incorrect.
     * @throws FileNotFoundException if a required file was not found.
     * @throws ApkCreationException if an error happened during the creation of the APK.
     */
    public static void createApk(String[] args) throws FileNotFoundException, WrongOptionException,
            ApkCreationException {
        new ApkBuilderImpl().run(args);
    }

    private static void printUsageAndQuit() {
        // 80 cols marker:  01234567890123456789012345678901234567890123456789012345678901234567890123456789
        System.err.println("A command line tool to package an Android application from various sources.");
        System.err.println("Usage: apkbuilder <out archive> [-v][-u][-storetype STORE_TYPE] [-z inputzip]");
        System.err.println("            [-f inputfile] [-rf input-folder] [-rj -input-path]");
        System.err.println("");
        System.err.println("    -v      Verbose.");
        System.err.println("    -u      Creates an unsigned package.");
        System.err.println("    -storetype Forces the KeyStore type. If ommited the default is used.");
        System.err.println("");
        System.err.println("    -z      Followed by the path to a zip archive.");
        System.err.println("            Adds the content of the application package.");
        System.err.println("");
        System.err.println("    -f      Followed by the path to a file.");
        System.err.println("            Adds the file to the application package.");
        System.err.println("");
        System.err.println("    -rf     Followed by the path to a source folder.");
        System.err.println("            Adds the java resources found in that folder to the application");
        System.err.println("            package, while keeping their path relative to the source folder.");
        System.err.println("");
        System.err.println("    -rj     Followed by the path to a jar file or a folder containing");
        System.err.println("            jar files.");
        System.err.println("            Adds the java resources found in the jar file(s) to the application");
        System.err.println("            package.");
        System.err.println("");
        System.err.println("    -nf     Followed by the root folder containing native libraries to");
        System.err.println("            include in the application package.");

        System.exit(1);
    }

    private static void printAndExit(String... messages) {
        for (String message : messages) {
            System.err.println(message);
        }
        System.exit(1);
    }
}
