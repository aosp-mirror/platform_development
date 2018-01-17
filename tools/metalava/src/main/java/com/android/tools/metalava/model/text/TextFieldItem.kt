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

import com.android.tools.metalava.doclava1.SourcePositionInfo
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.TypeItem

class TextFieldItem(
    codebase: Codebase,
    name: String,
    containingClass: TextClassItem,
    isPublic: Boolean,
    isProtected: Boolean,
    isPrivate: Boolean,
    isFinal: Boolean, isStatic: Boolean,
    isTransient: Boolean,
    isVolatile: Boolean,
    private val type: TextTypeItem,
    private val constantValue: Any?,
    position: SourcePositionInfo,
    annotations: List<String>?
) : TextMemberItem(
    codebase, name, containingClass, position,
    TextModifiers(
        codebase = codebase,
        annotationStrings = annotations,
        public = isPublic, protected = isProtected, private = isPrivate,
        static = isStatic, final = isFinal, transient = isTransient, volatile = isVolatile
    )
), FieldItem {

    init {
        (modifiers as TextModifiers).owner = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FieldItem) return false

        if (name() != other.name()) {
            return false
        }

        return containingClass() == other.containingClass()
    }

    override fun hashCode(): Int = name().hashCode()

    override fun type(): TypeItem = type

    override fun initialValue(requireConstant: Boolean): Any? = constantValue

    override fun toString(): String = "Field ${containingClass().fullName()}.${name()}"

    override fun duplicate(targetContainingClass: ClassItem): FieldItem = codebase.unsupported()

    private var isEnumConstant = false
    override fun isEnumConstant(): Boolean = isEnumConstant
    fun setEnumConstant(isEnumConstant: Boolean) {
        this.isEnumConstant = isEnumConstant
    }
}