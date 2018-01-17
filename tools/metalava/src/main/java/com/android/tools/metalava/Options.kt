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
import com.android.tools.lint.annotations.ApiDatabase
import com.android.tools.lint.annotations.SdkUtils2
import com.android.tools.lint.annotations.SdkUtils2.wrap
import com.android.tools.metalava.doclava1.Errors
import com.google.common.base.CharMatcher
import com.google.common.base.Splitter
import com.google.common.collect.Lists
import com.google.common.io.Files
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.StringWriter

/** Global options for the metadata extraction tool */
var options = Options(emptyArray())

private const val MAX_LINE_WIDTH = 90

private const val ARG_HELP = "--help"
private const val ARG_QUIET = "--quiet"
private const val ARG_VERBOSE = "--verbose"
private const val ARG_CLASS_PATH = "--classpath"
private const val ARG_SOURCE_PATH = "--source-path"
private const val ARG_SOURCE_FILES = "--source-files"
private const val ARG_API = "--api"
private const val ARGS_COMPAT_OUTPUT = "--compatible-output"
private const val ARG_REMOVED_API = "--removed-api"
private const val ARG_MERGE_ANNOTATIONS = "--merge-annotations"
private const val ARG_INPUT_API_JAR = "--input-api-jar"
private const val ARG_EXACT_API = "--exact-api"
private const val ARG_STUBS = "--stubs"
private const val ARG_STUBS_SOURCE_LIST = "--write-stubs-source-list"
private const val ARG_PROGUARD = "--proguard"
private const val ARG_EXTRACT_ANNOTATIONS = "--extract-annotations"
private const val ARG_EXCLUDE_ANNOTATIONS = "--exclude-annotations"
private const val ARG_API_FILTER = "--api-filter"
private const val ARG_RM_TYPEDEFS = "--rmtypedefs"
private const val ARG_TYPEDEF_FILE = "--typedef-file"
private const val ARG_SKIP_CLASS_RETENTION = "--skip-class-retention"
private const val ARG_HIDE_FILTERED = "--hide-filtered"
private const val ARG_HIDE_PACKAGE = "--hide-package"
private const val ARG_MANIFEST = "--manifest"
private const val ARG_PREVIOUS_API = "--previous-api"
private const val ARG_MIGRATE_NULLNESS = "--migrate-nullness"
private const val ARG_CHECK_COMPATIBILITY = "--check-compatibility"
private const val ARG_INPUT_KOTLIN_NULLS = "--input-kotlin-nulls"
private const val ARG_OUTPUT_KOTLIN_NULLS = "--output-kotlin-nulls"
private const val ARG_ANNOTATION_COVERAGE_STATS = "--annotation-coverage-stats"
private const val ARG_ANNOTATION_COVERAGE_OF = "--annotation-coverage-of"
private const val ARG_WARNINGS_AS_ERRORS = "--warnings-as-errors"
private const val ARG_LINTS_AS_ERRORS = "--lints-as-errors"
private const val ARG_SHOW_ANNOTATION = "--show-annotation"
private const val ARG_COLOR = "--color"
private const val ARG_NO_COLOR = "--no-color"
private const val ARG_OMIT_COMMON_PACKAGES = "--omit-common-packages"
private const val ARG_SKIP_JAVA_IN_COVERAGE_REPORT = "--skip-java-in-coverage-report"
private const val ARG_NO_BANNER = "--no-banner"
private const val ARG_ERROR = "--error"
private const val ARG_WARNING = "--warning"
private const val ARG_LINT = "--lint"
private const val ARG_HIDE = "--hide"
private const val ARG_UNHIDE_CLASSPATH_CLASSES = "--unhide-classpath-classes"
private const val ARG_ALLOW_REFERENCING_UNKNOWN_CLASSES = "--allow-referencing-unknown-classes"
private const val ARG_INCLUDE_DOC_ONLY = "--include-doconly"
private const val ARG_APPLY_API_LEVELS = "--apply-api-levels"
private const val ARG_GENERATE_API_LEVELS = "--generate-api-levels"
private const val ARG_ANDROID_JAR_PATTERN = "--android-jar-pattern"
private const val ARG_CURRENT_VERSION = "--current-version"
private const val ARG_CURRENT_CODENAME = "--current-codename"
private const val ARG_CURRENT_JAR = "--current-jar"

