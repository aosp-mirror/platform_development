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

import org.objectweb.asm.Opcodes;

/**
 * Source generator for the access fields of methods, fields and classes.
 * <p/>
 * Given an integer access field and a type ({@link #IS_CLASS}, {@link #IS_FIELD} or
 * {@link #IS_METHOD}), the {@link #write(int, int)} method can generate a string
 * desribing the access modifiers for a Java source. 
 */
class AccessSourcer {

    private final Output mOutput;

    public static int IS_CLASS  = 1;
    public static int IS_FIELD  = 2;
    public static int IS_METHOD = 4;
    
    private enum Flag {
        ACC_PUBLIC(Opcodes.ACC_PUBLIC               , IS_CLASS | IS_FIELD | IS_METHOD),
        ACC_PRIVATE(Opcodes.ACC_PRIVATE             , IS_CLASS | IS_FIELD | IS_METHOD),
        ACC_PROTECTED(Opcodes.ACC_PROTECTED         , IS_CLASS | IS_FIELD | IS_METHOD),
        ACC_STATIC(Opcodes.ACC_STATIC               , IS_FIELD | IS_METHOD),
        ACC_FINAL(Opcodes.ACC_FINAL                 , IS_CLASS | IS_FIELD | IS_METHOD),
        ACC_SUPER(Opcodes.ACC_SUPER                 , IS_CLASS),
        ACC_SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED   , IS_METHOD),
        ACC_VOLATILE(Opcodes.ACC_VOLATILE           , IS_FIELD),
        ACC_BRIDGE(Opcodes.ACC_BRIDGE               , IS_METHOD),
        ACC_VARARGS(Opcodes.ACC_VARARGS             , IS_METHOD),
        ACC_TRANSIENT(Opcodes.ACC_TRANSIENT         , IS_FIELD),
        ACC_NATIVE(Opcodes.ACC_NATIVE               , IS_METHOD),
        ACC_INTERFACE(Opcodes.ACC_INTERFACE         , IS_CLASS),
        ACC_ABSTRACT(Opcodes.ACC_ABSTRACT           , IS_CLASS | IS_METHOD),
        ACC_STRICT(Opcodes.ACC_STRICT               , IS_METHOD),
        ACC_SYNTHETIC(Opcodes.ACC_SYNTHETIC         , IS_CLASS | IS_FIELD | IS_METHOD),
        ACC_ANNOTATION(Opcodes.ACC_ANNOTATION       , IS_CLASS),
        ACC_ENUM(Opcodes.ACC_ENUM                   , IS_CLASS),
        ACC_DEPRECATED(Opcodes.ACC_DEPRECATED       , IS_CLASS | IS_FIELD | IS_METHOD)
        ;
        
        private final int mValue;
        private final int mFilter;

        private Flag(int value, int filter) {
            mValue = value;
            mFilter = filter;
        }
        
        public int getValue() {
            return mValue;
        }
        
        public int getFilter() {
            return mFilter;
        }
        
        /** Transforms "ACC_PUBLIC" into "public" */
        @Override
        public String toString() {
            return super.toString().substring(4).toLowerCase();
        }
    }

    
    public AccessSourcer(Output output) {
        mOutput = output;
    }

    /**
     * Generates a list of access keywords, e.g. "public final".
     * <p/>
     * It is up to the caller to filter extra keywords that should not be generated,
     * e.g. {@link Flag#ACC_SYNTHETIC}.
     *  
     * @param access The access mode, e.g. 33 or 18
     * @param filter One of {@link #IS_CLASS}, {@link #IS_FIELD} or {@link #IS_METHOD}, which
     *        indicates the validity context.
     */
    public void write(int access, int filter) {

        boolean need_sep = false;
        
        for (Flag f : Flag.values()) {
            if ((f.getFilter() & filter) != 0 && (access & f.getValue()) != 0) {
                if (need_sep) {
                    mOutput.write(" ");
                }
                mOutput.write(f.toString());
                need_sep = true;
            }
        }
 
    }
}
