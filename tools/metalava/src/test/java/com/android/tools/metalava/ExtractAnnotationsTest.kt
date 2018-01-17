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

import com.android.utils.SdkUtils.fileToUrlString
import com.google.common.base.Charsets
import com.google.common.io.ByteStreams
import com.google.common.io.Closeables
import com.google.common.io.Files
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URL

@SuppressWarnings("ALL") // Sample code
class ExtractAnnotationsTest : DriverTest() {

    @Test
    fun `Include class retention`() {
        val androidJar = getPlatformFile("android.jar")

        val project = createProject(
            packageTest,
            genericTest,
            intDefTest,
            permissionsTest,
            manifest,
            intDefAnnotation,
            intRangeAnnotation,
            permissionAnnotation,
            nullableAnnotation
        )

        val output = temporaryFolder.newFile("annotations.zip")

        runDriver(
            "--sources",
            File(project, "src").path,
            "--classpath",
            androidJar.path,
            "--extract-annotations",
            output.path
        )

        // Check extracted annotations
        checkPackageXml(
            "test.pkg", output, """<?xml version="1.0" encoding="UTF-8"?>
<root>
  <item name="test.pkg">
    <annotation name="android.support.annotation.IntRange">
      <val name="from" val="20" />
    </annotation>
  </item>
  <item name="test.pkg.IntDefTest void setFlags(java.lang.Object, int) 1">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT, 3, 4}" />
      <val name="flag" val="true" />
    </annotation>
  </item>
  <item name="test.pkg.IntDefTest void setStyle(int, int) 0">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT}" />
    </annotation>
    <annotation name="android.support.annotation.IntRange">
      <val name="from" val="20" />
    </annotation>
  </item>
  <item name="test.pkg.IntDefTest.Inner void setInner(int) 0">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT, 3, 4}" />
      <val name="flag" val="true" />
    </annotation>
  </item>
  <item name="test.pkg.MyEnhancedList">
    <annotation name="android.support.annotation.IntRange">
      <val name="from" val="0" />
    </annotation>
  </item>
  <item name="test.pkg.MyEnhancedList E getReversed(java.util.List&lt;java.lang.String&gt;, java.util.Comparator&lt;? super E&gt;)">
    <annotation name="android.support.annotation.IntRange">
      <val name="from" val="10" />
    </annotation>
  </item>
  <item name="test.pkg.MyEnhancedList java.lang.String getPrefix()">
    <annotation name="android.support.annotation.Nullable" />
  </item>
  <item name="test.pkg.PermissionsTest CONTENT_URI">
    <annotation name="android.support.annotation.RequiresPermission.Read">
      <val name="value" val="&quot;android.permission.MY_READ_PERMISSION_STRING&quot;" />
    </annotation>
    <annotation name="android.support.annotation.RequiresPermission.Write">
      <val name="value" val="&quot;android.permission.MY_WRITE_PERMISSION_STRING&quot;" />
    </annotation>
  </item>
  <item name="test.pkg.PermissionsTest void myMethod()">
    <annotation name="android.support.annotation.RequiresPermission">
      <val name="value" val="&quot;android.permission.MY_PERMISSION_STRING&quot;" />
    </annotation>
  </item>
  <item name="test.pkg.PermissionsTest void myMethod2()">
    <annotation name="android.support.annotation.RequiresPermission">
      <val name="anyOf" val="{&quot;android.permission.MY_PERMISSION_STRING&quot;, &quot;android.permission.MY_PERMISSION_STRING2&quot;}" />
    </annotation>
  </item>
</root>

"""
        )
    }

    @Test
    fun `Skip class retention`() {
        val androidJar = getPlatformFile("android.jar")

        val project = createProject(
            intDefTest,
            permissionsTest,
            manifest,
            intDefAnnotation,
            intRangeAnnotation,
            permissionAnnotation
        )

        val output = temporaryFolder.newFile("annotations.zip")

        runDriver(
            "--sources",
            File(project, "src").path,
            "--classpath",
            androidJar.path,
            "--skip-class-retention",
            "--extract-annotations",
            output.path
        )

        // Check external annotations
        checkPackageXml(
            "test.pkg", output,
            """<?xml version="1.0" encoding="UTF-8"?>
<root>
  <item name="test.pkg.IntDefTest void setFlags(java.lang.Object, int) 1">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT, 3, 4}" />
      <val name="flag" val="true" />
    </annotation>
  </item>
  <item name="test.pkg.IntDefTest void setStyle(int, int) 0">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT}" />
    </annotation>
    <annotation name="android.support.annotation.IntRange">
      <val name="from" val="20" />
    </annotation>
  </item>
  <item name="test.pkg.IntDefTest.Inner void setInner(int) 0">
    <annotation name="android.support.annotation.IntDef">
      <val name="value" val="{test.pkg.IntDefTest.STYLE_NORMAL, test.pkg.IntDefTest.STYLE_NO_TITLE, test.pkg.IntDefTest.STYLE_NO_FRAME, test.pkg.IntDefTest.STYLE_NO_INPUT, 3, 4}" />
      <val name="flag" val="true" />
    </annotation>
  </item>
</root>

"""
        )
    }

