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

import com.android.tools.metalava.Options
import com.android.tools.metalava.compatibility
import com.android.tools.metalava.options
import java.io.Writer

interface ModifierList {
    val codebase: Codebase
    fun annotations(): List<AnnotationItem>

    fun owner(): Item
    fun isPublic(): Boolean
    fun isProtected(): Boolean
    fun isPrivate(): Boolean
    fun isStatic(): Boolean
    fun isAbstract(): Boolean
    fun isFinal(): Boolean
    fun isNative(): Boolean
    fun isSynchronized(): Boolean
    fun isStrictFp(): Boolean
    fun isTransient(): Boolean
    fun isVolatile(): Boolean
    fun isDefault(): Boolean

    fun isEmpty(): Boolean

    fun isPackagePrivate() = !(isPublic() || isProtected() || isPrivate())

    // Rename? It's not a full equality, it's whether an override's modifier set is significant
    fun equivalentTo(other: ModifierList): Boolean {
        if (isPublic() != other.isPublic()) return false
        if (isProtected() != other.isProtected()) return false
        if (isPrivate() != other.isPrivate()) return false

        if (isStatic() != other.isStatic()) return false
        if (isAbstract() != other.isAbstract()) return false
        if (isFinal() != other.isFinal()) return false
        if (!compatibility.skipNativeModifier && isNative() != other.isNative()) return false
        if (isSynchronized() != other.isSynchronized()) return false
        if (!compatibility.skipStrictFpModifier && isStrictFp() != other.isStrictFp()) return false
        if (isTransient() != other.isTransient()) return false
        if (isVolatile() != other.isVolatile()) return false

        // Default does not require an override to "remove" it
        //if (isDefault() != other.isDefault()) return false

        return true
    }

    /** Returns true if this modifier list contains any nullness information */
    fun hasNullnessInfo(): Boolean {
        return annotations().any { it.isNonNull() || it.isNullable() }
    }

    /**
     * Returns true if this modifier list contains any annotations explicitly passed in
     * via [Options.showAnnotations]
     */
    fun hasShowAnnotation(): Boolean {
        if (options.showAnnotations.isEmpty()) {
            return false
        }
        return annotations().any {
            options.showAnnotations.contains(it.qualifiedName())
        }
    }

    /**
     * Returns true if this modifier list contains any annotations explicitly passed in
     * via [Options.hideAnnotations]
     */
    fun hasHideAnnotations(): Boolean {
        if (options.hideAnnotations.isEmpty()) {
            return false
        }
        return annotations().any {
            options.hideAnnotations.contains(it.qualifiedName())
        }
    }

    /** Returns true if this modifier list contains the given annotation */
    fun isAnnotatedWith(qualifiedName: String): Boolean {
        return findAnnotation(qualifiedName) != null
    }

    /** Returns the annotation of the given qualified name if found in this modifier list */
    fun findAnnotation(qualifiedName: String): AnnotationItem? {
        val mappedName = AnnotationItem.mapName(codebase, qualifiedName)
        return annotations().firstOrNull {
            mappedName == it.qualifiedName()
        }
    }

