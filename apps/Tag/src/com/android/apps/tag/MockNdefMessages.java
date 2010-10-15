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

/**
 * Tags that we've seen in the field, for testing purposes.
 */
public class MockNdefMessages {

    /**
     * A real NFC tag containing an NFC "smart poster".  This smart poster
     * consists of the text "NFC Forum Type 4 Tag" in english combined with
     * the URL "http://www.nxp.com/nfc"
     */
    public static final byte[] REAL_NFC_MSG = new byte[] {
            (byte) 0xd1,                   // MB=1 ME=1 CF=0 SR=1 IL=0 TNF=001
            (byte) 0x02,                   // Type Length = 2
            (byte) 0x2b,                   // Payload Length = 43
            (byte) 0x53, (byte) 0x70,      // Type = {'S', 'p'} (smart poster)

            // begin smart poster payload
            // begin smart poster record #1
            (byte) 0x91,                   // MB=1 ME=0 CF=0 SR=1 IL=0 TNF=001
            (byte) 0x01,                   // Type Length = 1
            (byte) 0x17,                   // Payload Length = 23
            (byte) 0x54,                   // Type = {'T'} (Text data)
            (byte) 0x02,                   // UTF-8 encoding, language code length = 2
            (byte) 0x65, (byte) 0x6e,      // language = {'e', 'n'} (english)

            // Begin text data within smart poster record #1
            (byte) 0x4e,                   // 'N'
            (byte) 0x46,                   // 'F'
            (byte) 0x43,                   // 'C'
            (byte) 0x20,                   // ' '
            (byte) 0x46,                   // 'F'
            (byte) 0x6f,                   // 'o'
            (byte) 0x72,                   // 'r'
            (byte) 0x75,                   // 'u'
            (byte) 0x6d,                   // 'm'
            (byte) 0x20,                   // ' '
            (byte) 0x54,                   // 'T'
            (byte) 0x79,                   // 'y'
            (byte) 0x70,                   // 'p'
            (byte) 0x65,                   // 'e'
            (byte) 0x20,                   // ' '
            (byte) 0x34,                   // '4'
            (byte) 0x20,                   // ' '
            (byte) 0x54,                   // 'T'
            (byte) 0x61,                   // 'a'
            (byte) 0x67,                   // 'g'
            // end Text data within smart poster record #1
            // end smart poster record #1

            // begin smart poster record #2
            (byte) 0x51,                   // MB=0 ME=1 CF=0 SR=1 IL=0 TNF=001
            (byte) 0x01,                   // Type Length = 1
            (byte) 0x0c,                   // Payload Length = 12
            (byte) 0x55,                   // Type = { 'U' } (URI)

            // begin URI data within smart poster record #2
            (byte) 0x01,                   // URI Prefix = 1 ("http://www.")
            (byte) 0x6e,                   // 'n'
            (byte) 0x78,                   // 'x'
            (byte) 0x70,                   // 'p'
            (byte) 0x2e,                   // '.'
            (byte) 0x63,                   // 'c'
            (byte) 0x6f,                   // 'o'
            (byte) 0x6d,                   // 'm'
            (byte) 0x2f,                   // '/'
            (byte) 0x6e,                   // 'n'
            (byte) 0x66,                   // 'f'
            (byte) 0x63                    // 'c'
            // end URI data within smart poster record #2
            // end smart poster record #2
            // end smart poster payload
    };


