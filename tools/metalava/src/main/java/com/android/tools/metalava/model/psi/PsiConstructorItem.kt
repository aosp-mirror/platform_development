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

import com.android.tools.metalava.model.ConstructorItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiWhiteSpace
import java.util.function.Predicate

class PsiConstructorItem(
    codebase: PsiBasedCodebase,
    psiMethod: PsiMethod,
    containingClass: PsiClassItem,
    name: String,
    modifiers: PsiModifierItem,
    documentation: String,
    parameters: List<PsiParameterItem>,
    returnType: PsiTypeItem,
    private val implicitConstructor: Boolean = false
) :
    PsiMethodItem(
        codebase = codebase,
        modifiers = modifiers,
        documentation = documentation,
        psiMethod = psiMethod,
        containingClass = containingClass,
        name = name,
        returnType = returnType,
        parameters = parameters
    ), ConstructorItem {

    init {
        if (implicitConstructor) {
            setThrowsTypes(emptyList())
        }
    }

    override fun isImplicitConstructor(): Boolean = implicitConstructor
    override fun isConstructor(): Boolean = true
    override var superConstructor: ConstructorItem? = null

    private var _superMethods: List<MethodItem>? = null
    override fun superMethods(): List<MethodItem> {
        if (_superMethods == null) {
            val result = mutableListOf<MethodItem>()
            psiMethod.findSuperMethods().mapTo(result) { codebase.findMethod(it) }

            if (result.isEmpty() && isConstructor() && containingClass().superClass() != null) {
                // Try a little harder; psi findSuperMethod doesn't seem to find super constructors in
                // some cases, but maybe we can find it by resolving actual super() calls!
                // TODO: Port to UAST
                var curr: PsiElement? = psiMethod.body?.firstBodyElement
                while (curr != null && curr is PsiWhiteSpace) {
                    curr = curr.nextSibling
                }
                if (curr is PsiExpressionStatement && curr.expression is PsiMethodCallExpression &&
                    curr.expression.firstChild?.lastChild is PsiKeyword &&
                    curr.expression.firstChild?.lastChild?.text == "super"
                ) {
                    val resolved = (curr.expression as PsiMethodCallExpression).resolveMethod()
                    if (resolved is PsiMethod) {
                        val superConstructor = codebase.findMethod(resolved)
                        result.add(superConstructor)
                    }
                }
            }
            _superMethods = result
        }

        return _superMethods!!
    }

    fun findDelegate(predicate: Predicate<Item>, allowInexactMatch: Boolean = true): PsiConstructorItem? {
        if (isImplicitConstructor()) {
            // Delegate to parent implicit constructors
            (containingClass().superClass() as? PsiClassItem)?.constructors()?.forEach {
                if (it.implicitConstructor) {
                    if (predicate.test(it)) {
                        return it
                    } else {
                        return it.findDelegate(predicate, allowInexactMatch)
                    }
                }
            }
        }

        val superPsiMethod = PsiConstructorItem.findSuperOrThis(psiMethod)
        if (superPsiMethod != null) {
            val superMethod = codebase.findMethod(superPsiMethod) as PsiConstructorItem
            if (!predicate.test(superMethod)) {
                return superMethod.findDelegate(predicate, allowInexactMatch)
            }
            return superMethod
        }

        // Try to pick an alternative - for example adding package private bridging
        // methods if the super class is in the same package
        val constructors = (containingClass().superClass() as? PsiClassItem)?.constructors()
        constructors?.forEach { constructor ->
            if (predicate.test(constructor)) {
                return constructor
            }
            val superMethod = constructor.findDelegate(predicate, allowInexactMatch)
            if (superMethod != null) {
                return superMethod
            }
        }

        return null
    }

    companion object {
        fun create(
            codebase: PsiBasedCodebase, containingClass: PsiClassItem,
            psiMethod: PsiMethod
        ): PsiConstructorItem {
            assert(psiMethod.isConstructor)
            val name = psiMethod.name
            val commentText = javadoc(psiMethod)
            val modifiers = modifiers(codebase, psiMethod, commentText)
            val parameters = psiMethod.parameterList.parameters.mapIndexed { index, parameter ->
                PsiParameterItem.create(codebase, parameter, index)
            }

            val constructor = PsiConstructorItem(
                codebase = codebase,
                psiMethod = psiMethod,
                containingClass = containingClass,
                name = name,
                documentation = commentText,
                modifiers = modifiers,
                parameters = parameters,
                returnType = codebase.getType(containingClass.psiClass),
                implicitConstructor = false
            )
            constructor.modifiers.setOwner(constructor)
            return constructor
        }

        fun createDefaultConstructor(
            codebase: PsiBasedCodebase,
            containingClass: PsiClassItem,
            psiClass: PsiClass
        ): PsiConstructorItem {
            val name = psiClass.name!!

            val factory = JavaPsiFacade.getInstance(psiClass.project).elementFactory
            val psiMethod = factory.createConstructor(name, psiClass)
            val flags = containingClass.modifiers.getAccessFlags()
            val modifiers = PsiModifierItem(codebase, flags, null)

            val item = PsiConstructorItem(
                codebase = codebase,
                psiMethod = psiMethod,
                containingClass = containingClass,
                name = name,
                documentation = "",
                modifiers = modifiers,
                parameters = emptyList(),
                returnType = codebase.getType(psiClass),
                implicitConstructor = true
            )
            modifiers.setOwner(item)
            return item
        }

        fun create(
            codebase: PsiBasedCodebase,
            containingClass: PsiClassItem,
            original: PsiConstructorItem
        ): PsiConstructorItem {
            val constructor = PsiConstructorItem(
                codebase = codebase,
                psiMethod = original.psiMethod,
                containingClass = containingClass,
                name = original.name(),
                documentation = original.documentation,
                modifiers = PsiModifierItem.create(codebase, original.modifiers),
                parameters = PsiParameterItem.create(codebase, original.parameters()),
                returnType = codebase.getType(containingClass.psiClass),
                implicitConstructor = original.implicitConstructor
            )

            constructor.modifiers.setOwner(constructor)
            constructor.source = original

            return constructor
        }

        internal fun findSuperOrThis(psiMethod: PsiMethod): PsiMethod? {
            val superMethods = psiMethod.findSuperMethods()
            if (superMethods.isNotEmpty()) {
                return superMethods[0]
            }

// WARNING: I've deleted private constructors from class model; may not be right for here!

            // TODO: Port to UAST
            var curr: PsiElement? = psiMethod.body?.firstBodyElement
            while (curr != null && curr is PsiWhiteSpace) {
                curr = curr.nextSibling
            }
            if (curr is PsiExpressionStatement && curr.expression is PsiMethodCallExpression) {
                val call = curr.expression as PsiMethodCallExpression
                if (call.firstChild?.lastChild is PsiKeyword) {
                    val keyword = call.firstChild?.lastChild
                    // TODO: Check Kotlin!
                    if (keyword?.text == "super" || keyword?.text == "this") {
                        val resolved = call.resolveMethod()
                        if (resolved is PsiMethod) {
                            return resolved
                        }
                    }
                }
            }

            // TODO: Try to find a super call *anywhere* in the method

            // See if we have an implicit constructor in the parent that we can call
//            psiMethod.containingClass?.constructors?.forEach {
//                // PsiUtil.hasDefaultConstructor(psiClass) if (it.impl)
//            }

            return null
        }
    }
}