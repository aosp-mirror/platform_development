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

import com.intellij.psi.JavaDocTokenType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.javadoc.PsiDocTag
import com.intellij.psi.javadoc.PsiDocToken

/*
 * Various utilities for merging comments into existing javadoc sections.
 *
 * TODO: Handle KDoc
 */

/**
 * Merges the given [newText] into the existing documentation block [existingDoc]
 * (which should be a full documentation node, including the surrounding comment
 * start and end tokens.)
 *
 * If the [tagSection] is null, add the comment to the initial text block
 * of the description. Otherwise if it is "@return", add the comment
 * to the return value. Otherwise the [tagSection] is taken to be the
 * parameter name, and the comment added as parameter documentation
 * for the given parameter.
 */
fun mergeDocumentation(
    existingDoc: String,
    psiElement: PsiElement,
    newText: String,
    tagSection: String?,
    append: Boolean
): String {

    if (existingDoc.isBlank()) {
        // There's no existing comment: Create a new one. This is easy.
        val content = when {
            tagSection == "@return" -> "@return $newText"
            tagSection?.startsWith("@") ?: false -> "$tagSection $newText"
            tagSection != null -> "@param $tagSection $newText"
            else -> newText
        }

        // TODO: Handle prefixing "*" on lines, if already done in the document?
        return if (newText.contains('\n')) {
            "/** $content */"
        } else {
            return insertInto("/**\n */", content, 3)
        }
    }

    val doc = trimDocIndent(existingDoc)

    // We'll use the PSI Javadoc support to parse the documentation
    // to help us scan the tokens in the documentation, such that
    // we don't have to search for raw substrings like "@return" which
    // can incorrectly find matches in escaped code snippets etc.
    val factory = JavaPsiFacade.getElementFactory(psiElement.project)
            ?: error("Invalid tool configuration; did not find JavaPsiFacade factory")
    val docComment = factory.createDocCommentFromText(doc)

    if (tagSection == "@return") {
        // Add in return value
        val returnTag = docComment.findTagByName("return")
        if (returnTag == null) {
            // Find last tag
            val lastTag = findLastTag(docComment)
            val offset = if (lastTag != null) {
                findTagEnd(lastTag)
            } else {
                doc.length - 2
            }
            return insertInto(doc, "@return $newText", offset)
        } else {
            // Add text to the existing @return tag
            val offset = if (append)
                findTagEnd(returnTag)
            else
                returnTag.textRange.startOffset + returnTag.name.length + 1
            return insertInto(doc, newText, offset)
        }
    } else if (tagSection != null) {
        val parameter = if (tagSection.startsWith("@"))
            docComment.findTagByName(tagSection.substring(1))
        else
            findParamTag(docComment, tagSection)
        if (parameter == null) {
            // Add new parameter or tag
            // TODO: Decide whether to place it alphabetically or place it by parameter order
            // in the signature. Arguably I should follow the convention already present in the
            // doc, if any
            // For now just appending to the last tag before the return tag (if any).
            // This actually works out well in practice where arguments are generally all documented
            // or all not documented; when none of the arguments are documented these end up appending
            // exactly in the right parameter order!
            val returnTag = docComment.findTagByName("return")
            val anchor = returnTag ?: findLastTag(docComment)
            val offset = when {
                returnTag != null -> returnTag.textRange.startOffset
                anchor != null -> findTagEnd(anchor)
                else -> doc.length - 2  // "*/
            }
            val tagName = if (tagSection.startsWith("@")) tagSection else "@param $tagSection"
            return insertInto(doc, "$tagName $newText", offset)
        } else {
            // Add to existing tag/parameter
            val offset = if (append)
                findTagEnd(parameter)
            else
                parameter.textRange.startOffset + parameter.name.length + 1
            return insertInto(doc, newText, offset)
        }
    } else {
        // Add to the main text section of the comment.
        val firstTag = findFirstTag(docComment)
        val startOffset =
            if (!append) {
                4 // "/** ".length
            } else if (firstTag != null) {
                firstTag.textRange.startOffset
            } else {
                doc.length - 2 // -2: end marker */
            }
        return insertInto(doc, newText, startOffset)
    }
}

fun findParamTag(docComment: PsiDocComment, paramName: String): PsiDocTag? {
    return docComment.findTagsByName("param").firstOrNull { it.valueElement?.text == paramName }
}

fun findFirstTag(docComment: PsiDocComment): PsiDocTag? {
    return docComment.tags.asSequence().minBy { it.textRange.startOffset }
}

fun findLastTag(docComment: PsiDocComment): PsiDocTag? {
    return docComment.tags.asSequence().maxBy { it.textRange.startOffset }
}

fun findTagEnd(tag: PsiDocTag): Int {
    var curr: PsiElement? = tag.nextSibling
    while (curr != null) {
        if (curr is PsiDocToken && curr.tokenType == JavaDocTokenType.DOC_COMMENT_END) {
            return curr.textRange.startOffset
        } else if (curr is PsiDocTag) {
            return curr.textRange.startOffset
        }

        curr = curr.nextSibling
    }

    return tag.textRange.endOffset
}

fun trimDocIndent(existingDoc: String): String {
    val index = existingDoc.indexOf('\n')
    if (index == -1) {
        return existingDoc
    }

    return existingDoc.substring(0, index + 1) +
            existingDoc.substring(index + 1).trimIndent().split('\n').joinToString(separator = "\n") {
                if (!it.startsWith(" ")) {
                    " ${it.trimEnd()}"
                } else {
                    it.trimEnd()
                }
            }
}

fun insertInto(existingDoc: String, newText: String, initialOffset: Int): String {
    // TODO: Insert "." between existing documentation and new documentation, if necessary.

    val offset = if (initialOffset > 4 && existingDoc.regionMatches(initialOffset - 4, "\n * ", 0, 4, false)) {
        initialOffset - 4
    } else {
        initialOffset
    }
    val index = existingDoc.indexOf('\n')
    val prefixWithStar = index == -1 || existingDoc[index + 1] == '*' ||
            existingDoc[index + 1] == ' ' && existingDoc[index + 2] == '*'

    val prefix = existingDoc.substring(0, offset)
    val suffix = existingDoc.substring(offset)
    val startSeparator = "\n"
    val endSeparator =
        if (suffix.startsWith("\n") || suffix.startsWith(" \n")) "" else if (suffix == "*/") "\n" else if (prefixWithStar) "\n * " else "\n"

    val middle = if (prefixWithStar) {
        startSeparator + newText.split('\n').joinToString(separator = "\n") { " * $it" } +
                endSeparator
    } else {
        "$startSeparator$newText$endSeparator"
    }

    // Going from single-line to multi-line?
    return if (existingDoc.indexOf('\n') == -1 && existingDoc.startsWith("/** ")) {
        prefix.substring(0, 3) + "\n *" + prefix.substring(3) + middle +
                if (suffix == "*/") " */" else suffix
    } else {
        prefix + middle + suffix
    }
}
