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

import com.android.tools.metalava.ApiAnalyzer
import com.android.tools.metalava.compatibility
import com.android.tools.metalava.doclava1.ElidingPredicate
import com.android.tools.metalava.model.visitors.ApiVisitor
import com.android.tools.metalava.model.visitors.ItemVisitor
import com.android.tools.metalava.model.visitors.TypeVisitor
import com.android.tools.metalava.options
import java.util.*
import java.util.function.Predicate
import kotlin.Comparator

interface ClassItem : Item {
    /** The simple name of a class. In class foo.bar.Outer.Inner, the simple name is "Inner" */
    fun simpleName(): String

    /** The full name of a class. In class foo.bar.Outer.Inner, the full name is "Outer.Inner" */
    fun fullName(): String

    /** The qualified name of a class. In class foo.bar.Outer.Inner, the qualified name is the whole thing. */
    fun qualifiedName(): String

    /** Is this an innerclass? */
    fun isInnerClass(): Boolean = containingClass() != null

    /** Is this a top level class? */
    fun isTopLevelClass(): Boolean = containingClass() == null

    /** This [ClassItem] and all of its inner classes, recursively */
    fun allClasses(): Sequence<ClassItem> {
        return sequenceOf(this).plus(innerClasses().asSequence().flatMap { it.allClasses() })
    }

    override fun parent(): Item? = containingClass() ?: containingPackage()

    /**
     * The qualified name where inner classes use $ as a separator.
     * In class foo.bar.Outer.Inner, this method will return foo.bar.Outer$Inner.
     * (This is the name format used in ProGuard keep files for example.)
     */
    fun qualifiedNameWithDollarInnerClasses(): String {
        var curr: ClassItem? = this
        while (curr?.containingClass() != null) {
            curr = curr.containingClass()
        }

        if (curr == null) {
            return fullName().replace('.', '$')
        }

        return curr.containingPackage().qualifiedName() + "." + fullName().replace('.', '$')
    }

    /** The super class of this class, if any  */
    fun superClass(): ClassItem?

    /** The super class type of this class, if any. The difference between this and [superClass] is
     * that the type reference can include type arguments; e.g. in "class MyList extends List<String>"
     * the super class is java.util.List and the super class type is java.util.List<java.lang.String>.
     * */
    fun superClassType(): TypeItem?

    /** Finds the public super class of this class, if any */
    fun publicSuperClass(): ClassItem? {
        var superClass = superClass()
        while (superClass != null && !superClass.checkLevel()) {
            superClass = superClass.superClass()
        }

        return superClass
    }

    /** Any interfaces implemented by this class */
    fun interfaceTypes(): List<TypeItem>

    /** All classes and interfaces implemented (by this class and its super classes and the interfaces themselves) */
    fun allInterfaces(): Sequence<ClassItem>

    /** Any inner classes of this class */
    fun innerClasses(): List<ClassItem>

    /** The constructors in this class */
    fun constructors(): List<ConstructorItem>

    /** Whether this class has an implicit default constructor */
    fun hasImplicitDefaultConstructor(): Boolean

    /** The non-constructor methods in this class */
    fun methods(): List<MethodItem>

    /** The fields in this class */
    fun fields(): List<FieldItem>

    /** The members in this class: constructors, methods, fields/enum constants */
    fun members(): Sequence<MemberItem> {
        return fields().asSequence().plus(constructors().asSequence()).plus(methods().asSequence())
    }

    /** Whether this class is an interface */
    fun isInterface(): Boolean

    /** Whether this class is an annotation type */
    fun isAnnotationType(): Boolean

    /** Whether this class is an enum */
    fun isEnum(): Boolean

    /** Whether this class is an interface */
    fun isClass(): Boolean = !isInterface() && !isAnnotationType() && !isEnum()

    /** The containing class, for inner classes */
    fun containingClass(): ClassItem?

    /** The containing package */
    fun containingPackage(): PackageItem

    /** Gets the type for this class */
    fun toType(): TypeItem

    /** Returns true if this class has type parameters */
    fun hasTypeVariables(): Boolean

    /** Any type parameters for the class, if any, as a source string (with fully qualified class names) */
    fun typeParameterList(): String?

    fun typeParameterNames(): List<String>

    /** Returns the classes that are part of the type parameters of this method, if any */
    fun typeArgumentClasses(): List<ClassItem> = TODO("Not yet implemented")

    fun isJavaLangObject(): Boolean {
        return qualifiedName() == "java.lang.Object"
    }

    // Mutation APIs: Used to "fix up" the API hierarchy (in [ApiAnalyzer]) to only expose
    // visible parts of the API)

    // This replaces the "real" super class
    fun setSuperClass(superClass: ClassItem?, superClassType: TypeItem? = superClass?.toType())

