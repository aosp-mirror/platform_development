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
package com.android.tools.metalava.doclava1;

import com.android.annotations.Nullable;
import com.android.tools.metalava.Severity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.android.sdklib.SdkVersionInfo.underlinesToCamelCase;
import static com.android.tools.metalava.Severity.ERROR;
import static com.android.tools.metalava.Severity.HIDDEN;
import static com.android.tools.metalava.Severity.INHERIT;
import static com.android.tools.metalava.Severity.LINT;
import static com.android.tools.metalava.Severity.WARNING;

// Copied from doclava1 (and a bunch of stuff left alone; preserving to have same error id's)
public class Errors {
    public static class Error {
        public final int code;

        private Severity level;
        private Severity defaultLevel;

        /**
         * The name of this error if known
         */
        @Nullable
        public String name;

        /**
         * When {@code level} is set to {@link Severity#INHERIT}, this is the parent from
         * which the error will inherit its level.
         */
        private final Error parent;

        public Error(int code, Severity level) {
            this.code = code;
            this.level = level;
            this.defaultLevel = level;
            this.parent = null;
            sErrors.add(this);
        }

        public Error(int code, Error parent) {
            this.code = code;
            this.level = Severity.INHERIT;
            this.defaultLevel = Severity.INHERIT;
            this.parent = parent;
            sErrors.add(this);
        }

        /**
         * Returns the implied level for this error.
         * <p>
         * If the level is {@link Severity#INHERIT}, the level will be returned for the
         * parent.
         *
         * @throws IllegalStateException if the level is {@link Severity#INHERIT} and the
         *                               parent is {@code null}
         */
        public Severity getLevel() {
            if (level == INHERIT) {
                if (parent == null) {
                    throw new IllegalStateException("Error with level INHERIT must have non-null parent");
                }
                return parent.getLevel();
            }
            return level;
        }

        /**
         * Sets the level.
         * <p>
         * Valid arguments are:
         * <ul>
         * <li>{@link Severity#HIDDEN}
         * <li>{@link Severity#WARNING}
         * <li>{@link Severity#ERROR}
         * </ul>
         *
         * @param level the level to set
         */
        public void setLevel(Severity level) {
            if (level == INHERIT) {
                throw new IllegalArgumentException("Error level may not be set to INHERIT");
            }
            this.level = level;
        }

        public String toString() {
            return "Error #" + this.code + " (" + this.name + ")";
        }
    }

    private static final List<Error> sErrors = new ArrayList<>();

    // Errors for API verification
    public static final Error PARSE_ERROR = new Error(1, ERROR);
    public static final Error ADDED_PACKAGE = new Error(2, WARNING);
    public static final Error ADDED_CLASS = new Error(3, WARNING);
    public static final Error ADDED_METHOD = new Error(4, WARNING);
    public static final Error ADDED_FIELD = new Error(5, WARNING);
    public static final Error ADDED_INTERFACE = new Error(6, WARNING);
    public static final Error REMOVED_PACKAGE = new Error(7, WARNING);
    public static final Error REMOVED_CLASS = new Error(8, WARNING);
    public static final Error REMOVED_METHOD = new Error(9, WARNING);
    public static final Error REMOVED_FIELD = new Error(10, WARNING);
    public static final Error REMOVED_INTERFACE = new Error(11, WARNING);
    public static final Error CHANGED_STATIC = new Error(12, WARNING);
    public static final Error ADDED_FINAL = new Error(13, WARNING);
    public static final Error CHANGED_TRANSIENT = new Error(14, WARNING);
    public static final Error CHANGED_VOLATILE = new Error(15, WARNING);
    public static final Error CHANGED_TYPE = new Error(16, WARNING);
    public static final Error CHANGED_VALUE = new Error(17, WARNING);
    public static final Error CHANGED_SUPERCLASS = new Error(18, WARNING);
    public static final Error CHANGED_SCOPE = new Error(19, WARNING);
    public static final Error CHANGED_ABSTRACT = new Error(20, WARNING);
    public static final Error CHANGED_THROWS = new Error(21, WARNING);
    public static final Error CHANGED_NATIVE = new Error(22, HIDDEN);
    public static final Error CHANGED_CLASS = new Error(23, WARNING);
    public static final Error CHANGED_DEPRECATED = new Error(24, WARNING);
    public static final Error CHANGED_SYNCHRONIZED = new Error(25, WARNING);
    public static final Error ADDED_FINAL_UNINSTANTIABLE = new Error(26, WARNING);
    public static final Error REMOVED_FINAL = new Error(27, WARNING);
    public static final Error REMOVED_DEPRECATED_CLASS = new Error(28, REMOVED_CLASS);
    public static final Error REMOVED_DEPRECATED_METHOD = new Error(29, REMOVED_METHOD);
    public static final Error REMOVED_DEPRECATED_FIELD = new Error(30, REMOVED_FIELD);


