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

import com.android.ide.eclipse.adt.internal.resources.IResourceRepository;
import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.LanguageQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.RegionQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IAbstractFolder;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.utils.ResourceValue;

import org.eclipse.core.resources.IFolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the resources of a project. This is a file view of the resources, with handling
 * for the alternate resource types. For a compiled view use CompiledResources.
 */
public class ProjectResources implements IResourceRepository {
    private final HashMap<ResourceFolderType, List<ResourceFolder>> mFolderMap =
        new HashMap<ResourceFolderType, List<ResourceFolder>>();
    
    private final HashMap<ResourceType, List<ProjectResourceItem>> mResourceMap =
        new HashMap<ResourceType, List<ProjectResourceItem>>();
    
    /** Map of (name, id) for resources of type {@link ResourceType#ID} coming from R.java */
    private Map<String, Map<String, Integer>> mResourceValueMap;
    /** Map of (id, [name, resType]) for all resources coming from R.java */
    private Map<Integer, String[]> mResIdValueToNameMap;
    /** Map of (int[], name) for styleable resources coming from R.java */
    private Map<IntArrayWrapper, String> mStyleableValueToNameMap;
    
    /** Cached list of {@link IdResourceItem}. This is mix of IdResourceItem created by
     * {@link MultiResourceFile} for ids coming from XML files under res/values and
     * {@link IdResourceItem} created manually, from the list coming from R.java */
    private final ArrayList<IdResourceItem> mIdResourceList = new ArrayList<IdResourceItem>();

    private final boolean mIsFrameworkRepository;
    
    private final IntArrayWrapper mWrapper = new IntArrayWrapper(null);

    public ProjectResources(boolean isFrameworkRepository) {
        mIsFrameworkRepository = isFrameworkRepository;
    }
    
    public boolean isSystemRepository() {
        return mIsFrameworkRepository;
    }

    /**
     * Adds a Folder Configuration to the project.
     * @param type The resource type.
     * @param config The resource configuration.
     * @param folder The workspace folder object.
     * @return the {@link ResourceFolder} object associated to this folder.
     */
    protected ResourceFolder add(ResourceFolderType type, FolderConfiguration config,
            IAbstractFolder folder) {
        // get the list for the resource type
        List<ResourceFolder> list = mFolderMap.get(type);
        
        if (list == null) {
            list = new ArrayList<ResourceFolder>();

            ResourceFolder cf = new ResourceFolder(type, config, folder, mIsFrameworkRepository);
            list.add(cf);

            mFolderMap.put(type, list);
            
            return cf;
        }

        // look for an already existing folder configuration.
        for (ResourceFolder cFolder : list) {
            if (cFolder.mConfiguration.equals(config)) {
                // config already exist. Nothing to be done really, besides making sure
                // the IFolder object is up to date.
                cFolder.mFolder = folder;
                return cFolder;
            }
        }

        // If we arrive here, this means we didn't find a matching configuration.
        // So we add one.
        ResourceFolder cf = new ResourceFolder(type, config, folder, mIsFrameworkRepository);
        list.add(cf);
        
        return cf;
    }

