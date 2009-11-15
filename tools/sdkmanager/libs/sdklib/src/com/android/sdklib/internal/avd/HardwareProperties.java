/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.sdklib.internal.avd;

import com.android.sdklib.ISdkLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardwareProperties {
    private final static Pattern PATTERN_PROP = Pattern.compile(
    "^([a-zA-Z0-9._-]+)\\s*=\\s*(.*)\\s*$");

    private final static String HW_PROP_NAME = "name";
    private final static String HW_PROP_TYPE = "type";
    private final static String HW_PROP_DEFAULT = "default";
    private final static String HW_PROP_ABSTRACT = "abstract";
    private final static String HW_PROP_DESC = "description";

    private final static String BOOLEAN_YES = "yes";
    private final static String BOOLEAN_NO = "no";
    public final static String[] BOOLEAN_VALUES = new String[] { BOOLEAN_YES, BOOLEAN_NO };
    public final static Pattern DISKSIZE_PATTERN = Pattern.compile("\\d+[MK]B");

    public enum ValueType {
        INTEGER("integer"),
        BOOLEAN("boolean"),
        DISKSIZE("diskSize");

        private String mValue;

        ValueType(String value) {
            mValue = value;
        }

        public String getValue() {
            return mValue;
        }

        public static ValueType getEnum(String value) {
            for (ValueType type : values()) {
                if (type.mValue.equals(value)) {
                    return type;
                }
            }

            return null;
        }
    }

    public static final class HardwareProperty {
        private String mName;
        private ValueType mType;
        /** the string representation of the default value. can be null. */
        private String mDefault;
        private String mAbstract;
        private String mDescription;

        public String getName() {
            return mName;
        }

        public ValueType getType() {
            return mType;
        }

        public String getDefault() {
            return mDefault;
        }

        public String getAbstract() {
            return mAbstract;
        }

        public String getDescription() {
            return mDescription;
        }
    }

    /**
     * Parses the hardware definition file.
     * @param file the property file to parse
     * @param log the ISdkLog object receiving warning/error from the parsing.
     * @return the map of (key,value) pairs, or null if the parsing failed.
     */
    public static Map<String, HardwareProperty> parseHardwareDefinitions(File file, ISdkLog log) {
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            Map<String, HardwareProperty> map = new HashMap<String, HardwareProperty>();

            String line = null;
            HardwareProperty prop = null;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 0 && line.charAt(0) != '#') {
                    Matcher m = PATTERN_PROP.matcher(line);
                    if (m.matches()) {
                        String valueName = m.group(1);
                        String value = m.group(2);

                        if (HW_PROP_NAME.equals(valueName)) {
                            prop = new HardwareProperty();
                            prop.mName = value;
                            map.put(prop.mName, prop);
                        }

                        if (prop == null) {
                            log.warning("Error parsing '%1$s': missing '%2$s'",
                                    file.getAbsolutePath(), HW_PROP_NAME);
                            return null;
                        }

                        if (HW_PROP_TYPE.equals(valueName)) {
                            prop.mType = ValueType.getEnum(value);
                        } else if (HW_PROP_DEFAULT.equals(valueName)) {
                            prop.mDefault = value;
                        } else if (HW_PROP_ABSTRACT.equals(valueName)) {
                            prop.mAbstract = value;
                        } else if (HW_PROP_DESC.equals(valueName)) {
                            prop.mDescription = value;
                        }
                    } else {
                        log.warning("Error parsing '%1$s': \"%2$s\" is not a valid syntax",
                                file.getAbsolutePath(), line);
                        return null;
                    }
                }
            }

            return map;
        } catch (FileNotFoundException e) {
            // this should not happen since we usually test the file existence before
            // calling the method.
            // Return null below.
        } catch (IOException e) {
            if (log != null) {
                log.warning("Error parsing '%1$s': %2$s.", file.getAbsolutePath(),
                        e.getMessage());
            }
        }

        return null;
    }

    /**
     * Returns the index of <var>value</var> in {@link #BOOLEAN_VALUES}.
     */
    public static int getBooleanValueIndex(String value) {
        if (BOOLEAN_YES.equals(value)) {
            return 0;
        } else if (BOOLEAN_NO.equals(value)) {
            return 1;
        }

        return -1;
    }
}
