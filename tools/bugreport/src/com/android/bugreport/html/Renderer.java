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

package com.android.bugreport.html;

import com.android.bugreport.anr.Anr;
import com.android.bugreport.bugreport.Bugreport;
import com.android.bugreport.cpuinfo.CpuUsage;
import com.android.bugreport.cpuinfo.CpuUsageSnapshot;
import com.android.bugreport.logcat.Logcat;
import com.android.bugreport.logcat.LogLine;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.JavaStackFrameSnapshot;
import com.android.bugreport.stacks.KernelStackFrameSnapshot;
import com.android.bugreport.stacks.LockSnapshot;
import com.android.bugreport.stacks.NativeStackFrameSnapshot;
import com.android.bugreport.stacks.StackFrameSnapshot;
import com.android.bugreport.stacks.ThreadSnapshot;
import com.android.bugreport.stacks.VmTraces;

import com.google.clearsilver.jsilver.JSilver;
import com.google.clearsilver.jsilver.JSilverOptions;
import com.google.clearsilver.jsilver.autoescape.EscapeMode;
import com.google.clearsilver.jsilver.data.Data;
import com.google.clearsilver.jsilver.resourceloader.ClassResourceLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Formats a bugreport as html and writes the file.
 */
public class Renderer {
    /**
     * The next id of the panel to use.
     */
    private int mNextPanelId;

    public Renderer() {
    }

    /**
     * Render the Bugreport into the html file.
     */
    public void render(File outFile, Bugreport bugreport) throws IOException {
        // Load the template and renderer
        final JSilverOptions options = new JSilverOptions();
        options.setEscapeMode(EscapeMode.ESCAPE_HTML);
        final JSilver jsilver = new JSilver(new ClassResourceLoader(getClass()), options);
        final Data hdf = jsilver.createData(); 

        // Build the hierarchical data format data structure
        makeHdf(hdf, bugreport);

        if (false) {
            System.out.println(hdf);
        }

        // Render it
        final FileWriter writer = new FileWriter(outFile);
        try {
            jsilver.render("anr-template.html", hdf, writer);
            writer.close();
        } catch (IOException ex) {
            // Delete the file so we don't leave half-written files laying around.
            try {
                writer.close();
            } catch (IOException e) {
            }
            outFile.delete();
            // And rethrow the exception.
            throw ex;
        }
    }

    /**
     * Build the hdf for a Bugreport.
     */
    private void makeHdf(Data hdf, Bugreport bugreport) {
        // Triage
        makeTriageHdf(hdf, bugreport);

        // Logcat
        makeLogcatHdf(hdf.createChild("logcat"), bugreport);

        // Monkey Anr
        if (bugreport.monkeyAnr != null) {
            makeAnrHdf(hdf, bugreport.monkeyAnr);
        }

        // VM Traces Last ANR
        makeVmTracesHdf(hdf.createChild("vmTracesLastAnr"), bugreport.anr,
                bugreport.vmTracesLastAnr);

        // VM Traces Just Now
        makeVmTracesHdf(hdf.createChild("vmTracesJustNow"), bugreport.anr,
                bugreport.vmTracesJustNow);
    }

    /**
     * Build the hdf for an Anr.
     */
    private void makeAnrHdf(Data hdf, Anr anr) {
        // CPU Usage
        final int N = anr.cpuUsages.size();
        for (int i=0; i<N; i++) {
            makeCpuUsageSnapshotHdf(hdf.createChild("monkey.cpuUsage." + i), anr.cpuUsages.get(i));
        }

        // Processes
        makeVmTracesHdf(hdf.createChild("monkey"), anr, anr.vmTraces);
    }

    /**
     * Build the hdf for a set of vm traces.  Sorts them by likelihood based on the anr.
     */
    private void makeVmTracesHdf(Data hdf, Anr anr, VmTraces vmTraces) {
        // Process List
        final Data processesHdf = hdf.createChild("processes");
        sortProcesses(anr, vmTraces.processes);
        final int N = vmTraces.processes.size();
        for (int i=0; i<N; i++) {
            makeProcessSnapshotHdf(processesHdf.createChild(Integer.toString(i)),
                    vmTraces.processes.get(i));
        }
    }


