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
import org.objectweb.asm.ClassReader;

import java.io.StringWriter;

/**
 * 
 */
public class ClassSourcerTest extends TestHelper {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBaseClassSource() throws Exception {
        StringWriter sw = new StringWriter();
        ClassReader cr = new ClassReader("data/TestBaseClass");
        
        ClassSourcer jw = new ClassSourcer(new Output(sw));
        cr.accept(jw, 0);
        
        assertSourceEquals(
                "package data;\n" + 
        		"public class TestBaseClass extends java.lang.Object implements java.lang.Runnable {\n" + 
        		"\n" + 
        		"    private final java.lang.String mArg;\n" + 
        		"    \n" + 
        		"    public TestBaseClass() {\n" + 
        		"        throw new RuntimeException(\"Stub\");" +
        		"    }\n" + 
        		"    public TestBaseClass(java.lang.String arg0) {\n" +
                "        throw new RuntimeException(\"Stub\");" +
        		"    }\n" +
        		"    public java.lang.String getArg() {\n" +
                "        throw new RuntimeException(\"Stub\");" +
        		"    }\n" +
        		"    public void run() {\n" + 
                "        throw new RuntimeException(\"Stub\");" +
        		"    }\n" + 
        		"}",
        		sw.toString());
    }

    @Test
    public void testInnerClassSource() throws Exception {
        StringWriter sw = new StringWriter();
        ClassReader cr = new ClassReader("data/TestInnerClass");
        
        ClassSourcer jw = new ClassSourcer(new Output(sw));
        cr.accept(jw, 0);
        
        assertSourceEquals(
                "package data;\n" + 
                "public class TestInnerClass extends java.lang.Object {\n" + 
                "    private final java.lang.String mArg;\n" + 
                "    public TestInnerClass() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public TestInnerClass(java.lang.String arg0) {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public java.lang.String getArg() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public data.TestInnerClass$InnerPubClass getInnerPubClass() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "}",
                sw.toString());
    }

    @Test
    public void testTemplateClassSource() throws Exception {
        StringWriter sw = new StringWriter();
        ClassReader cr = new ClassReader("data/TestTemplateClass");
        
        ClassSourcer jw = new ClassSourcer(new Output(sw));
        cr.accept(jw, 0);
        
        assertSourceEquals(
                "package data;\n" + 
                "public class TestTemplateClass<T extends java.io.InputStream, U extends java.lang.Object> extends java.lang.Object {\n" + 
                "    private final java.util.Map<T, U> mMap_T_U;\n" + 
                "    public java.util.Map<java.util.ArrayList<T>, java.util.Map<java.lang.String, java.util.ArrayList<U>>> mMap_T_S_U;\n" + 
                "    public TestTemplateClass() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public java.util.Map<T, U> getMap_T_U() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public java.util.Map<java.util.ArrayList<T>, java.util.Map<java.lang.String, java.util.ArrayList<U>>> getMap_T_S_U() {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public void draw(java.util.List<? extends org.w3c.dom.css.Rect> arg0) {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public static <T extends java.lang.Comparable<? super T>> void sort(java.util.List<T> arg0) {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" + 
                "    public <X extends T, Y extends java.lang.Object> void getMap(java.util.List<T> arg0, java.util.Map<T, U> arg1, java.util.Map<X, java.util.Set<? super Y>> arg2) {\n" + 
                "        throw new RuntimeException(\"Stub\");\n" + 
                "    }\n" +
                "}",
                sw.toString());
    }
    
}
