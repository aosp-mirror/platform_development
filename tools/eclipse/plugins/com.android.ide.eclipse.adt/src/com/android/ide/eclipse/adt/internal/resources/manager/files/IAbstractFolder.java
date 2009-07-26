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

/**
 *  A folder.
 */
public interface IAbstractFolder extends IAbstractResource {

    /**
     * Returns true if the receiver contains a file with a given name 
     * @param name the name of the file. This is the name without the path leading to the
     * parent folder.
     */
    boolean hasFile(String name);

    /**
     * Returns the {@link IFolder} object that the receiver could represent.
     * Can be <code>null</code>
     */
    IFolder getIFolder();
}
