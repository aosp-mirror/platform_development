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
@file:JvmName("Driver")

package com.android.tools.metalava

import com.android.SdkConstants
import com.android.SdkConstants.DOT_JAVA
import com.android.SdkConstants.DOT_KT
import com.android.tools.lint.KotlinLintAnalyzerFacade
import com.android.tools.lint.LintCoreApplicationEnvironment
import com.android.tools.lint.LintCoreProjectEnvironment
import com.android.tools.lint.annotations.Extractor
import com.android.tools.metalava.apilevels.ApiGenerator
import com.android.tools.metalava.doclava1.ApiFile
import com.android.tools.metalava.doclava1.ApiParseException
import com.android.tools.metalava.doclava1.ApiPredicate
import com.android.tools.metalava.doclava1.Errors
import com.android.tools.metalava.model.Codebase
import com.android.tools.metalava.model.psi.PsiBasedCodebase
import com.google.common.base.Stopwatch
import com.google.common.collect.Lists
import com.google.common.io.Files
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiClassOwner
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

const val PROGRAM_NAME = "metalava"
const val HELP_PROLOGUE = "$PROGRAM_NAME extracts metadata from source code to generate artifacts such as the " +
        "signature files, the SDK stub files, external annotations etc."

@Suppress("PropertyName") // Can't mark const because trimIndent() :-(
val BANNER: String = """
                _        _
 _ __ ___   ___| |_ __ _| | __ ___   ____ _
| '_ ` _ \ / _ \ __/ _` | |/ _` \ \ / / _` |
| | | | | |  __/ || (_| | | (_| |\ V / (_| |
|_| |_| |_|\___|\__\__,_|_|\__,_| \_/ \__,_|
""".trimIndent()

fun main(args: Array<String>) {
    run(args, setExitCode = true)
}

/**
 * The metadata driver is a command line interface to extracting various metadata
 * from a source tree (or existing signature files etc). Run with --help to see
 * more details.
 */
fun run(
    args: Array<String>,
    stdout: PrintWriter = PrintWriter(OutputStreamWriter(System.out)),
    stderr: PrintWriter = PrintWriter(OutputStreamWriter(System.err)),
    setExitCode: Boolean = false
): Boolean {

    if (System.getenv("METALAVA_DUMP_ARGV") != null) {
        stdout.println("---Running $PROGRAM_NAME----")
        stdout.println("pwd=${File("").absolutePath}")
        args.forEach { arg ->
            stdout.println("\"$arg\",")
        }
        stdout.println("----------------------------")
    }

    try {
        val modifiedArgs =
            if (args.isEmpty()) {
                arrayOf("--help")
            } else {
                args
            }

        options = Options(modifiedArgs, stdout, stderr)
        compatibility = Compatibility(options.compatOutput)
        processFlags()
        stdout.flush()
        stderr.flush()
        return true
    } catch (e: Options.OptionsException) {
        if (e.stderr.isNotBlank()) {
            stderr.println("\n${e.stderr}")
        }
        if (e.stdout.isNotBlank()) {
            stdout.println("\n${e.stdout}")
        }
        if (setExitCode) {
            stdout.flush()
            stderr.flush()
            System.exit(e.exitCode)
        }
    }
    stdout.flush()
    stderr.flush()
    return false
}

