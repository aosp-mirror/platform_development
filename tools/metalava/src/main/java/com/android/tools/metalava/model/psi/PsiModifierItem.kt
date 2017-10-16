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

import com.android.tools.metalava.model.AnnotationItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.ModifierList
import com.android.tools.metalava.model.MutableModifierList
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiModifierListOwner

class PsiModifierItem(
    override val codebase: Codebase,
    var flags: Int = 0,
    private var annotations: MutableList<AnnotationItem>? = null
) : ModifierList, MutableModifierList {
    private lateinit var owner: Item

    private operator fun set(mask: Int, set: Boolean) {
        flags = if (set) {
            flags or mask
        } else {
            flags and mask.inv()
        }
    }

    private fun isSet(mask: Int): Boolean {
        return flags and mask != 0
    }

    override fun annotations(): List<AnnotationItem> {
        return annotations ?: emptyList()
    }

    override fun owner(): Item {
        return owner
    }

    fun setOwner(owner: Item) {
        this.owner = owner
    }

    override fun isPublic(): Boolean {
        return isSet(PUBLIC)
    }

    override fun isProtected(): Boolean {
        return isSet(PROTECTED)
    }

    override fun isPrivate(): Boolean {
        return isSet(PRIVATE)
    }

    override fun isStatic(): Boolean {
        return isSet(STATIC)
    }

    override fun isAbstract(): Boolean {
        return isSet(ABSTRACT)
    }

    override fun isFinal(): Boolean {
        return isSet(FINAL)
    }

    override fun isNative(): Boolean {
        return isSet(NATIVE)
    }

    override fun isSynchronized(): Boolean {
        return isSet(SYNCHRONIZED)
    }

    override fun isStrictFp(): Boolean {
        return isSet(STRICT_FP)
    }

    override fun isTransient(): Boolean {
        return isSet(TRANSIENT)
    }

    override fun isVolatile(): Boolean {
        return isSet(VOLATILE)
    }

    override fun isDefault(): Boolean {
        return isSet(DEFAULT)
    }

    fun isDeprecated(): Boolean {
        return isSet(DEPRECATED)
    }

    override fun setPublic(public: Boolean) {
        set(PUBLIC, public)
    }

    override fun setProtected(protected: Boolean) {
        set(PROTECTED, protected)
    }

    override fun setPrivate(private: Boolean) {
        set(PRIVATE, private)
    }

    override fun setStatic(static: Boolean) {
        set(STATIC, static)
    }

    override fun setAbstract(abstract: Boolean) {
        set(ABSTRACT, abstract)
    }

    override fun setFinal(final: Boolean) {
        set(FINAL, final)
    }

    override fun setNative(native: Boolean) {
        set(NATIVE, native)
    }

    override fun setSynchronized(synchronized: Boolean) {
        set(SYNCHRONIZED, synchronized)
    }

    override fun setStrictFp(strictfp: Boolean) {
        set(STRICT_FP, strictfp)
    }

    override fun setTransient(transient: Boolean) {
        set(TRANSIENT, transient)
    }

    override fun setVolatile(volatile: Boolean) {
        set(VOLATILE, volatile)
    }

    override fun setDefault(default: Boolean) {
        set(DEFAULT, default)
    }

    fun setDeprecated(deprecated: Boolean) {
        set(DEPRECATED, deprecated)
    }

    override fun addAnnotation(annotation: AnnotationItem) {
        if (annotations == null) {
            annotations = mutableListOf()
        }
        annotations?.add(annotation)
    }

    override fun removeAnnotation(annotation: AnnotationItem) {
        annotations?.remove(annotation)
    }

    override fun clearAnnotations(annotation: AnnotationItem) {
        annotations?.clear()
    }

    override fun isEmpty(): Boolean {
        return flags and DEPRECATED.inv() == 0 // deprecated isn't a real modifier
    }

    override fun isPackagePrivate(): Boolean {
        return flags and (PUBLIC or PROTECTED or PRIVATE) == 0
    }

    fun getAccessFlags(): Int {
        return flags and (PUBLIC or PROTECTED or PRIVATE)
    }

    // Rename? It's not a full equality, it's whether an override's modifier set is significant
    override fun equivalentTo(other: ModifierList): Boolean {
        if (other is PsiModifierItem) {
            val flags2 = other.flags
            val mask = EQUIVALENCE_MASK

            // Skipping the "default" flag
            // TODO: Compatibility: skipnative modiifier and skipstrictfp modifier flags!
            //if (!compatibility.skipNativeModifier && isNative() != other.isNative()) return false
            //if (!compatibility.skipStrictFpModifier && isStrictFp() != other.isStrictFp()) return false
            return flags and mask == flags2 and mask
        }
        return false
    }

    companion object {
        const val PUBLIC = 1 shl 0
        const val PROTECTED = 1 shl 1
        const val PRIVATE = 1 shl 2
        const val STATIC = 1 shl 3
        const val ABSTRACT = 1 shl 4
        const val FINAL = 1 shl 5
        const val NATIVE = 1 shl 6
        const val SYNCHRONIZED = 1 shl 7
        const val STRICT_FP = 1 shl 8
        const val TRANSIENT = 1 shl 9
        const val VOLATILE = 1 shl 10
        const val DEFAULT = 1 shl 11
        const val DEPRECATED = 1 shl 12

        private const val EQUIVALENCE_MASK = PUBLIC or PROTECTED or PRIVATE or STATIC or ABSTRACT or
                FINAL or TRANSIENT or VOLATILE or SYNCHRONIZED or DEPRECATED

        fun create(codebase: PsiBasedCodebase, element: PsiModifierListOwner, documentation: String?): PsiModifierItem {
            val modifiers = create(
                codebase,
                element.modifierList
            )

            if (documentation?.contains("@deprecated") == true ||
                // Check for @Deprecated annotation
                ((element as? PsiDocCommentOwner)?.isDeprecated == true)) {
                modifiers.setDeprecated(true)
            }

            return modifiers
        }

        fun create(codebase: PsiBasedCodebase, modifierList: PsiModifierList?): PsiModifierItem {
            modifierList ?: return PsiModifierItem(codebase)

            var flags = 0
            if (modifierList.hasModifierProperty(PsiModifier.PUBLIC)) {
                flags = flags or PUBLIC
            }
            if (modifierList.hasModifierProperty(PsiModifier.PROTECTED)) {
                flags = flags or PROTECTED
            }
            if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                flags = flags or PRIVATE
            }
            if (modifierList.hasModifierProperty(PsiModifier.STATIC)) {
                flags = flags or STATIC
            }
            if (modifierList.hasModifierProperty(PsiModifier.ABSTRACT)) {
                flags = flags or ABSTRACT
            }
            if (modifierList.hasModifierProperty(PsiModifier.FINAL)) {
                flags = flags or FINAL
            }
            if (modifierList.hasModifierProperty(PsiModifier.NATIVE)) {
                flags = flags or NATIVE
            }
            if (modifierList.hasModifierProperty(PsiModifier.SYNCHRONIZED)) {
                flags = flags or SYNCHRONIZED
            }
            if (modifierList.hasModifierProperty(PsiModifier.STRICTFP)) {
                flags = flags or STRICT_FP
            }
            if (modifierList.hasModifierProperty(PsiModifier.TRANSIENT)) {
                flags = flags or TRANSIENT
            }
            if (modifierList.hasModifierProperty(PsiModifier.VOLATILE)) {
                flags = flags or VOLATILE
            }
            if (modifierList.hasModifierProperty(PsiModifier.DEFAULT)) {
                flags = flags or DEFAULT
            }

            val psiAnnotations = modifierList.annotations
            return if (psiAnnotations.isEmpty()) {
                PsiModifierItem(codebase, flags)
            } else {
                val annotations: MutableList<AnnotationItem> =
                    psiAnnotations.map { PsiAnnotationItem.create(codebase, it) }.toMutableList()
                PsiModifierItem(codebase, flags, annotations)
            }
        }

        fun create(codebase: PsiBasedCodebase, original: PsiModifierItem): PsiModifierItem {
            val originalAnnotations = original.annotations ?: return PsiModifierItem(codebase, original.flags)
            val copy: MutableList<AnnotationItem> = ArrayList(originalAnnotations.size)
            originalAnnotations.mapTo(copy) { PsiAnnotationItem.create(codebase, it as PsiAnnotationItem) }
            return PsiModifierItem(codebase, original.flags, copy)
        }
    }
}
