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

import java.io.File;
import java.io.IOException;

/**
 * Special module used for framework to build one off resource directory.
 */
public class FrameworkModule extends Module {

    // Framework needs a special constant for it's intermediates because it does not follow
    // normal conventions.
    private static final String FRAMEWORK_INTERMEDIATES = "framework-res_intermediates";

    public FrameworkModule(File moduleDir) throws IOException {
        super(Preconditions.checkNotNull(moduleDir), false);
    }

    @Override
    protected String buildIntermediates() throws IOException {
        StringBuilder sb = new StringBuilder();
        File intermediates = new File(DirectorySearch.getRepoRoot(),
                REL_OUT_APP_DIR + File.separator +  FRAMEWORK_INTERMEDIATES);
        ImmutableList<File> intermediateSrcDirs = DirectorySearch.findSourceDirs(intermediates);
        sb.append("    <content url=\"file://").append(intermediates).append("\">\n");
        for (File src : intermediateSrcDirs) {
            sb.append("      <sourceFolder url=\"file://")
                    .append(src.getCanonicalPath()).append("\" isTestSource=\"false\" />\n");
        }
        sb.append("    </content>\n");
        return sb.toString();
    }
}
