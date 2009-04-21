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

package com.android.mkstubs.sourcer;

import java.io.IOException;
import java.io.Writer;

/**
 * An {@link Output} objects is an helper to write to a character stream {@link Writer}.
 * <p/>
 * It provide some helper methods to the various "sourcer" classes from this package
 * to help them write to the underlying stream.
 */
public class Output {
    
    private final Writer mWriter;

    /**
     * Creates a new {@link Output} object that wraps the given {@link Writer}.
     * <p/>
     * The caller is responsible of opening and closing the {@link Writer}.
     * 
     * @param writer The writer to write to. Could be a file, a string, etc.
     */
    public Output(Writer writer) {
        mWriter = writer;
    }

    /**
     * Writes a formatted string to the writer.
     * 
     * @param format The format string.
     * @param args The arguments for the format string.
     * 
     * @see String#format(String, Object...)
     */
    public void write(String format, Object... args) {
        try {
            mWriter.write(String.format(format, args));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes a single character to the writer.
     * 
     * @param c The character to write.
     */
    public void write(char c) {
        write(Character.toString(c));
    }
    
    /**
     * Writes a {@link StringBuilder} to the writer.
     * 
     * @param sb The {@link StringBuilder#toString()} method is used to ge the string to write.
     */
    public void write(StringBuilder sb) {
        write(sb.toString());
    }
}
