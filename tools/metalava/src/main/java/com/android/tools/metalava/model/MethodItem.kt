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
import java.util.*
import java.util.function.Predicate
import kotlin.Comparator

interface MethodItem : MemberItem {
    /** Whether this method is a constructor */
    fun isConstructor(): Boolean

    /** The type of this field, or null for constructors */
    fun returnType(): TypeItem?

    /** The list of parameters */
    fun parameters(): List<ParameterItem>

    /** Returns the super methods that this method is overriding */
    fun superMethods(): List<MethodItem>

    fun allSuperMethods(): Sequence<MethodItem> {
        val original = superMethods().firstOrNull() ?: return emptySequence()
        return generateSequence(original) { item ->
            val superMethods = item.superMethods()
            superMethods.firstOrNull()
        }
    }

    /** Any type parameters for the class, if any, as a source string (with fully qualified class names) */
    fun typeParameterList(): String?

    /** Returns the classes that are part of the type parameters of this method, if any */
    fun typeArgumentClasses(): List<ClassItem> = TODO("Not yet implemented")

    /** Types of exceptions that this method can throw */
    fun throwsTypes(): List<ClassItem>

    fun filteredThrowsTypes(predicate: Predicate<Item>): Collection<ClassItem> {
        return filteredThrowsTypes(predicate, LinkedHashSet())
    }

    private fun filteredThrowsTypes(
        predicate: Predicate<Item>,
        classes: LinkedHashSet<ClassItem>
    ): LinkedHashSet<ClassItem> {

        for (cls in throwsTypes()) {
            if (predicate.test(cls)) {
                classes.add(cls)
            } else {
                // Excluded, but it may have super class throwables that are included; if so, include those
                var curr = cls.publicSuperClass()
                while (curr != null) {
                    if (predicate.test(cls)) {
                        classes.add(curr)
                        break
                    }
                    curr = curr.publicSuperClass()
                }
            }
        }
        return classes
    }

    /**
     * If this method is inherited from a hidden super class, but implements a method
     * from a public interface, this property is set. This is necessary because these
     * methods should not be listed in signature files (at least not in compatibility mode),
     * whereas in stub files it's necessary for them to be included (otherwise subclasses
     * may think the method required and not yet implemented, e.g. the class must be
     * abstract.)
     */
    var inheritedInterfaceMethod: Boolean

    /**
     * Duplicates this field item. Used when we need to insert inherited fields from
     * interfaces etc.
     */
    fun duplicate(targetContainingClass: ClassItem): MethodItem

    fun findPredicateSuperMethod(predicate: Predicate<Item>): MethodItem? {
        if (isConstructor()) {
            return null
        }

        val superMethods = superMethods()
        for (method in superMethods) {
            if (predicate.test(method)) {
                return method
            }
        }

        for (method in superMethods) {
            val found = method.findPredicateSuperMethod(predicate)
            if (found != null) {
                return found
            }
        }

        return null
    }

    override fun accept(visitor: ItemVisitor) {
        if (visitor.skip(this)) {
            return
        }

        visitor.visitItem(this)
        if (isConstructor()) {
            visitor.visitConstructor(this as ConstructorItem)
            if (visitor.visitConstructorsAsMethods) {
                visitor.visitMethod(this)
            }
        } else {
            visitor.visitMethod(this)
        }

        for (parameter in parameters()) {
            parameter.accept(visitor)
        }

        if (isConstructor()) {
            if (visitor.visitConstructorsAsMethods) {
                visitor.afterVisitConstructor(this as ConstructorItem)
            }
            visitor.afterVisitConstructor(this as ConstructorItem)
        } else {
            visitor.afterVisitMethod(this)
        }
        visitor.afterVisitItem(this)
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        if (visitor.skip(this)) {
            return
        }

        if (!isConstructor()) {
            val type = returnType()
            if (type != null) { // always true when not a constructor
                visitor.visitType(type, this)
            }
        }

        for (parameter in parameters()) {
            parameter.acceptTypes(visitor)
        }

        for (exception in throwsTypes()) {
            exception.acceptTypes(visitor)
        }

        if (!isConstructor()) {
            val type = returnType()
            if (type != null) {
                visitor.visitType(type, this)
            }
        }
    }

