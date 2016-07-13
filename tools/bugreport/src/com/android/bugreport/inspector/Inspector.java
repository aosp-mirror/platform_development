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
import com.android.bugreport.anr.AnrParser;
import com.android.bugreport.bugreport.Bugreport;
import com.android.bugreport.bugreport.ProcessInfo;
import com.android.bugreport.bugreport.ThreadInfo;
import com.android.bugreport.logcat.Logcat;
import com.android.bugreport.logcat.LogcatParser;
import com.android.bugreport.logcat.LogLine;
import com.android.bugreport.stacks.ProcessSnapshot;
import com.android.bugreport.stacks.JavaStackFrameSnapshot;
import com.android.bugreport.stacks.LockSnapshot;
import com.android.bugreport.stacks.StackFrameSnapshot;
import com.android.bugreport.stacks.ThreadSnapshot;
import com.android.bugreport.stacks.VmTraces;
import com.android.bugreport.util.Utils;
import com.android.bugreport.util.Lines;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Inspects a raw parsed bugreport.  Makes connections between the different sections,
 * and annotates the different parts.
 *
 * (This is the "smarts" of the app. The rendering is mostly just straightforward view code.)
 */
public class Inspector {
    private static final String[] NO_JAVA_METHODS = new String[0];
    private static final String[] HANDWRITTEN_BINDER_SUFFIXES = new String[] { "Native", "Proxy" };

    private final Matcher mBufferBeginRe = LogcatParser.BUFFER_BEGIN_RE.matcher("");

    private final Bugreport mBugreport;

    /**
     * Inspect a bugreport.
     */
    public static void inspect(Bugreport bugreport) {
        (new Inspector(bugreport)).inspect();
    }

    /**
     * Constructor.
     */
    private Inspector(Bugreport bugreport) {
        mBugreport = bugreport;
    }

    /**
     * Do the inspection.  Calls to the various sub-functions to do the work.
     */
    private void inspect() {
        makeProcessInfo();

        findAnr();

        inspectProcesses(mBugreport.vmTracesJustNow);
        inspectProcesses(mBugreport.vmTracesLastAnr);

        if (mBugreport.anr != null) {
            inspectProcesses(mBugreport.anr.vmTraces);
            markDeadlocks(mBugreport.anr.vmTraces, mBugreport.anr.pid);
        }

        inventLogcatTimes();
        mergeLogcat();
        makeInterestingLogcat();
        markLogcatProcessesAndThreads();
        markAnrLogcatRegions();
        markBugreportRegions();
        //trimLogcat();

        if (mBugreport.anr != null) {
            makeInterestingProcesses(mBugreport.anr.vmTraces);
        }
    }

    /**
     * Go through all our sources of information and figure out as many process
     * and thread names as we can.
     */
    private void makeProcessInfo() {
        if (mBugreport.anr != null) {
            makeProcessInfo(mBugreport.anr.vmTraces.processes);
        }
        if (mBugreport.vmTracesJustNow != null) {
            makeProcessInfo(mBugreport.vmTracesJustNow.processes);
        }
        if (mBugreport.vmTracesLastAnr != null) {
            makeProcessInfo(mBugreport.vmTracesLastAnr.processes);
        }
    }

