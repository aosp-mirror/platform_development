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

package com.android.mkstubs;


import com.android.mkstubs.Main.Logger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.io.StringWriter;

/**
 *
 */
public class SourceGeneratorTest {

    private SourceGenerator mGen;

    @Before
    public void setUp() throws Exception {
        mGen = new SourceGenerator(new Logger(false));
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testDumpClass() throws Exception {
        StringWriter sw = new StringWriter();
        ClassReader cr = new ClassReader("data/TestBaseClass");

        mGen.visitClassSource(sw, cr, new Filter());

        String s = sw.toString();
        Assert.assertNotNull(s);
    }
}
