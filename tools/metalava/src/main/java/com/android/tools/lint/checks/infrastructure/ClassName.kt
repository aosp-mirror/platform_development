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

package com.android.tools.lint.checks.infrastructure

// ------------------------------------------------------------------------------------------
// ------------------------------------------------------------------------------------------
//
// Copy from lint; temporarily included in metalava sources since we need the latest
// version (from lint 3.1) which isn't available on maven.google.com yet. Delete this
// and replace with direct usage once it is.
//
// ------------------------------------------------------------------------------------------
// ------------------------------------------------------------------------------------------

import java.util.regex.Pattern

/** A pair of package name and class name inferred from Java or Kotlin source code */
class ClassName(source: String) {
    val packageName: String?
    val className: String?

    init {
        val withoutComments = stripComments(source)
        packageName = getPackage(withoutComments)
        className = getClassName(withoutComments)
    }

    fun packageNameWithDefault() = packageName ?: ""
}

/**
 * Strips line and block comments from the given Java or Kotlin source file
 */
@Suppress("LocalVariableName")
fun stripComments(source: String, stripLineComments: Boolean = true): String {
    val sb = StringBuilder(source.length)
    var state = 0
    val INIT = 0
    val INIT_SLASH = 1
    val LINE_COMMENT = 2
    val BLOCK_COMMENT = 3
    val BLOCK_COMMENT_ASTERISK = 4
    val IN_STRING = 5
    val IN_STRING_ESCAPE = 6
    val IN_CHAR = 7
    val AFTER_CHAR = 8
    for (i in 0 until source.length) {
        val c = source[i]
        when (state) {
            INIT -> {
                when (c) {
                    '/' -> state = INIT_SLASH
                    '"' -> {
                        state = IN_STRING
                        sb.append(c)
                    }
                    '\'' -> {
                        state = IN_CHAR
                        sb.append(c)
                    }
                    else -> sb.append(c)
                }
            }
            INIT_SLASH -> {
                when {
                    c == '*' -> state = BLOCK_COMMENT
                    c == '/' && stripLineComments -> state = LINE_COMMENT
                    else -> {
                        state = INIT
                        sb.append('/') // because we skipped it in init
                        sb.append(c)
                    }
                }
            }
            LINE_COMMENT -> {
                when (c) {
                    '\n' -> state = INIT
                }
            }
            BLOCK_COMMENT -> {
                when (c) {
                    '*' -> state = BLOCK_COMMENT_ASTERISK
                }
            }
            BLOCK_COMMENT_ASTERISK -> {
                state = when (c) {
                    '/' -> INIT
                    '*' -> BLOCK_COMMENT_ASTERISK
                    else -> BLOCK_COMMENT
                }
            }
            IN_STRING -> {
                when (c) {
                    '\\' -> state = IN_STRING_ESCAPE
                    '"' -> state = INIT
                }
                sb.append(c)
            }
            IN_STRING_ESCAPE -> {
                sb.append(c)
                state = IN_STRING
            }
            IN_CHAR -> {
                if (c != '\\') {
                    state = AFTER_CHAR
                }
                sb.append(c)
            }
            AFTER_CHAR -> {
                sb.append(c)
                if (c == '\\') {
                    state = INIT
                }
            }
        }
    }

    return sb.toString()
}

private val PACKAGE_PATTERN = Pattern.compile("""package\s+([\S&&[^;]]*)""")

private val CLASS_PATTERN = Pattern.compile("""(class|interface|enum|object)+?\s*([^\s:(]+)""",
        Pattern.MULTILINE)

fun getPackage(source: String): String? {
    val matcher = PACKAGE_PATTERN.matcher(source)
    return if (matcher.find()) {
        matcher.group(1).trim { it <= ' ' }
    } else {
        null
    }
}

fun getClassName(source: String): String? {
    val matcher = CLASS_PATTERN.matcher(source.replace('\n', ' '))
    var start = 0
    while (matcher.find(start)) {
        val cls = matcher.group(2)
        val groupStart = matcher.start(2)

        // Make sure this "class" reference isn't part of an annotation on the class
        // referencing a class literal -- Foo.class, or in Kotlin, Foo::class.java)
        if (groupStart == 0 || source[groupStart-1] != '.' && source[groupStart-1] != ':') {
            val trimmed = cls.trim { it <= ' ' }
            val typeParameter = trimmed.indexOf('<')
            return if (typeParameter != -1) {
                trimmed.substring(0, typeParameter)
            } else {
                trimmed
            }
        }
        start = matcher.end(2)
    }

    return null
}
