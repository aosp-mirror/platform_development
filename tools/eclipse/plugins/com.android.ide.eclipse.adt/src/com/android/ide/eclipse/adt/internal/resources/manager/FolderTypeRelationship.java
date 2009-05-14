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

import com.android.ide.eclipse.adt.internal.resources.ResourceType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * This class gives access to the bi directional relationship between {@link ResourceType} and
 * {@link ResourceFolderType}.
 */
public final class FolderTypeRelationship {
    
    private final static HashMap<ResourceType, ResourceFolderType[]> mTypeToFolderMap =
        new HashMap<ResourceType, ResourceFolderType[]>();
        
    private final static HashMap<ResourceFolderType, ResourceType[]> mFolderToTypeMap =
        new HashMap<ResourceFolderType, ResourceType[]>();
    
    // generate the relationships.
    static {
        HashMap<ResourceType, List<ResourceFolderType>> typeToFolderMap =
            new HashMap<ResourceType, List<ResourceFolderType>>();
        
        HashMap<ResourceFolderType, List<ResourceType>> folderToTypeMap =
            new HashMap<ResourceFolderType, List<ResourceType>>();

        add(ResourceType.ANIM, ResourceFolderType.ANIM, typeToFolderMap, folderToTypeMap);
        add(ResourceType.ARRAY, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.COLOR, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.COLOR, ResourceFolderType.COLOR, typeToFolderMap, folderToTypeMap);
        add(ResourceType.DIMEN, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.DRAWABLE, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.DRAWABLE, ResourceFolderType.DRAWABLE, typeToFolderMap, folderToTypeMap);
        add(ResourceType.ID, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.LAYOUT, ResourceFolderType.LAYOUT, typeToFolderMap, folderToTypeMap);
        add(ResourceType.MENU, ResourceFolderType.MENU, typeToFolderMap, folderToTypeMap);
        add(ResourceType.RAW, ResourceFolderType.RAW, typeToFolderMap, folderToTypeMap);
        add(ResourceType.STRING, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.STYLE, ResourceFolderType.VALUES, typeToFolderMap, folderToTypeMap);
        add(ResourceType.XML, ResourceFolderType.XML, typeToFolderMap, folderToTypeMap);
        
        optimize(typeToFolderMap, folderToTypeMap);
    }
    
    /**
     * Returns a list of {@link ResourceType}s that can be generated from files inside a folder
     * of the specified type.
     * @param folderType The folder type.
     * @return an array of {@link ResourceType}
     */
    public static ResourceType[] getRelatedResourceTypes(ResourceFolderType folderType) {
        ResourceType[] array = mFolderToTypeMap.get(folderType);
        if (array != null) {
            return array;
        }
        return new ResourceType[0];
    }
    
    /**
     * Returns a list of {@link ResourceFolderType} that can contain files generating resources
     * of the specified type.
     * @param resType the type of resource.
     * @return an array of {@link ResourceFolderType}
     */
    public static ResourceFolderType[] getRelatedFolders(ResourceType resType) {
        ResourceFolderType[] array = mTypeToFolderMap.get(resType);
        if (array != null) {
            return array;
        }
        return new ResourceFolderType[0];
    }
    
    /**
     * Returns true if the {@link ResourceType} and the {@link ResourceFolderType} values match.
     * @param resType the resource type.
     * @param folderType the folder type.
     * @return true if files inside the folder of the specified {@link ResourceFolderType}
     * could generate a resource of the specified {@link ResourceType}
     */
    public static boolean match(ResourceType resType, ResourceFolderType folderType) {
        ResourceFolderType[] array = mTypeToFolderMap.get(resType);
        
        if (array != null && array.length > 0) {
            for (ResourceFolderType fType : array) {
                if (fType == folderType) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Adds a {@link ResourceType} - {@link ResourceFolderType} relationship. this indicates that
     * a file in the folder can generate a resource of the specified type.
     * @param type The resourceType
     * @param folder The {@link ResourceFolderType}
     * @param folderToTypeMap 
     * @param typeToFolderMap 
     */
    private static void add(ResourceType type, ResourceFolderType folder,
            HashMap<ResourceType, List<ResourceFolderType>> typeToFolderMap,
            HashMap<ResourceFolderType, List<ResourceType>> folderToTypeMap) {
        // first we add the folder to the list associated with the type.
        List<ResourceFolderType> folderList = typeToFolderMap.get(type);
        if (folderList == null) {
            folderList = new ArrayList<ResourceFolderType>();
            typeToFolderMap.put(type, folderList);
        }
        if (folderList.indexOf(folder) == -1) {
            folderList.add(folder);
        }
        
        // now we add the type to the list associated with the folder.
        List<ResourceType> typeList = folderToTypeMap.get(folder);
        if (typeList == null) {
            typeList = new ArrayList<ResourceType>();
            folderToTypeMap.put(folder, typeList);
        }
        if (typeList.indexOf(type) == -1) {
            typeList.add(type);
        }
    }

    /**
     * Optimize the map to contains array instead of lists (since the api returns arrays)
     * @param typeToFolderMap
     * @param folderToTypeMap
     */
    private static void optimize(HashMap<ResourceType, List<ResourceFolderType>> typeToFolderMap,
            HashMap<ResourceFolderType, List<ResourceType>> folderToTypeMap) {
        Set<ResourceType> types = typeToFolderMap.keySet();
        for (ResourceType type : types) {
            List<ResourceFolderType> list = typeToFolderMap.get(type);
            mTypeToFolderMap.put(type, list.toArray(new ResourceFolderType[list.size()]));
        }

        Set<ResourceFolderType> folders = folderToTypeMap.keySet();
        for (ResourceFolderType folder : folders) {
            List<ResourceType> list = folderToTypeMap.get(folder);
            mFolderToTypeMap.put(folder, list.toArray(new ResourceType[list.size()]));
        }
    }
}
