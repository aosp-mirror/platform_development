/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.apps.tag.message;

import com.android.apps.tag.record.ParsedNdefRecord;
import com.google.common.collect.ImmutableList;

import java.util.Locale;

/**
 * The catchall parsed message format for when nothing else better applies.
 */
class UnknownMessage implements ParsedNdefMessage {

    private final ImmutableList<ParsedNdefRecord> mRecords;

    UnknownMessage(Iterable<ParsedNdefRecord> records) {
        mRecords = ImmutableList.copyOf(records);
    }

    @Override
    public String getSnippet(Locale locale) {
        // TODO: localize
        return "Unknown record type with " + mRecords.size() + " elements.";
    }

    @Override
    public boolean isStarred() {
        return false;
    }
}
