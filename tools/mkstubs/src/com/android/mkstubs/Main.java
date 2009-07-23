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

package com.android.mkstubs;

import org.objectweb.asm.ClassReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;


/**
 * Main entry point of the MkStubs app.
 * <p/>
 * For workflow details, see {@link #process(Params)}.
 */
public class Main {
    
    /**
     * A struct-like class to hold the various input values (e.g. command-line args)
     */
    static class Params {
        private String mInputJarPath;
        private String mOutputJarPath;
        private Filter mFilter;
        
        public Params(String inputJarPath, String outputJarPath) {
            mInputJarPath = inputJarPath;
            mOutputJarPath = outputJarPath;
            mFilter = new Filter();
        }

        /** Returns the name of the input jar, where to read classes from. */
        public String getInputJarPath() {
            return mInputJarPath;
        }

        /** Returns the name of the output jar, where to write classes to. */
        public String getOutputJarPath() {
            return mOutputJarPath;
        }

        /** Returns the current instance of the filter, the include/exclude patterns. */
        public Filter getFilter() {
            return mFilter;
        }
    }
    
    /**
     * Main entry point. Processes arguments then performs the "real" work.
     */
    public static void main(String[] args) {

        Main m = new Main();
        try {
            Params p = m.processArgs(args);
            m.process(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Grabs command-line arguments.
     * The expected arguments are:
     * <ul>
     * <li> The filename of the input Jar.
     * <li> The filename of the output Jar.
     * <li> One or more include/exclude patterns or files containing these patterns.
     *      See {@link #addString(Params, String)} for syntax.
     * </ul>
     * @throws IOException on failure to read a pattern file.
     */
    private Params processArgs(String[] args) throws IOException {
        
        if (args.length < 2) {
            usage();
        }

        Params p = new Params(args[0], args[1]);
        
        for (int i = 2; i < args.length; i++) {
            addString(p, args[i]);
        }
        
        return p;
    }

    /**
     * Adds one pattern string to the current filter.
     * The syntax must be:
     * <ul>
     * <li> +full_include or +prefix_include*
     * <li> -full_exclude or -prefix_exclude*
     * <li> @filename
     * </ul>
     * The input string is trimmed so any space around the first letter (-/+/@) or
     * at the end is removed. Empty strings are ignored.
     * 
     * @param p The params which filters to edit.
     * @param s The string to examine.
     * @throws IOException
     */
    private void addString(Params p, String s) throws IOException {
        if (s == null) {
            return;
        }

        s = s.trim();

        if (s.length() < 2) {
            return;
        }
        
        char mode = s.charAt(0);
        s = s.substring(1).trim();

        if (mode == '@') {
            addStringsFromFile(p, s);
            
        } else if (mode == '-') {
            s = s.replace('.', '/');  // transform FQCN into ASM internal name
            if (s.endsWith("*")) {
                p.getFilter().getExcludePrefix().add(s.substring(0, s.length() - 1));
            } else {
                p.getFilter().getExcludeFull().add(s);
            }

        } else if (mode == '+') {
            s = s.replace('.', '/');  // transform FQCN into ASM internal name
            if (s.endsWith("*")) {
                p.getFilter().getIncludePrefix().add(s.substring(0, s.length() - 1));
            } else {
                p.getFilter().getIncludeFull().add(s);
            }
        }
    }

    /**
     * Adds all the filter strings from the given file.
     * 
     * @param p The params which filter to edit.
     * @param osFilePath The OS path to the file containing the patterns.
     * @throws IOException
     * 
     * @see #addString(Params, String)
     */
    private void addStringsFromFile(Params p, String osFilePath)
            throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(osFilePath));
            String line;
            while ((line = br.readLine()) != null) {
                addString(p, line);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    /**
     * Prints some help to stdout.
     */
    private void usage() {
        System.out.println("Usage: mkstub input.jar output.jar [excluded-class @excluded-classes-file ...]");

        System.out.println("Include syntax:\n" +
                "+com.package.* : whole package, with glob\n" +
                "+com.package.Class[$Inner] or ...Class*: whole classes with optional glob\n" +
                "Inclusion is not supported at method/field level.\n\n");

        System.out.println("Exclude syntax:\n" +
        		"-com.package.* : whole package, with glob\n" +
        		"-com.package.Class[$Inner] or ...Class*: whole classes with optional glob\n" +
        		"-com.package.Class#method: whole method or field\n" +
                "-com.package.Class#method(IILjava/lang/String;)V: specific method with signature.\n\n");
        System.exit(1);
    }

    /**
     * Performs the main workflow of this app:
     * <ul>
     * <li> Read the input Jar to get all its classes.
     * <li> Filter out all classes that should not be included or that should be excluded.
     * <li> Goes thru the classes, filters methods/fields and generate their source
     *      in a directory called "&lt;outpath_jar_path&gt;_sources"
     * <li> Does the same filtering on the classes but this time generates the real stubbed
     *      output jar.
     * </ul>
     */
    private void process(Params p) throws IOException {
        AsmAnalyzer aa = new AsmAnalyzer();
        Map<String, ClassReader> classes = aa.parseInputJar(p.getInputJarPath());

        System.out.println(String.format("Classes loaded: %d", classes.size()));
        
        aa.filter(classes, p.getFilter());

        System.out.println(String.format("Classes filtered: %d", classes.size()));

        // dump as Java source files, mostly for debugging
        SourceGenerator src_gen = new SourceGenerator();
        File dst_src_dir = new File(p.getOutputJarPath() + "_sources");
        dst_src_dir.mkdir();
        src_gen.generateSource(dst_src_dir, classes, p.getFilter());
        
        // dump the stubbed jar
        StubGenerator stub_gen = new StubGenerator();
        File dst_jar = new File(p.getOutputJarPath());
        stub_gen.generateStubbedJar(dst_jar, classes, p.getFilter());
    }
}