    /**
     * Sniff this VmTraces object for ProcessInfo and ThreadInfos that we need to create
     * and add them to the Bugreport.
     */
    private void makeProcessInfo(ArrayList<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            final ProcessInfo pi = makeProcessInfo(process.pid, process.cmdLine);
            for (ThreadSnapshot thread: process.threads) {
                makeThreadInfo(pi, thread.sysTid, thread.name);
            }
        }
    }

    /**
     * If there isn't already one for this pid, make a ProcessInfo.  If one already
     * exists, return that. If we now have a more complete cmdLine, fill that in too.
     */
    private ProcessInfo makeProcessInfo(int pid, String cmdLine) {
        ProcessInfo pi = mBugreport.allKnownProcesses.get(pid);
        if (pi == null) {
            pi = new ProcessInfo(pid, cmdLine);
            mBugreport.allKnownProcesses.put(pid, pi);
        } else {
            if (cmdLine.length() > pi.cmdLine.length()) {
                pi.cmdLine = cmdLine;
            }
        }
        return pi;
    }

    /**
     * If there isn't already one for this tid, make a ThreadInfo.  If one already
     * exists, return that. If we now have a more complete name, fill that in too.
     */
    private ThreadInfo makeThreadInfo(ProcessInfo pi, int tid, String name) {
        ThreadInfo ti = pi.threads.get(tid);
        if (ti == null) {
            ti = new ThreadInfo(pi, tid, name);
            pi.threads.put(tid, ti);
        } else {
            if (name.length() > ti.name.length()) {
                ti.name = name;
            }
        }
        return ti;
    }

    /**
     * If there isn't already an ANR set on the bugreport (e.g. from monkeys), find
     * one in the logcat.
     */
    private void findAnr() {
        // TODO: It would be better to restructure the whole triage thing into a more
        // modular "suggested problem" format, rather than it all being centered around
        // there being an anr.  More thoughts on this later...
        if (mBugreport.anr != null) {
            return;
        }
        final ArrayList<LogLine> logLines = mBugreport.systemLog.filter("ActivityManager", "E");
        final AnrParser parser = new AnrParser();
        final ArrayList<Anr> anrs = parser.parse(new Lines<LogLine>(logLines), false);
        if (anrs.size() > 0) {
            mBugreport.anr = anrs.get(0);
            // TODO: This is LAST anr, not FIRST anr, so it might not actually match.
            // We really should find a better way of recording the traces.
            mBugreport.anr.vmTraces = mBugreport.vmTracesLastAnr;
        }
    }

    /**
     * Do all the process inspection.  Works on any list of processes, not just ANRs.
     */
    private void inspectProcesses(VmTraces vmTraces) {
        combineLocks(vmTraces.processes);
        markBinderThreads(vmTraces.processes);
        markBlockedThreads(vmTraces.processes);
        markInterestingThreads(vmTraces.processes);
    }

    /**
     * Pulls the locks out of the individual stack frames and tags the threads
     * with which locks are being held or blocked on.
     */
    private void combineLocks(ArrayList<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            for (ThreadSnapshot thread: process.threads) {
                for (StackFrameSnapshot frame: thread.frames) {
                    if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_JAVA) {
                        final JavaStackFrameSnapshot f = (JavaStackFrameSnapshot)frame;
                        for (LockSnapshot lock: f.locks) {
                            final LockSnapshot prev = thread.locks.get(lock.address);
                            if (prev != null) {
                                prev.type |= lock.type;
                            } else {
                                thread.locks.put(lock.address, lock.clone());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Mark the threads that are doing binder transactions.
     */
    private void markBinderThreads(ArrayList<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            for (ThreadSnapshot thread: process.threads) {
                markOutgoingBinderThread(thread);
                markIncomingBinderThread(thread);
            }
        }
    }

    /**
     * Sniff a thread thread stack for whether it is doing an outgoing binder
     * transaction (at the top of the stack).
     */
    private boolean markOutgoingBinderThread(ThreadSnapshot thread) {
        // If top of the stack is android.os.BinderProxy.transactNative...
        int i;
        final int N = thread.frames.size();
        StackFrameSnapshot frame = null;
        for (i=0; i<N; i++) {
            frame = thread.frames.get(i);
            if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_JAVA) {
                break;
            }
        }
        if (i >= N) {
            return false;
        }
        JavaStackFrameSnapshot f = (JavaStackFrameSnapshot)frame;
        if (!("android.os".equals(f.packageName)
                && "BinderProxy".equals(f.className)
                && "transactNative".equals(f.methodName))) {
            return false;
        }

        // And the next one is android.os.BinderProxy.transact...
        i++;
        if (i >= N) {
            return false;
        }
        frame = thread.frames.get(i);
        if (frame.frameType != StackFrameSnapshot.FRAME_TYPE_JAVA) {
            return false;
        }
        f = (JavaStackFrameSnapshot)frame;
        if (!("android.os".equals(f.packageName)
                && "BinderProxy".equals(f.className)
                && "transact".equals(f.methodName))) {
            return false;
        }

        // Then the one after that is the glue code for that IPC.
        i++;
        if (i >= N) {
            return false;
        }
        frame = thread.frames.get(i);
        if (frame.frameType != StackFrameSnapshot.FRAME_TYPE_JAVA) {
            return false;
        }
        f = (JavaStackFrameSnapshot)frame;
        thread.outboundBinderPackage = f.packageName;
        thread.outboundBinderClass = fixBinderClass(f.className);
        thread.outboundBinderMethod = f.methodName;
        return true;
    }

    /**
     * Sniff a thread thread stack for whether it is doing an inbound binder
     * transaction (at the bottom of the stack).
     */
    private boolean markIncomingBinderThread(ThreadSnapshot thread) {
        // If bottom of the stack is android.os.Binder.execTransact...
        int i;
        final int N = thread.frames.size();
        StackFrameSnapshot frame = null;
        for (i=N-1; i>=0; i--) {
            frame = thread.frames.get(i);
            if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_JAVA) {
                break;
            }
        }
        if (i < 0) {
            return false;
        }
        JavaStackFrameSnapshot f = (JavaStackFrameSnapshot)frame;
        if (!("android.os".equals(f.packageName)
                && "Binder".equals(f.className)
                && "execTransact".equals(f.methodName))) {
            return false;
        }

        // The next one will be the binder glue, which has the package and interface
        i--;
        if (i < 0) {
            return false;
        }
        frame = thread.frames.get(i);
        if (frame.frameType != StackFrameSnapshot.FRAME_TYPE_JAVA) {
            return false;
        }
        f = (JavaStackFrameSnapshot)frame;
        thread.inboundBinderPackage = f.packageName;
        thread.inboundBinderClass = fixBinderClass(f.className);

        // And the one after that will be the implementation, which has the method.
        // If it got inlined, e.g. by proguard, we might not get a method.
        i--;
        if (i < 0) {
            return true;
        }
        frame = thread.frames.get(i);
        if (frame.frameType != StackFrameSnapshot.FRAME_TYPE_JAVA) {
            return true;
        }
        f = (JavaStackFrameSnapshot)frame;
        thread.inboundBinderMethod = f.methodName;
        return true;
    }

    /**
     * Try to clean up the bomder class name by removing the aidl inner classes
     * and sniffing out the older manually written binder glue convention of
     * calling the functions "Native."
     */
    private String fixBinderClass(String className) {
        if (className == null) {
            return null;
        }

        final String stubProxySuffix = "$Stub$Proxy";
        if (className.endsWith(stubProxySuffix)) {
            return className.substring(0, className.length() - stubProxySuffix.length());
        }

        final String stubSuffix = "$Stub";
        if (className.endsWith(stubSuffix)) {
            return className.substring(0, className.length() - stubSuffix.length());
        }

        for (String suffix: HANDWRITTEN_BINDER_SUFFIXES) {
            if (className.length() > suffix.length() + 2) {
                if (className.endsWith(suffix)) {
                    final char first = className.charAt(0);
                    final char second = className.charAt(1);
                    if (className.endsWith(suffix)) {
                        if (first == 'I' && Character.isUpperCase(second)) {
                            return className.substring(0, className.length()-suffix.length());
                        } else {
                            return "I" + className.substring(0, className.length()-suffix.length());
                        }
                    }
                }
            }
        }

        return className;
    }

    /**
     * Sniff the threads that are blocked on other things.
     */
    private void markBlockedThreads(ArrayList<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            for (ThreadSnapshot thread: process.threads) {
                // These threads are technically blocked, but it's expected so don't report it.
                if (matchesJavaStack(thread, "HeapTaskDaemon", new String[] {
                            "dalvik.system.VMRuntime.runHeapTasks",
                            "java.lang.Daemons$HeapTaskDaemon.run",
                            "java.lang.Thread.run",
                        })) {
                    continue;
                }

                thread.blocked = isThreadBlocked(thread);
            }
        }
    }

    /**
     * Sniff whether a thread is blocked on at least one java lock.
     */
    private boolean isThreadBlocked(ThreadSnapshot thread) {
        for (LockSnapshot lock: thread.locks.values()) {
            if ((lock.type & LockSnapshot.BLOCKED) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mark threads to be flagged in the bugreport view.
     */
    private void markInterestingThreads(ArrayList<ProcessSnapshot> processes) {
        for (ProcessSnapshot process: processes) {
            for (ThreadSnapshot thread: process.threads) {
                thread.interesting = isThreadInteresting(thread);
            }
        }
    }

    /**
     * Clone the "interesting" processes and filter out any threads that aren't
     * marked "interesting" and any processes without any interesting threads.
     */
    private void makeInterestingProcesses(VmTraces vmTraces) {
        for (ProcessSnapshot process: vmTraces.processes) {
            // Make a deep copy of the process
            process = process.clone();

            // Filter out the threads that aren't interesting
            for (int i=process.threads.size()-1; i>=0; i--) {
                if (!process.threads.get(i).interesting) {
                    process.threads.remove(i);
                }
            }

            // If there is anything interesting about the process itself, add it
            if (isProcessInteresting(process)) {
                vmTraces.interestingProcesses.add(process);
            }
        }
    }

    /**
     * Determine whether there is anything worth noting about this process.
     */
    private boolean isProcessInteresting(ProcessSnapshot process) {
        // This is the Process mentioned by the ANR report
        if (mBugreport.anr != null && mBugreport.anr.pid == process.pid) {
            return true;
        }

        // There are > 1 threads that are interesting in this process
        if (process.threads.size() > 0) {
            return true;
        }

        // TODO: The CPU usage for this process is > 10%
        if (false) {
            return true;
        }

        // Otherwise it's boring
        return false;
    }

    /**
     * Determine whether there is anything worth noting about this thread.
     */
    private boolean isThreadInteresting(ThreadSnapshot thread) {
        // The thread that dumps the stack traces is boring
        if (matchesJavaStack(thread, "Signal Catcher", NO_JAVA_METHODS)) {
            return false;
        }

        // The thread is marked runnable
        if (thread.runnable) {
            return true;
        }

        // TODO: It's holding a mutex that's not "mutator lock"(shared held)?

        // Binder threads are interesting
        if (thread.isBinder()) {
            return true;
        }

        // Otherwise it's boring
        return false;
    }

    /**
     * Return whether the java stack for a thread is the same as the signature supplied.
     * Skips non-java stack frames.
     */
    private boolean matchesJavaStack(ThreadSnapshot thread, String name, String[] signature) {
        // Check the name
        if (name != null && !name.equals(thread.name)) {
            return false;
        }

        final ArrayList<StackFrameSnapshot> frames = thread.frames;
        int i = 0;
        final int N = frames.size();
        int j = 0;
        final int M = signature.length;

        while (i<N && j<M) {
            final StackFrameSnapshot frame = frames.get(i);
            if (frame.frameType != JavaStackFrameSnapshot.FRAME_TYPE_JAVA) {
                // Not java, keep advancing.
                i++;
                continue;
            }
            final JavaStackFrameSnapshot f = (JavaStackFrameSnapshot)frame;
            final String full = (f.packageName != null ? f.packageName + "." : "")
                    + f.className + "." + f.methodName;
            if (!full.equals(signature[j])) {
                // This java frame doesn't match the expected signature element,
                // so it's not a match.
                return false;
            }
            // Advance both
            i++;
            j++;
        }

        // If we didn't get through the signature, it's not a match
        if (j != M) {
            return false;
        }

        // If there are more java frames, it's not a match
        for (; i<N; i++) {
            if (frames.get(i).frameType == JavaStackFrameSnapshot.FRAME_TYPE_JAVA) {
                return false;
            }
        }

        // We have a winner.
        return true;
    }

    /**
     * Traverse the threads looking for cyclical dependencies of blocked threads.
     *
     * TODO: If pid isn't provided, we should run it for all main threads.  And show all of
     * the deadlock cycles, with the one from the anr at the top if possible.
     *
     * @see DeadlockDetector
     */
    private void markDeadlocks(VmTraces vmTraces, int pid) {
        final Set<ProcessSnapshot> deadlock = DeadlockDetector.detectDeadlocks(vmTraces, pid);
        vmTraces.deadlockedProcesses.addAll(deadlock);
    }

    /**
     * Fill in times for the logcat section log lines that don't have one (like
     * the beginning of buffer lines).
     */
    private void inventLogcatTimes() {
        inventLogcatTimes(mBugreport.systemLog.lines);
        inventLogcatTimes(mBugreport.eventLog.lines);
        if (mBugreport.logcat != null) {
            inventLogcatTimes(mBugreport.logcat.lines);
        }
    }

    /**
     * Fill in times for a logcat section by taking the time from an adjacent line.
     * Prefers to get the time from a line after the log line.
     */
    private void inventLogcatTimes(ArrayList<LogLine> lines) {
        GregorianCalendar time = null;
        final int N = lines.size();
        int i;
        // Going backwards first makes most missing ones get the next time
        // which will pair it with the next log line in the merge, which is
        // what we want.
        for (i=N-1; i>=0; i--) {
            final LogLine line = lines.get(i);
            if (line.time == null) {
                line.time = time;
            } else {
                time = line.time;
            }
        }

        // Then go find the last one that's null, and get it a time.
        // If none have times, then... oh well.
        for (i=N-1; i>=0; i--) {
            final LogLine line = lines.get(i);
            if (line.time != null) {
                time = line.time;
                break;
            }
        }
        for (; i<N && i>=0; i++) {
            final LogLine line = lines.get(i);
            line.time = time;
        }
    }

    /**
     * Merge the system and event logs by timestamp.
     */
    private void mergeLogcat() {
        // Only do this if they haven't already supplied a logcat.
        if (mBugreport.logcat != null) {
            return;
        }

        // Renumber the logcat lines.  We mess up the other lists, but that
        // saves the work of making copies of the logcat lines.  If this
        // really becomes a problem, then it's not too much work to add
        // LogLine.clone().
        int lineno = 1;
        final Logcat result = mBugreport.logcat =  new Logcat();
        final ArrayList<LogLine> system = mBugreport.systemLog.lines;
        final ArrayList<LogLine> event = mBugreport.eventLog.lines;

        final int systemSize = system != null ? system.size() : 0;
        final int eventSize = event != null ? event.size() : 0;

        int systemIndex = 0;
        int eventIndex = 0;

        // The event log doesn't have a beginning of marker.  Make up one
        // when we see the first event line.
        boolean seenEvent = false;

        while (systemIndex < systemSize && eventIndex < eventSize) {
            final LogLine systemLine = system.get(systemIndex);
            final LogLine eventLine = event.get(eventIndex);

            if (systemLine.time == null) {
                systemLine.lineno = lineno++;
                result.lines.add(systemLine);
                systemIndex++;
                continue;
            }

            if (eventLine.time == null) {
                eventLine.lineno = lineno++;
                result.lines.add(eventLine);
                eventIndex++;
                seenEvent = true;
                continue;
            }

            if (systemLine.time.compareTo(eventLine.time) <= 0) {
                systemLine.lineno = lineno++;
                result.lines.add(systemLine);
                systemIndex++;
            } else {
                if (!seenEvent) {
                    final LogLine synthetic = new LogLine();
                    synthetic.lineno = lineno++;
                    synthetic.rawText = synthetic.text = "--------- beginning of event";
                    synthetic.bufferBegin = "event";
                    synthetic.time = eventLine.time;
                    result.lines.add(synthetic);
                    seenEvent = true;
                }
                eventLine.lineno = lineno++;
                result.lines.add(eventLine);
                eventIndex++;
            }
        }

        for (; systemIndex < systemSize; systemIndex++) {
            final LogLine systemLine = system.get(systemIndex);
            systemLine.lineno = lineno++;
            result.lines.add(systemLine);
        }

        for (; eventIndex < eventSize; eventIndex++) {
            final LogLine eventLine = event.get(eventIndex);
            if (!seenEvent) {
                final LogLine synthetic = new LogLine();
                synthetic.lineno = lineno++;
                synthetic.rawText = synthetic.text = "--------- beginning of event";
                synthetic.bufferBegin = "event";
                synthetic.time = eventLine.time;
                result.lines.add(synthetic);
                seenEvent = true;
            }
            eventLine.lineno = lineno++;
            result.lines.add(eventLine);
        }
    }

    /**
     * Utility class to match log lines that are "interesting" and will
     * be called out with links at the top of the log and triage sections.
     */
    private class InterestingLineMatcher {
        private String mTag;
        protected Matcher mMatcher;

        /**
         * Construct the helper object with the log tag that must be an
         * exact match and a message which is a regex pattern.
         */
        public InterestingLineMatcher(String tag, String regex) {
            mTag = tag;
            mMatcher = Pattern.compile(regex).matcher("");
        }

        /**
         * Return whether the LogLine text matches the patterns supplied in the
         * constructor.
         */
        public boolean match(LogLine line) {
            return mTag.equals(line.tag)
                    && Utils.matches(mMatcher, line.text);
        }
    }

    /**
     * The matchers to use to detect interesting log lines.
     */
    private final InterestingLineMatcher[] mInterestingLineMatchers
            = new InterestingLineMatcher[] {
                // ANR logcat
                new InterestingLineMatcher("ActivityManager",
                        "ANR in \\S+.*"),
            };

    /**
     * Mark the log lines to be called out with links at the top of the
     * log and triage sections.
     */
    private void makeInterestingLogcat() {
        final Logcat logcat = mBugreport.logcat;
        Matcher m;

        for (LogLine line: logcat.lines) {
            // Beginning of buffer
            if ((m = Utils.match(mBufferBeginRe, line.rawText)) != null) {
                mBugreport.interestingLogLines.add(line);
            }

            
            // Regular log lines
            for (InterestingLineMatcher ilm: mInterestingLineMatchers) {
                if (ilm.match(line)) {
                    mBugreport.interestingLogLines.add(line);
                }
            }
        }
    }

    /**
     * For each of the log lines, attach a process and a thread.
     */
    private void markLogcatProcessesAndThreads() {
        final Logcat logcat = mBugreport.logcat;

        final Matcher inputDispatcherRe = Pattern.compile(
                "Application is not responding: .* It has been (\\d+\\.?\\d*)ms since event,"
                + " (\\d+\\.?\\d*)ms since wait started.*").matcher("");

        for (LogLine line: logcat.lines) {
            line.process = mBugreport.allKnownProcesses.get(line.pid);
            if (line.process != null) {
                line.thread = line.process.threads.get(line.tid);
            }
        }
    }

    /**
     * For each of the log lines that indicate a time range between the beginning
     * of an anr timer and when it went off, mark that range.
     */
    private void markAnrLogcatRegions() {
        final Logcat logcat = mBugreport.logcat;

        final Matcher inputDispatcherRe = Pattern.compile(
                "Application is not responding: .* It has been (\\d+\\.?\\d*)ms since event,"
                + " (\\d+\\.?\\d*)ms since wait started.*").matcher("");

        for (LogLine line: logcat.lines) {
            if ("InputDispatcher".equals(line.tag)
                    && Utils.matches(inputDispatcherRe, line.text)) {
                float f = Float.parseFloat(inputDispatcherRe.group(2));
                int seconds = (int)(f / 1000);
                int milliseconds = Math.round(f % 1000);
                final Calendar begin = (Calendar)line.time.clone();
                begin.add(Calendar.SECOND, -seconds);
                begin.add(Calendar.MILLISECOND, -milliseconds);
                markAnrRegion(begin, line.time);
            }
        }
    }

    /**
     * Mark the log lines that happened between the begin and end timestamps
     * as during the period between when an ANR timer is set and when it goes
     * off.
     */
    private void markAnrRegion(Calendar begin, Calendar end) {
        for (LogLine line: mBugreport.logcat.lines) {
            if (line.time.compareTo(begin) >= 0
                    && line.time.compareTo(end) < 0) {
                line.regionAnr = true;
            }
        }
    }

    /**
     * Mark the log lines that were captured while this bugreport was being
     * taken. Those tend to be less reliable, and are also an indicator of
     * when the user saw the bug that caused them to take a bugreport.
     */
    private void markBugreportRegions() {
        final Calendar begin = mBugreport.startTime;
        final Calendar end = mBugreport.endTime;
        for (LogLine line: mBugreport.logcat.lines) {
            if (line.time != null) {
                if (line.time.compareTo(begin) >= 0
                        && line.time.compareTo(end) < 0) {
                    line.regionBugreport = true;
                }
            }
        }
    }

    /**
     * Trim the logcat to show no more than 3 seconds after the beginning of
     * the bugreport, and no more than 5000 lines before the beginning of the bugreport.
     */
    private void trimLogcat() {
        final Calendar end = (Calendar)mBugreport.startTime.clone();
        end.add(Calendar.SECOND, 3);

        final ArrayList<LogLine> lines = mBugreport.logcat.lines;
        int i;

        // Trim the ones at the end
        int endIndex = lines.size() - 1;
        for (i=lines.size()-1; i>=0; i--) {
            final LogLine line = lines.get(i);
            if (line.time != null) {
                // If we've gotten to 3s after when the bugreport started getting taken, stop.
                if (line.time.compareTo(end) > 0) {
                    endIndex = i;
                    break;
                }
            }
        }

        // Trim the ones at the beginning
        int startIndex = 0;
        int count = 0;
        for (; i>=0; i--) {
            final LogLine line = lines.get(i);
            count++;
            if (count >= 5000) {
                startIndex = i;
                break;
            }
        }

        mBugreport.logcat.lines = new ArrayList<LogLine>(lines.subList(startIndex, endIndex));
    }
}
