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

import com.android.SdkConstants
import com.android.tools.metalava.model.ClassItem
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.FieldItem
import com.android.tools.metalava.model.Item
import com.android.tools.metalava.model.MemberItem
import com.android.tools.metalava.model.MethodItem
import com.android.tools.metalava.model.PackageItem
import com.android.tools.metalava.model.ParameterItem
import com.android.tools.metalava.model.visitors.ApiVisitor
import com.google.common.io.ByteStreams
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.util.zip.ZipFile

const val CLASS_COLUMN_WIDTH = 60
const val COUNT_COLUMN_WIDTH = 16
const val USAGE_REPORT_MAX_ROWS = 15

class AnnotationStatistics(val api: Codebase) {
    /** Measure the coverage statistics for the API */
    fun count() {
        var allMethods = 0
        var annotatedMethods = 0
        var allFields = 0
        var annotatedFields = 0
        var allParameters = 0
        var annotatedParameters = 0

        api.accept(object : ApiVisitor(api) {
            override fun skip(item: Item): Boolean {
                if (options.omitRuntimePackageStats && item is PackageItem) {
                    val name = item.qualifiedName()
                    if (name.startsWith("java.") ||
                        name.startsWith("javax.") ||
                        name.startsWith("kotlin.") ||
                        name.startsWith("kotlinx.")) {
                        return true
                    }
                }
                return super.skip(item)
            }

            override fun visitParameter(parameter: ParameterItem) {
                allParameters++
                if (parameter.modifiers.annotations().any { it.isNonNull() || it.isNullable() }) {
                    annotatedParameters++
                }
            }

            override fun visitField(field: FieldItem) {
                allFields++
                if (field.modifiers.annotations().any { it.isNonNull() || it.isNullable() }) {
                    annotatedFields++
                }
            }

            override fun visitMethod(method: MethodItem) {
                allMethods++
                if (method.modifiers.annotations().any { it.isNonNull() || it.isNullable() }) {
                    annotatedMethods++
                }
            }
        })

        options.stdout.println()
        options.stdout.println(
            """
            Nullness Annotation Coverage Statistics:
            $annotatedMethods out of $allMethods methods were annotated (${percent(annotatedMethods, allMethods)}%)
            $annotatedFields out of $allFields fields were annotated (${percent(annotatedFields, allFields)}%)
            $annotatedParameters out of $allParameters parameters were annotated (${percent(annotatedParameters, allParameters)}%)
            """.trimIndent()
        )
    }

    private fun percent(numerator: Int, denominator: Int): Int {
        return if (denominator == 0) {
            0
        } else {
            numerator * 100 / denominator
        }
    }

    fun measureCoverageOf(classPath: List<File>) {
        val used = HashMap<MemberItem, Int>(1000)

        for (entry in classPath) {
            recordUsages(used, entry, entry.path)
        }

        // Keep only those items where there is at least one un-annotated element in the API
        val filtered = used.keys.filter {
            !it.hasNullnessInfo()
        }

        val referenceCount = used.size
        val missingCount = filtered.size
        val annotatedCount = used.size - filtered.size

        // Sort by descending usage
        val sorted = filtered.sortedWith(Comparator { o1, o2 ->
            // Sort first by descending count, then increasing alphabetical
            val delta = used[o2]!! - used[o1]!!
            if (delta != 0) {
                return@Comparator delta
            }
            o1.toString().compareTo(o2.toString())
        })

        // High level summary
        options.stdout.println()
        options.stdout.println(
            "$missingCount methods and fields were missing nullness annotations out of " +
                    "$referenceCount total API references."
        )
        options.stdout.println("API nullness coverage is ${percent(annotatedCount, referenceCount)}%")
        options.stdout.println()

        reportTopUnannotatedClasses(sorted, used)
        printMemberTable(sorted, used)
    }

