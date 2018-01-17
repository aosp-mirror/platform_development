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

import com.android.tools.metalava.model.DefaultItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MutableModifierList
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.ParameterItem
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.javadoc.PsiDocTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.uast.UElement
import org.jetbrains.uast.sourcePsiElement

abstract class PsiItem(
    override val codebase: PsiBasedCodebase,
    val element: PsiElement,
    override val modifiers: PsiModifierItem,
    override var documentation: String
) : DefaultItem() {

    override val deprecated: Boolean get() = modifiers.isDeprecated()

    @Suppress("LeakingThis") // Documentation can change, but we don't want to pick up subsequent @docOnly mutations
    override var docOnly = documentation.contains("@doconly")
    @Suppress("LeakingThis")
    override var removed = documentation.contains("@removed")
    @Suppress("LeakingThis")
    override var hidden = (documentation.contains("@hide") || documentation.contains("@pending")
            || modifiers.hasHideAnnotations()) && !modifiers.hasShowAnnotation()

    override fun psi(): PsiElement? = element

    // TODO: Consider only doing this in tests!
    override fun isFromClassPath(): Boolean {
        return if (element is UElement) {
            element.psi is PsiCompiledElement
        } else {
            element is PsiCompiledElement
        }
    }

    /** Get a mutable version of modifiers for this item */
    override fun mutableModifiers(): MutableModifierList = modifiers

    override fun findTagDocumentation(tag: String): String? {
        if (element is PsiCompiledElement) {
            return null
        }
        if (documentation.isBlank()) {
            return null
        }

        // We can't just use element.docComment here because we may have modified
        // the comment and then the comment snapshot in PSI isn't up to date with our
        // latest changes
        val docComment = codebase.getComment(documentation)
        val docTag = docComment.findTagByName(tag) ?: return null
        val text = docTag.text

        // Trim trailing next line (javadoc *)
        var index = text.length - 1
        while (index > 0) {
            val c = text[index]
            if (!(c == '*' || c.isWhitespace())) {
                break
            }
            index--
        }
        index++
        return if (index < text.length) {
            text.substring(0, index)
        } else {
            text
        }
    }

    override fun appendDocumentation(comment: String, tagSection: String?, append: Boolean) {
        if (comment.isBlank()) {
            return
        }

        // TODO: Figure out if an annotation should go on the return value, or on the method.
        // For example; threading: on the method, range: on the return value.
        // TODO: Find a good way to add or append to a given tag (@param <something>, @return, etc)

        if (this is ParameterItem) {
            // For parameters, the documentation goes into the surrounding method's documentation!
            // Find the right parameter location!
            val parameterName = name()
            val target = containingMethod()
            target.appendDocumentation(comment, parameterName)
            return
        }

        documentation = mergeDocumentation(documentation, element, comment.trim(), tagSection, append)
    }

    private fun packageName(): String? {
        var curr: Item? = this
        while (curr != null) {
            if (curr is PackageItem) {
                return curr.qualifiedName()
            }
            curr = curr.parent()
        }

        return null
    }

    override fun fullyQualifiedDocumentation(): String {
        if (documentation.isBlank()) {
            return documentation
        }

        if (!(documentation.contains("@link") || // includes @linkplain
                    documentation.contains("@see") ||
                    documentation.contains("@throws"))) {
            // No relevant tags that need to be expanded/rewritten
            return documentation
        }

        val comment = codebase.getComment(documentation, psi())

        val sb = StringBuilder(documentation.length)
        var curr = comment.firstChild
        while (curr != null) {
            if (curr is PsiDocTag) {
                sb.append(getExpanded(curr))
            } else {
                sb.append(curr.text)
            }
            curr = curr.nextSibling
        }

        return sb.toString()
    }

    private fun getExpanded(tag: PsiDocTag): String {
        val text = tag.text
        val reference = extractReference(tag)
        var resolved = reference?.resolve()
        var referenceText = reference?.element?.text
        if (resolved == null && tag.name == "throws") {
            // Workaround: @throws does not provide a valid reference to the class
            val dataElements = tag.dataElements
            if (dataElements.isNotEmpty()) {
                val exceptionName = dataElements[0].text
                val exceptionReference = codebase.createReferenceFromText(exceptionName, psi())
                resolved = exceptionReference.resolve()
                referenceText = exceptionName
            } else {
                return text
            }
        }

        if (resolved != null && referenceText != null) {

            when (resolved) {
            // TODO: If same package, do nothing
            // TODO: If not absolute, preserve syntax
                is PsiClass -> {
                    if (samePackage(resolved)) {
                        return text
                    }
                    val qualifiedName = resolved.qualifiedName ?: return text
                    if (referenceText == qualifiedName) {
                        // Already absolute
                        return text
                    }
                    val valueElement = tag.valueElement
                    return when {
                        valueElement != null -> {
                            val start = valueElement.startOffsetInParent
                            val end = start + valueElement.textLength
                            text.substring(0, start) + qualifiedName + text.substring(end)
                        }
                        tag.name == "see" -> {
                            val suffix = text.substring(text.indexOf(referenceText) + referenceText.length)
                            "@see $qualifiedName$suffix"
                        }
                        text.startsWith("{") -> "{@${tag.name} $qualifiedName $referenceText}"
                        else -> "@${tag.name} $qualifiedName $referenceText"
                    }
                }
                is PsiMember -> {
                    val containing = resolved.containingClass ?: return text
                    if (samePackage(containing)) {
                        return text
                    }
                    val qualifiedName = containing.qualifiedName ?: return text
                    if (referenceText.startsWith(qualifiedName)) {
                        // Already absolute
                        return text
                    }

                    val name = containing.name ?: return text
                    val valueElement = tag.valueElement
                    if (valueElement != null) {
                        val start = valueElement.startOffsetInParent
                        val close = text.lastIndexOf('}')
                        if (close == -1) {
                            return text // invalid javadoc
                        }
                        val memberPart = text.substring(text.indexOf(name, start) + name.length, close)
                        return "${text.substring(0, start)}$qualifiedName$memberPart $referenceText}"
                    }
                }
            }
        }

        return text
    }

    private fun samePackage(cls: PsiClass): Boolean {
        val pkg = packageName() ?: return false
        return cls.qualifiedName == "$pkg.${cls.name}"
    }

    // Copied from UnnecessaryJavaDocLinkInspection
    private fun extractReference(tag: PsiDocTag): PsiReference? {
        val valueElement = tag.valueElement
        if (valueElement != null) {
            return valueElement.reference
        }
        // hack around the fact that a reference to a class is apparently
        // not a PsiDocTagValue
        val dataElements = tag.dataElements
        if (dataElements.isEmpty()) {
            return null
        }
        val salientElement: PsiElement = dataElements.firstOrNull { it !is PsiWhiteSpace } ?: return null
        val child = salientElement.firstChild
        return if (child !is PsiReference) null else child
    }

    /** Finish initialization of the item */
    open fun finishInitialization() {
        modifiers.setOwner(this)
    }

    companion object {
        fun javadoc(element: PsiElement): String {
            if (element is PsiCompiledElement) {
                return ""
            }

            if (element is UElement) {
                val comments = element.comments
                if (comments.isNotEmpty()) {
                    val sb = StringBuilder()
                    comments.asSequence().joinTo(buffer = sb, separator = "\n")
                    return sb.toString()
                } else {
                    // Temporary workaround: UAST seems to not return document nodes
                    // https://youtrack.jetbrains.com/issue/KT-22135
                    val first = element.sourcePsiElement?.firstChild
                    if (first is KDoc) {
                        return first.text
                    }

                }
            }

            if (element is PsiDocCommentOwner) {
                return element.docComment?.text ?: ""
            }

            return ""
        }

        fun modifiers(codebase: PsiBasedCodebase, element: PsiModifierListOwner, documentation: String): PsiModifierItem {
            return PsiModifierItem.create(codebase, element, documentation)
        }
    }
}