    companion object {
        private fun compareMethods(o1: MethodItem, o2: MethodItem): Int {
            val name1 = o1.name()
            val name2 = o2.name()
            if (name1 == name2) {
                val rankDelta = o1.sortingRank - o2.sortingRank
                if (rankDelta != 0) {
                    return rankDelta
                }

                // Compare by the rest of the signature to ensure stable output (we don't need to sort
                // by return value or modifiers or modifiers or throws-lists since methods can't be overloaded
                // by just those attributes
                val p1 = o1.parameters()
                val p2 = o2.parameters()
                val p1n = p1.size
                val p2n = p2.size
                for (i in 0 until minOf(p1n, p2n)) {
                    val compareTypes = p1[i].type().toString().compareTo(p2[i].type().toString(), ignoreCase = true)
                    if (compareTypes != 0) {
                        return compareTypes
                    }
                    // (Don't compare names; they're not part of the signatures)
                }
                return p1n.compareTo(p2n)
            }

            return name1.compareTo(name2)
        }

        val comparator: Comparator<MethodItem> = Comparator { o1, o2 -> compareMethods(o1, o2) }
        val sourceOrderComparator: Comparator<MethodItem> = Comparator { o1, o2 ->
            val delta = o1.sortingRank - o2.sortingRank
            if (delta == 0) {
                // Within a source file all the items will have unique sorting ranks, but since
                // we copy methods in from hidden super classes it's possible for ranks to clash,
                // and in that case we'll revert to a signature based comparison
                comparator.compare(o1, o2)
            } else {
                delta
            }
        }

        /** Gets the primary super method from a given method */
        fun getPrimarySuperMethod(method: MethodItem): MethodItem? {
            val superMethods = method.superMethods()
            return when {
                superMethods.isEmpty() -> null
                superMethods.size > 1 -> {
                    // Prefer default methods (or super class method bodies)
                    superMethods
                        .filter { it.modifiers.isDefault() || it.containingClass().isClass() }
                        .forEach { return it }
                    superMethods[0]
                }
                else -> superMethods[0]
            }
        }

        fun sameSignature(method: MethodItem, superMethod: MethodItem, compareRawTypes: Boolean = false): Boolean {
            // If the return types differ, override it (e.g. parent implements clone(),
            // subclass overrides with more specific return type)
            if (method.returnType() != superMethod.returnType()) {
                return false
            }

            // IntentService#onStart - is it here because they vary in deprecation status?
            if (method.deprecated != superMethod.deprecated) {
                return false
            }

            // Compare modifier lists; note that here we need to
            // skip modifiers that don't apply in compat mode if set
            if (!method.modifiers.equivalentTo(superMethod.modifiers)) {
                return false
            }

            val parameterList1 = method.parameters()
            val parameterList2 = superMethod.parameters()

            if (parameterList1.size != parameterList2.size) {
                return false
            }

            assert(parameterList1.size == parameterList2.size)
            for (i in 0 until parameterList1.size) {
                val p1 = parameterList1[i]
                val p2 = parameterList2[i]
                val pt1 = p1.type()
                val pt2 = p2.type()

                if (compareRawTypes) {
                    if (pt1.toErasedTypeString() != pt2.toErasedTypeString()) {
                        return false
                    }

                } else {
                    if (pt1 != pt2) {
                        return false
                    }
                }

                // TODO: Compare annotations to see for example whether
                // you've refined the nullness policy; if so, that should be included
            }

            // Also compare throws lists
            val throwsList12 = method.throwsTypes()
            val throwsList2 = superMethod.throwsTypes()

            if (throwsList12.size != throwsList2.size) {
                return false
            }

            assert(throwsList12.size == throwsList2.size)
            for (i in 0 until throwsList12.size) {
                val p1 = throwsList12[i]
                val p2 = throwsList2[i]
                val pt1 = p1.qualifiedName()
                val pt2 = p2.qualifiedName()
                if (pt1 != pt2) { // assumes throws lists are sorted!
                    return false
                }
            }

            return true
        }
    }

    fun formatParameters(): String? {
        // TODO: Generalize, allow callers to control whether to include annotations, whether to erase types,
        // whether to include names, etc
        if (parameters().isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        for (parameter in parameters()) {
            if (!sb.isEmpty()) {
                sb.append(", ")
            }
            sb.append(parameter.type().toString())
        }

        return sb.toString()
    }

    override fun requiresNullnessInfo(): Boolean {
        if (isConstructor()) {
            return false
        } else if (returnType()?.primitive != true) {
            return true
        }
        for (parameter in parameters()) {
            if (!parameter.type().primitive) {
                return true
            }
        }
        return false
    }

    override fun hasNullnessInfo(): Boolean {
        if (!isConstructor() && returnType()?.primitive != true) {
            if (!modifiers.hasNullnessInfo()) {
                return false
            }
        }

        @Suppress("LoopToCallChain") // The quickfix is wrong! (covered by AnnotationStatisticsTest)
        for (parameter in parameters()) {
            if (!parameter.hasNullnessInfo()) {
                return false
            }
        }

        return true
    }

    fun isImplicitConstructor(): Boolean {
        return isConstructor() && modifiers.isPublic() && parameters().isEmpty()
    }

    /**
     * Returns true if this method is a signature match for the given method (e.g. can
     * be overriding). This checks that the name and parameter lists match, but ignores
     * differences in parameter names, return value types and throws list types.
     */
    fun matches(other: MethodItem): Boolean {
        if (this === other) return true

        if (name() != other.name()) {
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
            val type1 = parameter1.type().toErasedTypeString()
            val type2 = parameter2.type().toErasedTypeString()
            if (type1 != type2) {
                return false
            }
        }
        return true
    }
}