    @Test
    fun `Test writing jar recipe file`() {
        val androidJar = getPlatformFile("android.jar")

        val project = createProject(
            intDefTest,
            permissionsTest,
            manifest,
            intDefAnnotation,
            intRangeAnnotation,
            permissionAnnotation
        )

        val output = temporaryFolder.newFile("annotations.zip")
        val typedefFile = temporaryFolder.newFile("typedefs.txt")

        runDriver(
            "--sources",
            File(project, "src").path,
            "--classpath",
            androidJar.path,

            "--extract-annotations",
            output.path,
            "--typedef-file",
            typedefFile.path
        )

        // Check recipe
        assertEquals(
            """D test/pkg/IntDefTest${"$"}DialogFlags
D test/pkg/IntDefTest${"$"}DialogStyle
""",
            Files.asCharSource(typedefFile, Charsets.UTF_8).read()
        )
    }

    @SuppressWarnings("all") // sample code
    private val intDefAnnotation = java(
        """
package android.support.annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@Retention(SOURCE)
@Target({ANNOTATION_TYPE})
public @interface IntDef {
    long[] value() default {};
    boolean flag() default false;
}
"""
    )

    @SuppressWarnings("all") // sample code
    private val intRangeAnnotation = java(
        """
package android.support.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(CLASS)
@Target({CONSTRUCTOR,METHOD,PARAMETER,FIELD,LOCAL_VARIABLE,ANNOTATION_TYPE,PACKAGE})
public @interface IntRange {
    long from() default Long.MIN_VALUE;
    long to() default Long.MAX_VALUE;
}
"""
    )

    @SuppressWarnings("all") // sample code
    private val permissionAnnotation = java(
        """
package android.support.annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
@Retention(CLASS)
@Target({ANNOTATION_TYPE,METHOD,CONSTRUCTOR,FIELD})
public @interface RequiresPermission {
    String value() default "";
    String[] allOf() default {};
    String[] anyOf() default {};
    boolean conditional() default false;
    @Target(FIELD)
    @interface Read {
        RequiresPermission value();
    }
    @Target(FIELD)
    @interface Write {
        RequiresPermission value();
    }
}"""
    )

    @SuppressWarnings("all") // sample code
    private val nullableAnnotation = java(
        """
package android.support.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
@Retention(CLASS)
@Target({METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE})
public @interface Nullable {
}"""
    )

    @SuppressWarnings("all") // sample code
    private val packageTest = java(
        """
@IntRange(from = 20)
package test.pkg;

import android.support.annotation.IntRange;"""
    )

    @SuppressWarnings("all") // sample code
    private val genericTest = java(
        """package test.pkg;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.List;

@IntRange(from = 0)
public interface MyEnhancedList<E> extends List<E> {
    @IntRange(from = 10)
    E getReversed(List<String> filter, Comparator<? super E> comparator);
    @Nullable String getPrefix();
}
"""
    )

    @SuppressWarnings("all") // sample code
    private val intDefTest = java(
        """
package test.pkg;

import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.Keep;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"})
public class IntDefTest {
    @IntDef({STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT})
    @IntRange(from = 20)
    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogStyle {}

    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_NO_TITLE = 1;
    public static final int STYLE_NO_FRAME = 2;
    public static final int STYLE_NO_INPUT = 3;
    public static final int UNRELATED = 3;

    public void setStyle(@DialogStyle int style, int theme) {
    }

    @Keep    public void testIntDef(int arg) {
    }
    @IntDef(value = {STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT, 3, 3 + 1}, flag=true)
    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogFlags {}

    public void setFlags(Object first, @DialogFlags int flags) {
    }

    public static final String TYPE_1 = "type1";
    public static final String TYPE_2 = "type2";
    public static final String UNRELATED_TYPE = "other";

    public static class Inner {
        public void setInner(@DialogFlags int flags) {
        }
    }
}"""
    )

    @SuppressWarnings("all") // sample code
    private val permissionsTest = java(
        """package test.pkg;

import android.support.annotation.RequiresPermission;

public class PermissionsTest {
    @RequiresPermission(Manifest.permission.MY_PERMISSION)
    public void myMethod() {
    }
    @RequiresPermission(anyOf={Manifest.permission.MY_PERMISSION,Manifest.permission.MY_PERMISSION2})
    public void myMethod2() {
    }


    @RequiresPermission.Read(@RequiresPermission(Manifest.permission.MY_READ_PERMISSION))
    @RequiresPermission.Write(@RequiresPermission(Manifest.permission.MY_WRITE_PERMISSION))
    public static final String CONTENT_URI = "";
}
"""
    )

    @SuppressWarnings("all") // sample code
    private val manifest = java(
        """
package test.pkg;

public class Manifest {
    public static final class permission {
        public static final String MY_PERMISSION = "android.permission.MY_PERMISSION_STRING";
        public static final String MY_PERMISSION2 = "android.permission.MY_PERMISSION_STRING2";
        public static final String MY_READ_PERMISSION = "android.permission.MY_READ_PERMISSION_STRING";
        public static final String MY_WRITE_PERMISSION = "android.permission.MY_WRITE_PERMISSION_STRING";
    }
}
"""
    )

    private fun checkPackageXml(pkg: String, output: File, @Language("XML") expected: String) {
        assertNotNull(output)
        assertTrue(output.exists())
        val url = URL(
            "jar:" + fileToUrlString(output) + "!/" + pkg.replace('.', '/') +
                    "/annotations.xml"
        )
        val stream = url.openStream()
        try {
            val bytes = ByteStreams.toByteArray(stream)
            assertNotNull(bytes)
            val xml = String(bytes, Charsets.UTF_8).replace("\r\n", "\n")
            assertEquals(expected, xml)
        } finally {
            Closeables.closeQuietly(stream)
        }
    }
}