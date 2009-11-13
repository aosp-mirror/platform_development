/*
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.regex.Pattern;

/**
 * Immutable representation of an IDE configuration. Assumes that the current
 * directory is the project's root directory.
 */
public class Configuration {

    /** Java source tree roots. */
    public final SortedSet<File> sourceRoots;

    /** Found .jar files (that weren't excluded). */
    public final List<File> jarFiles;

    /** Excluded directories which may or may not be under a source root. */
    public final SortedSet<File> excludedDirs;

    /** The root directory for this tool. */
    public final File toolDirectory;

    /** File name used for excluded path files. */
    private static final String EXCLUDED_PATHS = "excluded-paths";

    /**
     * Constructs a Configuration by traversing the directory tree, looking
     * for .java and .jar files and identifying source roots.
     */
    public Configuration() throws IOException {
        this.toolDirectory = new File("development/tools/idegen");
        if (!toolDirectory.isDirectory()) {
            // The wrapper script should have already verified this.
            throw new AssertionError("Not in root directory.");
        }

        Stopwatch stopwatch = new Stopwatch();

        Excludes excludes = readExcludes();

        stopwatch.reset("Read excludes");

        List<File> jarFiles = new ArrayList<File>(500);
        SortedSet<File> excludedDirs = new TreeSet<File>();
        SortedSet<File> sourceRoots = new TreeSet<File>();

        traverse(new File("."), sourceRoots, jarFiles, excludedDirs, excludes);

        stopwatch.reset("Traversed tree");

        Log.debug(sourceRoots.size() + " source roots");
        Log.debug(jarFiles.size() + " jar files");
        Log.debug(excludedDirs.size() + " excluded dirs");

        this.sourceRoots = Collections.unmodifiableSortedSet(sourceRoots);
        this.jarFiles = Collections.unmodifiableList(jarFiles);
        this.excludedDirs = Collections.unmodifiableSortedSet(excludedDirs);
    }

    /**
     * Reads excluded path files.
     */
    private Excludes readExcludes() throws IOException {
        List<Pattern> patterns = new ArrayList<Pattern>();

        File globalExcludes = new File(toolDirectory, EXCLUDED_PATHS);
        parseFile(globalExcludes, patterns);

        // Look for Google-specific excludes.
        // TODO: Traverse all vendor-specific directories.
        File googleExcludes = new File("./vendor/google/" + EXCLUDED_PATHS);
        if (googleExcludes.exists()) {
            parseFile(googleExcludes, patterns);
        }

        // Look for user-specific excluded-paths file in current directory.
        File localExcludes = new File(EXCLUDED_PATHS);
        if (localExcludes.exists()) {
            parseFile(localExcludes, patterns);
        }

        return new Excludes(patterns);
    }

    /**
     * Recursively finds .java source roots, .jar files, and excluded
     * directories.
     */
    private static void traverse(File directory, Set<File> sourceRoots,
            Collection<File> jarFiles, Collection<File> excludedDirs,
            Excludes excludes) throws IOException {
        /*
         * Note it would be faster to stop traversing a source root as soon as
         * we encounter the first .java file, but it appears we have nested
         * source roots in our generated source directory (specifically,
         * R.java files and aidl .java files don't share the same source
         * root).
         */

        boolean firstJavaFile = true;
	File[] files = directory.listFiles();
	if (files == null) {
	    return;
	}
        for (File file : files) {
            // Trim preceding "./" from path.
            String path = file.getPath().substring(2);

            // Keep track of source roots for .java files.
            if (path.endsWith(".java")) {
                if (firstJavaFile) {
                    // Only parse one .java file per directory.
                    firstJavaFile = false;

                    File sourceRoot = rootOf(file);
                    if (sourceRoot != null) {
                        sourceRoots.add(sourceRoot);
                    }
                }
                                
                continue;
            }

            // Keep track of .jar files.
            if (path.endsWith(".jar")) {
                if (!excludes.exclude(path)) {
                    jarFiles.add(file);
                } else {
                    Log.debug("Skipped: " + file);
                }

                continue;
            }

            // Traverse nested directories.
            if (file.isDirectory()) {
                if (excludes.exclude(path)) {
                    // Don't recurse into excluded dirs.
                    Log.debug("Excluding: " + path);
                    excludedDirs.add(file);
                } else {
                    traverse(file, sourceRoots, jarFiles, excludedDirs,
                            excludes);
                }
            }
        }
    }

    /**
     * Determines the source root for a given .java file. Returns null
     * if the file doesn't have a package or if the file isn't in the
     * correct directory structure.
     */
    private static File rootOf(File javaFile) throws IOException {
        String packageName = parsePackageName(javaFile);
        if (packageName == null) {
            // No package.
            // TODO: Treat this as a source root?
            return null;
        }

        String packagePath = packageName.replace('.', File.separatorChar);
        File parent = javaFile.getParentFile();
        String parentPath = parent.getPath();
        if (!parentPath.endsWith(packagePath)) {
            // Bad dir structure.
            return null;
        }

        return new File(parentPath.substring(
                0, parentPath.length() - packagePath.length()));
    }

    /**
     * Reads a Java file and parses out the package name. Returns null if none
     * found.
     */
    private static String parsePackageName(File file) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("package")) {
                    // TODO: Make this more robust.
                    // Assumes there's only once space after "package" and the
                    // line ends in a ";".
                    return trimmed.substring(8, trimmed.length() - 1);
                }
            }

            return null;
        } finally {
            in.close();
        }
    }

    /**
     * Picks out excluded directories that are under source roots.
     */
    public SortedSet<File> excludesUnderSourceRoots() {
        // TODO: Refactor this to share the similar logic in
        // Eclipse.constructExcluding().
        SortedSet<File> picked = new TreeSet<File>();
        for (File sourceRoot : sourceRoots) {
            String sourcePath = sourceRoot.getPath() + "/";
            SortedSet<File> tailSet = excludedDirs.tailSet(sourceRoot);
            for (File file : tailSet) {
                if (file.getPath().startsWith(sourcePath)) {
                    picked.add(file);
                } else {
                    break;
                }
            }
        }
        return picked;
    }

    /**
     * Reads a list of regular expressions from a file, one per line, and adds
     * the compiled patterns to the given collection. Ignores lines starting
     * with '#'.
     *
     * @param file containing regular expressions, one per line
     * @param patterns collection to add compiled patterns from file to
     */
    public static void parseFile(File file, Collection<Pattern> patterns)
            throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(file));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() > 0 && !trimmed.startsWith("#")) {
                    patterns.add(Pattern.compile(trimmed));
                }
            }
        } finally {
            in.close();
        }
    }
}
