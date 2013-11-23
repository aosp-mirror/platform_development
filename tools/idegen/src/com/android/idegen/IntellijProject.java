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
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Build Intellij projects.
 */
public class IntellijProject {

    public static final String FRAMEWORK_MODULE = "framework";
    public static final Charset CHARSET = Charset.forName("UTF-8");

    private static final Logger logger = Logger.getLogger(IntellijProject.class.getName());

    private static final String MODULES_TEMPLATE_FILE_NAME = "modules.xml";
    private static final String VCS_TEMPLATE_FILE_NAME = "vcs.xml";

    ModuleCache cache = ModuleCache.getInstance();

    File indexFile;
    File repoRoot;
    File projectIdeaDir;
    String moduleName;

    public IntellijProject(String indexFile, String moduleName) {
        this.indexFile = new File(Preconditions.checkNotNull(indexFile));
        this.moduleName = Preconditions.checkNotNull(moduleName);
    }

    private void init() throws IOException {
        repoRoot = DirectorySearch.findRepoRoot(indexFile);
        cache.init(indexFile);
    }

    public void build() throws IOException {
        init();
        buildFrameWorkModule();

        // First pass, find all dependencies and cache them.
        Module module = cache.getAndCache(moduleName);
        if (module == null) {
            logger.info("Module '" + moduleName + "' not found." +
                    " Module names are case senstive.");
            return;
        }
        projectIdeaDir = new File(module.getDir(), ".idea");
        projectIdeaDir.mkdir();
        copyTemplates();

        // Second phase, build aggregate modules.
        Set<String> deps = module.getAllDependencies();
        for (String dep : deps) {
            cache.buildAndCacheAggregatedModule(dep);
        }

        // Third phase, replace individual modules with aggregated modules
        Iterable<Module> modules = cache.getModules();
        for (Module mod : modules) {
            replaceWithAggregate(mod);
        }

        // Finally create iml files for dependencies
        for (Module mod : modules) {
            mod.buildImlFile();
        }

        createModulesFile(module);
        createVcsFile(module);
        createNameFile(moduleName);
    }

    private void replaceWithAggregate(Module module) {
        replaceWithAggregate(module.getDirectDependencies(), module.getName());
        replaceWithAggregate(module.getAllDependencies(), module.getName());

    }

    private void replaceWithAggregate(Set<String> deps, String moduleName) {
        for (String dep : Sets.newHashSet(deps)) {
            String replacement = cache.getAggregateReplacementName(dep);
            if (replacement != null) {

                deps.remove(dep);
                // There could be dependencies on self due to aggregation.
                // Only add if the replacement is not self.
                if (!replacement.equals(moduleName)) {
                    deps.add(replacement);
                }
            }
        }
    }

    /**
     * Framework module needs special handling due to one off resource path:
     * frameworks/base/Android.mk
     */
    private void buildFrameWorkModule() throws IOException {
        String makeFile = cache.getMakeFile(FRAMEWORK_MODULE);
        if (makeFile == null) {
            logger.warning("Unable to find framework module: " + FRAMEWORK_MODULE +
                    ". Skipping.");
        } else {
            logger.info("makefile: " + makeFile);
            StandardModule frameworkModule = new FrameworkModule(FRAMEWORK_MODULE,
                    makeFile);
            frameworkModule.build();
            cache.put(frameworkModule);
        }
    }

    private void createModulesFile(Module module) throws IOException {
        String modulesContent = Files.toString(
                new File(DirectorySearch.findTemplateDir(),
                        "idea" + File.separator + MODULES_TEMPLATE_FILE_NAME),
                CHARSET);
        StringBuilder sb = new StringBuilder();
        File moduleIml = module.getImlFile();
        sb.append("      <module fileurl=\"file://").append(moduleIml.getAbsolutePath())
                .append("\" filepath=\"").append(moduleIml.getAbsolutePath()).append("\" />\n");
        for (String name : module.getAllDependencies()) {
            Module mod = cache.getAndCache(name);
            File iml = mod.getImlFile();
            sb.append("      <module fileurl=\"file://").append(iml.getAbsolutePath())
                    .append("\" filepath=\"").append(iml.getAbsolutePath()).append("\" />\n");
        }
        modulesContent = modulesContent.replace("@MODULES@", sb.toString());

        File out = new File(projectIdeaDir, "modules.xml");
        logger.info("Creating " + out.getAbsolutePath());
        Files.write(modulesContent, out, CHARSET);
    }

    private void createVcsFile(Module module) throws IOException {
        String vcsTemplate = Files.toString(
                new File(DirectorySearch.findTemplateDir(),
                        "idea" + File.separator + VCS_TEMPLATE_FILE_NAME),
                CHARSET);

        StringBuilder sb = new StringBuilder();
        for (String name : module.getAllDependencies()) {
            Module mod = cache.getAndCache(name);
            File dir = mod.getDir();
            File gitRoot = new File(dir, ".git");
            if (gitRoot.exists()) {
                sb.append("    <mapping directory=\"").append(dir.getAbsolutePath())
                        .append("\" vcs=\"Git\" />\n");
            }
        }
        vcsTemplate = vcsTemplate.replace("@VCS@", sb.toString());
        Files.write(vcsTemplate, new File(projectIdeaDir, "vcs.xml"), CHARSET);
    }

    private void createNameFile(String name) throws IOException {
        File out = new File(projectIdeaDir, ".name");
        Files.write(name, out, CHARSET);
    }

    private void copyTemplates() throws IOException {
        File templateDir = DirectorySearch.findTemplateDir();
        copyTemplates(new File(templateDir, "idea"), projectIdeaDir);
    }

    private void copyTemplates(File fromDir, File toDir) throws IOException {
        toDir.mkdir();
        File[] files = fromDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                File destDir = new File(toDir, file.getName());
                copyTemplates(file, destDir);
            } else {
                File toFile = new File(toDir, file.getName());
                logger.info("copying " + file.getAbsolutePath() + " to " +
                        toFile.getAbsolutePath());
                Files.copy(file, toFile);
            }
        }
    }

    public static void main(String[] args) {
        logger.info("Args: " + Arrays.toString(args));

        String indexFile = args[0];
        String module = args[1];

        IntellijProject intellij = new IntellijProject(indexFile, module);
        try {
            intellij.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
