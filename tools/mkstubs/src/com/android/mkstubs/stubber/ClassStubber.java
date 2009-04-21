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

package com.android.mkstubs.stubber;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A class visitor that generates stubs for all methods of the visited class.
 * Everything else is passed as-is.
 */
public class ClassStubber extends ClassAdapter {

    public ClassStubber(ClassVisitor cv) {
        super(cv);
    }

    @Override
    public void visit(int version, int access,
            String name,
            String signature,
            String superName,
            String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
    }
    
    @Override
    public void visitEnd() {
        super.visitEnd();
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return super.visitAnnotation(desc, visible);
    }
    
    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }
    
    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        MethodVisitor mw = super.visitMethod(access, name, desc, signature, exceptions);
        return new MethodStubber(mw, access, name, desc, signature, exceptions);
    }
    
    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
            Object value) {
        return super.visitField(access, name, desc, signature, value);
    }
    
    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }
    
    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        super.visitOuterClass(owner, name, desc);
    }
    
    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
    }
}