private fun processFlags() {
    val stopwatch = Stopwatch.createStarted()

    val androidApiLevelXml = options.generateApiLevelXml
    val apiLevelJars = options.apiLevelJars
    if (androidApiLevelXml != null && apiLevelJars != null) {
        ApiGenerator.generate(apiLevelJars, androidApiLevelXml)

        if (options.apiJar == null && options.sources.isEmpty() &&
            options.sourcePath.isEmpty() && options.previousApi == null) {
            // Done
            return
        }
    }

    val codebase =
        if (options.sources.size == 1 && options.sources[0].path.endsWith(SdkConstants.DOT_TXT)) {
            loadFromSignatureFiles(
                file = options.sources[0], kotlinStyleNulls = options.inputKotlinStyleNulls,
                manifest = options.manifest, performChecks = true, supportsStagedNullability = true
            )
        } else if (options.apiJar != null) {
            loadFromJarFile(options.apiJar!!)
        } else {
            loadFromSources()
        }
    options.manifest?.let { codebase.manifest = it }

    if (options.verbose) {
        options.stdout.println("\n$PROGRAM_NAME analyzed API in ${stopwatch.elapsed(TimeUnit.SECONDS)} seconds")
    }

    val previousApiFile = options.previousApi
    if (previousApiFile != null) {
        val previous = loadFromSignatureFiles(
            previousApiFile, options.inputKotlinStyleNulls,
            supportsStagedNullability = true
        )
        codebase.description = "Source tree"
        previous.description = "Previous stable API"

        // If configured, compares the new API with the previous API and reports
        // any incompatibilities.
        checkCompatibility(codebase, previous)

        // If configured, checks for newly added nullness information compared
        // to the previous stable API and marks the newly annotated elements
        // as migrated (which will cause the Kotlin compiler to treat problems
        // as warnings instead of errors

        migrateNulls(codebase, previous)
    }

    // Based on the input flags, generates various output files such
    // as signature files and/or stubs files
    generateOutputs(codebase)

    // Coverage stats?
    if (options.dumpAnnotationStatistics) {
        progress("\nMeasuring annotation statistics: ")
        AnnotationStatistics(codebase).count()
    }
    if (options.annotationCoverageOf.isNotEmpty()) {
        progress("\nMeasuring annotation coverage: ")
        AnnotationStatistics(codebase).measureCoverageOf(options.annotationCoverageOf)
    }

    Disposer.dispose(LintCoreApplicationEnvironment.get().parentDisposable)

    if (!options.quiet) {
        val packageCount = codebase.size()
        options.stdout.println("\n$PROGRAM_NAME finished handling $packageCount packages in $stopwatch")
        options.stdout.flush()
    }
}

private fun migrateNulls(codebase: Codebase, previous: Codebase) {
    if (options.migrateNulls) {
        val prev = previous.supportsStagedNullability
        try {
            previous.supportsStagedNullability = true
            previous.compareWith(NullnessMigration(), codebase)
        } finally {
            previous.supportsStagedNullability = prev
        }
    }
}

private fun checkCompatibility(codebase: Codebase, previous: Codebase) {
    if (options.checkCompatibility) {
        previous.compareWith(CompatibilityCheck(), codebase)
    }
}

private fun loadFromSignatureFiles(
    file: File, kotlinStyleNulls: Boolean,
    manifest: File? = null,
    performChecks: Boolean = false,
    supportsStagedNullability: Boolean = false
): Codebase {
    try {
        val codebase = ApiFile.parseApi(File(file.path), kotlinStyleNulls, supportsStagedNullability)
        codebase.manifest = manifest
        codebase.description = "Codebase loaded from ${file.name}"

        if (performChecks) {
            val analyzer = ApiAnalyzer(codebase)
            analyzer.performChecks()
        }
        return codebase
    } catch (ex: ApiParseException) {
        val message = "Unable to parse signature file $file: ${ex.message}"
        throw Options.OptionsException(message)
    }
}

