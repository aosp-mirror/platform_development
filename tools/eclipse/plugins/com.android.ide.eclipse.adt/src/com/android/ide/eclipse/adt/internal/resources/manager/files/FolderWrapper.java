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

import org.eclipse.core.resources.IFolder;

import java.io.File;
import java.io.IOException;

/**
 * An implementation of {@link IAbstractFolder} on top of a {@link File} object.
 */
public class FolderWrapper implements IAbstractFolder {

    private File mFolder;

    /**
     * Constructs a {@link FileWrapper} object. If {@link File#isDirectory()} returns
     * <code>false</code> then an {@link IOException} is thrown. 
     */
    public FolderWrapper(File folder) throws IOException {
        if (folder.isDirectory() == false) {
            throw new IOException("FileWrapper must wrap a File object representing an existing folder!"); //$NON-NLS-1$
        }
        
        mFolder = folder;
    }
    
    public boolean hasFile(String name) {
        return false;
    }

    public String getName() {
        return mFolder.getName();
    }

    public IFolder getIFolder() {
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FolderWrapper) {
            return mFolder.equals(((FolderWrapper)obj).mFolder);
        }
        
        if (obj instanceof File) {
            return mFolder.equals(obj);
        }

        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return mFolder.hashCode();
    }

}
