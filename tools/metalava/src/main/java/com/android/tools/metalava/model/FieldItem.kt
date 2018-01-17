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
import java.io.PrintWriter

interface FieldItem : MemberItem {
    /** The type of this field */
    fun type(): TypeItem

    /**
     * The initial/constant value, if any. If [requireConstant] the initial value will
     * only be returned if it's constant.
     */
    fun initialValue(requireConstant: Boolean = true): Any?

    /**
     * An enum can contain both enum constants and fields; this method provides a way
     * to distinguish between them.
     */
    fun isEnumConstant(): Boolean

    /**
     * Duplicates this field item. Used when we need to insert inherited fields from
     * interfaces etc.
     */
    fun duplicate(targetContainingClass: ClassItem): FieldItem

    override fun accept(visitor: ItemVisitor) {
        if (visitor.skip(this)) {
            return
        }

        visitor.visitItem(this)
        visitor.visitField(this)

        visitor.afterVisitField(this)
        visitor.afterVisitItem(this)
    }

    override fun acceptTypes(visitor: TypeVisitor) {
        if (visitor.skip(this)) {
            return
        }

        val type = type()
        visitor.visitType(type, this)
        visitor.afterVisitType(type, this)
    }

    companion object {
        val comparator: java.util.Comparator<FieldItem> = Comparator { a, b -> a.name().compareTo(b.name()) }
    }

    /**
     * If this field has an initial value, it just writes ";", otherwise it writes
     * " = value;" with the correct Java syntax for the initial value
     */
    fun writeValueWithSemicolon(
        writer: PrintWriter,
        allowDefaultValue: Boolean = false,
        requireInitialValue: Boolean = false
    ) {
        val value =
            initialValue(!allowDefaultValue) ?: if (allowDefaultValue && !containingClass().isClass()) type().defaultValue() else null
        if (value != null) {
            when (value) {
                is Int -> {
                    writer.print(" = ")
                    writer.print(value)
                    writer.print("; // 0x")
                    writer.print(Integer.toHexString(value))
                }
                is String -> {
                    writer.print(" = ")
                    writer.print('"')
                    writer.print(javaEscapeString(value))
                    writer.print('"')
                    writer.print(";")
                }
                is Long -> {
                    writer.print(" = ")
                    writer.print(value)
                    writer.print(String.format("L; // 0x%xL", value))
                }
                is Boolean -> {
                    writer.print(" = ")
                    writer.print(value)
                    writer.print(";")
                }
                is Byte -> {
                    writer.print(" = ")
                    writer.print(value)
                    writer.print("; // 0x")
                    writer.print(Integer.toHexString(value.toInt()))
                }
                is Short -> {
                    writer.print(" = ")
                    writer.print(value)
                    writer.print("; // 0x")
                    writer.print(Integer.toHexString(value.toInt()))
                }
                is Float -> {
                    writer.print(" = ")
                    when (value) {
                        Float.POSITIVE_INFINITY -> writer.print("(1.0f/0.0f);")
                        Float.NEGATIVE_INFINITY -> writer.print("(-1.0f/0.0f);")
                        Float.NaN -> writer.print("(0.0f/0.0f);")
                        else -> {
                            writer.print(canonicalizeFloatingPointString(value.toString()))
                            writer.print("f;")
                        }
                    }
                }
                is Double -> {
                    writer.print(" = ")
                    when (value) {
                        Double.POSITIVE_INFINITY -> writer.print("(1.0/0.0);")
                        Double.NEGATIVE_INFINITY -> writer.print("(-1.0/0.0);")
                        Double.NaN -> writer.print("(0.0/0.0);")
                        else -> {
                            writer.print(canonicalizeFloatingPointString(value.toString()))
                            writer.print(";")
                        }
                    }
                }
                is Char -> {
                    writer.print(" = ")
                    val intValue = value.toInt()
                    writer.print(intValue)
                    writer.print("; // ")
                    writer.print(
                        String.format(
                            "0x%04x '%s'", intValue,
                            javaEscapeString(value.toString())
                        )
                    )
                }
                else -> {
                    writer.print(';')
                }
            }
        } else {
            // in interfaces etc we must have an initial value
            if (requireInitialValue && !containingClass().isClass()) {
                writer.print(" = null")
            }
            writer.print(';')
        }
    }
}

fun javaEscapeString(str: String): String {
    var result = ""
    val n = str.length
    for (i in 0 until n) {
        val c = str[i]
        result += when (c) {
            '\\' -> "\\\\"
            '\t' -> "\\t"
            '\b' -> "\\b"
            '\r' -> "\\r"
            '\n' -> "\\n"
            '\'' -> "\\'"
            '\"' -> "\\\""
            in ' '..'~' -> c
            else -> String.format("\\u%04x", c.toInt())
        }
    }
    return result
}

// From doclava1 TextFieldItem#javaUnescapeString
fun javaUnescapeString(str: String): String {
    val n = str.length
    var simple = true
    for (i in 0 until n) {
        val c = str[i]
        if (c == '\\') {
            simple = false
            break
        }
    }
    if (simple) {
        return str
    }

    val buf = StringBuilder(str.length)
    var escaped: Char = 0.toChar()
    val START = 0
    val CHAR1 = 1
    val CHAR2 = 2
    val CHAR3 = 3
    val CHAR4 = 4
    val ESCAPE = 5
    var state = START

    for (i in 0 until n) {
        val c = str[i]
        when (state) {
            START -> if (c == '\\') {
                state = ESCAPE
            } else {
                buf.append(c)
            }
            ESCAPE -> when (c) {
                '\\' -> {
                    buf.append('\\')
                    state = START
                }
                't' -> {
                    buf.append('\t')
                    state = START
                }
                'b' -> {
                    buf.append('\b')
                    state = START
                }
                'r' -> {
                    buf.append('\r')
                    state = START
                }
                'n' -> {
                    buf.append('\n')
                    state = START
                }
                '\'' -> {
                    buf.append('\'')
                    state = START
                }
                '\"' -> {
                    buf.append('\"')
                    state = START
                }
                'u' -> {
                    state = CHAR1
                    escaped = 0.toChar()
                }
            }
            CHAR1, CHAR2, CHAR3, CHAR4 -> {

                escaped = (escaped.toInt() shl 4).toChar()
                escaped = when (c) {
                    in '0'..'9' -> (escaped.toInt() or (c - '0')).toChar()
                    in 'a'..'f' -> (escaped.toInt() or (10 + (c - 'a'))).toChar()
                    in 'A'..'F' -> (escaped.toInt() or (10 + (c - 'A'))).toChar()
                    else -> throw IllegalArgumentException(
                        "bad escape sequence: '" + c + "' at pos " + i + " in: \""
                                + str + "\""
                    )
                }
                if (state == CHAR4) {
                    buf.append(escaped)
                    state = START
                } else {
                    state++
                }
            }
        }
    }
    if (state != START) {
        throw IllegalArgumentException("unfinished escape sequence: " + str)
    }
    return buf.toString()
}

/**
 * Returns a canonical string representation of a floating point
 * number. The representation is suitable for use as Java source
 * code. This method also addresses bug #4428022 in the Sun JDK.
 */
// From doclava1
fun canonicalizeFloatingPointString(value: String): String {
    var str = value
    if (str.indexOf('E') != -1) {
        return str
    }

    // 1.0 is the only case where a trailing "0" is allowed.
    // 1.00 is canonicalized as 1.0.
    var i = str.length - 1
    val d = str.indexOf('.')
    while (i >= d + 2 && str[i] == '0') {
        str = str.substring(0, i--)
    }
    return str
}
