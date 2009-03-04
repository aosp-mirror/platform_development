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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * An implementation of {@link IAbstractFolder} on top of an {@link IFolder} object.
 */
public class IFolderWrapper implements IAbstractFolder {
    
    private IFolder mFolder;

    public IFolderWrapper(IFolder folder) {
        mFolder = folder;
    }

    public String getName() {
        return mFolder.getName();
    }

    public boolean hasFile(String name) {
        try {
            IResource[] files = mFolder.members();
            for (IResource file : files) {
                if (name.equals(file.getName())) {
                    return true;
                }
            }
        } catch (CoreException e) {
            // we'll return false below.
        }

        return false;
    }
    
    public IFolder getIFolder() {
        return mFolder;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof IFolderWrapper) {
            return mFolder.equals(((IFolderWrapper)obj).mFolder);
        }
        
        if (obj instanceof IFolder) {
            return mFolder.equals(obj);
        }

        return super.equals(obj);
    }
    
    @Override
    public int hashCode() {
        return mFolder.hashCode();
    }
}
