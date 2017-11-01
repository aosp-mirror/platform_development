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

import com.android.bugreport.cpuinfo.CpuUsageSnapshot;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.ThreadSnapshot;

import java.util.ArrayList;

/**
 * Contains the information about any ANRs that happend in this bugreport.
 */
public class VmTraces {
    public ArrayList<ProcessSnapshot> processes = new ArrayList<ProcessSnapshot>();
    public ArrayList<ProcessSnapshot> interestingProcesses = new ArrayList<ProcessSnapshot>();
    public ArrayList<ProcessSnapshot> deadlockedProcesses = new ArrayList<ProcessSnapshot>();

    public ProcessSnapshot getProcess(int pid) {
        for (ProcessSnapshot process: this.processes) {
            if (process.pid == pid) {
                return process;
            }
        }
        return null;
    }

    public ThreadSnapshot getThread(int pid, String name) {
        final ProcessSnapshot process = getProcess(pid);
        if (process == null) {
            return null;
        }
        return process.getThread(name);
    }

}

