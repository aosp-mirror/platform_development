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

package com.android.ide.eclipse.adt.internal.resources.manager.files;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import java.io.InputStream;

/**
 * An implementation of {@link IAbstractFile} on top of an {@link IFile} object.
 */
public class IFileWrapper implements IAbstractFile {

    private IFile mFile;

    public IFileWrapper(IFile file) {
        mFile = file;
    }
    
    public InputStream getContents() throws CoreException {
        return mFile.getContents();
    }

    public String getOsLocation() {
        return mFile.getLocation().toOSString();
    }

    public String getName() {
        return mFile.getName();
    }

    public IFile getIFile() {
        return mFile;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IFileWrapper) {
            return mFile.equals(((IFileWrapper)obj).mFile);
        }
        
        if (obj instanceof IFile) {
            return mFile.equals(obj);
        }

        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return mFile.hashCode();
    }
}
