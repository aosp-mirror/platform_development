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

import com.android.tools.metalava.compatibility

/** Represents a type */
interface TypeItem {
    override fun toString(): String

    fun toErasedTypeString(): String

    fun toFullyQualifiedString(): String

    fun asClass(): ClassItem?

    fun toSimpleType(): String {
        return toString().replace("java.lang.", "")
    }

    val primitive: Boolean

    fun typeArgumentClasses(): List<ClassItem>

    fun convertType(from: ClassItem, to: ClassItem): TypeItem {
        val map = from.mapTypeVariables(to)
        if (!map.isEmpty()) {
            return convertType(map)
        }

        return this
    }

    fun convertType(replacementMap: Map<String, String>?, owner: Item? = null): TypeItem

    fun convertTypeString(replacementMap: Map<String, String>?): String {
        return convertTypeString(toString(), replacementMap)
    }

    fun isJavaLangObject(): Boolean {
        return toString() == "java.lang.Object"
    }

    fun defaultValue(): Any? {
        return when (toString()) {
            "boolean" -> false
            "byte" -> 0.toByte()
            "char" -> '\u0000'
            "short" -> 0.toShort()
            "int" -> 0
            "long" -> 0L
            "float" -> 0f
            "double" -> 0.0
            else -> null
        }
    }

    fun defaultValueString(): String = defaultValue()?.toString() ?: "null"

    companion object {
        fun formatType(type: String?): String {
            if (type == null) {
                return ""
            }
            if (compatibility.spacesAfterCommas && type.indexOf(',') != -1) {
                // The compat files have spaces after commas where we normally don't
                return type.replace(",", ", ").replace(",  ", ", ")
            }

            return cleanupGenerics(type)
        }

        fun cleanupGenerics(signature: String): String {
            // <T extends java.lang.Object> is the same as <T>
            //  but NOT for <T extends Object & java.lang.Comparable> -- you can't
            //  shorten this to <T & java.lang.Comparable
            //return type.replace(" extends java.lang.Object", "")
            return signature.replace(" extends java.lang.Object>", ">")

        }

        val comparator: Comparator<TypeItem> = Comparator { type1, type2 ->
            val cls1 = type1.asClass()
            val cls2 = type2.asClass()
            if (cls1 != null && cls2 != null) {
                ClassItem.fullNameComparator.compare(cls1, cls2)
            } else {
                type1.toString().compareTo(type2.toString())
            }
        }

        fun convertTypeString(typeString: String, replacementMap: Map<String, String>?): String {
            var string = typeString
            if (replacementMap != null && replacementMap.isNotEmpty()) {
                // This is a moved method (typically an implementation of an interface
                // method provided in a hidden superclass), with generics signatures.
                // We need to rewrite the generics variables in case they differ
                // between the classes.
                if (!replacementMap.isEmpty()) {
                    replacementMap.forEach { from, to ->
                        // We can't just replace one string at a time:
                        // what if I have a map of {"A"->"B", "B"->"C"} and I tried to convert A,B,C?
                        // If I do the replacements one letter at a time I end up with C,C,C; if I do the substitutions
                        // simultaneously I get B,C,C. Therefore, we insert "___" as a magical prefix to prevent
                        // scenarios like this, and then we'll drop them afterwards.
                        string = string.replace(Regex(pattern = """\b$from\b"""), replacement = "___$to")
                    }
                }
                string = string.replace("___", "")
                return string
            } else {
                return string
            }
        }
    }

    fun hasTypeArguments(): Boolean = toString().contains("<")

    fun isTypeParameter(): Boolean = toString().length == 1 // heuristic; accurate implementation in PSI subclass
}