    /**
     * A Smart Poster containing a URL and no text.  This message was created
     * using the NXP reference phone.
     */
    private static final byte[] SMART_POSTER_URL_NO_TEXT = new byte[] {
            (byte) 0xd1, (byte) 0x02, (byte) 0x0f, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
            (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67, (byte) 0x6f,
            (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e, (byte) 0x63,
            (byte) 0x6f, (byte) 0x6d
    };

    /**
     * A plain text tag in english.  Generated using the NXP evaluation tool.
     */
    private static final byte[] ENGLISH_PLAIN_TEXT = new byte[] {
            (byte) 0xd1, (byte) 0x01, (byte) 0x1c, (byte) 0x54, (byte) 0x02, (byte) 0x65,
            (byte) 0x6e, (byte) 0x53, (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x20,
            (byte) 0x72, (byte) 0x61, (byte) 0x6e, (byte) 0x64, (byte) 0x6f, (byte) 0x6d,
            (byte) 0x20, (byte) 0x65, (byte) 0x6e, (byte) 0x67, (byte) 0x6c, (byte) 0x69,
            (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x78,
            (byte) 0x74, (byte) 0x2e
    };

    /**
     * Smart Poster containing a URL and Text.  Generated using the NXP
     * evaluation tool.
     */
    private static final byte[] SMART_POSTER_URL_AND_TEXT = new byte[] {
            (byte) 0xd1, (byte) 0x02, (byte) 0x1c, (byte) 0x53, (byte) 0x70, (byte) 0x91,
            (byte) 0x01, (byte) 0x09, (byte) 0x54, (byte) 0x02, (byte) 0x65, (byte) 0x6e,
            (byte) 0x47, (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65,
            (byte) 0x51, (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67,
            (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e,
            (byte) 0x63, (byte) 0x6f, (byte) 0x6d
    };

    /**
     * A plain URI.  Generated using the NXP evaluation tool.
     */
    private static final byte[] URI = new byte[] {
            (byte) 0xd1, (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67,
            (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e,
            (byte) 0x63, (byte) 0x6f, (byte) 0x6d
    };

    /**
     * A vcard.  Generated using the NXP evaluation tool.
     */
    private static final byte[] VCARD = new byte[] {
            (byte) 0xc2, (byte) 0x0c, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x05,
            (byte) 0x74, (byte) 0x65, (byte) 0x78, (byte) 0x74, (byte) 0x2f, (byte) 0x78,
            (byte) 0x2d, (byte) 0x76, (byte) 0x43, (byte) 0x61, (byte) 0x72, (byte) 0x64,
            (byte) 0x42, (byte) 0x45, (byte) 0x47, (byte) 0x49, (byte) 0x4e, (byte) 0x3a,
            (byte) 0x56, (byte) 0x43, (byte) 0x41, (byte) 0x52, (byte) 0x44, (byte) 0x0d,
            (byte) 0x0a, (byte) 0x56, (byte) 0x45, (byte) 0x52, (byte) 0x53, (byte) 0x49,
            (byte) 0x4f, (byte) 0x4e, (byte) 0x3a, (byte) 0x33, (byte) 0x2e, (byte) 0x30,
            (byte) 0x0d, (byte) 0x0a, (byte) 0x46, (byte) 0x4e, (byte) 0x3a, (byte) 0x4a,
            (byte) 0x6f, (byte) 0x65, (byte) 0x20, (byte) 0x47, (byte) 0x6f, (byte) 0x6f,
            (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6d,
            (byte) 0x70, (byte) 0x6c, (byte) 0x6f, (byte) 0x79, (byte) 0x65, (byte) 0x65,
            (byte) 0x0d, (byte) 0x0a, (byte) 0x41, (byte) 0x44, (byte) 0x52, (byte) 0x3b,
            (byte) 0x54, (byte) 0x59, (byte) 0x50, (byte) 0x45, (byte) 0x3d, (byte) 0x57,
            (byte) 0x4f, (byte) 0x52, (byte) 0x4b, (byte) 0x3a, (byte) 0x3b, (byte) 0x3b,
            (byte) 0x31, (byte) 0x36, (byte) 0x30, (byte) 0x30, (byte) 0x20, (byte) 0x41,
            (byte) 0x6d, (byte) 0x70, (byte) 0x68, (byte) 0x69, (byte) 0x74, (byte) 0x68,
            (byte) 0x65, (byte) 0x61, (byte) 0x74, (byte) 0x72, (byte) 0x65, (byte) 0x20,
            (byte) 0x50, (byte) 0x61, (byte) 0x72, (byte) 0x6b, (byte) 0x77, (byte) 0x61,
            (byte) 0x79, (byte) 0x3b, (byte) 0x39, (byte) 0x34, (byte) 0x30, (byte) 0x34,
            (byte) 0x33, (byte) 0x20, (byte) 0x4d, (byte) 0x6f, (byte) 0x75, (byte) 0x6e,
            (byte) 0x74, (byte) 0x61, (byte) 0x69, (byte) 0x6e, (byte) 0x20, (byte) 0x56,
            (byte) 0x69, (byte) 0x65, (byte) 0x77, (byte) 0x0d, (byte) 0x0a, (byte) 0x54,
            (byte) 0x45, (byte) 0x4c, (byte) 0x3b, (byte) 0x54, (byte) 0x59, (byte) 0x50,
            (byte) 0x45, (byte) 0x3d, (byte) 0x50, (byte) 0x52, (byte) 0x45, (byte) 0x46,
            (byte) 0x2c, (byte) 0x57, (byte) 0x4f, (byte) 0x52, (byte) 0x4b, (byte) 0x3a,
            (byte) 0x36, (byte) 0x35, (byte) 0x30, (byte) 0x2d, (byte) 0x32, (byte) 0x35,
            (byte) 0x33, (byte) 0x2d, (byte) 0x30, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            (byte) 0x0d, (byte) 0x0a, (byte) 0x45, (byte) 0x4d, (byte) 0x41, (byte) 0x49,
            (byte) 0x4c, (byte) 0x3b, (byte) 0x54, (byte) 0x59, (byte) 0x50, (byte) 0x45,
            (byte) 0x3d, (byte) 0x49, (byte) 0x4e, (byte) 0x54, (byte) 0x45, (byte) 0x52,
            (byte) 0x4e, (byte) 0x45, (byte) 0x54, (byte) 0x3a, (byte) 0x73, (byte) 0x75,
            (byte) 0x70, (byte) 0x70, (byte) 0x6f, (byte) 0x72, (byte) 0x74, (byte) 0x40,
            (byte) 0x67, (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65,
            (byte) 0x2e, (byte) 0x63, (byte) 0x6f, (byte) 0x6d, (byte) 0x0d, (byte) 0x0a,
            (byte) 0x54, (byte) 0x49, (byte) 0x54, (byte) 0x4c, (byte) 0x45, (byte) 0x3a,
            (byte) 0x53, (byte) 0x6f, (byte) 0x66, (byte) 0x74, (byte) 0x77, (byte) 0x61,
            (byte) 0x72, (byte) 0x65, (byte) 0x20, (byte) 0x45, (byte) 0x6e, (byte) 0x67,
            (byte) 0x69, (byte) 0x6e, (byte) 0x65, (byte) 0x65, (byte) 0x72, (byte) 0x0d,
            (byte) 0x0a, (byte) 0x4f, (byte) 0x52, (byte) 0x47, (byte) 0x3a, (byte) 0x47,
            (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x0d,
            (byte) 0x0a, (byte) 0x55, (byte) 0x52, (byte) 0x4c, (byte) 0x3a, (byte) 0x68,
            (byte) 0x74, (byte) 0x74, (byte) 0x70, (byte) 0x3a, (byte) 0x2f, (byte) 0x2f,
            (byte) 0x77, (byte) 0x77, (byte) 0x77, (byte) 0x2e, (byte) 0x67, (byte) 0x6f,
            (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e, (byte) 0x63,
            (byte) 0x6f, (byte) 0x6d, (byte) 0x0d, (byte) 0x0a, (byte) 0x45, (byte) 0x4e,
            (byte) 0x44, (byte) 0x3a, (byte) 0x56, (byte) 0x43, (byte) 0x41, (byte) 0x52,
            (byte) 0x44, (byte) 0x0d, (byte) 0x0a
    };

    /**
     * Send the text message "hello world" to a phone number.  This was generated using
     * the NXP reference phone.
     */
    private static final byte[] SEND_TEXT_MESSAGE = new byte[] {
            (byte) 0xd1, (byte) 0x02, (byte) 0x25, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
            (byte) 0x01, (byte) 0x21, (byte) 0x55, (byte) 0x00, (byte) 0x73, (byte) 0x6d,
            (byte) 0x73, (byte) 0x3a, (byte) 0x31, (byte) 0x36, (byte) 0x35, (byte) 0x30,
            (byte) 0x32, (byte) 0x35, (byte) 0x33, (byte) 0x30, (byte) 0x30, (byte) 0x30,
            (byte) 0x30, (byte) 0x3f, (byte) 0x62, (byte) 0x6f, (byte) 0x64, (byte) 0x79,
            (byte) 0x3d, (byte) 0x48, (byte) 0x65, (byte) 0x6c, (byte) 0x6c, (byte) 0x6f,
            (byte) 0x20, (byte) 0x77, (byte) 0x6f, (byte) 0x72, (byte) 0x6c, (byte) 0x64
    };

    /**
     * Call Google.  Generated using the NXP reference phone.
     */
    private static final byte[] CALL_GOOGLE = new byte[] {
            (byte) 0xd1, (byte) 0x02, (byte) 0x10, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
            (byte) 0x01, (byte) 0x0c, (byte) 0x55, (byte) 0x05, (byte) 0x31, (byte) 0x36,
            (byte) 0x35, (byte) 0x30, (byte) 0x32, (byte) 0x35, (byte) 0x33, (byte) 0x30,
            (byte) 0x30, (byte) 0x30, (byte) 0x30
    };

    /**
     * All the real ndef messages we've seen in the field.
     */
    public static final byte[][] ALL_MOCK_MESSAGES = new byte[][] {
            REAL_NFC_MSG, SMART_POSTER_URL_NO_TEXT, ENGLISH_PLAIN_TEXT,
            SMART_POSTER_URL_AND_TEXT, URI, VCARD, SEND_TEXT_MESSAGE,
            CALL_GOOGLE
    };

}
