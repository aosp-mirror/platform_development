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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 
 */
class AsmAnalyzer {

    /**
     * Parses a JAR file and returns a list of all classes founds using a map
     * class name => ASM ClassReader. Class names are in the form "android.view.View".
     */
    Map<String,ClassReader> parseInputJar(String inputJarPath) throws IOException {
        TreeMap<String, ClassReader> classes = new TreeMap<String, ClassReader>();

        ZipFile zip = new ZipFile(inputJarPath);
        Enumeration<? extends ZipEntry> entries = zip.entries();
        ZipEntry entry;
        while (entries.hasMoreElements()) {
            entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                ClassReader cr = new ClassReader(zip.getInputStream(entry));
                String className = classReaderToAsmName(cr);
                classes.put(className, cr);
            }
        }
        
        return classes;
    }

    /**
     * Utility that returns the fully qualified ASM class name for a ClassReader.
     * E.g. it returns something like android/view/View.
     */
    static String classReaderToAsmName(ClassReader classReader) {
        if (classReader == null) {
            return null;
        } else {
            return classReader.getClassName();
        }
    }

    public void filter(
            Map<String, ClassReader> classes,
            ArrayList<String> inclusions,
            ArrayList<String> exclusions) {

        ArrayList<String> inPrefix = new ArrayList<String>();
        HashSet  <String> inFull   = new HashSet  <String>();
        ArrayList<String> exPrefix = new ArrayList<String>();
        HashSet  <String> exFull   = new HashSet  <String>();
        
        for (String in : inclusions) {
            if (in.endsWith("*")) {
                inPrefix.add(in.substring(0, in.length() - 1));
            } else {
                inFull.add(in);
            }
        }
        
        for (String ex : exclusions) {
            if (ex.endsWith("*")) {
                exPrefix.add(ex.substring(0, ex.length() - 1));
            } else {
                exFull.add(ex);
            }
        }
        
        
        Set<String> keys = classes.keySet();
        for(Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String key = it.next();

            
            // Check if it can be included.
            boolean keep = inFull.contains(key);
            if (!keep) {
                // Check for a prefix inclusion
                for (String prefix : inPrefix) {
                    if (key.startsWith(prefix)) {
                        keep = true;
                        break;
                    }
                }
            }
            
            if (keep) {
                // check for a full exclusion
                keep = !exFull.contains(key);
            }
            if (keep) {
                // or check for prefix exclusion
                for (String prefix : exPrefix) {
                    if (key.startsWith(prefix)) {
                        keep = false;
                        break;
                    }
                }
            }
            
            // remove if we don't keep it
            if (!keep) {
                System.out.println("- Remove class " + key);
                it.remove();
            }
        }
    }
    
}
