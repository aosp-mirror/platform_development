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
package com.example.android.nfc.simulator;

/**
 * This class provides a list of fake NFC Ndef format Tags.
 */
public class MockNdefMessages {

    /**
     * A Smart Poster containing a URL and no text.
     */
    public static final byte[] SMART_POSTER_URL_NO_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x02, (byte) 0x0f, (byte) 0x53, (byte) 0x70, (byte) 0xd1,
            (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67, (byte) 0x6f,
            (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e, (byte) 0x63,
            (byte) 0x6f, (byte) 0x6d};

    /**
     * A plain text tag in english.
     */
    public static final byte[] ENGLISH_PLAIN_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x01, (byte) 0x1c, (byte) 0x54, (byte) 0x02, (byte) 0x65,
            (byte) 0x6e, (byte) 0x53, (byte) 0x6f, (byte) 0x6d, (byte) 0x65, (byte) 0x20,
            (byte) 0x72, (byte) 0x61, (byte) 0x6e, (byte) 0x64, (byte) 0x6f, (byte) 0x6d,
            (byte) 0x20, (byte) 0x65, (byte) 0x6e, (byte) 0x67, (byte) 0x6c, (byte) 0x69,
            (byte) 0x73, (byte) 0x68, (byte) 0x20, (byte) 0x74, (byte) 0x65, (byte) 0x78,
            (byte) 0x74, (byte) 0x2e};

    /**
     * Smart Poster containing a URL and Text.
     */
    public static final byte[] SMART_POSTER_URL_AND_TEXT =
        new byte[] {(byte) 0xd1, (byte) 0x02, (byte) 0x1c, (byte) 0x53, (byte) 0x70, (byte) 0x91,
            (byte) 0x01, (byte) 0x09, (byte) 0x54, (byte) 0x02, (byte) 0x65, (byte) 0x6e,
            (byte) 0x47, (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65,
            (byte) 0x51, (byte) 0x01, (byte) 0x0b, (byte) 0x55, (byte) 0x01, (byte) 0x67,
            (byte) 0x6f, (byte) 0x6f, (byte) 0x67, (byte) 0x6c, (byte) 0x65, (byte) 0x2e,
            (byte) 0x63, (byte) 0x6f, (byte) 0x6d};

    /**
     * All the mock Ndef tags.
     */
    public static final byte[][] ALL_MOCK_MESSAGES =
        new byte[][] {SMART_POSTER_URL_NO_TEXT, ENGLISH_PLAIN_TEXT, SMART_POSTER_URL_AND_TEXT};
}
