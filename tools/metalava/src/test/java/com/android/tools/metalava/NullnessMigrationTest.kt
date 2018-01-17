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

class NullnessMigrationTest : DriverTest() {
    @Test
    fun `Test Kotlin-style null signatures`() {
        check(
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method @Nullable public Double convert1(@NonNull Float);
                    method @Nullable public Double convert2(@NonNull Float);
                    method @Nullable public Double convert3(@NonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double! convert0(Float!);
                    method public Double? convert1(Float);
                    method public Double? convert2(Float);
                    method public Double? convert3(Float);
                    method public Double? convert4(Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Method which is now marked null should be marked as newly migrated null`() {
        check(
            migrateNulls = true,
            outputKotlinStyleNulls = false,
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public abstract class MyTest {
                    method @Nullable public Double convert1(Float);
                  }
                }
                """,
            previousApi = """
                package test.pkg {
                  public abstract class MyTest {
                    method public Double convert1(Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public abstract class MyTest {
                    method @NewlyNullable public Double convert1(Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Parameter which is now marked null should be marked as newly migrated null`() {
        check(
            migrateNulls = true,
            outputKotlinStyleNulls = false,
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public abstract class MyTest {
                    method public Double convert1(@NonNull Float);
                  }
                }
                """,
            previousApi = """
                package test.pkg {
                  public abstract class MyTest {
                    method public Double convert1(Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public abstract class MyTest {
                    method public Double convert1(@NewlyNonNull Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Comprehensive check of migration`() {
        check(
            migrateNulls = true,
            outputKotlinStyleNulls = false,
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method @Nullable public Double convert1(@NonNull Float);
                    method @Nullable public Double convert2(@NonNull Float);
                    method @Nullable public Double convert3(@NonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """,
            previousApi = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method public Double convert1(Float);
                    method @NewlyNullable public Double convert2(@NewlyNonNull Float);
                    method @RecentlyNullable public Double convert3(@RecentlyNonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method @NewlyNullable public Double convert1(@NewlyNonNull Float);
                    method @RecentlyNullable public Double convert2(@RecentlyNonNull Float);
                    method @Nullable public Double convert3(@NonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Comprehensive check of migration, Kotlin-style output`() {
        check(
            migrateNulls = true,
            outputKotlinStyleNulls = true,
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method @Nullable public Double convert1(@NonNull Float);
                    method @Nullable public Double convert2(@NonNull Float);
                    method @Nullable public Double convert3(@NonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """,
            previousApi = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double convert0(Float);
                    method public Double convert1(Float);
                    method @NewlyNullable public Double convert2(@NewlyNonNull Float);
                    method @RecentlyNullable public Double convert3(@RecentlyNonNull Float);
                    method @Nullable public Double convert4(@NonNull Float);
                  }
                }
                """,
            api = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public Double! convert0(Float!);
                    method public Double? convert1(Float);
                    method public Double? convert2(Float);
                    method public Double? convert3(Float);
                    method public Double? convert4(Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Convert libcore nullness annotations to support`() {
        check(
            outputKotlinStyleNulls = false,
            compatibilityMode = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;
                    public class Test {
                        public @libcore.util.NonNull Object compute() {
                            return 5;
                        }
                    }
                    """
                ),
                java(
                    """
                    package libcore.util;
                    import static java.lang.annotation.ElementType.TYPE_USE;
                    import static java.lang.annotation.ElementType.TYPE_PARAMETER;
                    import static java.lang.annotation.RetentionPolicy.SOURCE;
                    import java.lang.annotation.Documented;
                    import java.lang.annotation.Retention;
                    @Documented
                    @Retention(SOURCE)
                    @Target({TYPE_USE})
                    public @interface NonNull {
                       int from() default Integer.MIN_VALUE;
                       int to() default Integer.MAX_VALUE;
                    }
                    """
                )
            ),
            api = """
                    package libcore.util {
                      public @interface NonNull {
                        method public abstract int from();
                        method public abstract int to();
                      }
                    }
                    package test.pkg {
                      public class Test {
                        ctor public Test();
                        method @NonNull public Object compute();
                      }
                    }
                """
        )
    }
}