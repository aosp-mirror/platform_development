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
 * Handle the "hello" chunk (HELO) and feature discovery.
 */
final class HandleHello extends ChunkHandler {

    public static final int CHUNK_HELO = ChunkHandler.type("HELO");
    public static final int CHUNK_FEAT = ChunkHandler.type("FEAT");

    private static final HandleHello mInst = new HandleHello();

    private HandleHello() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_HELO, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {
        Log.d("ddm-hello", "Now ready: " + client);
    }

    /**
     * Client went away.
     */
    @Override
    public void clientDisconnected(Client client) {
        Log.d("ddm-hello", "Now disconnected: " + client);
    }

    /**
     * Sends HELLO-type commands to the VM after a good handshake.
     * @param client
     * @param serverProtocolVersion
     * @throws IOException
     */
    public static void sendHelloCommands(Client client, int serverProtocolVersion)
            throws IOException {
        sendHELO(client, serverProtocolVersion);
        sendFEAT(client);
        HandleProfiling.sendMPRQ(client);
    }

    /**
     * Chunk handler entry point.
     */
    @Override
    public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {

        Log.d("ddm-hello", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_HELO) {
            assert isReply;
            handleHELO(client, data);
        } else if (type == CHUNK_FEAT) {
            handleFEAT(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /*
     * Handle a reply to our HELO message.
     */
    private static void handleHELO(Client client, ByteBuffer data) {
        int version, pid, vmIdentLen, appNameLen;
        String vmIdent, appName;

        version = data.getInt();
        pid = data.getInt();
        vmIdentLen = data.getInt();
        appNameLen = data.getInt();

        vmIdent = getString(data, vmIdentLen);
        appName = getString(data, appNameLen);

        Log.d("ddm-hello", "HELO: v=" + version + ", pid=" + pid
            + ", vm='" + vmIdent + "', app='" + appName + "'");

        ClientData cd = client.getClientData();

        synchronized (cd) {
            if (cd.getPid() == pid) {
                cd.setVmIdentifier(vmIdent);
                cd.setClientDescription(appName);
                cd.isDdmAware(true);
            } else {
                Log.e("ddm-hello", "Received pid (" + pid + ") does not match client pid ("
                        + cd.getPid() + ")");
            }
        }

        client = checkDebuggerPortForAppName(client, appName);

        if (client != null) {
            client.update(Client.CHANGE_NAME);
        }
    }


    /**
     * Send a HELO request to the client.
     */
    public static void sendHELO(Client client, int serverProtocolVersion)
        throws IOException
    {
        ByteBuffer rawBuf = allocBuffer(4);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(serverProtocolVersion);

        finishChunkPacket(packet, CHUNK_HELO, buf.position());
        Log.d("ddm-hello", "Sending " + name(CHUNK_HELO)
            + " ID=0x" + Integer.toHexString(packet.getId()));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Handle a reply to our FEAT request.
     */
    private static void handleFEAT(Client client, ByteBuffer data) {
        int featureCount;
        int i;

        featureCount = data.getInt();
        for (i = 0; i < featureCount; i++) {
            int len = data.getInt();
            String feature = getString(data, len);
            client.getClientData().addFeature(feature);

            Log.d("ddm-hello", "Feature: " + feature);
        }
    }

    /**
     * Send a FEAT request to the client.
     */
    public static void sendFEAT(Client client) throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_FEAT, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_FEAT));
        client.sendAndConsume(packet, mInst);
    }
}

