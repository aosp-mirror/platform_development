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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Module constructed from a make file.
 *
 * TODO: read the make file and understand included source dirs in addition to searching
 * sub-directories.  Make files can include sources that are not sub-directories.  For example,
 * the framework module includes sources from:
 *
 *   external/libphonenumber/java/src
 *
 *   to provide:
 *
 *   com.android.i18n.phonenumbers.PhoneNumberUtil;
 */
public class StandardModule extends Module {

    static final String REL_OUT_APP_DIR = "out/target/common/obj/APPS";

    private static final Logger logger = Logger.getLogger(StandardModule.class.getName());

    private static final Pattern SRC_PATTERN = Pattern.compile(
            ".*\\(call all-java-files-under, (.*)\\)");
    private static final String[] AUTO_DEPENDENCIES = new String[]{
            IntellijProject.FRAMEWORK_MODULE, "libcore"
    };
    private static final String[] DIRS_WITH_AUTO_DEPENDENCIES = new String[]{
            "packages", "vendor", "frameworks/ex", "frameworks/opt", "frameworks/support"
    };

    String moduleName;
    File makeFile;
    File moduleRoot;
    File repoRoot;

    Set<String> directDependencies = Sets.newHashSet();

    File intermediatesDir;
    MakeFileParser makeFileParser;
    boolean searchForSrc;

    public StandardModule(String moduleName, String makeFile) {
        this(moduleName, new File(makeFile), false);
    }

    public StandardModule(String moduleName, String makeFile, boolean searchForSrc) {
        this(Preconditions.checkNotNull(moduleName), new File(Preconditions.checkNotNull(makeFile)),
                searchForSrc);
    }

    public StandardModule(String moduleName, File makeFile, boolean searchForSrc) {
        this.moduleName = moduleName;
        this.makeFile = makeFile;
        this.moduleRoot = makeFile.getParentFile();
        this.repoRoot = DirectorySearch.findRepoRoot(makeFile);
        this.intermediatesDir = new File(repoRoot.getAbsolutePath() + File.separator +
                REL_OUT_APP_DIR + File.separator + getName() + "_intermediates" +
                File.separator + "src");
        this.searchForSrc = searchForSrc;

        // TODO: auto-detect when framework dependency is needed instead of using coded list.
        for (String dir : DIRS_WITH_AUTO_DEPENDENCIES) {
            // length + 2 to account for slash
            boolean isDir = makeFile.getAbsolutePath().startsWith(repoRoot + "/" + dir);
            if (isDir) {
                for (String dependency : AUTO_DEPENDENCIES) {
                    this.directDependencies.add(dependency);
                }
            }
        }

        makeFileParser = new MakeFileParser(makeFile, moduleName);
    }

    protected void build() throws IOException {
        makeFileParser.parse();
        buildDependencyList();
        buildDependentModules();
        //buildImlFile();
        logger.info("Done building module " + moduleName);
        logger.info(toString());
    }

    @Override
    protected File getDir() {
        return moduleRoot;
    }

    @Override
    protected String getName() {
        return moduleName;
    }

    @Override
    protected List<File> getIntermediatesDirs() {
        return Lists.newArrayList(intermediatesDir);
    }

    @Override
    public File getRepoRoot() {
        return this.repoRoot;
    }

    public Set<String> getDirectDependencies() {
        return this.directDependencies;
    }

    @Override
    protected ImmutableList<File> getSourceDirs() {
        ImmutableList<File> srcDirs;
        if (searchForSrc) {
            srcDirs = DirectorySearch.findSourceDirs(makeFile);
        } else {
            srcDirs = parseSourceFiles(makeFile);
        }
        return srcDirs;
    }

    @Override
    protected ImmutableList<File> getExcludeDirs() {
        return DirectorySearch.findExcludeDirs(makeFile);
    }

    @Override
    protected boolean isAndroidModule() {
        File manifest = new File(moduleRoot, "AndroidManifest.xml");
        return manifest.exists();
    }

    private ImmutableList<File> parseSourceFiles(File root) {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        File rootDir;
        if (root.isFile()) {
            rootDir = root.getParentFile();
        } else {
            rootDir = root;
        }

        Iterable<String> values = makeFileParser.getValues(Key.LOCAL_SRC_FILES.name());
        if (values != null) {
            for (String value : values) {
                Matcher matcher = SRC_PATTERN.matcher(value);
                if (matcher.matches()) {
                    String dir = matcher.group(1);
                    builder.add(new File(rootDir, dir));
                } else if (value.contains("/")) {
                    // Treat as individual file.
                    builder.add(new File(rootDir, value));
                }
            }
        }
        return builder.build();
    }

    private void buildDependencyList() {
        parseDirectDependencies(Key.LOCAL_STATIC_JAVA_LIBRARIES);
        parseDirectDependencies(Key.LOCAL_JAVA_LIBRARIES);
    }

    private void parseDirectDependencies(Key key) {
        Iterable<String> names = makeFileParser.getValues(key.name());
        if (names != null) {
            for (String dependency : names) {
                directDependencies.add(dependency);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StandardModule other = (StandardModule) obj;
        return Objects.equal(getName(), other.getName());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("super", super.toString())
                .add("makeFileParser", makeFileParser)
                .add("directDependencies", Iterables.toString(directDependencies))
                .toString();
    }
}
