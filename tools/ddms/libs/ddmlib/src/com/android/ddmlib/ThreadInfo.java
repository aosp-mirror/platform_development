/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.ddmlib;

/**
 * Holds a thread information.
 */
public final class ThreadInfo implements IStackTraceInfo {
    private int mThreadId;
    private String mThreadName;
    private int mStatus;
    private int mTid;
    private int mUtime;
    private int mStime;
    private boolean mIsDaemon;
    private StackTraceElement[] mTrace;
    private long mTraceTime;

    // priority?
    // total CPU used?
    // method at top of stack?

    /**
     * Construct with basic identification.
     */
    ThreadInfo(int threadId, String threadName) {
        mThreadId = threadId;
        mThreadName = threadName;

        mStatus = -1;
        //mTid = mUtime = mStime = 0;
        //mIsDaemon = false;
    }

    /**
     * Set with the values we get from a THST chunk.
     */
    void updateThread(int status, int tid, int utime, int stime, boolean isDaemon) {

        mStatus = status;
        mTid = tid;
        mUtime = utime;
        mStime = stime;
        mIsDaemon = isDaemon;
    }
    
    /**
     * Sets the stack call of the thread.
     * @param trace stackcall information.
     */
    void setStackCall(StackTraceElement[] trace) {
        mTrace = trace;
        mTraceTime = System.currentTimeMillis();
    }

    /**
     * Returns the thread's ID.
     */
    public int getThreadId() {
        return mThreadId;
    }

    /**
     * Returns the thread's name.
     */
    public String getThreadName() {
        return mThreadName;
    }
    
    void setThreadName(String name) {
        mThreadName = name;
    }

    /**
     * Returns the system tid.
     */
    public int getTid() {
        return mTid;
    }

    /**
     * Returns the VM thread status.
     */
    public int getStatus() {
        return mStatus;
    }

    /**
     * Returns the cumulative user time.
     */
    public int getUtime() {
        return mUtime;
    }

    /**
     * Returns the cumulative system time.
     */
    public int getStime() {
        return mStime;
    }

    /**
     * Returns whether this is a daemon thread.
     */
    public boolean isDaemon() {
        return mIsDaemon;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.IStackTraceInfo#getStackTrace()
     */
    public StackTraceElement[] getStackTrace() {
        return mTrace;
    }

    /**
     * Returns the approximate time of the stacktrace data.
     * @see #getStackTrace()
     */
    public long getStackCallTime() {
        return mTraceTime;
    }
}

