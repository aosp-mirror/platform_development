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

package com.android.ide.eclipse.adt.internal.resources;

/**
 * Base class representing a Resource Item, as returned by a {@link IResourceRepository}.
 */
public class ResourceItem implements Comparable<ResourceItem> {
    
    private final String mName;
    
    /**
     * Constructs a new ResourceItem
     * @param name the name of the resource as it appears in the XML and R.java files.
     */
    public ResourceItem(String name) {
        mName = name;
    }

    /**
     * Returns the name of the resource item.
     */
    public final String getName() {
        return mName;
    }

    /**
     * Compares the {@link ResourceItem} to another.
     * @param other the ResourceItem to be compared to.
     */
    public int compareTo(ResourceItem other) {
        return mName.compareTo(other.mName);
    }
}
