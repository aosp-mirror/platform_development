/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.project;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;

/**
 * Classpath container for the Android projects.
 */
class AndroidClasspathContainer implements IClasspathContainer {
    
    private IClasspathEntry[] mClasspathEntry;
    private IPath mContainerPath;
    private String mName;
    
    /**
     * Constructs the container with the {@link IClasspathEntry} representing the android
     * framework jar file and the container id
     * @param entries the entries representing the android framework and optional libraries.
     * @param path the path containing the classpath container id.
     * @param name the name of the container to display.
     */
    AndroidClasspathContainer(IClasspathEntry[] entries, IPath path, String name) {
        mClasspathEntry = entries;
        mContainerPath = path;
        mName = name;
    }
    
    public IClasspathEntry[] getClasspathEntries() {
        return mClasspathEntry;
    }

    public String getDescription() {
        return mName;
    }

    public int getKind() {
        return IClasspathContainer.K_DEFAULT_SYSTEM;
    }

    public IPath getPath() {
        return mContainerPath;
    }
}
