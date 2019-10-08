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
package com.android.dumpviewer.utils;

import androidx.core.os.BuildCompat;

public abstract class GrepHelper {
    protected GrepHelper() {
    }

    public static GrepHelper getHelper() {
        if (BuildCompat.isAtLeastQ()) {
            return new PcreGrepHelper();
        } else {
            return new BsdGrepHelper();
        }
    }

    public abstract String getCommandName();

    public abstract String[] getMetaCharacters();

    public abstract void buildCommand(StringBuilder sb, String pattern, int before, int after,
            boolean ignoreCase);

    private static class PcreGrepHelper extends GrepHelper {
        private static final String[] sRePickerPatterns = {
                "\\ (backslash / escape)",
                ". (any char)",
                "* (more than 0)",
                "+ (more than 1)",
                "^ (line start / negate char class)",
                "$ (line end)",
                "( (selection start)",
                "| (selection or)",
                ") (selection end)",
                "[ (char class start)",
                "] (char class end)",
                "{ (\"bound\" start)",
                ",",
                "} (\"bound\" end)",
                "\\s (whitespace)",
                "\\S (not whitespace)",
                "\\d (digit)",
                "\\D (not digit)",
                "\\w (word character)",
                "\\W (not word character)",
                "\\b (word boundary)",
                "\\B (not word boundary)",
        };

        @Override
        public String getCommandName() {
            return "pcre2grep";
        }

        @Override
        public String[] getMetaCharacters() {
            return sRePickerPatterns;
        }

        @Override
        public void buildCommand(StringBuilder sb, String pattern, int before, int after,
                boolean ignoreCase) {
            sb.append("grep");
            if (ignoreCase) {
                sb.append(" -i");
            }
            if (before > 0) {
                sb.append(" -B");
                sb.append(before);
            }
            if (after > 0) {
                sb.append(" -A");
                sb.append(after);
            }
            sb.append(" -- ");
            sb.append(Utils.shellEscape(pattern));
        }
    }

    private static class BsdGrepHelper extends GrepHelper {
        private static final String[] sRePickerPatterns = {
                "\\ (backslash / escape)",
                ". (any char)",
                "* (more than 0)",
                "+ (more than 1)",
                "^ (line start / negate char class)",
                "$ (line end)",
                "( (selection start)",
                "| (selection or)",
                ") (selection end)",
                "[ (char class start)",
                "] (char class end)",
                "{ (\"bound\" start)",
                ",",
                "} (\"bound\" end)",
                "[[:alpha:]] ([a-zA-Z])",
                "[[:digit:]] ([0-9])",
                "[[:alnum:]] ([0-9a-zA-Z])",
                "[[:<:]] (start of word)",
                "[[:>:]] (end of word)",
                "[[:",
                ":]]",
        };

        @Override
        public String getCommandName() {
            return "egrep";
        }

        @Override
        public String[] getMetaCharacters() {
            return sRePickerPatterns;
        }

        @Override
        public void buildCommand(StringBuilder sb, String pattern, int before, int after,
                boolean ignoreCase) {
            sb.append("grep");
            sb.append(" -E");
            if (ignoreCase) {
                sb.append(" -i");
            }
            if (before > 0) {
                sb.append(" -B");
                sb.append(before);
            }
            if (after > 0) {
                sb.append(" -A");
                sb.append(after);
            }
            sb.append(" -- ");
            sb.append(Utils.shellEscape(pattern));
        }
    }
}
