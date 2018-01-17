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
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.options
import com.intellij.psi.PsiPackage

class PsiPackageItem(
    override val codebase: PsiBasedCodebase,
    private val psiPackage: PsiPackage,
    private val qualifiedName: String,
    modifiers: PsiModifierItem,
    documentation: String
) :
    PsiItem(
        codebase = codebase,
        modifiers = modifiers,
        documentation = documentation,
        element = psiPackage
    ), PackageItem {
    // Note - top level classes only
    private val classes: MutableList<PsiClassItem> = mutableListOf()

    override fun topLevelClasses(): Sequence<ClassItem> = classes.asSequence().filter { it.isTopLevelClass() }

    lateinit var containingPackageField: PsiPackageItem

    override var hidden: Boolean = super.hidden || options.hidePackages.contains(qualifiedName)

    override fun containingPackage(): PackageItem? {
        return if (qualifiedName.isEmpty()) null else {
            if (!::containingPackageField.isInitialized) {
                var parentPackage = qualifiedName
                while (true) {
                    val index = parentPackage.lastIndexOf('.')
                    if (index == -1) {
                        containingPackageField = codebase.findPackage("")!!
                        return containingPackageField
                    }
                    parentPackage = parentPackage.substring(0, index)
                    val pkg = codebase.findPackage(parentPackage)
                    if (pkg != null) {
                        containingPackageField = pkg
                        return pkg
                    }
                }

                @Suppress("UNREACHABLE_CODE")
                null
            } else {
                containingPackageField
            }
        }
    }

    fun addClass(cls: PsiClassItem) {
        if (!cls.isTopLevelClass()) {
            // TODO: Stash in a list somewhere to make allClasses() faster?
            return
        }

        /*
        // Temp debugging:
        val q = cls.qualifiedName()
        for (c in classes) {
            if (q == c.qualifiedName()) {
                assert(false, { "Unexpectedly found class $q already listed in $this" })
                return
            }
        }
        */

        classes.add(cls)
        cls.containingPackage = this
    }

    fun addClasses(classList: List<PsiClassItem>) {
        for (cls in classList) {
            addClass(cls)
        }
    }

    override fun qualifiedName(): String = qualifiedName

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return other is PackageItem && qualifiedName == other.qualifiedName()
    }

    override fun hashCode(): Int = qualifiedName.hashCode()

    override fun toString(): String = "Package $qualifiedName"

    override fun finishInitialization() {
        super.finishInitialization()
        val initialClasses = ArrayList(classes)
        var original = initialClasses.size // classes added after this point will have indices >= original
        for (cls in initialClasses) {
            cls.finishInitialization()
        }

        // Finish initialization of any additional classes that were registered during
        // the above initialization (recursively)
        while (original < classes.size) {
            val added = ArrayList(classes.subList(original, classes.size))
            original = classes.size
            for (cls in added) {
                cls.finishInitialization()
            }
        }
    }

    companion object {
        fun create(codebase: PsiBasedCodebase, psiPackage: PsiPackage, extraDocs: String?): PsiPackageItem {
            val commentText = javadoc(psiPackage) + if (extraDocs != null) "\n$extraDocs" else ""
            val modifiers = modifiers(codebase, psiPackage, commentText)
            if (modifiers.isPackagePrivate()) {
                modifiers.setPublic(true) // packages are always public (if not hidden explicitly with private)
            }
            val qualifiedName = psiPackage.qualifiedName

            val pkg = PsiPackageItem(
                codebase = codebase,
                psiPackage = psiPackage,
                qualifiedName = qualifiedName,
                documentation = commentText,
                modifiers = modifiers
            )
            pkg.modifiers.setOwner(pkg)
            return pkg
        }

        fun create(codebase: PsiBasedCodebase, original: PsiPackageItem): PsiPackageItem {
            val pkg = PsiPackageItem(
                codebase = codebase,
                psiPackage = original.psiPackage,
                qualifiedName = original.qualifiedName,
                documentation = original.documentation,
                modifiers = PsiModifierItem.create(codebase, original.modifiers)
            )
            pkg.modifiers.setOwner(pkg)
            return pkg
        }
    }
}