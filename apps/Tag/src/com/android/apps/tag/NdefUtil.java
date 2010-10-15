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

import android.net.Uri;
import android.nfc.NdefRecord;

import com.google.common.primitives.Bytes;

import java.nio.charset.Charsets;

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
}
