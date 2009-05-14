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

import com.android.ide.eclipse.adt.internal.project.BaseProjectHelper;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import java.util.ArrayList;

/**
 * Resource Monitor for the whole editor plugin. Other, more simple, listeners can register to
 * that one.
 */
public class ResourceMonitor implements IResourceChangeListener {

    private final static ResourceMonitor sThis = new ResourceMonitor();

    /**
     * Classes which implement this interface provide a method that deals
     * with file change events.
     */
    public interface IFileListener {
        /**
         * Sent when a file changed.
         * @param file The file that changed.
         * @param markerDeltas The marker deltas for the file.
         * @param kind The change kind. This is equivalent to
         * {@link IResourceDelta#accept(IResourceDeltaVisitor)}
         */
        public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind);
    }

    /**
     * Classes which implements this interface provide methods dealing with project events.
     */
    public interface IProjectListener {
        /**
         * Sent for each opened android project at the time the listener is put in place.
         * @param project the opened project.
         */
        public void projectOpenedWithWorkspace(IProject project);
        /**
         * Sent when a project is opened.
         * @param project the project being opened.
         */
        public void projectOpened(IProject project);
        /**
         * Sent when a project is closed.
         * @param project the project being closed.
         */
        public void projectClosed(IProject project);
        /**
         * Sent when a project is deleted.
         * @param project the project about to be deleted.
         */
        public void projectDeleted(IProject project);
    }

    /**
     * Classes which implement this interface provide a method that deals
     * with folder change events
     */
    public interface IFolderListener {
        /**
         * Sent when a folder changed.
         * @param folder The file that was changed
         * @param kind The change kind. This is equivalent to {@link IResourceDelta#getKind()}
         */
        public void folderChanged(IFolder folder, int kind);
    }
    
    /**
     * Interface for a listener to be notified when resource change event starts and ends.
     */
    public interface IResourceEventListener {
        public void resourceChangeEventStart();
        public void resourceChangeEventEnd();
    }
    
    /**
     * Base listener bundle to associate a listener to an event mask.
     */
    private static class ListenerBundle {
        /** Mask value to accept all events */
        public final static int MASK_NONE = -1; 

        /**
         * Event mask. Values accepted are IResourceDelta.###
         * @see IResourceDelta#ADDED
         * @see IResourceDelta#REMOVED
         * @see IResourceDelta#CHANGED
         * @see IResourceDelta#ADDED_PHANTOM
         * @see IResourceDelta#REMOVED_PHANTOM
         * */
        int kindMask;
    }
    
    /**
     * Listener bundle for file event.
     */
    private static class FileListenerBundle extends ListenerBundle {

        /** The file listener */
        IFileListener listener;
    }
    
    /**
     * Listener bundle for folder event.
     */
    private static class FolderListenerBundle extends ListenerBundle {
        /** The file listener */
        IFolderListener listener;
    }
    
    private final ArrayList<FileListenerBundle> mFileListeners =
        new ArrayList<FileListenerBundle>();

    private final ArrayList<FolderListenerBundle> mFolderListeners =
        new ArrayList<FolderListenerBundle>();

    private final ArrayList<IProjectListener> mProjectListeners = new ArrayList<IProjectListener>();
    
    private final ArrayList<IResourceEventListener> mEventListeners =
        new ArrayList<IResourceEventListener>();
    
    private IWorkspace mWorkspace;

    /**
     * Delta visitor for resource changes.
     */
    private final class DeltaVisitor implements IResourceDeltaVisitor {

        public boolean visit(IResourceDelta delta) {
            IResource r = delta.getResource();
            int type = r.getType();
            if (type == IResource.FILE) {
                int kind = delta.getKind();
                // notify the listeners.
                for (FileListenerBundle bundle : mFileListeners) {
                    if (bundle.kindMask == ListenerBundle.MASK_NONE
                            || (bundle.kindMask & kind) != 0) {
                        bundle.listener.fileChanged((IFile)r, delta.getMarkerDeltas(), kind);
                    }
                }
                return false;
            } else if (type == IResource.FOLDER) {
                int kind = delta.getKind();
                // notify the listeners.
                for (FolderListenerBundle bundle : mFolderListeners) {
                    if (bundle.kindMask == ListenerBundle.MASK_NONE
                            || (bundle.kindMask & kind) != 0) {
                        bundle.listener.folderChanged((IFolder)r, kind);
                    }
                }
                return true;
            } else if (type == IResource.PROJECT) {
                int flags = delta.getFlags();

                if (flags == IResourceDelta.OPEN) {
                    // the project is opening or closing.
                    IProject project = (IProject)r;
                    
                    if (project.isOpen()) {
                        // notify the listeners.
                        for (IProjectListener pl : mProjectListeners) {
                            pl.projectOpened(project);
                        }
                    } else {
                        // notify the listeners.
                        for (IProjectListener pl : mProjectListeners) {
                            pl.projectClosed(project);
                        }
                    }
                }
            }

            return true;
        }
    }
    
    public static ResourceMonitor getMonitor() {
        return sThis;
    }

    
    /**
     * Starts the resource monitoring.
     * @param ws The current workspace.
     * @return The monitor object.
     */
    public static ResourceMonitor startMonitoring(IWorkspace ws) {
        if (sThis != null) {
            ws.addResourceChangeListener(sThis,
                    IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_DELETE);
            sThis.mWorkspace = ws;
        }
        return sThis;
    }

    /**
     * Stops the resource monitoring.
     * @param ws The current workspace.
     */
    public static void stopMonitoring(IWorkspace ws) {
        if (sThis != null) {
            ws.removeResourceChangeListener(sThis);
            
            sThis.mFileListeners.clear();
            sThis.mProjectListeners.clear();
        }
    }

    /**
     * Adds a file listener.
     * @param listener The listener to receive the events.
     * @param kindMask The event mask to filter out specific events.
     * {@link ListenerBundle#MASK_NONE} will forward all events. 
     */
    public synchronized void addFileListener(IFileListener listener, int kindMask) {
        FileListenerBundle bundle = new FileListenerBundle();
        bundle.listener = listener;
        bundle.kindMask = kindMask;
        
        mFileListeners.add(bundle);
    }
    
    /**
     * Removes an existing file listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeFileListener(IFileListener listener) {
        for (int i = 0 ; i < mFileListeners.size() ; i++) {
            FileListenerBundle bundle = mFileListeners.get(i);
            if (bundle.listener == listener) {
                mFileListeners.remove(i);
                return;
            }
        }
    }

    /**
     * Adds a folder listener.
     * @param listener The listener to receive the events.
     * @param kindMask The event mask to filter out specific events.
     * {@link ListenerBundle#MASK_NONE} will forward all events. 
     */
    public synchronized void addFolderListener(IFolderListener listener, int kindMask) {
        FolderListenerBundle bundle = new FolderListenerBundle();
        bundle.listener = listener;
        bundle.kindMask = kindMask;
        
        mFolderListeners.add(bundle);
    }

    /**
     * Removes an existing folder listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeFolderListener(IFolderListener listener) {
        for (int i = 0 ; i < mFolderListeners.size() ; i++) {
            FolderListenerBundle bundle = mFolderListeners.get(i);
            if (bundle.listener == listener) {
                mFolderListeners.remove(i);
                return;
            }
        }
    }

    /**
     * Adds a project listener.
     * @param listener The listener to receive the events.
     */
    public synchronized void addProjectListener(IProjectListener listener) {
        mProjectListeners.add(listener);
        
        // we need to look at the opened projects and give them to the listener.

        // get the list of opened android projects.
        IWorkspaceRoot workspaceRoot = mWorkspace.getRoot();
        IJavaModel javaModel = JavaCore.create(workspaceRoot);
        IJavaProject[] androidProjects = BaseProjectHelper.getAndroidProjects(javaModel);

        for (IJavaProject androidProject : androidProjects) {
            listener.projectOpenedWithWorkspace(androidProject.getProject());
        }
    }
    
    /**
     * Removes an existing project listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeProjectListener(IProjectListener listener) {
        mProjectListeners.remove(listener);
    }
    
    /**
     * Adds a resource event listener.
     * @param listener The listener to receive the events.
     */
    public synchronized void addResourceEventListener(IResourceEventListener listener) {
        mEventListeners.add(listener);
    }

    /**
     * Removes an existing Resource Event listener.
     * @param listener the listener to remove.
     */
    public synchronized void removeResourceEventListener(IResourceEventListener listener) {
        mEventListeners.remove(listener);
    }

    /**
     * Processes the workspace resource change events.
     */
    public void resourceChanged(IResourceChangeEvent event) {
        // notify the event listeners of a start.
        for (IResourceEventListener listener : mEventListeners) {
            listener.resourceChangeEventStart();
        }
        
        if (event.getType() == IResourceChangeEvent.PRE_DELETE) {
            // a project is being deleted. Lets get the project object and remove
            // its compiled resource list.
            IResource r = event.getResource();
            IProject project = r.getProject();

            // notify the listeners.
            for (IProjectListener pl : mProjectListeners) {
                pl.projectDeleted(project);
            }
        } else {
            // this a regular resource change. We get the delta and go through it with a visitor.
            IResourceDelta delta = event.getDelta();
            
            DeltaVisitor visitor = new DeltaVisitor();
            try {
                delta.accept(visitor);
            } catch (CoreException e) {
            }
        }

        // we're done, notify the event listeners.
        for (IResourceEventListener listener : mEventListeners) {
            listener.resourceChangeEventEnd();
        }
    }
    
}
