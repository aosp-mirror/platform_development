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

package com.android.ide.eclipse.adt.internal.resources;

/**
 * A repository of resources. This allows access to the resource by {@link ResourceType}.
 */
public interface IResourceRepository {

    /**
     * Returns the present {@link ResourceType}s in the project.
     * @return an array containing all the type of resources existing in the project.
     */
    public abstract ResourceType[] getAvailableResourceTypes();

    /**
     * Returns an array of the existing resource for the specified type.
     * @param type the type of the resources to return
     */
    public abstract ResourceItem[] getResources(ResourceType type);

    /**
     * Returns whether resources of the specified type are present.
     * @param type the type of the resources to check.
     */
    public abstract boolean hasResources(ResourceType type);
    
    /**
     * Returns whether the repository is a system repository.
     */
    public abstract boolean isSystemRepository();

}
