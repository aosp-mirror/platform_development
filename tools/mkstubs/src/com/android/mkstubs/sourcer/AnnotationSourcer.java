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
import org.objectweb.asm.Opcodes;

/**
 * An annotation visitor that generates Java source for an annotation.
 */
class AnnotationSourcer extends AnnotationVisitor {

    private final String mOpenChar;
    private final String mCloseChar;
    private final Output mOutput;
    private boolean mNeedClose;

    public AnnotationSourcer(Output output) {
        this(output, false /*isArray*/);
    }

    public AnnotationSourcer(Output output, boolean isArray) {
        super(Opcodes.ASM4);
        mOutput = output;
        mOpenChar = isArray ? "[" : "(";
        mCloseChar = isArray ? "]" : ")";
    }

    @Override
    public void visit(String name, Object value) {
        startOpen();

        if (name != null) {
            mOutput.write("%s=", name);
        }
        if (value != null) {
            mOutput.write(name.toString());
        }
    }

    private void startOpen() {
        if (!mNeedClose) {
            mNeedClose = true;
            mOutput.write(mOpenChar);
        }
    }

    @Override
    public void visitEnd() {
        if (mNeedClose) {
            mOutput.write(mCloseChar);
        }
        mOutput.write("\n");
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        startOpen();

        mOutput.write("@%s", name);
        return this;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        startOpen();
        return new AnnotationSourcer(mOutput, true /*isArray*/);
    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        mOutput.write("/* annotation enum not supported: %s */\n", name);
    }

}
