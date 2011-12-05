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
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

import java.util.ArrayList;

/**
 * A method visitor that generates the Java source for a whole method.
 */
class MethodSourcer extends MethodVisitor {

    private final Output mOutput;
    private final int mAccess;
    private final String mClassName;
    private final String mName;
    private final String mDesc;
    private final String mSignature;
    private final String[] mExceptions;
    private boolean mNeedDeclaration;
    private boolean mIsConstructor;

    public MethodSourcer(Output output, String className, int access, String name,
            String desc, String signature, String[] exceptions) {
        super(Opcodes.ASM4);
        mOutput = output;
        mClassName = className;
        mAccess = access;
        mName = name;
        mDesc = desc;
        mSignature = signature;
        mExceptions = exceptions;

        mNeedDeclaration = true;
        mIsConstructor = "<init>".equals(name);
    }

    private void writeHeader() {
        if (!mNeedDeclaration) {
            return;
        }

        AccessSourcer as = new AccessSourcer(mOutput);
        as.write(mAccess, AccessSourcer.IS_METHOD);

        // preprocess the signature to get the return type and the arguments
        SignatureSourcer sigSourcer = null;
        if (mSignature != null) {
            SignatureReader sigReader = new SignatureReader(mSignature);
            sigSourcer = new SignatureSourcer();
            sigReader.accept(sigSourcer);

            if (sigSourcer.hasFormalsContent()) {
                // dump formal template parameter definitions
                mOutput.write(" %s", sigSourcer.formalsToString());
            }
        }

        // output return type (constructor have no return type)
        if (!mIsConstructor) {
            // The signature overrides desc, if present
            if (sigSourcer == null || sigSourcer.getReturnType() == null) {
                mOutput.write(" %s", Type.getReturnType(mDesc).getClassName());

            } else {
                mOutput.write(" %s", sigSourcer.getReturnType().toString());
            }
        }

        // output name
        mOutput.write(" %s(", mIsConstructor ? mClassName : mName);

        // output arguments. The signature overrides desc, if present
        if (mSignature == null) {
            Type[] types = Type.getArgumentTypes(mDesc);

            for(int i = 0; i < types.length; i++) {
                if (i > 0) {
                    mOutput.write(", ");
                }
                mOutput.write("%s arg%d", types[i].getClassName(), i);
            }
        } else {
            ArrayList<SignatureSourcer> params = sigSourcer.getParameters();

            for(int i = 0; i < params.size(); i++) {
                if (i > 0) {
                    mOutput.write(", ");
                }
                mOutput.write("%s arg%d", params.get(i).toString(), i);
            }
        }
        mOutput.write(")");

        // output throwable exceptions
        if (mExceptions != null && mExceptions.length > 0) {
            mOutput.write(" throws ");

            for (int i = 0; i < mExceptions.length; i++) {
                if (i > 0) {
                    mOutput.write(", ");
                }
                mOutput.write(mExceptions[i].replace('/', '.'));
            }
        }

        mOutput.write(" {\n");

        mNeedDeclaration = false;
    }

    @Override
    public void visitCode() {
        writeHeader();

        // write the stub itself
        mOutput.write("throw new RuntimeException(\"Stub\");");
    }

    @Override
    public void visitEnd() {
        writeHeader();
        mOutput.write("\n}\n");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        mOutput.write("@%s", desc);
        return new AnnotationSourcer(mOutput);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        // pass
        return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
        mOutput.write("%s /* non-standard method attribute */ ", attr.type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        // pass
    }

    @Override
    public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
        // pass
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        // pass
    }

    @Override
    public void visitInsn(int opcode) {
        // pass
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        // pass
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // pass
    }

    @Override
    public void visitLabel(Label label) {
        // pass
    }

    @Override
    public void visitLdcInsn(Object cst) {
        // pass
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        // pass
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature,
            Label start, Label end, int index) {
        // pass
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        // pass
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        // pass
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        // pass
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        // pass
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        // pass
        return null;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
        // pass
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        // pass
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // pass
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        // pass
    }

}
