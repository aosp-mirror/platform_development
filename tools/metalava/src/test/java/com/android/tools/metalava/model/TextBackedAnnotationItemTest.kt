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

package com.android.tools.metalava.model

import com.android.tools.metalava.model.text.TextBackedAnnotationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.function.Predicate

class TextBackedAnnotationItemTest {
    // Dummy for use in test where we don't need codebase functionality
    private val dummyCodebase = object : DefaultCodebase() {
        override fun supportsDocumentation(): Boolean = false
        override var description: String = ""
        override fun getPackages(): PackageList = unsupported()
        override fun size(): Int = unsupported()
        override fun findClass(className: String): ClassItem? = unsupported()
        override fun findPackage(pkgName: String): PackageItem? = unsupported()
        override fun trustedApi(): Boolean = false
        override fun filter(filterEmit: Predicate<Item>, filterReference: Predicate<Item>): Codebase = unsupported()
        override var supportsStagedNullability: Boolean = false
    }

    @Test
    fun testSimple() {
        val annotation = TextBackedAnnotationItem(
            dummyCodebase,
            "@android.support.annotation.Nullable"
        )
        assertEquals("@android.support.annotation.Nullable", annotation.toSource())
        assertEquals("android.support.annotation.Nullable", annotation.qualifiedName())
        assertTrue(annotation.attributes().isEmpty())
    }

    @Test
    fun testIntRange() {
        val annotation = TextBackedAnnotationItem(
            dummyCodebase,
            "@android.support.annotation.IntRange(from = 20, to = 40)"
        )
        assertEquals("@android.support.annotation.IntRange(from = 20, to = 40)", annotation.toSource())
        assertEquals("android.support.annotation.IntRange", annotation.qualifiedName())
        assertEquals(2, annotation.attributes().size)
        assertEquals("from", annotation.findAttribute("from")?.name)
        assertEquals("20", annotation.findAttribute("from")?.value.toString())
        assertEquals("to", annotation.findAttribute("to")?.name)
        assertEquals("40", annotation.findAttribute("to")?.value.toString())
    }

    @Test
    fun testIntDef() {
        val annotation = TextBackedAnnotationItem(
            dummyCodebase,
            "@android.support.annotation.IntDef({STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT})"
        )
        assertEquals(
            "@android.support.annotation.IntDef({STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT})",
            annotation.toSource()
        )
        assertEquals("android.support.annotation.IntDef", annotation.qualifiedName())
        assertEquals(1, annotation.attributes().size)
        val attribute = annotation.findAttribute("value")
        assertNotNull(attribute)
        assertEquals("value", attribute?.name)
        assertEquals(
            "{STYLE_NORMAL, STYLE_NO_TITLE, STYLE_NO_FRAME, STYLE_NO_INPUT}",
            annotation.findAttribute("value")?.value.toString()
        )

        assertTrue(attribute?.value is AnnotationArrayAttributeValue)
        if (attribute is AnnotationArrayAttributeValue) {
            val list = attribute.values
            assertEquals(3, list.size)
            assertEquals("STYLE_NO_TITLE", list[1].toSource())
        }
    }
}