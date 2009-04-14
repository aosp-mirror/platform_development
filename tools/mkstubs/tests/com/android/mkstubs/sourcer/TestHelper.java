/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.mkstubs.sourcer;

import org.junit.Assert;

/**
 * 
 */
abstract class TestHelper {

    /**
     * Test source equality after normalizing all whitespace.
     */
    public void assertSourceEquals(String expected, String actual) {
        String en = expected.replaceAll("[\\s]+", " ").trim();
        String an = actual.replaceAll(  "[\\s]+", " ").trim();
        
        Assert.assertEquals(
                String.format("Source comparison failure: expected:<%s> but was:<%s>", expected, actual),
                en, an);
    }
}
