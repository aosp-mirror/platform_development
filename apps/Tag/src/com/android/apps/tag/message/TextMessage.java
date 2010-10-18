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
import com.android.apps.tag.record.TextRecord;
import com.google.common.base.Preconditions;

import android.content.Context;

import java.util.List;
import java.util.Locale;

/**
 * A message containing one text element
 */
class TextMessage extends ParsedNdefMessage {
    private final TextRecord mRecord;

    TextMessage(TextRecord record, List<ParsedNdefRecord> records) {
        super(Preconditions.checkNotNull(records));
        mRecord = Preconditions.checkNotNull(record);
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        return mRecord.getText();
    }

    @Override
    public boolean isStarred() {
        return false;
    }
}