    /**
     * Make the HDF for the triaged panel.
     *
     * Any single thread will only appear once in the triage panel, at the topmost
     * position.
     */
    private void makeTriageHdf(Data hdf, Bugreport bugreport) {
        final Anr anr = bugreport.anr;

        int N;
        final HashMap<Integer,HashSet<Integer>> visited = new HashMap<Integer,HashSet<Integer>>();

        // General information
        hdf.setValue("triage.processName", anr.processName);
        hdf.setValue("triage.componentPackage", anr.componentPackage);
        hdf.setValue("triage.componentClass", anr.componentClass);
        hdf.setValue("triage.pid", Integer.toString(anr.pid));
        hdf.setValue("triage.reason", anr.reason);

        final ProcessSnapshot offendingProcess = anr.vmTraces.getProcess(anr.pid);
        final ThreadSnapshot offendingThread = anr.vmTraces.getThread(anr.pid, "main");
        if (offendingThread != null) {
            makeThreadSnapshotHdf(hdf.createChild("triage.mainThread"), offendingProcess,
                    offendingThread);

            HashSet<Integer> visitedThreads = new HashSet<Integer>();
            visitedThreads.add(offendingThread.tid);
            visited.put(offendingProcess.pid, visitedThreads);
        }

        // Deadlocked Processes
        final ArrayList<ProcessSnapshot> deadlockedProcesses = cloneAndFilter(visited,
                anr.vmTraces.deadlockedProcesses);
        sortProcesses(anr, deadlockedProcesses);
        N = deadlockedProcesses.size();
        for (int i=0; i<N; i++) {
            makeProcessSnapshotHdf(hdf.createChild("triage.deadlockedProcesses." + i),
                    deadlockedProcesses.get(i));
        }

        // Interesting Processes
        final ArrayList<ProcessSnapshot> interestingProcesses = cloneAndFilter(visited,
                anr.vmTraces.interestingProcesses);
        sortProcesses(anr, interestingProcesses);
        N = interestingProcesses.size();
        for (int i=0; i<N; i++) {
            makeProcessSnapshotHdf(hdf.createChild("triage.interestingProcesses." + i),
                    interestingProcesses.get(i));
        }
    }

    /**
     * Makes a copy of the process and threads, removing ones that have accumulated in the
     * visited list (probably from previous sections on the current page).
     *
     * @see #makeTriageHdf
     */
    private ArrayList<ProcessSnapshot> cloneAndFilter(HashMap<Integer,HashSet<Integer>> visited,
            Collection<ProcessSnapshot> list) {
        final ArrayList<ProcessSnapshot> result = new ArrayList<ProcessSnapshot>();
        for (ProcessSnapshot process: list) {
            final ProcessSnapshot cloneProcess = process.clone();
            HashSet<Integer> visitedThreads = visited.get(process.pid);
            if (visitedThreads == null) {
                visitedThreads = new HashSet<Integer>();
                visited.put(process.pid, visitedThreads);
            }
            final int N = cloneProcess.threads.size();
            for (int i=N-1; i>=0; i--) {
                final ThreadSnapshot cloneThread = cloneProcess.threads.get(i);
                if (visitedThreads.contains(cloneThread.tid)) {
                    cloneProcess.threads.remove(i);
                }
                visitedThreads.add(cloneThread.tid);
            }
            if (cloneProcess.threads.size() > 0) {
                result.add(cloneProcess);
            }
        }
        return result;
    }

    /**
     * Build the hdf for a CpuUsageSnapshot.
     */
    private void makeCpuUsageSnapshotHdf(Data hdf, CpuUsageSnapshot snapshot) {
        int N;

        N = snapshot.cpuUsage.size();
        for (int i=0; i<N; i++) {
            makeCpuUsageHdf(hdf.createChild(Integer.toString(i)), snapshot.cpuUsage.get(i));
        }
    }

    /**
     * Build the hdf for a CpuUsage.
     */
    private void makeCpuUsageHdf(Data hdf, CpuUsage cpuUsage) {
    }

    /**
     * Build the hdf for a ProcessSnapshot.
     */
    private void makeProcessSnapshotHdf(Data hdf, ProcessSnapshot process) {
        int N;

        hdf.setValue("panelId", Integer.toString(mNextPanelId++));

        hdf.setValue("pid", Integer.toString(process.pid));
        hdf.setValue("cmdLine", process.cmdLine);
        hdf.setValue("date", process.date);

        N = process.threads.size();
        for (int i=0; i<N; i++) {
            makeThreadSnapshotHdf(hdf.createChild("threads." + i), process, process.threads.get(i));
        }
    }

