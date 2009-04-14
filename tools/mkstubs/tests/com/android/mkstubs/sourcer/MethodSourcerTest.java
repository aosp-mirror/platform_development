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
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import java.io.StringWriter;

/**
 * 
 */
public class MethodSourcerTest extends TestHelper {

    private StringWriter mWriter;
    private Output mOutput;

    @Before
    public void setUp() throws Exception {
        mWriter = new StringWriter();
        mOutput = new Output(mWriter);
    }

    @After
    public void tearDown() throws Exception {
        mWriter = null;
    }

    @Test
    public void testVoid() {
        MethodSourcer m = new MethodSourcer(mOutput,
                "foo", //classname
                Opcodes.ACC_PUBLIC, //access
                "testVoid", //name
                "()V", //desc
                null, //signature
                null); //exception
        m.visitEnd();
        
        assertSourceEquals(
                "public void testVoid() { }",
                mWriter.toString());
    }
    
    @Test
    public void testVoidThrow() {
        MethodSourcer m = new MethodSourcer(mOutput,
                "foo", //classname
                Opcodes.ACC_PUBLIC, //access
                "testVoid", //name
                "()V", //desc
                null, //signature
                new String[] { "java/lang/Exception" }); //exception
        m.visitEnd();
        
        assertSourceEquals(
                "public void testVoid() throws java.lang.Exception { }",
                mWriter.toString());
    }
    
    @Test
    public void testReturnMap() {
        MethodSourcer m = new MethodSourcer(mOutput,
                "foo", //classname
                Opcodes.ACC_PUBLIC, //access
                "getMap_T_U", //name
                "()Ljava/util/Map;", //desc
                "()Ljava/util/Map<TT;TU;>;", //signature
                null); //exception
        m.visitEnd();
        
        assertSourceEquals(
                "public java.util.Map<T, U> getMap_T_U() { }",
                mWriter.toString());
    }

}
