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
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Generates an Eclipse project.
 */
public class Eclipse {

    /**
     * Generates an Eclipse .classpath file from the given configuration.
     */
    public static void generateFrom(Configuration c) throws IOException {
        StringBuilder classpath = new StringBuilder();

        classpath.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<classpath>\n");

        /*
         * If the user has a file named "path-precedence" in their project's
         * root directory, we'll order source roots based on how they match
         * regular expressions in that file. Source roots that match earlier
         * patterns will come sooner in configuration file.
         */
        List<Pattern> patterns = new ArrayList<Pattern>();

        File precedence = new File("path-precedence");
        if (precedence.exists()) {
            Configuration.parseFile(precedence, patterns);
        } else {
            // Put ./out at the bottom by default.
            patterns.add(Pattern.compile("^(?!out/)"));
        }

        // Everything not matched by the user's precedence spec.
        patterns.add(Pattern.compile(".*"));


        List<Bucket> buckets = new ArrayList<Bucket>(patterns.size());
        for (Pattern pattern : patterns) {
            buckets.add(new Bucket(pattern));
        }

        // Put source roots in respective buckets.
        OUTER: for (File sourceRoot : c.sourceRoots) {
            // Trim preceding "./" from path.
            String path = sourceRoot.getPath().substring(2);

            for (Bucket bucket : buckets) {
                if (bucket.matches(path)) {
                    bucket.sourceRoots.add(sourceRoot);
                    continue OUTER;
                }
            }
        }

        // Output source roots to configuration file.
        for (Bucket bucket : buckets) {
            for (File sourceRoot : bucket.sourceRoots) {
                classpath.append("  <classpathentry kind=\"src\"");
                CharSequence excluding = constructExcluding(sourceRoot, c);
                if (excluding.length() > 0) {
                    classpath.append(" excluding=\"")
                            .append(excluding).append("\"");
                }
                classpath.append(" path=\"")
                        .append(trimmed(sourceRoot)).append("\"/>\n");
            }

        }

        // Output .jar entries.
        for (File jar : c.jarFiles) {
            classpath.append("  <classpathentry kind=\"lib\" path=\"")
                    .append(trimmed(jar)).append("\"/>\n");
        }

        /*
         * Output directory. Unfortunately, Eclipse forces us to put it
         * somewhere under the project directory.
         */
        classpath.append("  <classpathentry kind=\"output\" path=\""
                + "out/eclipse\"/>\n");

        classpath.append("</classpath>\n");

        Files.toFile(classpath.toString(), new File(".classpath"));
    }


    /**
     * Constructs the "excluding" argument for a given source root.
     */
    private static CharSequence constructExcluding(File sourceRoot,
            Configuration c) {
        StringBuilder classpath = new StringBuilder();
        String path = sourceRoot.getPath();

        // Exclude nested source roots.
        SortedSet<File> nextRoots = c.sourceRoots.tailSet(sourceRoot);
        int count = 0;
        for (File nextRoot : nextRoots) {
            // The first root is this root.
            if (count == 0) {
                count++;
                continue;
            }

            String nextPath = nextRoot.getPath();
            if (!nextPath.startsWith(path)) {
                break;
            }

            if (count > 1) {
                classpath.append('|');
            }
            classpath.append(nextPath.substring(path.length() + 1))
                    .append('/');

            count++;
        }

        // Exclude excluded directories under this source root.
        SortedSet<File> excludedDirs = c.excludedDirs.tailSet(sourceRoot);
        for (File excludedDir : excludedDirs) {
            String excludedPath = excludedDir.getPath();
            if (!excludedPath.startsWith(path)) {
                break;
            }

            if (count > 1) {
                classpath.append('|');
            }
            classpath.append(excludedPath.substring(path.length() + 1))
                    .append('/');

            count++;
        }

        return classpath;
    }

    /**
     * Returns the trimmed path.
     */
    private static String trimmed(File file) {
        return file.getPath().substring(2);
    }

    /**
     * A precedence bucket for source roots.
     */
    private static class Bucket {

        private final Pattern pattern;
        private final List<File> sourceRoots = new ArrayList<File>();

        private Bucket(Pattern pattern) {
            this.pattern = pattern;
        }

        private boolean matches(String path) {
            return pattern.matcher(path).find();
        }
    }
}
