/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.sdklib.internal.repository.Archive.Arch;
import com.android.sdklib.internal.repository.Archive.Os;

/**
 * A mock {@link ToolPackage} for testing.
 *
 * By design, this package contains one and only one archive.
 */
public class MockToolPackage extends ToolPackage {

    /**
     * Creates a {@link MockToolPackage} with the given revision and hardcoded defaults
     * for everything else.
     * <p/>
     * By design, this creates a package with one and only one archive.
     */
    public MockToolPackage(int revision) {
        super(
            null, // source,
            null, // props,
            revision,
            null, // license,
            "desc", // description,
            "url", // descUrl,
            Os.getCurrentOs(), // archiveOs,
            Arch.getCurrentArch(), // archiveArch,
            "foo" // archiveOsPath
            );
    }
}