    // This replaces the interface types implemented by this class
    fun setInterfaceTypes(interfaceTypes: List<TypeItem>)

    val isTypeParameter: Boolean

    var hasPrivateConstructor: Boolean

    override fun accept(visitor: ItemVisitor) {
        if (visitor is ApiVisitor) {
            accept(visitor)
            return
        }

        if (visitor.skip(this)) {
            return
        }

        visitor.visitItem(this)
        visitor.visitClass(this)

        for (constructor in constructors()) {
            constructor.accept(visitor)
        }

        for (method in methods()) {
            method.accept(visitor)
        }

        if (isEnum()) {
            // In enums, visit the enum constants first, then the fields
            for (field in fields()) {
                if (field.isEnumConstant()) {
                    field.accept(visitor)
                }
            }
            for (field in fields()) {
                if (!field.isEnumConstant()) {
                    field.accept(visitor)
                }
            }
        } else {
            for (field in fields()) {
                field.accept(visitor)
            }
        }

        if (visitor.nestInnerClasses) {
            for (cls in innerClasses()) {
                cls.accept(visitor)
            }
        } // otherwise done below

        visitor.afterVisitClass(this)
        visitor.afterVisitItem(this)

        if (!visitor.nestInnerClasses) {
            for (cls in innerClasses()) {
                cls.accept(visitor)
            }
        }
    }

    fun accept(visitor: ApiVisitor) {
        if (visitor.skip(this)) {
            return
        }

        if (!visitor.include(this)) {
            return
        }

        // We build up a separate data structure such that we can compute the
        // sets of fields, methods, etc even for inner classes (recursively); that way
        // we can easily and up front determine whether we have any matches for
        // inner classes (which is vital for computing the removed-api for example, where
        // only something like the appearance of a removed method inside an inner class
        // results in the outer class being described in the signature file.
        val candidate = VisitCandidate(this, visitor)
        candidate.accept()
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        if (visitor.skip(this)) {
            return
        }

        val type = toType()
        visitor.visitType(type, this)

        // TODO: Visit type parameter list (at least the bounds types, e.g. View in <T extends View>
        superClass()?.let {
            visitor.visitType(it.toType(), it)
        }

        if (visitor.includeInterfaces) {
            for (itf in interfaceTypes()) {
                val owner = itf.asClass()
                owner?.let { visitor.visitType(itf, it) }
            }
        }

        for (constructor in constructors()) {
            constructor.acceptTypes(visitor)
        }
        for (field in fields()) {
            field.acceptTypes(visitor)
        }
        for (method in methods()) {
            method.acceptTypes(visitor)
        }
        for (cls in innerClasses()) {
            cls.acceptTypes(visitor)
        }

        visitor.afterVisitType(type, this)
    }

    companion object {
        // Same as doclava1 (modulo the new handling when class names match
        val comparator: Comparator<in ClassItem> = Comparator { o1, o2 ->
            val delta = o1.fullName().compareTo(o2.fullName())
            if (delta == 0) {
                o1.qualifiedName().compareTo(o2.qualifiedName())
            } else {
                delta
            }
        }

        val nameComparator: Comparator<ClassItem> = Comparator { a, b ->
            a.simpleName().compareTo(b.simpleName())
        }

        val fullNameComparator: Comparator<ClassItem> = Comparator { a, b -> a.fullName().compareTo(b.fullName()) }

        val qualifiedComparator: Comparator<ClassItem> = Comparator { a, b ->
            a.qualifiedName().compareTo(b.qualifiedName())
        }

        fun classNameSorter(): Comparator<in ClassItem> =
            if (compatibility.sortClassesBySimpleName) {
                ClassItem.comparator
            } else {
                ClassItem.qualifiedComparator
            }
    }

    fun findMethod(methodName: String, parameters: String): MethodItem?

    fun findField(fieldName: String): FieldItem?

    /** Returns the corresponding compilation unit, if any */
    fun getCompilationUnit(): CompilationUnit? = null

    /**
     * Return superclass matching the given predicate. When a superclass doesn't
     * match, we'll keep crawling up the tree until we find someone who matches.
     */
    fun filteredSuperclass(predicate: Predicate<Item>): ClassItem? {
        val superClass = superClass() ?: return null
        return if (predicate.test(superClass)) {
            superClass
        } else {
            superClass.filteredSuperclass(predicate)
        }
    }

    fun filteredSuperClassType(predicate: Predicate<Item>): TypeItem? {
        var superClassType: TypeItem? = superClassType() ?: return null
        var prev: ClassItem? = null
        while (superClassType != null) {
            val superClass = superClassType.asClass() ?: return null
            if (predicate.test(superClass)) {
                if (prev == null || superClass == superClass()) {
                    // Direct reference; no need to map type variables
                    return superClassType
                }
                if (!superClassType.hasTypeArguments()) {
                    // No type variables - also no need for mapping
                    return superClassType
                }

                return superClassType.convertType(this, prev)
            }

            prev = superClass
            superClassType = superClass.superClassType()
        }

        return null
    }

