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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A class visitor that filters out all members (fields, methods and inner classes) that are
 * either private, default-access or rejected by the {@link Filter}.
 */
class FilterClassAdapter extends ClassVisitor {

    private final Logger mLog;
    private final Filter mFilter;
    private String mClassName;

    public FilterClassAdapter(ClassVisitor writer, Filter filter, Logger log) {
        super(Opcodes.ASM4, writer);
        mFilter = filter;
        mLog = log;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {

        mClassName = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
    }

    /**
     * Visits a field.
     *
     * {@inheritDoc}
     *
     * Examples:
     * name = mArg
     * desc = Ljava/Lang/String;
     * signature = null (not a template) or template type
     */
    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        // only accept public/protected fields
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }

        // filter on field name
        String filterName = String.format("%s#%s", mClassName, name);

        if (!mFilter.accept(filterName)) {
            mLog.debug("- Remove field " + filterName);
            return null;
        }

        // TODO we should produce an error if a filtered desc/signature is being used.

        return super.visitField(access, name, desc, signature, value);
    }

    /**
     * Visits a method.
     *
     * {@inheritDoc}
     *
     * Examples:
     * name = <init>
     * desc = ()V
     * signature = null (not a template) or template type
     */
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {

        // only accept public/protected methods
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return null;
        }

        // filter on method name using the non-generic descriptor
        String filterName = String.format("%s#%s%s", mClassName, name, desc);

        if (!mFilter.accept(filterName)) {
            mLog.debug("- Remove method " + filterName);
            return null;
        }

        // filter on method name using the generic signature
        if (signature != null) {
            filterName = String.format("%s#%s%s", mClassName, name, signature);

            if (!mFilter.accept(filterName)) {
                mLog.debug("- Remove method " + filterName);
                return null;
            }
        }

        // TODO we should produce an error if a filtered desc/signature/exception is being used.

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

        // TODO produce an error if a filtered annotation type is being used
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // pass
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

        // only accept public/protected inner classes
        if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
            return;
        }

        // filter on name
        if (!mFilter.accept(name)) {
            return;
        }

        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // pass
    }

    @Override
    public void visitSource(String source, String debug) {
        // pass
    }
}
