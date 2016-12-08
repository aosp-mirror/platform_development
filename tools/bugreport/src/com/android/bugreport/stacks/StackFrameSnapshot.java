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

/**
 * Base class for the different types of stack frames.
 *
 * The type is indicated by frameType.
 */
public class StackFrameSnapshot {
    public static final int FRAME_TYPE_UNKNOWN = 0;
    public static final int FRAME_TYPE_NATIVE = 1;
    public static final int FRAME_TYPE_KERNEL = 2;
    public static final int FRAME_TYPE_JAVA = 3;

    public final int frameType;
    public String text;
    
    protected StackFrameSnapshot() {
        this.frameType = FRAME_TYPE_UNKNOWN;
    }

    protected StackFrameSnapshot(int frameType) {
        this.frameType = frameType;
    }

    protected StackFrameSnapshot(StackFrameSnapshot that) {
        this.frameType = that.frameType;
        this.text = that.text;
    }

    @Override
    public StackFrameSnapshot clone() {
        return new StackFrameSnapshot(this);
    }
}

