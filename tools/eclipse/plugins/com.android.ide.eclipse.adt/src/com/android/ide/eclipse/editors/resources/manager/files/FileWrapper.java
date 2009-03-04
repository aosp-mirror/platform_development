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

package com.android.ide.eclipse.editors.resources.manager.files;

import org.eclipse.core.resources.IFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * An implementation of {@link IAbstractFile} on top of a {@link File} object.
 *
 */
public class FileWrapper implements IAbstractFile {
    
    private File mFile;

    /**
     * Constructs a {@link FileWrapper} object. If {@link File#isFile()} returns <code>false</code>
     * then an {@link IOException} is thrown. 
     */
    public FileWrapper(File file) throws IOException {
        if (file.isFile() == false) {
            throw new IOException("FileWrapper must wrap a File object representing an existing file!"); //$NON-NLS-1$
        }
        
        mFile = file;
    }

    public InputStream getContents() {
        try {
            return new FileInputStream(mFile);
        } catch (FileNotFoundException e) {
            // we'll return null below.
        }
        
        return null;
    }

    public IFile getIFile() {
        return null;
    }

    public String getOsLocation() {
        return mFile.getAbsolutePath();
    }

    public String getName() {
        return mFile.getName();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FileWrapper) {
            return mFile.equals(((FileWrapper)obj).mFile);
        }
        
        if (obj instanceof File) {
            return mFile.equals(obj);
        }

        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return mFile.hashCode();
    }
}