private fun loadFromSources(): Codebase {
    val projectEnvironment = createProjectEnvironment()

    progress("\nProcessing sources: ")

    val sources = if (options.sources.isEmpty()) {
        if (!options.quiet) {
            options.stdout.println("No source files specified: recursively including all sources found in the source path")
        }
        gatherSources(options.sourcePath)
    } else {
        options.sources
    }

    val joined = mutableListOf<File>()
    joined.addAll(options.sourcePath.map { it.absoluteFile })
    joined.addAll(options.classpath.map { it.absoluteFile })
    // Add in source roots implied by the source files
    extractRoots(sources, joined)

    // Create project environment with those paths
    projectEnvironment.registerPaths(joined)
    val project = projectEnvironment.project

    val kotlinFiles = sources.filter { it.path.endsWith(SdkConstants.DOT_KT) }
    KotlinLintAnalyzerFacade.analyze(kotlinFiles, joined, project)

    val units = Extractor.createUnitsForFiles(project, sources)
    val packageDocs = gatherHiddenPackagesFromJavaDocs(options.sourcePath)

    progress("\nReading Codebase: ")

    val codebase = PsiBasedCodebase("Codebase loaded from source folders")
    codebase.initialize(project, units, packageDocs)
    codebase.manifest = options.manifest

    progress("\nAnalyzing API: ")

    val analyzer = ApiAnalyzer(codebase)
    analyzer.mergeExternalAnnotations()
    analyzer.computeApi()
    analyzer.handleStripping()

    progress("\nInsert missing constructors: ")
    val ignoreShown = options.showAnnotations.isEmpty()
    val filterEmit = ApiPredicate(codebase, ignoreShown = ignoreShown, ignoreRemoved = false)

    analyzer.addConstructors(filterEmit)

    if (options.stubsDir != null) {
        progress("\nEnhancing docs: ")
        val docAnalyzer = DocAnalyzer(codebase)
        docAnalyzer.enhance()

        val applyApiLevelsXml = options.applyApiLevelsXml
        if (applyApiLevelsXml != null) {
            progress("\nApplying API levels")
            docAnalyzer.applyApiLevels(applyApiLevelsXml)
        }
    }

    progress("\nPerforming misc API checks: ")
    analyzer.performChecks()

    // TODO: Move the filtering earlier
    progress("\nFiltering API: ")
    return filterCodebase(codebase)
}

private fun filterCodebase(codebase: PsiBasedCodebase): Codebase {
    val ignoreShown = options.showAnnotations.isEmpty()

    // We ignore removals when limiting the API
    val remove = false
    val filterEmit = ApiPredicate(codebase, ignoreShown = ignoreShown, ignoreRemoved = remove, allowFromJar = false)
    val filterReference = ApiPredicate(codebase, ignoreShown = true, ignoreRemoved = remove, allowFromJar = true)

    // Copy methods from soon-to-be-hidden parents into descendant classes, when necessary
    // TODO: Only do this for stub generation?
    progress("\nInsert missing stubs methods: ")
    ApiAnalyzer(codebase).generateInheritedStubs(filterEmit, filterReference)

    return codebase.filter(filterEmit, filterReference)
}

private fun loadFromJarFile(apiJar: File, manifest: File? = null): Codebase {
    val projectEnvironment = createProjectEnvironment()

    progress("Processing jar file: ")

    // Create project environment with those paths
    val project = projectEnvironment.project
    projectEnvironment.registerPaths(listOf(apiJar))
    val codebase = PsiBasedCodebase()
    codebase.initialize(project, apiJar)
    if (manifest != null) {
        codebase.manifest = options.manifest
    }
    val analyzer = ApiAnalyzer(codebase)
    analyzer.mergeExternalAnnotations()
    codebase.description = "Codebase loaded from ${apiJar.name}"
    return codebase
}

private fun createProjectEnvironment(): LintCoreProjectEnvironment {
    ensurePsiFileCapacity()
    val appEnv = LintCoreApplicationEnvironment.get()
    val parentDisposable = Disposer.newDisposable()
    return LintCoreProjectEnvironment.create(parentDisposable, appEnv)
}

private fun ensurePsiFileCapacity() {
    val fileSize = System.getProperty("idea.max.intellisense.filesize")
    if (fileSize == null) {
        // Ensure we can handle large compilation units like android.R
        System.setProperty("idea.max.intellisense.filesize", "100000")
    }
}

