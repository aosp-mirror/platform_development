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

class AnnotationsMergerTest : DriverTest() {

    // TODO: Test what happens when we have conflicting data
    //   - NULLABLE_SOURCE on one non null on the other
    //   - annotation specified with different parameters (e.g @Size(4) vs @Size(6))

    @Test
    fun `Signature files contain annotations`() {
        check(
            compatibilityMode = false,
            outputKotlinStyleNulls = false,
            includeSystemApiAnnotations = false,
            omitCommonPackages = false,
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    import android.support.annotation.NonNull;
                    import android.support.annotation.Nullable;
                    import android.annotation.IntRange;
                    import android.support.annotation.UiThread;

                    @UiThread
                    public class MyTest {
                        public @Nullable Number myNumber;
                        public @Nullable Double convert(@NonNull Float f) { return null; }
                        public @IntRange(from=10,to=20) int clamp(int i) { return 10; }
                    }"""
                ),
                uiThreadSource,
                intRangeAnnotationSource,
                supportNonNullSource,
                supportNullableSource
            ),
            // Skip the annotations themselves from the output
            extraArguments = arrayOf(
                "--hide-package", "android.annotation",
                "--hide-package", "android.support.annotation"
            ),
            api = """
                package test.pkg {
                  @android.support.annotation.UiThread public class MyTest {
                    ctor public MyTest();
                    method @android.support.annotation.IntRange(from=10, to=20) public int clamp(int);
                    method @android.support.annotation.Nullable public java.lang.Double convert(@android.support.annotation.NonNull java.lang.Float);
                    field @android.support.annotation.Nullable public java.lang.Number myNumber;
                  }
                }
                """
        )
    }

    @Test
    fun `Merged class and method annotations with no arguments`() {
        check(
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    public class MyTest {
                        public Number myNumber;
                        public Double convert(Float f) { return null; }
                        public int clamp(int i) { return 10; }
                    }
                    """
                )
            ),
            compatibilityMode = false,
            outputKotlinStyleNulls = false,
            omitCommonPackages = false,
            mergeAnnotations = """<?xml version="1.0" encoding="UTF-8"?>
                <root>
                  <item name="test.pkg.MyTest">
                    <annotation name="android.support.annotation.UiThread" />
                  </item>
                  <item name="test.pkg.MyTest java.lang.Double convert(java.lang.Float)">
                    <annotation name="android.support.annotation.Nullable" />
                  </item>
                  <item name="test.pkg.MyTest java.lang.Double convert(java.lang.Float) 0">
                    <annotation name="android.support.annotation.NonNull" />
                  </item>
                  <item name="test.pkg.MyTest myNumber">
                    <annotation name="android.support.annotation.Nullable" />
                  </item>
                  <item name="test.pkg.MyTest int clamp(int)">
                    <annotation name="android.support.annotation.IntRange">
                      <val name="from" val="10" />
                      <val name="to" val="20" />
                    </annotation>
                  </item>
                  </root>
                """,
            api = """
                package test.pkg {
                  @android.support.annotation.UiThread public class MyTest {
                    ctor public MyTest();
                    method @android.support.annotation.IntRange(from=10, to=20) public int clamp(int);
                    method @android.support.annotation.Nullable public java.lang.Double convert(@android.support.annotation.NonNull java.lang.Float);
                    field @android.support.annotation.Nullable public java.lang.Number myNumber;
                  }
                }
                """
        )
    }
}
