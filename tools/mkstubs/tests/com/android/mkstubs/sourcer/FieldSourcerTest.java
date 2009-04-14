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


import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.StringWriter;

/**
 * 
 */
public class FieldSourcerTest {

    private StringWriter mWriter;

    @Before
    public void setUp() throws Exception {
        mWriter = new StringWriter();
    }

    @After
    public void tearDown() throws Exception {
        mWriter = null;
    }
    
    @Test
    public void testStringField() throws Exception {
        
        FieldSourcer fs = new FieldSourcer(new Output(mWriter),
                Opcodes.ACC_PUBLIC, // access
                "mArg", // name
                "Ljava/lang/String;", // desc
                null // signature
                );
        fs.visitEnd();

        String s = mWriter.toString();
        Assert.assertEquals("public java.lang.String mArg;\n", s);
    }
    
    @Test
    public void testTemplateTypeField() throws Exception {
        
        FieldSourcer fs = new FieldSourcer(new Output(mWriter),
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, // access
                "mList", // name
                "Ljava/util/ArrayList;", // desc
                "Ljava/util/ArrayList<Ljava/lang/String;>;" // signature
                );
        fs.visitEnd();

        String s = mWriter.toString();
        Assert.assertEquals("private final java.util.ArrayList<java.lang.String> mList;\n", s);
    }

}
