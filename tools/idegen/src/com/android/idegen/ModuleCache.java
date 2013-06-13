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
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Cache to hold built modules.
 */
public class ModuleCache {

    private static final Logger logger = Logger.getLogger(ModuleCache.class.getName());

    private static ModuleCache cache = new ModuleCache();

    ModuleIndexes indexes;

    // Mapping of canonical module directory to module.  Use string instead of File since File
    // does not provide equality based on canonical path.
    HashMap<String, Module> modulesByPath = Maps.newHashMap();

    private ModuleCache() {
    }

    public static ModuleCache getInstance() {
        return cache;
    }

    public void init(File indexFile) throws IOException {
        indexes = new ModuleIndexes(indexFile);
        indexes.build();
    }

    public Module getAndCacheByDir(File moduleDir) throws IOException {
        Preconditions.checkNotNull(moduleDir);

        if (moduleDir.exists()) {
            Module module = getModule(moduleDir);
            if (module == null) {
                module = new Module(moduleDir);
                // Must put module before building it.  Otherwise infinite loop.
                putModule(moduleDir, module);
                module.build();
            }
            return module;
        }
        return null;
    }

    public Module getAndCacheByName(String moduleName) throws IOException {
        Preconditions.checkState(indexes != null, "You must call init() first.");
        Preconditions.checkNotNull(moduleName);

        String makeFile = indexes.getMakeFile(moduleName);
        if (makeFile == null) {
            logger.warning("Unable to find make file for module: " + moduleName);
            return null;
        }
        return getAndCacheByDir(new File(makeFile).getParentFile());
    }

    private void putModule(File moduleDir, Module module) throws IOException {
        modulesByPath.put(moduleDir.getCanonicalPath(), module);
    }

    private Module getModule(File moduleDir) throws IOException {
        return modulesByPath.get(moduleDir.getCanonicalPath());
    }

    public Iterable<Module> getModules() {
        return modulesByPath.values();
    }

    public void put(Module module) throws IOException {
        Preconditions.checkNotNull(module);
        putModule(module.getDir(), module);
    }
}
