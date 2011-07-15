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
 
import java.io.*;
import java.util.*;

public class LooperTraceToJSON {
    private static final int CURRENT_VERSION = 1;

    private static class Entry {
        int messageId;
        String name;
        long wallStart;
        long wallTime;
        long threadTime;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java LooperTraceToJSON [variablename] <filename>");
            System.exit(0);
        }

        String var = "";

        int fileIndex = 0;
        if (args.length > 1) {
            var = args[0];
            fileIndex++;
        }

        File inputFile = new File(args[fileIndex]);
        FileInputStream fis = new FileInputStream(inputFile);
        DataInputStream in = new DataInputStream(new BufferedInputStream(fis));

        int version = in.readInt();
        if (version != CURRENT_VERSION) {
            System.out.println("Only traces of version " + CURRENT_VERSION + " are supported");
            System.exit(1);
        }
        int count = in.readInt();

        ArrayList<Entry> entries = new ArrayList<Entry>(count);
        for (int i = 0; i < count; i++) {
            entries.add(readEntry(in));
        }

        String json = generateJSON(var, entries);
        System.out.println(json);
    }
    
    private static Entry readEntry(DataInputStream in) throws IOException {
        Entry entry = new Entry();
        entry.messageId = in.readInt();
        entry.name = in.readUTF();
        entry.wallStart = in.readLong();
        entry.wallTime = in.readLong();
        entry.threadTime = in.readLong();
        return entry;
    }
    
    private static String generateJSON(String var, ArrayList<Entry> entries) {
        StringBuilder buffer = new StringBuilder(256);
        if (var.length() > 0) {
            buffer.append("var ").append(var).append(" = ");
        }
        buffer.append("[\n");

        for (Entry entry : entries) {
            buffer.append("    { ");

            appendNumber(buffer, "messageId", entry.messageId);
            buffer.append(", ");
            
            appendName(buffer, entry.name);
            buffer.append(", ");

            appendNumber(buffer, "wallStart", entry.wallStart);
            buffer.append(", ");

            appendNumber(buffer, "wallTime", entry.wallTime);
            buffer.append(", ");

            appendNumber(buffer, "threadTime", entry.threadTime);
            buffer.append(" },\n");
        }

        buffer.append("];\n");
        return buffer.toString();
    }

    private static void appendName(StringBuilder buffer, String name) {
        buffer.append("\"name\": \"").append(name).append('"');
    }

    private static void appendNumber(StringBuilder buffer, String name, long number) {
        buffer.append('"').append(name).append("\": ").append(number);
    }
}
