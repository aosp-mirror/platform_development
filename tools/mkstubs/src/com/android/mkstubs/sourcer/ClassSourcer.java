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

package com.android.mkstubs.sourcer;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureReader;

/**
 * A class visitor that writes a java source.
 */
public class ClassSourcer extends ClassVisitor {

    private final Output mOutput;
    private final AccessSourcer mAccessSourcer;
    private String mClassName;

    public ClassSourcer(Output output) {
        super(Opcodes.ASM4);
        mOutput = output;
        mAccessSourcer = new AccessSourcer(mOutput);
    }

    /* Examples:
     * name = com/foo/MyClass
     * signature = null (if not generic)
     * superName = java/lang/Object
     * interfaces = [ java/lang/Runnable ... ]
     */
    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {

        String pkg = name.substring(0, name.lastIndexOf('/')).replace('/', '.');
        mClassName = name.substring(name.lastIndexOf('/') + 1);

        mOutput.write("package %s;\n", pkg);

        // dump access keywords. Note: do not dump "super" here
        mAccessSourcer.write(access & ~Opcodes.ACC_SUPER, AccessSourcer.IS_CLASS);

        // write class name
        mOutput.write(" class %s", mClassName);

        if (signature != null) {
            // write template formal definition and super type
            SignatureReader sigReader = new SignatureReader(signature);
            SignatureSourcer sigSourcer = new SignatureSourcer();
            sigReader.accept(sigSourcer);

            if (sigSourcer.hasFormalsContent()) {
                mOutput.write(sigSourcer.formalsToString());
            }

            mOutput.write(" extends %s", sigSourcer.getSuperClass().toString());

        } else {
            // write non-generic super type
            mOutput.write(" extends %s", superName.replace('/', '.'));
        }

        // write interfaces defined, if any
        if (interfaces != null && interfaces.length > 0) {
            mOutput.write(" implements ");
            boolean need_sep = false;
            for (String i : interfaces) {
                if (need_sep) {
                    mOutput.write(", ");
                }
                mOutput.write(i.replace('/', '.'));
                need_sep = true;
            }
        }

        // open class body
        mOutput.write(" {\n");
    }

    @Override
    public void visitEnd() {
        mOutput.write("}\n");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        mOutput.write("@%s", desc);
        return new AnnotationSourcer(mOutput);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        mOutput.write("%s /* non-standard class attribute */ ", attr.type);
    }


    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
            Object value) {
        // skip synthetic fields
        if ((access & Opcodes.ACC_SYNTHETIC) != 0) {
            return null;
        }

        return new FieldSourcer(mOutput, access, name, desc, signature);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {

        // Visit the method and dump its stub.
        return new MethodSourcer(mOutput, mClassName, access, name, desc, signature, exceptions);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        // Skip inner classes. This just indicates there's an inner class definition but
        // they are visited at the top level as separate classes.
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // Skip outer classes.
    }

    @Override
    public void visitSource(String source, String debug) {
        // Skip source information.
    }

}