    private fun reportTopUnannotatedClasses(sorted: List<MemberItem>, used: HashMap<MemberItem, Int>) {
        // Aggregate class counts
        val classCount = mutableMapOf<Item, Int>()
        for (item in sorted) {
            val containingClass = item.containingClass()
            val itemCount = used[item]!!
            val count = classCount[containingClass]
            if (count == null) {
                classCount[containingClass] = itemCount
            } else {
                classCount[containingClass] = count + itemCount
            }
        }

        // Print out top entries
        val classes = classCount.keys.sortedWith(Comparator { o1, o2 ->
            // Sort first by descending count, then increasing alphabetical
            val delta = classCount[o2]!! - classCount[o1]!!
            if (delta != 0) {
                return@Comparator delta
            }
            o1.toString().compareTo(o2.toString())
        })

        printClassTable(classes, classCount)
    }

    /** Print table in clean Markdown table syntax */
    private fun printTable(
        labelHeader: String,
        countHeader: String,
        items: List<Item>,
        getLabel: (Item) -> String,
        getCount: (Item) -> Int,
        printer: PrintWriter = options.stdout
    ) {
        // Print table in clean Markdown table syntax
        edge(printer, CLASS_COLUMN_WIDTH + 2, COUNT_COLUMN_WIDTH + 2)
        printer.printf(
            "| %-${CLASS_COLUMN_WIDTH}s | %${COUNT_COLUMN_WIDTH}s |\n",
            labelHeader, countHeader
        )
        separator(printer, CLASS_COLUMN_WIDTH + 2, COUNT_COLUMN_WIDTH + 2, rightJustify = true)

        for (i in 0 until items.size) {
            val item = items[i]
            val label = getLabel(item)
            val count = getCount(item)
            printer.printf(
                "| %-${CLASS_COLUMN_WIDTH}s | %${COUNT_COLUMN_WIDTH}d |\n",
                truncate(label, CLASS_COLUMN_WIDTH), count
            )

            if (i == USAGE_REPORT_MAX_ROWS) {
                printer.printf(
                    "| %-${CLASS_COLUMN_WIDTH}s | %${COUNT_COLUMN_WIDTH}s |\n",
                    "... (${items.size - USAGE_REPORT_MAX_ROWS} more items", ""
                )
                break
            }
        }
        edge(printer, CLASS_COLUMN_WIDTH + 2, COUNT_COLUMN_WIDTH + 2)
    }

    private fun printClassTable(classes: List<Item>, classCount: MutableMap<Item, Int>) {
        printTable("Qualified Class Name",
            "Usage Count",
            classes,
            { (it as ClassItem).qualifiedName() },
            { classCount[it]!! })
    }

    private fun printMemberTable(
        sorted: List<MemberItem>, used: HashMap<MemberItem, Int>,
        printer: PrintWriter = options.stdout
    ) {
        // Top APIs
        printer.println("\nTop referenced un-annotated members:\n")

        printTable(
            "Member",
            "Usage Count",
            sorted,
            {
                val member = it as MemberItem
                "${member.containingClass().simpleName()}.${member.name()}${if (member is MethodItem) "(${member.parameters().joinToString {
                    it.type().toSimpleType()
                }})" else ""}"
            },
            { used[it]!! },
            printer
        )
    }

    private fun dashes(printer: PrintWriter, max: Int) {
        for (count in 0 until max) {
            printer.print('-')
        }
    }

    private fun edge(printer: PrintWriter, column1: Int, column2: Int) {
        printer.print("|")
        dashes(printer, column1)
        printer.print("|")
        dashes(printer, column2)
        printer.print("|")
        printer.println()
    }

    private fun separator(printer: PrintWriter, cell1: Int, cell2: Int, rightJustify: Boolean = false) {
        printer.print('|')
        dashes(printer, cell1)
        printer.print('|')
        if (rightJustify) {
            dashes(printer, cell2 - 1)
            // Markdown syntax to force column to be right justified instead of left justified
            printer.print(":|")
        } else {
            dashes(printer, cell2)
            printer.print('|')
        }
        printer.println()
    }

    private fun truncate(string: String, maxLength: Int): String {
        if (string.length < maxLength) {
            return string
        }

        return string.substring(0, maxLength - 3) + "..."
    }

