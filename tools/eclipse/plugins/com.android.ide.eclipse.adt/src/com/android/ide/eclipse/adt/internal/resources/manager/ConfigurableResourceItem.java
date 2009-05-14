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

/**
 * Represents a resource item that can exist in multiple "alternate" versions. 
 */
public class ConfigurableResourceItem extends ProjectResourceItem {
    
    /**
     * Constructs a new Resource Item.
     * @param name the name of the resource as it appears in the XML and R.java files.
     */
    public ConfigurableResourceItem(String name) {
        super(name);
    }

    /**
     * Returns if the resource item has at least one non-default configuration.
     */
    public boolean hasAlternates() {
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault() == false) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Returns whether the resource has a default version, with no qualifier.
     */
    public boolean hasDefault() {
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault()) {
                return true;
            }
        }
        
        // We only want to return false if there's no default and more than 0 items.
        return (mFiles.size() == 0);
    }

    /**
     * Returns the number of alternate versions of this resource.
     */
    public int getAlternateCount() {
        int count = 0;
        for (ResourceFile file : mFiles) {
            if (file.getFolder().getConfiguration().isDefault() == false) {
                count++;
            }
        }

        return count;
    }

    /*
     * (non-Javadoc)
     * Returns whether the item can be edited directly (ie it does not have alternate versions).
     */
    @Override
    public boolean isEditableDirectly() {
        return hasAlternates() == false;
    }

}
