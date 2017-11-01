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

package com.android.bugreport.bugreport;

/**
 * Class to represent what we can determine about a thread.
 */
public class ThreadInfo {
    /**
     * The process this thread is in.
     */
    ProcessInfo process;

    /**
     * The thread id.  Known as sysTid in some java contexts.
     */
    public int tid;

    /**
     * The name of the thread.
     */
    public String name;

    /**
     * Construct a ThreadInfo.
     */
    public ThreadInfo(ProcessInfo pi, int tid, String name) {
        this.process = pi;
        this.tid = tid;
        this.name = name;
    }
}

