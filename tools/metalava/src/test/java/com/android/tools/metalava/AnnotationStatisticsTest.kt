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

import com.android.tools.lint.checks.infrastructure.TestFiles.base64gzip
import com.android.tools.lint.checks.infrastructure.TestFiles.jar
import org.junit.Test

class AnnotationStatisticsTest : DriverTest() {
    @Test
    fun `Test emitting annotation statistics`() {
        check(
            extraArguments = arrayOf("--annotation-coverage-stats"),
            expectedOutput = """
                Nullness Annotation Coverage Statistics:
                4 out of 7 methods were annotated (57%)
                0 out of 0 fields were annotated (0%)
                4 out of 5 parameters were annotated (80%)
                """,
            compatibilityMode = false,
            signatureSource = """
                package test.pkg {
                  public class MyTest {
                    ctor public MyTest();
                    method public java.lang.Double convert0(java.lang.Float);
                    method @android.support.annotation.Nullable public java.lang.Double convert1(@android.support.annotation.NonNull java.lang.Float);
                    method @android.support.annotation.Nullable public java.lang.Double convert2(@android.support.annotation.NonNull java.lang.Float);
                    method @android.support.annotation.Nullable public java.lang.Double convert3(@android.support.annotation.NonNull java.lang.Float);
                    method @android.support.annotation.Nullable public java.lang.Double convert4(@android.support.annotation.NonNull java.lang.Float);
                  }
                }
                """
        )
    }

    @Test
    fun `Test counting annotation usages of missing APIs`() {
        check(
            coverageJars = arrayOf(
                /*
                    package test.pkg;

                    public class ApiUsage {
                        ApiUsage() {
                            new ApiSurface(null).annotated1("Hello");
                            new ApiSurface(null).missing1(null);
                        }

                        Number myField = new ApiSurface(null, 5).missingField1;

                        public void usage() {
                            ApiSurface apiSurface = new ApiSurface(null, 5);
                            apiSurface.annotated1("Hello");
                            apiSurface.missing1(null);
                            apiSurface.missing2(null, 5);
                            apiSurface.missing3(5);
                            apiSurface.missing4(null);
                            apiSurface.missing5("Hello");
                        }
                    }
                 */
                jar(
                    "libs/api-usage.jar",
                    base64gzip(
                        "test/pkg/ApiUsage.class", "" +
                                "H4sIAAAAAAAAAH1Ta28SQRQ9Q4Fd1qW0VPD9KNZKWe0WaH3VmBiTRpKNfqiW" +
                                "+HGgIw4uu4RdTPzqPzJRSDTxB/ijjHcGWqqlZjNn5945c86du7O/fn//CaCO" +
                                "xxZyuG7ghoUEbmYIVjNYREmFt0ysqfdtBesK7igoK9hQUDHgGLjLYPQ+7Unh" +
                                "HzLkvS7/yF2fBx335bDXEoNdhvQTGcj4KcNCeeOAIfk8PBQMOU8GYsJ5zVu+" +
                                "UJvDNvcP+ECqeJpMxu9lxLDixSKK3f6HjvusL99EvCNIOTVUEwaL9+X+cPCO" +
                                "tyko/EWdpols7YfDQVvsSSWbPVLZVAXbyGOFTOZsVEv3bGxi2caSgjxcMn4h" +
                                "fD+0sYWqjRrqNraxY+M+Hth4qHKPUGVYPlUzw9KsQa9aXdGOGYpl79/kbkN1" +
                                "KtuTUSSDjm4u6RXmEBXP4kEQxjwWirQ+j3Q6xWBO1c8SbswotbOKPMGpK5nG" +
                                "f522Z9MdrNI9y9ElZDSosYQJGvQdKHOeZl2k9NpWZQxW+YHEW2aOsfAVyW9I" +
                                "6XCMdN4YwWweRWyETPOLVioQFkkBSNKTIcUUSkjDhUF5wJ5o4wIu6houHft+" +
                                "Jr6qpHZs6TkTG0frO8wcwWo6hOd0ym7q9ewJ5xJM7WEhSydbJBf6y+iUaxRV" +
                                "6IxVcivqCrXTtAoLZVzGFaqD4arWuvYH9nECI6kDAAA="
                    )
                )
            ),
            sourceFiles = *arrayOf(
                java(
                    """
                    package test.pkg;

                    import android.support.annotation.NonNull;
                    import android.support.annotation.Nullable;

                    public class ApiSurface {
                        ApiSurface(Object param) {
                        }

                        ApiSurface(@Nullable Object param, int param2) {
                        }

                        @Nullable public Object annotated1(@NonNull Object param1) { return null; }
                        public int annotated2(@NonNull Object param1, int param2) { return 1; }

                        @NonNull public String annotatedField1 = "";
                        public int annotatedField2;
                        public Number missingField1;
                        public Number missingField2;

                        public int missing1(Object param1) { return 0; }
                        public int missing2(Object param1, int param2) { return 0; }
                        public Object missing3(int param1) { return null; }
                        @Nullable public Object missing4(Object param1) { return null; }
                        public Object missing5(@NonNull Object param1) { return null; }

                        public class InnerClass {
                            @Nullable public Object annotated3(@NonNull Object param1) { return null; }
                            public int annotated4(@NonNull Object param1, int param2) { return 1; }
                            public int missing1(Object param1) { return 0; }
                            public int missing2(Object param1, int param2) { return 0; }
                        }
                    }
                    """
                ),
                supportNonNullSource,
                supportNullableSource
            ),
            expectedOutput = """
                6 methods and fields were missing nullness annotations out of 7 total API references.
                API nullness coverage is 14%

                |--------------------------------------------------------------|------------------|
                | Qualified Class Name                                         |      Usage Count |
                |--------------------------------------------------------------|-----------------:|
                | test.pkg.ApiSurface                                          |                7 |
                |--------------------------------------------------------------|------------------|

                Top referenced un-annotated members:

                |--------------------------------------------------------------|------------------|
                | Member                                                       |      Usage Count |
                |--------------------------------------------------------------|-----------------:|
                | ApiSurface.missing1(Object)                                  |                2 |
                | ApiSurface.missingField1                                     |                1 |
                | ApiSurface.missing2(Object, int)                             |                1 |
                | ApiSurface.missing3(int)                                     |                1 |
                | ApiSurface.missing4(Object)                                  |                1 |
                | ApiSurface.missing5(Object)                                  |                1 |
                |--------------------------------------------------------------|------------------|
                """,
            compatibilityMode = false
        )
    }
}