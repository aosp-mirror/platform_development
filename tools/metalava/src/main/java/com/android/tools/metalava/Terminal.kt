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

import java.io.PrintWriter

enum class TerminalColor(val value: Int) {
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7)
}

fun terminalAttributes(
    bold: Boolean = false, underline: Boolean = false, reverse: Boolean = false,
    foreground: TerminalColor? = null, background: TerminalColor? = null
): String {
    val sb = StringBuilder()
    sb.append("\u001B[")
    if (foreground != null) {
        sb.append('3').append('0' + foreground.value)
    }
    if (background != null) {
        if (sb.last().isDigit())
            sb.append(';')
        sb.append('4').append('0' + background.value)
    }

    if (bold) {
        if (sb.last().isDigit())
            sb.append(';')
        sb.append('1')
    }
    if (underline) {
        if (sb.last().isDigit())
            sb.append(';')
        sb.append('4')
    }
    if (reverse) {
        if (sb.last().isDigit())
            sb.append(';')
        sb.append('7')
    }
    if (sb.last() == '[') {
        // Nothing: Reset
        sb.append('0')
    }
    sb.append("m")
    return sb.toString()
}

fun resetTerminal(): String {
    return "\u001b[0m"
}

fun colorized(string: String, color: TerminalColor): String {
    return "${terminalAttributes(foreground = color)}$string${resetTerminal()}"
}

fun bold(string: String): String {
    return "${terminalAttributes(bold = true)}$string${resetTerminal()}"
}

fun PrintWriter.terminalPrint(
    string: String,
    bold: Boolean = false, underline: Boolean = false, reverse: Boolean = false,
    foreground: TerminalColor? = null, background: TerminalColor? = null
) {
    print(
        terminalAttributes(
            bold = bold, underline = underline, reverse = reverse, foreground = foreground,
            background = background
        )
    )
    print(string)
    print(resetTerminal())
}