    /**
     * Return methods matching the given predicate. Forcibly includes local
     * methods that override a matching method in an ancestor class.
     */
    fun filteredMethods(predicate: Predicate<Item>): Collection<MethodItem> {
        val methods = LinkedHashSet<MethodItem>()
        for (method in methods()) {
            if (predicate.test(method) || method.findPredicateSuperMethod(predicate) != null) {
                //val duplicated = method.duplicate(this)
                //methods.add(duplicated)
                methods.remove(method)
                methods.add(method)
            }
        }
        return methods
    }

    /**
     * Return fields matching the given predicate. Also clones fields from
     * ancestors that would match had they been defined in this class.
     */
    fun filteredFields(predicate: Predicate<Item>): List<FieldItem> {
        val fields = LinkedHashSet<FieldItem>()
        if (options.showAnnotations.isEmpty()) {
            for (clazz in allInterfaces()) {
                if (!clazz.isInterface()) {
                    continue
                }
                for (field in clazz.fields()) {
                    if (!predicate.test(field)) {
                        val clz = this
                        val duplicated = field.duplicate(clz)
                        if (predicate.test(duplicated)) {
                            fields.remove(duplicated)
                            fields.add(duplicated)
                        }
                    }
                }
            }
        }
        for (field in fields()) {
            if (predicate.test(field)) {
                fields.remove(field)
                fields.add(field)
            }
        }
        if (fields.isEmpty()) {
            return emptyList()
        }
        val list = fields.toMutableList()
        list.sortWith(FieldItem.comparator)
        return list
    }

    fun filteredInterfaceTypes(predicate: Predicate<Item>): Collection<TypeItem> {
        val interfaceTypes = filteredInterfaceTypes(
            predicate, LinkedHashSet(),
            includeSelf = false, includeParents = false, target = this
        )
        if (interfaceTypes.isEmpty()) {
            return interfaceTypes
        }

        return interfaceTypes
    }

    fun allInterfaceTypes(predicate: Predicate<Item>): Collection<TypeItem> {
        val interfaceTypes = filteredInterfaceTypes(
            predicate, LinkedHashSet(),
            includeSelf = false, includeParents = true, target = this
        )
        if (interfaceTypes.isEmpty()) {
            return interfaceTypes
        }

        return interfaceTypes
    }

    private fun filteredInterfaceTypes(
        predicate: Predicate<Item>,
        types: LinkedHashSet<TypeItem>,
        includeSelf: Boolean,
        includeParents: Boolean,
        target: ClassItem
    ): LinkedHashSet<TypeItem> {
        val superClassType = superClassType()
        if (superClassType != null) {
            val superClass = superClassType.asClass()
            if (superClass != null) {
                if (!predicate.test(superClass)) {
                    superClass.filteredInterfaceTypes(predicate, types, true, includeParents, target)
                } else if (includeSelf && superClass.isInterface()) {
                    types.add(superClassType)
                    if (includeParents) {
                        superClass.filteredInterfaceTypes(predicate, types, true, includeParents, target)
                    }
                }
            }
        }
        for (type in interfaceTypes()) {
            val cls = type.asClass() ?: continue
            if (predicate.test(cls)) {
                if (hasTypeVariables() && type.hasTypeArguments()) {
                    val replacementMap = target.mapTypeVariables(this)
                    if (replacementMap.isNotEmpty()) {
                        val mapped = type.convertType(replacementMap)
                        types.add(mapped)
                        continue
                    }
                }
                types.add(type)
                if (includeParents) {
                    cls.filteredInterfaceTypes(predicate, types, true, includeParents, target)
                }
            } else {
                cls.filteredInterfaceTypes(predicate, types, true, includeParents, target)
            }
        }
        return types
    }

    fun allInnerClasses(includeSelf: Boolean = false): Sequence<ClassItem> {
        val list = ArrayList<ClassItem>()
        if (includeSelf) {
            list.add(this)
        }
        addInnerClasses(list, this)
        return list.asSequence()
    }

    private fun addInnerClasses(list: MutableList<ClassItem>, cls: ClassItem) {
        for (innerClass in cls.innerClasses()) {
            list.add(innerClass)
            addInnerClasses(list, innerClass)
        }
    }

    /**
     * The default constructor to invoke on this class from subclasses; initially null
     * but populated by [ApiAnalyzer.addConstructors]. (Note that in some cases
     * [defaultConstructor] may not be in [constructors], e.g. when we need to
     * create a constructor to match a public parent class with a non-default constructor
     * and the one in the code is not a match, e.g. is marked @hide etc.)
     */
    var defaultConstructor: ConstructorItem?

