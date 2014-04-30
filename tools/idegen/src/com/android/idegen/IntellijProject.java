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
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Build Intellij projects.
 */
public class IntellijProject {

    public static final String FRAMEWORK_MODULE_DIR = "frameworks/base";
    public static final Charset CHARSET = Charset.forName("UTF-8");

    private static final Logger logger = Logger.getLogger(IntellijProject.class.getName());

    private static final String MODULES_TEMPLATE_FILE_NAME = "modules.xml";
    private static final String VCS_TEMPLATE_FILE_NAME = "vcs.xml";

    ModuleCache cache = ModuleCache.getInstance();

    boolean buildFramework;
    File indexFile;
    File projectPath;
    ArrayList<String> moduleDirs;

    public IntellijProject(String indexFile, String projectPath, ArrayList<String> moduleDirs,
            boolean buildFramework) {
        this.indexFile = new File(Preconditions.checkNotNull(indexFile));
        this.projectPath = new File(Preconditions.checkNotNull(projectPath));
        this.moduleDirs = Preconditions.checkNotNull(moduleDirs);
        this.buildFramework = buildFramework;
        DirectorySearch.findAndInitRepoRoot(this.indexFile);
    }

    public void build() throws IOException {
        cache.init(indexFile);
        File repoRoot = DirectorySearch.getRepoRoot();
        if (buildFramework) {
            File frameworkDir = new File(repoRoot, FRAMEWORK_MODULE_DIR);
            // Some unbundled apps/branches do not include the framework.
            if (frameworkDir.exists()) {
                buildFrameWorkModule(new File(repoRoot, FRAMEWORK_MODULE_DIR));
            }
        }

        for (String moduleDir : moduleDirs) {
            // First pass, find all dependencies and cache them.
            File dir = new File(repoRoot, moduleDir);
            if (!dir.exists()) {
                logger.info("Directory " + moduleDir + " does not exist in " + repoRoot +
                        ". Are you sure the directory is correct?");
                return;
            }
            Module module = cache.getAndCacheByDir(dir);
            if (module == null) {
                logger.info("Module '" + dir.getPath() + "' not found." +
                        " Module names are case senstive.");
                return;
            }
        }

        // Finally create iml files for dependencies
        Iterable<Module> modules = cache.getModules();
        for (Module mod : modules) {
            mod.buildImlFile();
        }

        createProjectFiles();
    }

    private void createProjectFiles() throws IOException {
        File ideaDir = new File(projectPath, ".idea");
        ideaDir.mkdirs();
        copyTemplates(ideaDir);
        createModulesFile(ideaDir, cache.getModules());
        createVcsFile(ideaDir, cache.getModules());
        createNameFile(ideaDir, projectPath.getName());
    }

    /**
     * Framework module needs special handling due to one off resource path:
     * frameworks/base/Android.mk
     */
    private void buildFrameWorkModule(File frameworkModuleDir) throws IOException {
        FrameworkModule frameworkModule = new FrameworkModule(frameworkModuleDir);
        frameworkModule.build();
        cache.put(frameworkModule);
    }

    private void createModulesFile(File ideaDir, Iterable<Module> modules) throws IOException {
        String modulesContent = Files.toString(new File(DirectorySearch.findTemplateDir(),
                "idea" + File.separator + MODULES_TEMPLATE_FILE_NAME), CHARSET);
        StringBuilder sb = new StringBuilder();
        for (Module mod : modules) {
            File iml = mod.getImlFile();
            sb.append("      <module fileurl=\"file://").append(iml.getCanonicalPath()).append(
                    "\" filepath=\"").append(iml.getCanonicalPath()).append("\" />\n");
        }
        modulesContent = modulesContent.replace("@MODULES@", sb.toString());

        File out = new File(ideaDir, "modules.xml");
        logger.info("Creating " + out.getCanonicalPath());
        Files.write(modulesContent, out, CHARSET);
    }

    private void createVcsFile(File ideaDir, Iterable<Module> modules) throws IOException {
        String vcsTemplate = Files.toString(new File(DirectorySearch.findTemplateDir(),
                "idea" + File.separator + VCS_TEMPLATE_FILE_NAME), CHARSET);

        StringBuilder sb = new StringBuilder();
        for (Module mod : modules) {
            File dir = mod.getDir();
            File gitRoot = new File(dir, ".git");
            if (gitRoot.exists()) {
                sb.append("    <mapping directory=\"").append(dir.getCanonicalPath()).append(
                        "\" vcs=\"Git\" />\n");
            }
        }
        vcsTemplate = vcsTemplate.replace("@VCS@", sb.toString());
        Files.write(vcsTemplate, new File(ideaDir, "vcs.xml"), CHARSET);
    }

    private void createNameFile(File ideaDir, String name) throws IOException {
        File out = new File(ideaDir, ".name");
        Files.write(name, out, CHARSET);
    }

    private void copyTemplates(File ideaDir) throws IOException {
        File templateDir = DirectorySearch.findTemplateDir();
        copyTemplates(new File(templateDir, "idea"), ideaDir);
    }

    private void copyTemplates(File fromDir, File toDir) throws IOException {
        toDir.mkdir();
        File[] files = fromDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                File destDir = new File(toDir, file.getName());
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                copyTemplates(file, destDir);
            } else {
                File toFile = new File(toDir, file.getName());
                logger.info("copying " + file.getCanonicalPath() + " to " +
                        toFile.getCanonicalPath());
                Files.copy(file, toFile);
            }
        }
    }

    public static void main(String[] args) {
        logger.info("Args: " + Arrays.toString(args));

        if (args.length < 3) {
            logger.severe("Not enough input arguments. Aborting");
            return;
        }

        boolean buildFramework = true;
        int argIndex = 0;
        String arg = args[argIndex];
        while (arg.startsWith("--")) {
            if  (arg.equals("--no-framework")) {
                buildFramework = false;
            }
            argIndex++;
            arg = args[argIndex];
        }

        String indexFile = args[argIndex++];
        String projectPath = args[argIndex++];
        // Remaining args are module directories
        ArrayList<String> moduleDirs = Lists.newArrayList();
        for (int i = argIndex; i < args.length; i++) {
            moduleDirs.add(args[i]);
        }

        IntellijProject intellij = new IntellijProject(indexFile, projectPath, moduleDirs,
                buildFramework);
        try {
            intellij.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
