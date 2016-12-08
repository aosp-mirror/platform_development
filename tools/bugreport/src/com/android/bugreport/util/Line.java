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

/**
 * A line of text read from the original source file.
 *
 * Contains the text without the newline and line position (starting at 1)
 * from within the file.
 */
public class Line {
    public int lineno;
    public String text;

    /**
     * Construct a new Line no lineno or text.
     */
    public Line() {
    }

    /**
     * Construct a new Line with the supplied lineno and text.
     */
    public Line(int lineno, String text) {
        this.lineno = lineno;
        this.text = text;
    }
}

