/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import org.eclipse.swt.graphics.Color;

class Call implements TimeLineView.Block {
    
    // Values for bits within the mFlags field.
    private static final int METHOD_ACTION_MASK = 0x3;
    private static final int IS_RECURSIVE = 0x10;

    private int mThreadId;
    private int mFlags;
    MethodData mMethodData;
    
    /** 0-based thread-local start time */
    long mThreadStartTime;
    
    /**  global start time */
    long mGlobalStartTime;

    /** global end time */
    long mGlobalEndTime;
    
    private String mName;

    /**
     * This constructor is used for the root of a Call tree. The name is
     * the name of the corresponding thread. 
     */
    Call(String name, MethodData methodData) {
        mName = name;
        mMethodData = methodData;
    }

    Call() {
    }
    
    Call(int threadId, MethodData methodData, long time, int methodAction) {
        mThreadId = threadId;
        mMethodData = methodData;
        mThreadStartTime = time;
        mFlags = methodAction & METHOD_ACTION_MASK;
        mName = methodData.getProfileName();
    }
    
    public void set(int threadId, MethodData methodData, long time, int methodAction) {
        mThreadId = threadId;
        mMethodData = methodData;
        mThreadStartTime = time;
        mFlags = methodAction & METHOD_ACTION_MASK;
        mName = methodData.getProfileName();
    }

    public void updateName() {
        mName = mMethodData.getProfileName();
    }

    public double addWeight(int x, int y, double weight) {
        return mMethodData.addWeight(x, y, weight);
    }

    public void clearWeight() {
        mMethodData.clearWeight();
    }

    public long getStartTime() {
        return mGlobalStartTime;
    }

    public long getEndTime() {
        return mGlobalEndTime;
    }

    public Color getColor() {
        return mMethodData.getColor();
    }

    public void addExclusiveTime(long elapsed) {
        mMethodData.addElapsedExclusive(elapsed);
        if ((mFlags & IS_RECURSIVE) == 0) {
            mMethodData.addTopExclusive(elapsed);
        }
    }

    public void addInclusiveTime(long elapsed, Call parent) {
        boolean isRecursive = (mFlags & IS_RECURSIVE) != 0;
        mMethodData.addElapsedInclusive(elapsed, isRecursive, parent);
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    int getThreadId() {
        return mThreadId;
    }

    public MethodData getMethodData() {
        return mMethodData;
    }

    int getMethodAction() {
        return mFlags & METHOD_ACTION_MASK;
    }

    public void dump() {
        System.out.printf("%s [%d, %d]\n", mName, mGlobalStartTime, mGlobalEndTime);
    }

    public void setRecursive(boolean isRecursive) {
        if (isRecursive) {
            mFlags |= IS_RECURSIVE;
        } else {
            mFlags &= ~IS_RECURSIVE;
        }
    }

    public boolean isRecursive() {
        return (mFlags & IS_RECURSIVE) != 0;
    }
}