    /**
     * Removes a {@link ResourceFolder} associated with the specified {@link IAbstractFolder}.
     * @param type The type of the folder
     * @param folder the IFolder object.
     */
    protected void removeFolder(ResourceFolderType type, IFolder folder) {
        // get the list of folders for the resource type.
        List<ResourceFolder> list = mFolderMap.get(type);
        
        if (list != null) {
            int count = list.size();
            for (int i = 0 ; i < count ; i++) {
                ResourceFolder resFolder = list.get(i);
                if (resFolder.getFolder().getIFolder().equals(folder)) {
                    // we found the matching ResourceFolder. we need to remove it.
                    list.remove(i);
                    
                    // we now need to invalidate this resource type.
                    // The easiest way is to touch one of the other folders of the same type.
                    if (list.size() > 0) {
                        list.get(0).touch();
                    } else {
                        // if the list is now empty, and we have a single ResouceType out of this
                        // ResourceFolderType, then we are done.
                        // However, if another ResourceFolderType can generate similar ResourceType
                        // than this, we need to update those ResourceTypes as well.
                        // For instance, if the last "drawable-*" folder is deleted, we need to
                        // refresh the ResourceItem associated with ResourceType.DRAWABLE.
                        // Those can be found in ResourceFolderType.DRAWABLE but also in
                        // ResourceFolderType.VALUES.
                        // If we don't find a single folder to touch, then it's fine, as the top
                        // level items (the list of generated resource types) is not cached
                        // (for now)
                        
                        // get the lists of ResourceTypes generated by this ResourceFolderType
                        ResourceType[] resTypes = FolderTypeRelationship.getRelatedResourceTypes(
                                type);
                        
                        // for each of those, make sure to find one folder to touch so that the
                        // list of ResourceItem associated with the type is rebuilt.
                        for (ResourceType resType : resTypes) {
                            // get the list of folder that can generate this type
                            ResourceFolderType[] folderTypes =
                                FolderTypeRelationship.getRelatedFolders(resType);
                            
                            // we only need to touch one folder in any of those (since it's one
                            // folder per type, not per folder type).
                            for (ResourceFolderType folderType : folderTypes) {
                                List<ResourceFolder> resFolders = mFolderMap.get(folderType);
                                
                                if (resFolders != null && resFolders.size() > 0) {
                                    resFolders.get(0).touch();
                                    break;
                                }
                            }
                        }
                    }
                    
                    // we're done updating/touching, we can stop
                    break;
                }
            }
        }
    }

    
    /**
     * Returns a list of {@link ResourceFolder} for a specific {@link ResourceFolderType}.
     * @param type The {@link ResourceFolderType}
     */
    public List<ResourceFolder> getFolders(ResourceFolderType type) {
        return mFolderMap.get(type);
    }
    
    /* (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.IResourceRepository#getAvailableResourceTypes()
     */
    public ResourceType[] getAvailableResourceTypes() {
        ArrayList<ResourceType> list = new ArrayList<ResourceType>();
        
        // For each key, we check if there's a single ResourceType match.
        // If not, we look for the actual content to give us the resource type.

        for (ResourceFolderType folderType : mFolderMap.keySet()) {
            ResourceType types[] = FolderTypeRelationship.getRelatedResourceTypes(folderType);
            if (types.length == 1) {
                // before we add it we check if it's not already present, since a ResourceType
                // could be created from multiple folders, even for the folders that only create
                // one type of resource (drawable for instance, can be created from drawable/ and
                // values/)
                if (list.indexOf(types[0]) == -1) {
                    list.add(types[0]);
                }
            } else {
                // there isn't a single resource type out of this folder, so we look for all
                // content.
                List<ResourceFolder> folders = mFolderMap.get(folderType);
                if (folders != null) {
                    for (ResourceFolder folder : folders) {
                        Collection<ResourceType> folderContent = folder.getResourceTypes();
                        
                        // then we add them, but only if they aren't already in the list.
                        for (ResourceType folderResType : folderContent) {
                            if (list.indexOf(folderResType) == -1) {
                                list.add(folderResType);
                            }
                        }
                    }
                }
            }
        }
        
        // in case ResourceType.ID haven't been added yet because there's no id defined
        // in XML, we check on the list of compiled id resources.
        if (list.indexOf(ResourceType.ID) == -1 && mResourceValueMap != null) {
            Map<String, Integer> map = mResourceValueMap.get(ResourceType.ID.getName());
            if (map != null && map.size() > 0) {
                list.add(ResourceType.ID);
            }
        }

        // at this point the list is full of ResourceType defined in the files.
        // We need to sort it.
        Collections.sort(list);
        
        return list.toArray(new ResourceType[list.size()]);
    }

    /* (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.IResourceRepository#getResources(com.android.ide.eclipse.common.resources.ResourceType)
     */
    public ProjectResourceItem[] getResources(ResourceType type) {
        checkAndUpdate(type);
        
        if (type == ResourceType.ID) {
            synchronized (mIdResourceList) {
                return mIdResourceList.toArray(new ProjectResourceItem[mIdResourceList.size()]);
            }
        }
        
        List<ProjectResourceItem> items = mResourceMap.get(type);
        
        return items.toArray(new ProjectResourceItem[items.size()]);
    }

    /* (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.IResourceRepository#hasResources(com.android.ide.eclipse.common.resources.ResourceType)
     */
    public boolean hasResources(ResourceType type) {
        checkAndUpdate(type);

        if (type == ResourceType.ID) {
            synchronized (mIdResourceList) {
                return mIdResourceList.size() > 0;
            }
        }

        List<ProjectResourceItem> items = mResourceMap.get(type);
        return (items != null && items.size() > 0);
    }

