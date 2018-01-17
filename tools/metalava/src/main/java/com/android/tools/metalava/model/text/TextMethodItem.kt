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
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.TypeItem
import java.util.function.Predicate

open class TextMethodItem(
    codebase: Codebase,
    name: String,
    containingClass: TextClassItem,
    isPublic: Boolean,
    isProtected: Boolean,
    isPrivate: Boolean,
    isFinal: Boolean,
    isStatic: Boolean,
    isAbstract: Boolean,
    isSynchronized: Boolean,
    isNative: Boolean,
    isDefault: Boolean,
    private val returnType: TextTypeItem?,
    position: SourcePositionInfo,
    annotations: List<String>?
) : TextMemberItem(
    // Explicitly coerce 'final' state of Java6-compiled enum values() method, to match
    // the Java5-emitted base API description.
    codebase, name, containingClass, position,
    modifiers = TextModifiers(
        codebase = codebase,
        annotationStrings = annotations, public = isPublic, protected = isProtected,
        private = isPrivate, static = isStatic, final = isFinal, abstract = isAbstract,
        synchronized = isSynchronized, native = isNative, default = isDefault
    )
), MethodItem {

    init {
        (modifiers as TextModifiers).owner = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MethodItem) return false

        if (name() != other.name()) {
            return false
        }

        if (containingClass() != other.containingClass()) {
            return false
        }

        val parameters1 = parameters()
        val parameters2 = other.parameters()

        if (parameters1.size != parameters2.size) {
            return false
        }

        for (i in 0 until parameters1.size) {
            val parameter1 = parameters1[i]
            val parameter2 = parameters2[i]
            if (parameter1.type() != parameter2.type()) {
                return false
            }
        }
        return true
    }

    override fun hashCode(): Int {
        return name().hashCode()
    }

    override fun isConstructor(): Boolean = false

    override fun returnType(): TypeItem? = returnType

    override fun superMethods(): List<MethodItem> = codebase.unsupported()

    override fun findPredicateSuperMethod(predicate: Predicate<Item>): MethodItem? = null

    private var typeParameterList: String? = null

    fun setTypeParameterList(typeParameterList: String?) {
        this.typeParameterList = typeParameterList
    }

    override fun typeParameterList(): String? = typeParameterList

    override fun duplicate(targetContainingClass: ClassItem): MethodItem = codebase.unsupported()

    private val throwsTypes = mutableListOf<String>()
    private val parameters = mutableListOf<TextParameterItem>()
    private var throwsClasses: List<ClassItem>? = null

    fun throwsTypeNames(): List<String> {
        return throwsTypes
    }

    override fun throwsTypes(): List<ClassItem> = if (throwsClasses == null) emptyList() else throwsClasses!!

    fun setThrowsList(throwsClasses: List<TextClassItem>) {
        this.throwsClasses = throwsClasses
    }

    override fun parameters(): List<ParameterItem> = parameters

    fun addException(throwsType: String) {
        throwsTypes += throwsType
    }

    fun addParameter(parameter: TextParameterItem) {
        parameters += parameter
    }

    private var varargs: Boolean = false

    fun setVarargs(varargs: Boolean) {
        this.varargs = varargs
    }

    fun isVarargs(): Boolean = varargs

    override var inheritedInterfaceMethod: Boolean = false

    override fun toString(): String =
        "${if (isConstructor()) "Constructor" else "Method"} ${containingClass().qualifiedName()}.${name()}(${parameters().joinToString {
            it.type().toSimpleType()
        }})"
}


