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

import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.TypeItem
import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.psi.psiUtil.parameterIndex

class PsiParameterItem(
    override val codebase: PsiBasedCodebase,
    private val psiParameter: PsiParameter,
    private val name: String,
    modifiers: PsiModifierItem,
    documentation: String,
    private val type: PsiTypeItem
) : PsiItem(
    codebase = codebase,
    modifiers = modifiers,
    documentation = documentation,
    element = psiParameter
), ParameterItem {
    lateinit var containingMethod: PsiMethodItem

    override fun name(): String = name
    override fun type(): TypeItem = type
    override fun containingMethod(): MethodItem = containingMethod

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is ParameterItem && name == other.name() && containingMethod == other.containingMethod()
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String = "parameter ${name()}"

    companion object {
        fun create(
            codebase: PsiBasedCodebase,
            psiParameter: PsiParameter
        ): PsiParameterItem {
            val name = psiParameter.name ?: "arg${psiParameter.parameterIndex()}"
            val commentText = "" // no javadocs on individual parameters
            val modifiers = modifiers(codebase, psiParameter, commentText)
            val type = codebase.getType(psiParameter.type)
            val parameter = PsiParameterItem(
                codebase = codebase,
                psiParameter = psiParameter,
                name = name,
                documentation = commentText,
                modifiers = modifiers,
                type = type
            )
            parameter.modifiers.setOwner(parameter)
            return parameter
        }

        fun create(
            codebase: PsiBasedCodebase,
            original: PsiParameterItem
        ): PsiParameterItem {
            val parameter = PsiParameterItem(
                codebase = codebase,
                psiParameter = original.psiParameter,
                name = original.name,
                documentation = original.documentation,
                modifiers = PsiModifierItem.create(codebase, original.modifiers),
                type = PsiTypeItem.create(codebase, original.type)
            )
            parameter.modifiers.setOwner(parameter)
            return parameter
        }

        fun create(
            codebase: PsiBasedCodebase,
            original: List<ParameterItem>
        ): List<PsiParameterItem> {
            return original.map { create(codebase, it as PsiParameterItem) }
        }
    }
}