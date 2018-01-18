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
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.ConstructorItem
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.ModifierList
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.psi.PsiClassItem
import com.android.tools.metalava.model.psi.trimDocIndent
import com.android.tools.metalava.model.visitors.ApiVisitor
import com.google.common.io.Files
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import kotlin.text.Charsets.UTF_8

class StubWriter(
    codebase: Codebase,
    private val stubsDir: File,
    ignoreShown: Boolean = false,
    private val generateAnnotations: Boolean = false,
    private val prefiltered: Boolean = true
) : ApiVisitor(
    codebase = codebase,
    visitConstructorsAsMethods = false,
    nestInnerClasses = true,
    ignoreShown = ignoreShown,
    remove = false,
    elide = false,
    fieldComparator = FieldItem.comparator,
    // Methods are by default sorted in source order in stubs, to encourage methods
    // that are near each other in the source to show up near each other in the documentation
    methodComparator = MethodItem.sourceOrderComparator
) {

    private val sourceList = StringBuilder(20000)

    /** Writes a source file list of the generated stubs */
    fun writeSourceList(target: File, root: File?) {
        target.parentFile?.mkdirs()
        val contents = if (root != null) {
            val path = root.path.replace('\\', '/')
            sourceList.toString().replace(path, "")
        } else {
            sourceList.toString()
        }
        Files.asCharSink(target, UTF_8).write(contents)
    }

    private fun startFile(sourceFile: File) {
        if (sourceList.isNotEmpty()) {
            sourceList.append(' ')
        }
        sourceList.append(sourceFile.path.replace('\\', '/'))
    }

    override fun visitPackage(pkg: PackageItem) {
        getPackageDir(pkg, create = true)

        // TODO: Write package annotations into package-info.java!
        // TODO: Write package.html, if applicable
    }

    private fun getPackageDir(packageItem: PackageItem, create: Boolean = true): File {
        val relative = packageItem.qualifiedName().replace('.', File.separatorChar)
        val dir = File(stubsDir, relative)
        if (create && !dir.isDirectory) {
            val ok = dir.mkdirs()
            if (!ok) {
                throw IOException("Could not create $dir")
            }
        }

        return dir
    }

    private fun getClassFile(classItem: ClassItem): File {
        assert(classItem.containingClass() == null, { "Should only be called on top level classes" })
        // TODO: Look up compilation unit language
        return File(getPackageDir(classItem.containingPackage()), "${classItem.simpleName()}.java")
    }

    /**
     * Between top level class files the [writer] field doesn't point to a real file; it
     * points to this writer, which redirects to the error output. Nothing should be written
     * to the writer at that time.
     */
    private var errorWriter = PrintWriter(options.stderr)

    /** The writer to write the stubs file to */
    private var writer: PrintWriter = errorWriter

    override fun visitClass(cls: ClassItem) {
        if (cls.isTopLevelClass()) {
            val sourceFile = getClassFile(cls)
            writer = try {
                PrintWriter(BufferedWriter(FileWriter(sourceFile)))
            } catch (e: IOException) {
                reporter.report(Errors.IO_ERROR, sourceFile, "Cannot open file for write.")
                errorWriter
            }

            startFile(sourceFile)

            // Copyright statements from the original file?
            val compilationUnit = cls.getCompilationUnit()
            compilationUnit?.getHeaderComments()?.let { writer.println(it) }

            val qualifiedName = cls.containingPackage().qualifiedName()
            if (qualifiedName.isNotBlank()) {
                writer.println("package $qualifiedName;")
                writer.println()
            }

            compilationUnit?.getImportStatements(filterReference)?.let {
                for (importedClass in it) {
                    writer.println("import $importedClass;")
                }
                writer.println()
            }
        }

        appendDocumentation(cls, writer)

        // "ALL" doesn't do it; compiler still warns unless you actually explicitly list "unchecked"
        writer.println("@SuppressWarnings({\"unchecked\", \"deprecation\", \"all\"})")

        // Need to filter out abstract from the modifiers list and turn it
        // into a concrete method to make the stub compile
        val removeAbstract = cls.modifiers.isAbstract() && (cls.isEnum() || cls.isAnnotationType())

        appendModifiers(cls, removeAbstract)

        when {
            cls.isAnnotationType() -> writer.print("@interface")
            cls.isInterface() -> writer.print("interface")
            cls.isEnum() -> writer.print("enum")
            else -> writer.print("class")
        }

        writer.print(" ")
        writer.print(cls.simpleName())

        generateTypeParameterList(typeList = cls.typeParameterList(), addSpace = false)
        generateSuperClassStatement(cls)
        generateInterfaceList(cls)

        writer.print(" {\n")

        if (cls.isEnum()) {
            var first = true
            // Enums should preserve the original source order, not alphabetical etc sort
            for (field in cls.fields().sortedBy { it.sortingRank }) {
                if (field.isEnumConstant()) {
                    if (first) {
                        first = false
                    } else {
                        writer.write(", ")
                    }
                    writer.write(field.name())
                }
            }
            writer.println(";")
        }

        generateMissingConstructors(cls)
    }

    private fun appendDocumentation(item: Item, writer: PrintWriter) {
        val documentation = item.fullyQualifiedDocumentation()
        if (documentation.isNotBlank()) {
            val trimmed = trimDocIndent(documentation)
            writer.println(trimmed)
            writer.println()
        }
    }

    override fun afterVisitClass(cls: ClassItem) {
        writer.print("}\n\n")

        if (cls.isTopLevelClass()) {
            writer.flush()
            writer.close()
            writer = errorWriter
        }
    }

    private fun appendModifiers(
        item: Item,
        removeAbstract: Boolean,
        removeFinal: Boolean = false,
        addPublic: Boolean = false
    ) {
        appendModifiers(item, item.modifiers, removeAbstract, removeFinal, addPublic)
    }

    private fun appendModifiers(
        item: Item,
        modifiers: ModifierList,
        removeAbstract: Boolean,
        removeFinal: Boolean = false,
        addPublic: Boolean = false
    ) {
        if (item.deprecated) {
            writer.write("@Deprecated ")
        }

        ModifierList.write(
            writer, modifiers, item, removeAbstract = removeAbstract, removeFinal = removeFinal,
            addPublic = addPublic, includeAnnotations = generateAnnotations
        )
    }

    private fun generateSuperClassStatement(cls: ClassItem) {
        if (cls.isEnum() || cls.isAnnotationType()) {
            // No extends statement for enums and annotations; it's implied by the "enum" and "@interface" keywords
            return
        }

        val superClass = if (prefiltered)
            cls.superClassType()
        else
            cls.filteredSuperClassType(filterReference)


        if (superClass != null && !superClass.isJavaLangObject()) {
            val qualifiedName = superClass.toString()
            writer.print(" extends ")

            if (qualifiedName.contains("<")) {
                // TODO: I need to push this into the model at filter-time such that clients don't need
                // to remember to do this!!
                val s = superClass.asClass()
                if (s != null) {
                    val map = cls.mapTypeVariables(s)
                    val replaced = superClass.convertTypeString(map)
                    writer.print(replaced)
                    return
                }
            }
            (cls as PsiClassItem).psiClass.superClassType
            writer.print(qualifiedName)
        }
    }

    private fun generateInterfaceList(cls: ClassItem) {
        if (cls.isAnnotationType()) {
            // No extends statement for annotations; it's implied by the "@interface" keyword
            return
        }

        val interfaces = if (prefiltered)
            cls.interfaceTypes().asSequence()
        else
            cls.filteredInterfaceTypes(filterReference).asSequence()

        if (interfaces.any()) {
            if (cls.isInterface() && cls.superClassType() != null)
                writer.print(", ")
            else
                writer.print(" implements")
            interfaces.forEachIndexed { index, type ->
                if (index > 0) {
                    writer.print(",")
                }
                writer.print(" ")
                writer.print(type.toFullyQualifiedString())
            }
        } else if (compatibility.classForAnnotations && cls.isAnnotationType()) {
            writer.print(" implements java.lang.annotation.Annotation")
        }
    }

    private fun generateTypeParameterList(
        typeList: String?,
        addSpace: Boolean
    ) {
        // TODO: Do I need to map type variables?

        if (typeList != null) {
            writer.print(typeList)

            if (addSpace) {
                writer.print(' ')
            }
        }
    }

    override fun visitConstructor(constructor: ConstructorItem) {
        val containingClass = constructor.containingClass()
        // public MarginLayoutParams(android.content.Context c, android.util.AttributeSet attrs) { throw new RuntimeException("Stub!"); }

        // Attempt to pick the real super method instead of just the first one,
        // if there is a match
        constructor.superConstructor?.let { superMethod ->
            if (filterEmit.test(superMethod)) {
                writeConstructor(constructor, superMethod)
                return
            }
        }

        // Attempt to match it via indirect hidden intermediaries
        val publicSuperClass = containingClass.publicSuperClass()
        if (publicSuperClass != null) {
            val publicSuperClassConstructors = publicSuperClass.constructors()

            // Try to pick a constructor that is at least public
            for (superConstructor in publicSuperClassConstructors) {
                if (filterEmit.test(superConstructor)) {
                    writeConstructor(constructor, superConstructor)
                    return
                }
            }
        }

        writeConstructor(constructor, publicSuperClass?.constructors()?.firstOrNull())
    }

    private fun writeConstructor(
        constructor: MethodItem,
        superConstructor: MethodItem?
    ) {
        writer.println()
        appendDocumentation(constructor, writer)
        appendModifiers(constructor, false)
        generateTypeParameterList(
            typeList = constructor.typeParameterList(),
            addSpace = true
        )
        writer.print(constructor.containingClass().simpleName())

        generateParameterList(constructor)
        generateThrowsList(constructor)

        writer.print(" { ")

        if (superConstructor != null && !filterReference.test(superConstructor)) {
            // Using real signature from public constructor but the super constructor is not
            // available: that means it might have an unexpected set of exceptions that we don't
            // catch here, so prepare for the worst
            writeThrowStub()
            writer.println(" }")
            return
        }


        writeConstructorBody(constructor, superConstructor)
        writer.println(" }")
    }

    private fun writeConstructorBody(constructor: MethodItem?, superConstructor: MethodItem?) {
        // Find any constructor in parent that we can compile against
        superConstructor?.let { it ->
            val parameters = it.parameters()
            val invokeOnThis = constructor != null && constructor.containingClass() == it.containingClass()
            if (invokeOnThis || parameters.isNotEmpty()) {
                val includeCasts = parameters.isNotEmpty() &&
                        it.containingClass().constructors().filter { filterReference.test(it) }.size > 1
                if (invokeOnThis) {
                    writer.print("this(")
                } else {
                    writer.print("super(")
                }
                parameters.forEachIndexed { index, parameter ->
                    if (index > 0) {
                        writer.write(", ")
                    }
                    val type = parameter.type()
                    val typeString = type.toErasedTypeString()
                    if (!type.primitive) {
                        if (includeCasts) {
                            writer.write("(")

                            // Types with varargs can't appear as varargs when used as an argument
                            if (typeString.contains("...")) {
                                writer.write(typeString.replace("...", "[]"))
                            } else {
                                writer.write(typeString)
                            }
                            writer.write(")")
                        }
                        writer.write("null")

                    } else {
                        if (typeString != "boolean" && typeString != "int" && typeString != "long") {
                            writer.write("(")
                            writer.write(typeString)
                            writer.write(")")
                        }
                        writer.write(type.defaultValueString())
                    }
                }
                writer.print("); ")
            }
        }

        writeThrowStub()
    }

    private fun generateMissingConstructors(cls: ClassItem) {
        val clsDefaultConstructor = cls.defaultConstructor
        if (clsDefaultConstructor != null && !cls.constructors().contains(clsDefaultConstructor)) {
            visitConstructor(clsDefaultConstructor)
            return
        }

        if (cls.isClass()) {
            val constructors = cls.constructors().asSequence().filter { filterEmit.test(it) }
            if (constructors.none() && cls.hasPrivateConstructor) {
                val superConstructor = cls.filteredSuperclass(filterEmit)?.defaultConstructor
                if (superConstructor != null) {
                    generateTypeParameterList(typeList = superConstructor.typeParameterList(), addSpace = true)
                }

                // Not writing modifiers: leave package private since this wasn't a public constructor
                writer.print(cls.simpleName())

                if (superConstructor != null) {
                    // What if the super constructor isn't public? In that case it may reference hidden
                    // types that we don't include stubs for!
                    generateParameterList(superConstructor)
                    generateThrowsList(superConstructor)
                    writer.print(" { ")

                    // TODO: I need to be careful here: what if the cast types are hidden?
                    writeConstructorBody(null, superConstructor)
                } else {
                    writer.print("() { ")
                    writeThrowStub()
                }
                //writeConstructorBody(superClass.constructors().first())
                writer.println(" }")
            }
        }
    }

    override fun visitMethod(method: MethodItem) {
        writeMethod(method.containingClass(), method, false)
    }

    private fun writeMethod(containingClass: ClassItem, method: MethodItem, movedFromInterface: Boolean) {
        val modifiers = method.modifiers
        val isEnum = containingClass.isEnum()
        val isAnnotation = containingClass.isAnnotationType()

        if (isEnum && (method.name() == "values" ||
                    method.name() == "valueOf" && method.parameters().size == 1 &&
                    method.parameters()[0].type().toString() == "java.lang.String")) {
            // Skip the values() and valueOf(String) methods in enums: these are added by
            // the compiler for enums anyway, but was part of the doclava1 signature files
            // so inserted in compat mode.
            return
        }

        writer.println()
        appendDocumentation(method, writer)

        // Need to filter out abstract from the modifiers list and turn it
        // into a concrete method to make the stub compile
        val removeAbstract = modifiers.isAbstract() && (isEnum || isAnnotation) || movedFromInterface

        appendModifiers(method, modifiers, removeAbstract, movedFromInterface)
        generateTypeParameterList(typeList = method.typeParameterList(), addSpace = true)

        val returnType = method.returnType()
        writer.print(returnType?.toString())

        writer.print(' ')
        writer.print(method.name())
        generateParameterList(method)
        generateThrowsList(method)

        if (modifiers.isAbstract() && !removeAbstract && !isEnum || isAnnotation || modifiers.isNative()) {
            writer.println(";")
        } else {
            writer.print(" { ")
            writeThrowStub()
            writer.println(" }")
        }
    }

    override fun visitField(field: FieldItem) {
        // Handled earlier in visitClass
        if (field.isEnumConstant()) {
            return
        }

        writer.println()

        appendDocumentation(field, writer)
        appendModifiers(field, false, false)
        writer.print(field.type().toString())
        writer.print(' ')
        writer.print(field.name())
        val needsInitialization = field.modifiers.isFinal() && field.initialValue(true) == null && field.containingClass().isClass()
        field.writeValueWithSemicolon(writer, allowDefaultValue = !needsInitialization, requireInitialValue = !needsInitialization)
        writer.print("\n")

        if (needsInitialization) {
            if (field.modifiers.isStatic()) {
                writer.print("static ")
            }
            writer.print("{ ${field.name()} = ${field.type().defaultValueString()}; }\n")
        }
    }

    private fun writeThrowStub() {
        writer.write("throw new RuntimeException(\"Stub!\");")
    }

    private fun generateParameterList(method: MethodItem) {
        writer.print("(")
        method.parameters().asSequence().forEachIndexed { i, parameter ->
            if (i > 0) {
                writer.print(", ")
            }
            appendModifiers(parameter, false)
            writer.print(parameter.type().toString())
            writer.print(' ')
            val name = parameter.publicName() ?: parameter.name()
            writer.print(name)
        }
        writer.print(")")
    }

    private fun generateThrowsList(method: MethodItem) {
        // Note that throws types are already sorted internally to help comparison matching
        val throws = if (prefiltered)
            method.throwsTypes().asSequence()
        else
            method.throwsTypes().asSequence().filter { filterReference.test(it) }
        if (throws.any()) {
            writer.print(" throws ")
            throws.asSequence().sortedWith(ClassItem.fullNameComparator).forEachIndexed { i, type ->
                if (i > 0) {
                    writer.print(", ")
                }
                // TODO: Shouldn't declare raw types here!
                writer.print(type.qualifiedName())
            }
        }
    }
}