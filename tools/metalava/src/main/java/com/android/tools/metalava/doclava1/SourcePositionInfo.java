/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.tools.metalava.doclava1;

// Copied from doclava1
public class SourcePositionInfo implements Comparable {
    public static final SourcePositionInfo UNKNOWN = new SourcePositionInfo("(unknown)", 0, 0);

    public SourcePositionInfo(String file, int line, int column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }

    /**
     * Given this position and str which occurs at that position, as well as str an index into str,
     * find the SourcePositionInfo.
     *
     * @throw StringIndexOutOfBoundsException if index &gt; str.length()
     */
    public static SourcePositionInfo add(SourcePositionInfo that, String str, int index) {
        if (that == null) {
            return null;
        }
        int line = that.line;
        char prev = 0;
        for (int i = 0; i < index; i++) {
            char c = str.charAt(i);
            if (c == '\r' || (c == '\n' && prev != '\r')) {
                line++;
            }
            prev = c;
        }
        return new SourcePositionInfo(that.file, line, 0);
    }

    @Override
    public String toString() {
        return file + ':' + line;
    }

    public int compareTo(Object o) {
        SourcePositionInfo that = (SourcePositionInfo) o;
        int r = this.file.compareTo(that.file);
        if (r != 0) return r;
        return this.line - that.line;
    }

    public String file;
    public int line;
    public int column;
}
