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

import com.android.SdkConstants
import com.android.SdkConstants.ATTR_VALUE
import com.android.tools.metalava.NEWLY_NONNULL
import com.android.tools.metalava.NEWLY_NULLABLE
import com.android.tools.metalava.RECENTLY_NONNULL
import com.android.tools.metalava.RECENTLY_NULLABLE
import com.android.tools.metalava.options

fun isNullableAnnotation(qualifiedName: String): Boolean {
    return qualifiedName.endsWith("Nullable")
}

fun isNonNullAnnotation(qualifiedName: String): Boolean {
    return qualifiedName.endsWith("NonNull") ||
            qualifiedName.endsWith("NotNull") ||
            qualifiedName.endsWith("Nonnull")
}

interface AnnotationItem {
    val codebase: Codebase

    /** Fully qualified name of the annotation */
    fun qualifiedName(): String?

    /** Generates source code for this annotation (using fully qualified names) */
    fun toSource(): String

    /** Whether this annotation is significant and should be included in signature files, stubs, etc */
    fun isSignificant(): Boolean {
        return isSignificantAnnotation(qualifiedName() ?: return false)
    }

    /** Attributes of the annotation (may be empty) */
    fun attributes(): List<AnnotationAttribute>

    /** True if this annotation represents @Nullable or @NonNull (or some synonymous annotation) */
    fun isNullnessAnnotation(): Boolean {
        return isNullable() || isNonNull()
    }

    /** True if this annotation represents @Nullable (or some synonymous annotation) */
    fun isNullable(): Boolean {
        return isNullableAnnotation(qualifiedName() ?: return false)
    }

    /** True if this annotation represents @NonNull (or some synonymous annotation) */
    fun isNonNull(): Boolean {
        return isNonNullAnnotation(qualifiedName() ?: return false)
    }

    /**
     * True if this annotation represents a @ParameterName annotation (or some synonymous annotation).
     * The parameter name should be the default atttribute or "value".
     */
    fun isParameterName(): Boolean {
        return qualifiedName()?.endsWith(".ParameterName") ?: return false
    }

    /** Returns the given named attribute if specified */
    fun findAttribute(name: String?): AnnotationAttribute? {
        val actualName = name ?: ATTR_VALUE
        return attributes().firstOrNull { it.name == actualName }
    }

    /** Find the class declaration for the given annotation */
    fun resolve(): ClassItem? {
        return codebase.findClass(qualifiedName() ?: return null)
    }

