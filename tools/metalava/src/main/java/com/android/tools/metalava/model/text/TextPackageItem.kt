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
import com.android.tools.metalava.model.PackageItem

class TextPackageItem(
    codebase: Codebase,
    private val name: String,
    position: SourcePositionInfo
) : TextItem(codebase, position, modifiers = TextModifiers(codebase = codebase, public = true)), PackageItem {
    init {
        (modifiers as TextModifiers).owner = this
    }

    private val classes = ArrayList<TextClassItem>(100)

    fun name() = name

    fun addClass(classInfo: TextClassItem) {
        classes.add(classInfo)
    }

    internal fun classList(): List<ClassItem> = classes

    override fun topLevelClasses(): Sequence<ClassItem> = classes.asSequence()

    override fun qualifiedName(): String = name

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PackageItem) return false

        return name == other.qualifiedName()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = name
}
