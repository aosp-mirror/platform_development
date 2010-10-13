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

import android.test.AndroidTestCase;
import com.google.common.primitives.Bytes;
import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;

public class NdefUtilTest extends AndroidTestCase {
    public void testToText() throws UnsupportedEncodingException {
        checkWord("Hello", "en-US", true);
        checkWord("Hello", "en-US", false);
        checkWord("abc\\u5639\\u563b", "cp1251", true);
        checkWord("abc\\u5639\\u563b", "cp1251", false);
    }

    private static void checkWord(String word, String encoding, boolean isUtf8) throws UnsupportedEncodingException {
        String utfEncoding = isUtf8 ? "UTF-8" : "UTF-16";

        byte[] encodingBytes = encoding.getBytes("US-ASCII");
        byte[] text = word.getBytes(utfEncoding);

        int utfBit = isUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + encodingBytes.length);

        byte[] data = Bytes.concat(
           new byte[] { (byte) status },
           encodingBytes,
           text
        );

        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
        assertEquals(word, NdefUtil.toText(record));
    }
}
