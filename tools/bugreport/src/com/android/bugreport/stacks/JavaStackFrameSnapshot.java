/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bugreport.stacks;

import java.util.ArrayList;

/**
 * A java stack frame within a thread.
 *
 * This includes both java and jni, which are indicated by the language
 * field being set to either LANGUAGE_JAVA or LANGUAGE_JNI.
 */
public class JavaStackFrameSnapshot extends StackFrameSnapshot {
    public static final int LANGUAGE_JAVA = 0;
    public static final int LANGUAGE_JNI = 1;

    public String packageName;
    public String className;
    public String methodName;
    public String sourceFile;
    public int sourceLine;
    public int language;
    public ArrayList<LockSnapshot> locks = new ArrayList<LockSnapshot>();
    
    public JavaStackFrameSnapshot() {
        super(FRAME_TYPE_JAVA);
    }
    
    public JavaStackFrameSnapshot(JavaStackFrameSnapshot that) {
        super(that);
        this.packageName = that.packageName;
        this.className = that.className;
        this.methodName = that.methodName;
        this.sourceFile = that.sourceFile;
        this.sourceLine = that.sourceLine;
        this.language = that.language;
        final int N = that.locks.size();
        for (int i=0; i<N; i++) {
            this.locks.add(that.locks.get(i).clone());
        }
    }

    @Override
    public JavaStackFrameSnapshot clone() {
        return new JavaStackFrameSnapshot(this);
    }

}

