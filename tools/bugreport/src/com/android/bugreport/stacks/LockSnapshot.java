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
 * Represents an instance of a held lock (java monitor object) in a thread.
 */
public class LockSnapshot implements Comparable<LockSnapshot> {
    public static final int LOCKED = 1;
    public static final int WAITING = 2;
    public static final int SLEEPING = 4;
    public static final int BLOCKED = 8;

    public static final int ANY = LOCKED | WAITING | SLEEPING | BLOCKED;

    public int type;
    public String address;
    public String packageName;
    public String className;
    public int threadId = -1;

    public LockSnapshot() {
    }

    public LockSnapshot(LockSnapshot that) {
        this.type = that.type;
        this.address = that.address;
        this.packageName = that.packageName;
        this.className = that.className;
        this.threadId = that.threadId;
    }

    @Override
    public LockSnapshot clone() {
        return new LockSnapshot(this);
    }

    public boolean equals(LockSnapshot that) {
        return this.address == that.address
                || (this.address != null && that.address != null
                    && this.address.equals(that.address));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        if (this.address != null) {
            hash = hash * 31 + this.address.hashCode();
        }
        return hash;
    }

    public int compareTo(LockSnapshot that) {
        if (this.address == that.address) {
            return 0;
        } else if (this.address == null) {
            return -1;
        } else if (that.address == null) {
            return 1;
        } else {
            return this.address.compareTo(that.address);
        }
    }

}

