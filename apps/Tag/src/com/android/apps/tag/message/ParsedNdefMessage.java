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

import android.content.Context;

import java.util.List;
import java.util.Locale;

/**
 * A parsed version of an {@link android.nfc.NdefMessage}
 */
public abstract class ParsedNdefMessage {

    private List<ParsedNdefRecord> mRecords;

    public ParsedNdefMessage(List<ParsedNdefRecord> records) {
        mRecords = ImmutableList.copyOf(records);
    }

    /**
     * Returns the list of parsed records on this message.
     */
    public List<ParsedNdefRecord> getRecords() {
        return mRecords;
    }

    /**
     * Returns the snippet information associated with the NdefMessage
     * most appropriate for the given {@code locale}.
     */
    public abstract String getSnippet(Context context, Locale locale);

    // TODO: Determine if this is the best place for holding whether
    // the user has starred this parsed message.
    public abstract boolean isStarred();
}
