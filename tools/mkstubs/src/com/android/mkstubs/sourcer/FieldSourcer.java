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
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;

/**
 * A field visitor that generates Java source defining a field.
 */
class FieldSourcer extends FieldVisitor {

    private final Output mOutput;
    private final int mAccess;
    private final String mName;
    private final String mDesc;
    private final String mSignature;

    public FieldSourcer(Output output, int access, String name, String desc, String signature) {
        super(Opcodes.ASM4);
        mOutput = output;
        mAccess = access;
        mName = name;
        mDesc = desc;
        mSignature = signature;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        mOutput.write("@%s", desc);
        return new AnnotationSourcer(mOutput);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        mOutput.write("%s /* non-standard attribute */ ", attr.type);
    }

    @Override
    public void visitEnd() {
        // Need to write type and field name after the annotations and attributes.

        AccessSourcer as = new AccessSourcer(mOutput);
        as.write(mAccess, AccessSourcer.IS_FIELD);

        if (mSignature == null) {
            mOutput.write(" %s", Type.getType(mDesc).getClassName());
        } else {
            mOutput.write(" ");
            SignatureReader sigReader = new SignatureReader(mSignature);
            SignatureSourcer sigSourcer = new SignatureSourcer();
            sigReader.acceptType(sigSourcer);
            mOutput.write(sigSourcer.toString());
        }

        mOutput.write(" %s", mName);

        mOutput.write(";\n");
    }

}