private fun generateOutputs(codebase: Codebase) {
    options.apiFile?.let { apiFile -> createApiSignatureFile(apiFile, codebase) }
    options.removedApiFile?.let { apiFile -> createRemovedSignatureFile(apiFile, codebase) }
    options.proguard?.let { proguard -> createProguardFile(proguard, codebase) }
    options.stubsDir?.let { createStubFiles(it, codebase) }
    options.externalAnnotations?.let { extractAnnotations(codebase, it) }
    progress("\n")
}

private fun extractAnnotations(codebase: Codebase, file: File) {
    val localTimer = Stopwatch.createStarted()
    val units = codebase.units

    @Suppress("UNCHECKED_CAST")
    ExtractAnnotations().extractAnnotations(units.asSequence().filter { it is PsiClassOwner }.toList() as List<PsiClassOwner>)
    if (options.verbose) {
        options.stdout.print("\n$PROGRAM_NAME extracted annotations into $file in $localTimer")
        options.stdout.flush()
    }
}

private fun createStubFiles(stubDir: File, codebase: Codebase) {
    // Generating stubs from a sig-file-based codebase is problematic
    assert(codebase.supportsDocumentation())

    progress("\nGenerating stub files: ")
    val localTimer = Stopwatch.createStarted()
    val prevCompatibility = compatibility
    if (compatibility.compat) {
        //if (!options.quiet) {
        //    options.stderr.println("Warning: Turning off compat mode when generating stubs")
        //}
        compatibility = Compatibility(false)
        // But preserve the setting for whether we want to erase throws signatures (to ensure the API
        // stays compatible)
        compatibility.useErasureInThrows = prevCompatibility.useErasureInThrows
    }

    val stubWriter = StubWriter(codebase, stubDir, ignoreShown = true, generateAnnotations = options.generateAnnotations)
    codebase.accept(stubWriter)

    // Optionally also write out a list of source files that were generated; used
    // for example to point javadoc to the stubs output to generate documentation
    options.stubsSourceList?.let {
        val root = File("").absoluteFile
        stubWriter.writeSourceList(it, root)
    }

    compatibility = prevCompatibility

    progress("\n$PROGRAM_NAME wrote stubs directory $stubDir in $localTimer")
}

private fun createApiSignatureFile(apiFile: File, codebase: Codebase) {
    progress("\nWriting API signature file: ")
    createSignatureFile(codebase, apiFile, false)
}

private fun createRemovedSignatureFile(apiFile: File, codebase: Codebase) {
    progress("\nWriting removed API signature file: ")

    // When generating removed signature files, we operate on the original sources that contain removed elements too
    val original = codebase.original!!

    createSignatureFile(original, apiFile, true)
}

private fun progress(message: String) {
    if (options.verbose) {
        options.stdout.print(message)
        options.stdout.flush()
    }
}

private fun createProguardFile(apiFile: File, codebase: Codebase) {
    val localTimer = Stopwatch.createStarted()
    try {
        val writer = PrintWriter(Files.asCharSink(apiFile, Charsets.UTF_8).openBufferedStream())
        writer.use { printWriter ->
            val apiWriter = ProguardWriter(codebase, printWriter)
            codebase.accept(apiWriter)
        }
    } catch (e: IOException) {
        reporter.report(Errors.IO_ERROR, apiFile, "Cannot open file for write.")
    }
    if (options.verbose) {
        options.stdout.print("\n$PROGRAM_NAME wrote API file $apiFile in $localTimer")
    }
}

private fun createSignatureFile(codebase: Codebase, apiFile: File, showRemovedApi: Boolean) {
    val localTimer = Stopwatch.createStarted()
    try {
        val writer = PrintWriter(Files.asCharSink(apiFile, Charsets.UTF_8).openBufferedStream())
        writer.use { printWriter ->
            val ignoreShown = options.showAnnotations.isEmpty()
            val apiWriter = SignatureWriter(codebase, printWriter, ignoreShown, showRemovedApi)
            codebase.accept(apiWriter)
        }
    } catch (e: IOException) {
        reporter.report(Errors.IO_ERROR, apiFile, "Cannot open file for write.")
    }
    if (options.verbose) {
        options.stdout.print("\n$PROGRAM_NAME wrote API file $apiFile in $localTimer")
    }
}

