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
import java.util.ArrayList;
import java.util.Map;


/**
 * 
 */
public class Main {
    
    static class Params {
        private String mInputJarPath;
        private String mOutputJarPath;
        private ArrayList<String> mInclusions = new ArrayList<String>();
        private ArrayList<String> mExclusions = new ArrayList<String>();

        public Params(String inputJarPath, String outputJarPath) {
            mInputJarPath = inputJarPath;
            mOutputJarPath = outputJarPath;
        }
        
        public String getInputJarPath() {
            return mInputJarPath;
        }

        public String getOutputJarPath() {
            return mOutputJarPath;
        }

        public ArrayList<String> getExclusions() {
            return mExclusions;
        }
        
        public ArrayList<String> getInclusions() {
            return mInclusions;
        }
    }
    
    /**
     * @param args
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

    private Params processArgs(String[] args) throws IOException {
        
        if (args.length < 2) {
            usage();
        }

        Params p = new Params(args[0], args[1]);
        
        for (int i = 2; i < args.length; i++) {
            String s = args[i];
            if (s.startsWith("@")) {
                addStringsFromFile(p, s.substring(1));
            } else if (s.startsWith("-")) {
                p.getExclusions().add(s.substring(1));
            } else if (s.startsWith("+")) {
                p.getInclusions().add(s.substring(1));
            }
        }
        
        return p;
    }

    private void addStringsFromFile(Params p, String inputFile)
            throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(inputFile));
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                char mode = line.charAt(0);
                line = line.substring(1).trim();
                
                if (line.length() > 0) {
                    // Keep all class names in ASM path-like format, e.g. android/view/View
                    line = line.replace('.', '/');
                    if (mode == '-') {
                        p.getExclusions().add(line);
                    } else if (mode == '+') {
                        p.getInclusions().add(line);
                    }
                }
            }
        } finally {
            br.close();
        }
    }

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

    private void process(Params p) throws IOException {
        AsmAnalyzer aa = new AsmAnalyzer();
        Map<String, ClassReader> classes = aa.parseInputJar(p.getInputJarPath());
     
        aa.filter(classes, p.getInclusions(), p.getExclusions());

        // dump as Java source files, mostly for debugging
        SourceGenerator src_gen = new SourceGenerator();
        File dst_src_dir = new File(p.getOutputJarPath() + "_sources");
        dst_src_dir.mkdir();
        src_gen.generateSource(dst_src_dir, classes, p.getExclusions());
        
        // dump the stubbed jar
        StubGenerator stub_gen = new StubGenerator();
        File dst_jar = new File(p.getOutputJarPath());
        stub_gen.generateStubbedJar(dst_jar, classes, p.getExclusions());
    }
}
