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
 * A native (C/C++) stack frame inside a thread.
 */
public class NativeStackFrameSnapshot extends StackFrameSnapshot {

    public String library;
    public String symbol;
    public int offset;
    
    public NativeStackFrameSnapshot() {
        super(FRAME_TYPE_NATIVE);
    }

    @Override
    public NativeStackFrameSnapshot clone() {
        final NativeStackFrameSnapshot that = new NativeStackFrameSnapshot();
        that.library = this.library;
        that.symbol = this.symbol;
        that.offset = this.offset;
        return that;
    }
}

