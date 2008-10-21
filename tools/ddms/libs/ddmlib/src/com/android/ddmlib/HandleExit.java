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
 * Submit an exit request.
 */
final class HandleExit extends ChunkHandler {

    public static final int CHUNK_EXIT = type("EXIT");

    private static final HandleExit mInst = new HandleExit();


    private HandleExit() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {}

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
    public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {
        handleUnknownChunk(client, type, data, isReply, msgId);
    }

    /**
     * Send an EXIT request to the client.
     */
    public static void sendEXIT(Client client, int status)
        throws IOException
    {
        ByteBuffer rawBuf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(status);

        finishChunkPacket(packet, CHUNK_EXIT, buf.position());
        Log.d("ddm-exit", "Sending " + name(CHUNK_EXIT) + ": " + status);
        client.sendAndConsume(packet, mInst);
    }
}

