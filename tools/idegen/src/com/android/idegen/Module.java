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
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Module constructed from a make file.
 *
 * TODO: read the make file and understand included source dirs in addition to searching
 * sub-directories.  Make files can include sources that are not sub-directories.  For example, the
 * framework module includes sources from:
 *
 * external/libphonenumber/java/src
 *
 * to provide:
 *
 * com.android.i18n.phonenumbers.PhoneNumberUtil;
 */
public class Module {

    private static final Logger logger = Logger.getLogger(Module.class.getName());

    public static final String REL_OUT_APP_DIR = "out/target/common/obj/APPS";

    private static final String IML_TEMPLATE_FILE_NAME = "module-template.iml";
    private static final String[] AUTO_DEPENDENCIES = new String[]{"framework", "libcore"};
    private static final String[] DIRS_WITH_AUTO_DEPENDENCIES = new String[]{"packages", "vendor",
            "frameworks/ex", "frameworks/opt", "frameworks/support"};

    /**
     * All possible attributes for the make file.
     */
    protected enum Key {
        LOCAL_STATIC_JAVA_LIBRARIES,
        LOCAL_JAVA_LIBRARIES,
        LOCAL_SRC_FILES
    }

    private ModuleCache moduleCache = ModuleCache.getInstance();

    private File imlFile;
    private Set<String> allDependencies = Sets.newHashSet(); // direct + indirect
    private Set<File> allDependentImlFiles = Sets.newHashSet();

    private File makeFile;
    private File moduleRoot;
    private HashSet<File> sourceFiles = Sets.newHashSet();

    // Module dependencies come from LOCAL_STATIC_JAVA_LIBRARIES or LOCAL_JAVA_LIBRARIES
    Set<String> explicitModuleNameDependencies = Sets.newHashSet();
    // Implicit module dependencies come from src files that fall outside the module root directory.
    // For example, if packages/apps/Contacts includes src files from packages/apps/ContactsCommon,
    // that is an implicit module dependency.  It's not a module dependency from the build
    // perspective but it needs to be a separate module in intellij so that the src files can be
    // shared by multiple intellij modules.
    Set<File> implicitModulePathDependencies = Sets.newHashSet();

    String relativeIntermediatesDir;
    MakeFileParser makeFileParser;
    boolean parseMakeFileForSource;

    public Module(File moduleDir) throws IOException {
        this(moduleDir, true);
    }

    public Module(File moduleDir, boolean parseMakeFileForSource) throws IOException {
        this.moduleRoot = Preconditions.checkNotNull(moduleDir);
        this.makeFile = new File(moduleDir, "Android.mk");
        this.relativeIntermediatesDir = calculateRelativePartToRepoRoot() + REL_OUT_APP_DIR +
                File.separatorChar + getName() + "_intermediates" + File.separator + "src";
        this.parseMakeFileForSource = parseMakeFileForSource;

        // TODO: auto-detect when framework dependency is needed instead of using coded list.
        for (String dir : DIRS_WITH_AUTO_DEPENDENCIES) {
            // length + 2 to account for slash
            boolean isDir = makeFile.getCanonicalPath().startsWith(
                    DirectorySearch.getRepoRoot() + "/" + dir);
            if (isDir) {
                Collections.addAll(this.explicitModuleNameDependencies, AUTO_DEPENDENCIES);
            }
        }

        makeFileParser = new MakeFileParser(makeFile);
    }

