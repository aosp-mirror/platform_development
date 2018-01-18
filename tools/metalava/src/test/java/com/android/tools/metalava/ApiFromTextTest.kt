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

import org.intellij.lang.annotations.Language
import org.junit.Test

class ApiFromTextTest : DriverTest() {

    @Test
    fun `Loading a signature file and writing the API back out`() {
        val source = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public int clamp(int);
                    method public java.lang.Double convert(java.lang.Float);
                    field public static final java.lang.String ANY_CURSOR_ITEM_TYPE = "vnd.android.cursor.item/*";
                    field public java.lang.Number myNumber;
                  }
                }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Test generics, superclasses and interfaces`() {
        val source = """
            package a.b.c {
              public abstract interface MyStream<T, S extends a.b.c.MyStream<T, S>> implements java.lang.AutoCloseable {
              }
            }
            package test.pkg {
              public final class Foo extends java.lang.Enum {
                ctor public Foo(int);
                ctor public Foo(int, int);
                method public static test.pkg.Foo valueOf(java.lang.String);
                method public static final test.pkg.Foo[] values();
                enum_constant public static final test.pkg.Foo A;
                enum_constant public static final test.pkg.Foo B;
              }
              public abstract interface MyBaseInterface {
              }
              public abstract interface MyInterface<T> implements test.pkg.MyBaseInterface {
              }
              public abstract interface MyInterface2<T extends java.lang.Number> implements test.pkg.MyBaseInterface {
              }
              public static abstract class MyInterface2.Range<T extends java.lang.Comparable<? super T>> {
                ctor public MyInterface2.Range();
              }
              public static class MyInterface2.TtsSpan<C extends test.pkg.MyInterface<?>> {
                ctor public MyInterface2.TtsSpan();
              }
              public final class Test<T> {
                ctor public Test();
                method public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T);
                method public static <T & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);
                method public <X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X>) throws java.lang.Throwable;
                field public static java.util.List<java.lang.String> LIST;
              }
            }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Test constants`() {
        val source = """
                package test.pkg {
                  public class Foo2 {
                    ctor public Foo2();
                    field public static final java.lang.String GOOD_IRI_CHAR = "a-zA-Z0-9\u00a0-\ud7ff\uf900-\ufdcf\ufdf0-\uffef";
                    field public static final char HEX_INPUT = 61184; // 0xef00 '\uef00'
                    field protected int field00;
                    field public static final boolean field01 = true;
                    field public static final int field02 = 42; // 0x2a
                    field public static final long field03 = 42L; // 0x2aL
                    field public static final short field04 = 5; // 0x5
                    field public static final byte field05 = 5; // 0x5
                    field public static final char field06 = 99; // 0x0063 'c'
                    field public static final float field07 = 98.5f;
                    field public static final double field08 = 98.5;
                    field public static final java.lang.String field09 = "String with \"escapes\" and \u00a9...";
                    field public static final double field10 = (0.0/0.0);
                    field public static final double field11 = (1.0/0.0);
                  }
                }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Test inner classes`() {
        val source = """
                package test.pkg {
                  public abstract class Foo {
                    ctor public Foo();
                    method public static final deprecated synchronized void method1();
                    method public static final deprecated synchronized void method2();
                  }
                  protected static final deprecated class Foo.Inner1 {
                    ctor protected Foo.Inner1();
                  }
                  protected static abstract deprecated class Foo.Inner2 {
                    ctor protected Foo.Inner2();
                  }
                  protected static abstract deprecated interface Foo.Inner3 {
                    method public default void method3();
                    method public static void method4(int);
                  }
                }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Test throws`() {
        val source = """
                package test.pkg {
                  public final class Test<T> {
                    ctor public Test();
                    method public <X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X>) throws java.lang.Throwable;
                  }
                }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Loading a signature file with annotations on classes, fields, methods and parameters`() {
        @Language("TEXT")
        val source = """
                package test.pkg {
                  @android.support.annotation.UiThread public class MyTest {
                    ctor public MyTest();
                    method @android.support.annotation.IntRange(from=10, to=20) public int clamp(int);
                    method public java.lang.Double? convert(java.lang.Float myPublicName);
                    field public java.lang.Number? myNumber;
                  }
                }
                """

        check(
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            omitCommonPackages = false,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Enums and annotations`() {
        // In non-compat mode we write interfaces out as "@interface" (instead of abstract class)
        // and similarly for enums we write "enum" instead of "class extends java.lang.Enum".
        // Make sure we can also read this back in.
        val source = """
                package android.annotation {
                  public @interface SuppressLint {
                    method public abstract String[] value();
                  }
                }
                package test.pkg {
                  public enum Foo {
                    enum_constant public static final test.pkg.Foo A;
                    enum_constant public static final test.pkg.Foo B;
                  }
                }
                """

        check(
            compatibilityMode = false,
            outputKotlinStyleNulls = false,
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Enums and annotations exported to compat`() {
        val source = """
                package android.annotation {
                  public @interface SuppressLint {
                  }
                }
                package test.pkg {
                  public final enum Foo {
                    enum_constant public static final test.pkg.Foo A;
                    enum_constant public static final test.pkg.Foo B;
                  }
                }
                """

        check(
            compatibilityMode = true,
            signatureSource = source,
            api = """
                package android.annotation {
                  public abstract class SuppressLint implements java.lang.annotation.Annotation {
                  }
                }
                package test.pkg {
                  public final class Foo extends java.lang.Enum {
                    enum_constant public static final test.pkg.Foo A;
                    enum_constant public static final test.pkg.Foo B;
                  }
                }
                """
        )
    }

    @Test
    fun `Sort throws list by full name`() {
        check(
            compatibilityMode = true,
            signatureSource = """
                    package android.accounts {
                      public abstract interface AccountManagerFuture<V> {
                        method public abstract boolean cancel(boolean);
                        method public abstract V getResult() throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException;
                        method public abstract V getResult(long, java.util.concurrent.TimeUnit) throws android.accounts.OperationCanceledException, java.io.IOException, android.accounts.AuthenticatorException;
                        method public abstract boolean isCancelled();
                        method public abstract boolean isDone();
                      }
                    }
                    """,
            api = """
                    package android.accounts {
                      public abstract interface AccountManagerFuture<V> {
                        method public abstract boolean cancel(boolean);
                        method public abstract V getResult() throws android.accounts.AuthenticatorException, java.io.IOException, android.accounts.OperationCanceledException;
                        method public abstract V getResult(long, java.util.concurrent.TimeUnit) throws android.accounts.AuthenticatorException, java.io.IOException, android.accounts.OperationCanceledException;
                        method public abstract boolean isCancelled();
                        method public abstract boolean isDone();
                      }
                    }
                    """
        )
    }

}