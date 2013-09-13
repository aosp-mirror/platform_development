/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.rmtypedefs;

import com.google.common.io.Files;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM4;

/**
 * Finds and deletes typedef annotation classes (and also warns if their
 * retention was wrong, such that uses embeds
 */
public class RmTypeDefs {

    private static final String ANNOTATION = "java/lang/annotation/Annotation";
    private static final String STRING_DEF = "android/annotation/StringDef";
    private static final String INT_DEF = "android/annotation/IntDef";
    private static final String INT_DEF_DESC = "L" + INT_DEF + ";";
    private static final String STRING_DEF_DESC = "L" + STRING_DEF + ";";
    private static final String RETENTION_DESC = "Ljava/lang/annotation/Retention;";
    private static final String RETENTION_POLICY_DESC = "Ljava/lang/annotation/RetentionPolicy;";
    private static final String SOURCE_RETENTION_VALUE = "SOURCE";

    private boolean mQuiet;
    private boolean mVerbose;
    private boolean mHaveError;
    private boolean mDryRun = true;

    public static void main(String[] args) {
        new RmTypeDefs().run(args);
    }

    private void run(String[] args) {
        if (args.length == 0) {
            usage(System.err);
            System.exit(1);
        }

        List<File> dirs = new ArrayList<File>();
        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                usage(System.out);
                return;
            } else if (arg.equals("-q") || arg.equals("--quiet") || arg.equals("--silent")) {
                mQuiet = true;
            } else if (arg.equals("-v") || arg.equals("--verbose")) {
                mVerbose = true;
            } else if (arg.equals("-n") || arg.equals("--dry-run")) {
                mDryRun = true;
            } else if (arg.startsWith("-")) {
                System.err.println("Unknown argument " + arg);
                usage(System.err);
                System.exit(1);

            } else {
                // Other arguments should be file names
                File file = new File(arg);
                if (file.exists()) {
                    dirs.add(file);
                } else {
                    System.err.println(file + " does not exist");
                    usage(System.err);
                    System.exit(1);
                }
            }
        }

        if (!mQuiet) {
            System.out.println("Deleting @IntDef and @StringDef annotation class files");
        }

        for (File dir : dirs) {
            find(dir);
        }

        System.exit(mHaveError ? -1 : 0);
    }

    private void find(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    find(f);
                }
            }
        } else if (file.isFile()) {
            String path = file.getPath();
            if (path.endsWith(".class")) {
                checkClass(file);
            } else if (path.endsWith(".jar")) {
                System.err.println(path + ": Warning: Encountered .jar file; .class files "
                        + "are not scanned and removed inside .jar files");
            }
        }
    }

    private void checkClass(File file) {
        try {
            byte[] bytes = Files.toByteArray(file);
            ClassReader classReader = new ClassReader(bytes);
            classReader.accept(new MyVisitor(file), 0);
        } catch (IOException e) {
            System.err.println("Could not read " + file + ": " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /**
     * Prints usage statement.
     */
    static void usage(PrintStream out) {
        out.println("Android TypeDef Remover 1.0");
        out.println("Copyright (C) 2013 The Android Open Source Project\n");
        out.println("Usage: rmtypedefs folder1 [folder2 [folder3...]]\n");
        out.println("Options:");
        out.println("  -h,--help                  show this message");
        out.println("  -q,--quiet                 quiet");
        out.println("  -v,--verbose               verbose");
        out.println("  -n,--dry-run               dry-run only, leaves files alone");
    }

    private class MyVisitor extends ClassVisitor {

        /** Class file name */
        private File mFile;

        /** Class name */
        private String mName;

        /** Is this class an annotation? */
        private boolean mAnnotation;

        /** Is this annotation a typedef? Only applies if {@link #mAnnotation} */
        private boolean mTypedef;

        /** Does the annotation have source retention? Only applies if {@link #mAnnotation} */
        private boolean mSourceRetention;

        public MyVisitor(File file) {
            super(ASM4);
            mFile = file;
        }

        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces) {
            mName = name;
            mAnnotation = interfaces != null && interfaces.length >= 1
                    && ANNOTATION.equals(interfaces[0]);

            // Special case: Also delete the actual @IntDef and @StringDef .class files.
            // These have class file retention
            mTypedef = name.equals(INT_DEF) || name.equals(STRING_DEF);
        }

        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            mTypedef = desc.equals(INT_DEF_DESC) || desc.equals(STRING_DEF_DESC);
            if (desc.equals(RETENTION_DESC)) {
                return new AnnotationVisitor(ASM4) {
                    public void visitEnum(String name, String desc, String value) {
                        if (desc.equals(RETENTION_POLICY_DESC)) {
                            mSourceRetention = SOURCE_RETENTION_VALUE.equals(value);
                        }
                    }
                };
            }
            return null;
        }

        public void visitEnd() {
            if (mAnnotation && mTypedef) {
                if (!mSourceRetention && !mName.equals(STRING_DEF) && !mName.equals(INT_DEF)) {
                    System.err.println(mFile + ": Warning: Annotation should be annotated "
                            + "with @Retention(RetentionPolicy.SOURCE)");
                    mHaveError = true;
                }
                if (mVerbose) {
                    if (mDryRun) {
                        System.out.println("Would delete " + mFile);
                    } else {
                        System.out.println("Deleting " + mFile);
                    }
                }
                if (!mDryRun) {
                    boolean deleted = mFile.delete();
                    if (!deleted) {
                        System.err.println("Could not delete " + mFile);
                        mHaveError = true;
                    }
                }
            }
        }
    }
}

