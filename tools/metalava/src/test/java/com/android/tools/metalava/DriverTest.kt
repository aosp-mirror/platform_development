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
import com.android.SdkConstants.DOT_JAVA
import com.android.SdkConstants.DOT_KT
import com.android.annotations.NonNull
import com.android.ide.common.process.DefaultProcessExecutor
import com.android.ide.common.process.LoggedProcessOutputHandler
import com.android.ide.common.process.ProcessException
import com.android.ide.common.process.ProcessInfoBuilder
import com.android.tools.lint.checks.ApiLookup
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.stripComments
import com.android.tools.metalava.doclava1.Errors
import com.android.utils.FileUtils
import com.android.utils.StdLogger
import com.google.common.base.Charsets
import com.google.common.io.Files
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

const val CHECK_OLD_DOCLAVA_TOO = false
const val CHECK_STUB_COMPILATION = false
const val SKIP_NON_COMPAT = false

abstract class DriverTest {
    @get:Rule
    var temporaryFolder = TemporaryFolder()

    protected fun createProject(vararg files: TestFile): File {
        val dir = temporaryFolder.newFolder()

        files
            .map { it.createFile(dir) }
            .forEach { assertNotNull(it) }

        return dir
    }

    protected fun runDriver(vararg args: String): String {
        val sw = StringWriter()
        val writer = PrintWriter(sw)
        if (!com.android.tools.metalava.run(arrayOf(*args), writer, writer)) {
            fail(sw.toString())
        }

        return sw.toString()
    }

    private fun findKotlinStdlibPath(): List<String> {
        val classPath: String = System.getProperty("java.class.path")
        val paths = mutableListOf<String>()
        for (path in classPath.split(':')) {
            val file = File(path)
            val name = file.name
            if (name.startsWith("kotlin-stdlib") ||
                name.startsWith("kotlin-reflect") ||
                name.startsWith("kotlin-script-runtime")) {
                paths.add(file.path)
            }
        }
        if (paths.isEmpty()) {
            error("Did not find kotlin-stdlib-jre8 in $PROGRAM_NAME classpath: $classPath")
        }
        return paths
    }

    private fun getJdkPath(): String? {
        val javaHome = System.getProperty("java.home")
        if (javaHome != null) {
            var javaHomeFile = File(javaHome)
            if (File(javaHomeFile, "bin${File.separator}javac").exists()) {
                return javaHome
            } else if (javaHomeFile.name == "jre") {
                javaHomeFile = javaHomeFile.parentFile
                if (javaHomeFile != null && File(javaHomeFile, "bin${File.separator}javac").exists()) {
                    return javaHomeFile.path
                }
            }
        }
        return System.getenv("JAVA_HOME")
    }