    /**
     * Build the hdf for a ThreadSnapshot.
     */
    private void makeThreadSnapshotHdf(Data hdf, ProcessSnapshot process, ThreadSnapshot thread) {
        int N, M;

        hdf.setValue("name", thread.name);
        hdf.setValue("daemon", thread.daemon);
        hdf.setValue("priority", Integer.toString(thread.priority));
        hdf.setValue("tid", Integer.toString(thread.tid));
        hdf.setValue("sysTid", Integer.toString(thread.sysTid));
        hdf.setValue("vmState", thread.vmState);
        hdf.setValue("runnable", thread.runnable ? "1" : "0");
        hdf.setValue("blocked", thread.blocked ? "1" : "0");
        hdf.setValue("interesting", thread.interesting ? "1" : "0");
        hdf.setValue("binder", thread.isBinder() ? "1" : "0");
        hdf.setValue("outboundBinderCall", buildFunctionName(thread.outboundBinderPackage,
                    thread.outboundBinderClass, thread.outboundBinderMethod));
        hdf.setValue("inboundBinderCall", buildFunctionName(thread.inboundBinderPackage,
                    thread.inboundBinderClass, thread.inboundBinderMethod));

        N = thread.attributeText.size();
        for (int i=0; i<N; i++) {
            hdf.setValue("attributes." + i, thread.attributeText.get(i));
        }

        hdf.setValue("heldMutexes", thread.heldMutexes);

        N = thread.frames.size();
        for (int i=0; i<N; i++) {
            makeStackFrameSnapshotHdf(hdf.createChild("frames." + i), process,
                    thread.frames.get(i));
        }
    }

    /**
     * Combine package, class and method into fully qualified name.
     */
    private String buildFunctionName(String pkg, String cls, String meth) {
        final StringBuilder result = new StringBuilder();
        if (pkg != null && pkg.length() > 0) {
            result.append(pkg);
            result.append('.');
        }
        if (cls != null && cls.length() > 0) {
            result.append(cls);
            result.append('.');
        }
        if (meth != null && meth.length() > 0) {
            result.append(meth);
        }
        return result.toString();
    }

