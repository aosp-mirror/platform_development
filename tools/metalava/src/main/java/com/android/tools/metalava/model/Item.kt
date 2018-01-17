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
import com.intellij.psi.PsiElement

/**
 * Represents a code element such as a package, a class, a method, a field, a parameter.
 *
 * This extra abstraction on top of PSI allows us to more model the API (and customize
 * visibility, which cannot always be done by looking at a particular piece of code and examining
 * visibility and @hide/@removed annotations: sometimes package private APIs are unhidden by
 * being used in public APIs for example.
 *
 * The abstraction also lets us back the model by an alternative implementation read from
 * signature files, to do compatibility checks.
 * */
interface Item {
    val codebase: Codebase

    /** Return the modifiers of this class */
    val modifiers: ModifierList

    /**
     * Whether this element should be part of the API. The algorithm for this is complicated, so it can't
     * be computed initially; we'll make passes over the source code to determine eligibility and mark all
     * items as included or not.
     */
    var included: Boolean

    /** Whether this element has been hidden with @hide/@Hide (or after propagation, in some containing class/pkg) */
    var hidden: Boolean

    var emit: Boolean

    fun parent(): Item?

    /** Recursive check to see if this item or any of its parents (containing class, containing package) are hidden */
    fun hidden(): Boolean {
        return hidden || parent()?.hidden() ?: false
    }

    /** Whether this element has been removed with @removed/@Remove (or after propagation, in some containing class) */
    var removed: Boolean

    /** True if this element has been marked deprecated */
    val deprecated: Boolean

    /** True if this element is only intended for documentation */
    var docOnly: Boolean

    /** True if this item is either hidden or removed */
    fun isHiddenOrRemoved(): Boolean = hidden || removed

    /** Visits this element using the given [visitor] */
    fun accept(visitor: ItemVisitor)

    /** Visits all types in this item hierarchy */
    fun acceptTypes(visitor: TypeVisitor)

    /** Get a mutable version of modifiers for this item */
    fun mutableModifiers(): MutableModifierList

    /** The javadoc/KDoc comment for this code element, if any. This is
     * the original content of the documentation, including lexical tokens
     * to begin, continue and end the comment (such as /+*).
     * See [fullyQualifiedDocumentation] to look up the documentation with
     * fully qualified references to classes.
     */
    var documentation: String

    /** Looks up docs for a specific tag */
    fun findTagDocumentation(tag: String): String?

    /**
     * A rank used for sorting. This allows signature files etc to
     * sort similar items by a natural order, if non-zero.
     * (Even though in signature files the elements are normally
     * sorted first logically (constructors, then methods, then fields)
     * and then alphabetically, this lets us preserve the source
     * ordering for example for overloaded methods of the same name,
     * where it's not clear that an alphabetical order (of each
     * parameter?) would be preferable.)
     */
    val sortingRank: Int

    /**
     * Add the given text to the documentation.
     *
     * If the [tagSection] is null, add the comment to the initial text block
     * of the description. Otherwise if it is "@return", add the comment
     * to the return value. Otherwise the [tagSection] is taken to be the
     * parameter name, and the comment added as parameter documentation
     * for the given parameter.
     */
    fun appendDocumentation(comment: String, tagSection: String? = null, append: Boolean = true)

    val isPublic: Boolean
    val isProtected: Boolean
    val isPackagePrivate: Boolean
    val isPrivate: Boolean

    // make sure these are implemented so we can place in maps:
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    /**
     * Returns true if this item requires nullness information (e.g. for a method
     * where either the return value or any of the parameters are non-primitives.
     * Note that it doesn't consider whether it already has nullness annotations;
     * for that see [hasNullnessInfo].
     */
    fun requiresNullnessInfo(): Boolean {
        return false
    }

    /**
     * Whether this item was loaded from the classpath (e.g. jar dependencies)
     * rather than be declared as source
     */
    fun isFromClassPath(): Boolean = false

    /**
     * Returns true if this item requires nullness information and supplies it
     * (for all items, e.g. if a method is partially annotated this method would
     * still return false)
     */
    fun hasNullnessInfo(): Boolean {
        when (this) {
            is ParameterItem -> {
                return !type().primitive
            }

            is MethodItem -> {
                val returnType = returnType()
                if (returnType != null && !returnType.primitive) {
                    return true
                }
                for (parameter in parameters()) {
                    if (!parameter.type().primitive) {
                        return true
                    }
                }
                return false
            }
        }

        return false
    }

    fun hasShowAnnotation(): Boolean = modifiers.hasShowAnnotation()
    fun hasHideAnnotation(): Boolean = modifiers.hasHideAnnotations()

    // TODO: Cache?
    fun checkLevel(): Boolean {
        if (isHiddenOrRemoved()) {
            return false
        }
        return modifiers.isPublic() || modifiers.isProtected()
    }

    fun compilationUnit(): CompilationUnit? {
        var curr: Item? = this
        while (curr != null) {
            if (curr is ClassItem && curr.isTopLevelClass()) {
                return curr.getCompilationUnit()
            }
            curr = curr.parent()
        }

        return null
    }

    /** Returns the PSI element for this item, if any */
    fun psi(): PsiElement? = null

    /** Tag field used for DFS etc */
    var tag: Boolean

    /**
     * Returns the [documentation], but with fully qualified links (except for the same package, and
     * when turning a relative reference into a fully qualified reference, use the javadoc syntax
     * for continuing to display the relative text, e.g. instead of {@link java.util.List}, use
     * {@link java.util.List List}.
     */
    fun fullyQualifiedDocumentation(): String = documentation
}

abstract class DefaultItem(override val sortingRank: Int = nextRank++) : Item {
    override val isPublic: Boolean get() = modifiers.isPublic()
    override val isProtected: Boolean get() = modifiers.isProtected()
    override val isPackagePrivate: Boolean get() = modifiers.isPackagePrivate()
    override val isPrivate: Boolean get() = modifiers.isPrivate()

    override var emit = true
    override var tag: Boolean = false

    // TODO: Get rid of this; with the new predicate approach it's redundant (and
    // storing it per element is problematic since the predicate sometimes includes
    // methods from parent interfaces etc)
    override var included: Boolean = true

    companion object {
        private var nextRank: Int = 1
    }
}