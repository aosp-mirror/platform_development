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

import com.android.tools.metalava.model.visitors.ApiVisitor
import com.android.tools.metalava.model.visitors.ItemVisitor
import com.android.tools.metalava.model.visitors.TypeVisitor
import com.android.tools.metalava.tick

interface PackageItem : Item {
    /** The qualified name of this package */
    fun qualifiedName(): String

    /** All top level classes in this package */
    fun topLevelClasses(): Sequence<ClassItem>

    /** All top level classes **and inner classes** in this package */
    fun allClasses(): Sequence<ClassItem> {
        return topLevelClasses().asSequence().flatMap { it.allClasses() }
    }

    val isDefault get() = qualifiedName().isEmpty()

    override fun parent(): PackageItem? = if (qualifiedName().isEmpty()) null else containingPackage()

    fun containingPackage(): PackageItem? {
        val name = qualifiedName()
        val lastDot = name.lastIndexOf('.')
        return if (lastDot != -1) {
            codebase.findPackage(name.substring(0, lastDot))
        } else {
            null
        }
    }

    /** Whether this package is empty */
    fun empty() = topLevelClasses().none()

    override fun accept(visitor: ItemVisitor) {
        if (visitor.skipEmptyPackages && empty()) {
            return
        }

        if (visitor is ApiVisitor) {
            if (!emit) {
                return
            }

            // For the API visitor packages are visited lazily; only when we encounter
            // an unfiltered item within the class
            topLevelClasses()
                .asSequence()
                .sortedWith(ClassItem.classNameSorter())
                .forEach {
                    tick()
                    it.accept(visitor)
                }

            if (visitor.visitingPackage) {
                visitor.visitingPackage = false
                visitor.afterVisitPackage(this)
                visitor.afterVisitItem(this)
            }

            return
        }


        if (visitor.skip(this)) {
            return
        }

        visitor.visitItem(this)
        visitor.visitPackage(this)

        for (cls in topLevelClasses()) {
            cls.accept(visitor)
        }

        visitor.afterVisitPackage(this)
        visitor.afterVisitItem(this)
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        if (visitor.skip(this)) {
            return
        }

        for (unit in topLevelClasses()) {
            unit.acceptTypes(visitor)
        }
    }

    companion object {
        val comparator: Comparator<PackageItem> = Comparator { a, b -> a.qualifiedName().compareTo(b.qualifiedName()) }
    }
}