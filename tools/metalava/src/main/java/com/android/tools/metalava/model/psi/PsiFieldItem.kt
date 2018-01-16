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

package com.android.tools.metalava.model.psi

import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.TypeItem
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.impl.JavaConstantExpressionEvaluator

class PsiFieldItem(
    override val codebase: PsiBasedCodebase,
    private val psiField: PsiField,
    private val containingClass: PsiClassItem,
    private val name: String,
    modifiers: PsiModifierItem,
    documentation: String,
    private val fieldType: PsiTypeItem,
    private val isEnumConstant: Boolean,
    private val initialValue: Any?
) :
    PsiItem(
        codebase = codebase,
        modifiers = modifiers,
        documentation = documentation,
        element = psiField
    ), FieldItem {

    override fun type(): TypeItem = fieldType
    override fun initialValue(requireConstant: Boolean): Any? {
        if (initialValue != null) {
            return initialValue
        }
        val constant = psiField.computeConstantValue()
        if (constant != null) {
            return constant
        }

        return if (!requireConstant) {
            val initializer = psiField.initializer ?: return null
            JavaConstantExpressionEvaluator.computeConstantExpression(initializer, false)
        } else {
            null
        }
    }

    override fun isEnumConstant(): Boolean = isEnumConstant
    override fun name(): String = name
    override fun containingClass(): ClassItem = containingClass

    override fun duplicate(targetContainingClass: ClassItem): PsiFieldItem {
        val duplicated = create(codebase, targetContainingClass as PsiClassItem, psiField)

        // Preserve flags that may have been inherited (propagated) fro surrounding packages
        if (targetContainingClass.hidden) {
            duplicated.hidden = true
        }
        if (targetContainingClass.removed) {
            duplicated.removed = true
        }
        if (targetContainingClass.docOnly) {
            duplicated.docOnly = true
        }

        return duplicated
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is FieldItem && name == other.name() && containingClass == other.containingClass()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = "field ${containingClass.fullName()}.${name()}"

    companion object {
        fun create(codebase: PsiBasedCodebase, containingClass: PsiClassItem, psiField: PsiField): PsiFieldItem {
            val name = psiField.name
            val commentText = javadoc(psiField)
            val modifiers = modifiers(codebase, psiField, commentText)

            val fieldType = codebase.getType(psiField.type)
            val isEnumConstant = psiField is PsiEnumConstant
            val initialValue = null // compute lazily

            val field = PsiFieldItem(
                codebase = codebase,
                psiField = psiField,
                containingClass = containingClass,
                name = name,
                documentation = commentText,
                modifiers = modifiers,
                fieldType = fieldType,
                isEnumConstant = isEnumConstant,
                initialValue = initialValue
            )
            field.modifiers.setOwner(field)
            return field
        }

        fun create(codebase: PsiBasedCodebase, containingClass: PsiClassItem, original: PsiFieldItem): PsiFieldItem {
            val field = PsiFieldItem(
                codebase = codebase,
                psiField = original.psiField,
                containingClass = containingClass,
                name = original.name,
                documentation = original.documentation,
                modifiers = PsiModifierItem.create(codebase, original.modifiers),
                fieldType = PsiTypeItem.create(codebase, original.fieldType),
                isEnumConstant = original.isEnumConstant,
                initialValue = original.initialValue
            )
            field.modifiers.setOwner(field)
            return field
        }
    }
}