    protected fun check(
        /** The source files to pass to the analyzer */
        vararg sourceFiles: TestFile,
        /** The API signature content (corresponds to --api) */
        api: String? = null,
        /** The exact API signature content (corresponds to --exact-api) */
        exactApi: String? = null,
        /** The removed API (corresponds to --removed-api) */
        removedApi: String? = null,
        /** Expected stubs (corresponds to --stubs) */
        @Language("JAVA") stubs: Array<String> = emptyArray(),
        /** Stub source file list generated */
        stubsSourceList: String? = null,
        /** Whether to run in doclava1 compat mode */
        compatibilityMode: Boolean = true,
        /** Whether to trim the output (leading/trailing whitespace removal) */
        trim: Boolean = true,
        /** Whether to remove blank lines in the output (the signature file usually contains a lot of these) */
        stripBlankLines: Boolean = true,
        /** Warnings expected to be generated when analyzing these sources */
        warnings: String? = "",
        /** Whether to run doclava1 on the test output and assert that the output is identical */
        checkDoclava1: Boolean = compatibilityMode,
        checkCompilation: Boolean = false,
        /** Annotations to merge in */
        @Language("XML") mergeAnnotations: String? = null,
        /** An optional API signature file content to load **instead** of Java/Kotlin source files */
        @Language("TEXT") signatureSource: String? = null,
        /** An optional API signature representing the previous API level to diff */
        @Language("TEXT") previousApi: String? = null,
        /** An optional Proguard keep file to generate */
        @Language("Proguard") proguard: String? = null,
        /** Whether we should migrate nullness information */
        migrateNulls: Boolean = false,
        /** Whether we should check compatibility */
        checkCompatibility: Boolean = false,
        /** Show annotations (--show-annotation arguments) */
        showAnnotations: Array<String> = emptyArray(),
        /** Additional arguments to supply */
        extraArguments: Array<String> = emptyArray(),
        /** Whether we should emit Kotlin-style null signatures */
        outputKotlinStyleNulls: Boolean = !compatibilityMode,
        /** Whether we should interpret API files being read as having Kotlin-style nullness types */
        inputKotlinStyleNulls: Boolean = false,
        /** Whether we should omit java.lang. etc from signature files */
        omitCommonPackages: Boolean = !compatibilityMode,
        /** Expected output (stdout and stderr combined). If null, don't check. */
        expectedOutput: String? = null,
        /** List of extra jar files to record annotation coverage from */
        coverageJars: Array<TestFile>? = null,
        /** Optional manifest to load and associate with the codebase */
        @Language("XML")
        manifest: String? = null,
        /** Packages to pre-import (these will therefore NOT be included in emitted stubs, signature files etc */
        importedPackages: List<String> = emptyList(),
        /** Packages to skip emitting signatures/stubs for even if public (typically used for unit tests
         * referencing to classpath classes that aren't part of the definitions and shouldn't be part of the
         * test output; e.g. a test may reference java.lang.Enum but we don't want to start reporting all the
         * public APIs in the java.lang package just because it's indirectly referenced via the "enum" superclass
         */
        skipEmitPackages: List<String> = listOf("java.lang", "java.util", "java.io"),
        /** Whether we should include --showAnnotations=android.annotation.SystemApi */
        includeSystemApiAnnotations: Boolean = false,
        /** Whether we should warn about super classes that are stripped because they are hidden */
        includeStrippedSuperclassWarnings: Boolean = false,
        /** Apply level to XML */
        applyApiLevelsXml: String? = null
    ) {

        if (compatibilityMode && mergeAnnotations != null) {
            fail(
                "Can't specify both compatibilityMode and mergeAnnotations: there were no " +
                        "annotations output in doclava1"
            )
        }

        Errors.resetLevels()

        // Unit test which checks that a signature file is as expected
        val androidJar = getPlatformFile("android.jar")

        val project = createProject(*sourceFiles)

        val packages = sourceFiles.asSequence().map { findPackage(it.getContents()!!) }.filterNotNull().toSet()

        val sourcePathDir = File(project, "src")
        val sourcePath = sourcePathDir.path
        val sourceList =
            if (signatureSource != null) {
                sourcePathDir.mkdirs()
                assert(sourceFiles.isEmpty(), { "Shouldn't combine sources with signature file loads" })
                val signatureFile = File(project, "load-api.txt")
                Files.asCharSink(signatureFile, Charsets.UTF_8).write(signatureSource.trimIndent())
                if (includeStrippedSuperclassWarnings) {
                    arrayOf(signatureFile.path)
                } else {
                    arrayOf(
                        signatureFile.path,
                        "--hide",
                        "HiddenSuperclass"
                    ) // Suppress warning #111
                }
            } else {
                sourceFiles.asSequence().map { File(project, it.targetPath).path }.toList().toTypedArray()
            }

        val reportedWarnings = StringBuilder()
        reporter = object : Reporter(project) {
            override fun print(message: String) {
                reportedWarnings.append(message.replace(project.path, "TESTROOT").trim()).append('\n')
            }
        }

        val mergeAnnotationsArgs = if (mergeAnnotations != null) {
            val merged = File(project, "merged-annotations.xml")
            Files.asCharSink(merged, Charsets.UTF_8).write(mergeAnnotations.trimIndent())
            arrayOf("--merge-annotations", merged.path)
        } else {
            emptyArray()
        }

        val previousApiFile = if (previousApi != null) {
            val file = File(project, "previous-api.txt")
            Files.asCharSink(file, Charsets.UTF_8).write(previousApi.trimIndent())
            file
        } else {
            null
        }

        val previousApiArgs = if (previousApiFile != null) {
            arrayOf("--previous-api", previousApiFile.path)
        } else {
            emptyArray()
        }

        val manifestFileArgs = if (manifest != null) {
            val file = File(project, "manifest.xml")
            Files.asCharSink(file, Charsets.UTF_8).write(manifest.trimIndent())
            arrayOf("--manifest", file.path)
        } else {
            emptyArray()
        }

        val migrateNullsArguments = if (migrateNulls) {
            arrayOf("--migrate-nullness")
        } else {
            emptyArray()
        }

        val checkCompatibilityArguments = if (checkCompatibility) {
            arrayOf("--check-compatibility")
        } else {
            emptyArray()
        }

        val quiet = if (expectedOutput != null && !extraArguments.contains("--verbose")) {
            // If comparing output, avoid noisy output such as the banner etc
            arrayOf("--quiet")
        } else {
            emptyArray()
        }

        val coverageStats = if (coverageJars != null && coverageJars.isNotEmpty()) {
            val sb = StringBuilder()
            val root = File(project, "coverageJars")
            root.mkdirs()
            for (jar in coverageJars) {
                if (sb.isNotEmpty()) {
                    sb.append(File.pathSeparator)
                }
                val file = jar.createFile(root)
                sb.append(file.path)
            }
            arrayOf("--annotation-coverage-of", sb.toString())
        } else {
            emptyArray()
        }

        var proguardFile: File? = null
        val proguardKeepArguments = if (proguard != null) {
            proguardFile = File(project, "proguard.cfg")
            arrayOf("--proguard", proguardFile.path)
        } else {
            emptyArray()
        }

        val showAnnotationArguments = if (showAnnotations.isNotEmpty() || includeSystemApiAnnotations) {
            val args = mutableListOf<String>()
            for (annotation in showAnnotations) {
                args.add("--show-annotation")
                args.add(annotation)
            }
            if (includeSystemApiAnnotations && !args.contains("android.annotation.SystemApi")) {
                args.add("--show-annotation")
                args.add("android.annotation.SystemApi")
            }
            args.toTypedArray()
        } else {
            emptyArray()
        }

        var removedApiFile: File? = null
        val removedArgs = if (removedApi != null) {
            removedApiFile = temporaryFolder.newFile("removed.txt")
            arrayOf("--removed-api", removedApiFile.path)
        } else {
            emptyArray()
        }

        var apiFile: File? = null
        val apiArgs = if (api != null) {
            apiFile = temporaryFolder.newFile("api.txt")
            arrayOf("--api", apiFile.path)
        } else {
            emptyArray()
        }

        var exactApiFile: File? = null
        val exactApiArgs = if (exactApi != null) {
            exactApiFile = temporaryFolder.newFile("exact-api.txt")
            arrayOf("--exact-api", exactApiFile.path)
        } else {
            emptyArray()
        }

        var stubsDir: File? = null
        val stubsArgs = if (stubs.isNotEmpty()) {
            stubsDir = temporaryFolder.newFolder("stubs")
            arrayOf("--stubs", stubsDir.path)
        } else {
            emptyArray()
        }

        var stubsSourceListFile: File? = null
        val stubsSourceListArgs = if (stubsSourceList != null) {
            stubsSourceListFile = temporaryFolder.newFile("droiddoc-src-list")
            arrayOf("--write-stubs-source-list", stubsSourceListFile.path)
        } else {
            emptyArray()
        }

        val applyApiLevelsXmlFile: File?
        val applyApiLevelsXmlArgs = if (applyApiLevelsXml != null) {
            ApiLookup::class.java.getDeclaredMethod("dispose").apply { isAccessible = true }.invoke(null)
            applyApiLevelsXmlFile = temporaryFolder.newFile("api-versions.xml")
            Files.asCharSink(applyApiLevelsXmlFile!!, Charsets.UTF_8).write(applyApiLevelsXml.trimIndent())
            arrayOf("--apply-api-levels", applyApiLevelsXmlFile.path)
        } else {
            emptyArray()
        }

        val importedPackageArgs = mutableListOf<String>()
        importedPackages.forEach {
            importedPackageArgs.add("--stub-import-packages")
            importedPackageArgs.add(it)
        }

        val skipEmitPackagesArgs = mutableListOf<String>()
        skipEmitPackages.forEach {
            skipEmitPackagesArgs.add("--skip-emit-packages")
            skipEmitPackagesArgs.add(it)
        }

        val kotlinPath = findKotlinStdlibPath()
        val kotlinPathArgs =
            if (kotlinPath.isNotEmpty() &&
                sourceList.asSequence().any { it.endsWith(DOT_KT) }) {
                arrayOf("--classpath", kotlinPath.joinToString(separator = File.pathSeparator) { it })
            } else {
                emptyArray()
            }

        val actualOutput = runDriver(
            "--no-color",

            // For the tests we want to treat references to APIs like java.io.Closeable
            // as a class that is part of the API surface, not as a hidden class as would
            // be the case when analyzing a complete API surface
            //"--unhide-classpath-classes",
            "--allow-referencing-unknown-classes",

            "--sourcepath",
            sourcePath,
            "--classpath",
            androidJar.path,
            *kotlinPathArgs,
            *removedArgs,
            *apiArgs,
            *exactApiArgs,
            *stubsArgs,
            *stubsSourceListArgs,
            "--compatible-output=${if (compatibilityMode) "yes" else "no"}",
            "--output-kotlin-nulls=${if (outputKotlinStyleNulls) "yes" else "no"}",
            "--input-kotlin-nulls=${if (inputKotlinStyleNulls) "yes" else "no"}",
            "--omit-common-packages=${if (omitCommonPackages) "yes" else "no"}",
            *coverageStats,
            *quiet,
            *mergeAnnotationsArgs,
            *previousApiArgs,
            *migrateNullsArguments,
            *checkCompatibilityArguments,
            *proguardKeepArguments,
            *manifestFileArgs,
            *applyApiLevelsXmlArgs,
            *showAnnotationArguments,
            *importedPackageArgs.toTypedArray(),
            *skipEmitPackagesArgs.toTypedArray(),
            *extraArguments,
            *sourceList
        )

        if (expectedOutput != null) {
            assertEquals(expectedOutput.trimIndent().trim(), actualOutput.trim())
        }

        if (api != null && apiFile != null) {
            assertTrue("${apiFile.path} does not exist even though --api was used", apiFile.exists())
            val expectedText = readFile(apiFile, stripBlankLines, trim)
            assertEquals(stripComments(api, stripLineComments = false).trimIndent(), expectedText)
        }

        if (removedApi != null && removedApiFile != null) {
            assertTrue(
                "${removedApiFile.path} does not exist even though --removed-api was used",
                removedApiFile.exists()
            )
            val expectedText = readFile(removedApiFile, stripBlankLines, trim)
            assertEquals(stripComments(removedApi, stripLineComments = false).trimIndent(), expectedText)
        }

        if (exactApi != null && exactApiFile != null) {
            assertTrue("${exactApiFile.path} does not exist even though --exact-api was used", exactApiFile.exists())
            val expectedText = readFile(exactApiFile, stripBlankLines, trim)
            assertEquals(stripComments(exactApi, stripLineComments = false).trimIndent(), expectedText)
        }

        if (proguard != null && proguardFile != null) {
            val expectedProguard = readFile(proguardFile)
            assertTrue("${proguardFile.path} does not exist even though --proguard was used", proguardFile.exists())
            assertEquals(stripComments(proguard, stripLineComments = false).trimIndent(), expectedProguard.trim())
        }

        if (warnings != null) {
            assertEquals(
                warnings.trimIndent().trim(),
                reportedWarnings.toString().replace(project.path, "TESTROOT").replace(project.canonicalPath, "TESTROOT").trim()
            )
        }

        if (stubs.isNotEmpty() && stubsDir != null) {
            for (i in 0 until stubs.size) {
                val stub = stubs[i]
                val sourceFile = sourceFiles[i]
                val targetPath = if (sourceFile.targetPath.endsWith(DOT_KT)) {
                    // Kotlin source stubs are rewritten as .java files for now
                    sourceFile.targetPath.substring(0, sourceFile.targetPath.length - 3) + DOT_JAVA
                } else {
                    sourceFile.targetPath
                }
                val stubFile = File(stubsDir, targetPath.substring("src/".length))
                val expectedText = readFile(stubFile, stripBlankLines, trim)
                assertEquals(stub.trimIndent(), expectedText)
            }
        }

        if (stubsSourceList != null && stubsSourceListFile != null) {
            assertTrue(
                "${stubsSourceListFile.path} does not exist even though --write-stubs-source-list was used",
                stubsSourceListFile.exists()
            )
            val expectedText = readFile(stubsSourceListFile, stripBlankLines, trim)
            assertEquals(stripComments(stubsSourceList, stripLineComments = false).trimIndent(), expectedText)
        }

        if (checkCompilation && stubsDir != null && CHECK_STUB_COMPILATION) {
            val generated = gatherSources(listOf(stubsDir)).map { it.path }.toList().toTypedArray()

            // Also need to include on the compile path annotation classes referenced in the stubs
            val supportAnnotationsDir = File("../../../frameworks/support/annotations/src/main/java/")
            if (!supportAnnotationsDir.isDirectory) {
                fail("Couldn't find $supportAnnotationsDir: Is the pwd set to the root of the metalava source code?")
            }
            val supportAnnotations = gatherSources(listOf(supportAnnotationsDir)).map { it.path }.toList().toTypedArray()

            val extraAnnotationsDir = File("stub-annotations/src/main/java")
            if (!extraAnnotationsDir.isDirectory) {
                fail("Couldn't find $extraAnnotationsDir: Is the pwd set to the root of the metalava source code?")
                fail("Couldn't find $extraAnnotationsDir: Is the pwd set to the root of an Android source tree?")
            }
            val extraAnnotations = gatherSources(listOf(extraAnnotationsDir)).map { it.path }.toList().toTypedArray()


            if (!runCommand(
                    "${getJdkPath()}/bin/javac", arrayOf(
                        "-d", project.path, *generated,
                        *supportAnnotations, *extraAnnotations
                    )
                )) {
                fail("Couldn't compile stub file -- compilation problems")
                return
            }
        }

        if (checkDoclava1 && !CHECK_OLD_DOCLAVA_TOO) {
            println(
                "This test requested diffing with doclava1, but doclava1 testing was disabled with the " +
                        "DriverTest#CHECK_OLD_DOCLAVA_TOO = false"
            )
        }

        if (CHECK_OLD_DOCLAVA_TOO && checkDoclava1 && signatureSource == null &&
            api != null && apiFile != null) {
            apiFile.delete()
            checkSignaturesWithDoclava1(
                api, "-api", apiFile, apiFile, sourceList, sourcePath, packages, androidJar,
                trim, stripBlankLines, showAnnotationArguments, importedPackages
            )
        }

        if (CHECK_OLD_DOCLAVA_TOO && checkDoclava1 && signatureSource == null &&
            exactApi != null && exactApiFile != null) {
            exactApiFile.delete()
            checkSignaturesWithDoclava1(
                exactApi, "-exactApi", exactApiFile, exactApiFile, sourceList, sourcePath,
                packages, androidJar, trim, stripBlankLines, showAnnotationArguments, importedPackages
            )
        }

        if (CHECK_OLD_DOCLAVA_TOO && checkDoclava1 && signatureSource == null
            && removedApi != null && removedApiFile != null) {
            removedApiFile.delete()
            checkSignaturesWithDoclava1(
                removedApi, "-removedApi", removedApiFile, removedApiFile, sourceList,
                sourcePath, packages, androidJar, trim, stripBlankLines, showAnnotationArguments, importedPackages
            )
        }

        if (CHECK_OLD_DOCLAVA_TOO && checkDoclava1 && signatureSource == null && stubsDir != null) {
            stubsDir.deleteRecursively()
            val firstFile = File(stubsDir, sourceFiles[0].targetPath.substring("src/".length))
            checkSignaturesWithDoclava1(
                stubs[0], "-stubs", stubsDir, firstFile, sourceList, sourcePath, packages,
                androidJar, trim, stripBlankLines, showAnnotationArguments, importedPackages
            )
        }

        if (CHECK_OLD_DOCLAVA_TOO && checkDoclava1 && proguard != null && proguardFile != null) {
            proguardFile.delete()
            checkSignaturesWithDoclava1(
                proguard, "-proguard", proguardFile, proguardFile, sourceList,
                sourcePath, packages, androidJar, trim, stripBlankLines, showAnnotationArguments, importedPackages
            )
        }
    }

