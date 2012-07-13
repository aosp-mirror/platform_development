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
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Cache to hold built modules.
 */
public class ModuleCache {

    private static final Logger logger = Logger.getLogger(ModuleCache.class.getName());

    private static ModuleCache cache = new ModuleCache();

    ModuleIndexes indexes;

    HashMap<String, Module> modulesByName = Maps.newHashMap();

    private ModuleCache() {
    }

    public static ModuleCache getInstance() {
        return cache;
    }

    public void init(File indexFile) throws IOException {
        indexes = new ModuleIndexes(indexFile);
        indexes.build();
    }

    public Module getAndCache(String moduleName) throws IOException {
        Preconditions.checkState(indexes != null, "You must call init() first.");

        Module module = modulesByName.get(moduleName);
        if (module == null) {
            String makeFile = indexes.getMakeFile(moduleName);
            if (makeFile == null) {
                logger.warning("Unable to find make file for module: " + moduleName);
            } else {
                module = new StandardModule(moduleName, makeFile);
                module.build();
                modulesByName.put(moduleName, module);
            }
        }
        return module;
    }

    public void buildAndCacheAggregatedModule(String moduleName) throws IOException {
        if (indexes.isPartOfAggregatedModule(moduleName)) {
            Set<String> moduleNames = indexes.getAggregatedModules(moduleName);
            Set<Module> modules = Sets.newHashSet();
            for (String name : moduleNames) {
                Module m = modulesByName.get(name);
                if (m != null) {
                    modules.add(m);
                }
            }
            String aggregatedName = indexes.getAggregateName(moduleName);
            AggregatedModule module = new AggregatedModule(aggregatedName, modules);
            module.build();
            modulesByName.put(aggregatedName, module);
        }
    }

    public Iterable<Module> getModules() {
        return modulesByName.values();
    }

    public String getMakeFile(String moduleName) {
        return indexes.getMakeFile(moduleName);
    }

    public void put(StandardModule module) {
        Preconditions.checkNotNull(module);
        modulesByName.put(module.getName(), module);
    }

    public String getAggregateReplacementName(String moduleName) {
        if (indexes.isPartOfAggregatedModule(moduleName)) {
            return indexes.getAggregateName(moduleName);
        }
        return null;
    }
}