    /**
     * Build the hdf for a StackFrameSnapshot.
     */
    private void makeStackFrameSnapshotHdf(Data hdf, ProcessSnapshot process,
            StackFrameSnapshot frame) {
        hdf.setValue("text", frame.text);

        if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_NATIVE) {
            final NativeStackFrameSnapshot f = (NativeStackFrameSnapshot)frame;
            hdf.setValue("frameType", "native");
            hdf.setValue("symbol", f.symbol);
            hdf.setValue("library", f.library);
            hdf.setValue("offset", Integer.toString(f.offset));

        } else if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_KERNEL) {
            final KernelStackFrameSnapshot f = (KernelStackFrameSnapshot)frame;
            hdf.setValue("frameType", "kernel");
            hdf.setValue("syscall", f.syscall);
            hdf.setValue("offset0", Integer.toString(f.offset0));
            hdf.setValue("offset1", Integer.toString(f.offset1));

        } else if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_JAVA) {
            final JavaStackFrameSnapshot f = (JavaStackFrameSnapshot)frame;
            hdf.setValue("frameType", "java");
            hdf.setValue("packageName", f.packageName);
            hdf.setValue("className", f.className);
            hdf.setValue("methodName", f.methodName);
            hdf.setValue("sourceFile", f.sourceFile);
            hdf.setValue("sourceLine", Integer.toString(f.sourceLine));
            hdf.setValue("language",
                    (f.language == JavaStackFrameSnapshot.LANGUAGE_JAVA ? "java" : "jni"));
            final int N = f.locks.size();
            for (int i=0; i<N; i++) {
                final LockSnapshot lock = f.locks.get(i);
                final Data lockHdf = hdf.createChild("locks." + i);
                if (lock.type == LockSnapshot.LOCKED) {
                    lockHdf.setValue("type", "locked");
                } else if (lock.type == LockSnapshot.WAITING) {
                    lockHdf.setValue("type", "waiting");
                } else if (lock.type == LockSnapshot.BLOCKED) {
                    lockHdf.setValue("type", "blocked");
                }
                lockHdf.setValue("address", lock.address);
                lockHdf.setValue("packageName", lock.packageName);
                lockHdf.setValue("className", lock.className);
                lockHdf.setValue("threadId", Integer.toString(lock.threadId));
                if (lock.threadId >= 0) {
                    final ThreadSnapshot referenced = process.getThread(lock.threadId);
                    if (referenced != null) {
                        lockHdf.setValue("threadName", referenced.name);
                    }
                }
            }
        } else {
            hdf.setValue("frameType", "other");
        }
    }

    /**
     * Sort processes so the more interesting ones are at the top.
     */
    private void sortProcesses(Anr anr, List<ProcessSnapshot> processes) {
        final int N = processes.size();

        // Last is alphabetical
        processes.sort(new java.util.Comparator<ProcessSnapshot>() {
                @Override
                public int compare(ProcessSnapshot a, ProcessSnapshot b) {
                    return a.cmdLine.compareTo(b.cmdLine);
                }

                @Override
                public boolean equals(Object that) {
                    return this == that;
                }
            });

        // Move the ones that start with / to the end. They're typically not interesting
        for (int i=0, j=0; i<N; i++) {
            final ProcessSnapshot process = processes.get(j);
            if (process.cmdLine.length() > 0 && process.cmdLine.charAt(0) == '/') {
                processes.remove(j);
                processes.add(process);
            } else {
                j++;
            }
        }

        // TODO: Next is by CPU %

        // The system process always goes second
        for (int i=0; i<N; i++) {
            final ProcessSnapshot process = processes.get(i);
            if ("system_server".equals(process.cmdLine)) {
                processes.remove(i);
                processes.add(0, process);
                break;
            }
        }

        // The blamed process always goes first
        for (int i=0; i<N; i++) {
            final ProcessSnapshot process = processes.get(i);
            if (process.pid == anr.pid) {
                processes.remove(i);
                processes.add(0, process);
                break;
            }
        }

        // And do the threads too.
        sortThreads(processes);
    }

    /**
     * Sort threads so the more interesting ones are at the top.
     */
    private void sortThreads(List<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            final int N = process.threads.size();

            final ArrayList<ThreadSnapshot> mainThreads = new ArrayList<ThreadSnapshot>();
            final ArrayList<ThreadSnapshot> blockedThreads = new ArrayList<ThreadSnapshot>();
            final ArrayList<ThreadSnapshot> binderThreads = new ArrayList<ThreadSnapshot>();
            final ArrayList<ThreadSnapshot> interestingThreads = new ArrayList<ThreadSnapshot>();
            final ArrayList<ThreadSnapshot> otherThreads = new ArrayList<ThreadSnapshot>();

            int insertAt = 0; // in case there are more than one called "main"
            for (int i=0; i<N; i++) {
                final ThreadSnapshot thread = process.threads.get(i);
                if ("main".equals(thread.name)) {
                    mainThreads.add(thread);
                } else if (thread.blocked) {
                    blockedThreads.add(thread);
                } else if (thread.isBinder()) {
                    binderThreads.add(thread);
                } else if (thread.interesting) {
                    interestingThreads.add(thread);
                } else {
                    otherThreads.add(thread);
                }
            }

            // Within those groups, sort by name.
            final java.util.Comparator<ThreadSnapshot> cmp
                    = new java.util.Comparator<ThreadSnapshot>() {
                @Override
                public int compare(ThreadSnapshot a, ThreadSnapshot b) {
                    return a.name.compareTo(b.name);
                }

                @Override
                public boolean equals(Object that) {
                    return this == that;
                }
            };
            mainThreads.sort(cmp);
            blockedThreads.sort(cmp);
            binderThreads.sort(cmp);
            interestingThreads.sort(cmp);
            otherThreads.sort(cmp);

            process.threads = mainThreads;
            process.threads.addAll(blockedThreads);
            process.threads.addAll(binderThreads);
            process.threads.addAll(interestingThreads);
            process.threads.addAll(otherThreads);
        }
    }
    
    /**
     * Make the hdf for the logcat panel.
     */
    private void makeLogcatHdf(Data hdf, Bugreport bugreport) {
        int N;

        final Data interestingHdf = hdf.createChild("interesting");
        N = bugreport.interestingLogLines.size();
        for (int i=0; i<N; i++) {
            final LogLine line = bugreport.interestingLogLines.get(i);
            makeLogcatLineHdf(interestingHdf.createChild(Integer.toString(i)), line);
        }

        final Logcat logcat = bugreport.logcat;
        final Data linesHdf = hdf.createChild("lines");
        N = logcat.lines.size();
        for (int i=0; i<N; i++) {
            final LogLine line = logcat.lines.get(i);
            makeLogcatLineHdf(linesHdf.createChild(Integer.toString(i)), line);
        }
    }

    /**
     * Make hdf for a line of logcat.
     */
    private void makeLogcatLineHdf(Data hdf, LogLine line) {
        hdf.setValue("lineno", Integer.toString(line.lineno));
        if (line.bufferBegin != null) {
            hdf.setValue("bufferBegin", line.bufferBegin);
            hdf.setValue("rawText", line.rawText);
        } else {
            hdf.setValue("header", line.header);
            hdf.setValue("level", Character.toString(line.level));
            hdf.setValue("tag", line.tag);
            hdf.setValue("text", line.text);
            if (line.regionAnr) {
                hdf.setValue("regionAnr", "1");
            }
            if (line.regionBugreport) {
                hdf.setValue("regionBugreport", "1");
            }

            String title = "Process: ??";
            if (line.process != null) {
                title = "Process: " + line.process.cmdLine;
                if (line.thread != null) {
                    title += "\nThread: " + line.thread.name;
                }
            }
            hdf.setValue("title", title);
        }
    }
}