    private fun checkSignaturesWithDoclava1(
        api: String,
        argument: String,
        output: File,
        expected: File = output,
        sourceList: Array<String>,
        sourcePath: String,
        packages: Set<String>,
        androidJar: File,
        trim: Boolean = true,
        stripBlankLines: Boolean = true,
        showAnnotationArgs: Array<String> = emptyArray(),
        stubImportPackages: List<String>
    ) {
        // We have to run Doclava out of process because running it in process
        // (with Doclava1 jars on the test classpath) only works once; it leaves
        // around state such that the second test fails. Instead we invoke it
        // separately on each test; slower but reliable.

        val doclavaArg = when (argument) {
            "--api" -> "-api"
            "--removed-api" -> "-removedApi"
            else -> if (argument.startsWith("--")) argument.substring(1) else argument
        }

        val showAnnotation: Array<String> = if (showAnnotationArgs.isNotEmpty()) {
            showAnnotationArgs.map { if (it == "--show-annotation") "-showAnnotation" else it }.toTypedArray()
        } else {
            emptyArray()
        }

        val docLava1 = File("testlibs/doclava1.jar")
        if (!docLava1.isFile) {
            fail("Couldn't find $docLava1: Is the pwd set to the root of the metalava source code?")
        }

        val jdkPath = getJdkPath()
        if (jdkPath == null) {
            fail("JDK not found in the environment; make sure \$JAVA_HOME is set.")
        }

        val hidePackageArgs = mutableListOf<String>()
        options.hidePackages.forEach {
            hidePackageArgs.add("-hidePackage")
            hidePackageArgs.add(it)
        }

        val stubImports = if (stubImportPackages.isNotEmpty()) {
            arrayOf("-stubimportpackages", stubImportPackages.joinToString(separator = ":") { it })
        } else {
            emptyArray()
        }

        val args = arrayOf<String>(
            *sourceList,
            "-stubpackages",
            packages.joinToString(separator = ":") { it },
            *stubImports,
            "-doclet",
            "com.google.doclava.Doclava",
            "-docletpath",
            docLava1.path,
            "-encoding",
            "UTF-8",
            "-source",
            "1.8",
            "-nodocs",
            "-quiet",
            "-sourcepath",
            sourcePath,
            "-classpath",
            androidJar.path,

            *showAnnotation,
            *hidePackageArgs.toTypedArray(),

            // -api, or // -stub, etc
            doclavaArg,
            output.path
        )

        if (!runCommand(
                "$jdkPath/bin/java",
                arrayOf(
                    "-classpath",
                    "${docLava1.path}:$jdkPath/lib/tools.jar",
                    "com.google.doclava.Doclava",
                    *args
                )
            )) {
            return
        }

        val expectedText = readFile(expected, stripBlankLines, trim)
        assertEquals(stripComments(api, stripLineComments = false).trimIndent(), expectedText)
    }

