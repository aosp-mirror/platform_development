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
package com.android.monkeyrunner.adb;

import com.google.common.base.Joiner;
import com.google.common.io.Resources;

import junit.framework.TestCase;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Unit Tests for AdbMonkeyDevice.
 */
public class AdbMonkeyDeviceTest extends TestCase {
    private static String MULTILINE_RESULT = "\r\n" +
    "Test results for InstrumentationTestRunner=.\r\n" +
    "Time: 2.242\r\n" +
    "\r\n" +
    "OK (1 test)";

    private static String getResource(String resName) throws IOException {
        URL resource = Resources.getResource(AdbMonkeyDeviceTest.class, resName);
        List<String> lines = Resources.readLines(resource, Charset.defaultCharset());
        return Joiner.on("\r\n").join(lines);
    }

    public void testSimpleResultParse() throws IOException {
        String result = getResource("instrument_result.txt");
        Map<String, Object> convertedResult = AdbMonkeyDevice.convertInstrumentResult(result);

        assertEquals("one", convertedResult.get("result1"));
        assertEquals("two", convertedResult.get("result2"));
    }

    public void testMultilineResultParse() throws IOException {
        String result = getResource("multiline_instrument_result.txt");
        Map<String, Object> convertedResult = AdbMonkeyDevice.convertInstrumentResult(result);

        assertEquals(MULTILINE_RESULT, convertedResult.get("stream"));
    }
}