    // Stuff I've added
    public static final Error INVALID_NULL_CONVERSION = new Error(40, WARNING);
    public static final Error PARAMETER_NAME_CHANGE = new Error(41, WARNING);


    // Errors in javadoc generation
    public static final Error UNRESOLVED_LINK = new Error(101, LINT);
    public static final Error BAD_INCLUDE_TAG = new Error(102, LINT);
    public static final Error UNKNOWN_TAG = new Error(103, LINT);
    public static final Error UNKNOWN_PARAM_TAG_NAME = new Error(104, LINT);
    public static final Error UNDOCUMENTED_PARAMETER = new Error(105, HIDDEN); // LINT
    public static final Error BAD_ATTR_TAG = new Error(106, LINT);
    public static final Error BAD_INHERITDOC = new Error(107, HIDDEN); // LINT
    public static final Error HIDDEN_LINK = new Error(108, LINT);
    public static final Error HIDDEN_CONSTRUCTOR = new Error(109, WARNING);
    public static final Error UNAVAILABLE_SYMBOL = new Error(110, WARNING);
    public static final Error HIDDEN_SUPERCLASS = new Error(111, WARNING);
    public static final Error DEPRECATED = new Error(112, HIDDEN);
    public static final Error DEPRECATION_MISMATCH = new Error(113, WARNING);
    public static final Error MISSING_COMMENT = new Error(114, LINT);
    public static final Error IO_ERROR = new Error(115, ERROR);
    public static final Error NO_SINCE_DATA = new Error(116, HIDDEN);
    public static final Error NO_FEDERATION_DATA = new Error(117, WARNING);
    public static final Error BROKEN_SINCE_FILE = new Error(118, ERROR);
    public static final Error INVALID_CONTENT_TYPE = new Error(119, ERROR);
    public static final Error INVALID_SAMPLE_INDEX = new Error(120, ERROR);
    public static final Error HIDDEN_TYPE_PARAMETER = new Error(121, WARNING);
    public static final Error PRIVATE_SUPERCLASS = new Error(122, WARNING);
    public static final Error NULLABLE = new Error(123, HIDDEN); // LINT
    public static final Error INT_DEF = new Error(124, HIDDEN); // LINT
    public static final Error REQUIRES_PERMISSION = new Error(125, LINT);
    public static final Error BROADCAST_BEHAVIOR = new Error(126, LINT);
    public static final Error SDK_CONSTANT = new Error(127, LINT);
    public static final Error TODO = new Error(128, LINT);
    public static final Error NO_ARTIFACT_DATA = new Error(129, HIDDEN);
    public static final Error BROKEN_ARTIFACT_FILE = new Error(130, ERROR);

    // Metalava new warnings
    public static final Error TYPO = new Error(131, LINT);
    public static final Error MISSING_PERMISSION = new Error(132, LINT);
    public static final Error MULTIPLE_THREAD_ANNOTATIONS = new Error(133, LINT);
    public static final Error UNRESOLVED_CLASS = new Error(134, LINT);

    static {
        // Attempt to initialize error names based on the field names
        try {
            for (Field field : Errors.class.getDeclaredFields()) {
                Object o = field.get(null);
                if (o instanceof Error) {
                    Error error = (Error) o;
                    String fieldName = field.getName();
                    error.name = underlinesToCamelCase(fieldName.toLowerCase(Locale.US));
                }
            }
        } catch (Throwable unexpected) {
            unexpected.printStackTrace();
        }
    }

    public static boolean setErrorLevel(String id, Severity level) {
        int code = -1;
        if (Character.isDigit(id.charAt(0))) {
            code = Integer.parseInt(id);
        }
        for (Error e : sErrors) {
            if (e.code == code || e.name.equalsIgnoreCase(id)) {
                e.setLevel(level);
                return true;
            }
        }
        return false;
    }

    // Set error severity for all the compatibility related checks
    public static void enforceCompatibility() {
        for (Error e : sErrors) {
            if (e.code >= Errors.PARSE_ERROR.code && e.code <= Errors.PARAMETER_NAME_CHANGE.code) {
                e.setLevel(ERROR);
            }
        }
    }

    // Primary needed by unit tests; ensure that a previous test doesn't influence
    // a later one
    public static void resetLevels() {
        for (Error error : sErrors) {
            error.level = error.defaultLevel;
        }
    }
}
