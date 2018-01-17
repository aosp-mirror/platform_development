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

import com.android.tools.metalava.compatibility
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.ModifierList
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.TypeItem
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.TypeConversionUtil
import org.intellij.lang.annotations.Language
import java.io.StringWriter

open class PsiMethodItem(
    override val codebase: PsiBasedCodebase,
    val psiMethod: PsiMethod,
    private val containingClass: PsiClassItem,
    private val name: String,
    modifiers: PsiModifierItem,
    documentation: String,
    private val returnType: PsiTypeItem,
    private val parameters: List<PsiParameterItem>
) :
    PsiItem(
        codebase = codebase,
        modifiers = modifiers,
        documentation = documentation,
        element = psiMethod
    ), MethodItem {

    init {
        for (parameter in parameters) {
            @Suppress("LeakingThis")
            parameter.containingMethod = this
        }
    }

    /**
     * If this item was created by filtering down a different codebase, this temporarily
     * points to the original item during construction. This is used to let us initialize
     * for example throws lists later, when all classes in the codebase have been
     * initialized.
     */
    internal var source: PsiMethodItem? = null

    override var inheritedInterfaceMethod: Boolean = false

    override fun name(): String = name
    override fun containingClass(): PsiClassItem = containingClass

    override fun equals(other: Any?): Boolean {
        // TODO: Allow mix and matching with other MethodItems?
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PsiMethodItem

        if (psiMethod != other.psiMethod) return false

        return true
    }

    override fun hashCode(): Int {
        return psiMethod.hashCode()
    }

    override fun isConstructor(): Boolean = false

    override fun isImplicitConstructor(): Boolean = false

    override fun returnType(): TypeItem? = returnType

    override fun parameters(): List<ParameterItem> = parameters

    private var superMethods: List<MethodItem>? = null
    override fun superMethods(): List<MethodItem> {
        if (superMethods == null) {
            val result = mutableListOf<MethodItem>()
            psiMethod.findSuperMethods().mapTo(result) { codebase.findMethod(it) }
            superMethods = result
        }

        return superMethods!!
    }

    fun setSuperMethods(superMethods: List<MethodItem>) {
        this.superMethods = superMethods
    }

    override fun typeParameterList(): String? {
        return PsiTypeItem.typeParameterList(psiMethod.typeParameterList)
    }

    override fun typeArgumentClasses(): List<ClassItem> {
        return PsiTypeItem.typeParameterClasses(codebase, psiMethod.typeParameterList)
    }

    //    private var throwsTypes: List<ClassItem>? = null
    private lateinit var throwsTypes: List<ClassItem>

    fun setThrowsTypes(throwsTypes: List<ClassItem>) {
        this.throwsTypes = throwsTypes
    }

    override fun throwsTypes(): List<ClassItem> = throwsTypes

    override fun duplicate(targetContainingClass: ClassItem): PsiMethodItem {
        val duplicated = create(codebase, targetContainingClass as PsiClassItem, psiMethod)

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

        duplicated.throwsTypes = throwsTypes
        return duplicated
    }

    /* Call corresponding PSI utility method -- if I can find it!
    override fun matches(other: MethodItem): Boolean {
        if (other !is PsiMethodItem) {
            return super.matches(other)
        }

        // TODO: Find better API: this also checks surrounding class which we don't want!
        return psiMethod.isEquivalentTo(other.psiMethod)
    }
    */

    @Language("JAVA")
    fun toStub(replacementMap: Map<String, String> = emptyMap()): String {
        val method = this
        // There are type variables; we have to recreate the method signature
        val sb = StringBuilder(100)

        val modifierString = StringWriter()
        ModifierList.write(
            modifierString, method.modifiers, method, removeAbstract = false,
            removeFinal = false, addPublic = true
        )
        sb.append(modifierString.toString())

        val typeParameters = typeParameterList()
        if (typeParameters != null) {
            sb.append(' ')
            sb.append(TypeItem.convertTypeString(typeParameters, replacementMap))
        }

        val returnType = method.returnType()
        sb.append(returnType?.convertTypeString(replacementMap))

        sb.append(' ')
        sb.append(method.name())

        sb.append("(")
        method.parameters().asSequence().forEachIndexed { i, parameter ->
            if (i > 0) {
                sb.append(", ")
            }

            sb.append(parameter.type().convertTypeString(replacementMap))
            sb.append(' ')
            sb.append(parameter.name())
        }
        sb.append(")")

        val throws = method.throwsTypes().asSequence().sortedWith(ClassItem.fullNameComparator)
        if (throws.any()) {
            sb.append(" throws ")
            throws.asSequence().sortedWith(ClassItem.fullNameComparator).forEachIndexed { i, type ->
                if (i > 0) {
                    sb.append(", ")
                }
                // No need to replace variables; we can't have type arguments for exceptions
                sb.append(type.qualifiedName())
            }
        }

        sb.append(" { return ")
        val defaultValue = PsiTypesUtil.getDefaultValueOfType(method.psiMethod.returnType)
        sb.append(defaultValue)
        sb.append("; }")

        return sb.toString()
    }

    override fun finishInitialization() {
        super.finishInitialization()

        throwsTypes = throwsTypes(codebase, psiMethod)
    }

    companion object {
        fun create(
            codebase: PsiBasedCodebase,
            containingClass: PsiClassItem,
            psiMethod: PsiMethod
        ): PsiMethodItem {
            assert(!psiMethod.isConstructor)
            val name = psiMethod.name
            val commentText = javadoc(psiMethod)
            val modifiers = modifiers(codebase, psiMethod, commentText)
            val parameters = psiMethod.parameterList.parameters.map { PsiParameterItem.create(codebase, it) }
            val returnType = codebase.getType(psiMethod.returnType!!)
            val method = PsiMethodItem(
                codebase = codebase,
                psiMethod = psiMethod,
                containingClass = containingClass,
                name = name,
                documentation = commentText,
                modifiers = modifiers,
                returnType = returnType,
                parameters = parameters
            )
            method.modifiers.setOwner(method)
            return method
        }

        fun create(
            codebase: PsiBasedCodebase,
            containingClass: PsiClassItem,
            original: PsiMethodItem
        ): PsiMethodItem {
            val method = PsiMethodItem(
                codebase = codebase,
                psiMethod = original.psiMethod,
                containingClass = containingClass,
                name = original.name(),
                documentation = original.documentation,
                modifiers = PsiModifierItem.create(codebase, original.modifiers),
                returnType = PsiTypeItem.create(codebase, original.returnType),
                parameters = PsiParameterItem.create(codebase, original.parameters())
            )
            method.modifiers.setOwner(method)
            method.source = original
            method.inheritedInterfaceMethod = original.inheritedInterfaceMethod

            return method
        }

        private fun throwsTypes(codebase: PsiBasedCodebase, psiMethod: PsiMethod): List<ClassItem> {
            val interfaces = psiMethod.throwsList.referencedTypes
            if (interfaces.isEmpty()) {
                return emptyList()
            }

            val result = ArrayList<ClassItem>(interfaces.size)
            for (cls in interfaces) {
                if (compatibility.useErasureInThrows) {
                    val erased = TypeConversionUtil.erasure(cls)
                    result.add(codebase.findClass(erased) ?: continue)
                    continue
                }

                result.add(codebase.findClass(cls) ?: continue)
            }

            // We're sorting the names here even though outputs typically do their own sorting,
            // since for example the MethodItem.sameSignature check wants to do an element-by-element
            // comparison to see if the signature matches, and that should match overrides even if
            // they specify their elements in different orders.
            result.sortWith(ClassItem.fullNameComparator)
            return result
        }
    }

    override fun toString(): String = "${if (isConstructor()) "constructor" else "method"} ${
    containingClass.qualifiedName()}.${name()}(${parameters().joinToString { it.type().toSimpleType() }})"
}