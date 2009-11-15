/*
 * Copyright (C) 2009 The Android Open Source Project
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

import com.android.ddmlib.ClientData.IMethodProfilingHandler;
import com.android.ddmlib.ClientData.MethodProfilingStatus;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Handle heap status updates.
 */
final class HandleProfiling extends ChunkHandler {

    public static final int CHUNK_MPRS = type("MPRS");
    public static final int CHUNK_MPRE = type("MPRE");
    public static final int CHUNK_MPRQ = type("MPRQ");
    public static final int CHUNK_FAIL = type("FAIL");

    private static final HandleProfiling mInst = new HandleProfiling();

    private HandleProfiling() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_MPRE, mInst);
        mt.registerChunkHandler(CHUNK_MPRQ, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {}

    /**
     * Client went away.
     */
    @Override
    public void clientDisconnected(Client client) {}

    /**
     * Chunk handler entry point.
     */
    @Override
    public void handleChunk(Client client, int type, ByteBuffer data,
        boolean isReply, int msgId) {

        Log.d("ddm-prof", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_MPRE) {
            handleMPRE(client, data);
        } else if (type == CHUNK_MPRQ) {
            handleMPRQ(client, data);
        } else if (type == CHUNK_FAIL) {
            handleFAIL(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /**
     * Send a MPRS (Method PRofiling Start) request to the client.
     *
     * The arguments to this method will eventually be passed to
     * android.os.Debug.startMethodTracing() on the device.
     *
     * @param fileName is the name of the file to which profiling data
     *          will be written (on the device); it will have ".trace"
     *          appended if necessary
     * @param bufferSize is the desired buffer size in bytes (8MB is good)
     * @param flags see startMethodTracing() docs; use 0 for default behavior
     */
    public static void sendMPRS(Client client, String fileName, int bufferSize,
        int flags) throws IOException {

        ByteBuffer rawBuf = allocBuffer(3*4 + fileName.length() * 2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(bufferSize);
        buf.putInt(flags);
        buf.putInt(fileName.length());
        putString(buf, fileName);

        finishChunkPacket(packet, CHUNK_MPRS, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRS) + " '" + fileName
            + "', size=" + bufferSize + ", flags=" + flags);
        client.sendAndConsume(packet, mInst);

        // record the filename we asked for.
        client.getClientData().setPendingMethodProfiling(fileName);

        // send a status query. this ensure that the status is properly updated if for some
        // reason starting the tracing failed.
        sendMPRQ(client);
    }

    /**
     * Send a MPRE (Method PRofiling End) request to the client.
     */
    public static void sendMPRE(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_MPRE, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRE));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Handle notification that method profiling has finished writing
     * data to disk.
     */
    private void handleMPRE(Client client, ByteBuffer data) {
        byte result;

        // get the filename and make the client not have pending HPROF dump anymore.
        String filename = client.getClientData().getPendingMethodProfiling();
        client.getClientData().setPendingMethodProfiling(null);

        result = data.get();

        // get the app-level handler for method tracing dump
        IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
        if (handler != null) {
            if (result == 0) {
                handler.onSuccess(filename, client);

                Log.d("ddm-prof", "Method profiling has finished");
            } else {
                handler.onFailure(client);

                Log.w("ddm-prof", "Method profiling has failed (check device log)");
            }
        }

        client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.OFF);
        client.update(Client.CHANGE_METHOD_PROFILING_STATUS);
    }

    /**
     * Send a MPRQ (Method PRofiling Query) request to the client.
     */
    public static void sendMPRQ(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_MPRQ, buf.position());
        Log.d("ddm-prof", "Sending " + name(CHUNK_MPRQ));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Receive response to query.
     */
    private void handleMPRQ(Client client, ByteBuffer data) {
        byte result;

        result = data.get();

        if (result == 0) {
            client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.OFF);
            Log.d("ddm-prof", "Method profiling is not running");
        } else {
            client.getClientData().setMethodProfilingStatus(MethodProfilingStatus.ON);
            Log.d("ddm-prof", "Method profiling is running");
        }
        client.update(Client.CHANGE_METHOD_PROFILING_STATUS);
    }

    private void handleFAIL(Client client, ByteBuffer data) {
        // this can be sent if MPRS failed (like wrong permission)

        String filename = client.getClientData().getPendingMethodProfiling();
        if (filename != null) {
            // reset the pending file.
            client.getClientData().setPendingMethodProfiling(null);

            // and notify of failure
            IMethodProfilingHandler handler = ClientData.getMethodProfilingHandler();
            if (handler != null) {
                handler.onFailure(client);
            }

        }

        // send a query to know the current status
        try {
            sendMPRQ(client);
        } catch (IOException e) {
            Log.e("HandleProfiling", e);
        }
    }
}

