/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.idegen;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses the make files and finds the appropriate section a given module.
 */
public class MakeFileParser {

    private static final Logger logger = Logger.getLogger(MakeFileParser.class.getName());
    public static final String VALUE_DELIMITER = "|";

    private File makeFile;
    private HashMap<String, String> values;

    /**
     * Create a parser for a given make file and module name.
     * <p>
     * A make file may contain multiple modules.
     *
     * @param makeFile The make file to parse.
     */
    public MakeFileParser(File makeFile) {
        this.makeFile = Preconditions.checkNotNull(makeFile);
    }

    public Iterable<String> getValues(String key) {
        String str = values.get(key);
        if (str == null) {
            return null;
        }
        return Splitter.on(VALUE_DELIMITER).trimResults().omitEmptyStrings().split(str);
    }

    /**
     * Extracts the relevant portion of the make file and converts into key value pairs. <p> Since
     * each make file may contain multiple build targets (modules), this method will determine which
     * part is the correct portion for the given module name.
     */
    public void parse() throws IOException {
        values = Maps.newHashMap();
        logger.info("Parsing " + makeFile.getCanonicalPath());

        Files.readLines(makeFile, Charset.forName("UTF-8"), new MakeFileLineProcessor());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("values", values).toString();
    }

    private class MakeFileLineProcessor implements LineProcessor<Object> {

        private StringBuilder lineBuffer;

        // Keep a list of LOCAL_ variables to clear when CLEAR_VARS is encountered.
        private HashSet<String> localVars = Sets.newHashSet();

        @Override
        public boolean processLine(String line) throws IOException {
            String trimmed = line.trim();
            // Skip comments.
            if (!trimmed.isEmpty() && trimmed.charAt(0) == '#') {
                return true;
            }
            appendPartialLine(trimmed);

            if (!trimmed.isEmpty() && trimmed.charAt(trimmed.length() - 1) == '\\') {
                // This is a partial line.  Do not process yet.
                return true;
            }

            String completeLine = lineBuffer.toString().trim();
            // Reset the line buffer.
            lineBuffer = null;

            if (Strings.isNullOrEmpty(completeLine)) {
                return true;
            }

            processKeyValuePairs(completeLine);
            return true;
        }

        private void processKeyValuePairs(String line) {
            if (line.contains("=")) {
                String[] arr;
                if (line.contains(":")) {
                    arr = line.split(":=");
                } else {
                    arr = line.split("\\+=");
                }
                if (arr.length > 2) {
                    logger.info("Malformed line " + line);
                } else {
                    // Store the key in case the line continues
                    String key = arr[0].trim();
                    if (arr.length == 2) {
                        // There may be multiple values on one line.
                        List<String> valuesArr = tokenizeValue(arr[1]);
                        for (String value : valuesArr) {
                            appendValue(key, value);
                        }

                    }
                }
            } else {
                //logger.info("Skipping line " + line);
            }
        }

        private void appendPartialLine(String line) {
            if (lineBuffer == null) {
                lineBuffer = new StringBuilder();
            } else {
                lineBuffer.append(" ");
            }
            if (line.endsWith("\\")) {
                lineBuffer.append(line.substring(0, line.length() - 2).trim());
            } else {
                lineBuffer.append(line);
            }
        }

        private List<String> tokenizeValue(String rawValue) {
            String value = rawValue.trim();
            ArrayList<String> result = Lists.newArrayList();
            if (value.isEmpty()) {
                return result;
            }

            // Value may contain function calls such as "$(call all-java-files-under)" or refer
            // to variables such as "$(my_var)"
            value = findVariables(value);

            String[] tokens = value.split(" ");
            Collections.addAll(result, tokens);
            return result;
        }

        private String findVariables(String value) {

            int variableStart = value.indexOf('$');
            // Keep going until we substituted all variables.
            while (variableStart > -1) {
                StringBuilder sb = new StringBuilder();
                sb.append(value.substring(0, variableStart));

                // variable found
                int variableEnd = findClosingParen(value, variableStart);
                if (variableEnd > variableStart) {
                    String result = substituteVariables(value.substring(variableStart + 2, variableEnd));
                    sb.append(result);
                } else {
                    throw new IllegalArgumentException(
                            "Malformed variable reference in make file: " + value);
                }
                if (variableEnd + 1 < value.length()) {
                    sb.append(value.substring(variableEnd + 1));
                }
                value = sb.toString();
                variableStart = value.indexOf('$');
            }
            return value;
        }

        private int findClosingParen(String value, int startIndex) {
            int openParenCount = 0;
            for (int i = startIndex; i < value.length(); i++) {
                char ch = value.charAt(i);
                if (ch == ')') {
                    openParenCount--;
                    if (openParenCount == 0) {
                        return i;
                    }
                } else if (ch == '(') {
                    openParenCount++;
                }
            }
            return -1;
        }

        /**
         * Look for and handle $(...) variables.
         */
        private String substituteVariables(String rawValue) {
            if (rawValue.isEmpty()) {
                return rawValue;
            }
            String value = rawValue;
            if (value.startsWith("call all-java-files-under")) {
                // Ignore the call and function, keep the args.
                value = value.substring(25).trim();
            } else if (value.startsWith("call")) {
                value = value.substring(4).trim();
            }

            // Check for single variable
            if (value.indexOf(' ') == -1) {
                // Substitute.
                value = values.get(value);
                if (value == null) {
                    value = "";
                }
                return value;
            } else {
                return findVariables(value);
            }
        }

        @Override
        public Object getResult() {
            return null;
        }

        /**
         * Add a value to the hash map. If the key already exists, will append instead of
         * over-writing the existing value.
         *
         * @param key The hashmap key
         * @param newValue The value to append.
         */
        private void appendValue(String key, String newValue) {
            String value = values.get(key);
            if (value == null) {
                values.put(key, newValue);
            } else {
                values.put(key, value + VALUE_DELIMITER + newValue);
            }
        }
    }

    public static void main(String[] args) {
        MakeFileParser parser = new MakeFileParser(new File(args[0]));
        try {
            parser.parse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(parser.toString());
    }
}