    /**
     * Creates a map of type variables from this class to the given target class.
     * If class A<X,Y> extends B<X,Y>, and B is declared as class B<M,N>,
     * this returns the map {"X"->"M", "Y"->"N"}. There could be multiple intermediate
     * classes between this class and the target class, and in some cases we could
     * be substituting in a concrete class, e.g. class MyClass extends B<String,Number>
     * would return the map {"java.lang.String"->"M", "java.lang.Number"->"N"}.
     *
     * If [reverse] is true, compute the reverse map: keys are the variables in
     * the target and the values are the variables in the source.
     */
    fun mapTypeVariables(target: ClassItem, reverse: Boolean = false): Map<String, String> = codebase.unsupported()

    /**
     * Creates a constructor in this class
     */
    fun createDefaultConstructor(): ConstructorItem = codebase.unsupported()

    /**
     * Creates a method corresponding to the given method signature in this class
     */
    fun createMethod(template: MethodItem): MethodItem = codebase.unsupported()

    fun addMethod(method: MethodItem): Unit = codebase.unsupported()
}

class VisitCandidate(private val cls: ClassItem, private val visitor: ApiVisitor) {
    private val innerClasses: Sequence<VisitCandidate>
    private val constructors: Sequence<MethodItem>
    private val methods: Sequence<MethodItem>
    private val fields: Sequence<FieldItem>
    private val enums: Sequence<FieldItem>

    init {
        val filterEmit = visitor.filterEmit
        val filterReference = visitor.filterReference

        constructors = cls.constructors().asSequence().filter { filterEmit.test(it) }
            .sortedWith(MethodItem.comparator)
        val elidingPredicate = ElidingPredicate(filterReference)
        methods = cls.filteredMethods(filterEmit).asSequence()
            .filter { !visitor.elide || elidingPredicate.test(it) }
            .sortedWith(MethodItem.comparator)

        if (cls.isEnum()) {
            fields = cls.filteredFields(filterEmit).asSequence()
                .filter({ !it.isEnumConstant() })
                .sortedWith(FieldItem.comparator)
            enums = cls.filteredFields(filterEmit).asSequence()
                .filter({ it.isEnumConstant() })
                .sortedWith(FieldItem.comparator)
        } else {
            fields = cls.filteredFields(filterEmit).asSequence()
                .sortedWith(FieldItem.comparator)
            enums = emptySequence()
        }

        innerClasses = cls.innerClasses()
            .asSequence()
            .sortedWith(ClassItem.classNameSorter())
            .map { VisitCandidate(it, visitor) }
    }

    /** Will this class emit anything? */
    private fun emit(): Boolean {
        val emit = emitClass()
        if (emit) {
            return true
        }

        return innerClasses.any { it.emit() }
    }

    /** Does the body of this class (everything other than the inner classes) emit anything? */
    private fun emitClass(): Boolean {
        val classEmpty = (constructors.none() && methods.none() && enums.none() && fields.none())
        return if (visitor.filterEmit.test(cls)) {
            true
        } else if (!classEmpty) {
            visitor.filterReference.test(cls)
        } else {
            false
        }
    }

    fun accept() {
        if (visitor.skip(cls)) {
            return
        }

        if (!visitor.include(cls)) {
            return
        }

        if (!emit()) {
            return
        }

        val emitClass = emitClass()

        if (emitClass) {
            if (!visitor.visitingPackage) {
                visitor.visitingPackage = true
                val pkg = cls.containingPackage()
                visitor.visitItem(pkg)
                visitor.visitPackage(pkg)
            }

            visitor.visitItem(cls)
            visitor.visitClass(cls)

            val sortedConstructors = if (visitor.methodComparator != null) {
                constructors.sortedWith(visitor.methodComparator)
            } else {
                constructors
            }
            val sortedMethods = if (visitor.methodComparator != null) {
                methods.sortedWith(visitor.methodComparator)
            } else {
                methods
            }
            val sortedFields = if (visitor.fieldComparator != null) {
                fields.sortedWith(visitor.fieldComparator)
            } else {
                fields
            }


            for (constructor in sortedConstructors) {
                constructor.accept(visitor)
            }

            for (method in sortedMethods) {
                method.accept(visitor)
            }

            for (enumConstant in enums) {
                enumConstant.accept(visitor)
            }
            for (field in sortedFields) {
                field.accept(visitor)
            }
        }

        if (visitor.nestInnerClasses) {  // otherwise done below
            innerClasses.forEach { it.accept() }
        }

        if (emitClass) {
            visitor.afterVisitClass(cls)
            visitor.afterVisitItem(cls)
        }

        if (!visitor.nestInnerClasses) {
            innerClasses.forEach { it.accept() }
        }
    }
}
