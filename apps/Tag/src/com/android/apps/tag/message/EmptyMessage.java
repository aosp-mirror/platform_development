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

import com.android.apps.tag.R;
import com.android.apps.tag.record.ParsedNdefRecord;

import android.content.Context;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A parsed message containing no elements.
 */
class EmptyMessage extends ParsedNdefMessage {

    /* package private */ EmptyMessage() {
        super(new ArrayList<ParsedNdefRecord>());
    }

    @Override
    public String getSnippet(Context context, Locale locale) {
        return context.getString(R.string.tag_empty);
    }

    @Override
    public boolean isStarred() {
        return false;
    }
}
