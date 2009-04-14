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

import com.android.mkstubs.sourcer.JavaSourcer;
import com.android.mkstubs.sourcer.Output;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 */
class AsmGenerator {

    /**
     * Generate source for the stubbed classes, mostly for debug purposes.
     * @throws IOException 
     */
    public void generateSource(File baseDir,
            Map<String, ClassReader> classes,
            List<String> exclusions) throws IOException {
        
        for (Entry<String, ClassReader> entry : classes.entrySet()) {
            ClassReader cr = entry.getValue();
            
            String name = classNameToJavaPath(cr.getClassName());

            FileWriter fw = null;
            try {
                fw = createWriter(baseDir, name);
                dumpClass(fw, cr, exclusions);
            } finally {
                fw.close();
            }
        }
    }

    FileWriter createWriter(File baseDir, String name) throws IOException {
        File f = new File(baseDir, name);
        f.getParentFile().mkdirs();
        
        System.out.println("Writing " + f.getPath());
        
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
    void dumpClass(Writer fw, ClassReader cr, List<String> exclusions) {
        System.out.println("Dump " + cr.getClassName());
        
        ClassVisitor javaWriter = new JavaSourcer(new Output(fw));
        ClassVisitor filter = new FilterClassAdapter(javaWriter, exclusions);
        cr.accept(filter, 0 /*flags*/);
    }

}
