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

package com.android.ide.eclipse.editors.resources.manager;

import com.android.ide.eclipse.common.resources.ResourceType;
import com.android.ide.eclipse.editors.resources.manager.files.IAbstractFile;
import com.android.layoutlib.api.IResourceValue;
import com.android.layoutlib.utils.ResourceValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParserFactory;

/**
 * Represents a resource file describing a single resource.
 * <p/>
 * This is typically an XML file inside res/anim, res/layout, or res/menu or an image file
 * under res/drawable.
 */
public class SingleResourceFile extends ResourceFile {

    private final static SAXParserFactory sParserFactory = SAXParserFactory.newInstance();
    static {
        sParserFactory.setNamespaceAware(true);
    }
    
    private final static Pattern sXmlPattern = Pattern.compile("^(.+)\\.xml", //$NON-NLS-1$
            Pattern.CASE_INSENSITIVE);
    
    private final static Pattern[] sDrawablePattern = new Pattern[] {
        Pattern.compile("^(.+)\\.9\\.png", Pattern.CASE_INSENSITIVE), //$NON-NLS-1$
        Pattern.compile("^(.+)\\.png", Pattern.CASE_INSENSITIVE), //$NON-NLS-1$
        Pattern.compile("^(.+)\\.jpg", Pattern.CASE_INSENSITIVE), //$NON-NLS-1$
        Pattern.compile("^(.+)\\.gif", Pattern.CASE_INSENSITIVE), //$NON-NLS-1$
    };
    
    private String mResourceName;
    private ResourceType mType;
    private IResourceValue mValue;

    public SingleResourceFile(IAbstractFile file, ResourceFolder folder) {
        super(file, folder);
        
        // we need to infer the type of the resource from the folder type.
        // This is easy since this is a single Resource file.
        ResourceType[] types = FolderTypeRelationship.getRelatedResourceTypes(folder.getType());
        mType = types[0];

        // compute the resource name
        mResourceName = getResourceName(mType);
        
        mValue = new ResourceValue(mType.getName(), getResourceName(mType), file.getOsLocation(),
                isFramework());
    }

    @Override
    public ResourceType[] getResourceTypes() {
        return FolderTypeRelationship.getRelatedResourceTypes(getFolder().getType());
    }

    @Override
    public boolean hasResources(ResourceType type) {
        return FolderTypeRelationship.match(type, getFolder().getType());
    }

    @Override
    public Collection<ProjectResourceItem> getResources(ResourceType type,
            ProjectResources projectResources) {
        
        // looking for an existing ResourceItem with this name and type
        ProjectResourceItem item = projectResources.findResourceItem(type, mResourceName);
        
        ArrayList<ProjectResourceItem> items = new ArrayList<ProjectResourceItem>();

        if (item == null) {
            item = new ConfigurableResourceItem(mResourceName);
            items.add(item);
        }
        
        // add this ResourceFile to the ResourceItem
        item.add(this);
        
        return items;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.manager.ResourceFile#getValue(com.android.ide.eclipse.common.resources.ResourceType, java.lang.String)
     * 
     * This particular implementation does not care about the type or name since a
     * SingleResourceFile represents a file generating only one resource.
     * The value returned is the full absolute path of the file in OS form.
     */
    @Override
    public IResourceValue getValue(ResourceType type, String name) {
        return mValue;
    }
    
    /**
     * Returns the name of the resources.
     */
    private String getResourceName(ResourceType type) {
        // get the name from the filename.
        String name = getFile().getName();
        
        if (type == ResourceType.ANIM || type == ResourceType.LAYOUT || type == ResourceType.MENU ||
                type == ResourceType.COLOR || type == ResourceType.XML) {
            Matcher m = sXmlPattern.matcher(name);
            if (m.matches()) {
                return m.group(1);
            }
        } else if (type == ResourceType.DRAWABLE) {
            for (Pattern p : sDrawablePattern) {
                Matcher m = p.matcher(name);
                if (m.matches()) {
                    return m.group(1);
                }
            }
            
            // also try the Xml pattern for selector/shape based drawable.
            Matcher m = sXmlPattern.matcher(name);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return name;
    }
}
