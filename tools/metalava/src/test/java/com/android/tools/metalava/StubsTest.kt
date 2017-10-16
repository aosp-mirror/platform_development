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

import com.android.tools.lint.checks.infrastructure.TestFile
import org.intellij.lang.annotations.Language
import org.junit.Test

@SuppressWarnings("ALL")
class StubsTest : DriverTest() {
    // TODO: test fields that need initialization
    // TODO: test @DocOnly handling

    private fun checkStubs(
        @Language("JAVA") source: String,
        compatibilityMode: Boolean = true,
        warnings: String? = "",
        checkDoclava1: Boolean = false,
        api: String? = null,
        extraArguments: Array<String> = emptyArray(),
        vararg sourceFiles: TestFile
    ) {
        check(
            *sourceFiles,
            stubs = arrayOf(source),
            compatibilityMode = compatibilityMode,
            warnings = warnings,
            checkDoclava1 = checkDoclava1,
            checkCompilation = true,
            api = api,
            extraArguments = extraArguments
        )
    }

    @Test
    fun `Generate stubs for basic class`() {
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    /** This is the documentation for the class */
                    @SuppressWarnings("ALL")
                    public class Foo {
                        private int hidden;

                        /** My field doc */
                        protected static final String field = "a\nb\n\"test\"";

                        /**
                         * Method documentation.
                         * Maybe it spans
                         * multiple lines.
                         */
                        protected static void onCreate(String parameter1) {
                            // This is not in the stub
                            System.out.println(parameter1);
                        }

                        static {
                           System.out.println("Not included in stub");
                        }
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                /** This is the documentation for the class */
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Foo {
                public Foo() { throw new RuntimeException("Stub!"); }
                /**
                 * Method documentation.
                 * Maybe it spans
                 * multiple lines.
                 */
                protected static void onCreate(java.lang.String parameter1) { throw new RuntimeException("Stub!"); }
                /** My field doc */
                protected static final java.lang.String field = "a\nb\n\"test\"";
                }
                """
        )
    }

    @Test
    fun `Generate stubs for generics`() {
        // Basic interface with generics; makes sure <T extends Object> is written as just <T>
        // Also include some more complex generics expressions to make sure they're serialized
        // correctly (in particular, using fully qualified names instead of what appears in
        // the source code.)
        check(
            checkDoclava1 = false,
            checkCompilation = true,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface MyInterface2<T extends Number>
                            extends MyBaseInterface {
                        class TtsSpan<C extends MyInterface<?>> { }
                        abstract class Range<T extends Comparable<? super T>> { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    @SuppressWarnings("ALL")
                    public interface MyInterface<T extends Object>
                            extends MyBaseInterface {
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface MyBaseInterface {
                    }
                    """
                )
            ),
            warnings = "",
            stubs = arrayOf(
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface MyInterface2<T extends java.lang.Number> extends test.pkg.MyBaseInterface {
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public abstract static class Range<T extends java.lang.Comparable<? super T>> {
                public Range() { throw new RuntimeException("Stub!"); }
                }
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public static class TtsSpan<C extends test.pkg.MyInterface<?>> {
                public TtsSpan() { throw new RuntimeException("Stub!"); }
                }
                }
                """,
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface MyInterface<T> extends test.pkg.MyBaseInterface {
                }
                """,
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface MyBaseInterface {
                }
                """
            )
        )
    }

    @Test
    fun `Generate stubs for class that should not get default constructor (has other constructors)`() {
        // Class without explicit constructors (shouldn't insert default constructor)
        checkStubs(
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
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Foo {
                public Foo(int i) { throw new RuntimeException("Stub!"); }
                public Foo(int i, int j) { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for class that already has a private constructor`() {
        // Class without private constructor; no default constructor should be inserted
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class Foo {
                        private Foo() {
                        }
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Foo {
                Foo() { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for interface class`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public interface Foo {
                        void foo();
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface Foo {
                public void foo();
                }
                """
        )
    }

    @Test
    fun `Generate stubs for enum`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        checkStubs(
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
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public enum Foo {
                A, B;
                }
                """
        )
    }

    @Test
    fun `Generate stubs for annotation type`() {
        // Interface: makes sure the right modifiers etc are shown (and that "package private" methods
        // in the interface are taken to be public etc)
        checkStubs(
            // For unknown reasons, doclava1 behaves differently here than when invoked on the
            // whole platform
            checkDoclava1 = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    import static java.lang.annotation.ElementType.*;
                    import java.lang.annotation.*;
                    @Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
                    @Retention(RetentionPolicy.CLASS)
                    public @interface Foo {
                        String value();
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public @interface Foo {
                public java.lang.String value();
                }
                """
        )
    }

    @Test
    fun `Generate stubs for class with superclass`() {
        // Make sure superclass statement is correct; unlike signature files, inherited method from parent
        // that has same signature should be included in the child
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class Foo extends Super {
                        @Override public void base() { }
                        public void child() { }
                    }
                    """
                ), java(
                    """
                    package test.pkg;
                    public class Super {
                        public void base() { }
                    }
                    """
                )
            ),
            source =
            """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Foo extends test.pkg.Super {
                public Foo() { throw new RuntimeException("Stub!"); }
                public void base() { throw new RuntimeException("Stub!"); }
                public void child() { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for fields with initial values`() {
        checkStubs(
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
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Foo {
                public Foo() { throw new RuntimeException("Stub!"); }
                public static final java.lang.String GOOD_IRI_CHAR = "a-zA-Z0-9\u00a0-\ud7ff\uf900-\ufdcf\ufdf0-\uffef";
                public static final char HEX_INPUT = 61184; // 0xef00 '\uef00'
                protected int field00;
                public static final boolean field01 = true;
                public static final int field02 = 42; // 0x2a
                public static final long field03 = 42L; // 0x2aL
                public static final short field04 = 5; // 0x5
                public static final byte field05 = 5; // 0x5
                public static final char field06 = 99; // 0x0063 'c'
                public static final float field07 = 98.5f;
                public static final double field08 = 98.5;
                public static final java.lang.String field09 = "String with \"escapes\" and \u00a9...";
                public static final double field10 = (0.0/0.0);
                public static final double field11 = (1.0/0.0);
                }
                """
        )
    }

    @Test
    fun `Generate stubs for various modifier scenarios`() {
        // Include as many modifiers as possible to see which ones are included
        // in the signature files, and the expected sorting order.
        // Note that the signature files treat "deprecated" as a fake modifier.
        // Note also how the "protected" modifier on the interface method gets
        // promoted to public.
        checkStubs(
            warnings = null,
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
                            static void method4() { }
                        }
                    }
                    """
                )
            ),

            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public abstract class Foo {
                public Foo() { throw new RuntimeException("Stub!"); }
                @Deprecated public static final synchronized strictfp void method1() { throw new RuntimeException("Stub!"); }
                @Deprecated public static final synchronized native void method2();
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                @Deprecated protected static final class Inner1 {
                protected Inner1() { throw new RuntimeException("Stub!"); }
                }
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                @Deprecated protected abstract static class Inner2 {
                protected Inner2() { throw new RuntimeException("Stub!"); }
                }
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                @Deprecated protected static interface Inner3 {
                public default void method3() { throw new RuntimeException("Stub!"); }
                public static void method4() { throw new RuntimeException("Stub!"); }
                }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for class with abstract enum methods`() {
        // As per https://bugs.openjdk.java.net/browse/JDK-6287639
        // abstract methods in enums should not be listed as abstract,
        // but doclava1 does, so replicate this.
        // Also checks that we handle both enum fields and regular fields
        // and that they are listed separately.

        checkStubs(
            checkDoclava1 = false, // Doclava1 does not generate compileable source for this
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

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
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public enum FooBar {
                ABC, DEF;
                protected void foo() { throw new RuntimeException("Stub!"); }
                public static int field1 = 1; // 0x1
                public int field2 = 2; // 0x2
                }
            """
        )
    }

    @Test
    fun `Check erasure in throws list`() {
        // Makes sure that when we have a generic signature in the throws list we take
        // the erasure instead (in compat mode); "Throwable" instead of "X" in the below
        // test. Real world example: Optional.orElseThrow.
        checkStubs(
            compatibilityMode = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.util.function.Supplier;

                    @SuppressWarnings("RedundantThrows")
                    public final class Test<T> {
                        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
                            return null;
                        }
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public final class Test<T> {
                public Test() { throw new RuntimeException("Stub!"); }
                public <X extends java.lang.Throwable> T orElseThrow(java.util.function.Supplier<? extends X> exceptionSupplier) throws java.lang.Throwable { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for additional generics scenarios`() {
        // Some additional declarations where PSI default type handling diffs from doclava1
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    public abstract class Collections {
                        public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T> collection) {
                            return null;
                        }
                        public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T t);
                        public final class Range<T extends java.lang.Comparable<? super T>> { }
                    }
                    """
                )
            ),

            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public abstract class Collections {
                public Collections() { throw new RuntimeException("Stub!"); }
                public static <T extends java.lang.Object & java.lang.Comparable<? super T>> T max(java.util.Collection<? extends T> collection) { throw new RuntimeException("Stub!"); }
                public abstract <T extends java.util.Collection<java.lang.String>> T addAllTo(T t);
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public final class Range<T extends java.lang.Comparable<? super T>> {
                public Range() { throw new RuntimeException("Stub!"); }
                }
                }
                """
        )
    }

    @Test
    fun `Generate stubs for even more generics scenarios`() {
        // Some additional declarations where PSI default type handling diffs from doclava1
        checkStubs(
            sourceFiles = *arrayOf(
                java(
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

            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MoreAsserts {
                public MoreAsserts() { throw new RuntimeException("Stub!"); }
                public static void assertEquals(java.lang.String arg0, java.util.Set<?> arg1, java.util.Set<?> arg2) { throw new RuntimeException("Stub!"); }
                public static void assertEquals(java.util.Set<?> arg1, java.util.Set<?> arg2) { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Generate stubs enum instance methods`() {
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    public enum ChronUnit implements TempUnit {
                        C(1), B(2), A(3);

                        ChronUnit(int y) {
                        }

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
                ), java(
                    """
                    package test.pkg;

                    public interface TempUnit {
                        @Override
                        String toString();
                    }
                     """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public enum ChronUnit implements test.pkg.TempUnit {
                C, B, A;
                public java.lang.String valueOf(int x) { throw new RuntimeException("Stub!"); }
                public java.lang.String toString() { throw new RuntimeException("Stub!"); }
                }
            """
        )
    }

    @Test
    fun `Generate stubs with superclass filtering`() {
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass extends HiddenParent {
                        public void method4() { }
                    }
                    """
                ), java(
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
                ), java(
                    """
                    package test.pkg;
                    /** @hide */
                    @SuppressWarnings("ALL")
                    public class HiddenParent2 extends PublicParent {
                        public void method2() { }
                    }
                    """
                ), java(
                    """
                    package test.pkg;
                    public class PublicParent {
                        public void method1() { }
                    }
                    """
                )
            ),
            // Notice how the intermediate methods (method2, method3) have been removed
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass extends test.pkg.PublicParent {
                public MyClass() { throw new RuntimeException("Stub!"); }
                public void method4() { throw new RuntimeException("Stub!"); }
                }
                """,
            warnings = "src/test/pkg/MyClass.java:2: warning: Public class test.pkg.MyClass stripped of unavailable superclass test.pkg.HiddenParent [HiddenSuperclass:111]"
        )
    }

    @Test
    fun `Check inheriting from package private class`() {
        checkStubs(
            // Disabled because doclava1 includes fields here that it doesn't include in the
            // signature file; not sure if it's a bug or intentional but it seems suspicious.
            //checkDoclava1 = true,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass extends HiddenParent {
                        public void method1() { }
                    }
                    """
                ), java(
                    """
                    package test.pkg;
                    class HiddenParent {
                        public static final String CONSTANT = "MyConstant";
                        protected int mContext;
                        public void method2() { }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass {
                    public MyClass() { throw new RuntimeException("Stub!"); }
                    public void method1() { throw new RuntimeException("Stub!"); }
                    }
                """
        )
    }

    @Test
    fun `Check implementing a package private interface`() {
        // If you implement a package private interface, we just remove it and inline the members into
        // the subclass

        // BUG: Note that we need to implement the parent
        checkStubs(
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
                ), java(
                    """
                    package test.pkg;
                    public interface OtherInterface {
                        void other();
                    }
                    """
                ), java(
                    """
                    package test.pkg;
                    interface HiddenInterface extends OtherInterface {
                        void method() { }
                        String CONSTANT = "MyConstant";
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass implements test.pkg.OtherInterface {
                public MyClass() { throw new RuntimeException("Stub!"); }
                public void method() { throw new RuntimeException("Stub!"); }
                public void other() { throw new RuntimeException("Stub!"); }
                public static final java.lang.String CONSTANT = "MyConstant";
                }
                """
        )
    }

    @Test
    fun `Check throws list`() {
        // Make sure we format a throws list
        checkStubs(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    import java.io.IOException;

                    @SuppressWarnings("RedundantThrows")
                    public abstract class AbstractCursor {
                        @Override protected void finalize1() throws Throwable { }
                        @Override protected void finalize2() throws IOException, IllegalArgumentException {  }
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public abstract class AbstractCursor {
                public AbstractCursor() { throw new RuntimeException("Stub!"); }
                protected void finalize1() throws java.lang.Throwable { throw new RuntimeException("Stub!"); }
                protected void finalize2() throws java.io.IOException, java.lang.IllegalArgumentException { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Check generating constants in interface without inline-able initializers`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    public interface MyClass {
                        String[] CONSTANT1 = {"MyConstant","MyConstant2"};
                        boolean CONSTANT2 = Boolean.getBoolean(System.getenv("VAR1"));
                        int CONSTANT3 = Integer.parseInt(System.getenv("VAR2"));
                        String CONSTANT4 = null;
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface MyClass {
                public static final java.lang.String[] CONSTANT1 = null;
                public static final boolean CONSTANT2 = false;
                public static final int CONSTANT3 = 0; // 0x0
                public static final java.lang.String CONSTANT4 = null;
                }
                """
        )
    }

    @Test
    fun `Handle non-constant fields in final classes`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("all")
                    public class FinalFieldTest {
                        public interface TemporalField {
                            String getBaseUnit();
                        }
                        public static final class IsoFields {
                            public static final TemporalField DAY_OF_QUARTER = Field.DAY_OF_QUARTER;
                            private IsoFields() {
                                throw new AssertionError("Not instantiable");
                            }

                            private static enum Field implements TemporalField {
                                DAY_OF_QUARTER {
                                    @Override
                                    public String getBaseUnit() {
                                        return "days";
                                    }
                               }
                           };
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class FinalFieldTest {
                    public FinalFieldTest() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static final class IsoFields {
                    IsoFields() { throw new RuntimeException("Stub!"); }
                    public static final test.pkg.FinalFieldTest.TemporalField DAY_OF_QUARTER;
                    static { DAY_OF_QUARTER = null; }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface TemporalField {
                    public java.lang.String getBaseUnit();
                    }
                    }
                    """
        )
    }

    @Test
    fun `Test final instance fields`() {
        // Instance fields in a class must be initialized
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("all")
                    public class InstanceFieldTest {
                        public static final class WindowLayout {
                            public WindowLayout(int width, int height, int gravity) {
                                this.width = width;
                                this.height = height;
                                this.gravity = gravity;
                            }

                            public final int width;
                            public final int height;
                            public final int gravity;

                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class InstanceFieldTest {
                    public InstanceFieldTest() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static final class WindowLayout {
                    public WindowLayout(int width, int height, int gravity) { throw new RuntimeException("Stub!"); }
                    public final int gravity;
                    { gravity = 0; }
                    public final int height;
                    { height = 0; }
                    public final int width;
                    { width = 0; }
                    }
                    }
                    """
        )
    }

    @Test
    fun `Check generating constants in class without inline-able initializers`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass {
                        public static String[] CONSTANT1 = {"MyConstant","MyConstant2"};
                        public static boolean CONSTANT2 = Boolean.getBoolean(System.getenv("VAR1"));
                        public static int CONSTANT3 = Integer.parseInt(System.getenv("VAR2"));
                        public static String CONSTANT4 = null;
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass {
                public MyClass() { throw new RuntimeException("Stub!"); }
                public static java.lang.String[] CONSTANT1;
                public static boolean CONSTANT2;
                public static int CONSTANT3;
                public static java.lang.String CONSTANT4;
                }
                """
        )
    }

    @Test
    fun `Check generating annotation source`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package android.view.View;
                    import android.annotation.IntDef;
                    import android.annotation.IntRange;
                    import java.lang.annotation.Retention;
                    import java.lang.annotation.RetentionPolicy;
                    public class View {
                        @SuppressWarnings("all")
                        public static class MeasureSpec {
                            private static final int MODE_SHIFT = 30;
                            private static final int MODE_MASK  = 0x3 << MODE_SHIFT;
                            /** @hide */
                            @SuppressWarnings("all")
                            @IntDef({UNSPECIFIED, EXACTLY, AT_MOST})
                            @Retention(RetentionPolicy.SOURCE)
                            public @interface MeasureSpecMode {}
                            public static final int UNSPECIFIED = 0 << MODE_SHIFT;
                            public static final int EXACTLY     = 1 << MODE_SHIFT;
                            public static final int AT_MOST     = 2 << MODE_SHIFT;

                            public static int makeMeasureSpec(@IntRange(from = 0, to = (1 << MeasureSpec.MODE_SHIFT) - 1) int size,
                                                              @MeasureSpecMode int mode) {
                                return 0;
                            }
                        }
                    }
                    """
                ),
                intDefAnnotationSource,
                intRangeAnnotationSource
            ),
            warnings = "",
            source = """
                    package android.view.View;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class View {
                    public View() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class MeasureSpec {
                    public MeasureSpec() { throw new RuntimeException("Stub!"); }
                    /**
                     * @param size Value is between 0 and (1 << MeasureSpec.MODE_SHIFT) - 1 inclusive
                     * @param mode Value is {@link android.view.View.View.MeasureSpec#UNSPECIFIED}, {@link android.view.View.View.MeasureSpec#EXACTLY}, or {@link android.view.View.View.MeasureSpec#AT_MOST}
                     */
                    public static int makeMeasureSpec(@android.support.annotation.IntRange(from=0, to=0x40000000 - 1) int size, int mode) { throw new RuntimeException("Stub!"); }
                    public static final int AT_MOST = -2147483648; // 0x80000000
                    public static final int EXACTLY = 1073741824; // 0x40000000
                    public static final int UNSPECIFIED = 0; // 0x0
                    }
                    }
                """
        )
    }

    @Test
    fun `Check generating classes with generics`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    public class Generics {
                        public <T> Generics(int surfaceSize, Class<T> klass) {
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Generics {
                    public <T> Generics(int surfaceSize, java.lang.Class<T> klass) { throw new RuntimeException("Stub!"); }
                    }
                """
        )
    }

    @Test
    fun `Check generating annotation for hidden constants`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    import android.content.Intent;
                    import android.annotation.RequiresPermission;

                    public abstract class HiddenPermission {
                        @RequiresPermission(allOf = {
                                android.Manifest.permission.INTERACT_ACROSS_USERS,
                                android.Manifest.permission.BROADCAST_STICKY
                        })
                        public abstract void removeStickyBroadcast(@RequiresPermission Object intent);
                    }
                    """
                ),
                java(
                    """
                    package android;

                    public final class Manifest {
                        @SuppressWarnings("JavaDoc")
                        public static final class permission {
                            public static final String BROADCAST_STICKY = "android.permission.BROADCAST_STICKY";
                            /** @SystemApi @hide Allows an application to call APIs that allow it to do interactions
                             across the users on the device, using singleton services and
                             user-targeted broadcasts.  This permission is not available to
                             third party applications. */
                            public static final String INTERACT_ACROSS_USERS = "android.permission.INTERACT_ACROSS_USERS";
                        }
                    }
                    """
                ),
                requiresPermissionSource
            ),
            warnings = "src/test/pkg/HiddenPermission.java:5: lint: Permission android.Manifest.permission.INTERACT_ACROSS_USERS required by method test.pkg.HiddenPermission.removeStickyBroadcast(Object) is hidden or removed [MissingPermission:132]",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class HiddenPermission {
                    public HiddenPermission() { throw new RuntimeException("Stub!"); }
                    /**
                     * Requires {@link android.Manifest.permission#INTERACT_ACROSS_USERS} and {@link android.Manifest.permission#BROADCAST_STICKY}
                     */
                    @android.support.annotation.RequiresPermission(allOf={"android.permission.INTERACT_ACROSS_USERS", android.Manifest.permission.BROADCAST_STICKY}) public abstract void removeStickyBroadcast(@android.support.annotation.RequiresPermission java.lang.Object intent);
                    }
                """
        )
    }

    @Test
    fun `Check generating type parameters in interface list`() {
        // In signature files we don't include generics in the interface list.
        // In stubs, we do.
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("NullableProblems")
                    public class GenericsInInterfaces<T> implements Comparable<GenericsInInterfaces> {
                        @Override
                        public int compareTo(GenericsInInterfaces o) {
                            return 0;
                        }

                        void foo(T bar) {
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public class GenericsInInterfaces<T> implements java.lang.Comparable {
                    ctor public GenericsInInterfaces();
                    method public int compareTo(test.pkg.GenericsInInterfaces);
                  }
                }
                """,
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class GenericsInInterfaces<T> implements java.lang.Comparable<test.pkg.GenericsInInterfaces> {
                public GenericsInInterfaces() { throw new RuntimeException("Stub!"); }
                public int compareTo(test.pkg.GenericsInInterfaces o) { throw new RuntimeException("Stub!"); }
                }
                """
        )
    }

    @Test
    fun `Preserve file header comments`() {
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    /*
                    My header 1
                     */

                    /*
                    My header 2
                     */

                    // My third comment

                    package test.pkg;

                    public class HeaderComments {
                    }
                    """
                )
            ),
            source = """
                    /*
                    My header 1
                     */
                    /*
                    My header 2
                     */
                    // My third comment
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class HeaderComments {
                    public HeaderComments() { throw new RuntimeException("Stub!"); }
                    }
                    """
        )
    }

    @Test
    fun `Basic Kotlin class`() {
        checkStubs(
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    /* My file header */
                    // Another comment
                    @file:JvmName("Driver")
                    package test.pkg
                    /** My class doc */
                    class Kotlin(val property1: String = "Default Value", arg2: Int) : Parent() {
                        override fun method() = "Hello World"
                        /** My method doc */
                        fun otherMethod(ok: Boolean, times: Int) {
                        }

                        /** property doc */
                        var property2: String? = null

                        /** @hide */
                        var hiddenProperty: String? = "hidden"

                        private var someField = 42
                        @JvmField
                        var someField2 = 42
                    }

                    open class Parent {
                        open fun method(): String? = null
                        open fun method2(value1: Boolean, value2: Boolean?): String? = null
                        open fun method3(value1: Int?, value2: Int): Int = null
                    }
                    """
                )
            ),
            source = """
                    /* My file header */
                    // Another comment
                    package test.pkg;
                    /** My class doc */
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public final class Kotlin extends test.pkg.Parent {
                    public Kotlin(@android.support.annotation.NonNull java.lang.String property1, int arg2) { throw new RuntimeException("Stub!"); }
                    @android.support.annotation.NonNull public java.lang.String method() { throw new RuntimeException("Stub!"); }
                    /** My method doc */
                    public final void otherMethod(boolean ok, int times) { throw new RuntimeException("Stub!"); }
                    /** property doc */
                    @android.support.annotation.Nullable public final java.lang.String getProperty2() { throw new RuntimeException("Stub!"); }
                    /** property doc */
                    public final void setProperty2(@android.support.annotation.Nullable java.lang.String p) { throw new RuntimeException("Stub!"); }
                    @android.support.annotation.NonNull public final java.lang.String getProperty1() { throw new RuntimeException("Stub!"); }
                    public int someField2;
                    }
                """,
            checkDoclava1 = false /* doesn't support Kotlin... */
        )
    }

    @Test
    fun `Arguments to super constructors`() {
        // When overriding constructors we have to supply arguments
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("WeakerAccess")
                    public class Constructors {
                        public class Parent {
                            public Parent(String s, int i, long l, boolean b, short sh) {
                            }
                        }

                        public class Child extends Parent {
                            public Child(String s, int i, long l, boolean b, short sh) {
                                super(s, i, l, b, sh);
                            }

                            private Child(String s) {
                                super(s, i, l, b, sh);
                            }
                        }

                        public class Child2 extends Parent {
                            Child2(String s) {
                                super(s, i, l, b, sh);
                            }
                        }

                        public class Child3 extends Child2 {
                            private Child3(String s) {
                                super("something");
                            }
                        }
                    }
                    """
                )
            ),
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Constructors {
                    public Constructors() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Child extends test.pkg.Constructors.Parent {
                    public Child(java.lang.String s, int i, long l, boolean b, short sh) { super(null, 0, 0, false, (short)0); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Child2 extends test.pkg.Constructors.Parent {
                    Child2() { super(null, 0, 0, false, (short)0); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Child3 extends test.pkg.Constructors.Child2 {
                    Child3() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Parent {
                    public Parent(java.lang.String s, int i, long l, boolean b, short sh) { throw new RuntimeException("Stub!"); }
                    }
                    }
                    """
        )
    }

    // TODO: Add test to see what happens if I have Child4 in a different package which can't access the package private constructor of child3?

    @Test
    fun `DocOnly members should be omitted`() {
        // When marked @doconly don't include in stubs or signature files
        // unless specifically asked for (which we do when generating docs).
        checkStubs(
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("JavaDoc")
                    public class Outer {
                        /** @doconly Some docs here */
                        public class MyClass1 {
                            public int myField;
                        }

                        public class MyClass2 {
                            /** @doconly Some docs here */
                            public int myField;

                            /** @doconly Some docs here */
                            public int myMethod() { return 0; }
                        }
                    }
                    """
                )
            ),
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class Outer {
                public Outer() { throw new RuntimeException("Stub!"); }
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass2 {
                public MyClass2() { throw new RuntimeException("Stub!"); }
                }
                }
                    """,
            api = """
                package test.pkg {
                  public class Outer {
                    ctor public Outer();
                  }
                  public class Outer.MyClass2 {
                    ctor public Outer.MyClass2();
                  }
                }
                """
        )
    }

    @Test
    fun `DocOnly members should be included when requested`() {
        // When marked @doconly don't include in stubs or signature files
        // unless specifically asked for (which we do when generating docs).
        checkStubs(
            extraArguments = arrayOf("--include-doconly"),
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("JavaDoc")
                    public class Outer {
                        /** @doconly Some docs here */
                        public class MyClass1 {
                            public int myField;
                        }

                        public class MyClass2 {
                            /** @doconly Some docs here */
                            public int myField;

                            /** @doconly Some docs here */
                            public int myMethod() { return 0; }
                        }
                    }
                    """
                )
            ),
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Outer {
                    public Outer() { throw new RuntimeException("Stub!"); }
                    /** @doconly Some docs here */
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass1 {
                    public MyClass1() { throw new RuntimeException("Stub!"); }
                    public int myField;
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass2 {
                    public MyClass2() { throw new RuntimeException("Stub!"); }
                    /** @doconly Some docs here */
                    public int myMethod() { throw new RuntimeException("Stub!"); }
                    /** @doconly Some docs here */
                    public int myField;
                    }
                    }
                    """
        )
    }

    @Test
    fun `Check generating required stubs from hidden super classes and interfaces`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass extends HiddenSuperClass implements HiddenInterface, PublicInterface2 {
                        public void myMethod() { }
                        @Override public void publicInterfaceMethod2() { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    class HiddenSuperClass extends PublicSuperParent {
                        @Override public void inheritedMethod2() { }
                        @Override public void publicInterfaceMethod() { }
                        @Override public void publicMethod() {}
                        @Override public void publicMethod2() {}
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public abstract class PublicSuperParent {
                        public void inheritedMethod1() {}
                        public void inheritedMethod2() {}
                        public abstract void publicMethod() {}
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    interface HiddenInterface extends PublicInterface {
                        int MY_CONSTANT = 5;
                        void hiddenInterfaceMethod();
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface PublicInterface {
                        void publicInterfaceMethod();
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public interface PublicInterface2 {
                        void publicInterfaceMethod2();
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class MyClass extends test.pkg.PublicSuperParent implements test.pkg.PublicInterface test.pkg.PublicInterface2 {
                        ctor public MyClass();
                        method public void myMethod();
                        method public void publicInterfaceMethod2();
                        field public static final int MY_CONSTANT = 5; // 0x5
                      }
                      public abstract interface PublicInterface {
                        method public abstract void publicInterfaceMethod();
                      }
                      public abstract interface PublicInterface2 {
                        method public abstract void publicInterfaceMethod2();
                      }
                      public abstract class PublicSuperParent {
                        ctor public PublicSuperParent();
                        method public void inheritedMethod1();
                        method public void inheritedMethod2();
                        method public abstract void publicMethod();
                      }
                    }
                """,
            source = """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass extends test.pkg.PublicSuperParent implements test.pkg.PublicInterface, test.pkg.PublicInterface2 {
                public MyClass() { throw new RuntimeException("Stub!"); }
                public void myMethod() { throw new RuntimeException("Stub!"); }
                public void publicInterfaceMethod2() { throw new RuntimeException("Stub!"); }
                // Inlined stub from hidden parent class test.pkg.HiddenSuperClass
                public void publicMethod() { throw new RuntimeException("Stub!"); }
                // Inlined stub from hidden parent class test.pkg.HiddenSuperClass
                public void publicInterfaceMethod() { throw new RuntimeException("Stub!"); }
                public static final int MY_CONSTANT = 5; // 0x5
                }
                """
        )
    }

    @Test
    fun `Test inaccessible constructors`() {
        // If the constructors of a class are not visible, and the class has subclasses,
        // those subclass stubs will need to reference these inaccessible constructors.
        // This generally only happens when the constructors are package private (and
        // therefore hidden) but the subclass using it is also in the same package.

        check(
            checkDoclava1 = false,
            checkCompilation = true,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class MyClass1 {
                        MyClass1(int myVar) { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    import java.io.IOException;
                    @SuppressWarnings("RedundantThrows")
                    public class MySubClass1 extends MyClass1 {
                        MyClass1(int myVar) throws IOException { super(myVar); }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public class MyClass2 {
                        /** @hide */
                        public MyClass2(int myVar) { }
                    }
                    """
                ),
                java(
                    """
                    package test.pkg;
                    public class MySubClass2 extends MyClass2 {
                        public MyClass2() { super(5); }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class MyClass1 {
                      }
                      public class MyClass2 {
                      }
                      public class MySubClass1 extends test.pkg.MyClass1 {
                      }
                      public class MySubClass2 extends test.pkg.MyClass2 {
                        ctor public MySubClass2();
                      }
                    }
                    """,
            stubs = arrayOf(
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass1 {
                MyClass1() { throw new RuntimeException("Stub!"); }
                }
                """,
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MySubClass1 extends test.pkg.MyClass1 {
                MySubClass1() { throw new RuntimeException("Stub!"); }
                }
                """,
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MyClass2 {
                MyClass2() { throw new RuntimeException("Stub!"); }
                }
                """,
                """
                package test.pkg;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public class MySubClass2 extends test.pkg.MyClass2 {
                public MySubClass2() { throw new RuntimeException("Stub!"); }
                }
                """
            )
        )
    }

    @Test
    fun `Generics Variable Rewriting`() {
        // When we move methods from hidden superclasses into the subclass since they
        // provide the implementation for a required method, it's possible that the
        // method we copied in is referencing generics with a different variable than
        // in the current class, so we need to handle this

        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                // TODO: Try using prefixes like "A", and "AA" to make sure my generics
                // variable renaming doesn't do something really dumb
                java(
                    """
                    package test.pkg;

                    import java.util.List;
                    import java.util.Map;

                    public class Generics {
                        public class MyClass<X extends Number,Y> extends HiddenParent<X,Y> implements PublicParent<X,Y> {
                        }

                        public class MyClass2<W> extends HiddenParent<Float,W> implements PublicParent<Float, W> {
                        }

                        public class MyClass3 extends HiddenParent<Float,Double> implements PublicParent<Float,Double> {
                        }

                        class HiddenParent<M, N> extends HiddenParent2<M, N>  {
                        }

                        class HiddenParent2<T, TT>  {
                            public Map<T,Map<TT, String>> createMap(List<T> list) {
                                return null;
                            }
                        }

                        public interface PublicParent<A extends Number,B> {
                            Map<A,Map<B, String>> createMap(List<A> list);
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Generics {
                    public Generics() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass<X extends java.lang.Number, Y> implements test.pkg.Generics.PublicParent<X,Y> {
                    public MyClass() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.Generics.HiddenParent2
                    public java.util.Map<X,java.util.Map<Y,java.lang.String>> createMap(java.util.List<X> list) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass2<W> implements test.pkg.Generics.PublicParent<java.lang.Float,W> {
                    public MyClass2() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.Generics.HiddenParent2
                    public java.util.Map<java.lang.Float,java.util.Map<W,java.lang.String>> createMap(java.util.List<java.lang.Float> list) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass3 implements test.pkg.Generics.PublicParent<java.lang.Float,java.lang.Double> {
                    public MyClass3() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.Generics.HiddenParent2
                    public java.util.Map<java.lang.Float,java.util.Map<java.lang.Double,java.lang.String>> createMap(java.util.List<java.lang.Float> list) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface PublicParent<A extends java.lang.Number, B> {
                    public java.util.Map<A,java.util.Map<B,java.lang.String>> createMap(java.util.List<A> list);
                    }
                    }
                    """
        )
    }

    @Test
    fun `Rewriting type parameters in interfaces from hidden super classes and in throws lists`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.io.IOException;
                    import java.util.List;
                    import java.util.Map;

                    @SuppressWarnings({"RedundantThrows", "WeakerAccess"})
                    public class Generics {
                        public class MyClass<X, Y extends Number> extends HiddenParent<X, Y> implements PublicInterface<X, Y> {
                        }

                        class HiddenParent<M, N extends Number> extends PublicParent<M, N> {
                            public Map<M, Map<N, String>> createMap(List<M> list) throws MyThrowable {
                                return null;
                            }

                            protected List<M> foo() {
                                return null;
                            }

                        }

                        class MyThrowable extends IOException {
                        }

                        public abstract class PublicParent<A, B extends Number> {
                            protected abstract List<A> foo();
                        }

                        public interface PublicInterface<A, B> {
                            Map<A, Map<B, String>> createMap(List<A> list) throws IOException;
                        }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class Generics {
                        ctor public Generics();
                      }
                      public class Generics.MyClass<X, Y extends java.lang.Number> extends test.pkg.Generics.PublicParent implements test.pkg.Generics.PublicInterface {
                        ctor public Generics.MyClass();
                      }
                      public static abstract interface Generics.PublicInterface<A, B> {
                        method public abstract java.util.Map<A, java.util.Map<B, java.lang.String>> createMap(java.util.List<A>) throws java.io.IOException;
                      }
                      public abstract class Generics.PublicParent<A, B extends java.lang.Number> {
                        ctor public Generics.PublicParent();
                        method protected abstract java.util.List<A> foo();
                      }
                    }
                    """,
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Generics {
                    public Generics() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class MyClass<X, Y extends java.lang.Number> extends test.pkg.Generics.PublicParent<X, Y> implements test.pkg.Generics.PublicInterface<X,Y> {
                    public MyClass() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.Generics.HiddenParent
                    public java.util.List<X> foo() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.Generics.HiddenParent
                    public java.util.Map<X,java.util.Map<Y,java.lang.String>> createMap(java.util.List<X> list) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface PublicInterface<A, B> {
                    public java.util.Map<A, java.util.Map<B, java.lang.String>> createMap(java.util.List<A> list) throws java.io.IOException;
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class PublicParent<A, B extends java.lang.Number> {
                    public PublicParent() { throw new RuntimeException("Stub!"); }
                    protected abstract java.util.List<A> foo();
                    }
                    }
                    """
        )
    }

    @Test
    fun `Rewriting implements class references`() {
        // Checks some more subtle bugs around generics type variable renaming
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.util.Collection;
                    import java.util.Set;

                    @SuppressWarnings("all")
                    public class ConcurrentHashMap<K, V> {
                        public abstract static class KeySetView<K, V> extends CollectionView<K, V, K>
                                implements Set<K>, java.io.Serializable {
                        }

                        abstract static class CollectionView<K, V, E>
                                implements Collection<E>, java.io.Serializable {
                            public final Object[] toArray() { return null; }

                            public final <T> T[] toArray(T[] a) {
                                return null;
                            }

                            @Override
                            public int size() {
                                return 0;
                            }
                        }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class ConcurrentHashMap<K, V> {
                        ctor public ConcurrentHashMap();
                      }
                      public static abstract class ConcurrentHashMap.KeySetView<K, V> implements java.util.Collection java.io.Serializable java.util.Set {
                        ctor public ConcurrentHashMap.KeySetView();
                      }
                    }
                    """,
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class ConcurrentHashMap<K, V> {
                    public ConcurrentHashMap() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class KeySetView<K, V> implements java.util.Collection<K>, java.io.Serializable, java.util.Set<K> {
                    public KeySetView() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.ConcurrentHashMap.CollectionView
                    public int size() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.ConcurrentHashMap.CollectionView
                    public final java.lang.Object[] toArray() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.ConcurrentHashMap.CollectionView
                    public final <T> T[] toArray(T[] a) { throw new RuntimeException("Stub!"); }
                    }
                    }
                    """
        )
    }

    @Test
    fun `Arrays in type arguments`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    public class Generics2 {
                        public class FloatArrayEvaluator implements TypeEvaluator<float[]> {
                        }

                        @SuppressWarnings("WeakerAccess")
                        public interface TypeEvaluator<T> {
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Generics2 {
                    public Generics2() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class FloatArrayEvaluator implements test.pkg.Generics2.TypeEvaluator<float[]> {
                    public FloatArrayEvaluator() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface TypeEvaluator<T> {
                    }
                    }
                    """
        )
    }

    @Test
    fun `Interface extending multiple interfaces`() {
        // Ensure that we handle sorting correctly where we're mixing super classes and implementing
        // interfaces
        // Real-world example: XmlResourceParser
        check(
            checkDoclava1 = false,
            checkCompilation = true,
            sourceFiles = *arrayOf(
                java(
                    """
                    package android.content.res;
                    import android.util.AttributeSet;
                    import org.xmlpull.v1.XmlPullParser;

                    public interface XmlResourceParser extends XmlPullParser, AttributeSet, AutoCloseable {
                        public void close();
                    }
                    """
                ),
                java(
                    """
                    package android.util;
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
                    public interface XmlPullParser {
                    }
                    """
                )
            ),
            stubs = arrayOf(
                """
                package android.content.res;
                @SuppressWarnings({"unchecked", "deprecation", "all"})
                public interface XmlResourceParser extends org.xmlpull.v1.XmlPullParser,  android.util.AttributeSet, java.lang.AutoCloseable {
                public void close();
                }
                """
            )
        )
    }

    // TODO: Add a protected constructor too to make sure my code to make non-public constructors package private
    // don't accidentally demote protected constructors to package private!

    @Test
    fun `Picking Super Constructors`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.io.IOException;

                    @SuppressWarnings({"RedundantThrows", "JavaDoc", "WeakerAccess"})
                    public class PickConstructors {
                        public abstract static class FileInputStream extends InputStream {

                            public FileInputStream(String name) throws FileNotFoundException {
                            }

                            public FileInputStream(File file) throws FileNotFoundException {
                            }

                            public FileInputStream(FileDescriptor fdObj) {
                                this(fdObj, false /* isFdOwner */);
                            }

                            /**
                             * @hide
                             */
                            public FileInputStream(FileDescriptor fdObj, boolean isFdOwner) {
                            }
                        }

                        public abstract static class AutoCloseInputStream extends FileInputStream {
                            public AutoCloseInputStream(ParcelFileDescriptor pfd) {
                                super(pfd.getFileDescriptor());
                            }
                        }

                        abstract static class HiddenParentStream extends FileInputStream {
                            public HiddenParentStream(FileDescriptor pfd) {
                                super(pfd);
                            }
                        }

                        public abstract static class AutoCloseInputStream2 extends HiddenParentStream {
                            public AutoCloseInputStream2(ParcelFileDescriptor pfd) {
                                super(pfd.getFileDescriptor());
                            }
                        }

                        public abstract class ParcelFileDescriptor implements Closeable {
                            public abstract FileDescriptor getFileDescriptor();
                        }

                        public static interface Closeable extends AutoCloseable {
                        }

                        public static interface AutoCloseable {
                        }

                        public static abstract class InputStream implements Closeable {
                        }

                        public static class File {
                        }

                        public static final class FileDescriptor {
                        }

                        public static class FileNotFoundException extends IOException {
                        }

                        public static class IOException extends Exception {
                        }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class PickConstructors {
                        ctor public PickConstructors();
                      }
                      public static abstract class PickConstructors.AutoCloseInputStream extends test.pkg.PickConstructors.FileInputStream {
                        ctor public PickConstructors.AutoCloseInputStream(test.pkg.PickConstructors.ParcelFileDescriptor);
                      }
                      public static abstract class PickConstructors.AutoCloseInputStream2 extends test.pkg.PickConstructors.FileInputStream {
                        ctor public PickConstructors.AutoCloseInputStream2(test.pkg.PickConstructors.ParcelFileDescriptor);
                      }
                      public static abstract interface PickConstructors.AutoCloseable {
                      }
                      public static abstract interface PickConstructors.Closeable implements test.pkg.PickConstructors.AutoCloseable {
                      }
                      public static class PickConstructors.File {
                        ctor public PickConstructors.File();
                      }
                      public static final class PickConstructors.FileDescriptor {
                        ctor public PickConstructors.FileDescriptor();
                      }
                      public static abstract class PickConstructors.FileInputStream extends test.pkg.PickConstructors.InputStream {
                        ctor public PickConstructors.FileInputStream(java.lang.String) throws test.pkg.PickConstructors.FileNotFoundException;
                        ctor public PickConstructors.FileInputStream(test.pkg.PickConstructors.File) throws test.pkg.PickConstructors.FileNotFoundException;
                        ctor public PickConstructors.FileInputStream(test.pkg.PickConstructors.FileDescriptor);
                      }
                      public static class PickConstructors.FileNotFoundException extends test.pkg.PickConstructors.IOException implements java.io.Serializable {
                        ctor public PickConstructors.FileNotFoundException();
                      }
                      public static class PickConstructors.IOException extends java.lang.Exception implements java.io.Serializable {
                        ctor public PickConstructors.IOException();
                      }
                      public static abstract class PickConstructors.InputStream implements test.pkg.PickConstructors.Closeable {
                        ctor public PickConstructors.InputStream();
                      }
                      public abstract class PickConstructors.ParcelFileDescriptor implements test.pkg.PickConstructors.Closeable {
                        ctor public PickConstructors.ParcelFileDescriptor();
                        method public abstract test.pkg.PickConstructors.FileDescriptor getFileDescriptor();
                      }
                    }
                """,
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class PickConstructors {
                    public PickConstructors() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class AutoCloseInputStream extends test.pkg.PickConstructors.FileInputStream {
                    public AutoCloseInputStream(test.pkg.PickConstructors.ParcelFileDescriptor pfd) { super((test.pkg.PickConstructors.FileDescriptor)null); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class AutoCloseInputStream2 extends test.pkg.PickConstructors.FileInputStream {
                    public AutoCloseInputStream2(test.pkg.PickConstructors.ParcelFileDescriptor pfd) { super((test.pkg.PickConstructors.FileDescriptor)null); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface AutoCloseable {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface Closeable extends test.pkg.PickConstructors.AutoCloseable {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class File {
                    public File() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static final class FileDescriptor {
                    public FileDescriptor() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class FileInputStream extends test.pkg.PickConstructors.InputStream {
                    public FileInputStream(java.lang.String name) throws test.pkg.PickConstructors.FileNotFoundException { throw new RuntimeException("Stub!"); }
                    public FileInputStream(test.pkg.PickConstructors.File file) throws test.pkg.PickConstructors.FileNotFoundException { throw new RuntimeException("Stub!"); }
                    public FileInputStream(test.pkg.PickConstructors.FileDescriptor fdObj) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class FileNotFoundException extends test.pkg.PickConstructors.IOException implements java.io.Serializable {
                    public FileNotFoundException() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class IOException extends java.lang.Exception implements java.io.Serializable {
                    public IOException() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class InputStream implements test.pkg.PickConstructors.Closeable {
                    public InputStream() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class ParcelFileDescriptor implements test.pkg.PickConstructors.Closeable {
                    public ParcelFileDescriptor() { throw new RuntimeException("Stub!"); }
                    public abstract test.pkg.PickConstructors.FileDescriptor getFileDescriptor();
                    }
                    }
                    """
        )
    }

    @Test
    fun `Picking This Constructors`() {
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings({"WeakerAccess", "unused"})
                    public class Constructors2 {
                        public class TestSuite implements Test {

                            public TestSuite() {
                            }

                            public TestSuite(final Class<?> theClass) {
                            }

                            public TestSuite(Class<? extends TestCase> theClass, String name) {
                                this(theClass);
                            }

                            public TestSuite(String name) {
                            }
                            public TestSuite(Class<?>... classes) {
                            }

                            public TestSuite(Class<? extends TestCase>[] classes, String name) {
                                this(classes);
                            }
                        }

                        public class TestCase {
                        }

                        public interface Test {
                        }

                        public class Parent {
                            public Parent(int x) throws IOException {
                            }
                        }

                        class Intermediate extends Parent {
                            Intermediate(int x) throws IOException { super(x); }
                        }

                        public class Child extends Intermediate {
                            public Child() throws IOException { super(5); }
                            public Child(float x) throws IOException { this(); }
                        }

                        // ----------------------------------------------------

                        public abstract class DrawableWrapper {
                            public DrawableWrapper(Drawable dr) {
                            }

                            DrawableWrapper(Clipstate state, Object resources) {
                            }
                        }


                        public class ClipDrawable extends DrawableWrapper {
                            ClipDrawable() {
                                this(null);
                            }

                            public ClipDrawable(Drawable drawable, int gravity, int orientation) { this(null); }

                            private ClipDrawable(Clipstate clipstate) {
                                super(clipstate, null);
                            }
                        }

                        public class Drawable {
                        }

                        class Clipstate {
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Constructors2 {
                    public Constructors2() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Child extends test.pkg.Constructors2.Parent {
                    public Child() { super(0); throw new RuntimeException("Stub!"); }
                    public Child(float x) { this(); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class ClipDrawable extends test.pkg.Constructors2.DrawableWrapper {
                    public ClipDrawable(test.pkg.Constructors2.Drawable drawable, int gravity, int orientation) { super(null); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Drawable {
                    public Drawable() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class DrawableWrapper {
                    public DrawableWrapper(test.pkg.Constructors2.Drawable dr) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Parent {
                    public Parent(int x) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface Test {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class TestCase {
                    public TestCase() { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class TestSuite implements test.pkg.Constructors2.Test {
                    public TestSuite() { throw new RuntimeException("Stub!"); }
                    public TestSuite(java.lang.Class<?> theClass) { throw new RuntimeException("Stub!"); }
                    public TestSuite(java.lang.Class<? extends test.pkg.Constructors2.TestCase> theClass, java.lang.String name) { throw new RuntimeException("Stub!"); }
                    public TestSuite(java.lang.String name) { throw new RuntimeException("Stub!"); }
                    public TestSuite(java.lang.Class<?>... classes) { throw new RuntimeException("Stub!"); }
                    public TestSuite(java.lang.Class<? extends test.pkg.Constructors2.TestCase>[] classes, java.lang.String name) { throw new RuntimeException("Stub!"); }
                    }
                    }
                    """
        )
    }

    @Test
    fun `Another Constructor Test`() {
        // A specific scenario triggered in the API where the right super class detector was not chosen
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings({"RedundantThrows", "JavaDoc", "WeakerAccess"})
                    public class PickConstructors2 {
                        public interface EventListener {
                        }

                        public interface PropertyChangeListener extends EventListener {
                        }

                        public static abstract class EventListenerProxy<T extends EventListener>
                                implements EventListener {
                            public EventListenerProxy(T listener) {
                            }
                        }

                        public static class PropertyChangeListenerProxy
                                extends EventListenerProxy<PropertyChangeListener>
                                implements PropertyChangeListener {
                            public PropertyChangeListenerProxy(String propertyName, PropertyChangeListener listener) {
                                super(listener);
                            }
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class PickConstructors2 {
                    public PickConstructors2() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface EventListener {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class EventListenerProxy<T extends test.pkg.PickConstructors2.EventListener> implements test.pkg.PickConstructors2.EventListener {
                    public EventListenerProxy(T listener) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface PropertyChangeListener extends test.pkg.PickConstructors2.EventListener {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class PropertyChangeListenerProxy extends test.pkg.PickConstructors2.EventListenerProxy<test.pkg.PickConstructors2.PropertyChangeListener> implements test.pkg.PickConstructors2.PropertyChangeListener {
                    public PropertyChangeListenerProxy(java.lang.String propertyName, test.pkg.PickConstructors2.PropertyChangeListener listener) { super(null); throw new RuntimeException("Stub!"); }
                    }
                    }
                    """
        )
    }

    @Test
    fun `Overriding protected methods`() {
        // Checks a scenario where the stubs were missing overrides
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("all")
                    public class Layouts {
                        public static class View {
                            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                            }
                        }

                        public static abstract class ViewGroup extends View {
                            @Override
                            protected abstract void onLayout(boolean changed,
                                    int l, int t, int r, int b);
                        }

                        public static class Toolbar extends ViewGroup {
                            @Override
                            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                            }
                        }
                    }
                    """
                )
            ),
            warnings = "",
            api = """
                    package test.pkg {
                      public class Layouts {
                        ctor public Layouts();
                      }
                      public static class Layouts.Toolbar extends test.pkg.Layouts.ViewGroup {
                        ctor public Layouts.Toolbar();
                      }
                      public static class Layouts.View {
                        ctor public Layouts.View();
                        method protected void onLayout(boolean, int, int, int, int);
                      }
                      public static abstract class Layouts.ViewGroup extends test.pkg.Layouts.View {
                        ctor public Layouts.ViewGroup();
                        method protected abstract void onLayout(boolean, int, int, int, int);
                      }
                    }
                    """,
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Layouts {
                    public Layouts() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class Toolbar extends test.pkg.Layouts.ViewGroup {
                    public Toolbar() { throw new RuntimeException("Stub!"); }
                    protected void onLayout(boolean changed, int l, int t, int r, int b) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static class View {
                    public View() { throw new RuntimeException("Stub!"); }
                    protected void onLayout(boolean changed, int left, int top, int right, int bottom) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract static class ViewGroup extends test.pkg.Layouts.View {
                    public ViewGroup() { throw new RuntimeException("Stub!"); }
                    protected abstract void onLayout(boolean changed, int l, int t, int r, int b);
                    }
                    }
                    """
        )
    }

    @Test
    fun `Missing overridden method`() {
        // Another special case where overridden methods were missing
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    import java.util.Collection;
                    import java.util.Set;

                    @SuppressWarnings("all")
                    public class SpanTest {
                        public interface CharSequence {
                        }
                        public interface Spanned extends CharSequence {
                            public int nextSpanTransition(int start, int limit, Class type);
                        }

                        public interface Spannable extends Spanned {
                        }

                        public class SpannableString extends SpannableStringInternal implements CharSequence, Spannable {
                        }

                        /* package */ abstract class SpannableStringInternal {
                            public int nextSpanTransition(int start, int limit, Class kind) {
                                return 0;
                            }
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class SpanTest {
                    public SpanTest() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface CharSequence {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface Spannable extends test.pkg.SpanTest.Spanned {
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class SpannableString implements test.pkg.SpanTest.CharSequence, test.pkg.SpanTest.Spannable {
                    public SpannableString() { throw new RuntimeException("Stub!"); }
                    // Inlined stub from hidden parent class test.pkg.SpanTest.SpannableStringInternal
                    public int nextSpanTransition(int start, int limit, java.lang.Class kind) { throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public static interface Spanned extends test.pkg.SpanTest.CharSequence {
                    public int nextSpanTransition(int start, int limit, java.lang.Class type);
                    }
                    }
                    """
        )
    }

    @Test
    fun `Skip type variables in casts`() {
        // When generating casts in super constructor calls, use raw types
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg;

                    @SuppressWarnings("all")
                    public class Properties {
                        public abstract class Property<T, V> {
                            public Property(Class<V> type, String name) {
                            }
                            public Property(Class<V> type, String name, String name2) { // force casts in super
                            }
                        }

                        public abstract class IntProperty<T> extends Property<T, Integer> {

                            public IntProperty(String name) {
                                super(Integer.class, name);
                            }
                        }
                    }
                    """
                )
            ),
            warnings = "",
            source = """
                    package test.pkg;
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public class Properties {
                    public Properties() { throw new RuntimeException("Stub!"); }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class IntProperty<T> extends test.pkg.Properties.Property<T,java.lang.Integer> {
                    public IntProperty(java.lang.String name) { super((java.lang.Class)null, (java.lang.String)null); throw new RuntimeException("Stub!"); }
                    }
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    public abstract class Property<T, V> {
                    public Property(java.lang.Class<V> type, java.lang.String name) { throw new RuntimeException("Stub!"); }
                    public Property(java.lang.Class<V> type, java.lang.String name, java.lang.String name2) { throw new RuntimeException("Stub!"); }
                    }
                    }
                    """
        )
    }

    @Test
    fun `Rewrite relative documentation links`() {
        // When generating casts in super constructor calls, use raw types
        checkStubs(
            checkDoclava1 = false,
            sourceFiles =
            *arrayOf(
                java(
                    """
                    package test.pkg1;
                    import java.io.IOException;
                    import test.pkg2.OtherClass;

                    /**
                     *  Blah blah {@link OtherClass} blah blah.
                     *  Referencing <b>field</b> {@link OtherClass#foo},
                     *  and referencing method {@link OtherClass#bar(int,
                     *   boolean)}.
                     *  And relative reference {@link #baz()}.
                     *  Here's an already fully qualified reference: {@link test.pkg2.OtherClass}.
                     *  And here's one in the same package: {@link LocalClass}.
                     *
                     *  @deprecated For some reason
                     *  @see OtherClass
                     *  @see OtherClass#bar(int, boolean)
                     */
                    @SuppressWarnings("all")
                    public class SomeClass {
                       /**
                       * My method.
                       * @throws IOException when blah blah blah
                       */
                       public void baz() throws IOException;
                    }
                    """
                ),
                java(
                    """
                    package test.pkg2;

                    @SuppressWarnings("all")
                    public class OtherClass {
                        public int foo;
                        public void bar(int baz, boolean bar);
                    }
                    """
                ),
                java(
                    """
                    package test.pkg1;

                    @SuppressWarnings("all")
                    public class LocalClass {
                    }
                    """
        )
            ),
            warnings = "",
            source = """
                    package test.pkg1;
                    /**
                     *  Blah blah {@link test.pkg2.OtherClass OtherClass} blah blah.
                     *  Referencing <b>field</b> {@link test.pkg2.OtherClass#foo OtherClass#foo},
                     *  and referencing method {@link test.pkg2.OtherClass#bar(int,
                     *   boolean) OtherClass#bar(int,
                     *   boolean)}.
                     *  And relative reference {@link #baz()}.
                     *  Here's an already fully qualified reference: {@link test.pkg2.OtherClass}.
                     *  And here's one in the same package: {@link LocalClass}.
                     *
                     *  @deprecated For some reason
                     *  @see test.pkg2.OtherClass
                     *  @see OtherClass#bar(int, boolean)
                     */
                    @SuppressWarnings({"unchecked", "deprecation", "all"})
                    @Deprecated public class SomeClass {
                    public SomeClass() { throw new RuntimeException("Stub!"); }
                    /**
                     * My method.
                     * @throws java.io.IOException when blah blah blah
                     */
                    public void baz() throws java.io.IOException { throw new RuntimeException("Stub!"); }
                    }
                    """
        )
    }

    // TODO: Add in some type variables in method signatures and constructors!
    // TODO: Test what happens when a class extends a hidden extends a public in separate packages,
    // and the hidden has a @hide constructor so the stub in the leaf class doesn't compile -- I should
    // check for this and fail build.
}