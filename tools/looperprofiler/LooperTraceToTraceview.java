/*
 * Copyright (C) 2011 The Android Open Source Project
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LooperTraceToTraceview {
    private static final int CURRENT_VERSION = 1;

    private static class Entry {
        int traceId;
        long wallStart;
        long wallTime;
        long threadStart;
        long threadTime;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java LooperTraceToTraceview <filename> <output>");
            System.exit(0);
        }

        File inputFile = new File(args[0]);
        FileInputStream fis = new FileInputStream(inputFile);
        DataInputStream in = new DataInputStream(new BufferedInputStream(fis));

        int version = in.readInt();
        if (version != CURRENT_VERSION) {
            System.out.println("Only traces of version " + CURRENT_VERSION + " are supported");
            System.exit(1);
        }

        long wallStart = in.readLong();
        long threadStart = in.readLong();

        int count = in.readInt();
        HashMap<String, Integer> names = new HashMap<String, Integer>(count);
        for (int i = 0; i < count; i++) {
            readName(in, names);
        }

        count = in.readInt();
        ArrayList<Entry> entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) {
            entries.add(readEntry(in));
        }

        File outputFile = new File(args[1]);
        FileOutputStream fos = new FileOutputStream(outputFile);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos));

        writeHeader(out, wallStart, names, entries);
        out.flush();

        writeTraces(fos, out.size(), wallStart, threadStart, entries);

        out.close();
    }

    private static void writeTraces(FileOutputStream out, long offset, long wallStart,
            long threadStart, ArrayList<Entry> entries) throws IOException {

        FileChannel channel = out.getChannel();

        // Header
        ByteBuffer buffer = ByteBuffer.allocateDirect(32);
        buffer.put("SLOW".getBytes());
        buffer = buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) 3);    // version
        buffer.putShort((short) 32);   // offset to data
        buffer.putLong(wallStart);     // start time in usec
        buffer.putShort((short) 14);   // size of a record in bytes
        // padding to 32 bytes
        for (int i = 0; i < 32 - 18; i++) buffer.put((byte) 0);

        buffer.flip();
        channel.position(offset).write(buffer);
        
        buffer = ByteBuffer.allocateDirect(14).order(ByteOrder.LITTLE_ENDIAN);
        for (Entry entry : entries) {
            buffer.putShort((short) 1);
            buffer.putInt(entry.traceId); // entering method
            buffer.putInt((int) (entry.threadStart - threadStart));
            buffer.putInt((int) (entry.wallStart - wallStart));

            buffer.flip();
            channel.write(buffer);
            buffer.clear();

            buffer.putShort((short) 1);
            buffer.putInt(entry.traceId | 0x1); // exiting method
            buffer.putInt((int) (entry.threadStart + entry.threadTime - threadStart));
            buffer.putInt((int) (entry.wallStart + entry.wallTime - wallStart));

            buffer.flip();
            channel.write(buffer);
            buffer.clear();
        }

        channel.close();
    }

    private static void writeHeader(DataOutputStream out, long start,
            HashMap<String, Integer> names, ArrayList<Entry> entries) throws IOException {

        Entry last = entries.get(entries.size() - 1);
        long wallTotal = (last.wallStart + last.wallTime) - start;

        startSection("version", out);
        addValue(null, "3", out);
        addValue("data-file-overflow", "false", out);
        addValue("clock", "dual", out);
        addValue("elapsed-time-usec", Long.toString(wallTotal), out);
        addValue("num-method-calls", Integer.toString(entries.size()), out);
        addValue("clock-call-overhead-nsec", "1", out);
        addValue("vm", "dalvik", out);

        startSection("threads", out);
        addThreadId(1, "main", out);

        startSection("methods", out);
        addMethods(names, out);

        startSection("end", out);
    }

    private static void addMethods(HashMap<String, Integer> names, DataOutputStream out)
            throws IOException {

        for (Map.Entry<String, Integer> name : names.entrySet()) {
            out.writeBytes("0x" + String.format("%08x", name.getValue()) + "\tEventQueue\t" +
                    name.getKey().replace('$', '_') + "\t()V\tLooper.java\t-2\n");
        }
    }

    private static void addThreadId(int id, String name, DataOutputStream out) throws IOException {
        out.writeBytes(Integer.toString(id) + '\t' + name + '\n');
    }

    private static void addValue(String name, String value, DataOutputStream out)
            throws IOException {

        if (name != null) {
            out.writeBytes(name + "=");
        }
        out.writeBytes(value + '\n');
    }

    private static void startSection(String name, DataOutputStream out) throws IOException {
        out.writeBytes("*" + name + '\n');
    }

    private static void readName(DataInputStream in, HashMap<String, Integer> names)
            throws IOException {
        int id = 0x56000000 | (in.readShort() << 2);
        String name = in.readUTF();
        names.put(name, id);
    }

    private static Entry readEntry(DataInputStream in) throws IOException {
        Entry entry = new Entry();
        entry.traceId = 0x56000000 | (in.readShort() << 2);
        entry.wallStart = in.readLong();
        entry.wallTime = in.readLong();
        entry.threadStart = in.readLong();
        entry.threadTime = in.readLong();
        return entry;
    }
}
