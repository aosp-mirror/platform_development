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

import com.android.tools.metalava.model.visitors.ItemVisitor
import com.android.tools.metalava.model.visitors.TypeVisitor

class PackageList(val packages: List<PackageItem>) {
    fun accept(visitor: ItemVisitor) {
        packages.forEach {
            it.accept(visitor)
        }
    }

    fun acceptTypes(visitor: TypeVisitor) {
        packages.forEach {
            it.acceptTypes(visitor)
        }
    }

    /** All top level classes in all packages */
    fun allTopLevelClasses(): Sequence<ClassItem> {
        return packages.asSequence().flatMap { it.topLevelClasses() }
    }

    /** All top level classes **and inner classes** in all packages */
    fun allClasses(): Sequence<ClassItem> {
        return packages.asSequence().flatMap { it.allClasses() }
    }
}