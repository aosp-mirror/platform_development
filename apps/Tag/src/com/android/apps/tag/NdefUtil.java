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

package com.android.apps.tag;

import com.android.apps.tag.record.ParsedNdefRecord;
import com.android.apps.tag.record.SmartPoster;
import com.android.apps.tag.record.TextRecord;
import com.android.apps.tag.record.UriRecord;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;

import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.nio.charset.Charsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for dealing with conversions to and from NdefRecords.
 *
 * TODO: Possibly move this class into core Android.
 */
public class NdefUtil {
    private static final byte[] EMPTY = new byte[0];

    /**
     * Create a new {@link NdefRecord} containing the supplied {@link Uri}.
     */
    public static NdefRecord toUriRecord(Uri uri) {
        byte[] uriBytes = uri.toString().getBytes(Charsets.UTF_8);

        /*
         * We prepend 0x00 to the bytes of the URI to indicate that this
         * is the entire URI, and we are not taking advantage of the
         * URI shortening rules in the NFC Forum URI spec section 3.2.2.
         * This produces a NdefRecord which is slightly larger than
         * necessary.
         *
         * In the future, we should use the URI shortening rules in 3.2.2
         * to create a smaller NdefRecord.
         */
        byte[] payload = Bytes.concat(new byte[] { 0x00 }, uriBytes);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_URI, EMPTY, payload);
    }

    public static Iterable<TextRecord> getTextFields(NdefMessage message) {
        return Iterables.filter(getObjects(message), TextRecord.class);
    }

    public static Iterable<UriRecord> getUris(NdefMessage message) {
        return Iterables.filter(getObjects(message), UriRecord.class);
    }

    /**
     * Parse the provided {@code NdefMessage}, extracting all known
     * objects from the message.  Typically this list will consist of
     * {@link String}s corresponding to NDEF text records, or {@link Uri}s
     * corresponding to NDEF URI records.
     * <p>
     * TODO: Is this API too generic?  Should we keep it?
     */
    public static Iterable<ParsedNdefRecord> getObjects(NdefMessage message) {
        List<ParsedNdefRecord> retval = new ArrayList<ParsedNdefRecord>();
        for (NdefRecord record : message.getRecords()) {
            if (UriRecord.isUri(record)) {
                retval.add(UriRecord.parse(record));
            } else if (TextRecord.isText(record)) {
                retval.add(TextRecord.parse(record));
            } else if (SmartPoster.isPoster(record)) {
                retval.add(SmartPoster.parse(record));
            }
        }
        return retval;
    }
}
