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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A method visitor that generates a code stub for the visited method.
 * <p/>
 * Annotations and parameters are passed as-is.
 * All other code is replaced by the following:
 * <pre>throw new RuntimeException("stub");</pre>
 * Note that constructors rewritten this way will probably fail with the runtime bytecode
 * verifier since no call to <code>super</code> is generated.
 */
public class MethodStubber extends MethodVisitor {

    public MethodStubber(MethodVisitor mw,
            int access, String name, String desc, String signature, String[] exceptions) {
        super(Opcodes.ASM4, mw);
    }

    @Override
    public void visitCode() {
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(36, l0);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("stub");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,          // opcode
                "java/lang/RuntimeException",   // owner
                "<init>",                       // name
                "(Ljava/lang/String;)V");       // desc
        mv.visitInsn(Opcodes.ATHROW);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable(
                "this",                                         // name
                "Lcom/android/mkstubs/stubber/MethodStubber;",  // desc
                null,                                           // signature
                l0,                                             // label start
                l1,                                             // label end
                0);                                             // index
        mv.visitMaxs(3, 1); // maxStack, maxLocals
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
    public AnnotationVisitor visitAnnotationDefault() {
        return super.visitAnnotationDefault();
    }

    @Override
    public void visitAttribute(Attribute attr) {
        super.visitAttribute(attr);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    // -- stuff that gets skipped

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        // skip
    }

    @Override
    public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
        // skip
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        // skip
    }

    @Override
    public void visitInsn(int opcode) {
        // skip
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        // skip
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // skip
    }

    @Override
    public void visitLabel(Label label) {
        // skip
    }

    @Override
    public void visitLdcInsn(Object cst) {
        // skip
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        // skip
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        // skip
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        // skip
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // skip
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // skip
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        // skip
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        // skip
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        // skip
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // skip
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        // skip
    }
}
