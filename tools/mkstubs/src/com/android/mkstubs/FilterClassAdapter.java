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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

/**
 * A class visitor that filters out all the referenced exclusions
 */
class FilterClassAdapter extends ClassAdapter {

    private final List<String> mExclusions;

    public FilterClassAdapter(ClassVisitor writer, List<String> exclusions) {
        super(writer);
        mExclusions = exclusions;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {

        // TODO filter super type
        // TODO filter interfaces 
        
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
        // exclude private fields
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return null;
        }
        
        // TODO filter on name

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

        // exclude private methods
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return null;
        }
        
        // TODO filter exceptions: error if filtered exception is being used

        // TODO filter on name; error if filtered desc or signatures is being used

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        
        // Filter on desc type
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // pass
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {

        // exclude private methods
        if ((access & Opcodes.ACC_PRIVATE) != 0) {
            return;
        }

        // TODO filter on name

        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // TODO Auto-generated method stub
    }

    @Override
    public void visitSource(String source, String debug) {
        // pass
    }
}
