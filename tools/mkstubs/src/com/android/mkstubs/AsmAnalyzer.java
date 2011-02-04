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

import com.android.mkstubs.Main.Logger;

import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Analyzes an input Jar to get all the relevant classes according to the given filter.
 * <p/>
 * This is mostly a helper extracted for convenience. Callers will want to use
 * {@link #parseInputJar(String)} followed by {@link #filter(Map, Filter)}.
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

    /**
     * Filters the set of classes. Removes all classes that should not be included in the
     * filter or that should be excluded. This modifies the map in-place.
     *
     * @param classes The in-out map of classes to examine and filter. The map is filtered
     *                in-place.
     * @param filter  A filter describing which classes to include and which ones to exclude.
     * @param log
     */
    void filter(Map<String, ClassReader> classes, Filter filter, Logger log) {

        Set<String> keys = classes.keySet();
        for(Iterator<String> it = keys.iterator(); it.hasNext(); ) {
            String key = it.next();

            // TODO: We *could* filter out all private classes here: classes.get(key).getAccess().

            // remove if we don't keep it
            if (!filter.accept(key)) {
                log.debug("- Remove class " + key);
                it.remove();
            }
        }
    }

}