/** Used for verbose output to show progress bar */
private var tick = 0

/** Print progress */
fun tick() {
    tick++
    if (tick % 100 == 0) {
        options.stdout.print(".")
        options.stdout.flush()
    }
}

private fun addSourceFiles(list: MutableList<File>, file: File) {
    if (file.isDirectory) {
        val files = file.listFiles()
        if (files != null) {
            for (child in files) {
                addSourceFiles(list, child)
            }
        }
    } else {
        if (file.isFile && (file.path.endsWith(DOT_JAVA) || file.path.endsWith(DOT_KT))) {
            list.add(file)
        }
    }
}

fun gatherSources(sourcePath: List<File>): List<File> {
    val sources = Lists.newArrayList<File>()
    for (file in sourcePath) {
        addSourceFiles(sources, file.absoluteFile)
    }
    return sources
}

private fun addHiddenPackages(
    packageToDoc: MutableMap<String, String>,
    hiddenPackages: MutableSet<String>,
    file: File,
    pkg: String
) {
    if (file.isDirectory) {
        val files = file.listFiles()
        if (files != null) {
            for (child in files) {
                val subPkg =
                    if (child.isDirectory)
                        if (pkg.isEmpty())
                            child.name
                        else
                            pkg + "." + child.name
                    else
                        pkg
                addHiddenPackages(packageToDoc, hiddenPackages, child, subPkg)
            }
        }
    } else if (file.isFile && file.name == "package.html") {
        val contents = Files.asCharSource(file, Charsets.UTF_8).read()
        packageToDoc.put(pkg, contents)
        if (contents.contains("@hide")) {
            hiddenPackages.add(pkg)
        }
    }
}

private fun gatherHiddenPackagesFromJavaDocs(sourcePath: List<File>): PackageDocs {
    val map = HashMap<String, String>(100)
    val set = HashSet<String>(100)
    for (file in sourcePath) {
        addHiddenPackages(map, set, file, "")
    }
    return PackageDocs(map, set)
}

private fun extractRoots(sources: List<File>, sourceRoots: MutableList<File> = mutableListOf()): List<File> {
    // Cache for each directory since computing root for a source file is
    // expensive
    val dirToRootCache = mutableMapOf<String, File>()
    for (file in sources) {
        val parent = file.parentFile ?: continue
        val found = dirToRootCache[parent.path]
        if (found != null) {
            continue
        }

        val root = findRoot(file) ?: continue
        dirToRootCache.put(parent.path, root)

        if (!sourceRoots.contains(root)) {
            sourceRoots.add(root)
        }
    }

    return sourceRoots
}

/**
 * If given a full path to a Java or Kotlin source file, produces the path to
 * the source root if possible.
 */
private fun findRoot(file: File): File? {
    val path = file.path
    if (path.endsWith(DOT_JAVA) || path.endsWith(DOT_KT)) {
        val pkg = findPackage(file) ?: return null
        val parent = file.parentFile ?: return null
        return File(path.substring(0, parent.path.length - pkg.length))
    }

    return null
}

/** Finds the package of the given Java/Kotlin source file, if possible */
fun findPackage(file: File): String? {
    val source = Files.asCharSource(file, Charsets.UTF_8).read()
    return findPackage(source)
}

@Suppress("PrivatePropertyName")
private val PACKAGE_PATTERN = Pattern.compile("package\\s+([\\S&&[^;]]*)")

/** Finds the package of the given Java/Kotlin source code, if possible */
fun findPackage(source: String): String? {
    val matcher = PACKAGE_PATTERN.matcher(source)
    val foundPackage = matcher.find()
    return if (foundPackage) {
        matcher.group(1).trim { it <= ' ' }
    } else {
        null
    }
}

data class PackageDocs(val packageDocs: MutableMap<String, String>, val hiddenPackages: MutableSet<String>)
