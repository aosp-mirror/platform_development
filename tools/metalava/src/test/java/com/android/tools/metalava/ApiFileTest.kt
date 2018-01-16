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

@file:Suppress("ALL")

package com.android.tools.metalava

import org.junit.Test

class ApiFileTest : DriverTest() {
/*
   Conditions to test:
   - test all the error scenarios found in the notStrippable case!
   - split up test into many individual test cases
   - try referencing a class from an annotation!
   - test having a throws list where some exceptions are hidden but extend
     public exceptions: do we map over to the referenced ones?

   - test type reference from all the possible places -- in type signatures - interfaces,
     extends, throws, type bounds, etc.
   - method which overrides @hide method: should appear in subclass (test chain
     of two nested too)
   - BluetoothGattCharacteristic.java#describeContents: Was marked @hide,
     but is unhidden because it extends a public interface method
   - package javadoc (also make sure merging both!, e.g. try having @hide in each)
   - StopWatchMap -- inner class with @hide marks allh top levels!
   - Test field inlining: should I include fields from an interface, if that
     inteface was implemented by the parent class (and therefore appears there too?)
     What if the superclass is abstract?
   - Exposing package private classes. Test that I only do this for package private
     classes, NOT Those marked @hide (is that, having @hide on a used type, illegal?)
   - Test error handling (invalid @hide combinations))
   - Consider what happens if we promote a package private class (because it's
     extended by a public class), and then we restore its public members; the
     override logic there isn't quite right. We've duplicated the significant-override
     code to not skip private members, but that could change semantics. This isn't
     ideal; instead we should now mark this class as public, and re-run the analysis
     again (with the new hidden state for this class).
   - compilation unit sorting - top level classes out of order
   - Massive classes such as android.R.java? Maybe do synthetic test.
   - HttpResponseCache implemented a public OkHttp interface, but the sole implementation
     method was marked @hide, so the method doesn't show up. Is that some other rule --
     that we skip interfaces if their implementation methods are marked @hide?
   - Test recursive package filtering.
 */