    /**
     * Returns the {@link ResourceFolder} associated with a {@link IFolder}.
     * @param folder The {@link IFolder} object.
     * @return the {@link ResourceFolder} or null if it was not found.
     */
    public ResourceFolder getResourceFolder(IFolder folder) {
        for (List<ResourceFolder> list : mFolderMap.values()) {
            for (ResourceFolder resFolder : list) {
                if (resFolder.getFolder().getIFolder().equals(folder)) {
                    return resFolder;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Returns the {@link ResourceFile} matching the given name, {@link ResourceFolderType} and
     * configuration.
     * <p/>This only works with files generating one resource named after the file (for instance,
     * layouts, bitmap based drawable, xml, anims).
     * @return the matching file or <code>null</code> if no match was found.
     */
    public ResourceFile getMatchingFile(String name, ResourceFolderType type,
            FolderConfiguration config) {
        // get the folders for the given type
        List<ResourceFolder> folders = mFolderMap.get(type);

        // look for folders containing a file with the given name.
        ArrayList<ResourceFolder> matchingFolders = new ArrayList<ResourceFolder>();
        
        // remove the folders that do not have a file with the given name, or if their config
        // is incompatible.
        for (int i = 0 ; i < folders.size(); i++) {
            ResourceFolder folder = folders.get(i);
            
            if (folder.hasFile(name) == true) {
                matchingFolders.add(folder);
            }
        }
        
        // from those, get the folder with a config matching the given reference configuration.
        Resource match = findMatchingConfiguredResource(matchingFolders, config);
        
        // do we have a matching folder?
        if (match instanceof ResourceFolder) {
            // get the ResourceFile from the filename
            return ((ResourceFolder)match).getFile(name);
        }
        
        return null;
    }
    
    /**
     * Returns the resources values matching a given {@link FolderConfiguration}.
     * @param referenceConfig the configuration that each value must match.
     */
    public Map<String, Map<String, IResourceValue>> getConfiguredResources(
            FolderConfiguration referenceConfig) {

        Map<String, Map<String, IResourceValue>> map =
            new HashMap<String, Map<String, IResourceValue>>();
        
        // special case for Id since there's a mix of compiled id (declared inline) and id declared
        // in the XML files.
        if (mIdResourceList.size() > 0) {
            Map<String, IResourceValue> idMap = new HashMap<String, IResourceValue>();
            String idType = ResourceType.ID.getName();
            for (IdResourceItem id : mIdResourceList) {
                // FIXME: cache the ResourceValue!
                idMap.put(id.getName(), new ResourceValue(idType, id.getName(),
                        mIsFrameworkRepository));
            }
            
            map.put(ResourceType.ID.getName(), idMap);
        }
        
        Set<ResourceType> keys = mResourceMap.keySet();
        for (ResourceType key : keys) {
            // we don't process ID resources since we already did it above.
            if (key != ResourceType.ID) {
                map.put(key.getName(), getConfiguredResource(key, referenceConfig));
            }
        }
        
        return map;
    }
    
    /**
     * Loads all the resources. Essentially this forces to load the values from the
     * {@link ResourceFile} objects to make sure they are up to date and loaded
     * in {@link #mResourceMap}.
     */
    public void loadAll() {
        // gets all the resource types available.
        ResourceType[] types = getAvailableResourceTypes();
        
        // loop on them and load them
        for (ResourceType type: types) {
            checkAndUpdate(type);
        }
    }
    
    /**
     * Resolves a compiled resource id into the resource name and type
     * @param id
     * @return an array of 2 strings { name, type } or null if the id could not be resolved
     */
    public String[] resolveResourceValue(int id) {
        if (mResIdValueToNameMap != null) {
            return mResIdValueToNameMap.get(id);
        }
        
        return null;
    }

    /**
     * Resolves a compiled resource id of type int[] into the resource name.
     */
    public String resolveResourceValue(int[] id) {
        if (mStyleableValueToNameMap != null) {
            mWrapper.set(id);
            return mStyleableValueToNameMap.get(mWrapper);
        }
        
        return null;
    }

    /**
     * Returns the value of a resource by its type and name.
     */
    public Integer getResourceValue(String type, String name) {
        if (mResourceValueMap != null) {
            Map<String, Integer> map = mResourceValueMap.get(type);
            if (map != null) {
                return map.get(name);
            }
        }

        return null;
    }
    
    /**
     * Returns the list of languages used in the resources.
     */
    public Set<String> getLanguages() {
        Set<String> set = new HashSet<String>();

        Collection<List<ResourceFolder>> folderList = mFolderMap.values();
        for (List<ResourceFolder> folderSubList : folderList) {
            for (ResourceFolder folder : folderSubList) {
                FolderConfiguration config = folder.getConfiguration();
                LanguageQualifier lang = config.getLanguageQualifier();
                if (lang != null) {
                    set.add(lang.getStringValue());
                }
            }
        }
        
        return set;
    }
    
    /**
     * Returns the list of regions used in the resources with the given language.
     * @param currentLanguage the current language the region must be associated with.
     */
    public Set<String> getRegions(String currentLanguage) {
        Set<String> set = new HashSet<String>();

        Collection<List<ResourceFolder>> folderList = mFolderMap.values();
        for (List<ResourceFolder> folderSubList : folderList) {
            for (ResourceFolder folder : folderSubList) {
                FolderConfiguration config = folder.getConfiguration();
                
                // get the language
                LanguageQualifier lang = config.getLanguageQualifier();
                if (lang != null && lang.getStringValue().equals(currentLanguage)) {
                    RegionQualifier region = config.getRegionQualifier();
                    if (region != null) {
                        set.add(region.getStringValue());
                    }
                }
            }
        }
        
        return set;
    }

    /**
     * Returns a map of (resource name, resource value) for the given {@link ResourceType}.
     * <p/>The values returned are taken from the resource files best matching a given
     * {@link FolderConfiguration}.
     * @param type the type of the resources.
     * @param referenceConfig the configuration to best match.
     */
    private Map<String, IResourceValue> getConfiguredResource(ResourceType type,
            FolderConfiguration referenceConfig) {
        // get the resource item for the given type
        List<ProjectResourceItem> items = mResourceMap.get(type);
        
        // create the map
        HashMap<String, IResourceValue> map = new HashMap<String, IResourceValue>();
        
        for (ProjectResourceItem item : items) {
            // get the source files generating this resource
            List<ResourceFile> list = item.getSourceFileList();
            
            // look for the best match for the given configuration
            Resource match = findMatchingConfiguredResource(list, referenceConfig);
            
            if (match instanceof ResourceFile) {
                ResourceFile matchResFile = (ResourceFile)match;
                
                // get the value of this configured resource.
                IResourceValue value = matchResFile.getValue(type, item.getName());
                
                if (value != null) {
                    map.put(item.getName(), value);
                }
            }
        }

        return map;
    }

    /**
     * Returns the best matching {@link Resource}. 
     * @param resources the list of {@link Resource} to choose from.
     * @param referenceConfig the {@link FolderConfiguration} to match.
     */
    private Resource findMatchingConfiguredResource(List<? extends Resource> resources,
            FolderConfiguration referenceConfig) {
        // look for resources with the maximum number of qualifier match.
        int currentMax = -1;
        ArrayList<Resource> matchingResources = new ArrayList<Resource>();
        for (int i = 0 ; i < resources.size(); i++) {
            Resource res = resources.get(i);
            
            int count = res.getConfiguration().match(referenceConfig);
            if (count > currentMax) {
                matchingResources.clear();
                matchingResources.add(res);
                currentMax = count;
            } else if (count != -1 && count == currentMax) {
                matchingResources.add(res);
            }
        }
        
        // if we have more than one match, we look for the match with the qualifiers with the
        // highest priority.
        Resource resMatch = null;
        if (matchingResources.size() == 1) {
            resMatch = matchingResources.get(0);
        } else if (matchingResources.size() > 1) {
            // More than one resource with the same number of qualifier match.
            // We loop, looking for the resource with the highest priority qualifiers.
            ArrayList<Resource> tmpResources = new ArrayList<Resource>();
            int startIndex = 0;
            while (matchingResources.size() > 1) {
                int highest = -1;
                for (int i = 0 ; i < matchingResources.size() ; i++) {
                    Resource folder = matchingResources.get(i);
                 
                    // get highest priority qualifiers.
                    int m = folder.getConfiguration().getHighestPriorityQualifier(startIndex);

                    // add to the list if highest.
                    if (m != -1) {
                        if (highest == -1 || m == highest) {
                            tmpResources.add(folder);
                            highest = m;
                        } else if (m < highest) { // highest priority == lowest index.
                            tmpResources.clear();
                            tmpResources.add(folder);
                        }
                    }
                }
                
                // at this point, we have a list with 1+ resources that all have the same highest
                // priority qualifiers. Go through the list again looking for the next highest
                // priority qualifier.
                startIndex = highest + 1;
                
                // this should not happen, but it's better to check.
                if (matchingResources.size() == tmpResources.size() && highest == -1) {
                    // this means all the resources match with the same qualifiers
                    // (highest == -1 means we reached the end of the qualifier list)
                    // In this case, we arbitrarily take the first resource.
                    matchingResources.clear();
                    matchingResources.add(tmpResources.get(0));
                } else {
                    matchingResources.clear();
                    matchingResources.addAll(tmpResources);
                }
                tmpResources.clear();
            }
            
            // we should have only one match here.
            resMatch = matchingResources.get(0);
        }

        return resMatch;
    }

    /**
     * Checks if the list of {@link ResourceItem}s for the specified {@link ResourceType} needs
     * to be updated. 
     * @param type the Resource Type.
     */
    private void checkAndUpdate(ResourceType type) {
        // get the list of folder that can output this type
        ResourceFolderType[] folderTypes = FolderTypeRelationship.getRelatedFolders(type);

        for (ResourceFolderType folderType : folderTypes) {
            List<ResourceFolder> folders = mFolderMap.get(folderType);
            
            if (folders != null) {
                for (ResourceFolder folder : folders) {
                    if (folder.isTouched()) {
                        // if this folder is touched we need to update all the types that can
                        // be generated from a file in this folder.
                        // This will include 'type' obviously.
                        ResourceType[] resTypes = FolderTypeRelationship.getRelatedResourceTypes(
                                folderType);
                        for (ResourceType resType : resTypes) {
                            update(resType);
                        }
                        return;
                    }
                }
            }
        }
    }

    /**
     * Updates the list of {@link ResourceItem} objects associated with a {@link ResourceType}.
     * This will reset the touch status of all the folders that can generate this resource type.
     * @param type the Resource Type.
     */
    private void update(ResourceType type) {
        // get the cache list, and lets make a backup
        List<ProjectResourceItem> items = mResourceMap.get(type);
        List<ProjectResourceItem> backup = new ArrayList<ProjectResourceItem>();
        
        if (items == null) {
            items = new ArrayList<ProjectResourceItem>();
            mResourceMap.put(type, items);
        } else {
            // backup the list
            backup.addAll(items);

            // we reset the list itself.
            items.clear();
        }
        
        // get the list of folder that can output this type
        ResourceFolderType[] folderTypes = FolderTypeRelationship.getRelatedFolders(type);

        for (ResourceFolderType folderType : folderTypes) {
            List<ResourceFolder> folders = mFolderMap.get(folderType);

            if (folders != null) {
                for (ResourceFolder folder : folders) {
                    items.addAll(folder.getResources(type, this));
                    folder.resetTouch();
                }
            }
        }

        // now items contains the new list. We "merge" it with the backup list.
        // Basically, we need to keep the old instances of ResourceItem (where applicable),
        // but replace them by the content of the new items.
        // This will let the resource explorer keep the expanded state of the nodes whose data
        // is a ResourceItem object.
        if (backup.size() > 0) {
            // this is not going to change as we're only replacing instances.
            int count = items.size();

            for (int i = 0 ; i < count;) {
                // get the "new" item
                ProjectResourceItem item = items.get(i);
                
                // look for a similar item in the old list.
                ProjectResourceItem foundOldItem = null;
                for (ProjectResourceItem oldItem : backup) {
                    if (oldItem.getName().equals(item.getName())) {
                        foundOldItem = oldItem;
                        break;
                    }
                }
                
                if (foundOldItem != null) {
                    // erase the data of the old item with the data from the new one.
                    foundOldItem.replaceWith(item);
                    
                    // remove the old and new item from their respective lists
                    items.remove(i);
                    backup.remove(foundOldItem);
                    
                    // add the old item to the new list
                    items.add(foundOldItem);
                } else {
                    // this is a new item, we skip to the next object
                    i++;
                }
            }
        }
        
        // if this is the ResourceType.ID, we create the actual list, from this list and
        // the compiled resource list.
        if (type == ResourceType.ID) {
            mergeIdResources();
        } else {
            // else this is the list that will actually be displayed, so we sort it.
            Collections.sort(items);
        }
    }

    /**
     * Looks up an existing {@link ProjectResourceItem} by {@link ResourceType} and name. 
     * @param type the Resource Type.
     * @param name the Resource name.
     * @return the existing ResourceItem or null if no match was found.
     */
    protected ProjectResourceItem findResourceItem(ResourceType type, String name) {
        List<ProjectResourceItem> list = mResourceMap.get(type);
        
        for (ProjectResourceItem item : list) {
            if (name.equals(item.getName())) {
                return item;
            }
        }
        
        return null;
    }

    /**
     * Sets compiled resource information.
     * @param resIdValueToNameMap a map of compiled resource id to resource name.
     *  The map is acquired by the {@link ProjectResources} object.
     * @param styleableValueMap
     * @param resourceValueMap a map of (name, id) for resources of type {@link ResourceType#ID}.
     * The list is acquired by the {@link ProjectResources} object.
     */
    void setCompiledResources(Map<Integer, String[]> resIdValueToNameMap,
            Map<IntArrayWrapper, String> styleableValueMap,
            Map<String, Map<String, Integer>> resourceValueMap) {
        mResourceValueMap = resourceValueMap;
        mResIdValueToNameMap = resIdValueToNameMap;
        mStyleableValueToNameMap = styleableValueMap;
        mergeIdResources();
    }

    /**
     * Merges the list of ID resource coming from R.java and the list of ID resources
     * coming from XML declaration into the cached list {@link #mIdResourceList}.
     */
    void mergeIdResources() {
        // get the list of IDs coming from XML declaration. Those ids are present in
        // mCompiledIdResources already, so we'll need to use those instead of creating
        // new IdResourceItem
        List<ProjectResourceItem> xmlIdResources = mResourceMap.get(ResourceType.ID);

        synchronized (mIdResourceList) {
            // copy the currently cached items.
            ArrayList<IdResourceItem> oldItems = new ArrayList<IdResourceItem>();
            oldItems.addAll(mIdResourceList);

            // empty the current list
            mIdResourceList.clear();
            
            // get the list of compile id resources.
            Map<String, Integer> idMap = null;
            if (mResourceValueMap != null) {
                idMap = mResourceValueMap.get(ResourceType.ID.getName());
            }
            
            if (idMap == null) {
                if (xmlIdResources != null) {
                    for (ProjectResourceItem resourceItem : xmlIdResources) {
                        // check the actual class just for safety.
                        if (resourceItem instanceof IdResourceItem) {
                            mIdResourceList.add((IdResourceItem)resourceItem);
                        }
                    }
                }
            } else {
                // loop on the full list of id, and look for a match in the old list,
                // in the list coming from XML (in case a new XML item was created.)
                
                Set<String> idSet = idMap.keySet();
                
                idLoop: for (String idResource : idSet) {
                    // first look in the XML list in case an id went from inline to XML declared.
                    if (xmlIdResources != null) {
                        for (ProjectResourceItem resourceItem : xmlIdResources) {
                            if (resourceItem instanceof IdResourceItem && 
                                    resourceItem.getName().equals(idResource)) {
                                mIdResourceList.add((IdResourceItem)resourceItem);
                                continue idLoop;
                            }
                        }
                    }
                    
                    // if we haven't found it, look in the old items.
                    int count = oldItems.size();
                    for (int i = 0 ; i < count ; i++) {
                        IdResourceItem resourceItem = oldItems.get(i);
                        if (resourceItem.getName().equals(idResource)) {
                            oldItems.remove(i);
                            mIdResourceList.add(resourceItem);
                            continue idLoop;
                        }
                    }
                    
                    // if we haven't found it, it looks like it's a new id that was
                    // declared inline.
                    mIdResourceList.add(new IdResourceItem(idResource,
                            true /* isDeclaredInline */));
                }
            }
            
            // now we sort the list
            Collections.sort(mIdResourceList);
        }
    }
}