    private fun runCommand(executable: String, args: Array<String>): Boolean {
        try {
            val logger = StdLogger(StdLogger.Level.ERROR)
            val processExecutor = DefaultProcessExecutor(logger)
            val processInfo = ProcessInfoBuilder()
                .setExecutable(executable)
                .addArgs(args)
                .createProcess()

            val processOutputHandler = LoggedProcessOutputHandler(logger)
            val result = processExecutor.execute(processInfo, processOutputHandler)

            result.rethrowFailure().assertNormalExitValue()
        } catch (e: ProcessException) {
            fail("Failed to run $executable (${e.message}): not verifying this API on the old doclava engine")
            return false
        }
        return true
    }

    companion object {
        private val latestAndroidPlatform: String
            get() = "android-25"

        private val sdk: File
            get() = File(System.getenv("ANDROID_HOME"))

        fun getPlatformFile(path: String): File {
            val latestAndroidPlatform = latestAndroidPlatform
            val file = FileUtils.join(sdk, SdkConstants.FD_PLATFORMS, latestAndroidPlatform, path)
            if (!file.exists()) {
                throw IllegalArgumentException(
                    "File \"$path\" not found in platform $latestAndroidPlatform"
                )
            }
            return file
        }

        @NonNull
        fun java(@NonNull @Language("JAVA") source: String): LintDetectorTest.TestFile {
            return TestFiles.java(source.trimIndent())
        }

        @NonNull
        fun kotlin(@NonNull @Language("kotlin") source: String): LintDetectorTest.TestFile {
            return TestFiles.kotlin(source.trimIndent())
        }

        private fun readFile(file: File, stripBlankLines: Boolean = false, trim: Boolean = false): String {
            var apiLines: List<String> = Files.asCharSource(file, Charsets.UTF_8).readLines()
            if (stripBlankLines) {
                apiLines = apiLines.asSequence().filter { it.isNotBlank() }.toList()
            }
            var apiText = apiLines.joinToString(separator = "\n") { it }
            if (trim) {
                apiText = apiText.trim()
            }
            return apiText
        }
    }
}