    companion object {
        fun write(
            writer: Writer,
            modifiers: ModifierList,
            item: Item,
            // TODO: "deprecated" isn't a modifier; clarify method name
            includeDeprecated: Boolean = false,
            includeAnnotations: Boolean = true,
            skipNullnessAnnotations: Boolean = false,
            omitCommonPackages: Boolean = false,
            removeAbstract: Boolean = false,
            removeFinal: Boolean = false,
            addPublic: Boolean = false
        ) {

            val list = if (removeAbstract || removeFinal || addPublic) {
                class AbstractFiltering : ModifierList by modifiers {
                    override fun isAbstract(): Boolean {
                        return if (removeAbstract) false else modifiers.isAbstract()
                    }

                    override fun isFinal(): Boolean {
                        return if (removeFinal) false else modifiers.isFinal()
                    }

                    override fun isPublic(): Boolean {
                        return if (addPublic) true else modifiers.isPublic()
                    }
                }
                AbstractFiltering()
            } else {
                modifiers
            }

            if (includeAnnotations && list.annotations().isNotEmpty()) {
                for (annotation in list.annotations()) {
                    if ((annotation.isNonNull() || annotation.isNullable())) {
                        if (skipNullnessAnnotations) {
                            continue
                        }
                    } else if (!annotation.isSignificant()) {
                        continue
                    }
                    val source = annotation.toSource()
                    if (omitCommonPackages) {
                        writer.write(AnnotationItem.shortenAnnotation(source))
                    } else {
                        writer.write(source)
                    }
                    writer.write(" ")
                }
            }

            // Abstract: should appear in interfaces if in compat mode
            val classItem = item as? ClassItem
            val methodItem = item as? MethodItem

            // Order based on the old stubs code: TODO, use Java standard order instead?

            if (compatibility.nonstandardModifierOrder) {
                when {
                    list.isPublic() -> writer.write("public ")
                    list.isProtected() -> writer.write("protected ")
                    list.isPrivate() -> writer.write("private ")
                }

                if (list.isDefault()) {
                    writer.write("default ")
                }

                if (list.isStatic()) {
                    writer.write("static ")
                }

                if (list.isFinal() &&
                    // Don't show final on parameters: that's an implementation side detail
                    item !is ParameterItem &&
                    (classItem?.isEnum() != true || compatibility.finalInInterfaces) ||
                    compatibility.forceFinalInEnumValueMethods &&
                    methodItem?.name() == "values" && methodItem.containingClass().isEnum()) {
                    writer.write("final ")
                }

                val isInterface = classItem?.isInterface() == true
                        || (methodItem?.containingClass()?.isInterface() == true &&
                        !list.isDefault() && !list.isStatic())

                if ((compatibility.abstractInInterfaces && isInterface
                            || list.isAbstract() &&
                            (classItem?.isEnum() != true &&
                                    (compatibility.abstractInAnnotations || classItem?.isAnnotationType() != true)))
                    && (!isInterface || compatibility.abstractInInterfaces)) {
                    writer.write("abstract ")
                }

                if (!compatibility.skipNativeModifier && list.isNative()) {
                    writer.write("native ")
                }

                if (item.deprecated && includeDeprecated) {
                    writer.write("deprecated ")
                }

                if (list.isSynchronized()) {
                    writer.write("synchronized ")
                }

                if (!compatibility.skipStrictFpModifier && list.isStrictFp()) {
                    writer.write("strictfp ")
                }

                if (list.isTransient()) {
                    writer.write("transient ")
                }

                if (list.isVolatile()) {
                    writer.write("volatile ")
                }
            } else {
                if (item.deprecated && includeDeprecated) {
                    writer.write("deprecated ")
                }

                when {
                    list.isPublic() -> writer.write("public ")
                    list.isProtected() -> writer.write("protected ")
                    list.isPrivate() -> writer.write("private ")
                }

                val isInterface = classItem?.isInterface() == true
                        || (methodItem?.containingClass()?.isInterface() == true &&
                        !list.isDefault() && !list.isStatic())

                if ((compatibility.abstractInInterfaces && isInterface
                            || list.isAbstract() &&
                            (classItem?.isEnum() != true &&
                                    (compatibility.abstractInAnnotations || classItem?.isAnnotationType() != true)))
                    && (!isInterface || compatibility.abstractInInterfaces)) {
                    writer.write("abstract ")
                }

                if (list.isDefault() && item !is ParameterItem) {
                    writer.write("default ")
                }

                if (list.isStatic()) {
                    writer.write("static ")
                }

                if (list.isFinal() &&
                    // Don't show final on parameters: that's an implementation side detail
                    item !is ParameterItem &&
                    (classItem?.isEnum() != true || compatibility.finalInInterfaces)) {
                    writer.write("final ")
                }

                if (list.isTransient()) {
                    writer.write("transient ")
                }

                if (list.isVolatile()) {
                    writer.write("volatile ")
                }

                if (list.isSynchronized()) {
                    writer.write("synchronized ")
                }

                if (!compatibility.skipNativeModifier && list.isNative()) {
                    writer.write("native ")
                }

                if (!compatibility.skipStrictFpModifier && list.isStrictFp()) {
                    writer.write("strictfp ")
                }
            }
        }
    }
}