    companion object {
        /** Whether the given annotation name is "significant", e.g. should be included in signature files */
        fun isSignificantAnnotation(qualifiedName: String?): Boolean {
            return qualifiedName?.startsWith("android.support.annotation.") ?: false
        }

        /** The simple name of an annotation, which is the annotation name (not qualified name) prefixed by @ */
        fun simpleName(item: AnnotationItem): String {
            val qualifiedName = item.qualifiedName() ?: return ""
            return "@${qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1)}"
        }

        /**
         * Maps an annotation name to the name to be used in signatures/stubs/external annotation files.
         * Annotations that should not be exported are mapped to null.
         */
        fun mapName(codebase: Codebase, qualifiedName: String?): String? {
            qualifiedName ?: return null

            when (qualifiedName) {
            // Resource annotations
                "android.annotation.AnimRes" -> return "android.support.annotation.AnimRes"
                "android.annotation.AnimatorRes" -> return "android.support.annotation.AnimatorRes"
                "android.annotation.AnyRes" -> return "android.support.annotation.AnyRes"
                "android.annotation.ArrayRes" -> return "android.support.annotation.ArrayRes"
                "android.annotation.AttrRes" -> return "android.support.annotation.AttrRes"
                "android.annotation.BoolRes" -> return "android.support.annotation.BoolRes"
                "android.annotation.ColorRes" -> return "android.support.annotation.ColorRes"
                "android.annotation.DimenRes" -> return "android.support.annotation.DimenRes"
                "android.annotation.DrawableRes" -> return "android.support.annotation.DrawableRes"
                "android.annotation.FontRes" -> return "android.support.annotation.FontRes"
                "android.annotation.FractionRes" -> return "android.support.annotation.FractionRes"
                "android.annotation.IdRes" -> return "android.support.annotation.IdRes"
                "android.annotation.IntegerRes" -> return "android.support.annotation.IntegerRes"
                "android.annotation.InterpolatorRes" -> return "android.support.annotation.InterpolatorRes"
                "android.annotation.LayoutRes" -> return "android.support.annotation.LayoutRes"
                "android.annotation.MenuRes" -> return "android.support.annotation.MenuRes"
                "android.annotation.PluralsRes" -> return "android.support.annotation.PluralsRes"
                "android.annotation.RawRes" -> return "android.support.annotation.RawRes"
                "android.annotation.StringRes" -> return "android.support.annotation.StringRes"
                "android.annotation.StyleRes" -> return "android.support.annotation.StyleRes"
                "android.annotation.StyleableRes" -> return "android.support.annotation.StyleableRes"
                "android.annotation.TransitionRes" -> return "android.support.annotation.TransitionRes"
                "android.annotation.XmlRes" -> return "android.support.annotation.XmlRes"

            // Threading
                "android.annotation.AnyThread" -> return "android.support.annotation.AnyThread"
                "android.annotation.BinderThread" -> return "android.support.annotation.BinderThread"
                "android.annotation.MainThread" -> return "android.support.annotation.MainThread"
                "android.annotation.UiThread" -> return "android.support.annotation.UiThread"
                "android.annotation.WorkerThread" -> return "android.support.annotation.WorkerThread"

            // Colors
                "android.annotation.ColorInt" -> return "android.support.annotation.ColorInt"
                "android.annotation.ColorLong" -> return "android.support.annotation.ColorLong"
                "android.annotation.HalfFloat" -> return "android.support.annotation.HalfFloat"

            // Ranges and sizes
                "android.annotation.FloatRange" -> return "android.support.annotation.FloatRange"
                "android.annotation.IntRange" -> return "android.support.annotation.IntRange"
                "android.annotation.Size" -> return "android.support.annotation.Size"
                "android.annotation.Px" -> return "android.support.annotation.Px"
                "android.annotation.Dimension" -> return "android.support.annotation.Dimension"

            // Null
                "android.annotation.NonNull" -> return "android.support.annotation.NonNull"
                "android.annotation.Nullable" -> return "android.support.annotation.Nullable"
                "libcore.util.NonNull" -> return "android.support.annotation.NonNull"
                "libcore.util.Nullable" -> return "android.support.annotation.Nullable"

            // Typedefs
                "android.annotation.IntDef" -> return "android.support.annotation.IntDef"
                "android.annotation.StringDef" -> return "android.support.annotation.StringDef"

            // Misc
                "android.annotation.CallSuper" -> return "android.support.annotation.CallSuper"
                "android.annotation.CheckResult" -> return "android.support.annotation.CheckResult"
                "android.annotation.RequiresPermission" -> return "android.support.annotation.RequiresPermission"

            // These aren't support annotations, but could/should be:
                "android.annotation.CurrentTimeMillisLong",
                "android.annotation.DurationMillisLong",
                "android.annotation.ElapsedRealtimeLong",
                "android.annotation.UserIdInt",
                "android.annotation.BytesLong",

                    // These aren't support annotations
                "android.annotation.AppIdInt",
                "android.annotation.BroadcastBehavior",
                "android.annotation.SdkConstant",
                "android.annotation.SuppressAutoDoc",
                "android.annotation.SystemApi",
                "android.annotation.TestApi",
                "android.annotation.Widget" -> {
                    // Remove, unless specifically included in --showAnnotations
                    return if (options.showAnnotations.contains(qualifiedName)) {
                        qualifiedName
                    } else {
                        null
                    }
                }

            // Included for analysis, but should not be exported:
                "android.annotation.SystemService" -> return qualifiedName

            // Should not be mapped to a different package name:
                "android.annotation.TargetApi",
                "android.annotation.SuppressLint" -> return qualifiedName

            // We only change recently/newly nullable annotation if the codebase supports it
                NEWLY_NULLABLE, RECENTLY_NULLABLE -> return if (codebase.supportsStagedNullability) qualifiedName else "android.support.annotation.Nullable"
                NEWLY_NONNULL, RECENTLY_NONNULL -> return if (codebase.supportsStagedNullability) qualifiedName else "android.support.annotation.NonNull"

                else -> {
                    // Some new annotations added to the platform: assume they are support annotations?
                    return when {
                    // Special Kotlin annotations recognized by the compiler: map to supported package name
                        qualifiedName.endsWith(".ParameterName") || qualifiedName.endsWith(".DefaultValue") ->
                            "kotlin.annotations.jvm.internal${qualifiedName.substring(qualifiedName.lastIndexOf('.'))}"
                        qualifiedName.startsWith("android.annotation.") -> "android.support.annotation." + qualifiedName.substring("android.annotation.".length)
                    // Other third party nullness annotations?
                        isNullableAnnotation(qualifiedName) -> "android.support.annotation.Nullable"
                        isNonNullAnnotation(qualifiedName) -> "android.support.annotation.NonNull"
                        else -> qualifiedName
                    }
                }
            }
        }

        /**
         * Given a "full" annotation name, shortens it by removing redundant package names.
         * This is intended to be used by the [com.android.tools.metalava.Options.omitCommonPackages] flag
         * to reduce clutter in signature files.
         *
         * For example, this method will convert `@android.support.annotation.Nullable` to just
         * `@Nullable`, and `@android.support.annotation.IntRange(from=20)` to `IntRange(from=20)`.
         */
        fun shortenAnnotation(source: String): String {
            return when {
                source.startsWith("android.annotation.", 1) -> {
                    "@" + source.substring("@android.annotation.".length)
                }
                source.startsWith("android.support.annotation.", 1) -> {
                    "@" + source.substring("@android.support.annotation.".length)
                }
                else -> source
            }
        }

        /**
         * Reverses the [shortenAnnotation] method. Intended for use when reading in signature files
         * that contain shortened type references.
         */
        fun unshortenAnnotation(source: String): String {
            return when {
            // These 3 annotations are in the android.annotation. package, not android.support.annotation
                source.startsWith("@SystemService") ||
                        source.startsWith("@TargetApi") ||
                        source.startsWith("@SuppressLint") ->
                    "@android.annotation." + source.substring(1)
                else -> {
                    "@android.support.annotation." + source.substring(1)
                }
            }
        }
    }
}

