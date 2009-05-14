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

package com.android.ide.eclipse.adt.internal.project;

import com.android.ide.eclipse.adt.internal.project.ProjectHelper;
import com.android.ide.eclipse.mock.ClasspathEntryMock;
import com.android.ide.eclipse.mock.JavaProjectMock;

import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaModelException;

import junit.framework.TestCase;

public class ProjectHelperTest extends TestCase {

    /** The old container id */
    private final static String OLD_CONTAINER_ID =
        "com.android.ide.eclipse.adt.project.AndroidClasspathContainerInitializer"; //$NON-NLS-1$

    /** The container id for the android framework jar file */
    private final static String CONTAINER_ID =
        "com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"; //$NON-NLS-1$
    
    @Override
    public void setUp() throws Exception {
        // pass for now
    }

    @Override
    public void tearDown() throws Exception {
        // pass for now
    }
    
    public final void testFixProjectClasspathEntriesFromOldContainer() throws JavaModelException {
        // create a project with a path to an android .zip
        JavaProjectMock javaProject = new JavaProjectMock(
                new IClasspathEntry[] {
                        new ClasspathEntryMock(new Path("Project/src"), //$NON-NLS-1$
                                IClasspathEntry.CPE_SOURCE),
                        new ClasspathEntryMock(new Path(OLD_CONTAINER_ID),
                                IClasspathEntry.CPE_CONTAINER),
                },
                new Path("Project/bin"));
        
        ProjectHelper.fixProjectClasspathEntries(javaProject);
        
        IClasspathEntry[] fixedEntries = javaProject.getRawClasspath();
        assertEquals(3, fixedEntries.length);
        assertEquals("Project/src", fixedEntries[0].getPath().toString());
        assertEquals(OLD_CONTAINER_ID, fixedEntries[1].getPath().toString());
        assertEquals(CONTAINER_ID, fixedEntries[2].getPath().toString());
    }
}