val intRangeAnnotationSource: TestFile = java(
    """
        package android.annotation;
        import java.lang.annotation.*;
        import static java.lang.annotation.ElementType.*;
        import static java.lang.annotation.RetentionPolicy.SOURCE;
        @Retention(SOURCE)
        @Target({METHOD,PARAMETER,FIELD,LOCAL_VARIABLE,ANNOTATION_TYPE})
        public @interface IntRange {
            long from() default Long.MIN_VALUE;
            long to() default Long.MAX_VALUE;
        }
                """
).indented()

val intDefAnnotationSource: TestFile = java(
    """
package android.annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@Retention(SOURCE)
@Target({ANNOTATION_TYPE})
public @interface IntDef {
    long[] value() default {};
    boolean flag() default false;
}
"""
)

val nonNullSource: TestFile = java(
    """
package android.annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.SOURCE;
/**
 * Denotes that a parameter, field or method return value can never be null.
 * @paramDoc This value must never be {@code null}.
 * @returnDoc This value will never be {@code null}.
 * @hide
 */
@SuppressWarnings({"WeakerAccess", "JavaDoc"})
@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface NonNull {
}

"""
)

val requiresPermissionSource: TestFile = java(
    """
package android.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@Retention(SOURCE)
@Target({ANNOTATION_TYPE,METHOD,CONSTRUCTOR,FIELD,PARAMETER})
public @interface RequiresPermission {
    String value() default "";
    String[] allOf() default {};
    String[] anyOf() default {};
    boolean conditional() default false;
}
                """
)