/** An attribute of an annotation, such as "value" */
interface AnnotationAttribute {
    /** The name of the annotation */
    val name: String
    /** The annotation value */
    val value: AnnotationAttributeValue

    /**
     * Return all leaf values; this flattens the complication of handling
     * {@code @SuppressLint("warning")} and {@code @SuppressLint({"warning1","warning2"})
     */
    fun leafValues(): List<AnnotationAttributeValue> {
        val result = mutableListOf<AnnotationAttributeValue>()
        AnnotationAttributeValue.addValues(value, result)
        return result
    }
}

/** An annotation value */
interface AnnotationAttributeValue {
    /** Generates source code for this annotation value */
    fun toSource(): String

    /** The value of the annotation */
    fun value(): Any?

    /** If the annotation declaration references a field (or class etc), return the resolved class */
    fun resolve(): Item?

    companion object {
        fun addValues(value: AnnotationAttributeValue, into: MutableList<AnnotationAttributeValue>) {
            if (value is AnnotationArrayAttributeValue) {
                for (v in value.values) {
                    addValues(v, into)
                }
            } else if (value is AnnotationSingleAttributeValue) {
                into.add(value)
            }
        }
    }
}

/** An annotation value (for a single item, not an array) */
interface AnnotationSingleAttributeValue : AnnotationAttributeValue {
    /** The annotation value, expressed as source code */
    val valueSource: String
    /** The annotation value */
    val value: Any?

    override fun value() = value
}

/** An annotation value for an array of items */
interface AnnotationArrayAttributeValue : AnnotationAttributeValue {
    /** The annotation values */
    val values: List<AnnotationAttributeValue>

    override fun resolve(): Item? {
        error("resolve() should not be called on an array value")
    }

    override fun value() = values.mapNotNull { it.value() }.toTypedArray()
}

class DefaultAnnotationAttribute(
    override val name: String,
    override val value: DefaultAnnotationValue
) : AnnotationAttribute {
    companion object {
        fun create(name: String, value: String): DefaultAnnotationAttribute {
            return DefaultAnnotationAttribute(name, DefaultAnnotationValue.create(value))
        }

        fun createList(source: String): List<AnnotationAttribute> {
            val list = mutableListOf<AnnotationAttribute>()
            if (source.contains("{")) {
                assert(source.indexOf('{', source.indexOf('{', source.indexOf('{') + 1) + 1) != -1,
                    { "Multiple arrays not supported: $source" })
                val split = source.indexOf('=')
                val name: String
                val value: String
                if (split == -1) {
                    name = "value"
                    value = source.substring(source.indexOf('{'))
                } else {
                    name = source.substring(0, split).trim()
                    value = source.substring(split + 1).trim()
                }
                list.add(DefaultAnnotationAttribute.create(name, value))
                return list
            }

            source.split(",").forEach { declaration ->
                val split = declaration.indexOf('=')
                val name: String
                val value: String
                if (split == -1) {
                    name = "value"
                    value = declaration.trim()
                } else {
                    name = declaration.substring(0, split).trim()
                    value = declaration.substring(split + 1).trim()
                }
                list.add(DefaultAnnotationAttribute.create(name, value))
            }
            return list
        }
    }
}

abstract class DefaultAnnotationValue : AnnotationAttributeValue {
    companion object {
        fun create(value: String): DefaultAnnotationValue {
            return if (value.startsWith("{")) { // Array
                DefaultAnnotationArrayAttributeValue(value)
            } else {
                DefaultAnnotationSingleAttributeValue(value)
            }
        }
    }

    override fun toString(): String = toSource()
}

class DefaultAnnotationSingleAttributeValue(override val valueSource: String) : DefaultAnnotationValue(), AnnotationSingleAttributeValue {
    override val value = when {
        valueSource == SdkConstants.VALUE_TRUE -> true
        valueSource == SdkConstants.VALUE_FALSE -> false
        valueSource.startsWith("\"") -> valueSource.removeSurrounding("\"")
        valueSource.startsWith('\'') -> valueSource.removeSurrounding("'")[0]
        else -> try {
            if (valueSource.contains(".")) {
                valueSource.toDouble()
            } else {
                valueSource.toLong()
            }
        } catch (e: NumberFormatException) {
            valueSource
        }
    }

    override fun resolve(): Item? = null

    override fun toSource() = valueSource
}

class DefaultAnnotationArrayAttributeValue(val value: String) : DefaultAnnotationValue(), AnnotationArrayAttributeValue {
    init {
        assert(value.startsWith("{") && value.endsWith("}"), { value })
    }

    override val values = value.substring(1, value.length - 1).split(",").map {
        DefaultAnnotationValue.create(it.trim())
    }.toList()

    override fun toSource() = value
}
