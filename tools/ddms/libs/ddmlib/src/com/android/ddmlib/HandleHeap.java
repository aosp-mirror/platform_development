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

import com.android.ddmlib.ClientData.AllocationTrackingStatus;
import com.android.ddmlib.ClientData.IHprofDumpHandler;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Handle heap status updates.
 */
final class HandleHeap extends ChunkHandler {

    public static final int CHUNK_HPIF = type("HPIF");
    public static final int CHUNK_HPST = type("HPST");
    public static final int CHUNK_HPEN = type("HPEN");
    public static final int CHUNK_HPSG = type("HPSG");
    public static final int CHUNK_HPGC = type("HPGC");
    public static final int CHUNK_HPDU = type("HPDU");
    public static final int CHUNK_REAE = type("REAE");
    public static final int CHUNK_REAQ = type("REAQ");
    public static final int CHUNK_REAL = type("REAL");

    // args to sendHPSG
    public static final int WHEN_DISABLE = 0;
    public static final int WHEN_GC = 1;
    public static final int WHAT_MERGE = 0; // merge adjacent objects
    public static final int WHAT_OBJ = 1;   // keep objects distinct

    // args to sendHPIF
    public static final int HPIF_WHEN_NEVER = 0;
    public static final int HPIF_WHEN_NOW = 1;
    public static final int HPIF_WHEN_NEXT_GC = 2;
    public static final int HPIF_WHEN_EVERY_GC = 3;

    private static final HandleHeap mInst = new HandleHeap();

    private HandleHeap() {}

    /**
     * Register for the packets we expect to get from the client.
     */
    public static void register(MonitorThread mt) {
        mt.registerChunkHandler(CHUNK_HPIF, mInst);
        mt.registerChunkHandler(CHUNK_HPST, mInst);
        mt.registerChunkHandler(CHUNK_HPEN, mInst);
        mt.registerChunkHandler(CHUNK_HPSG, mInst);
        mt.registerChunkHandler(CHUNK_REAQ, mInst);
        mt.registerChunkHandler(CHUNK_REAL, mInst);
    }

    /**
     * Client is ready.
     */
    @Override
    public void clientReady(Client client) throws IOException {
        if (client.isHeapUpdateEnabled()) {
            //sendHPSG(client, WHEN_GC, WHAT_MERGE);
            sendHPIF(client, HPIF_WHEN_EVERY_GC);
        }
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
        Log.d("ddm-heap", "handling " + ChunkHandler.name(type));

        if (type == CHUNK_HPIF) {
            handleHPIF(client, data);
        } else if (type == CHUNK_HPST) {
            handleHPST(client, data);
        } else if (type == CHUNK_HPEN) {
            handleHPEN(client, data);
        } else if (type == CHUNK_HPSG) {
            handleHPSG(client, data);
        } else if (type == CHUNK_HPDU) {
            handleHPDU(client, data);
        } else if (type == CHUNK_REAQ) {
            handleREAQ(client, data);
        } else if (type == CHUNK_REAL) {
            handleREAL(client, data);
        } else {
            handleUnknownChunk(client, type, data, isReply, msgId);
        }
    }

    /*
     * Handle a heap info message.
     */
    private void handleHPIF(Client client, ByteBuffer data) {
        Log.d("ddm-heap", "HPIF!");
        try {
            int numHeaps = data.getInt();

            for (int i = 0; i < numHeaps; i++) {
                int heapId = data.getInt();
                @SuppressWarnings("unused")
                long timeStamp = data.getLong();
                @SuppressWarnings("unused")
                byte reason = data.get();
                long maxHeapSize = (long)data.getInt() & 0x00ffffffff;
                long heapSize = (long)data.getInt() & 0x00ffffffff;
                long bytesAllocated = (long)data.getInt() & 0x00ffffffff;
                long objectsAllocated = (long)data.getInt() & 0x00ffffffff;

                client.getClientData().setHeapInfo(heapId, maxHeapSize,
                        heapSize, bytesAllocated, objectsAllocated);
                client.update(Client.CHANGE_HEAP_DATA);
            }
        } catch (BufferUnderflowException ex) {
            Log.w("ddm-heap", "malformed HPIF chunk from client");
        }
    }

