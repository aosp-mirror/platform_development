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
import android.nfc.NdefMessage;

/**
 * Tests for {@link SmartPoster}.
 */
public class SmartPosterTest extends AndroidTestCase {
    public void testSmartPoster() throws Exception {
        NdefMessage msg = new NdefMessage(MockNdefMessages.REAL_NFC_MSG);

        SmartPoster poster = SmartPoster.from(msg.getRecords()[0]);
        assertEquals("NFC Forum Type 4 Tag", poster.getTitle());
        assertEquals("http://www.nxp.com/nfc", poster.getUri().toString());
    }
}
