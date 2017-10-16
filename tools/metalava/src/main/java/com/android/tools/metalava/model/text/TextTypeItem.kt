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

package com.android.tools.metalava.model.text

import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.TypeItem

class TextTypeItem(
    val codebase: Codebase,
    val type: String
) : TypeItem {
    override fun toString(): String = type

    override fun toErasedTypeString(): String {
        val s = toString()
        val index = s.indexOf('<')
        if (index != -1) {
            return s.substring(0, index)
        }
        return s
    }

    override fun toFullyQualifiedString(): String = type

    override fun asClass(): ClassItem? {
        val cls = toErasedTypeString()
        return codebase.findClass(cls)
    }

    fun qualifiedTypeName(): String = type

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextTypeItem) return false

        return qualifiedTypeName() == other.qualifiedTypeName()
    }

    override fun hashCode(): Int {
        return qualifiedTypeName().hashCode()
    }

    override val primitive: Boolean
        get() = isPrimitive(type)

    override fun typeArgumentClasses(): List<ClassItem> = codebase.unsupported()

    override fun convertType(replacementMap: Map<String, String>?, owner: Item?): TypeItem {
        return TextTypeItem(codebase, convertTypeString(replacementMap))
    }

    companion object {
        fun isPrimitive(type: String): Boolean {
            return when (type) {
                "byte", "char", "double", "float", "int", "long", "short", "boolean", "void", "null" -> true
                else -> false
            }
        }
    }
}