    /**
     * Send an HPIF (HeaP InFo) request to the client.
     */
    public static void sendHPIF(Client client, int when) throws IOException {
        ByteBuffer rawBuf = allocBuffer(1);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte)when);

        finishChunkPacket(packet, CHUNK_HPIF, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPIF) + ": when=" + when);
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle a heap segment series start message.
     */
    private void handleHPST(Client client, ByteBuffer data) {
        /* Clear out any data that's sitting around to
         * get ready for the chunks that are about to come.
         */
//xxx todo: only clear data that belongs to the heap mentioned in <data>.
        client.getClientData().getVmHeapData().clearHeapData();
    }

    /*
     * Handle a heap segment series end message.
     */
    private void handleHPEN(Client client, ByteBuffer data) {
        /* Let the UI know that we've received all of the
         * data for this heap.
         */
//xxx todo: only seal data that belongs to the heap mentioned in <data>.
        client.getClientData().getVmHeapData().sealHeapData();
        client.update(Client.CHANGE_HEAP_DATA);
    }

    /*
     * Handle a heap segment message.
     */
    private void handleHPSG(Client client, ByteBuffer data) {
        byte dataCopy[] = new byte[data.limit()];
        data.rewind();
        data.get(dataCopy);
        data = ByteBuffer.wrap(dataCopy);
        client.getClientData().getVmHeapData().addHeapData(data);
//xxx todo: add to the heap mentioned in <data>
    }

    /**
     * Sends an HPSG (HeaP SeGment) request to the client.
     */
    public static void sendHPSG(Client client, int when, int what)
        throws IOException {

        ByteBuffer rawBuf = allocBuffer(2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte)when);
        buf.put((byte)what);

        finishChunkPacket(packet, CHUNK_HPSG, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPSG) + ": when="
            + when + ", what=" + what);
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends an HPGC request to the client.
     */
    public static void sendHPGC(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_HPGC, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPGC));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends an HPDU request to the client.
     *
     * We will get an HPDU response when the heap dump has completed.  On
     * failure we get a generic failure response.
     *
     * @param fileName name of output file (on device)
     */
    public static void sendHPDU(Client client, String fileName)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(4 + fileName.length() * 2);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.putInt(fileName.length());
        putString(buf, fileName);

        finishChunkPacket(packet, CHUNK_HPDU, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_HPDU) + " '" + fileName +"'");
        client.sendAndConsume(packet, mInst);
        client.getClientData().setPendingHprofDump(fileName);
    }

    /*
     * Handle notification of completion of a HeaP DUmp.
     */
    private void handleHPDU(Client client, ByteBuffer data) {
        byte result;

        // get the filename and make the client not have pending HPROF dump anymore.
        String filename = client.getClientData().getPendingHprofDump();
        client.getClientData().setPendingHprofDump(null);

        // get the dump result
        result = data.get();

        // get the app-level handler for HPROF dump
        IHprofDumpHandler handler = ClientData.getHprofDumpHandler();
        if (handler != null) {
            if (result == 0) {
                handler.onSuccess(filename, client);

                Log.d("ddm-heap", "Heap dump request has finished");
            } else {
                handler.onFailure(client);
                Log.w("ddm-heap", "Heap dump request failed (check device log)");
            }
        }
    }

    /**
     * Sends a REAE (REcent Allocation Enable) request to the client.
     */
    public static void sendREAE(Client client, boolean enable)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(1);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        buf.put((byte) (enable ? 1 : 0));

        finishChunkPacket(packet, CHUNK_REAE, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAE) + ": " + enable);
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends a REAQ (REcent Allocation Query) request to the client.
     */
    public static void sendREAQ(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_REAQ, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAQ));
        client.sendAndConsume(packet, mInst);
    }

    /**
     * Sends a REAL (REcent ALlocation) request to the client.
     */
    public static void sendREAL(Client client)
        throws IOException {
        ByteBuffer rawBuf = allocBuffer(0);
        JdwpPacket packet = new JdwpPacket(rawBuf);
        ByteBuffer buf = getChunkDataBuf(rawBuf);

        // no data

        finishChunkPacket(packet, CHUNK_REAL, buf.position());
        Log.d("ddm-heap", "Sending " + name(CHUNK_REAL));
        client.sendAndConsume(packet, mInst);
    }

    /*
     * Handle the response from our REcent Allocation Query message.
     */
    private void handleREAQ(Client client, ByteBuffer data) {
        boolean enabled;

        enabled = (data.get() != 0);
        Log.d("ddm-heap", "REAQ says: enabled=" + enabled);

        client.getClientData().setAllocationStatus(enabled ?
                AllocationTrackingStatus.ON : AllocationTrackingStatus.OFF);
        client.update(Client.CHANGE_HEAP_ALLOCATION_STATUS);
    }

    /**
     * Converts a VM class descriptor string ("Landroid/os/Debug;") to
     * a dot-notation class name ("android.os.Debug").
     */
    private String descriptorToDot(String str) {
        // count the number of arrays.
        int array = 0;
        while (str.startsWith("[")) {
            str = str.substring(1);
            array++;
        }

        int len = str.length();

        /* strip off leading 'L' and trailing ';' if appropriate */
        if (len >= 2 && str.charAt(0) == 'L' && str.charAt(len - 1) == ';') {
            str = str.substring(1, len-1);
            str = str.replace('/', '.');
        } else {
            // convert the basic types
            if ("C".equals(str)) {
                str = "char";
            } else if ("B".equals(str)) {
                str = "byte";
            } else if ("Z".equals(str)) {
                str = "boolean";
            } else if ("S".equals(str)) {
                str = "short";
            } else if ("I".equals(str)) {
                str = "int";
            } else if ("J".equals(str)) {
                str = "long";
            } else if ("F".equals(str)) {
                str = "float";
            } else if ("D".equals(str)) {
                str = "double";
            }
        }

        // now add the array part
        for (int a = 0 ; a < array; a++) {
            str = str + "[]";
        }

        return str;
    }

    /**
     * Reads a string table out of "data".
     *
     * This is just a serial collection of strings, each of which is a
     * four-byte length followed by UTF-16 data.
     */
    private void readStringTable(ByteBuffer data, String[] strings) {
        int count = strings.length;
        int i;

        for (i = 0; i < count; i++) {
            int nameLen = data.getInt();
            String descriptor = getString(data, nameLen);
            strings[i] = descriptorToDot(descriptor);
        }
    }

    /*
     * Handle a REcent ALlocation response.
     *
     * Message header (all values big-endian):
     *   (1b) message header len (to allow future expansion); includes itself
     *   (1b) entry header len
     *   (1b) stack frame len
     *   (2b) number of entries
     *   (4b) offset to string table from start of message
     *   (2b) number of class name strings
     *   (2b) number of method name strings
     *   (2b) number of source file name strings
     *   For each entry:
     *     (4b) total allocation size
     *     (2b) threadId
     *     (2b) allocated object's class name index
     *     (1b) stack depth
     *     For each stack frame:
     *       (2b) method's class name
     *       (2b) method name
     *       (2b) method source file
     *       (2b) line number, clipped to 32767; -2 if native; -1 if no source
     *   (xb) class name strings
     *   (xb) method name strings
     *   (xb) source file strings
     *
     *   As with other DDM traffic, strings are sent as a 4-byte length
     *   followed by UTF-16 data.
     */
    private void handleREAL(Client client, ByteBuffer data) {
        Log.e("ddm-heap", "*** Received " + name(CHUNK_REAL));
        int messageHdrLen, entryHdrLen, stackFrameLen;
        int numEntries, offsetToStrings;
        int numClassNames, numMethodNames, numFileNames;

        /*
         * Read the header.
         */
        messageHdrLen = (data.get() & 0xff);
        entryHdrLen = (data.get() & 0xff);
        stackFrameLen = (data.get() & 0xff);
        numEntries = (data.getShort() & 0xffff);
        offsetToStrings = data.getInt();
        numClassNames = (data.getShort() & 0xffff);
        numMethodNames = (data.getShort() & 0xffff);
        numFileNames = (data.getShort() & 0xffff);


        /*
         * Skip forward to the strings and read them.
         */
        data.position(offsetToStrings);

        String[] classNames = new String[numClassNames];
        String[] methodNames = new String[numMethodNames];
        String[] fileNames = new String[numFileNames];

        readStringTable(data, classNames);
        readStringTable(data, methodNames);
        //System.out.println("METHODS: "
        //    + java.util.Arrays.deepToString(methodNames));
        readStringTable(data, fileNames);

        /*
         * Skip back to a point just past the header and start reading
         * entries.
         */
        data.position(messageHdrLen);

        ArrayList<AllocationInfo> list = new ArrayList<AllocationInfo>(numEntries);
        for (int i = 0; i < numEntries; i++) {
            int totalSize;
            int threadId, classNameIndex, stackDepth;

            totalSize = data.getInt();
            threadId = (data.getShort() & 0xffff);
            classNameIndex = (data.getShort() & 0xffff);
            stackDepth = (data.get() & 0xff);
            /* we've consumed 9 bytes; gobble up any extra */
            for (int skip = 9; skip < entryHdrLen; skip++)
                data.get();

            StackTraceElement[] steArray = new StackTraceElement[stackDepth];

            /*
             * Pull out the stack trace.
             */
            for (int sti = 0; sti < stackDepth; sti++) {
                int methodClassNameIndex, methodNameIndex;
                int methodSourceFileIndex;
                short lineNumber;
                String methodClassName, methodName, methodSourceFile;

                methodClassNameIndex = (data.getShort() & 0xffff);
                methodNameIndex = (data.getShort() & 0xffff);
                methodSourceFileIndex = (data.getShort() & 0xffff);
                lineNumber = data.getShort();

                methodClassName = classNames[methodClassNameIndex];
                methodName = methodNames[methodNameIndex];
                methodSourceFile = fileNames[methodSourceFileIndex];

                steArray[sti] = new StackTraceElement(methodClassName,
                    methodName, methodSourceFile, lineNumber);

                /* we've consumed 8 bytes; gobble up any extra */
                for (int skip = 9; skip < stackFrameLen; skip++)
                    data.get();
            }

            list.add(new AllocationInfo(classNames[classNameIndex],
                totalSize, (short) threadId, steArray));
        }

        // sort biggest allocations first.
        Collections.sort(list);

        client.getClientData().setAllocations(list.toArray(new AllocationInfo[numEntries]));
        client.update(Client.CHANGE_HEAP_ALLOCATIONS);
    }

    /*
     * For debugging: dump the contents of an AllocRecord array.
     *
     * The array starts with the oldest known allocation and ends with
     * the most recent allocation.
     */
    @SuppressWarnings("unused")
    private static void dumpRecords(AllocationInfo[] records) {
        System.out.println("Found " + records.length + " records:");

        for (AllocationInfo rec: records) {
            System.out.println("tid=" + rec.getThreadId() + " "
                + rec.getAllocatedClass() + " (" + rec.getSize() + " bytes)");

            for (StackTraceElement ste: rec.getStackTrace()) {
                if (ste.isNativeMethod()) {
                    System.out.println("    " + ste.getClassName()
                        + "." + ste.getMethodName()
                        + " (Native method)");
                } else {
                    System.out.println("    " + ste.getClassName()
                        + "." + ste.getMethodName()
                        + " (" + ste.getFileName()
                        + ":" + ste.getLineNumber() + ")");
                }
            }
        }
    }

}

