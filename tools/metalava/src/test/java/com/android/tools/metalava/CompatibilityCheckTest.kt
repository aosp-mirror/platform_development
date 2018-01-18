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

class
CompatibilityCheckTest : DriverTest() {
    @Test
    fun `Change between class and interface`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:2: error: Class test.pkg.MyTest1 changed class/interface declaration [ChangedClass:23]
                TESTROOT/load-api.txt:4: error: Class test.pkg.MyTest2 changed class/interface declaration [ChangedClass:23]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                  }
                  public interface MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """,
            // MyTest1 and MyTest2 reversed from class to interface or vice versa, MyTest3 and MyTest4 unchanged
            signatureSource = """
                package test.pkg {
                  public interface MyTest1 {
                  }
                  public class MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """
        )
    }

    @Test
    fun `Interfaces should not be dropped`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:2: error: Class test.pkg.MyTest1 changed class/interface declaration [ChangedClass:23]
                TESTROOT/load-api.txt:4: error: Class test.pkg.MyTest2 changed class/interface declaration [ChangedClass:23]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                  }
                  public interface MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """,
            // MyTest1 and MyTest2 reversed from class to interface or vice versa, MyTest3 and MyTest4 unchanged
            signatureSource = """
                package test.pkg {
                  public interface MyTest1 {
                  }
                  public class MyTest2 {
                  }
                  public class MyTest3 {
                  }
                  public interface MyTest4 {
                  }
                }
                """
        )
    }

    @Test
    fun `Ensure warnings for removed APIs`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/previous-api.txt:3: error: Removed method test.pkg.MyTest1.method [RemovedMethod:9]
                TESTROOT/previous-api.txt:4: error: Removed field test.pkg.MyTest1.field [RemovedField:10]
                TESTROOT/previous-api.txt:6: error: Removed class test.pkg.MyTest2 [RemovedClass:8]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class MyTest1 {
                    method public Double method(Float);
                    field public Double field;
                  }
                  public class MyTest2 {
                    method public Double method(Float);
                    field public Double field;
                  }
                }
                package test.pkg.other {
                }
                """,
            signatureSource = """
                package test.pkg {
                  public class MyTest1 {
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest1 {
                  }
                }
                """
        )
    }

    @Test
    fun `Flag invalid nullness changes`() {
        check(
            checkCompatibility = true,
            warnings = """
                TESTROOT/load-api.txt:5: error: Attempted to remove @Nullable annotation from method test.pkg.MyTest.convert3 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:5: error: Attempted to remove @Nullable annotation from parameter arg1 in test.pkg.MyTest.convert3 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:6: error: Attempted to remove @NonNull annotation from method test.pkg.MyTest.convert4 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:6: error: Attempted to remove @NonNull annotation from parameter arg1 in test.pkg.MyTest.convert4 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:7: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter arg1 in test.pkg.MyTest.convert5 [InvalidNullConversion:40]
                TESTROOT/load-api.txt:8: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.MyTest.convert6 [InvalidNullConversion:40]
                """,
            compatibilityMode = false,
            outputKotlinStyleNulls = false,
            previousApi = """
                package test.pkg {
                  public class MyTest {
                    method public Double convert1(Float);
                    method public Double convert2(Float);
                    method @Nullable public Double convert3(@Nullable Float);
                    method @NonNull public Double convert4(@NonNull Float);
                    method @Nullable public Double convert5(@Nullable Float);
                    method @NonNull public Double convert6(@NonNull Float);
                  }
                }
                """,
            // Changes: +nullness, -nullness, nullable->nonnull, nonnull->nullable
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    method @Nullable public Double convert1(@Nullable Float);
                    method @NonNull public Double convert2(@NonNull Float);
                    method public Double convert3(Float);
                    method public Double convert4(Float);
                    method @NonNull public Double convert5(@NonNull Float);
                    method @Nullable public Double convert6(@Nullable Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest {
                    method @Nullable public Double convert1(@Nullable Float);
                    method @NonNull public Double convert2(@NonNull Float);
                    method public Double convert3(Float);
                    method public Double convert4(Float);
                    method @NonNull public Double convert5(@NonNull Float);
                    method @Nullable public Double convert6(@Nullable Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Kotlin Nullness`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/Outer.kt:2: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.Inner.method3 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:3: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.Outer.Inner.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:3: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.Inner.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:5: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.method3 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:6: error: Attempted to change method return from @NonNull to @Nullable: incompatible change for method test.pkg.Outer.method2 [InvalidNullConversion:40]
                src/test/pkg/Outer.kt:6: error: Attempted to change parameter from @Nullable to @NonNull: incompatible change for parameter string in test.pkg.Outer.method2 [InvalidNullConversion:40]
                """,
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            outputKotlinStyleNulls = true,
            previousApi = """
                    package test.pkg {
                      public final class Outer {
                        ctor public Outer();
                        method public final String? method1(String, String?);
                        method public final String method2(String?, String);
                        method public final String? method3(String, String?);
                      }
                      public static final class Outer.Inner {
                        ctor public Outer.Inner();
                        method public final String method2(String?, String);
                        method public final String? method3(String, String?);
                      }
                    }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class Outer {
                        fun method1(string: String, maybeString: String?): String? = null
                        fun method2(string: String, maybeString: String?): String? = null
                        fun method3(maybeString: String?, string : String): String = ""
                        class Inner {
                            fun method2(string: String, maybeString: String?): String? = null
                            fun method3(maybeString: String?, string : String): String = ""
                        }
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class Outer {
                    ctor public Outer();
                    method public final String? method1(String string, String? maybeString);
                    method public final String? method2(String string, String? maybeString);
                    method public final String method3(String? maybeString, String string);
                  }
                  public static final class Outer.Inner {
                    ctor public Outer.Inner();
                    method public final String? method2(String string, String? maybeString);
                    method public final String method3(String? maybeString, String string);
                  }
                }
                """
        )
    }

    @Test
    fun `Java Parameter Name Change`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/JavaClass.java:1: error: Attempted to change parameter name from secondParameter to newName in method test.pkg.JavaClass.method2 [ParameterNameChange:41]
                src/test/pkg/JavaClass.java:2: error: Attempted to remove parameter name from parameter newName in test.pkg.JavaClass.method1 in method test.pkg.JavaClass.method1 [ParameterNameChange:41]
                """,
            compatibilityMode = false,
            previousApi = """
                package test.pkg {
                  public class JavaClass {
                    ctor public JavaClass();
                    method public String method1(String parameterName);
                    method public String method2(String firstParameter, String secondParameter);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    import android.support.annotation.ParameterName;

                    public class JavaClass {
                        public String method1(String newName) { return null; }
                        public String method2(@ParameterName("firstParameter") String s, @ParameterName("newName") String prevName) { return null; }
                    }
                    """
                ),
                supportParameterName
            ),
            api = """
                package test.pkg {
                  public class JavaClass {
                    ctor public JavaClass();
                    method public String! method1(String!);
                    method public String! method2(String! firstParameter, String! newName);
                  }
                }
                """,
            extraArguments = arrayOf("--hide-package", "android.support.annotation")
        )
    }

    @Test
    fun `Kotlin Parameter Name Change`() {
        check(
            checkCompatibility = true,
            warnings = """
                src/test/pkg/KotlinClass.kt:1: error: Attempted to change parameter name from prevName to newName in method test.pkg.KotlinClass.method1 [ParameterNameChange:41]
                """,
            compatibilityMode = false,
            inputKotlinStyleNulls = true,
            outputKotlinStyleNulls = true,
            previousApi = """
                package test.pkg {
                  public final class KotlinClass {
                    ctor public KotlinClass();
                    method public final String? method1(String prevName);
                  }
                }
                """,
            sourceFiles = *arrayOf(
                kotlin(
                    """
                    package test.pkg

                    class KotlinClass {
                        fun method1(newName: String): String? = null
                    }
                    """
                )
            ),
            api = """
                package test.pkg {
                  public final class KotlinClass {
                    ctor public KotlinClass();
                    method public final String? method1(String newName);
                  }
                }
                """
        )
    }

    // TODO: Check method signatures changing incompatibly (look especially out for adding new overloaded
    // methods and comparator getting confused!)
    //   ..equals on the method items should actually be very useful!
}