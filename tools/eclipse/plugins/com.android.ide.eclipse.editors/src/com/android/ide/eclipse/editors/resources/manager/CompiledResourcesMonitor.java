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

import com.android.ide.eclipse.common.AndroidConstants;
import com.android.ide.eclipse.common.project.AndroidManifestHelper;
import com.android.ide.eclipse.common.resources.ResourceType;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IFileListener;
import com.android.ide.eclipse.editors.resources.manager.ResourceMonitor.IProjectListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * A monitor for the compiled resources. This only monitors changes in the resources of type
 *  {@link ResourceType#ID}.
 */
public final class CompiledResourcesMonitor implements IFileListener, IProjectListener {

    private final static CompiledResourcesMonitor sThis = new CompiledResourcesMonitor();
    
    /**
     * Sets up the monitoring system.
     * @param monitor The main Resource Monitor.
     */
    public static void setupMonitor(ResourceMonitor monitor) {
        monitor.addFileListener(sThis, IResourceDelta.ADDED | IResourceDelta.CHANGED);
        monitor.addProjectListener(sThis);
    }

    /**
     * private constructor to prevent construction.
     */
    private CompiledResourcesMonitor() {
    }


    /* (non-Javadoc)
     * Sent when a file changed : if the file is the R class, then it is parsed again to update
     * the internal data.
     * 
     * @param file The file that changed.
     * @param markerDeltas The marker deltas for the file.
     * @param kind The change kind. This is equivalent to
     * {@link IResourceDelta#accept(IResourceDeltaVisitor)}
     * 
     * @see IFileListener#fileChanged
     */
    public void fileChanged(IFile file, IMarkerDelta[] markerDeltas, int kind) {
        if (file.getName().equals(AndroidConstants.FN_COMPILED_RESOURCE_CLASS)) {
            loadAndParseRClass(file.getProject());
        }
    }

    /**
     * Processes project close event.
     */
    public void projectClosed(IProject project) {
        // the ProjectResources object will be removed by the ResourceManager.
    }

    /**
     * Processes project delete event.
     */
    public void projectDeleted(IProject project) {
        // the ProjectResources object will be removed by the ResourceManager.
    }

    /**
     * Processes project open event.
     */
    public void projectOpened(IProject project) {
        // when the project is opened, we get an ADDED event for each file, so we don't
        // need to do anything here.
    }

    /**
     * Processes existing project at init time.
     */
    public void projectOpenedWithWorkspace(IProject project) {
        try {
            // check this is an android project
            if (project.hasNature(AndroidConstants.NATURE)) {
                loadAndParseRClass(project);
            }
        } catch (CoreException e) {
            // pass
        }
    }
    
    private void loadAndParseRClass(IProject project) {
        try {
            // first check there's a ProjectResources to store the content
            ProjectResources projectResources = ResourceManager.getInstance().getProjectResources(
                    project);

            if (projectResources != null) {
                // create the classname
                String className = getRClassName(project);
        
                // create a temporary class loader to load it. 
                ProjectClassLoader loader = new ProjectClassLoader(null /* parentClassLoader */,
                        project);
                
                Class<?> clazz = loader.loadClass(className);
                
                if (clazz != null) {
                    // create the maps to store the result of the parsing
                    Map<String, Map<String, Integer>> resourceValueMap =
                        new HashMap<String, Map<String, Integer>>();
                    Map<Integer, String[]> genericValueToNameMap =
                        new HashMap<Integer, String[]>();
                    Map<IntArrayWrapper, String> styleableValueToNameMap =
                        new HashMap<IntArrayWrapper, String>();
                    
                    // parse the class
                    if (parseClass(clazz, genericValueToNameMap, styleableValueToNameMap,
                            resourceValueMap)) {
                        // now we associate the maps to the project.
                        projectResources.setCompiledResources(genericValueToNameMap,
                                styleableValueToNameMap, resourceValueMap);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // pass
        }
    }

    /**
     * Parses a R class, and fills maps.
     * @param rClass the class to parse
     * @param genericValueToNameMap
     * @param styleableValueToNameMap
     * @param resourceValueMap
     * @return
     */
    private boolean parseClass(Class<?> rClass, Map<Integer, String[]> genericValueToNameMap,
            Map<IntArrayWrapper, String> styleableValueToNameMap, Map<String,
            Map<String, Integer>> resourceValueMap) {
        try {
            for (Class<?> inner : rClass.getDeclaredClasses()) {
                String resType = inner.getSimpleName();

                Map<String, Integer> fullMap = new HashMap<String, Integer>();
                resourceValueMap.put(resType, fullMap);
                
                for (Field f : inner.getDeclaredFields()) {
                    // only process static final fields.
                    int modifiers = f.getModifiers();
                    if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                        Class<?> type = f.getType();
                        if (type.isArray() && type.getComponentType() == int.class) {
                            // if the object is an int[] we put it in the styleable map
                            styleableValueToNameMap.put(new IntArrayWrapper((int[]) f.get(null)),
                                    f.getName());
                        } else if (type == int.class) {
                            Integer value = (Integer) f.get(null); 
                            genericValueToNameMap.put(value, new String[] { f.getName(), resType });
                            fullMap.put(f.getName(), value);
                        } else {
                            assert false;
                        }
                    }
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        }
        return false;
    }
    
    private String getRClassName(IProject project) {
        // create the classname
        AndroidManifestHelper manifest = new AndroidManifestHelper(project);
        String javaPackage = manifest.getPackageName();
        
        return javaPackage + ".R"; //$NON-NLS-1$
    }
    
}