class Options(
    args: Array<String>,
    /** Writer to direct output to */
    var stdout: PrintWriter = PrintWriter(OutputStreamWriter(System.out)),
    /** Writer to direct error messages to */
    var stderr: PrintWriter = PrintWriter(OutputStreamWriter(System.err))
) {

    /** Internal list backing [sources] */
    private val mutableSources: MutableList<File> = mutableListOf()
    /** Internal list backing [sourcePath] */
    private val mutableSourcePath: MutableList<File> = mutableListOf()
    /** Internal list backing [classpath] */
    private val mutableClassPath: MutableList<File> = mutableListOf()
    /** Internal list backing [showAnnotations] */
    private val mutableShowAnnotations: MutableList<String> = mutableListOf()
    /** Internal list backing [hideAnnotations] */
    private val mutableHideAnnotations: MutableList<String> = mutableListOf()
    /** Internal list backing [stubPackages] */
    private val mutableStubPackages: MutableSet<String> = mutableSetOf()
    /** Internal list backing [stubImportPackages] */
    private val mutableStubImportPackages: MutableSet<String> = mutableSetOf()
    /** Internal list backing [mergeAnnotations] */
    private val mutableMergeAnnotations: MutableList<File> = mutableListOf()
    /** Internal list backing [annotationCoverageOf] */
    private val mutableAnnotationCoverageOf: MutableList<File> = mutableListOf()
    /** Internal list backing [hidePackages] */
    private val mutableHidePackages: MutableList<String> = mutableListOf()
    /** Internal list backing [skipEmitPackages] */
    private val mutableSkipEmitPackages: MutableList<String> = mutableListOf()

    /** Ignored flags we've already warned about - store here such that we don't keep reporting them */
    private val alreadyWarned: MutableSet<String> = mutableSetOf()

    /**
     * Whether signature files should emit in "compat" mode, preserving the various
     * quirks of the previous signature file format -- this will for example use a non-standard
     * modifier ordering, it will call enums interfaces, etc. See the [Compatibility] class
     * for more fine grained control (which is not (currently) exposed as individual command line
     * flags.
     */
    var compatOutput = COMPAT_MODE_BY_DEFAULT && !args.contains("$ARGS_COMPAT_OUTPUT=no")

    /** Whether nullness annotations should be displayed as ?/!/empty instead of with @NonNull/@Nullable. */
    var outputKotlinStyleNulls = !compatOutput

    /** Whether we should omit common packages such as java.lang.* and kotlin.* from signature output */
    var omitCommonPackages = !compatOutput

    /**
     * Whether reading signature files should assume the input is formatted as Kotlin-style nulls
     * (e.g. ? means nullable, ! means unknown, empty means not null)
     */
    var inputKotlinStyleNulls = false

    /** If true, treat all warnings as errors */
    var warningsAreErrors: Boolean = false

    /** If true, treat all API lint warnings as errors */
    var lintsAreErrors: Boolean = false

    /** The list of source roots */
    val sourcePath: List<File> = mutableSourcePath

    /** The list of dependency jars */
    val classpath: List<File> = mutableClassPath

    /** All source files to parse */
    var sources: List<File> = mutableSources

    /** Whether to include APIs with annotations (intended for documentation purposes) */
    var showAnnotations = mutableShowAnnotations

    /** Packages to include (if empty, include all) */
    var stubPackages: Set<String> = mutableStubPackages

    /** Packages to import (if empty, include all) */
    var stubImportPackages: Set<String> = mutableStubImportPackages

    /** Packages to exclude/hide */
    var hidePackages = mutableHidePackages

    /** Packages that we should skip generating even if not hidden; typically only used by tests */
    var skipEmitPackages = mutableSkipEmitPackages

    var showAnnotationOverridesVisibility: Boolean = false

    /** Annotations to hide */
    var hideAnnotations = mutableHideAnnotations

    /** Whether to report warnings and other diagnostics along the way */
    var quiet = false

    /** Whether to report extra diagnostics along the way (note that verbose isn't the same as not quiet) */
    var verbose = false

    /** If set, a directory to write stub files to. Corresponds to the --stubs/-stubs flag. */
    var stubsDir: File? = null

    /** If set, a source file to write the stub index (list of source files) to. Can be passed to
     * other tools like javac/javadoc using the special @-syntax. */
    var stubsSourceList: File? = null

    /** Proguard Keep list file to write */
    var proguard: File? = null

    /** If set, a file to write an API file to. Corresponds to the --api/-api flag. */
    var apiFile: File? = null

    /** If set, a file to write extracted annotations to. Corresponds to the --extract-annotations flag. */
    var externalAnnotations: File? = null

    /** A manifest file to read to for example look up available permissions */
    var manifest: File? = null

    /** If set, a file to write an API file to. Corresponds to the --removed-api/-removedApi flag. */
    var removedApiFile: File? = null

    /** Whether output should be colorized */
    var color = System.getenv("TERM")?.startsWith("xterm") ?: false

    /** Whether to omit Java and Kotlin runtime library packages from annotation coverage stats */
    var omitRuntimePackageStats = true

    /** Whether to include doc-only-marked items */
    var includeDocOnly = false

    /** Whether to generate annotations into the stubs */
    var generateAnnotations = true

    /**
     * A signature file for the previous version of this API (for compatibility checks, nullness
     * migration, etc.)
     */
    var previousApi: File? = null

    /** Whether we should check API compatibility based on the previous API in [previousApi] */
    var checkCompatibility: Boolean = false

    /** Whether we should migrate nulls based on the previous API in [previousApi] */
    var migrateNulls: Boolean = false

    /** Existing external annotation files to merge in */
    var mergeAnnotations: List<File> = mutableMergeAnnotations

    /** Set of jars and class files for existing apps that we want to measure coverage of */
    var annotationCoverageOf: List<File> = mutableAnnotationCoverageOf

    /** Framework API definition to restrict included APIs to */
    var apiFilter: ApiDatabase? = null

    /** If filtering out non-APIs, supply this flag to hide listing matches */
    var hideFiltered: Boolean = false

    /** Don't extract annotations that have class retention */
    var skipClassRetention: Boolean = false

    /** Remove typedef classes found in the given folder */
    var rmTypeDefs: File? = null

    /** Framework API definition to restrict included APIs to */
    var typedefFile: File? = null

    /** An optional <b>jar</b> file to load classes from instead of from source.
     * This is similar to the [classpath] attribute except we're explicitly saying
     * that this is the complete set of classes and that we <b>should</b> generate
     * signatures/stubs from them or use them to diff APIs with (whereas [classpath]
     * is only used to resolve types.) */
    var apiJar: File? = null

    /** Whether to emit coverage statistics for annotations in the API surface */
    var dumpAnnotationStatistics = false

    /** Only used for tests: Normally we want to treat classes not found as source (e.g. supplied via
     * classpath) as hidden, but for the unit tests (where we're not passing in
     * a complete API surface) this makes the tests more cumbersome.
     * This option lets the testing infrastructure treat these classes differently.
     * To see the what this means in practice, try turning it back on for the tests
     * and see what it does to the results :)
     */
    var hideClasspathClasses = true

    /** Only used for tests: Whether during code filtering we allow referencing super classes
     * etc that are unknown (because they're not included in the codebase) */
    var allowReferencingUnknownClasses = false

    /**
     * mapping from API level to android.jar files, if computing API levels
     */
    var apiLevelJars: Array<File>? = null

    /** API level XML file to generate */
    var generateApiLevelXml: File? = null

    /** Reads API XML file to apply into documentation */
    var applyApiLevelsXml: File? = null

    init {
        // Pre-check whether --color/--no-color is present and use that to decide how
        // to emit the banner even before we emit errors
        if (args.contains(ARG_NO_COLOR)) {
            color = false
        } else if (args.contains(ARG_COLOR) || args.contains("-android")) {
            color = true
        }
        // empty args: only when building initial default Options (options field
        // at the top of this file; replaced once the driver runs and passes in
        // a real argv. Don't print a banner when initializing the default options.)
        if (args.isNotEmpty() && !args.contains(ARG_QUIET) && !args.contains(ARG_NO_BANNER)) {
            if (color) {
                stdout.print(colorized(BANNER.trimIndent(), TerminalColor.BLUE))
            } else {
                stdout.println(BANNER.trimIndent())
            }
        }
        stdout.println()
        stdout.flush()

        val apiFilters = mutableListOf<File>()
        var androidJarPatterns: MutableList<String>? = null
        var currentApiLevel: Int = -1
        var currentCodeName: String? = null
        var currentJar: File? = null

        var index = 0
        while (index < args.size) {
            val arg = args[index]

            when (arg) {
                ARG_HELP, "-h", "-?" -> {
                    helpAndQuit(color)
                }

                ARG_QUIET -> {
                    quiet = true; verbose = false
                }
                ARG_VERBOSE -> {
                    verbose = true; quiet = false
                }

                ARGS_COMPAT_OUTPUT -> compatOutput = true

            // For now we don't distinguish between bootclasspath and classpath
                ARG_CLASS_PATH, "-classpath", "-bootclasspath" ->
                    mutableClassPath.addAll(stringToExistingDirsOrJars(getValue(args, ++index)))

                ARG_SOURCE_PATH, "--sources", "--sourcepath", "-sourcepath" -> {
                    val path = getValue(args, ++index)
                    if (path.endsWith(SdkConstants.DOT_JAVA)) {
                        throw OptionsException(
                            "$arg should point to a source root directory, not a source file ($path)"
                        )
                    }
                    mutableSourcePath.addAll(stringToExistingDirsOrJars(path))
                }

                ARG_SOURCE_FILES -> {
                    val listString = getValue(args, ++index)
                    listString.split(",").forEach { path ->
                        mutableSources.addAll(stringToExistingFiles(path))
                    }
                }

                ARG_MERGE_ANNOTATIONS, "--merge-zips" -> mutableMergeAnnotations.addAll(
                    stringToExistingDirsOrFiles(
                        getValue(args, ++index)
                    )
                )

                ARG_API, "-api" -> apiFile = stringToNewFile(getValue(args, ++index))

                ARG_REMOVED_API, "-removedApi" -> removedApiFile = stringToNewFile(getValue(args, ++index))

                ARG_EXACT_API, "-exactApi" -> {
                    unimplemented(arg) // Not yet implemented (because it seems to no longer be hooked up in doclava1)
                }

                ARG_MANIFEST, "-manifest" -> manifest = stringToExistingFile(getValue(args, ++index))

                ARG_SHOW_ANNOTATION, "-showAnnotation" -> mutableShowAnnotations.add(getValue(args, ++index))

                "--showAnnotationOverridesVisibility" -> {
                    unimplemented(arg)
                    showAnnotationOverridesVisibility = true
                }

                "--hideAnnotations", "-hideAnnotation" -> mutableHideAnnotations.add(getValue(args, ++index))

                ARG_STUBS, "-stubs" -> stubsDir = stringToNewDir(getValue(args, ++index))
                ARG_STUBS_SOURCE_LIST -> stubsSourceList = stringToNewFile(getValue(args, ++index))

                ARG_EXCLUDE_ANNOTATIONS -> generateAnnotations = false

                ARG_PROGUARD, "-proguard" -> proguard = stringToNewFile(getValue(args, ++index))

                ARG_HIDE_PACKAGE, "-hidePackage" -> mutableHidePackages.add(getValue(args, ++index))

                "--stub-packages", "-stubpackages" -> {
                    val packages = getValue(args, ++index)
                    mutableStubPackages += packages.split(File.pathSeparatorChar)
                }

                "--stub-import-packages", "-stubimportpackages" -> {
                    val packages = getValue(args, ++index)
                    for (pkg in packages.split(File.pathSeparatorChar)) {
                        mutableStubImportPackages.add(pkg)
                        mutableHidePackages.add(pkg)
                    }
                }

                "--skip-emit-packages" -> {
                    val packages = getValue(args, ++index)
                    mutableSkipEmitPackages += packages.split(File.pathSeparatorChar)
                }

                ARG_INPUT_API_JAR -> apiJar = stringToExistingFile(getValue(args, ++index))

                ARG_EXTRACT_ANNOTATIONS -> externalAnnotations = stringToNewFile(getValue(args, ++index))

                ARG_PREVIOUS_API -> previousApi = stringToExistingFile(getValue(args, ++index))

                ARG_MIGRATE_NULLNESS -> migrateNulls = true

                ARG_CHECK_COMPATIBILITY -> checkCompatibility = true

                ARG_ANNOTATION_COVERAGE_STATS -> dumpAnnotationStatistics = true
                ARG_ANNOTATION_COVERAGE_OF -> mutableAnnotationCoverageOf.add(
                    stringToExistingFileOrDir(
                        getValue(args, ++index)
                    )
                )

                ARG_ERROR, "-error" -> Errors.setErrorLevel(getValue(args, ++index), Severity.ERROR)
                ARG_WARNING, "-warning" -> Errors.setErrorLevel(getValue(args, ++index), Severity.WARNING)
                ARG_LINT, "-lint" -> Errors.setErrorLevel(getValue(args, ++index), Severity.LINT)
                ARG_HIDE, "-hide" -> Errors.setErrorLevel(getValue(args, ++index), Severity.HIDDEN)

                ARG_WARNINGS_AS_ERRORS, "-werror" -> warningsAreErrors = true
                ARG_LINTS_AS_ERRORS, "-lerror" -> lintsAreErrors = true

                ARG_COLOR -> color = true
                ARG_NO_COLOR -> color = false

                ARG_OMIT_COMMON_PACKAGES, ARG_OMIT_COMMON_PACKAGES + "=yes" -> omitCommonPackages = true
                ARG_OMIT_COMMON_PACKAGES + "=no" -> omitCommonPackages = false

                ARG_SKIP_JAVA_IN_COVERAGE_REPORT -> omitRuntimePackageStats = true

                ARG_UNHIDE_CLASSPATH_CLASSES -> hideClasspathClasses = false
                ARG_ALLOW_REFERENCING_UNKNOWN_CLASSES -> allowReferencingUnknownClasses = true

                ARG_INCLUDE_DOC_ONLY -> includeDocOnly = true

            // Annotation extraction flags
                ARG_API_FILTER -> apiFilters.add(stringToExistingFile(getValue(args, ++index)))
                ARG_RM_TYPEDEFS -> rmTypeDefs = stringToExistingDir(getValue(args, ++index))
                ARG_TYPEDEF_FILE -> typedefFile = stringToNewFile(getValue(args, ++index))
                ARG_HIDE_FILTERED -> hideFiltered = true
                ARG_SKIP_CLASS_RETENTION -> skipClassRetention = true

            // Extracting API levels
                ARG_ANDROID_JAR_PATTERN -> {
                    val list = androidJarPatterns ?: run {
                        val list = arrayListOf<String>()
                        androidJarPatterns = list
                        list
                    }
                    list.add(getValue(args, ++index))
                }
                ARG_CURRENT_VERSION -> {
                    currentApiLevel = Integer.parseInt(getValue(args, ++index))
                    if (currentApiLevel <= 26) {
                        throw OptionsException("Suspicious currentApi=$currentApiLevel, expected at least 27")
                    }
                }
                ARG_CURRENT_CODENAME -> {
                    currentCodeName = getValue(args, ++index)
                }
                ARG_CURRENT_JAR -> {
                    currentJar = stringToExistingFile(getValue(args, ++index))
                }
                ARG_GENERATE_API_LEVELS -> {
                    generateApiLevelXml = stringToNewFile(getValue(args, ++index))
                }
                ARG_APPLY_API_LEVELS -> {
                    applyApiLevelsXml = stringToExistingFile(getValue(args, ++index))
                }

            // Unimplemented doclava1 flags (no arguments)
                "-quiet" -> {
                    unimplemented(arg)
                }

                "-android" -> { // partially implemented: Pick up the color hint, but there may be other implications
                    color = true
                    unimplemented(arg)
                }

                "-stubsourceonly" -> {
                    /* noop */
                }

            // Unimplemented doclava1 flags (1 argument)
                "-d" -> {
                    unimplemented(arg)
                    index++
                }

                "-encoding" -> {
                    val value = getValue(args, ++index)
                    if (value.toUpperCase() != "UTF-8") {
                        throw OptionsException("$value: Only UTF-8 encoding is supported")
                    }
                }

                "-source" -> {
                    val value = getValue(args, ++index)
                    if (value != "1.8") {
                        throw OptionsException("$value: Only source 1.8 is supported")
                    }
                }

            // Unimplemented doclava1 flags (2 arguments)
                "-since" -> {
                    unimplemented(arg)
                    index += 2
                }

            // doclava1 doc-related flags: only supported here to make this command a drop-in
            // replacement
                "-referenceonly",
                "-nodocs" -> {
                    javadoc(arg)
                }

            // doclava1 flags with 1 argument
                "-doclet",
                "-docletpath",
                "-templatedir",
                "-htmldir",
                "-knowntags",
                "-resourcesdir",
                "-resourcesoutdir",
                "-overview" -> {
                    javadoc(arg)
                    index++
                }

            // doclava1 flags with two arguments
                "-federate",
                "-federationapi" -> {
                    javadoc(arg)
                    index += 2
                }

            // doclava1 flag with variable number of arguments; skip everything until next arg
                "-hdf" -> {
                    javadoc(arg)
                    index++
                    while (index < args.size) {
                        if (args[index].startsWith("-")) {
                            break
                        }
                        index++
                    }
                    index--
                }

                else -> {
                    if (arg.startsWith("-J-") || arg.startsWith("-XD")) {
                        // -J: mechanism to pass extra flags to javadoc, e.g.
                        //    -J-XX:-OmitStackTraceInFastThrow
                        // -XD: mechanism to set properties, e.g.
                        //    -XDignore.symbol.file
                        javadoc(arg)
                    } else if (arg.startsWith(ARG_OUTPUT_KOTLIN_NULLS)) {
                        outputKotlinStyleNulls = if (arg == ARG_OUTPUT_KOTLIN_NULLS) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_OUTPUT_KOTLIN_NULLS.length + 1))
                        }
                    } else if (arg.startsWith(ARG_INPUT_KOTLIN_NULLS)) {
                        inputKotlinStyleNulls = if (arg == ARG_INPUT_KOTLIN_NULLS) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_INPUT_KOTLIN_NULLS.length + 1))
                        }
                    } else if (arg.startsWith(ARG_OMIT_COMMON_PACKAGES)) {
                        omitCommonPackages = if (arg == ARG_OMIT_COMMON_PACKAGES) {
                            true
                        } else {
                            yesNo(arg.substring(ARG_OMIT_COMMON_PACKAGES.length + 1))
                        }
                    } else if (arg.startsWith(ARGS_COMPAT_OUTPUT)) {
                        compatOutput = if (arg == ARGS_COMPAT_OUTPUT)
                            true
                        else
                            yesNo(arg.substring(ARGS_COMPAT_OUTPUT.length + 1))
                    } else if (arg.startsWith("-")) {
                        val usage = getUsage(includeHeader = false, colorize = color)
                        throw OptionsException(stderr = "Invalid argument $arg\n\n$usage")
                    } else {
                        // All args that don't start with "-" are taken to be filenames
                        mutableSources.addAll(stringToExistingFiles(arg))
                    }
                }
            }

            ++index
        }

        if (!apiFilters.isEmpty()) {
            apiFilter = try {
                val lines = Lists.newArrayList<String>()
                for (file in apiFilters) {
                    lines.addAll(Files.readLines(file, com.google.common.base.Charsets.UTF_8))
                }
                ApiDatabase(lines)
            } catch (e: IOException) {
                throw OptionsException("Could not open API database $apiFilters: ${e.localizedMessage}")
            }
        }

        if (generateApiLevelXml != null) {
            if (currentJar != null && currentApiLevel == -1 || currentJar == null && currentApiLevel != -1) {
                throw OptionsException("You must specify both --current-jar and --current-version (or neither one)")
            }
            if (androidJarPatterns == null) {
                androidJarPatterns = mutableListOf(
                    "prebuilts/tools/common/api-versions/android-%/android.jar",
                    "prebuilts/sdk/%/android.jar"
                )
            }
            apiLevelJars = findAndroidJars(androidJarPatterns!!, currentApiLevel, currentCodeName, currentJar)
        }

        checkFlagConsistency()
    }

    private fun findAndroidJars(
        androidJarPatterns: List<String>, currentApiLevel: Int,
        currentCodeName: String?, currentJar: File?
    ): Array<File> {

        @Suppress("NAME_SHADOWING")
        val currentApiLevel = if (currentCodeName != null && "REL" != currentCodeName) {
            currentApiLevel + 1
        } else {
            currentApiLevel
        }

        val apiLevelFiles = mutableListOf<File>()
        apiLevelFiles.add(File("")) // api level 0: dummy
        val minApi = 1

        // Get all the android.jar. They are in platforms-#
        var apiLevel = minApi - 1
        while (true) {
            apiLevel++
            try {
                var jar: File? = null
                if (apiLevel == currentApiLevel) {
                    jar = currentJar
                }
                if (jar == null) {
                    jar = getAndroidJarFile(apiLevel, androidJarPatterns)
                }
                if (jar == null || !jar.isFile) {
                    if (verbose) {
                        stdout.println("Last API level found: ${apiLevel - 1}")
                    }
                    break
                }
                if (verbose) {
                    stdout.println("Found API $apiLevel at ${jar.path}")
                }
                apiLevelFiles.add(jar)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return apiLevelFiles.toTypedArray()
    }

    private fun getAndroidJarFile(apiLevel: Int, patterns: List<String>): File? {
        return patterns
            .map { File(it.replace("%", Integer.toString(apiLevel))) }
            .firstOrNull { it.isFile }
    }

    private fun yesNo(answer: String): Boolean {
        return when (answer) {
            "yes", "true", "enabled", "on" -> true
            "no", "false", "disabled", "off" -> false
            else -> throw OptionsException(stderr = "Unexpected $answer; expected yes or no")
        }
    }

    /** Makes sure that the flag combinations make sense */
    private fun checkFlagConsistency() {
        if (checkCompatibility && previousApi == null) {
            throw OptionsException(stderr = "$ARG_CHECK_COMPATIBILITY requires $ARG_PREVIOUS_API")
        }

        if (migrateNulls && previousApi == null) {
            throw OptionsException(stderr = "$ARG_MIGRATE_NULLNESS requires $ARG_PREVIOUS_API")
        }

        if (apiJar != null && sources.isNotEmpty()) {
            throw OptionsException(stderr = "Specify either $ARG_SOURCE_FILES or $ARG_INPUT_API_JAR, not both")
        }

        if (compatOutput && outputKotlinStyleNulls) {
            throw OptionsException(
                stderr = "$ARG_OUTPUT_KOTLIN_NULLS should not be combined with " +
                        "$ARGS_COMPAT_OUTPUT=yes"
            )
        }

//        if (stubsSourceList != null && stubsDir == null) {
//            throw OptionsException(stderr = "$ARG_STUBS_SOURCE_LIST should only be used when $ARG_STUBS is set")
//        }
    }

    private fun javadoc(arg: String) {
        if (!alreadyWarned.add(arg)) {
            return
        }
        if (!options.quiet) {
            reporter.report(
                Severity.WARNING, null as String?, "Ignoring javadoc-related doclava1 flag $arg",
                color = color
            )
        }
    }

    private fun unimplemented(arg: String) {
        if (!alreadyWarned.add(arg)) {
            return
        }
        if (!options.quiet) {
            val message = "Ignoring unimplemented doclava1 flag $arg" +
                    when (arg) {
                        "-encoding" -> " (UTF-8 assumed)"
                        "-source" -> "  (1.8 assumed)"
                        else -> ""
                    }
            reporter.report(Severity.WARNING, null as String?, message, color = color)

        }
    }

    private fun helpAndQuit(colorize: Boolean = color) {
        throw OptionsException(stdout = getUsage(colorize = colorize))
    }

    private fun getValue(args: Array<String>, index: Int): String {
        if (index >= args.size) {
            throw OptionsException("Missing argument for ${args[index - 1]}")
        }
        return args[index]
    }

    private fun stringToExistingDir(value: String): File {
        val file = File(value)
        if (!file.isDirectory) {
            throw OptionsException("$file is not a directory")
        }
        return file
    }

    private fun stringToExistingDirs(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = File(path)
            if (!file.isDirectory) {
                throw OptionsException("$file is not a directory")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingDirsOrJars(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = File(path)
            if (!file.isDirectory && !(file.path.endsWith(SdkConstants.DOT_JAR) && file.isFile)) {
                throw OptionsException("$file is not a jar or directory")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingDirsOrFiles(value: String): List<File> {
        val files = mutableListOf<File>()
        for (path in value.split(File.pathSeparatorChar)) {
            val file = File(path)
            if (!file.exists()) {
                throw OptionsException("$file does not exist")
            }
            files.add(file)
        }
        return files
    }

    private fun stringToExistingFile(value: String): File {
        val file = File(value)
        if (!file.isFile) {
            throw OptionsException("$file is not a file")
        }
        return file
    }

    private fun stringToExistingFileOrDir(value: String): File {
        val file = File(value)
        if (!file.exists()) {
            throw OptionsException("$file is not a file or directory")
        }
        return file
    }

    private fun stringToExistingFiles(value: String): List<File> {
        val files = mutableListOf<File>()
        value.split(File.pathSeparatorChar)
            .map { File(it) }
            .forEach { file ->
                if (file.path.startsWith("@")) {
                    // File list; files to be read are stored inside. SHOULD have been one per line
                    // but sadly often uses spaces for separation too (so we split by whitespace,
                    // which means you can't point to files in paths with spaces)
                    val listFile = File(file.path.substring(1))
                    if (!listFile.isFile) {
                        throw OptionsException("$listFile is not a file")
                    }
                    val contents = Files.asCharSource(listFile, Charsets.UTF_8).read()
                    val pathList = Splitter.on(CharMatcher.whitespace()).trimResults().omitEmptyStrings().split(
                        contents
                    )
                    pathList.asSequence().map { File(it) }.forEach {
                        if (!it.isFile) {
                            throw OptionsException("$it is not a file")
                        }
                        files.add(it)
                    }
                } else {
                    if (!file.isFile) {
                        throw OptionsException("$file is not a file")
                    }
                    files.add(file)
                }
            }
        return files
    }

    private fun stringToNewFile(value: String): File {
        val output = File(value)

        if (output.exists()) {
            if (output.isDirectory) {
                throw OptionsException("$output is a directory")
            }
            val deleted = output.delete()
            if (!deleted) {
                throw OptionsException("Could not delete previous version of $output")
            }
        } else if (output.parentFile != null && !output.parentFile.exists()) {
            val ok = output.parentFile.mkdirs()
            if (!ok) {
                throw OptionsException("Could not create ${output.parentFile}")
            }
        }

        return output
    }

    private fun stringToNewDir(value: String): File {
        val output = File(value)

        if (output.exists()) {
            if (output.isDirectory) {
                output.deleteRecursively()
            }
        } else if (output.parentFile != null && !output.parentFile.exists()) {
            val ok = output.parentFile.mkdirs()
            if (!ok) {
                throw OptionsException("Could not create ${output.parentFile}")
            }
        }

        return output
    }

    private fun getUsage(includeHeader: Boolean = true, colorize: Boolean = color): String {
        val usage = StringWriter()
        val printWriter = PrintWriter(usage)
        usage(printWriter, includeHeader, colorize)
        return usage.toString()
    }

    private fun usage(out: PrintWriter, includeHeader: Boolean = true, colorize: Boolean = color) {
        if (includeHeader) {
            out.println(wrap(HELP_PROLOGUE, MAX_LINE_WIDTH, ""))
        }

        if (colorize) {
            out.println("Usage: ${colorized(PROGRAM_NAME, TerminalColor.BLUE)} <flags>")
        } else {
            out.println("Usage: $PROGRAM_NAME <flags>")
        }

        val args = arrayOf(
            "", "\nGeneral:",
            ARG_HELP, "This message.",
            ARG_QUIET, "Only include vital output",
            ARG_VERBOSE, "Include extra diagnostic output",
            ARG_COLOR, "Attempt to colorize the output (defaults to true if \$TERM is xterm)",
            ARG_NO_COLOR, "Do not attempt to colorize the output",

            "", "\nAPI sources:",
            ARG_SOURCE_FILES + " <files>", "A comma separated list of source files to be parsed. Can also be " +
                    "@ followed by a path to a text file containing paths to the full set of files to parse.",

            ARG_SOURCE_PATH + " <paths>", "One or more directories (separated by `${File.pathSeparator}`) " +
                    "containing source files (within a package hierarchy)",

            ARG_CLASS_PATH + " <paths>", "One or more directories or jars (separated by " +
                    "`${File.pathSeparator}`) containing classes that should be on the classpath when parsing the " +
                    "source files",

            ARG_MERGE_ANNOTATIONS + " <file>", "An external annotations file (using IntelliJ's external " +
                    "annotations database format) to merge and overlay the sources",

            ARG_INPUT_API_JAR + " <file>", "A .jar file to read APIs from directly",

            ARG_MANIFEST + " <file>", "A manifest file, used to for check permissions to cross check APIs",

            ARG_HIDE_PACKAGE + " <package>", "Remove the given packages from the API even if they have not been " +
                    "marked with @hide",

            ARG_SHOW_ANNOTATION + " <annotation class>", "Include the given annotation in the API analysis",

            "", "\nExtracting Signature Files:",
            // TODO: Document --show-annotation!
            ARG_API + " <file>", "Generate a signature descriptor file",
            ARG_REMOVED_API + " <file>", "Generate a signature descriptor file for APIs that have been removed",
            ARG_OUTPUT_KOTLIN_NULLS + "[=yes|no]", "Controls whether nullness annotations should be formatted as " +
                    "in Kotlin (with \"?\" for nullable types, \"\" for non nullable types, and \"!\" for unknown. " +
                    "The default is yes.",
            ARGS_COMPAT_OUTPUT + "=[yes|no]", "Controls whether to keep signature files compatible with the " +
                    "historical format (with its various quirks) or to generate the new format (which will also include " +
                    "annotations that are part of the API, etc.)",
            ARG_OMIT_COMMON_PACKAGES + "[=yes|no]", "Skip common package prefixes like java.lang.* and " +
                    "kotlin.* in signature files, along with packages for well known annotations like @Nullable and " +
                    "@NonNull.",

            ARG_PROGUARD + " <file>", "Write a ProGuard keep file for the API",

            "", "\nGenerating Stubs:",
            ARG_STUBS + " <dir>", "Generate stub source files for the API",
            ARG_EXCLUDE_ANNOTATIONS, "Exclude annotations such as @Nullable from the stub files",
            ARG_STUBS_SOURCE_LIST + " <file>", "Write the list of generated stub files into the given source " +
                    "list file",

            "", "\nDiffs and Checks:",
            ARG_PREVIOUS_API + " <signature file>", "A signature file for the previous version of this " +
                    "API to apply diffs with",
            ARG_INPUT_KOTLIN_NULLS + "[=yes|no]", "Whether the signature file being read should be " +
                    "interpreted as having encoded its types using Kotlin style types: a suffix of \"?\" for nullable " +
                    "types, no suffix for non nullable types, and \"!\" for unknown. The default is no.",
            ARG_CHECK_COMPATIBILITY, "Check compatibility with the previous API",
            ARG_MIGRATE_NULLNESS, "Compare nullness information with the previous API and mark newly " +
                    "annotated APIs as under migration.",
            ARG_WARNINGS_AS_ERRORS, "Promote all warnings to errors",
            ARG_LINTS_AS_ERRORS, "Promote all API lint warnings to errors",
            ARG_ERROR + " <id>", "Report issues of the given id as errors",
            ARG_WARNING + " <id>", "Report issues of the given id as warnings",
            ARG_LINT + " <id>", "Report issues of the given id as having lint-severity",
            ARG_HIDE + " <id>", "Hide/skip issues of the given id",

            "", "\nStatistics:",
            ARG_ANNOTATION_COVERAGE_STATS, "Whether $PROGRAM_NAME should emit coverage statistics for " +
                    "annotations, listing the percentage of the API that has been annotated with nullness information.",

            ARG_ANNOTATION_COVERAGE_OF + " <paths>", "One or more jars (separated by `${File.pathSeparator}`) " +
                    "containing existing apps that we want to measure annotation coverage statistics for. The set of " +
                    "API usages in those apps are counted up and the most frequently used APIs that are missing " +
                    "annotation metadata are listed in descending order.",

            ARG_SKIP_JAVA_IN_COVERAGE_REPORT, "In the coverage annotation report, skip java.** and kotlin.** to " +
                    "narrow the focus down to the Android framework APIs.",

            "", "\nExtracting Annotations:",
            ARG_EXTRACT_ANNOTATIONS + " <zipfile>", "Extracts annotations from the source files and writes them " +
                    "into the given zip file",

            ARG_API_FILTER + " <file>", "Applies the given signature file as a filter (which means no classes," +
                    "methods or fields not found in the filter will be included.)",
            ARG_HIDE_FILTERED, "Omit listing APIs that were skipped because of the $ARG_API_FILTER",

            ARG_SKIP_CLASS_RETENTION, "Do not extract annotations that have class file retention",
            ARG_RM_TYPEDEFS, "Delete all the typedef .class files",
            ARG_TYPEDEF_FILE + " <file>", "Writes an typedef annotation class names into the given file",

            "", "\nInjecting API Levels:",
            ARG_APPLY_API_LEVELS + " <api-versions.xml>", "Reads an XML file containing API level descriptions " +
                    "and merges the information into the documentation",

            "", "\nExtracting API Levels:",
            ARG_GENERATE_API_LEVELS + " <xmlfile>",
            "Reads android.jar SDK files and generates an XML file recording " +
                    "the API level for each class, method and field",
            ARG_ANDROID_JAR_PATTERN + " <pattern>", "Patterns to use to locate Android JAR files. The default " +
                    "is \$ANDROID_HOME/platforms/android-%/android.jar.",
            ARG_CURRENT_VERSION, "Sets the current API level of the current source code",
            ARG_CURRENT_CODENAME, "Sets the code name for the current source code",
            ARG_CURRENT_JAR, "Points to the current API jar, if any"
        )

        var argWidth = 0
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            argWidth = Math.max(argWidth, arg.length)
            i += 2
        }
        argWidth += 2
        val sb = StringBuilder(20)
        for (indent in 0 until argWidth) {
            sb.append(' ')
        }
        val indent = sb.toString()
        val formatString = "%1$-" + argWidth + "s%2\$s"

        i = 0
        while (i < args.size) {
            val arg = args[i]
            val description = args[i + 1]
            if (arg.isEmpty()) {
                if (colorize) {
                    out.println(colorized(description, TerminalColor.YELLOW))
                } else {
                    out.println(description)
                }
            } else {
                if (colorize) {
                    val colorArg = bold(arg)
                    val invisibleChars = colorArg.length - arg.length
                    // +invisibleChars: the extra chars in the above are counted but don't contribute to width
                    // so allow more space
                    val colorFormatString = "%1$-" + (argWidth + invisibleChars) + "s%2\$s"

                    out.print(
                        SdkUtils2.wrap(
                            String.format(colorFormatString, colorArg, description),
                            MAX_LINE_WIDTH + invisibleChars, MAX_LINE_WIDTH, indent
                        )
                    )
                } else {
                    out.print(
                        SdkUtils2.wrap(
                            String.format(formatString, arg, description),
                            MAX_LINE_WIDTH, indent
                        )
                    )
                }
            }
            i += 2
        }
    }

    class OptionsException(
        val stderr: String = "",
        val stdout: String = "",
        val exitCode: Int = if (stderr.isBlank()) 0 else -1
    ) : RuntimeException(stdout + stderr)
}
