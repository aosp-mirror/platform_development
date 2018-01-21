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

interface ParameterItem : Item {
    /** The name of this field */
    fun name(): String

    /** The type of this field */
    fun type(): TypeItem

    /** The containing method */
    fun containingMethod(): MethodItem

    /** Index of this parameter in the parameter list (0-based) */
    val parameterIndex: Int

    /**
     * The public name of this parameter. In Kotlin, names are part of the
     * public API; in Java they are not. In Java, you can annotate a
     * parameter with {@literal @ParameterName("foo")} to name the parameter
     * something (potentially different from the actual code parameter name).
     */
    fun publicName(): String?

    override fun parent(): MethodItem? = containingMethod()

    override fun accept(visitor: ItemVisitor) {
        if (visitor.skip(this)) {
            return
        }

        visitor.visitItem(this)
        visitor.visitParameter(this)

        visitor.afterVisitParameter(this)
        visitor.afterVisitItem(this)
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        if (visitor.skip(this)) {
            return
        }

        val type = type()
        visitor.visitType(type, this)
        visitor.afterVisitType(type, this)
    }

    override fun requiresNullnessInfo(): Boolean {
        return !type().primitive
    }

    override fun hasNullnessInfo(): Boolean {
        if (!requiresNullnessInfo()) {
            return true
        }

        return modifiers.hasNullnessInfo()
    }

    // TODO: modifier list
}