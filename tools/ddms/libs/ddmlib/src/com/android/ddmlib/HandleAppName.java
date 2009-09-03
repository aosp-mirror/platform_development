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
 * Handle the "app name" chunk (APNM).
 */
final class HandleAppName extends ChunkHandler {

    public static final int CHUNK_APNM = ChunkHandler.type("APNM");

    private static final HandleAppName mInst = new HandleAppName();


    private HandleAppName() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_APNM, mInst);
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

        Log.d("ddm-appname", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_APNM) {
            assert !isReply;
            handleAPNM(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /*
     * Handle a reply to our APNM message.
     */
    private static void handleAPNM(Client client, ByteBuffer data) {
        int appNameLen;
        String appName;

        appNameLen = data.getInt();
        appName = getString(data, appNameLen);

        Log.d("ddm-appname", "APNM: app='" + appName + "'");

        ClientData cd = client.getClientData();
        synchronized (cd) {
            cd.setClientDescription(appName);
        }

        client = checkDebuggerPortForAppName(client, appName);

        if (client != null) {
            client.update(Client.CHANGE_NAME);
        }
    }
 }

