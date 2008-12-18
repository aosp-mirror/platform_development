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


import java.io.*;

/**
 * File utility methods.
 */
class Files {

    /**
     * Reads file into a string using default encoding.
     */
    static String toString(File file) throws IOException {
        char[] buffer = new char[0x1000]; // 4k
        int read;
        Reader in = new FileReader(file);
        StringBuilder builder = new StringBuilder();
        while ((read = in.read(buffer)) > -1) {
            builder.append(buffer, 0, read);
        }
        in.close();
        return builder.toString();
    }

    /**
     * Writes a string to a file using default encoding.
     */
    static void toFile(String contents, File file) throws IOException {
        FileWriter out = new FileWriter(file);
        out.write(contents);
        out.close();
    }
}