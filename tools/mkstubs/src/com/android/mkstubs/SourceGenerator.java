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
import com.android.mkstubs.sourcer.ClassSourcer;
import com.android.mkstubs.sourcer.Output;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Given a set of already filtered classes, this filters out all private members and then
 * generates the Java source for the remaining classes.
 * <p/>
 * This is an helper extracted for convenience. Callers just need to use
 * {@link #generateSource(File, Map, Filter)}.
 */
class SourceGenerator {

    private Logger mLog;

    public SourceGenerator(Logger log) {
        mLog = log;
    }

    /**
     * Generate source for the stubbed classes, mostly for debug purposes.
     * @throws IOException
     */
    public void generateSource(File baseDir,
            Map<String, ClassReader> classes,
            Filter filter) throws IOException {

        for (Entry<String, ClassReader> entry : classes.entrySet()) {
            ClassReader cr = entry.getValue();

            String name = classNameToJavaPath(cr.getClassName());

            FileWriter fw = null;
            try {
                fw = createWriter(baseDir, name);
                visitClassSource(fw, cr, filter);
            } finally {
                fw.close();
            }
        }
    }

    FileWriter createWriter(File baseDir, String name) throws IOException {
        File f = new File(baseDir, name);
        f.getParentFile().mkdirs();

        mLog.debug("Writing " + f.getPath());

        return new FileWriter(f);
    }

    /**
     * Utility method that converts a fully qualified java name into a JAR entry path
     * e.g. for the input "android.view.View" it returns "android/view/View.java"
     */
    String classNameToJavaPath(String className) {
        return className.replace('.', '/').concat(".java");
    }

    /**
     * Generate a source equivalent to the stubbed version of the class reader,
     * minus all exclusions
     */
    void visitClassSource(Writer fw, ClassReader cr, Filter filter) {
        mLog.debug("Dump " + cr.getClassName());

        ClassVisitor javaWriter = new ClassSourcer(new Output(fw));
        ClassVisitor classFilter = new FilterClassAdapter(javaWriter, filter, mLog);
        cr.accept(classFilter, 0 /*flags*/);
    }

}