    private String calculateRelativePartToRepoRoot() throws IOException {
        String rel = moduleRoot.getCanonicalPath().substring(
                DirectorySearch.getRepoRoot().getCanonicalPath().length());
        int count = 0;
        // Count the number of slashes to determine how far back to go.
        for (int i = 0; i < rel.length(); i++) {
            if (rel.charAt(i) == '/') {
                count++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append("../");
        }
        return sb.toString();
    }

    public void build() throws IOException {
        makeFileParser.parse();
        buildDependencyList();
        buildDependentModules();
        logger.info("Done building module " + moduleRoot);
        logger.info(toString());
    }

    public File getDir() {
        return moduleRoot;
    }

    public String getName() {
        return moduleRoot.getName();
    }

    private List<String> getRelativeIntermediatesDirs() throws IOException {
        return Lists.newArrayList(relativeIntermediatesDir);
    }

    private ImmutableList<File> getSourceDirs() {
        return ImmutableList.copyOf(sourceFiles);
    }

    private ImmutableList<File> getExcludeDirs() {
        return DirectorySearch.findExcludeDirs(makeFile);
    }

    private boolean isAndroidModule() {
        File manifest = new File(moduleRoot, "AndroidManifest.xml");
        return manifest.exists();
    }

    private void findSourceFilesAndImplicitDependencies() throws IOException {
        Iterable<String> values = makeFileParser.getValues(Key.LOCAL_SRC_FILES.name());
        if (values != null) {
            for (String value : values) {
                File src = new File(moduleRoot, value);

                // value may contain garbage at this point due to relaxed make file parsing.
                // filter by existing file.
                if (src.exists()) {
                    // Look for directories outside the current module directory.
                    if (value.contains("..")) {
                        // Find the closest Android make file.
                        File moduleRoot = DirectorySearch.findModuleRoot(src);
                        implicitModulePathDependencies.add(moduleRoot);
                    } else {
                        if (parseMakeFileForSource) {
                            // Check if source files are subdirectories of generic parent src
                            // directories.  If so, no need to add since they are already included.
                            boolean alreadyIncluded = false;
                            for (String parentDir : DirectorySearch.SOURCE_DIRS) {
                                if (value.startsWith(parentDir)) {
                                    alreadyIncluded = true;
                                    break;
                                }
                            }

                            if (!alreadyIncluded) {
                                sourceFiles.add(src);
                            }
                        }
                    }
                }
            }
        }

        sourceFiles.addAll(DirectorySearch.findSourceDirs(moduleRoot));
    }

    private void buildDependencyList() throws IOException {
        parseDirectDependencies(Key.LOCAL_STATIC_JAVA_LIBRARIES);
        parseDirectDependencies(Key.LOCAL_JAVA_LIBRARIES);
        findSourceFilesAndImplicitDependencies();
    }

    private void parseDirectDependencies(Key key) {
        Iterable<String> names = makeFileParser.getValues(key.name());
        if (names != null) {
            for (String dependency : names) {
                explicitModuleNameDependencies.add(dependency);
            }
        }
    }

    public void buildImlFile() throws IOException {
        String imlTemplate = Files.toString(
                new File(DirectorySearch.findTemplateDir(), IML_TEMPLATE_FILE_NAME),
                IntellijProject.CHARSET);

        String facetXml = "";
        if (isAndroidModule()) {
            facetXml = buildAndroidFacet();
        }
        imlTemplate = imlTemplate.replace("@FACETS@", facetXml);

        String moduleDir = getDir().getCanonicalPath();

        StringBuilder sourceDirectories = new StringBuilder();
        sourceDirectories.append("    <content url=\"file://$MODULE_DIR$\">\n");
        ImmutableList<File> srcDirs = getSourceDirs();
        for (File src : srcDirs) {
            String relative = src.getCanonicalPath().substring(moduleDir.length());
            boolean isTestSource = false;
            if (relative.startsWith("/test")) {
                isTestSource = true;
            }
            sourceDirectories.append("      <sourceFolder url=\"file://$MODULE_DIR$")
                    .append(relative).append("\" isTestSource=\"").append(isTestSource)
                    .append("\" />\n");
        }
        ImmutableList<File> excludeDirs = getExcludeDirs();
        for (File src : excludeDirs) {
            String relative = src.getCanonicalPath().substring(moduleDir.length());
            sourceDirectories.append("      <excludeFolder url=\"file://$MODULE_DIR$")
                    .append(relative).append("\"/>\n");
        }
        sourceDirectories.append("    </content>\n");

        // Intermediates.
        sourceDirectories.append(buildIntermediates());

        imlTemplate = imlTemplate.replace("@SOURCES@", sourceDirectories.toString());

        StringBuilder moduleDependencies = new StringBuilder();
        for (String dependency : getAllDependencies()) {
            Module module = moduleCache.getAndCacheByDir(new File(dependency));
            moduleDependencies.append("    <orderEntry type=\"module\" module-name=\"")
                    .append(module.getName()).append("\" />\n");
        }
        imlTemplate = imlTemplate.replace("@MODULE_DEPENDENCIES@", moduleDependencies.toString());

        imlFile = new File(moduleDir, getName() + ".iml");
        logger.info("Creating " + imlFile.getCanonicalPath());
        Files.write(imlTemplate, imlFile, IntellijProject.CHARSET);
    }

    protected String buildIntermediates() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String intermediatesDir : getRelativeIntermediatesDirs()) {
            sb.append("    <content url=\"file://$MODULE_DIR$/").append(intermediatesDir)
                    .append("\">\n");
            sb.append("      <sourceFolder url=\"file://$MODULE_DIR$/")
                    .append(intermediatesDir)
                    .append("\" isTestSource=\"false\" />\n");
            sb.append("    </content>\n");
        }
        return sb.toString();
    }

    private void buildDependentModules() throws IOException {
        Set<String> moduleNameDependencies = explicitModuleNameDependencies;

        String[] copy = moduleNameDependencies.toArray(new String[moduleNameDependencies.size()]);
        for (String dependency : copy) {
            logger.info("Building dependency " + dependency);
            Module child = moduleCache.getAndCacheByName(dependency);
            if (child == null) {
                moduleNameDependencies.remove(dependency);
            } else {
                allDependencies.add(child.getDir().getCanonicalPath());
                //allDependencies.addAll(child.getAllDependencies());
                //logger.info("Adding iml " + child.getName() + " " + child.getImlFile());
                allDependentImlFiles.add(child.getImlFile());
                //allDependentImlFiles.addAll(child.getAllDependentImlFiles());
            }
        }
        // Don't include self.  The current module may have been brought in by framework
        // dependencies which will create a circular reference.
        allDependencies.remove(this.getDir().getCanonicalPath());
        allDependentImlFiles.remove(this.getImlFile());

        // TODO: add implicit dependencies.  Convert all modules to be based on directory.
        for (File dependency : implicitModulePathDependencies) {
            Module child = moduleCache.getAndCacheByDir(dependency);
            if (child != null) {
                allDependencies.add(child.getDir().getCanonicalPath());
                //allDependencies.addAll(child.getAllDependencies());
                //logger.info("Adding iml " + child.getName() + " " + child.getImlFile());
                allDependentImlFiles.add(child.getImlFile());
                //allDependentImlFiles.addAll(child.getAllDependentImlFiles());
            }
        }
    }

    public File getImlFile() {
        return imlFile;
    }

    public Set<String> getAllDependencies() {
        return allDependencies;
    }

    public Set<File> getAllDependentImlFiles() {
        return allDependentImlFiles;
    }

    private String buildAndroidFacet() throws IOException {
        // Not sure how to handle android facet for multi-module since there could be more than
        // one intermediates directory.
        String dir = getRelativeIntermediatesDirs().get(0);
        String xml = ""
                + "  <component name=\"FacetManager\">\n"
                + "    <facet type=\"android\" name=\"Android\">\n"
                + "      <configuration>\n"
                + "        <option name=\"GEN_FOLDER_RELATIVE_PATH_APT\" value=\"" +
                dir + "\" />\n"
                + "        <option name=\"GEN_FOLDER_RELATIVE_PATH_AIDL\" value=\"" +
                dir + "\" />\n"
                + "        <option name=\"MANIFEST_FILE_RELATIVE_PATH\" value=\""
                + "/AndroidManifest.xml\" />\n"
                + "        <option name=\"RES_FOLDER_RELATIVE_PATH\" value=\"/res\" />\n"
                + "        <option name=\"ASSETS_FOLDER_RELATIVE_PATH\" value=\"/assets\" />\n"
                + "        <option name=\"LIBS_FOLDER_RELATIVE_PATH\" value=\"/libs\" />\n"
                + "        <option name=\"REGENERATE_R_JAVA\" value=\"true\" />\n"
                + "        <option name=\"REGENERATE_JAVA_BY_AIDL\" value=\"true\" />\n"
                + "        <option name=\"USE_CUSTOM_APK_RESOURCE_FOLDER\" value=\"false\" />\n"
                + "        <option name=\"CUSTOM_APK_RESOURCE_FOLDER\" value=\"\" />\n"
                + "        <option name=\"USE_CUSTOM_COMPILER_MANIFEST\" value=\"false\" />\n"
                + "        <option name=\"CUSTOM_COMPILER_MANIFEST\" value=\"\" />\n"
                + "        <option name=\"APK_PATH\" value=\"\" />\n"
                + "        <option name=\"LIBRARY_PROJECT\" value=\"false\" />\n"
                + "        <option name=\"RUN_PROCESS_RESOURCES_MAVEN_TASK\" value=\"true\" />\n"
                + "        <option name=\"GENERATE_UNSIGNED_APK\" value=\"false\" />\n"
                + "      </configuration>\n"
                + "    </facet>\n"
                + "  </component>\n";
        return xml;
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
        Module other = (Module) obj;
        return Objects.equal(getName(), other.getName());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", getName())
                .add("allDependencies", allDependencies)
                .add("iml files", allDependentImlFiles).add("imlFile", imlFile)
                .add("makeFileParser", makeFileParser)
                .add("explicitModuleNameDependencies", Iterables.toString(
                        explicitModuleNameDependencies))
                .add("implicitModulePathDependencies", Iterables.toString(
                        implicitModulePathDependencies))
                .toString();
    }
}