val nullableSource: TestFile = java(
    """
package android.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
/**
 * Denotes that a parameter, field or method return value can be null.
 * @paramDoc This value may be {@code null}.
 * @returnDoc This value may be {@code null}.
 * @hide
 */
@SuppressWarnings({"WeakerAccess", "JavaDoc"})
@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface Nullable {
}
                """
)

val supportNonNullSource: TestFile = java(
    """
package android.support.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface NonNull {
}

"""
)

val supportNullableSource: TestFile = java(
    """
package android.support.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
@SuppressWarnings("WeakerAccess")
@Retention(SOURCE)
@Target({METHOD, PARAMETER, FIELD})
public @interface Nullable {
}
                """
)

val uiThreadSource: TestFile = java(
    """
package android.support.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
/**
 * Denotes that the annotated method or constructor should only be called on the
 * UI thread. If the annotated element is a class, then all methods in the class
 * should be called on the UI thread.
 * @memberDoc This method must be called on the thread that originally created
 *            this UI element. This is typically the main thread of your app.
 * @classDoc Methods in this class must be called on the thread that originally created
 *            this UI element, unless otherwise noted. This is typically the
 *            main thread of your app. * @hide
 */
@SuppressWarnings({"WeakerAccess", "JavaDoc"})
@Retention(SOURCE)
@Target({METHOD,CONSTRUCTOR,TYPE,PARAMETER})
public @interface UiThread {
}
                """
)

val workerThreadSource: TestFile = java(
    """
package android.support.annotation;
import java.lang.annotation.*;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;
/**
 * @memberDoc This method may take several seconds to complete, so it should
 *            only be called from a worker thread.
 * @classDoc Methods in this class may take several seconds to complete, so it should
 *            only be called from a worker thread unless otherwise noted.
 * @hide
 */
@SuppressWarnings({"WeakerAccess", "JavaDoc"})
@Retention(SOURCE)
@Target({METHOD,CONSTRUCTOR,TYPE,PARAMETER})
public @interface WorkerThread {
}
                """
)

val suppressLintSource: TestFile = java(
    """
package android.annotation;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.*;
@Target({TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE})
@Retention(RetentionPolicy.CLASS)
public @interface SuppressLint {
    String[] value();
}
                """
)

val systemServiceSource: TestFile = java(
    """
package android.annotation;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.*;
@Retention(SOURCE)
@Target(TYPE)
public @interface SystemService {
    String value();
}
                """
)

val systemApiSource: TestFile = java(
    """
package android.annotation;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.*;
@Target({TYPE, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE, PACKAGE})
@Retention(RetentionPolicy.SOURCE)
public @interface SystemApi {
}
"""
)

