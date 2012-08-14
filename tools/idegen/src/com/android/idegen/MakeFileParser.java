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
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses the make files and finds the appropriate section a given module.
 */
public class MakeFileParser {

    private static final Logger logger = Logger.getLogger(MakeFileParser.class.getName());
    public static final String VALUE_DELIMITER = "|";

    private enum State {
        NEW, CONTINUE
    }
    private enum ModuleNameKey {
        LOCAL_PACKAGE_NAME,
        LOCAL_MODULE
    };

    private File makeFile;
    private String moduleName;
    private HashMap<String, String> values;

    /**
     * Create a parser for a given make file and module name.
     * <p>
     * A make file may contain multiple modules.
     *
     * @param makeFile The make file to parse.
     * @param moduleName The module to extract.
     */
    public MakeFileParser(File makeFile, String moduleName) {
        this.makeFile = Preconditions.checkNotNull(makeFile);
        this.moduleName = Preconditions.checkNotNull(moduleName);
    }

    public Iterable<String> getValues(String key) {
        String str = values.get(key);
        if (str == null) {
            return null;
        }
        return Splitter.on(VALUE_DELIMITER)
                .trimResults()
                .omitEmptyStrings()
                .split(str);
    }

    /**
     * Extracts the relevant portion of the make file and converts into key value pairs.
     * <p>
     * Since each make file may contain multiple build targets (modules), this method will determine
     * which part is the correct portion for the given module name.
     *
     * @throws IOException
     */
    public void parse() throws IOException {
        values = Maps.newHashMap();
        logger.info("Parsing " + makeFile.getAbsolutePath() + " for module " + moduleName);

        Files.readLines(makeFile, Charset.forName("UTF-8"), new LineProcessor<Object>() {

            private String key;

            private State state = State.NEW;

            @Override
            public boolean processLine(String line) throws IOException {
                String trimmed = line.trim();
                if (Strings.isNullOrEmpty(trimmed)) {
                    state = State.NEW;
                    return true;
                }
                if (trimmed.equals("include $(CLEAR_VARS)")) {
                    // See if we are in the right module.
                    if (moduleName.equals(getModuleName())) {
                        return false;
                    } else {
                        values.clear();
                    }
                } else {
                    switch (state) {
                        case NEW:
                            trimmed = checkContinue(trimmed);
                            if (trimmed.contains("=")) {
                                String[] arr;
                                if (trimmed.contains(":")) {
                                    arr = trimmed.split(":=");
                                } else {
                                    arr = trimmed.split("\\+=");
                                }
                                if (arr.length > 2) {
                                    logger.info("Malformed line " + line);
                                } else {
                                    // Store the key in case the line continues
                                    this.key = arr[0].trim();
                                    if (arr.length == 2) {
                                        // There may be multiple values on one line.
                                        List<String> valuesArr = tokenizeValue(arr[1].trim());
                                        for (String value : valuesArr) {
                                            appendValue(this.key, value);
                                        }

                                    }
                                }
                            } else {
                                //logger.info("Skipping line " + line);
                            }
                            break;
                        case CONTINUE:
                            // append
                            trimmed = checkContinue(trimmed);
                            appendValue(key, trimmed);
                            break;
                        default:

                    }
                }
                return true;
            }

            private List<String> tokenizeValue(String value) {
                // Value may contain function calls such as "$(call all-java-files-under)".
                // Tokens are separated by spaces unless it's between parens.
                StringBuilder token = new StringBuilder();
                ArrayList<String> tokens = Lists.newArrayList();
                int parenCount = 0;
                for (int i = 0; i < value.length(); i++) {
                    char ch = value.charAt(i);
                    if (parenCount == 0 && ch == ' ') {
                        // Not in a paren and delimiter encountered.
                        // end token
                        if (token.length() > 0) {
                            tokens.add(token.toString());
                            token = new StringBuilder();
                        }
                    } else {
                        token.append(ch);
                    }
                    if (ch == '(') {
                        parenCount++;
                    } else if (ch == ')') {
                        parenCount--;
                    }
                }
                // end of line check
                if (token.length() > 0) {
                    tokens.add(token.toString());
                }
                return tokens;
            }

            private String getModuleName() {
                for (ModuleNameKey key : ModuleNameKey.values()) {
                    String name = values.get(key.name());
                    if (name != null) {
                        return name;
                    }
                }
                return null;
            }

            @Override
            public Object getResult() {
                return null;
            }

            private String checkContinue(String value) {
                // Check for continuation character
                if (value.charAt(value.length() - 1) == '\\') {
                    state = State.CONTINUE;
                    return value.substring(0, value.length() - 1);
                }
                state = State.NEW;
                return value;
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
        });
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("values", values)
                .toString();
    }
}
