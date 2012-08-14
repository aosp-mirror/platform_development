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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Module while is a composition of many other modules.
 * <p>
 * This is needed since intellij does not allow two modules to share the same content root.
 */
public class AggregatedModule extends Module {

    private static final Logger logger = Logger.getLogger(AggregatedModule.class.getName());

    private String aggregatedModuleName;
    private Set<Module> modules;
    private HashSet<String> directDependencies = Sets.newHashSet();

    public AggregatedModule(String aggregatedName, Set<Module> modules) {
        this.aggregatedModuleName = Preconditions.checkNotNull(aggregatedName);
        this.modules = Preconditions.checkNotNull(modules);
    }

    public void build() throws IOException {
        // Create an iml file that contains all the srcs of modules.
        buildDependentModules();
        buildDirectDependencies();
        //buildImlFile();
    }

    @Override
    protected File getDir() {
        // All modules should be in the same directory so just pull the first.
        return modules.iterator().next().getDir();
    }

    @Override
    protected boolean isAndroidModule() {
        for (Module module : modules) {
            if (module.isAndroidModule()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected List<File> getIntermediatesDirs() {
        List<File> result = Lists.newArrayList();
        for (Module module : modules) {
            Iterables.addAll(result, module.getIntermediatesDirs());
        }
        return result;
    }

    public void buildDirectDependencies() {
        for (Module module : modules) {
            Set<String> deps = module.getDirectDependencies();
            directDependencies.addAll(deps);
        }
    }

    @Override
    public Set<String> getDirectDependencies() {
        return directDependencies;
    }

    @Override
    protected ImmutableList<File> getSourceDirs() {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        for (Module module : modules) {
            builder.addAll(module.getSourceDirs());
        }
        return builder.build();
    }


    @Override
    protected ImmutableList<File> getExcludeDirs() {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        for (Module module : modules) {
            builder.addAll(module.getExcludeDirs());
        }
        return builder.build();
    }

    @Override
    public Set<File> getAllDependentImlFiles() {
        Set<File> result = Sets.newHashSet();
        for (Module module : modules) {
            result.addAll(module.getAllDependentImlFiles());
        }
        return result;
    }

    @Override
    public File getRepoRoot() {
        return modules.iterator().next().getRepoRoot();
    }

    @Override
    public String getName() {
        return aggregatedModuleName;
    }

}
