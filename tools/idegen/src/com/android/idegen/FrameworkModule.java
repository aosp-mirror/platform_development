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

import com.google.common.collect.ImmutableList;

import java.io.File;

/**
 * Special module used for framework to build one off resource directory.
 */
public class FrameworkModule extends StandardModule {

    public FrameworkModule(String moduleName, String makeFile) {
        super(Constants.FRAMEWORK_MODULE, makeFile, true);
    }

    @Override
    protected String buildIntermediates() {
        StringBuilder sb = new StringBuilder();
        File intermediates = new File(repoRoot,
                Constants.REL_OUT_APP_DIR + File.separator +  Constants.FRAMEWORK_INTERMEDIATES);
        ImmutableList<File> intermediateSrcDirs = DirectorySearch.findSourceDirs(intermediates);
        sb.append("    <content url=\"file://").append(intermediates).append("\">\n");
        for (File src : intermediateSrcDirs) {
            sb.append("      <sourceFolder url=\"file://")
                    .append(src.getAbsolutePath()).append("\" isTestSource=\"false\" />\n");
        }
        sb.append("    </content>\n");
        return sb.toString();
    }
}