    @Test
    fun `Basic class signature extraction`() {
        // Basic class; also checks that default constructor is made explicit
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class Foo {
                    }
                    """
                )
            ),
            api = """
                    package test.pkg {
                      public class Foo {
                        ctor public Foo();
                      }
                    }
                    """
        )
    }

    @Test
    fun `Basic Kotlin class`() {
        check(
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg
                    class Kotlin(val property1: String = "Default Value", arg2: Int) : Parent() {
                        override fun method() = "Hello World"
                        fun otherMethod(ok: Boolean, times: Int) {
                        }

                        var property2: String? = null

                        private var someField = 42
                        @JvmField
                        var someField2 = 42
                    }

                    open class Parent {
                        open fun method(): String? = null
                        open fun method2(value: Boolean, value: Boolean?): String? = null
                        open fun method3(value: Int?, value2: Int): Int = null
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class Kotlin extends test.pkg.Parent {
                    ctor public Kotlin(java.lang.String, int);
                    method public final java.lang.String getProperty1();
                    method public final java.lang.String getProperty2();
                    method public final void otherMethod(boolean, int);
                    method public final void setProperty2(java.lang.String);
                    field public int someField2;
                  }
                  public class Parent {
                    ctor public Parent();
                    method public java.lang.String method();
                    method public java.lang.String method2(boolean, java.lang.Boolean);
                    method public int method3(java.lang.Integer, int);
                  }
                }
                """,
            checkDoclava1 = false /* doesn't support Kotlin... */
        )
    }

    @Test
    fun `Propagate Platform types in Kotlin`() {
        check(
            compatibilityMode = false,
            outputKotlinStyleNulls = true,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    // Nullable Pair in Kotlin
                    package androidx.util

                    class NullableKotlinPair<out F, out S>(val first: F?, val second: S?)
                    """
                ),
                kotlin(
                    """
                    // Non-nullable Pair in Kotlin
                    package androidx.util
                    class NonNullableKotlinPair<out F: Any, out S: Any>(val first: F, val second: S)
                    """
                ),
                java(
                    """
                    // Platform nullability Pair in Java
                    package androidx.util;

                    @SuppressWarnings("WeakerAccess")
                    public class PlatformJavaPair<F, S> {
                        public final F first;
                        public final S second;

                        public PlatformJavaPair(F first, S second) {
                            this.first = first;
                            this.second = second;
                        }
                    }
                """
                ),
                java(
                    """
                    // Platform nullability Pair in Java
                    package androidx.util;
                    import android.support.annotation.NonNull;
                    import android.support.annotation.Nullable;

                    @SuppressWarnings("WeakerAccess")
                    public class NullableJavaPair<F, S> {
                        public final @Nullable F first;
                        public final @Nullable S second;

                        public NullableJavaPair(@Nullable F first, @Nullable S second) {
                            this.first = first;
                            this.second = second;
                        }
                    }
                    """
                ),
                java(
                    """
                    // Platform nullability Pair in Java
                    package androidx.util;

                    import android.support.annotation.NonNull;

                    @SuppressWarnings("WeakerAccess")
                    public class NonNullableJavaPair<F, S> {
                        public final @NonNull F first;
                        public final @NonNull S second;

                        public NonNullableJavaPair(@NonNull F first, @NonNull S second) {
                            this.first = first;
                            this.second = second;
                        }
                    }
                    """
                ),
                kotlin(
                    """
                    package androidx.util

                    @Suppress("HasPlatformType") // Intentionally propagating platform type with unknown nullability.
                    inline operator fun <F, S> PlatformJavaPair<F, S>.component1() = first
                    """
                ),
                supportNonNullSource,
                supportNullableSource
            ),
            api = """
                    package androidx.util {
                      public class NonNullableJavaPair<F, S> {
                        ctor public NonNullableJavaPair(F, S);
                        field public final F first;
                        field public final S second;
                      }
                      public final class NonNullableKotlinPair<F, S> {
                        ctor public NonNullableKotlinPair(F, S);
                        method public final F getFirst();
                        method public final S getSecond();
                      }
                      public class NullableJavaPair<F, S> {
                        ctor public NullableJavaPair(F?, S?);
                        field public final F? first;
                        field public final S? second;
                      }
                      public final class NullableKotlinPair<F, S> {
                        ctor public NullableKotlinPair(F?, S?);
                        method public final F? getFirst();
                        method public final S? getSecond();
                      }
                      public class PlatformJavaPair<F, S> {
                        ctor public PlatformJavaPair(F!, S!);
                        field public final F! first;
                        field public final S! second;
                      }
                      public final class TestKt {
                        ctor public TestKt();
                        method public static final <F, S> F! component1(androidx.util.PlatformJavaPair<F,S>);
                      }
                    }
                """,
            extraArguments = arrayOf("--hide-package", "android.support.annotation"),
            checkDoclava1 = false /* doesn't support Kotlin... */
        )
    }

    @Test
    fun `Extract class with generics`() {
        // Basic interface with generics; makes sure <T extends Object> is written as just <T>
        // Also include some more complex generics expressions to make sure they're serialized
        // correctly (in particular, using fully qualified names instead of what appears in
        // the source code.)
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
                    public interface MyStream<T, S extends MyStream<T, S>> extends test.pkg.AutoCloseable {
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
                ),
                java(
                    """
                    package test.pkg;
                    public interface MyOtherInterface extends MyBaseInterface, AutoCloseable {
                        void fun(int a, String b);
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface AutoCloseable {
                    }
                    """
                )
            ),
            api = """
                    package a.b.c {
                      public abstract interface MyStream<T, S extends a.b.c.MyStream<T, S>> implements test.pkg.AutoCloseable {
                      }
                    }
                    package test.pkg {
                      public abstract interface AutoCloseable {
                      }
                      public abstract interface MyBaseInterface {
                        method public abstract void fun(int, java.lang.String);
                      }
                      public abstract interface MyInterface<T> implements test.pkg.MyBaseInterface {
                      }
                      public abstract interface MyInterface2<T extends java.lang.Number> implements test.pkg.MyBaseInterface {
                      }
                      public static abstract class MyInterface2.Range<T extends java.lang.Comparable<? super T>> {
                        ctor public MyInterface2.Range();
                        field protected java.lang.String myString;
                      }
                      public static class MyInterface2.TtsSpan<C extends test.pkg.MyInterface<?>> {
                        ctor public MyInterface2.TtsSpan();
                      }
                      public abstract interface MyOtherInterface implements test.pkg.AutoCloseable test.pkg.MyBaseInterface {
                      }
                    }
                """
        )
    }

    @Test
    fun `Basic class without default constructor, has constructors with args`() {
        // Class without private constructors (shouldn't insert default constructor)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class Foo {
                        public Foo(int i) {

                        }
                        public Foo(int i, int j) {
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class Foo {
                    ctor public Foo(int);
                    ctor public Foo(int, int);
                  }
                }
                """
        )
    }

    @Test
    fun `Basic class without default constructor, has private constructor`() {
        // Class without private constructors; no default constructor should be inserted
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class Foo {
                        private Foo() {
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class Foo {
                  }
                }
                """
        )
    }

    @Test
    fun `Interface class extraction`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface Foo {
                        void foo();
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public abstract interface Foo {
                    method public abstract void foo();
                  }
                }
                """
        )
    }

    @Test
    fun `Enum class extraction`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public enum Foo {
                        A, B;
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class Foo extends java.lang.Enum {
                    method public static test.pkg.Foo valueOf(java.lang.String);
                    method public static final test.pkg.Foo[] values();
                    enum_constant public static final test.pkg.Foo A;
                    enum_constant public static final test.pkg.Foo B;
                  }
                }
                """
        )
    }

    @Test
    fun `Enum class, non-compat mode`() {
        @Suppress("ConstantConditionIf")
        if (SKIP_NON_COMPAT) {
            println("Skipping test for non-compatibility mode which isn't fully done yet")
            return
        }

        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public enum Foo {
                        A, B;
                    }
                    """
                )
            ),
            compatibilityMode = false,
            api = """
                package test.pkg {
                  public enum Foo {
                    enum_constant public static final test.pkg.Foo! A;
                    enum_constant public static final test.pkg.Foo! B;
                  }
                }
                """
        )
    }

    @Test
    fun `Annotation class extraction`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        check(
            // For unknown reasons, doclava1 behaves differently here than when invoked on the
            // whole platform
            checkDoclava1 = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public @interface Foo {
                        String value();
                    }
                    """
                ),
                java(
                    """
                    package android.annotation;
                    import static java.lang.annotation.ElementType.*;
                    import java.lang.annotation.*;
                    @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
                    @Retention(RetentionPolicy.CLASS)
                    @SuppressWarnings("ALL")
                    public @interface SuppressLint {
                        String[] value();
                    }
                """
                )
            ),
            api = """
                package android.annotation {
                  public abstract class SuppressLint implements java.lang.annotation.Annotation {
                  }
                }
                package test.pkg {
                  public abstract class Foo implements java.lang.annotation.Annotation {
                  }
                }
                """
        )
    }

    @Test
    fun `Annotation class extraction, non-compat mode`() {
        @Suppress("ConstantConditionIf")
        if (SKIP_NON_COMPAT) {
            println("Skipping test for non-compatibility mode which isn't fully done yet")
            return
        }

        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public @interface Foo {
                        String value();
                    }
                    """
                ),
                java(
                    """
                    package android.annotation;
                    import static java.lang.annotation.ElementType.*;
                    import java.lang.annotation.*;
                    @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
                    @Retention(RetentionPolicy.CLASS)
                    @SuppressWarnings("ALL")
                    public @interface SuppressLint {
                        String[] value();
                    }
                    """
                )
            ),
            compatibilityMode = false,
            api = """
                package android.annotation {
                  public @interface SuppressLint {
                    method public abstract String[]! value();
                  }
                }
                package test.pkg {
                  public @interface Foo {
                    method public abstract String! value();
                  }
                }
                """
        )
    }

    @Test
    fun `Superclass signature extraction`() {
        // Make sure superclass statement is correct; inherited method from parent that has same
        // signature isn't included in the child
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class Foo extends Super {
                        @Override public void base() { }
                        public void child() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class Super {
                        public void base() { }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class Foo extends test.pkg.Super {
                    ctor public Foo();
                    method public void child();
                  }
                  public class Super {
                    ctor public Super();
                    method public void base();
                  }
                }
                """
        )
    }

    @Test
    fun `Extract fields with types and initial values`() {
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class Foo {
                        private int hidden = 1;
                        int hidden2 = 2;
                        /** @hide */
                        int hidden3 = 3;

                        protected int field00; // No value
                        public static final boolean field01 = true;
                        public static final int field02 = 42;
                        public static final long field03 = 42L;
                        public static final short field04 = 5;
                        public static final byte field05 = 5;
                        public static final char field06 = 'c';
                        public static final float field07 = 98.5f;
                        public static final double field08 = 98.5;
                        public static final String field09 = "String with \"escapes\" and \u00a9...";
                        public static final double field10 = Double.NaN;
                        public static final double field11 = Double.POSITIVE_INFINITY;

                        public static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00a0-\ud7ff\uf900-\ufdcf\ufdf0-\uffef";
                        public static final char HEX_INPUT = 61184;
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class Foo {
                    ctor public Foo();
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
        )
    }

    @Test
    fun `Check all modifiers`() {
        // Include as many modifiers as possible to see which ones are included
        // in the signature files, and the expected sorting order.
        // Note that the signature files treat "deprecated" as a fake modifier.
        // Note also how the "protected" modifier on the interface method gets
        // promoted to public.
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public abstract class Foo {
                        @Deprecated private static final long field1 = 5;
                        @Deprecated private static volatile long field2 = 5;
                        @Deprecated public static strictfp final synchronized void method1() { }
                        @Deprecated public static final synchronized native void method2();
                        @Deprecated protected static final class Inner1 { }
                        @Deprecated protected static abstract  class Inner2 { }
                        @Deprecated protected interface Inner3 {
                            protected default void method3() { }
                            static void method4(final int arg) { }
                        }
                    }
                    """
                )
            ),

            warnings = """
                        src/test/pkg/Foo.java:8: warning: Method test.pkg.Foo.method1(): @Deprecated annotation (present) and @deprecated doc tag (not present) do not match [DeprecationMismatch:113]
                        src/test/pkg/Foo.java:7: warning: Method test.pkg.Foo.method2(): @Deprecated annotation (present) and @deprecated doc tag (not present) do not match [DeprecationMismatch:113]
                        src/test/pkg/Foo.java:6: warning: Class test.pkg.Foo.Inner1: @Deprecated annotation (present) and @deprecated doc tag (not present) do not match [DeprecationMismatch:113]
                        src/test/pkg/Foo.java:5: warning: Class test.pkg.Foo.Inner2: @Deprecated annotation (present) and @deprecated doc tag (not present) do not match [DeprecationMismatch:113]
                        src/test/pkg/Foo.java:4: warning: Class test.pkg.Foo.Inner3: @Deprecated annotation (present) and @deprecated doc tag (not present) do not match [DeprecationMismatch:113]
                        """,

            api = """
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
        )
    }

    @Test
    fun `Check all modifiers, non-compat mode`() {
        @Suppress("ConstantConditionIf")
        if (SKIP_NON_COMPAT) {
            @Suppress("ConstantConditionIf")
            println("Skipping test for non-compatibility mode which isn't fully done yet")
            return
        }

        // Like testModifiers but turns off compat mode, such that we have
        // a modifier order more in line with standard code conventions
        check(
            compatibilityMode = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public abstract class Foo {
                        @Deprecated private static final long field1 = 5;
                        @Deprecated private static volatile long field2 = 5;
                        /** @deprecated */ @Deprecated public static strictfp final synchronized void method1() { }
                        /** @deprecated */ @Deprecated public static final synchronized native void method2();
                        /** @deprecated */ @Deprecated protected static final class Inner1 { }
                        /** @deprecated */ @Deprecated protected static abstract class Inner2 { }
                        /** @deprecated */ @Deprecated protected interface Inner3 {
                            protected default void method3() { }
                            static void method4(final int arg) { }
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public abstract class Foo {
                    ctor public Foo();
                    method deprecated public static final synchronized strictfp void method1();
                    method deprecated public static final synchronized native void method2();
                  }
                  deprecated protected static final class Foo.Inner1 {
                    ctor protected Foo.Inner1();
                  }
                  deprecated protected abstract static class Foo.Inner2 {
                    ctor protected Foo.Inner2();
                  }
                  deprecated protected static interface Foo.Inner3 {
                    method public default void method3();
                    method public static void method4(int);
                  }
                }
                """
        )
    }

    @Test
    fun `Package with only hidden classes should be removed from signature files`() {
        // Checks that if we have packages that are hidden, or contain only hidden or doconly
        // classes, the entire package is omitted from the signature file. Note how the test.pkg1.sub
        // package is not marked @hide, but doclava now treats subpackages of a hidden package
        // as also hidden.
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    ${"/** @hide hidden package */" /* avoid dangling javadoc warning */}
                    package test.pkg1;
                    """
                ),
                java(
                    """
                    package test.pkg1;
                    @SuppressWarnings("ALL")
                    public class Foo {
                        // Hidden by package hide
                    }
                    """
                ),
                java(
                    """
                    package test.pkg2;
                    /** @hide hidden class in this package */
                    @SuppressWarnings("ALL")
                    public class Bar {
                    }
                    """
                ),
                java(
                    """
                    package test.pkg2;
                    /** @doconly hidden class in this package */
                    @SuppressWarnings("ALL")
                    public class Baz {
                    }
                    """
                ),
                java(
                    """
                    package test.pkg1.sub;
                    // Hidden by @hide in package above
                    @SuppressWarnings("ALL")
                    public class Test {
                    }
                    """
                ),
                java(
                    """
                    package test.pkg3;
                    // The only really visible class
                    @SuppressWarnings("ALL")
                    public class Boo {
                    }
                    """
                )
            ),
            api = """
                package test.pkg3 {
                  public class Boo {
                    ctor public Boo();
                  }
                }
                """
        )
    }

    @Test
    fun `Enums can be abstract`() {
        // As per https://bugs.openjdk.java.net/browse/JDK-6287639
        // abstract methods in enums should not be listed as abstract,
        // but doclava1 does, so replicate this.
        // Also checks that we handle both enum fields and regular fields
        // and that they are listed separately.

        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public enum FooBar {
                        ABC {
                            @Override
                            protected void foo() { }
                        }, DEF {
                            @Override
                            protected void foo() { }
                        };

                        protected abstract void foo();
                        public static int field1 = 1;
                        public int field2 = 2;
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class FooBar extends java.lang.Enum {
                    method protected abstract void foo();
                    method public static test.pkg.FooBar valueOf(java.lang.String);
                    method public static final test.pkg.FooBar[] values();
                    enum_constant public static final test.pkg.FooBar ABC;
                    enum_constant public static final test.pkg.FooBar DEF;
                    field public static int field1;
                    field public int field2;
                  }
                }
            """
        )
    }

    @Test
    fun `Check erasure in throws-list`() {
        // Makes sure that when we have a generic signature in the throws list we take
        // the erasure instead (in compat mode); "Throwable" instead of "X" in the below
        // test. Real world example: Optional.orElseThrow.
        check(
            compatibilityMode = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.util.function.Supplier;

                    @SuppressWarnings("ALL")
                    public final class Test<T> {
                        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
                            return null;
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class Test<T> {
                    ctor public Test();
                    method public <X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X>) throws java.lang.Throwable;
                  }
                }
                """
        )
    }

    @Test
    fun `Check various generics signature subtleties`() {
        // Some additional declarations where PSI default type handling diffs from doclava1
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public abstract class Collections {
                        public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T> collection) {
                            return null;
                        }
                        public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T t);
                        public final class Range<T extends java.lang.Comparable<? super T>> { }
                    }
                    """
                ), java(
                    """
                    package test.pkg;

                    import java.util.Set;

                    @SuppressWarnings("ALL")
                    public class MoreAsserts {
                        public static void assertEquals(String arg0, Set<? extends Object> arg1, Set<? extends Object> arg2) { }
                        public static void assertEquals(Set<? extends Object> arg1, Set<? extends Object> arg2) { }
                    }

                    """
                )
            ),

            // This is the output from doclava1; I'm not quite matching this yet (sorting order differs,
            // and my heuristic to remove "extends java.lang.Object" is somehow preserved here. I'm
            // not clear on when they do it and when they don't.
            /*
            api = """
            package test.pkg {
              public abstract class Collections {
                ctor public Collections();
                method public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T);
                method public static <T & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);
              }
              public final class Collections.Range<T extends java.lang.Comparable<? super T>> {
                ctor public Collections.Range();
              }
              public class MoreAsserts {
                ctor public MoreAsserts();
                method public static void assertEquals(java.util.Set<? extends java.lang.Object>, java.util.Set<? extends java.lang.Object>);
                method public static void assertEquals(java.lang.String, java.util.Set<? extends java.lang.Object>, java.util.Set<? extends java.lang.Object>);
              }
            }
            """,
            */
            api = """
                package test.pkg {
                  public abstract class Collections {
                    ctor public Collections();
                    method public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T);
                    method public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);
                  }
                  public final class Collections.Range<T extends java.lang.Comparable<? super T>> {
                    ctor public Collections.Range();
                  }
                  public class MoreAsserts {
                    ctor public MoreAsserts();
                    method public static void assertEquals(java.lang.String, java.util.Set<?>, java.util.Set<?>);
                    method public static void assertEquals(java.util.Set<?>, java.util.Set<?>);
                  }
                }
                """,

            // Can't check doclava1 on this: its output doesn't match javac, e.g. for the above declaration
            // of max, javap shows this signature:
            //   public static <T extends java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);
            // which matches metalava's output:
            //   method public static <T & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);
            // and not doclava1:
            //   method public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T>);

            checkDoclava1 = false
        )
    }

    @Test
    fun `Check instance methods in enums`() {
        // Make sure that when we have instance methods in an enum they're handled
        // correctly (there's some special casing around enums to insert extra methods
        // that was broken, as exposed by ChronoUnit#toString)
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public interface TempUnit {
                        @Override
                        String toString();
                    }
                     """
                ),
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("ALL")
                    public enum ChronUnit implements TempUnit {
                        C, B, A;

                        public String valueOf(int x) {
                            return Integer.toString(x + 5);
                        }

                        public String values(String separator) {
                            return null;
                        }

                        @Override
                        public String toString() {
                            return name();
                        }
                    }
                """
                )
            ),
            importedPackages = emptyList(),
            api = """
                package test.pkg {
                  public final class ChronUnit extends java.lang.Enum implements test.pkg.TempUnit {
                    method public static test.pkg.ChronUnit valueOf(java.lang.String);
                    method public java.lang.String valueOf(int);
                    method public static final test.pkg.ChronUnit[] values();
                    method public final java.lang.String values(java.lang.String);
                    enum_constant public static final test.pkg.ChronUnit A;
                    enum_constant public static final test.pkg.ChronUnit B;
                    enum_constant public static final test.pkg.ChronUnit C;
                  }
                  public abstract interface TempUnit {
                    method public abstract java.lang.String toString();
                  }
                }
                """
        )
    }

    @Test
    fun `Mixing enums and fields`() {
        // Checks sorting order of enum constant values
        val source = """
            package java.nio.file.attribute {
              public final class AclEntryPermission extends java.lang.Enum {
                method public static java.nio.file.attribute.AclEntryPermission valueOf(java.lang.String);
                method public static final java.nio.file.attribute.AclEntryPermission[] values();
                enum_constant public static final java.nio.file.attribute.AclEntryPermission APPEND_DATA;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission DELETE;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission DELETE_CHILD;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission EXECUTE;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission READ_ACL;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission READ_ATTRIBUTES;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission READ_DATA;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission READ_NAMED_ATTRS;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission SYNCHRONIZE;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission WRITE_ACL;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission WRITE_ATTRIBUTES;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission WRITE_DATA;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission WRITE_NAMED_ATTRS;
                enum_constant public static final java.nio.file.attribute.AclEntryPermission WRITE_OWNER;
                field public static final java.nio.file.attribute.AclEntryPermission ADD_FILE;
                field public static final java.nio.file.attribute.AclEntryPermission ADD_SUBDIRECTORY;
                field public static final java.nio.file.attribute.AclEntryPermission LIST_DIRECTORY;
              }
            }
                    """
        check(
            signatureSource = source,
            api = source
        )
    }

    @Test
    fun `Superclass filtering, should skip intermediate hidden classes`() {
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class MyClass extends HiddenParent {
                        public void method4() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    /** @hide */
                    @SuppressWarnings("ALL")
                    public class HiddenParent extends HiddenParent2 {
                        public static final String CONSTANT = "MyConstant";
                        protected int mContext;
                        public void method3() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    /** @hide */
                    @SuppressWarnings("ALL")
                    public class HiddenParent2 extends PublicParent {
                        public void method2() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class PublicParent {
                        public void method1() { }
                    }
                    """
                )
            ),
            // Notice how the intermediate methods (method2, method3) have been removed
            includeStrippedSuperclassWarnings = true,
            warnings = "src/test/pkg/MyClass.java:3: warning: Public class test.pkg.MyClass stripped of unavailable superclass test.pkg.HiddenParent [HiddenSuperclass:111]",
            api = """
                package test.pkg {
                  public class MyClass extends test.pkg.PublicParent {
                    ctor public MyClass();
                    method public void method4();
                  }
                  public class PublicParent {
                    ctor public PublicParent();
                    method public void method1();
                  }
                }
                """
        )
    }

    @Test
    fun `Inheriting from package private classes, package private class should be included`() {
        check(
            checkDoclava1 = true,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public class MyClass extends HiddenParent {
                        public void method1() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    class HiddenParent {
                        public static final String CONSTANT = "MyConstant";
                        protected int mContext;
                        public void method2() { }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class MyClass {
                        ctor public MyClass();
                        method public void method1();
                      }
                    }
            """
        )
    }

    @Test
    fun `When implementing rather than extending package private class, inline members instead`() {
        // If you implement a package private interface, we just remove it and inline the members into
        // the subclass
        check(
            compatibilityMode = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass implements HiddenInterface {
                        @Override public void method() { }
                        @Override public void other() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface OtherInterface {
                        void other();
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    interface HiddenInterface extends OtherInterface {
                        void method() { }
                        String CONSTANT = "MyConstant";
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class MyClass implements test.pkg.OtherInterface {
                    ctor public MyClass();
                    method public void method();
                    method public void other();
                    field public static final java.lang.String CONSTANT = "MyConstant";
                  }
                  public abstract interface OtherInterface {
                    method public abstract void other();
                  }
                }
                """
        )
    }

    @Test
    fun `Implementing package private class, non-compat mode`() {
        @Suppress("ConstantConditionIf")
        if (SKIP_NON_COMPAT) {
            println("Skipping test for non-compatibility mode which isn't fully done yet")
            return
        }

        // Like the previous test, but in non compat mode we correctly
        // include all the non-hidden public interfaces into the signature

        // BUG: Note that we need to implement the parent
        check(
            compatibilityMode = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass implements HiddenInterface {
                        @Override public void method() { }
                        @Override public void other() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface OtherInterface {
                        void other();
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    interface HiddenInterface extends OtherInterface {
                        void method() { }
                        String CONSTANT = "MyConstant";
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class MyClass implements test.pkg.OtherInterface {
                    ctor public MyClass();
                    method public void method();
                    method public void other();
                    field public static final String! CONSTANT = "MyConstant";
                  }
                  public interface OtherInterface {
                    method public void other();
                  }
                }
                """
        )
    }

    @Test
    fun `Default modifiers should be omitted`() {
        // If signatures vary only by the "default" modifier in the interface, don't show it on the implementing
        // class
        check(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    public class MyClass implements SuperInterface {
                        @Override public void method() {  }
                        @Override public void method2() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;

                    public interface SuperInterface {
                        void method();
                        default void method2() {
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class MyClass implements test.pkg.SuperInterface {
                    ctor public MyClass();
                    method public void method();
                  }
                  public abstract interface SuperInterface {
                    method public abstract void method();
                    method public default void method2();
                  }
                }
            """
        )
    }

    @Test
    fun `Override via different throws list should be included`() {
        // If a method overrides another but changes the throws list, the overriding
        // method must be listed in the subclass. This is observed for example in
        // AbstractCursor#finalize, which omits the throws clause from Object's finalize.
        check(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    public abstract class AbstractCursor extends Parent {
                        @Override protected void finalize2() {  } // note: not throws Throwable!
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("RedundantThrows")
                    public class Parent {
                        protected void finalize2() throws Throwable {
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public abstract class AbstractCursor extends test.pkg.Parent {
                    ctor public AbstractCursor();
                    method protected void finalize2();
                  }
                  public class Parent {
                    ctor public Parent();
                    method protected void finalize2() throws java.lang.Throwable;
                  }
                }
            """
        )
    }

    @Test
    fun `Implementing interface method`() {
        // If you have a public method that implements an interface method,
        // they'll vary in the "abstract" modifier, but it shouldn't be listed on the
        // class. This is an issue for example for the ZonedDateTime#getLong method
        // implementing the TemporalAccessor#getLong method
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public interface SomeInterface {
                        long getLong();
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface SomeInterface2 {
                        @Override default long getLong() {
                            return 42;
                        }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public class Foo implements SomeInterface, SomeInterface2 {
                        @Override
                        public long getLong() { return 0L; }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class Foo implements test.pkg.SomeInterface test.pkg.SomeInterface2 {
                    ctor public Foo();
                  }
                  public abstract interface SomeInterface {
                    method public abstract long getLong();
                  }
                  public abstract interface SomeInterface2 {
                    method public default long getLong();
                  }
                }
                """
        )
    }

    @Test
    fun `Check basic @remove scenarios`() {
        // Test basic @remove handling for methods and fields
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("JavaDoc")
                    public class Bar {
                        /** @removed */
                        public Bar() { }
                        public int field;
                        public void test() { }
                        /** @removed */
                        public int removedField;
                        /** @removed */
                        public void removedMethod() { }
                        /** @removed and @hide - should not be listed */
                        public int hiddenField;

                        /** @removed */
                        public class Inner { }

                        public class Inner2 {
                            public class Inner3 {
                                /** @removed */
                                public class Inner4 { }
                            }
                        }

                        public class Inner5 {
                            public class Inner6 {
                                public class Inner7 {
                                    /** @removed */
                                    public int removed;
                                }
                            }
                        }
                    }
                    """
                )
            ),
            removedApi = """
                package test.pkg {
                  public class Bar {
                    ctor public Bar();
                    method public void removedMethod();
                    field public int removedField;
                  }
                  public class Bar.Inner {
                    ctor public Bar.Inner();
                  }
                  public class Bar.Inner2.Inner3.Inner4 {
                    ctor public Bar.Inner2.Inner3.Inner4();
                  }
                  public class Bar.Inner5.Inner6.Inner7 {
                    field public int removed;
                  }
                }
                """
        )
    }

    @Test
    fun `Check @remove class`() {
        // Test removing classes
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    /** @removed */
                    @SuppressWarnings("JavaDoc")
                    public class Foo {
                        public void foo() { }
                        public class Inner {
                        }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("JavaDoc")
                    public class Bar implements Parcelable {
                        public int field;
                        public void method();

                        /** @removed */
                        public int removedField;
                        /** @removed */
                        public void removedMethod() { }

                        public class Inner1 {
                        }
                        /** @removed */
                        public class Inner2 {
                        }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface Parcelable {
                        void method();
                    }
                    """
                )
            ),
            /*
            I expected this: but doclava1 doesn't do that (and we now match its behavior)
            package test.pkg {
              public class Bar {
                method public void removedMethod();
                field public int removedField;
              }
              public class Bar.Inner2 {
              }
              public class Foo {
                method public void foo();
              }
            }
             */
            removedApi = """
                    package test.pkg {
                      public class Bar implements test.pkg.Parcelable {
                        method public void removedMethod();
                        field public int removedField;
                      }
                      public class Bar.Inner2 {
                        ctor public Bar.Inner2();
                      }
                      public class Foo {
                        ctor public Foo();
                        method public void foo();
                      }
                      public class Foo.Inner {
                        ctor public Foo.Inner();
                      }
                    }
                """
        )
    }

    @Test
    fun `Test include overridden @Deprecated even if annotated with @hide`() {
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("JavaDoc")
                    public class Child extends Parent {
                        /**
                        * @deprecated
                        * @hide
                        */
                        @Deprecated @Override
                        public String toString() {
                            return "Child";
                        }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public class Parent {
                        public String toString() {
                            return "Parent";
                        }
                    }
                    """
                )
            ),
            api = """
                    package test.pkg {
                      public class Child extends test.pkg.Parent {
                        ctor public Child();
                        method public deprecated java.lang.String toString();
                      }
                      public class Parent {
                        ctor public Parent();
                      }
                    }
                    """
        )
    }

    @Test
    fun `Indirect Field Includes from Interfaces`() {
        // Real-world example: include ZipConstants into ZipFile and JarFile
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg1;
                    interface MyConstants {
                        long CONSTANT1 = 12345;
                        long CONSTANT2 = 67890;
                        long CONSTANT3 = 42;
                    }
                    """
                ),
                java(
                    """
                    package test.pkg1;
                    import java.io.Closeable;
                    @SuppressWarnings("WeakerAccess")
                    public class MyParent implements MyConstants, Closeable {
                    }
                    """
                ),
                java(
                    """
                    package test.pkg2;

                    import test.pkg1.MyParent;
                    public class MyChild extends MyParent {
                    }
                    """
                )

            ),
            api = """
                    package test.pkg1 {
                      public class MyParent implements java.io.Closeable {
                        ctor public MyParent();
                        field public static final long CONSTANT1 = 12345L; // 0x3039L
                        field public static final long CONSTANT2 = 67890L; // 0x10932L
                        field public static final long CONSTANT3 = 42L; // 0x2aL
                      }
                    }
                    package test.pkg2 {
                      public class MyChild extends test.pkg1.MyParent {
                        ctor public MyChild();
                        field public static final long CONSTANT1 = 12345L; // 0x3039L
                        field public static final long CONSTANT2 = 67890L; // 0x10932L
                        field public static final long CONSTANT3 = 42L; // 0x2aL
                      }
                    }
                """
        )
    }

    @Test
    fun `Skip interfaces from packages explicitly hidden via arguments`() {
        // Real-world example: HttpResponseCache implements OkCacheContainer but hides the only inherited method
        check(
            checkDoclava1 = true,
            extraArguments = arrayOf(
                "--hide-package", "com.squareup.okhttp"
            ),
            sourceFiles = *arrayOf(
                java(
                    """
                    package android.net.http;
                    import com.squareup.okhttp.Cache;
                    import com.squareup.okhttp.OkCacheContainer;
                    import java.io.Closeable;
                    import java.net.ResponseCache;
                    @SuppressWarnings("JavaDoc")
                    public final class HttpResponseCache implements Closeable, OkCacheContainer {
                        /** @hide Needed for OkHttp integration. */
                        @Override
                        public Cache getCache() {
                            return delegate.getCache();
                        }
                    }
                    """
                ),
                java(
                    """
                    package com.squareup.okhttp;
                    public interface OkCacheContainer {
                      Cache getCache();
                    }
                    """
                )
            ),
            api = """
                package android.net.http {
                  public final class HttpResponseCache implements java.io.Closeable {
                    ctor public HttpResponseCache();
                  }
                }
                """
        )
    }

    @Test
    fun `Extend from multiple interfaces`() {
        // Real-world example: XmlResourceParser
        check(
            checkDoclava1 = true,
            checkCompilation = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package android.content.res;
                    import android.util.AttributeSet;
                    import org.xmlpull.v1.XmlPullParser;

                    @SuppressWarnings("UnnecessaryInterfaceModifier")
                    public interface XmlResourceParser extends XmlPullParser, AttributeSet, AutoCloseable {
                        public void close();
                    }
                    """
                ),
                java(
                    """
                    package android.util;
                    @SuppressWarnings("WeakerAccess")
                    public interface AttributeSet {
                    }
                    """
                ),
                java(
                    """
                    package java.lang;
                    public interface AutoCloseable {
                    }
                    """
                ),
                java(
                    """
                    package org.xmlpull.v1;
                    @SuppressWarnings("WeakerAccess")
                    public interface XmlPullParser {
                    }
                    """
                )
            ),
            api = """
                package android.content.res {
                  public abstract interface XmlResourceParser implements android.util.AttributeSet java.lang.AutoCloseable org.xmlpull.v1.XmlPullParser {
                    method public abstract void close();
                  }
                }
                package android.util {
                  public abstract interface AttributeSet {
                  }
                }
                package java.lang {
                  public abstract interface AutoCloseable {
                  }
                }
                package org.xmlpull.v1 {
                  public abstract interface XmlPullParser {
                  }
                }
                """
        )
    }

    @Test
    fun `Including private interfaces from types`() {
        check(
            checkDoclava1 = true,
            sourceFiles = *arrayOf(
                java("""package test.pkg1; interface Interface1 { }"""),
                java("""package test.pkg1; abstract class Class1 { }"""),
                java("""package test.pkg1; abstract class Class2 { }"""),
                java("""package test.pkg1; abstract class Class3 { }"""),
                java("""package test.pkg1; abstract class Class4 { }"""),
                java("""package test.pkg1; abstract class Class5 { }"""),
                java("""package test.pkg1; abstract class Class6 { }"""),
                java("""package test.pkg1; abstract class Class7 { }"""),
                java("""package test.pkg1; abstract class Class8 { }"""),
                java("""package test.pkg1; abstract class Class9 { }"""),
                java(
                    """
                    package test.pkg1;

                    import java.util.List;
                    import java.util.Map;
                    public abstract class Usage implements List<Class1> {
                       <T extends java.lang.Comparable<? super T>> void sort(java.util.List<T> list) {}
                       public Class3 myClass1 = null;
                       public List<? extends Class4> myClass2 = null;
                       public Map<String, ? extends Class5> myClass3 = null;
                       public <T extends Class6> void mySort(List<Class7> list, T element) {}
                       public void ellipsisType(Class8... myargs);
                       public void arrayType(Class9[] myargs);
                    }
                    """
                )
            ),

            // TODO: Test annotations! (values, annotation classes, etc.)
            warnings = """
                    src/test/pkg1/Usage.java:1: warning: Parameter myargs references hidden type class test.pkg1.Class9. [HiddenTypeParameter:121]
                    src/test/pkg1/Usage.java:2: warning: Parameter myargs references hidden type class test.pkg1.Class8. [HiddenTypeParameter:121]
                    src/test/pkg1/Usage.java:3: warning: Parameter list references hidden type class test.pkg1.Class7. [HiddenTypeParameter:121]
                    src/test/pkg1/Usage.java:6: warning: Field Usage.myClass1 references hidden type test.pkg1.Class3. [HiddenTypeParameter:121]
                    src/test/pkg1/Usage.java:5: warning: Field Usage.myClass2 references hidden type class test.pkg1.Class4. [HiddenTypeParameter:121]
                    src/test/pkg1/Usage.java:4: warning: Field Usage.myClass3 references hidden type class test.pkg1.Class5. [HiddenTypeParameter:121]
                    """,
            api = """
                    package test.pkg1 {
                      public abstract class Usage implements java.util.List {
                        ctor public Usage();
                        method public void arrayType(test.pkg1.Class9[]);
                        method public void ellipsisType(test.pkg1.Class8...);
                        method public <T extends test.pkg1.Class6> void mySort(java.util.List<test.pkg1.Class7>, T);
                        field public test.pkg1.Class3 myClass1;
                        field public java.util.List<? extends test.pkg1.Class4> myClass2;
                        field public java.util.Map<java.lang.String, ? extends test.pkg1.Class5> myClass3;
                      }
                    }
                """
        )
    }
}