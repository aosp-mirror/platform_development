/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.ide.eclipse.mock;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class ClasspathEntryMock implements IClasspathEntry {

    private int mKind;
    private IPath mPath;

    public ClasspathEntryMock(IPath path, int kind) {
        mPath = path;
        mKind = kind;
    }
    
    public int getEntryKind() {
        return mKind;
    }
    
    public IPath getPath() {
        return mPath;
    }
    
    // -------- UNIMPLEMENTED METHODS ----------------

    public boolean combineAccessRules() {
        throw new NotImplementedException();
    }

    public IAccessRule[] getAccessRules() {
        throw new NotImplementedException();
    }

    public int getContentKind() {
        throw new NotImplementedException();
    }

    public IPath[] getExclusionPatterns() {
        throw new NotImplementedException();
    }

    public IClasspathAttribute[] getExtraAttributes() {
        throw new NotImplementedException();
    }

    public IPath[] getInclusionPatterns() {
        throw new NotImplementedException();
    }

    public IPath getOutputLocation() {
        throw new NotImplementedException();
    }

    public IClasspathEntry getResolvedEntry() {
        throw new NotImplementedException();
    }

    public IPath getSourceAttachmentPath() {
        throw new NotImplementedException();
    }

    public IPath getSourceAttachmentRootPath() {
        throw new NotImplementedException();
    }

    public boolean isExported() {
        throw new NotImplementedException();
    }
}
