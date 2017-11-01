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

package com.android.bugreport.inspector;

import com.android.bugreport.anr.Anr;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.JavaStackFrameSnapshot;
import com.android.bugreport.stacks.LockSnapshot;
import com.android.bugreport.stacks.StackFrameSnapshot;
import com.android.bugreport.stacks.ThreadSnapshot;
import com.android.bugreport.stacks.VmTraces;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;

/**
 * Class to inspect an Anr object and determine which, if any threads are
 * in a cycle of lcoks and binder transactions.
 */
public class DeadlockDetector {

    /**
     * Entry in our growing list of involved threads.
     */
    private static class ThreadRecord implements Comparable<ThreadRecord> {
        public ProcessSnapshot process;
        public ThreadSnapshot thread;

        public ThreadRecord(ProcessSnapshot process, ThreadSnapshot thread) {
            this.process = process;
            this.thread = thread;
        }

        public boolean equals(ThreadRecord that) {
            return this.process == that.process
                    && this.thread == that.thread;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + this.process.hashCode();
            hash = hash * 31 + this.thread.hashCode();
            return hash;
        }

        public int compareTo(ThreadRecord that) {
            int cmp = this.process.compareTo(that.process);
            if (cmp != 0) {
                return cmp;
            }
            return this.thread.compareTo(that.thread);
        }
    }

    /**
     * Entry in our growing list of involved threads.
     */
    private static class LockRecord implements Comparable<LockRecord> {
        public ProcessSnapshot process;
        public LockSnapshot lock;

        public LockRecord(ProcessSnapshot process, LockSnapshot lock) {
            this.process = process;
            this.lock = lock;
        }

        public boolean equals(LockRecord that) {
            return this.process == that.process
                    && (this.lock.address == that.lock.address
                            || (this.lock.address != null && that.lock.address != null
                                && this.lock.address.equals(that.lock.address)));
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 31 + this.process.hashCode();
            if (this.lock.address != null) {
                hash = hash * 31 + this.lock.address.hashCode();
            }
            return hash;
        }

        public int compareTo(LockRecord that) {
            int cmp = this.process.compareTo(that.process);
            if (cmp != 0) {
                return cmp;
            }
            if (this.lock.address == that.lock.address) {
                return 0;
            } else if (this.lock.address == null) {
                return -1;
            } else if (that.lock.address == null) {
                return 1;
            } else {
                return this.lock.address.compareTo(that.lock.address);
            }
        }
    }

    /**
     * Detect any thread cycles that are affecting the main thread of the given pid.
     */
    public static Set<ProcessSnapshot> detectDeadlocks(VmTraces vmTraces, int pid) {
        final boolean dump = false;

        final TreeSet<ThreadRecord> involvedThreads = new TreeSet<ThreadRecord>();

        final TreeSet<LockRecord> locksToVisit = new TreeSet<LockRecord>();
        final TreeSet<LockRecord> locksVisited = new TreeSet<LockRecord>();

        // Seed the traversal with the locks held by the main thread.
        final ProcessSnapshot offendingProcess = vmTraces.getProcess(pid);
        if (offendingProcess == null) {
            return new TreeSet<ProcessSnapshot>();
        }
        final ThreadSnapshot offendingThread = offendingProcess.getThread("main");
        if (offendingThread == null) {
            return new TreeSet<ProcessSnapshot>();
        }
        addLockRecordsForThread(locksToVisit, locksVisited, offendingProcess, offendingThread);

        if (dump) {
            if (offendingThread.outboundBinderPackage != null
                    || offendingThread.outboundBinderClass != null
                    || offendingThread.inboundBinderMethod != null) {
                System.out.println("Offending thread:");
                System.out.print("  pid=" + offendingProcess.pid + " \"" + offendingThread.name
                        + "\" (tid=" + offendingThread.tid + ")");
                if (offendingThread.outboundBinderClass != null) {
                    System.out.print(" outbound=" + offendingThread.outboundBinderPackage + "."
                            + offendingThread.outboundBinderClass
                            + "." + offendingThread.outboundBinderMethod);
                }
                if (offendingThread.inboundBinderClass != null) {
                    System.out.print(" inbound=" + offendingThread.inboundBinderPackage + "."
                            + offendingThread.inboundBinderClass
                            + "." + offendingThread.inboundBinderMethod);
                }
                System.out.println();
            }
        }

        if (locksToVisit.size() == 0) {
            // There weren't any locks. Just stop here.
            return new TreeSet<ProcessSnapshot>();
        }

        involvedThreads.add(new ThreadRecord(offendingProcess, offendingThread));

        // Terminate when we stop finding new locks to look at. We will terminate
        // eventually because there are a finite number of locks in the system.
        while (locksToVisit.size() > 0) {
            final LockRecord lr = locksToVisit.pollFirst();
            
            // Don't allow ourselves to re-add this lock
            locksVisited.add(lr);

            // Find all the threads holding this lock.
            for (ThreadSnapshot thread: lr.process.threads) {
                final Map<String,LockSnapshot> locks = thread.locks;
                if (locks.containsKey(lr.lock.address)) {
                    if (dump) {
                        System.out.println("Thread " + thread.tid
                                + " contains lock " + lr.lock.address);
                    }
                    // This thread is holding the lock (or trying to).
                    // Enqeue its other locks that we haven't already done.
                    addLockRecordsForThread(locksToVisit, locksVisited, lr.process, thread);
                    involvedThreads.add(new ThreadRecord(lr.process, thread));
                }
            }
        }

        final HashMap<Integer,ProcessSnapshot> results = new HashMap<Integer,ProcessSnapshot>();

        // Add the process / thread pairs into the results
        if (dump) System.out.println("Involved threads:");
        for (ThreadRecord tr: involvedThreads) {
            if (dump) {
                System.out.print("  pid=" + tr.process.pid + " \"" + tr.thread.name
                        + "\" (tid=" + tr.thread.tid + ")");
                if (tr.thread.outboundBinderClass != null) {
                    System.out.print(" outbound=" + tr.thread.outboundBinderPackage + "."
                            + tr.thread.outboundBinderClass + "." + tr.thread.outboundBinderMethod);
                }
                if (tr.thread.inboundBinderClass != null) {
                    System.out.print(" inbound=" + tr.thread.inboundBinderPackage + "."
                            + tr.thread.inboundBinderClass + "." + tr.thread.inboundBinderMethod);
                }
                System.out.println();
            }

            ProcessSnapshot cloneProcess = results.get(tr.process.pid);
            if (cloneProcess == null) {
                cloneProcess = tr.process.clone();
                cloneProcess.threads.clear();
                results.put(tr.process.pid, cloneProcess);
            }
            cloneProcess.threads.add(tr.thread);
        }
        if (dump) {
            System.out.println("Involved locks:");
            for (LockRecord lr: locksVisited) {
                System.out.println("  pid=" + lr.process.pid + " " + lr.lock.packageName
                        + "." + lr.lock.className + " - " + lr.lock.address);
            }
        }

        return new TreeSet<ProcessSnapshot>(results.values());
    }

    /**
     * Add the LockRecords for the locks held by the thread to toVisit, unless
     * they're already in alreadyVisited.
     */
    private static void addLockRecordsForThread(TreeSet<LockRecord> toVisit, 
            TreeSet<LockRecord> alreadyVisited, ProcessSnapshot process, ThreadSnapshot thread) {
        for (LockSnapshot lock: thread.locks.values()) {
            final LockRecord next = new LockRecord(process, lock);
            if (!alreadyVisited.contains(next)) {
                toVisit.add(next);
            }
        }
    }
}
