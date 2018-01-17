/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.tools.metalava

import org.junit.Test

class KeepFileTest : DriverTest() {
    @Test
    fun `Generate Keep file`() {
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface MyInterface<T extends Object>
                            extends MyBaseInterface {
                    }
                    """
                ), java(
                    """
                    package a.b.c;
                    @SuppressWarnings("ALL")
                    public interface MyStream<T, S extends MyStream<T, S>> extends java.lang.AutoCloseable {
                    }
                    """
                ), java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface MyInterface2<T extends Number>
                            extends MyBaseInterface {
                        class TtsSpan<C extends MyInterface<?>> { }
                        abstract class Range<T extends Comparable<? super T>> {
                            protected String myString;
                        }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface MyBaseInterface {
                        void fun(int a, String b);
                    }
                    """
                )
            ),
            proguard = """
                -keep class a.b.c.MyStream {
                }
                -keep class test.pkg.MyBaseInterface {
                    public abstract void fun(int, java.lang.String);
                }
                -keep class test.pkg.MyInterface {
                }
                -keep class test.pkg.MyInterface2 {
                }
                -keep class test.pkg.MyInterface2${"$"}Range {
                    <init>();
                    protected java.lang.String myString;
                }
                -keep class test.pkg.MyInterface2${"$"}TtsSpan {
                    <init>();
                }
                """
        )
    }
}