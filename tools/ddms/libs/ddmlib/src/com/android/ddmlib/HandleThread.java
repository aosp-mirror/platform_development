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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handle thread status updates.
 */
final class HandleThread extends ChunkHandler {

    public static final int CHUNK_THEN = type("THEN");
    public static final int CHUNK_THCR = type("THCR");
    public static final int CHUNK_THDE = type("THDE");
    public static final int CHUNK_THST = type("THST");
    public static final int CHUNK_THNM = type("THNM");
    public static final int CHUNK_STKL = type("STKL");

    private static final HandleThread mInst = new HandleThread();

    // only read/written by requestThreadUpdates()
    private static volatile boolean mThreadStatusReqRunning = false;
    private static volatile boolean mThreadStackTraceReqRunning = false;

    private HandleThread() {}


    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_THCR, mInst);
        mt.registerChunkHandler(CHUNK_THDE, mInst);
        mt.registerChunkHandler(CHUNK_THST, mInst);
        mt.registerChunkHandler(CHUNK_THNM, mInst);
        mt.registerChunkHandler(CHUNK_STKL, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {
        Log.d("ddm-thread", "Now ready: " + client);
        if (client.isThreadUpdateEnabled())
            sendTHEN(client, true);
    }

    /**
     * Client went away.
     */
    @Override
    public void clientDisconnected(Client client) {}

    /**
     * Chunk handler entry point.
     */
    @Override
    public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {

        Log.d("ddm-thread", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_THCR) {
            handleTHCR(client, data);
        } else if (type == CHUNK_THDE) {
            handleTHDE(client, data);
        } else if (type == CHUNK_THST) {
            handleTHST(client, data);
        } else if (type == CHUNK_THNM) {
            handleTHNM(client, data);
        } else if (type == CHUNK_STKL) {
            handleSTKL(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /*
     * Handle a thread creation message.
     *
     * We should be tolerant of receiving a duplicate create message.  (It
     * shouldn't happen with the current implementation.)
     */
    private void handleTHCR(Client client, ByteBuffer data) {
        int threadId, nameLen;
        String name;

        threadId = data.getInt();
        nameLen = data.getInt();
        name = getString(data, nameLen);

        Log.v("ddm-thread", "THCR: " + threadId + " '" + name + "'");

        client.getClientData().addThread(threadId, name);
        client.update(Client.CHANGE_THREAD_DATA);
    }

    /*
     * Handle a thread death message.
     */
    private void handleTHDE(Client client, ByteBuffer data) {
        int threadId;

        threadId = data.getInt();
        Log.v("ddm-thread", "THDE: " + threadId);

        client.getClientData().removeThread(threadId);
        client.update(Client.CHANGE_THREAD_DATA);
    }

    /*
     * Handle a thread status update message.
     *
     * Response has:
     *  (1b) header len
     *  (1b) bytes per entry
     *  (2b) thread count
     * Then, for each thread:
     *  (4b) threadId (matches value from THCR)
     *  (1b) thread status
     *  (4b) tid
     *  (4b) utime
     *  (4b) stime
     */
    private void handleTHST(Client client, ByteBuffer data) {
        int headerLen, bytesPerEntry, extraPerEntry;
        int threadCount;

        headerLen = (data.get() & 0xff);
        bytesPerEntry = (data.get() & 0xff);
        threadCount = data.getShort();

        headerLen -= 4;     // we've read 4 bytes
        while (headerLen-- > 0)
            data.get();

        extraPerEntry = bytesPerEntry - 18;     // we want 18 bytes

        Log.v("ddm-thread", "THST: threadCount=" + threadCount);

        /*
         * For each thread, extract the data, find the appropriate
         * client, and add it to the ClientData.
         */
        for (int i = 0; i < threadCount; i++) {
            int threadId, status, tid, utime, stime;
            boolean isDaemon = false;

            threadId = data.getInt();
            status = data.get();
            tid = data.getInt();
            utime = data.getInt();
            stime = data.getInt();
            if (bytesPerEntry >= 18)
                isDaemon = (data.get() != 0);

            Log.v("ddm-thread", "  id=" + threadId
                + ", status=" + status + ", tid=" + tid
                + ", utime=" + utime + ", stime=" + stime);

            ClientData cd = client.getClientData();
            ThreadInfo threadInfo = cd.getThread(threadId);
            if (threadInfo != null)
                threadInfo.updateThread(status, tid, utime, stime, isDaemon);
            else
                Log.d("ddms", "Thread with id=" + threadId + " not found");

            // slurp up any extra
            for (int slurp = extraPerEntry; slurp > 0; slurp--)
                data.get();
        }

        client.update(Client.CHANGE_THREAD_DATA);
    }

    /*
     * Handle a THNM (THread NaMe) message.  We get one of these after
     * somebody calls Thread.setName() on a running thread.
     */
    private void handleTHNM(Client client, ByteBuffer data) {
        int threadId, nameLen;
        String name;

        threadId = data.getInt();
        nameLen = data.getInt();
        name = getString(data, nameLen);

        Log.v("ddm-thread", "THNM: " + threadId + " '" + name + "'");

        ThreadInfo threadInfo = client.getClientData().getThread(threadId);
        if (threadInfo != null) {
            threadInfo.setThreadName(name);
            client.update(Client.CHANGE_THREAD_DATA);
        } else {
            Log.d("ddms", "Thread with id=" + threadId + " not found");
        }
    }


    /**
     * Parse an incoming STKL.
     */
    private void handleSTKL(Client client, ByteBuffer data) {
        StackTraceElement[] trace;
        int i, threadId, stackDepth;
        @SuppressWarnings("unused")
        int future;

        future = data.getInt();
        threadId = data.getInt();

        Log.v("ddms", "STKL: " + threadId);

        /* un-serialize the StackTraceElement[] */
        stackDepth = data.getInt();
        trace = new StackTraceElement[stackDepth];
        for (i = 0; i < stackDepth; i++) {
            String className, methodName, fileName;
            int len, lineNumber;

            len = data.getInt();
            className = getString(data, len);
            len = data.getInt();
            methodName = getString(data, len);
            len = data.getInt();
            if (len == 0) {
                fileName = null;
            } else {
                fileName = getString(data, len);
            }
            lineNumber = data.getInt();

            trace[i] = new StackTraceElement(className, methodName, fileName,
                        lineNumber);
        }

        ThreadInfo threadInfo = client.getClientData().getThread(threadId);
        if (threadInfo != null) {
            threadInfo.setStackCall(trace);
            client.update(Client.CHANGE_THREAD_STACKTRACE);
        } else {
            Log.d("STKL", String.format(
                    "Got stackcall for thread %1$d, which does not exists (anymore?).", //$NON-NLS-1$
                    threadId));
        }
    }


    /**
     * Send a THEN (THread notification ENable) request to the client.
     */
    public static void sendTHEN(Client client, boolean enable)
        throws IOException {

        ByteBuffer rawBuf = allocBuffer(1);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        if (enable)
            buf.put((byte)1);
        else
            buf.put((byte)0);

        finishChunkPacket(packet, CHUNK_THEN, buf.position());
        Log.d("ddm-thread", "Sending " + name(CHUNK_THEN) + ": " + enable);
        client.sendAndConsume(packet, mInst);
    }


    /**
     * Send a STKL (STacK List) request to the client.  The VM will suspend
     * the target thread, obtain its stack, and return it.  If the thread
     * is no longer running, a failure result will be returned.
     */
    public static void sendSTKL(Client client, int threadId)
        throws IOException {

        if (false) {
            Log.d("ddm-thread", "would send STKL " + threadId);
            return;
        }

        ByteBuffer rawBuf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(threadId);

        finishChunkPacket(packet, CHUNK_STKL, buf.position());
        Log.d("ddm-thread", "Sending " + name(CHUNK_STKL) + ": " + threadId);
        client.sendAndConsume(packet, mInst);
    }


    /**
     * This is called periodically from the UI thread.  To avoid locking
     * the UI while we request the updates, we create a new thread.
     *
     */
    static void requestThreadUpdate(final Client client) {
        if (client.isDdmAware() && client.isThreadUpdateEnabled()) {
            if (mThreadStatusReqRunning) {
                Log.w("ddms", "Waiting for previous thread update req to finish");
                return;
            }

            new Thread("Thread Status Req") {
                @Override
                public void run() {
                    mThreadStatusReqRunning = true;
                    try {
                        sendTHST(client);
                    } catch (IOException ioe) {
                        Log.d("ddms", "Unable to request thread updates from "
                                + client + ": " + ioe.getMessage());
                    } finally {
                        mThreadStatusReqRunning = false;
                    }
                }
            }.start();
        }
    }

    static void requestThreadStackCallRefresh(final Client client, final int threadId) {
        if (client.isDdmAware() && client.isThreadUpdateEnabled()) {
            if (mThreadStackTraceReqRunning ) {
                Log.w("ddms", "Waiting for previous thread stack call req to finish");
                return;
            }

            new Thread("Thread Status Req") {
                @Override
                public void run() {
                    mThreadStackTraceReqRunning = true;
                    try {
                        sendSTKL(client, threadId);
                    } catch (IOException ioe) {
                        Log.d("ddms", "Unable to request thread stack call updates from "
                                + client + ": " + ioe.getMessage());
                    } finally {
                        mThreadStackTraceReqRunning = false;
                    }
                }
            }.start();
        }

    }

    /*
     * Send a THST request to the specified client.
     */
    private static void sendTHST(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // nothing much to say

        finishChunkPacket(packet, CHUNK_THST, buf.position());
        Log.d("ddm-thread", "Sending " + name(CHUNK_THST));
        client.sendAndConsume(packet, mInst);
    }
}

