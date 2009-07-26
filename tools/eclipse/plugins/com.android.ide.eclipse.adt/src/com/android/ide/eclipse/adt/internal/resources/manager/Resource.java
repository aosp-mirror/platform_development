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

package com.android.ide.eclipse.adt.internal.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;

/**
 * Base class for file system resource items (Folders, Files).
 */
public abstract class Resource {
    private boolean mTouched = true;
    
    /**
     * Returns the {@link FolderConfiguration} for this object.
     */
    public abstract FolderConfiguration getConfiguration();

    /**
     * Indicates that the underlying file was changed.
     */
    public final void touch() {
       mTouched = true; 
    }
    
    public final boolean isTouched() {
        return mTouched;
    }
    
    public final void resetTouch() {
        mTouched = false;
    }
}
