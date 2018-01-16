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

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

@Suppress("PrivatePropertyName")
class OptionsTest : DriverTest() {
    private val DESCRIPTION = """
$PROGRAM_NAME extracts metadata from source code to generate artifacts such as the signature
files, the SDK stub files, external annotations etc.
""".trimIndent()

    private val FLAGS = """
Usage: $PROGRAM_NAME <flags>

General:
--help                                 This message.
--quiet                                Only include vital output
--verbose                              Include extra diagnostic output
--color                                Attempt to colorize the output (defaults to true if
                                       ${"$"}TERM is xterm)
--no-color                             Do not attempt to colorize the output

API sources:
--source-files <files>                 A comma separated list of source files to be
                                       parsed. Can also be @ followed by a path to a text
                                       file containing paths to the full set of files to
                                       parse.
--source-path <paths>                  One or more directories (separated by `:`)
                                       containing source files (within a package
                                       hierarchy)
--classpath <paths>                    One or more directories or jars (separated by `:`)
                                       containing classes that should be on the classpath
                                       when parsing the source files
--merge-annotations <file>             An external annotations file (using IntelliJ's
                                       external annotations database format) to merge and
                                       overlay the sources
--input-api-jar <file>                 A .jar file to read APIs from directly
--manifest <file>                      A manifest file, used to for check permissions to
                                       cross check APIs
--hide-package <package>               Remove the given packages from the API even if they
                                       have not been marked with @hide
--show-annotation <annotation class>   Include the given annotation in the API analysis

Extracting Signature Files:
--api <file>                           Generate a signature descriptor file
--removed-api <file>                   Generate a signature descriptor file for APIs that
                                       have been removed
--output-kotlin-nulls[=yes|no]         Controls whether nullness annotations should be
                                       formatted as in Kotlin (with "?" for nullable
                                       types, "" for non nullable types, and "!" for
                                       unknown. The default is yes.
--compatible-output=[yes|no]           Controls whether to keep signature files compatible
                                       with the historical format (with its various
                                       quirks) or to generate the new format (which will
                                       also include annotations that are part of the API,
                                       etc.)
--omit-common-packages[=yes|no]        Skip common package prefixes like java.lang.* and
                                       kotlin.* in signature files, along with packages
                                       for well known annotations like @Nullable and
                                       @NonNull.
--proguard <file>                      Write a ProGuard keep file for the API

Generating Stubs:
--stubs <dir>                          Generate stub source files for the API
--exclude-annotations                  Exclude annotations such as @Nullable from the stub
                                       files
--write-stubs-source-list <file>       Write the list of generated stub files into the
                                       given source list file

Diffs and Checks:
--previous-api <signature file>        A signature file for the previous version of this
                                       API to apply diffs with
--input-kotlin-nulls[=yes|no]          Whether the signature file being read should be
                                       interpreted as having encoded its types using
                                       Kotlin style types: a suffix of "?" for nullable
                                       types, no suffix for non nullable types, and "!"
                                       for unknown. The default is no.
--check-compatibility                  Check compatibility with the previous API
--migrate-nullness                     Compare nullness information with the previous API
                                       and mark newly annotated APIs as under migration.
--warnings-as-errors                   Promote all warnings to errors
--lints-as-errors                      Promote all API lint warnings to errors
--error <id>                           Report issues of the given id as errors
--warning <id>                         Report issues of the given id as warnings
--lint <id>                            Report issues of the given id as having
                                       lint-severity
--hide <id>                            Hide/skip issues of the given id

Statistics:
--annotation-coverage-stats            Whether metalava should emit coverage statistics
                                       for annotations, listing the percentage of the API
                                       that has been annotated with nullness information.
--annotation-coverage-of <paths>       One or more jars (separated by `:`) containing
                                       existing apps that we want to measure annotation
                                       coverage statistics for. The set of API usages in
                                       those apps are counted up and the most frequently
                                       used APIs that are missing annotation metadata are
                                       listed in descending order.
--skip-java-in-coverage-report         In the coverage annotation report, skip java.** and
                                       kotlin.** to narrow the focus down to the Android
                                       framework APIs.

Extracting Annotations:
--extract-annotations <zipfile>        Extracts annotations from the source files and
                                       writes them into the given zip file
--api-filter <file>                    Applies the given signature file as a filter (which
                                       means no classes,methods or fields not found in the
                                       filter will be included.)
--hide-filtered                        Omit listing APIs that were skipped because of the
                                       --api-filter
--skip-class-retention                 Do not extract annotations that have class file
                                       retention
--rmtypedefs                           Delete all the typedef .class files
--typedef-file <file>                  Writes an typedef annotation class names into the
                                       given file

Injecting API Levels:
--apply-api-levels <api-versions.xml>  Reads an XML file containing API level descriptions
                                       and merges the information into the documentation

Extracting API Levels:
--generate-api-levels <xmlfile>        Reads android.jar SDK files and generates an XML
                                       file recording the API level for each class, method
                                       and field
--android-jar-pattern <pattern>        Patterns to use to locate Android JAR files. The
                                       default is
                                       ${"$"}ANDROID_HOME/platforms/android-%/android.jar.
--current-version                      Sets the current API level of the current source
                                       code
--current-codename                     Sets the code name for the current source code
--current-jar                          Points to the current API jar, if any

""".trimIndent()

    @Test
    fun `Test no arguments`() {
        val args = emptyList<String>()

        val stdout = StringWriter()
        val stderr = StringWriter()
        com.android.tools.metalava.run(args = args.toTypedArray(), stdout = PrintWriter(stdout), stderr = PrintWriter(stderr))
        assertEquals("", stderr.toString())
        assertEquals(
            """
$BANNER


$DESCRIPTION

$FLAGS

""".trimIndent(), stdout.toString()
        )
    }

    @Test
    fun `Test invalid arguments`() {
        val args = listOf("--blah-blah-blah")

        val stdout = StringWriter()
        val stderr = StringWriter()
        com.android.tools.metalava.run(args = args.toTypedArray(), stdout = PrintWriter(stdout), stderr = PrintWriter(stderr))
        assertEquals(BANNER + "\n\n", stdout.toString())
        assertEquals(
            """

Invalid argument --blah-blah-blah

$FLAGS

""".trimIndent(), stderr.toString()
        )
    }

    @Test
    fun `Test help`() {
        val args = listOf("--help")

        val stdout = StringWriter()
        val stderr = StringWriter()
        com.android.tools.metalava.run(args = args.toTypedArray(), stdout = PrintWriter(stdout), stderr = PrintWriter(stderr))
        assertEquals("", stderr.toString())
        assertEquals(
            """
$BANNER


$DESCRIPTION

$FLAGS

""".trimIndent(), stdout.toString()
        )
    }
}