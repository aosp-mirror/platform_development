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

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import com.android.apps.tag.record.ParsedNdefRecord;
import com.android.apps.tag.record.SmartPoster;
import com.android.apps.tag.record.TextRecord;
import com.android.apps.tag.record.UriRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for creating {@link ParsedNdefMessage}s.
 */
public class NdefMessageParser {

    // Utility class
    private NdefMessageParser() { }

    /** Parse an NdefMessage */
    public static ParsedNdefMessage parse(NdefMessage message) {
        List<ParsedNdefRecord> elements = getRecords(message);

        if (elements.isEmpty()) {
            return new EmptyMessage();
        }

        ParsedNdefRecord first = elements.get(0);

        if (elements.size() == 1) {
            if (first instanceof SmartPoster) {
                return new SmartPosterMessage((SmartPoster) first, elements);
            }
            if (first instanceof TextRecord) {
                return new TextMessage((TextRecord) first, elements);
            }
            if (first instanceof UriRecord) {
                return new UriMessage((UriRecord) first, elements);
            }
        }

        return new UnknownMessage(elements);
    }

    public static List<ParsedNdefRecord> getRecords(NdefMessage message) {
        List<ParsedNdefRecord> elements = new ArrayList<ParsedNdefRecord>();
        for (NdefRecord record : message.getRecords()) {
            if (UriRecord.isUri(record)) {
                elements.add(UriRecord.parse(record));
            } else if (TextRecord.isText(record)) {
                elements.add(TextRecord.parse(record));
            } else if (SmartPoster.isPoster(record)) {
                elements.add(SmartPoster.parse(record));
            }
        }
        return elements;
    }
}
