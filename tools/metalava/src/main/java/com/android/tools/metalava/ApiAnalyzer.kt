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

package com.android.tools.metalava

import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.model.AnnotationAttributeValue
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.ConstructorItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.PackageList
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.TypeItem
import com.android.tools.metalava.model.visitors.ApiVisitor
import com.android.tools.metalava.model.visitors.ItemVisitor
import com.android.tools.metalava.model.visitors.VisibleItemVisitor
import java.util.*
import java.util.function.Predicate

/**
 * The [ApiAnalyzer] is responsible for walking over the various
 * classes and members and compute visibility etc of the APIs
 */
class ApiAnalyzer(
    /** The code to analyze */
    private val codebase: Codebase
) {
    /** All packages in the API */
    private val packages: PackageList = codebase.getPackages()

    fun computeApi() {
        if (codebase.trustedApi()) {
            // The codebase is already an API; no consistency checks to be performed
            return
        }

        // Apply options for packages that should be hidden
        hidePackages()
        skipEmitPackages()

        // Propagate visibility down into individual elements -- if a class is hidden,
        // then the methods and fields are hidden etc
        propagateHiddenRemovedAndDocOnly(false)
    }

    fun addConstructors(filterEmit: Predicate<Item>) {
        // Let's say I have
        //  class GrandParent { public GrandParent(int) {} }
        //  class Parent {  Parent(int) {} }
        //  class Child { public Child(int) {} }
        //
        // Here Parent's constructor is not public. For normal stub generation I'd end up with this:
        //  class GrandParent { public GrandParent(int) {} }
        //  class Parent { }
        //  class Child { public Child(int) {} }
        //
        // This doesn't compile - Parent can't have a default constructor since there isn't
        // one for it to invoke on GrandParent.
        //
        // I can generate a fake constructor instead, such as
        //   Parent() { super(0); }
        //
        // But it's hard to do this lazily; what if I'm generating the Child class first?
        // Therefore, we'll instead walk over the hierarchy and insert these constructors
        // into the Item hierarchy such that code generation can find them.
        //
        // (We also need to handle the throws list, so we can't just unconditionally
        // insert package private constructors
        //
        // To do this right I really need to process super constructors before the classes
        // depending on them.

        // Mark all classes that are the super class of some other class:
        val allClasses = packages.allClasses()

        codebase.clearTags()
        allClasses.forEach { cls ->
            cls.superClass()?.tag = true
        }

        val leafClasses = allClasses.filter { !it.tag }.toList()

        // Now walk through all the leaf classes, and walk up the super hierarchy
        // and recursively add constructors; we'll do it recursively to make sure that
        // the superclass has had its constructors initialized first (such that we can
        // match the parameter lists and throws signatures), and we use the tag fields
        // to avoid looking at all the internal classes more than once.
        codebase.clearTags()
        leafClasses
            // Filter classes by filterEmit here to not waste time in hidden packages
            .filter { filterEmit.test(it) }
            .forEach { addConstructors(it, filterEmit, true) }
    }

    /**
     * Handle computing constructor hierarchy. We'll be setting several attributes:
     * [ClassItem.defaultConstructor] : The default constructor to invoke in this
     *   class from subclasses. **NOTE**: This constructor may not be part of
     *   the [ClassItem.constructors] list, e.g. for package private default constructors
     *   we've inserted (because there were no public constructors or constructors not
     *   using hidden parameter types.)
     *
     *   If we can find a public constructor we'll put that here instead.
     *
     * [ConstructorItem.superConstructor] The default constructor to invoke. If set,
     * use this rather than the [ClassItem.defaultConstructor].
     *
     * [ClassItem.hasPrivateConstructor] Set if this class has one or more private
     * constructors.
     *
     * [Item.tag] : mark for avoiding repeated iteration of internal item nodes
     *
     *
     */
    private fun addConstructors(cls: ClassItem, filterEmit: Predicate<Item>, isLeaf: Boolean) {
        // What happens if we have
        //  package foo:
        //     public class A { public A(int) }
        //  package bar
        //     public class B extends A { public B(int) }
        // If I just try inserting package private constructors here things will NOT work:
        //  package foo:
        //     public class A { public A(int); A() {} }
        //  package bar
        //     public class B extends A { public B(int); B() }
        //  because A() is not accessible from B() -- it's outside the same package.
        //
        // So, I'll need to model the real constructors for all the scenarios where that
        // works.
        //
        // The remaining challenge is that there will be some gaps: when I don't have
        // a default constructor, subclass constructors will have to have an explicit
        // super(args) call to pick the parent constructor to use. And which one?
        // It generally doesn't matter; just pick one, but unfortunately, the super
        // constructor can throw exceptions, and in that case the subclass constructor
        // must also throw all those constructors (you can't surround a super call
        // with try/catch.)  Luckily, the source code already needs to do this to
        // compile, so we can just use the same constructor as the super call.
        // But there are two cases we have to deal with:
        //   (1) the constructor doesn't call a super constructor; it calls another
        //       constructor on this class.
        //   (2) the super constructor it *does* call isn't available.
        //
        // For (1), this means that our stub code generator should be prepared to
        // handle both super- and this- dispatches; we'll handle this by pointing
        // it to the constructor to use, and it checks to see if the containing class
        // for the constructor is the same to decide whether to emit "this" or "super".

        if (cls.tag || !cls.isClass()) { // Don't add constructors to interfaces, enums, annotations, etc
            return
        }

        // First handle its super class hierarchy to make sure that we've
        // already constructed super classes
        val superClass = cls.superClass()
        superClass?.let { it -> addConstructors(it, filterEmit, false) }
        cls.tag = true

        // Use
        if (superClass != null) {
            val superDefaultConstructor = superClass.defaultConstructor
            if (superDefaultConstructor != null) {
                for (constructor in cls.constructors()) {
                    val superConstructor = constructor.superConstructor
                    if (superConstructor == null ||
                        (superConstructor.containingClass() != superClass &&
                                superConstructor.containingClass() != cls)) {
                        constructor.superConstructor = superDefaultConstructor
                    }
                }
            }
        }

        // Find default constructor, if one doesn't exist
        if (!isLeaf) {
            val constructors = cls.constructors()
            for (constructor in constructors) {
                if (constructor.parameters().isEmpty() && constructor.isPublic) {
                    cls.defaultConstructor = constructor
                    return
                }
            }

            // Try to pick the constructor with the fewest throwables, and among those
            // the ones with the fewest parameters
            if (!constructors.isEmpty()) {
                // Try to pick the constructor, sorting first by "matches filter", then by
                // fewest throwables, then fewest parameters, then based on order in list
                val first = constructors.first()
                cls.defaultConstructor = constructors.foldRight(first) { current, next ->
                    pickBest(current, next, filterEmit)
                }
                return
            } else {
                // No constructors, yet somebody extends this: we have to invent one, such that
                // subclasses can dispatch to it in the stub files etc
                cls.defaultConstructor = cls.createDefaultConstructor().also {
                    it.mutableModifiers().setPackagePrivate(true)
                    it.hidden = false
                }
            }
        }

        if (cls.hasPrivateConstructor) {
            // TODO: There was a private constructor: we have to make sure we insert one,
            // such that the class doesn't suddenly get an implicit constructor!
        }
    }

    // TODO: Annotation test: @ParameterName, if present, must be supplied on *all* the arguments!
    // Warn about @DefaultValue("null"); they probably meant @DefaultNull
    // Supplying default parameter in override is not allowed!

    private fun pickBest(
        current: ConstructorItem,
        next: ConstructorItem,
        filterEmit: Predicate<Item>
    ): ConstructorItem {
        val currentMatchesFilter = filterEmit.test(current)
        val nextMatchesFilter = filterEmit.test(next)
        if (currentMatchesFilter != nextMatchesFilter) {
            return if (currentMatchesFilter) {
                current
            } else {
                next
            }
        }

        val currentThrowsCount = current.throwsTypes().size
        val nextThrowsCount = next.throwsTypes().size

        return if (currentThrowsCount < nextThrowsCount) {
            current
        } else if (currentThrowsCount > nextThrowsCount) {
            next
        } else {
            val currentParameterCount = current.parameters().size
            val nextParameterCount = next.parameters().size
            if (currentParameterCount <= nextParameterCount) {
                current
            } else
                next
        }
    }

    fun generateInheritedStubs(filterEmit: Predicate<Item>, filterReference: Predicate<Item>) {
        packages.allClasses().forEach {
            if (filterEmit.test(it)) {
                generateInheritedStubs(it, filterEmit, filterReference)
            }
        }
    }

    private fun generateInheritedStubs(cls: ClassItem, filterEmit: Predicate<Item>, filterReference: Predicate<Item>) {
        if (!cls.isClass()) return
        if (cls.superClass() == null) return
        val superClasses: Sequence<ClassItem> = generateSequence(cls.superClass()) { it.superClass() }
        val hiddenSuperClasses: Sequence<ClassItem> = superClasses.filter { !filterEmit.test(it) && !it.isJavaLangObject() }
        if (hiddenSuperClasses.none()) { // not missing any implementation methods
            return
        }

        generateInheritedStubsFrom(cls, hiddenSuperClasses, superClasses, filterEmit, filterReference)
        addInheritedInterfacesFrom(cls, hiddenSuperClasses, filterReference)

    }

    private fun addInheritedInterfacesFrom(
        cls: ClassItem,
        hiddenSuperClasses: Sequence<ClassItem>,
        filterReference: Predicate<Item>
    ) {
        var interfaceTypes: MutableList<TypeItem>? = null
        var interfaceTypeClasses: MutableList<ClassItem>? = null
        for (hiddenSuperClass in hiddenSuperClasses) {
            for (hiddenInterface in hiddenSuperClass.interfaceTypes()) {
                val hiddenInterfaceClass = hiddenInterface.asClass()
                if (filterReference.test(hiddenInterfaceClass ?: continue)) {
                    if (interfaceTypes == null) {
                        interfaceTypes = cls.interfaceTypes().toMutableList()
                        interfaceTypeClasses = interfaceTypes.asSequence().map { it.asClass() }.filterNotNull().toMutableList()
                        if (cls.isInterface()) {
                            cls.superClass()?.let { interfaceTypeClasses.add(it) }
                        }
                        cls.setInterfaceTypes(interfaceTypes)
                    }
                    if (interfaceTypeClasses!!.any { it == hiddenInterfaceClass }) {
                        continue
                    }

                    interfaceTypeClasses.add(hiddenInterfaceClass)

                    if (hiddenInterfaceClass.hasTypeVariables()) {
                        val mapping = cls.mapTypeVariables(hiddenSuperClass)
                        if (mapping.isNotEmpty()) {
                            val mappedType: TypeItem = hiddenInterface.convertType(mapping, cls)
                            interfaceTypes.add(mappedType)
                            continue
                        }
                    }

                    interfaceTypes.add(hiddenInterface)
                }
            }
        }
    }

    private fun generateInheritedStubsFrom(
        cls: ClassItem,
        hiddenSuperClasses: Sequence<ClassItem>,
        superClasses: Sequence<ClassItem>,
        filterEmit: Predicate<Item>, filterReference: Predicate<Item>
    ) {

        // Also generate stubs for any methods we would have inherited from abstract parents
        // All methods from super classes that (1) aren't overridden in this class already, and
        // (2) are overriding some method that is in a public interface accessible from this class.
        val interfaces: Set<TypeItem> = cls.allInterfaceTypes(filterReference).asSequence().toSet()

        // Note that we can't just call method.superMethods() to and see whether any of their containing
        // classes are among our target APIs because it's possible that the super class doesn't actually
        // implement the interface, but still provides a matching signature for the interface.
        // Instead we'll look through all of our interface methods and look for potential overrides
        val interfaceNames = mutableMapOf<String, MutableList<MethodItem>>()
        for (interfaceType in interfaces) {
            val interfaceClass = interfaceType.asClass() ?: continue
            for (method in interfaceClass.methods()) {
                val name = method.name()
                val list = interfaceNames[name] ?: run {
                    val list = ArrayList<MethodItem>()
                    interfaceNames[name] = list
                    list
                }
                list.add(method)
            }
        }

        // Also add in any abstract methods from public super classes
        val publicSuperClasses = superClasses.filter { filterEmit.test(it) && !it.isJavaLangObject() }
        for (superClass in publicSuperClasses) {
            for (method in superClass.methods()) {
                if (!method.modifiers.isAbstract()) {
                    continue
                }
                val name = method.name()
                val list = interfaceNames[name] ?: run {
                    val list = ArrayList<MethodItem>()
                    interfaceNames[name] = list
                    list
                }
                list.add(method)
            }
        }

        // Find all methods that are inherited from these classes into our class
        // (making sure that we don't have duplicates, e.g. a method defined by one
        // inherited class and then overridden by another closer one).
        // map from method name to super methods overriding our interfaces
        val map = HashMap<String, MutableList<MethodItem>>()

        for (superClass in hiddenSuperClasses) {
            for (method in superClass.methods()) {
                val modifiers = method.modifiers
                if (!modifiers.isPrivate() && !modifiers.isAbstract()) {
                    val name = method.name()
                    val candidates = interfaceNames[name] ?: continue
                    val parameterCount = method.parameters().size
                    for (superMethod in candidates) {
                        if (parameterCount != superMethod.parameters().count()) {
                            continue
                        }
                        if (method.matches(superMethod)) {
                            val list = map[name] ?: run {
                                val newList = ArrayList<MethodItem>()
                                map[name] = newList
                                newList
                            }
                            list.add(method)
                            break
                        }
                    }
                }
            }
        }

        // Remove any methods that are overriding any of our existing methods
        for (method in cls.methods()) {
            val name = method.name()
            val candidates = map[name] ?: continue
            val iterator = candidates.listIterator()
            while (iterator.hasNext()) {
                val inheritedMethod = iterator.next()
                if (method.matches(inheritedMethod)) {
                    iterator.remove()
                }
            }
        }

        // Next remove any overrides among the remaining super methods (e.g. one method from a hidden parent is
        // overriding another method from a more distant hidden parent).
        map.values.forEach { methods ->
            if (methods.size >= 2) {
                for (candidate in ArrayList(methods)) {
                    methods.removeAll(candidate.superMethods())
                }
            }
        }

        // We're now left with concrete methods in hidden parents that are implementing methods in public
        // interfaces that are listed in this class. Create stubs for them:
        map.values.flatten().forEach {
            val method = cls.createMethod(it)
            method.documentation = "// Inlined stub from hidden parent class ${it.containingClass().qualifiedName()}\n" +
                    method.documentation
            method.inheritedInterfaceMethod = true
            cls.addMethod(method)
        }
    }

    /** Hide packages explicitly listed in [Options.hidePackages] */
    private fun hidePackages() {
        for (pkgName in options.hidePackages) {
            val pkg = codebase.findPackage(pkgName) ?: continue
            pkg.hidden = true
            pkg.included = false // because included has already been initialized
        }
    }

    /** Apply emit filters listed in [Options.skipEmitPackages] */
    private fun skipEmitPackages() {
        for (pkgName in options.skipEmitPackages) {
            val pkg = codebase.findPackage(pkgName) ?: continue
            pkg.emit = false
        }
    }

    /** Merge in external data from all configured sources */
    fun mergeExternalAnnotations() {
        val mergeAnnotations = options.mergeAnnotations
        if (!mergeAnnotations.isEmpty()) {
            AnnotationsMerger(codebase, options.apiFilter).merge(mergeAnnotations)
        }
    }

    /**
     * Propagate the hidden flag down into individual elements -- if a class is hidden, then the methods and fields
     * are hidden etc
     */
    private fun propagateHiddenRemovedAndDocOnly(includingFields: Boolean) {
        packages.accept(object : ItemVisitor(visitConstructorsAsMethods = true, nestInnerClasses = true) {
            override fun visitItem(item: Item) {
                if (item.modifiers.hasShowAnnotation()) {
                    item.hidden = false
                } else if (item.modifiers.hasHideAnnotations()) {
                    item.hidden = true
                }
            }

            override fun visitPackage(pkg: PackageItem) {
                val containingPackage = pkg.containingPackage()
                if (containingPackage != null) {
                    if (containingPackage.hidden) {
                        pkg.hidden = true
                    }
                    if (containingPackage.docOnly) {
                        pkg.docOnly = true
                    }
                }
            }

            override fun visitClass(cls: ClassItem) {
                val containingClass = cls.containingClass()
                if (containingClass != null) {
                    if (containingClass.hidden) {
                        cls.hidden = true
                    }
                    if (containingClass.docOnly) {
                        cls.docOnly = true
                    }
                    if (containingClass.removed) {
                        cls.removed = true
                    }
                } else {
                    val containingPackage = cls.containingPackage()
                    if (containingPackage.hidden && !containingPackage.isDefault) {
                        cls.hidden = true
                    }
                    if (containingPackage.docOnly && !containingPackage.isDefault) {
                        cls.docOnly = true
                    }
                    if (containingPackage.removed && !cls.modifiers.hasShowAnnotation()) {
                        cls.removed = true
                    }
                }
            }

            override fun visitMethod(method: MethodItem) {
                val containingClass = method.containingClass()
                if (containingClass.hidden) {
                    method.hidden = true
                }
                if (containingClass.docOnly) {
                    method.docOnly = true
                }
                if (containingClass.removed) {
                    method.removed = true
                }
            }

            override fun visitField(field: FieldItem) {
                val containingClass = field.containingClass()
                /* We don't always propagate field visibility down to the fields
                   because we sometimes move fields around, and in that
                   case we don't want to carry forward the "hidden" attribute
                   from the field that wasn't marked on the field but its
                   container interface.
                */
                if (includingFields && containingClass.hidden) {
                    field.hidden = true
                }
                if (containingClass.docOnly) {
                    field.docOnly = true
                }
                if (containingClass.removed) {
                    field.removed = true
                }
            }
        })
    }

    private fun applyApiFilter() {
        options.apiFilter?.let { filter ->
            packages.accept(object : VisibleItemVisitor() {

                override fun visitPackage(pkg: PackageItem) {
                    if (!filter.hasPackage(pkg.qualifiedName())) {
                        pkg.included = false
                    }
                }

                override fun visitClass(cls: ClassItem) {
                    if (!filter.hasClass(cls.qualifiedName())) {
                        cls.included = false
                    }
                }

                override fun visitMethod(method: MethodItem) {
                    if (!filter.hasMethod(
                            method.containingClass().qualifiedName(), method.name(),
                            method.formatParameters()
                        )) {
                        method.included = false
                    }
                }

                override fun visitField(field: FieldItem) {
                    if (!filter.hasField(field.containingClass().qualifiedName(), field.name())) {
                        field.included = false
                    }
                }
            })
        }
    }

    private fun checkHiddenTypes() {
        packages.accept(object : ApiVisitor(codebase, visitConstructorsAsMethods = false) {
            override fun visitMethod(method: MethodItem) {
                checkType(method, method.returnType() ?: return) // constructors don't have
            }

            override fun visitField(field: FieldItem) {
                checkType(field, field.type())
            }

            override fun visitParameter(parameter: ParameterItem) {
                checkType(parameter, parameter.type())
            }

            private fun checkType(item: Item, type: TypeItem) {
                if (type.primitive) {
                    return
                }

                val cls = type.asClass()

                // Don't flag type parameters like T
                if (cls?.isTypeParameter == true) {
                    return
                }

                // class may be null for things like array types and ellipsis types,
                // but iterating through the type argument classes below will find and
                // check the component class
                if (cls != null && !filterReference.test(cls) && !cls.isFromClassPath()) {
                    reporter.report(
                        Errors.HIDDEN_TYPE_PARAMETER, item,
                        "${item.toString().capitalize()} references hidden type $type."
                    )
                }

                type.typeArgumentClasses()
                    .filter { it != cls }
                    .forEach { checkType(item, it) }
            }

            private fun checkType(item: Item, cls: ClassItem) {
                if (!filterReference.test(cls)) {
                    if (!cls.isFromClassPath()) {
                        reporter.report(
                            Errors.HIDDEN_TYPE_PARAMETER, item,
                            "${item.toString().capitalize()} references hidden type $cls."
                        )
                    }
                } else {
                    cls.typeArgumentClasses()
                        .filter { it != cls }
                        .forEach { checkType(item, it) }
                }
            }
        })
    }

    private fun ensureSystemServicesProtectedWithPermission() {
        if (options.showAnnotations.contains("android.annotation.SystemApi") && options.manifest != null) {
            // Look for Android @SystemApi exposed outside the normal SDK; we require
            // that they're protected with a system permission.

            packages.accept(object : ApiVisitor(codebase) {
                override fun visitClass(cls: ClassItem) {
                    // This class is a system service if it's annotated with @SystemService,
                    // or if it's android.content.pm.PackageManager
                    if (cls.modifiers.isAnnotatedWith("android.annotation.SystemService") ||
                        cls.qualifiedName() == "android.content.pm.PackageManager") {
                        // Check permissions on system services
                        for (method in cls.filteredMethods(filterEmit)) {
                            checkSystemPermissions(method)
                        }
                    }
                }
            })
        }
    }

    private fun checkSystemPermissions(method: MethodItem) {
        if (method.isImplicitConstructor()) { // Don't warn on non-source elements like implicit default constructors
            return
        }

        val annotation = method.modifiers.findAnnotation("android.annotation.RequiresPermission")
        var hasAnnotation = false

        if (annotation != null) {
            hasAnnotation = true
            for (attribute in annotation.attributes()) {
                var values: List<AnnotationAttributeValue>? = null
                var any = false
                when (attribute.name) {
                    "value", "allOf" -> {
                        values = attribute.leafValues()
                    }
                    "anyOf" -> {
                        any = true
                        values = attribute.leafValues()
                    }
                }

                values ?: continue

                val system = ArrayList<String>()
                val nonSystem = ArrayList<String>()
                val missing = ArrayList<String>()
                for (value in values) {
                    val perm = (value.value() ?: value.toSource()).toString()
                    val level = codebase.getPermissionLevel(perm)
                    if (level == null) {
                        if (any) {
                            missing.add(perm)
                            continue
                        }

                        reporter.report(
                            Errors.REMOVED_FIELD, method,
                            "Permission '$perm' is not defined by manifest ${codebase.manifest}."
                        )
                        continue
                    }
                    if (level.contains("normal") || level.contains("dangerous")
                        || level.contains("ephemeral")) {
                        nonSystem.add(perm)
                    } else {
                        system.add(perm)
                    }
                }
                if (any && missing.size == values.size) {
                    reporter.report(
                        Errors.REMOVED_FIELD, method,
                        "None of the permissions ${missing.joinToString()} are defined by manifest " +
                                "${codebase.manifest}."
                    )
                }

                if (system.isEmpty() && nonSystem.isEmpty()) {
                    hasAnnotation = false
                } else if (any && !nonSystem.isEmpty() || !any && system.isEmpty()) {
                    reporter.report(
                        Errors.REQUIRES_PERMISSION, method, "Method '" + method.name()
                                + "' must be protected with a system permission; it currently"
                                + " allows non-system callers holding " + nonSystem.toString()
                    )
                }
            }
        }

        if (!hasAnnotation) {
            reporter.report(
                Errors.REQUIRES_PERMISSION, method, "Method '" + method.name()
                        + "' must be protected with a system permission."
            )
        }
    }

    fun performChecks() {
        if (codebase.trustedApi()) {
            // The codebase is already an API; no consistency checks to be performed
            return
        }

        // TODO for performance: Single iteration over the whole API surface!
        ensureSystemServicesProtectedWithPermission()
        checkHiddenTypes()

        packages.accept(object : ApiVisitor(codebase) {
            override fun visitItem(item: Item) {
                // TODO: Check annotations and also mark removed/hidden based on annotations
                if (item.deprecated && !item.documentation.contains("@deprecated")) {
                    reporter.report(
                        Errors.DEPRECATION_MISMATCH, item,
                        "${item.toString().capitalize()}: @Deprecated annotation (present) and @deprecated doc tag (not present) do not match"
                    )
                }
                // TODO: Check opposite (doc tag but no annotation)
                // TODO: Other checks
            }
        })
    }

    fun handleStripping() {
        // TODO: Switch to visitor iteration
        //val stubPackages = options.stubPackages
        val stubImportPackages = options.stubImportPackages
        handleStripping(stubImportPackages)
    }

    private fun handleStripping(stubImportPackages: Set<String>) {
        val notStrippable = HashSet<ClassItem>(5000)

        // If a class is public or protected, not hidden, not imported and marked as included,
        // then we can't strip it
        val allTopLevelClasses = codebase.getPackages().allTopLevelClasses().toList()
        allTopLevelClasses
            .filter { it.checkLevel() && it.emit && !it.hidden() }
            .forEach {
                cantStripThis(it, notStrippable, "0:0", stubImportPackages)
            }

        // complain about anything that looks includeable but is not supposed to
        // be written, e.g. hidden things
        for (cl in notStrippable) {
            if (!cl.isHiddenOrRemoved()) {
                for (m in cl.methods()) {
                    if (!m.checkLevel()) {
                        continue
                    }
                    if (m.isHiddenOrRemoved()) {
                        reporter.report(
                            Errors.UNAVAILABLE_SYMBOL, m,
                            "Reference to unavailable method " + m.name()
                        )
                    } else if (m.deprecated) {
                        // don't bother reporting deprecated methods
                        // unless they are public
                        reporter.report(
                            Errors.DEPRECATED, m, "Method " + cl.qualifiedName() + "."
                                    + m.name() + " is deprecated"
                        )
                    }

                    val returnType = m.returnType()
                    var hiddenClass = findHiddenClasses(returnType, stubImportPackages)
                    if (hiddenClass != null && !hiddenClass.isFromClassPath()) {
                        if (hiddenClass.qualifiedName() == returnType?.asClass()?.qualifiedName()) {
                            // Return type is hidden
                            reporter.report(
                                Errors.UNAVAILABLE_SYMBOL, m,
                                "Method ${cl.qualifiedName()}.${m.name()} returns unavailable " +
                                        "type ${hiddenClass.simpleName()}"
                            )
                        } else {
                            // Return type contains a generic parameter
                            reporter.report(
                                Errors.HIDDEN_TYPE_PARAMETER, m,
                                "Method ${cl.qualifiedName()}.${m.name()} returns unavailable " +
                                        "type ${hiddenClass.simpleName()} as a type parameter"
                            )
                        }
                    }

                    for (p in m.parameters()) {
                        val t = p.type()
                        if (!t.primitive) {
                            hiddenClass = findHiddenClasses(t, stubImportPackages)
                            if (hiddenClass != null && !hiddenClass.isFromClassPath()) {
                                if (hiddenClass.qualifiedName() == t.asClass()?.qualifiedName()) {
                                    // Parameter type is hidden
                                    reporter.report(
                                        Errors.UNAVAILABLE_SYMBOL, m,
                                        "Parameter of unavailable type $t in ${cl.qualifiedName()}.${m.name()}()"
                                    )
                                } else {
                                    // Parameter type contains a generic parameter
                                    reporter.report(
                                        Errors.HIDDEN_TYPE_PARAMETER, m,
                                        "Parameter uses type parameter of unavailable type $t in ${cl.qualifiedName()}.${m.name()}()"
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (cl.deprecated) {
                // not hidden, but deprecated
                reporter.report(Errors.DEPRECATED, cl, "Class ${cl.qualifiedName()} is deprecated")
            }
        }
    }

    private fun cantStripThis(
        cl: ClassItem, notStrippable: MutableSet<ClassItem>, why: String,
        stubImportPackages: Set<String>?
    ) {
        if (stubImportPackages != null && stubImportPackages.contains(cl.containingPackage().qualifiedName())) {
            // if the package is imported then it does not need stubbing.
            return
        }

        if (cl.isFromClassPath()) {
            return
        }

        if (!notStrippable.add(cl)) {
            // slight optimization: if it already contains cl, it already contains
            // all of cl's parents
            return
        }

        // cant strip any public fields or their generics
        for (field in cl.fields()) {
            if (!field.checkLevel()) {
                continue
            }
            val fieldType = field.type()
            if (!fieldType.primitive) {
                val typeClass = fieldType.asClass()
                if (typeClass != null) {
                    cantStripThis(
                        typeClass, notStrippable, "2:" + cl.qualifiedName(),
                        stubImportPackages
                    )
                }
                for (cls in fieldType.typeArgumentClasses()) {
                    cantStripThis(
                        cls, notStrippable, "3:" + cl.qualifiedName(),
                        stubImportPackages
                    )
                }
            }
        }
        // cant strip any of the type's generics
        for (cls in cl.typeArgumentClasses()) {
            cantStripThis(
                cls, notStrippable, "4:" + cl.qualifiedName(),
                stubImportPackages
            )
        }
        // cant strip any of the annotation elements
        // cantStripThis(cl.annotationElements(), notStrippable);
        // take care of methods
        cantStripThis(cl.methods(), notStrippable, stubImportPackages)
        cantStripThis(cl.constructors(), notStrippable, stubImportPackages)
        // blow the outer class open if this is an inner class
        val containingClass = cl.containingClass()
        if (containingClass != null) {
            cantStripThis(
                containingClass, notStrippable, "5:" + cl.qualifiedName(),
                stubImportPackages
            )
        }
        // blow open super class and interfaces
        val supr = cl.superClass()
        if (supr != null) {
            if (supr.isHiddenOrRemoved()) {
                // cl is a public class declared as extending a hidden superclass.
                // this is not a desired practice but it's happened, so we deal
                // with it by finding the first super class which passes checklevel for purposes of
                // generating the doc & stub information, and proceeding normally.
                val publicSuper = cl.publicSuperClass()
                // TODO: Initialize and pass super type too (in case generics are involved)
                cl.setSuperClass(publicSuper)
                if (!supr.isFromClassPath()) {
                    reporter.report(
                        Errors.HIDDEN_SUPERCLASS, cl, "Public class " + cl.qualifiedName()
                                + " stripped of unavailable superclass " + supr.qualifiedName()
                    )
                }
            } else {
                cantStripThis(
                    supr, notStrippable, "6:" + cl.superClass()?.simpleName() + cl.qualifiedName(),
                    stubImportPackages
                )
                if (supr.isPrivate && !supr.isFromClassPath()) {
                    reporter.report(
                        Errors.PRIVATE_SUPERCLASS, cl, "Public class "
                                + cl.qualifiedName() + " extends private class " + supr.qualifiedName()
                    )
                }
            }
        }
    }

    private fun cantStripThis(
        methods: List<MethodItem>, notStrippable: MutableSet<ClassItem>,
        stubImportPackages: Set<String>?
    ) {
        // for each method, blow open the parameters, throws and return types. also blow open their
        // generics
        for (method in methods) {
            if (!method.checkLevel()) {
                continue
            }
            for (typeParameterClass in method.typeArgumentClasses()) {
                cantStripThis(
                    typeParameterClass, notStrippable,
                    "8:${method.containingClass().qualifiedName()}:${method.name()}",
                    stubImportPackages
                )
            }
            for (parameter in method.parameters()) {
                for (parameterTypeClass in parameter.type().typeArgumentClasses()) {
                    cantStripThis(
                        parameterTypeClass, notStrippable, "9:"
                                + method.containingClass().qualifiedName() + ":" + method.name(),
                        stubImportPackages
                    )
                    for (tcl in parameter.type().typeArgumentClasses()) {
                        if (tcl.isHiddenOrRemoved()) {
                            reporter.report(
                                Errors.UNAVAILABLE_SYMBOL, method,
                                "Parameter of hidden type ${tcl.fullName()}" +
                                        "in ${method.containingClass().qualifiedName()}.${method.name()}()"
                            )
                        } else {
                            cantStripThis(
                                tcl, notStrippable,
                                "10:${method.containingClass().qualifiedName()}:${method.name()}",
                                stubImportPackages
                            )
                        }
                    }
                }
            }
            for (thrown in method.throwsTypes()) {
                cantStripThis(
                    thrown, notStrippable, ("11:" + method.containingClass().qualifiedName()
                            + ":" + method.name()), stubImportPackages
                )
            }
            val returnType = method.returnType()
            if (returnType != null && !returnType.primitive) {
                val returnTypeClass = returnType.asClass()
                if (returnTypeClass != null) {
                    cantStripThis(
                        returnTypeClass, notStrippable,
                        "12:${method.containingClass().qualifiedName()}:${method.name()}",
                        stubImportPackages
                    )
                    for (tyItem in returnType.typeArgumentClasses()) {
                        cantStripThis(
                            tyItem, notStrippable,
                            "13:${method.containingClass().qualifiedName()}:${method.name()}",
                            stubImportPackages
                        )
                    }
                }
            }
        }
    }

    /**
     * Find references to hidden classes.
     *
     * This finds hidden classes that are used by public parts of the API in order to ensure the
     * API is self consistent and does not reference classes that are not included in
     * the stubs. Any such references cause an error to be reported.
     *
     * A reference to an imported class is not treated as an error, even though imported classes
     * are hidden from the stub generation. That is because imported classes are, by definition,
     * excluded from the set of classes for which stubs are required.
     *
     * @param ti the type information to examine for references to hidden classes.
     * @param stubImportPackages the possibly null set of imported package names.
     * @return a reference to a hidden class or null if there are none
     */
    private fun findHiddenClasses(ti: TypeItem?, stubImportPackages: Set<String>?): ClassItem? {
        ti ?: return null
        val ci = ti.asClass() ?: return null
        return findHiddenClasses(ci, stubImportPackages)
    }

    private fun findHiddenClasses(ci: ClassItem, stubImportPackages: Set<String>?): ClassItem? {
        if (stubImportPackages != null && stubImportPackages.contains(ci.containingPackage().qualifiedName())) {
            return null
        }
        if (ci.isHiddenOrRemoved()) return ci
        for (tii in ci.toType().typeArgumentClasses()) {
            // Avoid infinite recursion in the case of Foo<T extends Foo>
            if (tii != ci) {
                val hiddenClass = findHiddenClasses(tii, stubImportPackages)
                if (hiddenClass != null) return hiddenClass
            }
        }
        return null
    }
}
