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
import java.util.HashMap;

/**
 * One thread in a vm traces snapshot.
 */
public class ThreadSnapshot implements Comparable<ThreadSnapshot> {
    public static final int TYPE_UNMANAGED = 0;
    public static final int TYPE_MANAGED = 1;

    public int type;
    public String name;
    public String daemon;
    public int priority;
    public int tid = -1;
    public int sysTid = -1;
    public String vmState;
    public ArrayList<String> attributeText = new ArrayList<String>();
    public String heldMutexes;
    public ArrayList<StackFrameSnapshot> frames = new ArrayList<StackFrameSnapshot>();
    public boolean runnable;

    public boolean blocked;

    public String outboundBinderPackage;
    public String outboundBinderClass;
    public String outboundBinderMethod;
    
    public String inboundBinderPackage;
    public String inboundBinderClass;
    public String inboundBinderMethod;
    
    public boolean interesting;

    public HashMap<String,LockSnapshot> locks = new HashMap<String,LockSnapshot>();

    /**
     * Construct an empty ThreadSnapshot.
     */
    public ThreadSnapshot() {
    }

    /**
     * Construct a deep copy of the ThreadSnapshot.
     */
    public ThreadSnapshot(ThreadSnapshot that) {
        this.name = that.name;
        this.daemon = that.daemon;
        this.priority = that.priority;
        this.tid = that.tid;
        this.sysTid = that.sysTid;
        this.vmState = that.vmState;
        int N = that.attributeText.size();
        for (int i=0; i<N; i++) {
            this.attributeText.add(that.attributeText.get(i));
        }
        this.heldMutexes = that.heldMutexes;
        N = that.frames.size();
        for (int i=0; i<N; i++) {
            this.frames.add(that.frames.get(i).clone());
        }
        this.runnable = that.runnable;
        this.blocked = that.blocked;
        this.outboundBinderPackage = that.outboundBinderPackage;
        this.outboundBinderClass = that.outboundBinderClass;
        this.outboundBinderMethod = that.outboundBinderMethod;
        this.inboundBinderPackage = that.inboundBinderPackage;
        this.inboundBinderClass = that.inboundBinderClass;
        this.inboundBinderMethod = that.inboundBinderMethod;
        this.interesting = that.interesting;
    }

    /**
     * Make a deep copy of the ThreadSnapshot.
     */
    @Override
    public ThreadSnapshot clone() {
        return new ThreadSnapshot(this);
    }

    /**
     * Compares the threads based on their tid.
     */
    public int compareTo(ThreadSnapshot that) {
        int cmp = this.tid - that.tid;
        if (cmp != 0) return cmp;
        return this.sysTid - that.sysTid;
    }
    
    /**
     * Returns true if this thread has an ingoing or an outgoing binder call.
     */
    public boolean isBinder() {
        return this.outboundBinderPackage != null
                || this.outboundBinderClass != null
                || this.outboundBinderMethod != null
                || this.inboundBinderPackage != null
                || this.inboundBinderClass != null
                || this.inboundBinderMethod != null;
    }
}

