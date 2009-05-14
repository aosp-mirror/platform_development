package com.android.ide.eclipse.adt.internal.resources.manager;

import com.android.ide.eclipse.adt.internal.resources.ResourceItem;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for Resource Item coming from an Android Project.
 */
public abstract class ProjectResourceItem extends ResourceItem {

    private final static Comparator<ResourceFile> sComparator = new Comparator<ResourceFile>() {
        public int compare(ResourceFile file1, ResourceFile file2) {
            // get both FolderConfiguration and compare them
            FolderConfiguration fc1 = file1.getFolder().getConfiguration();
            FolderConfiguration fc2 = file2.getFolder().getConfiguration();
            
            return fc1.compareTo(fc2);
        }
    };

    /**
     * List of files generating this ResourceItem.
     */
    protected final ArrayList<ResourceFile> mFiles = new ArrayList<ResourceFile>();

    /**
     * Constructs a new ResourceItem.
     * @param name the name of the resource as it appears in the XML and R.java files.
     */
    public ProjectResourceItem(String name) {
        super(name);
    }
    
    /**
     * Returns whether the resource item is editable directly.
     * <p/>
     * This is typically the case for resources that don't have alternate versions, or resources
     * of type {@link ResourceType#ID} that aren't declared inline.
     */
    public abstract boolean isEditableDirectly();

    /**
     * Adds a new version of this resource item, by adding its {@link ResourceFile}.
     * @param file the {@link ResourceFile} object.
     */
    protected void add(ResourceFile file) {
        mFiles.add(file);
    }
    
    /**
     * Reset the item by emptying its version list.
     */
    protected void reset() {
        mFiles.clear();
    }
    
    /**
     * Returns the sorted list of {@link ResourceItem} objects for this resource item.
     */
    public ResourceFile[] getSourceFileArray() {
        ArrayList<ResourceFile> list = new ArrayList<ResourceFile>();
        list.addAll(mFiles);
        
        Collections.sort(list, sComparator);
        
        return list.toArray(new ResourceFile[list.size()]);
    }
    
    /**
     * Returns the list of {@link ResourceItem} objects for this resource item.
     */
    public List<ResourceFile> getSourceFileList() {
        return Collections.unmodifiableList(mFiles);
    }
    

    /**
     * Replaces the content of the receiver with the ResourceItem received as parameter.
     * @param item
     */
    protected void replaceWith(ProjectResourceItem item) {
        mFiles.clear();
        mFiles.addAll(item.mFiles);
    }
}
