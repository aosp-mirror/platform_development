/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.idegen;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find directories utility.
 */
public class DirectorySearch {

    private static final Logger logger = Logger.getLogger(DirectorySearch.class.getName());

    private static final HashSet<String> SOURCE_DIRS = Sets.newHashSet();
    static {
        SOURCE_DIRS.add("src");
        SOURCE_DIRS.add("java");
    }

    private static final Pattern EXCLUDE_PATTERN = Pattern.compile("values-..(-.*)*");

    private static File repoRoot = null;
    public static final String REL_TEMPLATE_DIR = "templates";
    public static final String REL_TEMPLATE_PATH_FROM_ROOT = "development/tools/idegen/"
            + REL_TEMPLATE_DIR;

    /**
     * Find the repo root.  This is the root branch directory of a full repo checkout.
     *
     * @param file any file inside the root.
     * @return the root directory.
     */
    public static File findRepoRoot(File file) {
        Preconditions.checkNotNull(file);
        if (repoRoot != null) {
            return repoRoot;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                   if (".repo".equals(name)) {
                       return true;
                   }
                   return false;
                }
            });
            if (files.length > 0) {
                repoRoot = file;
                return file;
            }
        }
        File parent = file.getParentFile();
        if (parent == null) {
            return null;
        }
        return findRepoRoot(parent);
    }

    /**
     * Find all source directories from a given root file.
     *
     * If the root file is a file, the directory of that file will be used as the starting
     * location.
     *
     * @param file The starting location. Can be a file or directory.
     * @return List of
     */
    public static ImmutableList<File> findSourceDirs(File file) {
        Preconditions.checkNotNull(file);
        if (!file.exists()) {
            return ImmutableList.of();
        }
        if (!file.isDirectory()) {
            file = file.getParentFile();
        }
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        File[] children = file.listFiles();
        for (File child : children) {
            if (child.isDirectory()) {
                if (SOURCE_DIRS.contains(child.getName())) {
                    builder.add(child);
                } else {
                    ImmutableList<File> dirs = findSourceDirs(child);
                    builder.addAll(dirs);
                }
            }
        }

        return builder.build();
    }

    public static ImmutableList<File> findExcludeDirs(File file) {
        Preconditions.checkNotNull(file);
        if (!file.exists()) {
            return ImmutableList.of();
        }
        if (!file.isDirectory()) {
            file = file.getParentFile();
        }
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        // Go into the res folder
        File resFile = new File(file, "res");
        if (resFile.exists()) {

            File[] children = resFile.listFiles();
            for (File child : children) {
                if (child.isDirectory()) {
                    Matcher matcher = EXCLUDE_PATTERN.matcher(child.getName());
                    if (matcher.matches()) {
                        // Exclude internationalization language folders.
                        // ex: values-zh
                        // But don't exclude values-land.  Assume all language folders are two
                        // letters.
                        builder.add(child);
                    }
                }
            }
        }

        return builder.build();
    }

    private static File templateDirCurrent = null;
    private static File templateDirRoot = null;

    public static File findTemplateDir() throws FileNotFoundException {
        // Cache optimization.
        if (templateDirCurrent != null && templateDirCurrent.exists()) return templateDirCurrent;
        if (templateDirRoot != null && templateDirRoot.exists()) return templateDirRoot;

        File currentDir = null;
        try {
            currentDir = new File(IntellijProject.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI().getPath()).getParentFile();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Could not get jar location.", e);
            return null;
        }

        // First check relative to current run directory.
        templateDirCurrent = new File(currentDir, REL_TEMPLATE_DIR);
        if (templateDirCurrent.exists()) {
            return templateDirCurrent;
        } else {
            // Then check relative to root directory.
            templateDirRoot = new File(repoRoot, REL_TEMPLATE_PATH_FROM_ROOT);
            if (templateDirRoot.exists()) {
                return templateDirRoot;
            }
        }
        throw new FileNotFoundException(
                "Unable to find template dir. Tried the following locations:\n" +
                templateDirCurrent.getAbsolutePath() + "\n" +
                templateDirRoot.getAbsolutePath());
    }
}
