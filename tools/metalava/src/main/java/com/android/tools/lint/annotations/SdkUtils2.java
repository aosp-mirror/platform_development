/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.lint.annotations;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;

// Copy of SdkUtils but with a modification that isn't in SdkUtils yet

public class SdkUtils2 {
    /**
     * Wraps the given text at the given line width, with an optional hanging
     * indent.
     *
     * @param text          the text to be wrapped
     * @param lineWidth     the number of characters to wrap the text to
     * @param hangingIndent the hanging indent (to be used for the second and
     *                      subsequent lines in each paragraph, or null if not known
     * @return the string, wrapped
     */
    @NonNull
    public static String wrap(
            @NonNull String text,
            int lineWidth,
            @Nullable String hangingIndent) {
        return wrap(text, lineWidth, lineWidth, hangingIndent);
    }

    /**
     * Wraps the given text at the given line width, with an optional hanging
     * indent.
     *
     * @param text           the text to be wrapped
     * @param firstLineWidth the line width to wrap the text to (on the first line)
     * @param nextLineWidth  the line width to wrap the text to (on subsequent lines).
     *                       This does not include the hanging indent, if any.
     * @param hangingIndent  the hanging indent (to be used for the second and
     *                       subsequent lines in each paragraph, or null if not known
     * @return the string, wrapped
     */
    @NonNull
    public static String wrap(
            @NonNull String text,
            int firstLineWidth,
            int nextLineWidth,
            @Nullable String hangingIndent) {
        if (hangingIndent == null) {
            hangingIndent = "";
        }
        int lineWidth = firstLineWidth;
        int explanationLength = text.length();
        StringBuilder sb = new StringBuilder(explanationLength * 2);
        int index = 0;

        while (index < explanationLength) {
            int lineEnd = text.indexOf('\n', index);
            int next;

            if (lineEnd != -1 && (lineEnd - index) < lineWidth) {
                next = lineEnd + 1;
            } else {
                // Line is longer than available width; grab as much as we can
                lineEnd = Math.min(index + lineWidth, explanationLength);
                if (lineEnd - index < lineWidth) {
                    next = explanationLength;
                } else {
                    // then back up to the last space
                    int lastSpace = text.lastIndexOf(' ', lineEnd);
                    if (lastSpace > index) {
                        lineEnd = lastSpace;
                        next = lastSpace + 1;
                    } else {
                        // No space anywhere on the line: it contains something wider than
                        // can fit (like a long URL) so just hard break it
                        next = lineEnd;
                    }
                }
            }

            if (sb.length() > 0) {
                sb.append(hangingIndent);
            } else {
                lineWidth = nextLineWidth - hangingIndent.length();
            }

            sb.append(text.substring(index, lineEnd));
            sb.append('\n');
            index = next;
        }

        return sb.toString();
    }
}
