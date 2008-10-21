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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Handle thread status updates.
 */
final class HandleNativeHeap extends ChunkHandler {

    public static final int CHUNK_NHGT = type("NHGT"); // $NON-NLS-1$
    public static final int CHUNK_NHSG = type("NHSG"); // $NON-NLS-1$
    public static final int CHUNK_NHST = type("NHST"); // $NON-NLS-1$
    public static final int CHUNK_NHEN = type("NHEN"); // $NON-NLS-1$

    private static final HandleNativeHeap mInst = new HandleNativeHeap();

    private HandleNativeHeap() {
    }


    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_NHGT, mInst);
        mt.registerChunkHandler(CHUNK_NHSG, mInst);
        mt.registerChunkHandler(CHUNK_NHST, mInst);
        mt.registerChunkHandler(CHUNK_NHEN, mInst);
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
    public void handleChunk(Client client, int type, ByteBuffer data, boolean isReply, int msgId) {

        Log.d("ddm-nativeheap", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_NHGT) {
            handleNHGT(client, data);
        } else if (type == CHUNK_NHST) {
            // start chunk before any NHSG chunk(s)
            client.getClientData().getNativeHeapData().clearHeapData();
        } else if (type == CHUNK_NHEN) {
            // end chunk after NHSG chunk(s)
            client.getClientData().getNativeHeapData().sealHeapData();
        } else if (type == CHUNK_NHSG) {
            handleNHSG(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }

        client.update(Client.CHANGE_NATIVE_HEAP_DATA);
    }

    /**
     * Send an NHGT (Native Thread GeT) request to the client.
     */
    public static void sendNHGT(Client client) throws IOException {

        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data in request message

        finishChunkPacket(packet, CHUNK_NHGT, buf.position());
        Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHGT));
        client.sendAndConsume(packet, mInst);

        rawBuf = allocBuffer(2);
        packet = new JdwpPacket(rawBuf);
        buf = getChunkDataBuf(rawBuf);

        buf.put((byte)HandleHeap.WHEN_GC);
        buf.put((byte)HandleHeap.WHAT_OBJ);

        finishChunkPacket(packet, CHUNK_NHSG, buf.position());
        Log.d("ddm-nativeheap", "Sending " + name(CHUNK_NHSG));
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle our native heap data.
     */
    private void handleNHGT(Client client, ByteBuffer data) {
        ClientData cd = client.getClientData();

        Log.d("ddm-nativeheap", "NHGT: " + data.limit() + " bytes");

        // TODO - process incoming data and save in "cd"
        byte[] copy = new byte[data.limit()];
        data.get(copy);

        // clear the previous run
        cd.clearNativeAllocationInfo();

        ByteBuffer buffer = ByteBuffer.wrap(copy);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

//        read the header
//        typedef struct Header {
//            uint32_t mapSize;
//            uint32_t allocSize;
//            uint32_t allocInfoSize;
//            uint32_t totalMemory;
//              uint32_t backtraceSize;
//        };

        int mapSize = buffer.getInt();
        int allocSize = buffer.getInt();
        int allocInfoSize = buffer.getInt();
        int totalMemory = buffer.getInt();
        int backtraceSize = buffer.getInt();

        Log.d("ddms", "mapSize: " + mapSize);
        Log.d("ddms", "allocSize: " + allocSize);
        Log.d("ddms", "allocInfoSize: " + allocInfoSize);
        Log.d("ddms", "totalMemory: " + totalMemory);

        cd.setTotalNativeMemory(totalMemory);

        // this means that updates aren't turned on.
        if (allocInfoSize == 0)
          return;

        if (mapSize > 0) {
            byte[] maps = new byte[mapSize];
            buffer.get(maps, 0, mapSize);
            parseMaps(cd, maps);
        }

        int iterations = allocSize / allocInfoSize;

        for (int i = 0 ; i < iterations ; i++) {
            NativeAllocationInfo info = new NativeAllocationInfo(
                    buffer.getInt() /* size */,
                    buffer.getInt() /* allocations */);

            for (int j = 0 ; j < backtraceSize ; j++) {
                long addr = ((long)buffer.getInt()) & 0x00000000ffffffffL;

                info.addStackCallAddress(addr);;
            }

            cd.addNativeAllocation(info);
        }
    }

    private void handleNHSG(Client client, ByteBuffer data) {
        byte dataCopy[] = new byte[data.limit()];
        data.rewind();
        data.get(dataCopy);
        data = ByteBuffer.wrap(dataCopy);
        client.getClientData().getNativeHeapData().addHeapData(data);

        if (true) {
            return;
        }

        // WORK IN PROGRESS

//        Log.e("ddm-nativeheap", "NHSG: ----------------------------------");
//        Log.e("ddm-nativeheap", "NHSG: " + data.limit() + " bytes");

        byte[] copy = new byte[data.limit()];
        data.get(copy);

        ByteBuffer buffer = ByteBuffer.wrap(copy);
        buffer.order(ByteOrder.BIG_ENDIAN);

        int id = buffer.getInt();
        int unitsize = (int) buffer.get();
        long startAddress = (long) buffer.getInt() & 0x00000000ffffffffL;
        int offset = buffer.getInt();
        int allocationUnitCount = buffer.getInt();

//        Log.e("ddm-nativeheap", "id: " + id);
//        Log.e("ddm-nativeheap", "unitsize: " + unitsize);
//        Log.e("ddm-nativeheap", "startAddress: 0x" + Long.toHexString(startAddress));
//        Log.e("ddm-nativeheap", "offset: " + offset);
//        Log.e("ddm-nativeheap", "allocationUnitCount: " + allocationUnitCount);
//        Log.e("ddm-nativeheap", "end: 0x" +
//                Long.toHexString(startAddress + unitsize * allocationUnitCount));

        // read the usage
        while (buffer.position() < buffer.limit()) {
            int eState = (int)buffer.get() & 0x000000ff;
            int eLen = ((int)buffer.get() & 0x000000ff) + 1;
            //Log.e("ddm-nativeheap", "solidity: " + (eState & 0x7) + " - kind: "
            //        + ((eState >> 3) & 0x7) + " - len: " + eLen);
        }


//        count += unitsize * allocationUnitCount;
//        Log.e("ddm-nativeheap", "count = " + count);

    }

    private void parseMaps(ClientData cd, byte[] maps) {
        InputStreamReader input = new InputStreamReader(new ByteArrayInputStream(maps));
        BufferedReader reader = new BufferedReader(input);

        String line;

        try {

            // most libraries are defined on several lines, so we need to make sure we parse
            // all the library lines and only add the library at the end
            long startAddr = 0;
            long endAddr = 0;
            String library = null;

            while ((line = reader.readLine()) != null) {
                Log.d("ddms", "line: " + line);
                if (line.length() < 16) {
                    continue;
                }

                try {
                    long tmpStart = Long.parseLong(line.substring(0, 8), 16);
                    long tmpEnd = Long.parseLong(line.substring(9, 17), 16);

                     /*
                      * only check for library addresses as defined in
                      * //device/config/prelink-linux-arm.map
                      */
                    if (tmpStart >= 0x0000000080000000L && tmpStart <= 0x00000000BFFFFFFFL) {

                        int index = line.indexOf('/');

                        if (index == -1)
                            continue;

                        String tmpLib = line.substring(index);

                        if (library == null ||
                                (library != null && tmpLib.equals(library) == false)) {

                            if (library != null) {
                                cd.addNativeLibraryMapInfo(startAddr, endAddr, library);
                                Log.d("ddms", library + "(" + Long.toHexString(startAddr) +
                                        " - " + Long.toHexString(endAddr) + ")");
                            }

                            // now init the new library
                            library = tmpLib;
                            startAddr = tmpStart;
                            endAddr = tmpEnd;
                        } else {
                            // add the new end
                            endAddr = tmpEnd;
                        }
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            if (library != null) {
                cd.addNativeLibraryMapInfo(startAddr, endAddr, library);
                Log.d("ddms", library + "(" + Long.toHexString(startAddr) +
                        " - " + Long.toHexString(endAddr) + ")");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