    private fun recordUsages(used: MutableMap<MemberItem, Int>, file: File, path: String) {
        when {
            file.name.endsWith(SdkConstants.DOT_JAR) -> try {
                ZipFile(file).use({ jar ->
                    val enumeration = jar.entries()
                    while (enumeration.hasMoreElements()) {
                        val entry = enumeration.nextElement()
                        if (entry.name.endsWith(SdkConstants.DOT_CLASS)) {
                            try {
                                jar.getInputStream(entry).use({ `is` ->
                                    val bytes = ByteStreams.toByteArray(`is`)
                                    if (bytes != null) {
                                        recordUsages(used, bytes, path + ":" + entry.name)
                                    }
                                })
                            } catch (e: Exception) {
                                options.stdout.println("Could not read jar file entry ${entry.name} from $file: $e")
                            }
                        }
                    }
                })
            } catch (e: IOException) {
                options.stdout.println("Could not read jar file contents from $file: $e")
            }
            file.isDirectory -> {
                val listFiles = file.listFiles()
                listFiles?.forEach {
                    recordUsages(used, it, it.path)
                }
            }
            file.path.endsWith(SdkConstants.DOT_CLASS) -> {
                val bytes = file.readBytes()
                recordUsages(used, bytes, file.path)
            }
            else -> options.stdout.println("Ignoring entry $file")
        }
    }

    private fun recordUsages(used: MutableMap<MemberItem, Int>, bytes: ByteArray, path: String) {
        val reader: ClassReader
        val classNode: ClassNode
        try {
            reader = ClassReader(bytes)
            classNode = ClassNode()
            reader.accept(classNode, 0)
        } catch (t: Throwable) {
            options.stderr.println("Error processing $path: broken class file?")
            return
        }

        val skipJava = options.omitRuntimePackageStats

        for (methodObject in classNode.methods) {
            val method = methodObject as MethodNode
            val nodes = method.instructions
            for (i in 0 until nodes.size()) {
                val instruction = nodes.get(i)
                val type = instruction.type
                if (type == AbstractInsnNode.METHOD_INSN) {
                    val call = instruction as MethodInsnNode
                    if (skipJava && isSkippableOwner(call.owner)) {
                        continue
                    }
                    val item = findMethod(call)
                    item?.let {
                        val count = used[it]
                        if (count == null) {
                            used[it] = 1
                        } else {
                            used[it] = count + 1
                        }
                    }
                } else if (type == AbstractInsnNode.FIELD_INSN) {
                    val field = instruction as FieldInsnNode
                    if (skipJava && isSkippableOwner(field.owner)) {
                        continue
                    }
                    val item = findField(field)
                    item?.let {
                        val count = used[it]
                        if (count == null) {
                            used[it] = 1
                        } else {
                            used[it] = count + 1
                        }
                    }
                }
            }
        }
    }

    private fun isSkippableOwner(owner: String) =
        owner.startsWith("java/") ||
                owner.startsWith("javax/") ||
                owner.startsWith("kotlin") ||
                owner.startsWith("kotlinx/")

    private fun findField(node: FieldInsnNode): FieldItem? {
        val cls = findClass(node.owner) ?: return null
        return cls.findField(node.name)
    }

    private fun findClass(owner: String): ClassItem? {
        val className = owner.replace('/', '.').replace('$', '.')
        return api.findClass(className)
    }

    private fun findMethod(node: MethodInsnNode): MethodItem? {
        val cls = findClass(node.owner) ?: return null
        val types = Type.getArgumentTypes(node.desc)
        val parameters = if (types.isNotEmpty()) {
            val sb = StringBuilder()
            for (type in types) {
                if (!sb.isEmpty()) {
                    sb.append(", ")
                }
                sb.append(type.className.replace('/', '.').replace('$', '.'))
            }
            sb.toString()
        } else {
            ""
        }
        val methodName = if (node.name == "<init>") cls.simpleName() else node.name
        return cls.findMethod(methodName, parameters)
    }
}