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
 * A vm traces process snapshot.
 */
public class ProcessSnapshot implements Comparable<ProcessSnapshot> {
    public int pid;
    public String cmdLine;
    public String date;
    public ArrayList<ThreadSnapshot> threads = new ArrayList<ThreadSnapshot>();

    /**
     * Constructs an empty ProcessSnapshot;
     */
    public ProcessSnapshot() {
    }

    /**
     * Construct a deep copy of the ProcessSnapshot.
     */
    public ProcessSnapshot(ProcessSnapshot that) {
        this.pid = that.pid;
        this.cmdLine = that.cmdLine;
        this.date = that.date;
        final int N = that.threads.size();
        for (int i=0; i<N; i++) {
            this.threads.add(that.threads.get(i).clone());
        }
    }

    /**
     * Make a deep copy of the ProcessSnapshot.
     */
    @Override
    public ProcessSnapshot clone() {
        return new ProcessSnapshot(this);
    }

    /**
     * Compares the threads based on their tid.
     */
    public int compareTo(ProcessSnapshot that) {
        return this.pid - that.pid;
    }
    
    /**
     * Returns the first thread with the given name that's found, or null.
     */
    public ThreadSnapshot getThread(String name) {
        for (ThreadSnapshot thread: this.threads) {
            if (name.equals(thread.name)) {
                return thread;
            }
        }
        return null;
    }
    
    /**
     * Returns the first thread with the given name that's found, or null.
     */
    public ThreadSnapshot getThread(int tid) {
        for (ThreadSnapshot thread: this.threads) {
            if (tid == thread.tid) {
                return thread;
            }
        }
        return null;
    }
    /**
     * Returns the first thread with the given name that's found, or null.
     */
    public ThreadSnapshot getSysThread(int sysTid) {
        for (ThreadSnapshot thread: this.threads) {
            if (sysTid == thread.sysTid) {
                return thread;
            }
        }
        return null;
    }

}

