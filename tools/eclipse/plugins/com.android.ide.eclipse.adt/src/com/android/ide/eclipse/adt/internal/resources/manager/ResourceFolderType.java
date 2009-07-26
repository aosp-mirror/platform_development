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
import com.android.sdklib.SdkConstants;

/**
 * Enum representing a type of resource folder.
 */
public enum ResourceFolderType {
    ANIM(SdkConstants.FD_ANIM),
    COLOR(SdkConstants.FD_COLOR),
    DRAWABLE(SdkConstants.FD_DRAWABLE),
    LAYOUT(SdkConstants.FD_LAYOUT),
    MENU(SdkConstants.FD_MENU),
    RAW(SdkConstants.FD_RAW),
    VALUES(SdkConstants.FD_VALUES),
    XML(SdkConstants.FD_XML);

    private final String mName;

    ResourceFolderType(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }
    
    /**
     * Returns the enum by name.
     * @param name The enum string value.
     * @return the enum or null if not found.
     */
    public static ResourceFolderType getTypeByName(String name) {
        for (ResourceFolderType rType : values()) {
            if (rType.mName.equals(name)) {
                return rType;
            }
        }
        return null;
    }
    
    /**
     * Returns the {@link ResourceFolderType} from the folder name
     * @param folderName The name of the folder. This must be a valid folder name in the format
     * <code>resType[-resqualifiers[-resqualifiers[...]]</code>
     * @return the <code>ResourceFolderType</code> representing the type of the folder, or
     * <code>null</code> if no matching type was found.
     */
    public static ResourceFolderType getFolderType(String folderName) {
        // split the name of the folder in segments.
        String[] folderSegments = folderName.split(FolderConfiguration.QUALIFIER_SEP);

        // get the enum for the resource type.
        return getTypeByName(folderSegments[0]);
    }
}
