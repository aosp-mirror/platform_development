/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.bugreport.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A stream of parsed lines.  Can be rewound, and sub-regions cloned for 
 * recursive descent parsing.
 */
public class Lines<T extends Line> {
    private final ArrayList<? extends Line> mList;
    private final int mMin;
    private final int mMax;

    /**
     * The read position inside the list.
     */
    public int pos;

    /**
     * Read the whole file into a Lines object.
     */
    public static Lines<Line> readLines(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return Lines.readLines(reader);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     * Read the whole file into a Lines object.
     */
    public static Lines<Line> readLines(BufferedReader in) throws IOException {
        final ArrayList<Line> list = new ArrayList<Line>();

        int lineno = 0;
        String text;
        while ((text = in.readLine()) != null) {
            lineno++;
            list.add(new Line(lineno, text));
        }

        return new Lines<Line>(list);
    }
    
    /**
     * Construct with a list of lines.
     */
    public Lines(ArrayList<? extends Line> list) {
        this.mList = list;
        mMin = 0;
        mMax = mList.size();
    }

    /**
     * Construct with a list of lines, and a range inside that list.  The
     * read position will be set to min, so the new Lines can be read from
     * the beginning.
     */
    private Lines(ArrayList<? extends Line> list, int min, int max) {
        mList = list;
        mMin = min;
        mMax = max;
        this.pos = min;
    }

    /**
     * If there are more lines to read within the current range.
     */
    public boolean hasNext() {
        return pos < mMax;
    }

    /**
     * Return the next line, or null if there are no more lines to read. Also
     * returns null in the error condition where pos is before the beginning.
     */
    public Line next() {
        if (pos >= mMin && pos < mMax) {
            return this.mList.get(pos++);
        } else {
            return null;
        }
    }

    /**
     * Move the read position back by one line.
     */
    public void rewind() {
        pos--;
    }

    /**
     * Move th read position to the given pos.
     */
    public void rewind(int pos) {
        this.pos = pos;
    }

    /**
     * Return the number of lines.
     */
    public int size() {
        return mMax - mMin;
    }

    /**
     * Return a new Lines object restricted to the [from,to) range.
     * The array list and Lines objects are shared, so be careful
     * if you modify the lines themselves.
     */
    public Lines<T> copy(int from, int to) {
        return new Lines<T>(mList, Math.max(mMin, from), Math.min(mMax, to));
    }
}

