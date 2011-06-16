/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.voicemail.common.utils;

import android.database.DatabaseUtils;
import android.text.TextUtils;

/**
 * Static methods for helping us build database query selection strings.
 */
public class DbQueryUtils {
    // Static class with helper methods, so private constructor.
    private DbQueryUtils() {
    }

    /** Returns a WHERE clause assert equality of a field to a value for the specified table . */
    public static String getEqualityClause(String table, String field, String value) {
        return getEqualityClause(table + "." + field, value);
    }

    /** Returns a WHERE clause assert equality of a field to a value. */
    public static String getEqualityClause(String field, String value) {
        StringBuilder clause = new StringBuilder();
        clause.append(field);
        clause.append(" = ");
        DatabaseUtils.appendEscapedSQLString(clause, value);
        return clause.toString();
    }

    /** Concatenates any number of clauses using "AND". */
    // TODO: 0. It worries me that I can change the following "AND" to "OR" and the provider tests
    // all pass. I can also remove the braces, and the tests all pass.
    public static String concatenateClausesWithAnd(String... clauses) {
        return concatenateClausesWithOperation("AND", clauses);
    }

    /** Concatenates any number of clauses using "OR". */
    public static String concatenateClausesWithOr(String... clauses) {
        return concatenateClausesWithOperation("OR", clauses);
    }

    /** Concatenates any number of clauses using the specified operation. */
    public static String concatenateClausesWithOperation(String operation, String... clauses) {
        // Nothing to concatenate.
        if (clauses.length == 1) {
            return clauses[0];
        }

        StringBuilder builder = new StringBuilder();

        for (String clause : clauses) {
            if (!TextUtils.isEmpty(clause)) {
                if (builder.length() > 0) {
                    builder.append(" ").append(operation).append(" ");
                }
                builder.append("(");
                builder.append(clause);
                builder.append(")");
            }
        }
        return builder.toString();
    }
}
