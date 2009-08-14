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

import com.android.ide.eclipse.adt.AndroidConstants;
import com.android.ide.eclipse.adt.internal.resources.ResourceType;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ResourceQualifier;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IFolderListener;
import com.android.ide.eclipse.adt.internal.resources.manager.ResourceMonitor.IProjectListener;
import com.android.ide.eclipse.adt.internal.resources.manager.files.FileWrapper;
import com.android.ide.eclipse.adt.internal.resources.manager.files.FolderWrapper;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IAbstractFile;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IAbstractFolder;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IFileWrapper;
import com.android.ide.eclipse.adt.internal.resources.manager.files.IFolderWrapper;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkConstants;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public final class ResourceManager implements IProjectListener, IFolderListener, IFileListener {

    private final static ResourceManager sThis = new ResourceManager();

    /** List of the qualifier object helping for the parsing of folder names */
    private final ResourceQualifier[] mQualifiers;

    /**
     * Map associating project resource with project objects.
     */
    private final HashMap<IProject, ProjectResources> mMap =
        new HashMap<IProject, ProjectResources>();

    /**
     * Sets up the resource manager with the global resource monitor.
     * @param monitor The global resource monitor
     */
    public static void setup(ResourceMonitor monitor) {
        monitor.addProjectListener(sThis);
        int mask = IResourceDelta.ADDED | IResourceDelta.REMOVED | IResourceDelta.CHANGED;
        monitor.addFolderListener(sThis, mask);
        monitor.addFileListener(sThis, mask);

        CompiledResourcesMonitor.setupMonitor(monitor);
    }

    /**
     * Returns the singleton instance.
     */
    public static ResourceManager getInstance() {
        return sThis;
    }

    /**
     * Returns the resources of a project.
     * @param project The project
     * @return a ProjectResources object or null if none was found.
     */
    public ProjectResources getProjectResources(IProject project) {
        return mMap.get(project);
    }

    /**
     * Processes folder event.
     */
    public void folderChanged(IFolder folder, int kind) {
        ProjectResources resources;

        final IProject project = folder.getProject();

        try {
            if (project.hasNature(AndroidConstants.NATURE) == false) {
                return;
            }
        } catch (CoreException e) {
            // can't get the project nature? return!
            return;
        }

        switch (kind) {
            case IResourceDelta.ADDED:
                // checks if the folder is under res.
                IPath path = folder.getFullPath();

                // the path will be project/res/<something>
                if (path.segmentCount() == 3) {
                    if (isInResFolder(path)) {
                        // get the project and its resource object.
                        resources = mMap.get(project);

                        // if it doesn't exist, we create it.
                        if (resources == null) {
                            resources = new ProjectResources(false /* isFrameworkRepository */);
                            mMap.put(project, resources);
                        }

                        processFolder(new IFolderWrapper(folder), resources);
                    }
                }
                break;
            case IResourceDelta.CHANGED:
                resources = mMap.get(folder.getProject());
                if (resources != null) {
                    ResourceFolder resFolder = resources.getResourceFolder(folder);
                    if (resFolder != null) {
                        resFolder.touch();
                    }
                }
                break;
            case IResourceDelta.REMOVED:
                resources = mMap.get(folder.getProject());
                if (resources != null) {
                    // lets get the folder type
                    ResourceFolderType type = ResourceFolderType.getFolderType(folder.getName());

                    resources.removeFolder(type, folder);
                }
                break;
        }
    }

    /* (non-Javadoc)
     * Sent when a file changed. Depending on the file being changed, and the type of change (ADDED,
     * REMOVED, CHANGED), the file change is processed to update the resource manager data.
     *
     * @param file The file that changed.
     * @param markerDeltas The marker deltas for the file.
     * @param kind The change kind. This is equivalent to
     * {@link IResourceDelta#accept(IResourceDeltaVisitor)}
     *
     * @see IFileListener#fileChanged
     */
    public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind) {
        ProjectResources resources;

        final IProject project = file.getProject();

        try {
            if (project.hasNature(AndroidConstants.NATURE) == false) {
                return;
            }
        } catch (CoreException e) {
            // can't get the project nature? return!
            return;
        }

        switch (kind) {
            case IResourceDelta.ADDED:
                // checks if the file is under res/something.
                IPath path = file.getFullPath();

                if (path.segmentCount() == 4) {
                    if (isInResFolder(path)) {
                        // get the project and its resources
                        resources = mMap.get(project);

                        IContainer container = file.getParent();
                        if (container instanceof IFolder && resources != null) {

                            ResourceFolder folder = resources.getResourceFolder((IFolder)container);

                            if (folder != null) {
                                processFile(new IFileWrapper(file), folder);
                            }
                        }
                    }
                }
                break;
            case IResourceDelta.CHANGED:
                // try to find a matching ResourceFile
                resources = mMap.get(project);
                if (resources != null) {
                    IContainer container = file.getParent();
                    if (container instanceof IFolder) {
                        ResourceFolder resFolder = resources.getResourceFolder((IFolder)container);

                        // we get the delete on the folder before the file, so it is possible
                        // the associated ResourceFolder doesn't exist anymore.
                        if (resFolder != null) {
                            // get the resourceFile, and touch it.
                            ResourceFile resFile = resFolder.getFile(file);
                            if (resFile != null) {
                                resFile.touch();
                            }
                        }
                    }
                }
                break;
            case IResourceDelta.REMOVED:
                // try to find a matching ResourceFile
                resources = mMap.get(project);
                if (resources != null) {
                    IContainer container = file.getParent();
                    if (container instanceof IFolder) {
                        ResourceFolder resFolder = resources.getResourceFolder((IFolder)container);

                        // we get the delete on the folder before the file, so it is possible
                        // the associated ResourceFolder doesn't exist anymore.
                        if (resFolder != null) {
                            // remove the file
                            resFolder.removeFile(file);
                        }
                    }
                }
                break;
        }
    }

    public void projectClosed(IProject project) {
        mMap.remove(project);
    }

    public void projectDeleted(IProject project) {
        mMap.remove(project);
    }

    public void projectOpened(IProject project) {
        createProject(project);
    }

    public void projectOpenedWithWorkspace(IProject project) {
        createProject(project);
    }

    /**
     * Returns the {@link ResourceFolder} for the given file or <code>null</code> if none exists.
     */
    public ResourceFolder getResourceFolder(IFile file) {
        IContainer container = file.getParent();
        if (container.getType() == IResource.FOLDER) {
            IFolder parent = (IFolder)container;
            IProject project = file.getProject();

            ProjectResources resources = getProjectResources(project);
            if (resources != null) {
                return resources.getResourceFolder(parent);
            }
        }

        return null;
    }

    /**
     * Loads and returns the resources for a given {@link IAndroidTarget}
     * @param androidTarget the target from which to load the framework resources
     */
    public ProjectResources loadFrameworkResources(IAndroidTarget androidTarget) {
        String osResourcesPath = androidTarget.getPath(IAndroidTarget.RESOURCES);

        File frameworkRes = new File(osResourcesPath);
        if (frameworkRes.isDirectory()) {
            ProjectResources resources = new ProjectResources(true /* isFrameworkRepository */);

            try {
                loadResources(resources, frameworkRes);
                return resources;
            } catch (IOException e) {
                // since we test that folders are folders, and files are files, this shouldn't
                // happen. We can ignore it.
            }
        }

        return null;
    }

    /**
     * Loads the resources from a folder, and fills the given {@link ProjectResources}.
     * <p/>
     * This is mostly a utility method that should not be used to process actual Eclipse projects
     * (Those are loaded with {@link #createProject(IProject)} for new project or
     * {@link #processFolder(IAbstractFolder, ProjectResources)} and
     * {@link #processFile(IAbstractFile, ResourceFolder)} for folder/file modifications)<br>
     * This method will process files/folders with implementations of {@link IAbstractFile} and
     * {@link IAbstractFolder} based on {@link File} instead of {@link IFile} and {@link IFolder}
     * respectively. This is not proper for handling {@link IProject}s.
     * </p>
     * This is used to load the framework resources, or to do load project resources when
     * setting rendering tests.
     *
     *
     * @param resources The {@link ProjectResources} files to load. It is expected that the
     * framework flag has been properly setup. This is filled up with the content of the folder.
     * @param folder The folder to read the resources from. This is the top level resource folder
     * (res/)
     * @throws IOException
     */
    public void loadResources(ProjectResources resources, File folder) throws IOException {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                ResourceFolder resFolder = processFolder(new FolderWrapper(file),
                        resources);

                if (resFolder != null) {
                    // now we process the content of the folder
                    File[] children = file.listFiles();

                    for (File childRes : children) {
                        if (childRes.isFile()) {
                            processFile(new FileWrapper(childRes), resFolder);
                        }
                    }
                }

            }
        }

        // now that we have loaded the files, we need to force load the resources from them
        resources.loadAll();
    }

    /**
     * Initial project parsing to gather resource info.
     * @param project
     */
    private void createProject(IProject project) {
        if (project.isOpen()) {
            try {
                if (project.hasNature(AndroidConstants.NATURE) == false) {
                    return;
                }
            } catch (CoreException e1) {
                // can't check the nature of the project? ignore it.
                return;
            }

            IFolder resourceFolder = project.getFolder(SdkConstants.FD_RESOURCES);

            ProjectResources projectResources = mMap.get(project);
            if (projectResources == null) {
                projectResources = new ProjectResources(false /* isFrameworkRepository */);
                mMap.put(project, projectResources);
            }

            if (resourceFolder != null && resourceFolder.exists()) {
                try {
                    IResource[] resources = resourceFolder.members();

                    for (IResource res : resources) {
                        if (res.getType() == IResource.FOLDER) {
                            IFolder folder = (IFolder)res;
                            ResourceFolder resFolder = processFolder(new IFolderWrapper(folder),
                                    projectResources);

                            if (resFolder != null) {
                                // now we process the content of the folder
                                IResource[] files = folder.members();

                                for (IResource fileRes : files) {
                                    if (fileRes.getType() == IResource.FILE) {
                                        IFile file = (IFile)fileRes;

                                        processFile(new IFileWrapper(file), resFolder);
                                    }
                                }
                            }
                        }
                    }
                } catch (CoreException e) {
                    // This happens if the project is closed or if the folder doesn't exist.
                    // Since we already test for that, we can ignore this exception.
                }
            }
        }
    }

    /**
     * Creates a {@link FolderConfiguration} matching the folder segments.
     * @param folderSegments The segments of the folder name. The first segments should contain
     * the name of the folder
     * @return a FolderConfiguration object, or null if the folder name isn't valid..
     */
    public FolderConfiguration getConfig(String[] folderSegments) {
        FolderConfiguration config = new FolderConfiguration();

        // we are going to loop through the segments, and match them with the first
        // available qualifier. If the segment doesn't match we try with the next qualifier.
        // Because the order of the qualifier is fixed, we do not reset the first qualifier
        // after each sucessful segment.
        // If we run out of qualifier before processing all the segments, we fail.

        int qualifierIndex = 0;
        int qualifierCount = mQualifiers.length;

        for (int i = 1 ; i < folderSegments.length; i++) {
            String seg = folderSegments[i];
            if (seg.length() > 0) {
                while (qualifierIndex < qualifierCount &&
                        mQualifiers[qualifierIndex].checkAndSet(seg, config) == false) {
                    qualifierIndex++;
                }

                // if we reached the end of the qualifier we didn't find a matching qualifier.
                if (qualifierIndex == qualifierCount) {
                    return null;
                }

            } else {
                return null;
            }
        }

        return config;
    }

    /**
     * Processes a folder and adds it to the list of the project resources.
     * @param folder the folder to process
     * @param project the folder's project.
     * @return the ConfiguredFolder created from this folder, or null if the process failed.
     */
    private ResourceFolder processFolder(IAbstractFolder folder, ProjectResources project) {
        // split the name of the folder in segments.
        String[] folderSegments = folder.getName().split(FolderConfiguration.QUALIFIER_SEP);

        // get the enum for the resource type.
        ResourceFolderType type = ResourceFolderType.getTypeByName(folderSegments[0]);

        if (type != null) {
            // get the folder configuration.
            FolderConfiguration config = getConfig(folderSegments);

            if (config != null) {
                ResourceFolder configuredFolder = project.add(type, config, folder);

                return configuredFolder;
            }
        }

        return null;
    }

    /**
     * Processes a file and adds it to its parent folder resource.
     * @param file
     * @param folder
     */
    private void processFile(IAbstractFile file, ResourceFolder folder) {
        // get the type of the folder
        ResourceFolderType type = folder.getType();

        // look for this file if it's already been created
        ResourceFile resFile = folder.getFile(file);

        if (resFile != null) {
            // invalidate the file
            resFile.touch();
        } else {
            // create a ResourceFile for it.

            // check if that's a single or multi resource type folder. For now we define this by
            // the number of possible resource type output by files in the folder. This does
            // not make the difference between several resource types from a single file or
            // the ability to have 2 files in the same folder generating 2 different types of
            // resource. The former is handled by MultiResourceFile properly while we don't
            // handle the latter. If we were to add this behavior we'd have to change this call.
            ResourceType[] types = FolderTypeRelationship.getRelatedResourceTypes(type);

            if (types.length == 1) {
                resFile = new SingleResourceFile(file, folder);
            } else {
                resFile = new MultiResourceFile(file, folder);
            }

            // add it to the folder
            folder.addFile(resFile);
        }
    }

    /**
     * Returns true if the path is under /project/res/
     * @param path a workspace relative path
     * @return true if the path is under /project res/
     */
    private boolean isInResFolder(IPath path) {
        return SdkConstants.FD_RESOURCES.equalsIgnoreCase(path.segment(1));
    }

    /**
     * Private constructor to enforce singleton design.
     */
    ResourceManager() {
        // get the default qualifiers.
        FolderConfiguration defaultConfig = new FolderConfiguration();
        defaultConfig.createDefault();
        mQualifiers = defaultConfig.getQualifiers();
    }
}
