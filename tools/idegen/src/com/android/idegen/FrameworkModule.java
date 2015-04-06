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

    private static final String REL_OUT_LIB_DIR = "out/target/common/obj/JAVA_LIBRARIES";

    // Framework needs a special constant for it's intermediates because it does not follow
    // normal conventions.
    private static final ImmutableList<String> FRAMEWORK_APP_INTERMEDIATES = ImmutableList.of(
            "framework-res_intermediates"
           );
    private static final ImmutableList<String> FRAMEWORK_LIB_INTERMEDIATES = ImmutableList.of(
            "framework_intermediates",
            "services.core_intermediates"
    );

    public FrameworkModule(File moduleDir) throws IOException {
        super(Preconditions.checkNotNull(moduleDir), false);
    }

    @Override
    protected String buildIntermediates() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String intermediate : FRAMEWORK_APP_INTERMEDIATES) {
            appendContentRoot(sb, DirectorySearch.getRepoRoot() + File.separator +
                    REL_OUT_APP_DIR + File.separator + intermediate);
        }
        for (String intermediate : FRAMEWORK_LIB_INTERMEDIATES) {
            appendContentRoot(sb, DirectorySearch.getRepoRoot() + File.separator +
                    REL_OUT_LIB_DIR + File.separator + intermediate);
        }
        return sb.toString();
    }

    private void appendContentRoot(StringBuilder stringBuilder, String rootPath)
            throws IOException {
        File intermediates = new File(rootPath);
        ImmutableList<File> intermediateSrcDirs = DirectorySearch.findSourceDirs(intermediates);
        stringBuilder.append("    <content url=\"file://").append(intermediates).append("\">\n");
        for (File src : intermediateSrcDirs) {
            stringBuilder.append("      <sourceFolder url=\"file://")
                    .append(src.getCanonicalPath()).append("\" isTestSource=\"false\" />\n");
        }
        stringBuilder.append("    </content>\n");
    }
}
