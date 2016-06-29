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

import com.android.bugreport.util.Line;
import com.android.bugreport.util.Lines;
import com.android.bugreport.util.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Parse a vm traces thread.
 *
 * The parser can be reused, but is not thread safe.
 */
public class ThreadSnapshotParser {
    public static final Pattern BEGIN_UNMANAGED_THREAD_RE = Pattern.compile(
                    "\"(.*)\" sysTid=(\\d+)(.*)");
    public static final Pattern BEGIN_MANAGED_THREAD_RE = Pattern.compile(
                    "\"(.*)\" (.*) ?prio=(\\d+)\\s+tid=(\\d+)\\s*(.*)");
    public static final Pattern BEGIN_NOT_ATTACHED_THREAD_RE = Pattern.compile(
                    "\"(.*)\" (.*) ?prio=(\\d+)\\s+(\\(not attached\\))");

    public static final Pattern ATTR_RE = Pattern.compile(
                    "  \\| (.*)");
    public static final Pattern HELD_MUTEXES_RE = Pattern.compile(
                    "  \\| (held mutexes=\\s*(.*))");
    public static final Pattern NATIVE_RE = Pattern.compile(
                    "  (?:native: )?#\\d+ \\S+ [0-9a-fA-F]+\\s+(.*)\\s+\\((.*)\\+(\\d+)\\)");
    public static final Pattern NATIVE_NO_LOC_RE = Pattern.compile(
                    "  (?:native: )?#\\d+ \\S+ [0-9a-fA-F]+\\s+(.*)\\s*\\(?(.*)\\)?");
    public static final Pattern KERNEL_RE = Pattern.compile(
                    "  kernel: (.*)\\+0x([0-9a-fA-F]+)/0x([0-9a-fA-F]+)");
    public static final Pattern KERNEL_UNKNOWN_RE = Pattern.compile(
                    "  kernel: \\(couldn't read /proc/self/task/\\d+/stack\\)");
    public static final Pattern JAVA_RE = Pattern.compile(
                    "  at (?:(.+)\\.)?([^.]+)\\.([^.]+)\\((.*):([\\d-]+)\\)");
    public static final Pattern JNI_RE = Pattern.compile(
                    "  at (?:(.+)\\.)?([^.]+)\\.([^.]+)\\(Native method\\)");
    public static final Pattern LOCKED_RE = Pattern.compile(
                    "  - locked \\<0x([0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
    public static final Pattern SLEEPING_ON_RE = Pattern.compile(
                    "  - sleeping on \\<0x([0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
    public static final Pattern WAITING_ON_RE = Pattern.compile(
                    "  - waiting on \\<0x([0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
    public static final Pattern WAITING_TO_LOCK_RE = Pattern.compile(
                    "  - waiting to lock \\<0x([0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)");
    public static final Pattern WAITING_TO_LOCK_HELD_RE = Pattern.compile(
                    "  - waiting to lock \\<0x([0-9a-fA-F]{1,16})\\> \\(a (?:(.+)\\.)?([^.]+)\\)"
                    + "(?: held by thread (\\d+))");
    public static final Pattern WAITING_TO_LOCK_UNKNOWN_RE = Pattern.compile(
                    "  - waiting to lock an unknown object");
    public static final Pattern NO_MANAGED_STACK_FRAME_RE = Pattern.compile(
                    "  (\\(no managed stack frames\\))");
    private static final Pattern BLANK_RE
            = Pattern.compile("\\s+");

    public static final Pattern SYS_TID_ATTR_RE = Pattern.compile(
                    "  \\| sysTid=(\\d+) .*");
    public static final Pattern STATE_ATTR_RE = Pattern.compile(
                    "  \\| state=R .*");

    /**
     * Construct a new parser.
     */
    public ThreadSnapshotParser() {
    }

    /**
     * Parse the given Lines until the first blank line, which signals the
     * end of the thread. Return a ThreadSnapshot object or null if there wasn't
     * enough text to parse.
     */
    public ThreadSnapshot parse(Lines<? extends Line> lines) {
        final ThreadSnapshot result = new ThreadSnapshot();
        JavaStackFrameSnapshot lastJava = null;

        final Matcher beginUnmanagedThreadRe = BEGIN_UNMANAGED_THREAD_RE.matcher("");
        final Matcher beginManagedThreadRe = BEGIN_MANAGED_THREAD_RE.matcher("");
        final Matcher beginNotAttachedThreadRe = BEGIN_NOT_ATTACHED_THREAD_RE.matcher("");
        final Matcher attrRe = ATTR_RE.matcher("");
        final Matcher heldMutexesRe = HELD_MUTEXES_RE.matcher("");
        final Matcher nativeRe = NATIVE_RE.matcher("");
        final Matcher nativeNoLocRe = NATIVE_NO_LOC_RE.matcher("");
        final Matcher kernelRe = KERNEL_RE.matcher("");
        final Matcher kernelUnknownRe = KERNEL_UNKNOWN_RE.matcher("");
        final Matcher javaRe = JAVA_RE.matcher("");
        final Matcher jniRe = JNI_RE.matcher("");
        final Matcher lockedRe = LOCKED_RE.matcher("");
        final Matcher waitingOnRe = WAITING_ON_RE.matcher("");
        final Matcher sleepingOnRe = SLEEPING_ON_RE.matcher("");
        final Matcher waitingToLockHeldRe = WAITING_TO_LOCK_HELD_RE.matcher("");
        final Matcher waitingToLockRe = WAITING_TO_LOCK_RE.matcher("");
        final Matcher waitingToLockUnknownRe = WAITING_TO_LOCK_UNKNOWN_RE.matcher("");
        final Matcher noManagedStackFrameRe = NO_MANAGED_STACK_FRAME_RE.matcher("");
        final Matcher blankRe = BLANK_RE.matcher("");

        final Matcher sysTidAttrRe = SYS_TID_ATTR_RE.matcher("");
        final Matcher stateAttrRe = STATE_ATTR_RE.matcher("");


        Line line;
        String text;

        // First Line
        if (!lines.hasNext()) {
            // TODO: Handle errors.
            return null;
        }
        line = lines.next();
        if (Utils.matches(beginUnmanagedThreadRe, line.text)) {
            result.type = ThreadSnapshot.TYPE_UNMANAGED;
            result.name = beginUnmanagedThreadRe.group(1);
            result.priority = -1;
            result.tid = -1;
            result.sysTid = Integer.parseInt(beginUnmanagedThreadRe.group(2));
        } else if (Utils.matches(beginManagedThreadRe, line.text)) {
            result.type = ThreadSnapshot.TYPE_MANAGED;
            result.name = beginManagedThreadRe.group(1);
            result.daemon = beginManagedThreadRe.group(2);
            result.priority = Utils.getInt(beginManagedThreadRe, 3, -1);
            result.tid = Utils.getInt(beginManagedThreadRe, 4, -1);
            result.vmState = beginManagedThreadRe.group(5);
        } else if (Utils.matches(beginNotAttachedThreadRe, line.text)) {
            result.type = ThreadSnapshot.TYPE_MANAGED;
            result.name = beginNotAttachedThreadRe.group(1);
            result.daemon = beginNotAttachedThreadRe.group(2);
            result.priority = Utils.getInt(beginNotAttachedThreadRe, 3, -1);
            result.tid = -1;
            result.vmState = beginNotAttachedThreadRe.group(4);
        }

        // Attributes
        while (lines.hasNext()) {
            line = lines.next();
            text = line.text;
            if (Utils.matches(heldMutexesRe, text)) {
                result.attributeText.add(heldMutexesRe.group(1));
                result.heldMutexes = heldMutexesRe.group(2);
            } else if (Utils.matches(attrRe, text)) {
                result.attributeText.add(attrRe.group(1));
                if (Utils.matches(sysTidAttrRe, text)) {
                    result.sysTid = Integer.parseInt(sysTidAttrRe.group(1));
                }
                if (Utils.matches(stateAttrRe, text)) {
                    result.runnable = true;
                }
            } else {
                lines.rewind();
                break;
            }
        }

        // Stack
        while (lines.hasNext()) {
            line = lines.next();
            text = line.text;
            if (Utils.matches(nativeRe, text)) {
                final NativeStackFrameSnapshot frame = new NativeStackFrameSnapshot();
                frame.text = text;
                frame.library = nativeRe.group(1);
                frame.symbol = nativeRe.group(2);
                frame.offset = Integer.parseInt(nativeRe.group(3));
                result.frames.add(frame);
                lastJava = null;
            } else if (Utils.matches(nativeNoLocRe, text)) {
                final NativeStackFrameSnapshot frame = new NativeStackFrameSnapshot();
                frame.text = text;
                frame.library = nativeNoLocRe.group(1);
                frame.symbol = nativeNoLocRe.group(2);
                frame.offset = -1;
                result.frames.add(frame);
                lastJava = null;
            } else if (Utils.matches(kernelRe, text)) {
                final KernelStackFrameSnapshot frame = new KernelStackFrameSnapshot();
                frame.text = text;
                frame.syscall = kernelRe.group(1);
                frame.offset0 = Integer.parseInt(kernelRe.group(3), 16);
                frame.offset1 = Integer.parseInt(kernelRe.group(3), 16);
                result.frames.add(frame);
                lastJava = null;
            } else if (Utils.matches(kernelUnknownRe, text)) {
                final StackFrameSnapshot frame = new StackFrameSnapshot();
                frame.text = text;
                result.frames.add(frame);
                lastJava = null;
            } else if (Utils.matches(javaRe, text)) {
                final JavaStackFrameSnapshot frame = new JavaStackFrameSnapshot();
                frame.text = text;
                frame.packageName = javaRe.group(1);
                frame.className = javaRe.group(2);
                frame.methodName = javaRe.group(3);
                frame.sourceFile = javaRe.group(4);
                frame.sourceLine = Integer.parseInt(javaRe.group(5));
                frame.language = JavaStackFrameSnapshot.LANGUAGE_JAVA;
                result.frames.add(frame);
                lastJava = frame;
            } else if (Utils.matches(jniRe, text)) {
                final JavaStackFrameSnapshot frame = new JavaStackFrameSnapshot();
                frame.text = text;
                frame.packageName = jniRe.group(1);
                frame.className = jniRe.group(2);
                frame.methodName = jniRe.group(3);
                frame.language = JavaStackFrameSnapshot.LANGUAGE_JNI;
                result.frames.add(frame);
                lastJava = frame;
            } else if (Utils.matches(lockedRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.LOCKED;
                    lock.address = lockedRe.group(1);
                    lock.packageName = lockedRe.group(2);
                    lock.className = lockedRe.group(3);
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(waitingOnRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.WAITING;
                    lock.address = waitingOnRe.group(1);
                    lock.packageName = waitingOnRe.group(2);
                    lock.className = waitingOnRe.group(3);
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(sleepingOnRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.SLEEPING;
                    lock.address = sleepingOnRe.group(1);
                    lock.packageName = sleepingOnRe.group(2);
                    lock.className = sleepingOnRe.group(3);
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(waitingToLockHeldRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.BLOCKED;
                    lock.address = waitingToLockHeldRe.group(1);
                    lock.packageName = waitingToLockHeldRe.group(2);
                    lock.className = waitingToLockHeldRe.group(3);
                    lock.threadId = Integer.parseInt(waitingToLockHeldRe.group(4));
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(waitingToLockRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.BLOCKED;
                    lock.address = waitingToLockRe.group(1);
                    lock.packageName = waitingToLockRe.group(2);
                    lock.className = waitingToLockRe.group(3);
                    lock.threadId = -1;
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(waitingToLockUnknownRe, text)) {
                if (lastJava != null) {
                    final LockSnapshot lock = new LockSnapshot();
                    lock.type = LockSnapshot.BLOCKED;
                    lastJava.locks.add(lock);
                }
            } else if (Utils.matches(noManagedStackFrameRe, text)) {
                final StackFrameSnapshot frame = new StackFrameSnapshot();
                frame.text = noManagedStackFrameRe.group(1);
                result.frames.add(frame);
                lastJava = null;
            } else if (text.length() == 0 || Utils.matches(blankRe, text)) {
                break;
            } else {
                final StackFrameSnapshot frame = new StackFrameSnapshot();
                frame.text = text;
                result.frames.add(frame);
                lastJava = null;
                System.out.println("  other  ==> [" + frame.text + "]");
            }
        }


        if (false) {
            System.out.println();
            System.out.println("THREAD");
            System.out.println("name=" + result.name);
            System.out.println("daemon=" + result.daemon);
            System.out.println("priority=" + result.priority);
            System.out.println("tid=" + result.tid);
            System.out.println("vmState=" + result.vmState);
            for (String s: result.attributeText) {
                System.out.println("  attr --> " + s);
            }
            System.out.println("heldMutexes=" + result.heldMutexes);
            for (StackFrameSnapshot frame: result.frames) {
                if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_NATIVE) {
                    final NativeStackFrameSnapshot nf = (NativeStackFrameSnapshot)frame;
                    System.out.println("  frame(native) ==> " + nf.library
                            + " / " + nf.symbol + " / " + nf.offset);
                } else if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_KERNEL) {
                    final KernelStackFrameSnapshot kf = (KernelStackFrameSnapshot)frame;
                    System.out.println("  frame(kernel) ==> " + kf.syscall
                            + " / 0x" + kf.offset0 + " / 0x" + kf.offset1);
                } else if (frame.frameType == StackFrameSnapshot.FRAME_TYPE_JAVA) {
                    final JavaStackFrameSnapshot jf = (JavaStackFrameSnapshot)frame;
                    System.out.println("  frame(java)   ==> " + jf.packageName
                            + " / " + jf.className + " / " + jf.methodName
                            + " / " + jf.sourceFile + " / " + jf.sourceLine
                            + " / "
                            + (jf.language == JavaStackFrameSnapshot.LANGUAGE_JAVA ? "java" : "jni")
                            + " ===> " + jf.text);
                    for (LockSnapshot ls: jf.locks) {
                        System.out.println("                --> "
                                + (ls.type == LockSnapshot.LOCKED ? "locked" : "waiting")
                                + " / " + ls.address + " / " + ls.packageName
                                + " / " + ls.className);
                    }
                } else {
                    System.out.println("  frame(other)  ==> " + frame.text);
                }
            }
        }

        return result;
    }
}

