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

package com.android.ide.eclipse.editors.layout;

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.editors.resources.manager.ResourceFolder;
import com.android.ide.eclipse.editors.resources.manager.ResourceFolderType;
import com.android.ide.eclipse.editors.resources.manager.ResourceManager;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IResourceEventListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Monitor for file changes triggering a layout redraw.
 */
public final class LayoutReloadMonitor implements IFileListener, IResourceEventListener {
    
    // singleton, enforced by private constructor.
    private final static LayoutReloadMonitor sThis = new LayoutReloadMonitor();
    
    /**
     * Map of listeners by IProject.
     */
    private final Map<IProject, List<ILayoutReloadListener>> mListenerMap =
        new HashMap<IProject, List<ILayoutReloadListener>>();
    
    private final static int CHANGE_CODE = 0;
    private final static int CHANGE_RESOURCES = 1;
    private final static int CHANGE_R = 2;
    private final static int CHANGE_COUNT = 3;
    /**
     * List of projects having received a file change. the boolean[] contains 3 values:
     * <ul><li>CHANGE_CODE: code change flag.</li>
     * <li>CHANGE_RESOURCES: resource change flag.</li>
     * <li>CHANGE_R: R clas change flag</li></ul>
     */
    private final Map<IProject, boolean[]> mChangedProjects = new HashMap<IProject, boolean[]>();
    
    /**
     * Classes which implement this interface provide a method to respond to resource changes
     * triggering a layout redraw
     */
    public interface ILayoutReloadListener {
        /**
         * Sent when the layout needs to be redrawn
         * @param codeChange The trigger happened due to a code change.
         * @param rChange The trigger happened due to a change in the R class.
         * @param resChange The trigger happened due to a resource change.
         */
        void reloadLayout(boolean codeChange, boolean rChange, boolean resChange); 
    }
    
    /**
     * Returns the single instance of {@link LayoutReloadMonitor}.
     */
    public static LayoutReloadMonitor getMonitor() {
        return sThis;
    }
    
    private LayoutReloadMonitor() {
        ResourceMonitor monitor = ResourceMonitor.getMonitor();
        monitor.addFileListener(this, IResourceDelta.ADDED | IResourceDelta.CHANGED);
        monitor.addResourceEventListener(this);
    }
    
    /**
     * Adds a listener for a given {@link IProject}.
     * @param project
     * @param listener
     */
    public void addListener(IProject project, ILayoutReloadListener listener) {
        synchronized (mListenerMap) {
            List<ILayoutReloadListener> list = mListenerMap.get(project);
            if (list == null) {
                list = new ArrayList<ILayoutReloadListener>();
                mListenerMap.put(project, list);
            }
            
            list.add(listener);
        }
    }
    
    /**
     * Removes a listener for a given {@link IProject}.
     * @param project
     * @param listener
     */
    public void removeListener(IProject project, ILayoutReloadListener listener) {
        synchronized (mListenerMap) {
            List<ILayoutReloadListener> list = mListenerMap.get(project);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IFileListener#fileChanged(org.eclipse.core.resources.IFile, org.eclipse.core.resources.IMarkerDelta[], int)
     * 
     * Callback for ResourceMonitor.IFileListener. Called when a file changed.
     * This records the changes for each project, but does not notify listeners.
     * @see #resourceChangeEventEnd
     */
    public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind) {
        // get the file project
        IProject project = file.getProject();

        // if this project has already been marked as modified, we do nothing.
        boolean[] changeFlags = mChangedProjects.get(project);
        if (changeFlags != null && changeFlags[CHANGE_CODE] && changeFlags[CHANGE_RESOURCES] &&
                changeFlags[CHANGE_R]) {
            return;
        }
        
        // now check that the file is *NOT* a layout file (those automatically trigger a layout
        // reload and we don't want to do it twice.
        ResourceFolder resFolder = ResourceManager.getInstance().getResourceFolder(file);
        if (resFolder != null) {
            if (resFolder.getType() != ResourceFolderType.LAYOUT) {
                // this is a resource change!
                if (changeFlags == null) {
                    changeFlags = new boolean[CHANGE_COUNT];
                    mChangedProjects.put(project, changeFlags);
                }
    
                changeFlags[CHANGE_RESOURCES] = true;
            }
        } else if (AndroidConstants.EXT_CLASS.equals(file.getFileExtension())) {
            if (file.getName().matches("R[\\$\\.](.*)")) {
                // this is a R change!
                if (changeFlags == null) {
                    changeFlags = new boolean[CHANGE_COUNT];
                    mChangedProjects.put(project, changeFlags);
                }

                changeFlags[CHANGE_R] = true;
            } else {
                // this is a code change!
                if (changeFlags == null) {
                    changeFlags = new boolean[CHANGE_COUNT];
                    mChangedProjects.put(project, changeFlags);
                }

                changeFlags[CHANGE_CODE] = true;
            }
        }
    }
    
    /*
     * (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IResourceEventListener#resourceChangeEventStart()
     * 
     * Callback for ResourceMonitor.IResourceEventListener. Called at the beginning of a resource
     * change event. This is called once, while fileChanged can be called several times.
     * 
     */
    public void resourceChangeEventStart() {
        // nothing to be done here, it all happens in the resourceChangeEventEnd
    }

    /*
     * (non-Javadoc)
     * @see com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IResourceEventListener#resourceChangeEventEnd()
     * 
     * Callback for ResourceMonitor.IResourceEventListener. Called at the end of a resource
     * change event. This is where we notify the listeners.
     */
    public void resourceChangeEventEnd() {
        // for each IProject that was changed, we notify all the listeners.
        synchronized (mListenerMap) {
            for (Entry<IProject, boolean[]> project : mChangedProjects.entrySet()) {
                List<ILayoutReloadListener> listeners = mListenerMap.get(project.getKey());
                
                boolean[] flags = project.getValue();
                
                if (listeners != null) {
                    for (ILayoutReloadListener listener : listeners) {
                        listener.reloadLayout(flags[CHANGE_CODE], flags[CHANGE_R],
                                flags[CHANGE_RESOURCES]);
                    }
                }
            }
        }
        
        // empty the list.
        mChangedProjects.clear();
    }
}
