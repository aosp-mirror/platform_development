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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Super class for all modules.
 */
public abstract class Module {

    private static final Logger logger = Logger.getLogger(Module.class.getName());

    private static final String IML_TEMPLATE_FILE_NAME = "module-template.iml";

    /**
     * All possible attributes for the make file.
     */
    protected enum Key {
        LOCAL_STATIC_JAVA_LIBRARIES,
        LOCAL_JAVA_LIBRARIES,
        LOCAL_SRC_FILES
    }

    ModuleCache moduleCache = ModuleCache.getInstance();

    private File imlFile;

    private Set<String> allDependencies = Sets.newHashSet(); // direct + indirect

    private Set<File> allDependentImlFiles = Sets.newHashSet();

    protected abstract void build() throws IOException;

    protected abstract String getName();

    protected abstract File getDir();

    protected abstract boolean isAndroidModule();

    protected abstract List<File> getIntermediatesDirs();

    public abstract Set<String> getDirectDependencies();

    protected abstract ImmutableList<File> getSourceDirs();

    protected abstract ImmutableList<File> getExcludeDirs();

    public abstract File getRepoRoot();

    public void buildImlFile() throws IOException {
        String imlTemplate = Files.toString(
                new File(DirectorySearch.findTemplateDir(), IML_TEMPLATE_FILE_NAME),
                IntellijProject.CHARSET);

        String facetXml = "";
        if (isAndroidModule()) {
            facetXml = buildAndroidFacet();
        }
        imlTemplate = imlTemplate.replace("@FACETS@", facetXml);

        String moduleDir = getDir().getAbsolutePath();

        StringBuilder sourceDirectories = new StringBuilder();
        sourceDirectories.append("    <content url=\"file://$MODULE_DIR$\">\n");
        ImmutableList<File> srcDirs = getSourceDirs();
        for (File src : srcDirs) {
            String relative = src.getAbsolutePath().substring(moduleDir.length());
            sourceDirectories.append("      <sourceFolder url=\"file://$MODULE_DIR$")
                    .append(relative).append("\" isTestSource=\"false\" />\n");
        }
        ImmutableList<File> excludeDirs = getExcludeDirs();
        for (File src : excludeDirs) {
            String relative = src.getAbsolutePath().substring(moduleDir.length());
            sourceDirectories.append("      <excludeFolder url=\"file://$MODULE_DIR$")
                    .append(relative).append("\"/>\n");
        }
        sourceDirectories.append("    </content>\n");

        // Intermediates.
        sourceDirectories.append(buildIntermediates());

        imlTemplate = imlTemplate.replace("@SOURCES@", sourceDirectories.toString());

        StringBuilder moduleDependencies = new StringBuilder();
        for (String dependency : getDirectDependencies()) {
            moduleDependencies.append("    <orderEntry type=\"module\" module-name=\"")
                    .append(dependency).append("\" />\n");
        }
        imlTemplate = imlTemplate.replace("@MODULE_DEPENDENCIES@", moduleDependencies.toString());

        imlFile = new File(moduleDir, getName() + ".iml");
        logger.info("Creating " + imlFile.getAbsolutePath());
        Files.write(imlTemplate, imlFile, IntellijProject.CHARSET);
    }

    protected String buildIntermediates() {
        StringBuilder sb = new StringBuilder();
        for (File intermediatesDir : getIntermediatesDirs()) {
            sb.append("    <content url=\"file://").append(intermediatesDir).append("\">\n");
            sb.append("      <sourceFolder url=\"file://")
                    .append(intermediatesDir.getAbsolutePath())
                    .append("\" isTestSource=\"false\" />\n");
            sb.append("    </content>\n");
        }
        return sb.toString();
    }

    protected void buildDependentModules() throws IOException {
        Set<String> directDependencies = getDirectDependencies();
        String[] copy = directDependencies.toArray(new String[directDependencies.size()]);
        for (String dependency : copy) {

            Module child = moduleCache.getAndCache(dependency);
            if (child == null) {
                directDependencies.remove(dependency);
            } else {
                addAllDependencies(dependency);
                addAllDependencies(child.getAllDependencies());
                //logger.info("Adding iml " + child.getName() + " " + child.getImlFile());
                allDependentImlFiles.add(child.getImlFile());
                allDependentImlFiles.addAll(child.getAllDependentImlFiles());
            }
        }
    }

    public File getImlFile() {
        return imlFile;
    }

    public Set<String> getAllDependencies() {
        return allDependencies;
    }

    public void addAllDependencies(String dependency) {
        this.allDependencies.add(dependency);
    }

    public void addAllDependencies(Set<String> dependencies) {
        this.allDependencies.addAll(dependencies);
    }

    public Set<File> getAllDependentImlFiles() {
        return allDependentImlFiles;
    }

    private String buildAndroidFacet() {
        // Not sure how to handle android facet for multi-module since there could be more than
        // one intermediates directory.
        String dir = getIntermediatesDirs().get(0).getAbsolutePath();
        String xml = ""
                + "  <component name=\"FacetManager\">\n"
                + "    <facet type=\"android\" name=\"Android\">\n"
                + "      <configuration>\n"
                + "        <option name=\"GEN_FOLDER_RELATIVE_PATH_APT\" value=\"" + dir + "\" />\n"
                + "        <option name=\"GEN_FOLDER_RELATIVE_PATH_AIDL\" value=\"" + dir
                + "\" />\n"
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
    public String toString() {
        return Objects.toStringHelper(Module.class)
                .add("name", getName())
                .add("allDependencies", allDependencies)
                .add("iml files", allDependentImlFiles)
                .add("imlFile", imlFile)
                .toString();
    }
}

