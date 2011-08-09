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

package com.example.android.voicemail.common.core;

import static com.example.android.voicemail.common.utils.DbQueryUtils.concatenateClausesWithAnd;
import static com.example.android.voicemail.common.utils.DbQueryUtils.concatenateClausesWithOr;
import static com.example.android.voicemail.common.utils.DbQueryUtils.getEqualityClause;

import android.provider.VoicemailContract.Voicemails;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class to create {@link VoicemailFilter} objects for various filtering needs.
 * <p>
 * Factory methods like {@link #createWithReadStatus(boolean)} and
 * {@link #createWithMatchingFields(Voicemail)} can be used to create a voicemail filter that
 * matches the value of the specific field.
 * <p>
 * It is possible to combine multiple filters with OR or AND operation using the methods
 * {@link #createWithOrOf(VoicemailFilter...)} and {@link #createWithAndOf(VoicemailFilter...)}
 * respectively.
 * <p>
 * {@link #createWithWhereClause(String)} can be used to create an arbitrary filter for a specific
 * where clause. Using this method requires the knowledge of the name of columns used in voicemail
 * content provider database and is therefore less recommended.
 */
public class VoicemailFilterFactory {
    /**
     * Creates a voicemail filter with the specified where clause. Use this method only if you know
     * and want to directly use the column names of the content provider. For most of the usages
     * one of the other factory methods should be good enough.
     */
    public static VoicemailFilter createWithWhereClause(final String whereClause) {
        return new VoicemailFilter() {
          @Override
          public String getWhereClause() {
            return TextUtils.isEmpty(whereClause) ? null : whereClause;
          }
          @Override
          public String toString() {
              return getWhereClause();
          }
        };
    }

    /** Creates a filter with fields matching the ones set in the supplied voicemail object. */
    public static VoicemailFilter createWithMatchingFields(Voicemail fieldMatch) {
        if (fieldMatch == null) {
            throw new IllegalArgumentException("Cannot create filter null fieldMatch");
        }
        return VoicemailFilterFactory.createWithWhereClause(
                getWhereClauseForMatchingFields(fieldMatch));
    }

    /** Creates a voicemail filter with the specified read status. */
    public static VoicemailFilter createWithReadStatus(boolean isRead) {
        return createWithMatchingFields(
                VoicemailImpl.createEmptyBuilder().setIsRead(isRead).build());
    }

    /** Combine multiple filters with OR clause. */
    public static VoicemailFilter createWithAndOf(VoicemailFilter... filters) {
        return createWithWhereClause(concatenateClausesWithAnd(getClauses(filters)));
    }

    /** Combine multiple filters with AND clause. */
    public static VoicemailFilter createWithOrOf(VoicemailFilter... filters) {
        return createWithWhereClause(concatenateClausesWithOr(getClauses(filters)));
    }

    private static String[] getClauses(VoicemailFilter[] filters) {
        String[] clauses = new String[filters.length];
        for (int i = 0; i < filters.length; ++i) {
            clauses[i] = filters[i].getWhereClause();
        }
        return clauses;
    }

    private static String getWhereClauseForMatchingFields(Voicemail fieldMatch) {
        List<String> clauses = new ArrayList<String>();
        if (fieldMatch.hasRead()) {
            clauses.add(getEqualityClause(Voicemails.IS_READ, fieldMatch.isRead() ? "1" : "0"));
        }
        if (fieldMatch.hasNumber()) {
            clauses.add(getEqualityClause(Voicemails.NUMBER, fieldMatch.getNumber()));
        }
        if (fieldMatch.hasSourcePackage()) {
            clauses.add(getEqualityClause(Voicemails.SOURCE_PACKAGE,
                    fieldMatch.getSourcePackage()));
        }
        if (fieldMatch.hasSourceData()) {
            clauses.add(getEqualityClause(Voicemails.SOURCE_DATA, fieldMatch.getSourceData()));
        }
        if (fieldMatch.hasDuration()) {
            clauses.add(getEqualityClause(Voicemails.DURATION,
                    Long.toString(fieldMatch.getDuration())));
        }
        if (fieldMatch.hasTimestampMillis()) {
            clauses.add(getEqualityClause(Voicemails.DATE,
                    Long.toString(fieldMatch.getTimestampMillis())));
        }
        // Empty filter.
        if (clauses.size() == 0) {
            return null;
        }
        return concatenateClausesWithAnd(clauses.toArray(new String[0]));
    }
}
