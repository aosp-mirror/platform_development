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

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Bytes;
import com.trustedlogic.trustednfc.android.NdefRecord;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charsets;
import java.util.Arrays;

/**
 * Utilities for dealing with conversions to and from NdefRecords.
 *
 * TODO: Possibly move this class into core Android.
 */
public class NdefUtil {
    private static final byte[] EMPTY = new byte[0];

    /**
     * NFC Forum "URI Record Type Definition"
     *
     * This is a mapping of "URI Identifier Codes" to URI string prefixes,
     * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
     */
    private static final
    BiMap<Byte, String> URI_PREFIX_MAP = ImmutableBiMap.<Byte, String>builder()
            .put((byte) 0x00, "")
            .put((byte) 0x01, "http://www.")
            .put((byte) 0x02, "https://www.")
            .put((byte) 0x03, "http://")
            .put((byte) 0x04, "https://")
            .put((byte) 0x05, "tel:")
            .put((byte) 0x06, "mailto:")
            .put((byte) 0x07, "ftp://anonymous:anonymous@")
            .put((byte) 0x08, "ftp://ftp.")
            .put((byte) 0x09, "ftps://")
            .put((byte) 0x0A, "sftp://")
            .put((byte) 0x0B, "smb://")
            .put((byte) 0x0C, "nfs://")
            .put((byte) 0x0D, "ftp://")
            .put((byte) 0x0E, "dav://")
            .put((byte) 0x0F, "news:")
            .put((byte) 0x10, "telnet://")
            .put((byte) 0x11, "imap:")
            .put((byte) 0x12, "rtsp://")
            .put((byte) 0x13, "urn:")
            .put((byte) 0x14, "pop:")
            .put((byte) 0x15, "sip:")
            .put((byte) 0x16, "sips:")
            .put((byte) 0x17, "tftp:")
            .put((byte) 0x18, "btspp://")
            .put((byte) 0x19, "btl2cap://")
            .put((byte) 0x1A, "btgoep://")
            .put((byte) 0x1B, "tcpobex://")
            .put((byte) 0x1C, "irdaobex://")
            .put((byte) 0x1D, "file://")
            .put((byte) 0x1E, "urn:epc:id:")
            .put((byte) 0x1F, "urn:epc:tag:")
            .put((byte) 0x20, "urn:epc:pat:")
            .put((byte) 0x21, "urn:epc:raw:")
            .put((byte) 0x22, "urn:epc:")
            .put((byte) 0x23, "urn:nfc:")
            .build();

    /**
     * Create a new {@link NdefRecord} containing the supplied {@link URI}.
     */
    public static NdefRecord toUriRecord(URI uri) {
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

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN_TYPE,
                NdefRecord.TYPE_URI, EMPTY, payload);
    }

    /**
     * Convert {@link NdefRecord} into a {@link URI}.
     *
     * TODO: This class does not handle NdefRecords where the TNF
     * (Type Name Format) of the class is {@link NdefRecord#TNF_ABSOLUTE_URI}.
     * This should be fixed.
     *
     * @throws URISyntaxException if the {@code NdefRecord} contains an
     *     invalid URI.
     * @throws IllegalArgumentException if the NdefRecord is not a
     *     record containing a URI.
     */
    public static URI toURI(NdefRecord record) throws URISyntaxException {
        Preconditions.checkArgument(record.getTnf() == NdefRecord.TNF_WELL_KNOWN_TYPE);
        Preconditions.checkArgument(Arrays.equals(record.getType(), NdefRecord.TYPE_URI));

        byte[] payload = record.getPayload();

        /*
         * payload[0] contains the URI Identifier Code, per the
         * NFC Forum "URI Record Type Definition" section 3.2.2.
         *
         * payload[1]...payload[payload.length - 1] contains the rest of
         * the URI.
         */

        String prefix = URI_PREFIX_MAP.get(payload[0]);
        byte[] fullUri = Bytes.concat(
                prefix.getBytes(Charsets.UTF_8),
                Arrays.copyOfRange(payload, 1, payload.length - 1));

        return new URI(new String(fullUri, Charsets.UTF_8));
    }

    public static boolean isURI(NdefRecord record) {
        try {
            toURI(record);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
