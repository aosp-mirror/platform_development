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
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mapping of module names to make files.
 */
public class ModuleIndexes {

    private static final Logger logger = Logger.getLogger(ModuleIndexes.class.getName());

    private File indexFile;
    private HashMap<String, String> moduleNameToMakeFileMap;
    private HashMap<String, Set<String>> makeFileToModuleNamesMap;

    public ModuleIndexes(File indexFile) {
        this.indexFile = indexFile;
    }

    public void build() throws IOException {

        moduleNameToMakeFileMap = Maps.newHashMap();
        makeFileToModuleNamesMap = Maps.newHashMap();
        logger.info("Building index from " + indexFile.getAbsolutePath());
        Files.readLines(indexFile, Charset.forName("UTF-8"),
                new LineProcessor<Object>() {
                    int count = 0;

                    @Override
                    public boolean processLine(String line) throws IOException {
                        count++;
                        String[] arr = line.split(":");
                        if (arr.length < 2) {
                            logger.log(Level.WARNING,
                                    "Ignoring index line " + count + ". Bad format: " + line);
                        } else {
                            String makeFile = arr[0];
                            String moduleName = arr[1];
                            moduleNameToMakeFileMap.put(moduleName, makeFile);
                            append(makeFile, moduleName);
                        }
                        return true;
                    }

                    @Override
                    public Object getResult() {
                        return null;
                    }
                });
    }

    private void append(String makeFile, String moduleName) {
        Set<String> moduleNames = makeFileToModuleNamesMap.get(makeFile);
        if (moduleNames == null) {
            moduleNames = Sets.newHashSet();
            makeFileToModuleNamesMap.put(makeFile, moduleNames);
        } else {
            // Create a aggregate module place holder.
            //moduleNameToMakeFileMap.put(getAggregateName(moduleName), makeFile);
        }
        moduleNames.add(moduleName);
    }

    public String getMakeFile(String moduleName) {
        Preconditions.checkState(moduleNameToMakeFileMap != null,
                "Index not built. Call build() first.");
        return moduleNameToMakeFileMap.get(moduleName);
    }

    public Set<String> getAggregatedModules(String moduleName) {
        Preconditions.checkState(makeFileToModuleNamesMap != null,
                "Index not built. Call build() first.");
        String makeFile = getMakeFile(moduleName);
        return makeFileToModuleNamesMap.get(makeFile);
    }

    public boolean isPartOfAggregatedModule(String moduleName) {
        String makeFile = getMakeFile(moduleName);
        if (makeFile == null) {
            return false;
        }
        Set<String> moduleNames = makeFileToModuleNamesMap.get(makeFile);
        if (moduleNames == null) {
            return false;
        }
        return moduleNames.size() > 1;
    }

    public String getAggregateName(String moduleName) {
        String fileName = getMakeFile(moduleName);
        File file = new File(fileName);
        return file.getParentFile().getName() + "-aggregate";
    }
}
