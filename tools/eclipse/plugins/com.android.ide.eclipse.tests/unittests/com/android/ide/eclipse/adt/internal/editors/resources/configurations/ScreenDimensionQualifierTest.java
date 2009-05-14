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

package com.android.ide.eclipse.adt.internal.editors.resources.configurations;

import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;
import com.android.ide.eclipse.adt.internal.resources.configurations.ScreenDimensionQualifier;

import junit.framework.TestCase;

public class ScreenDimensionQualifierTest extends TestCase {

    private ScreenDimensionQualifier sdq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sdq = new ScreenDimensionQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        sdq = null;
        config = null;
    }
    
    public void testCheckAndSet() {
        assertEquals(true, sdq.checkAndSet("400x200", config));//$NON-NLS-1$
        assertTrue(config.getScreenDimensionQualifier() != null);
        assertEquals(400, config.getScreenDimensionQualifier().getValue1());
        assertEquals(200, config.getScreenDimensionQualifier().getValue2());
        assertEquals("400x200", config.getScreenDimensionQualifier().toString()); //$NON-NLS-1$
    }
    
    public void testFailures() {
        assertEquals(false, sdq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("400X200", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("x200", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("ax200", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("400x", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("400xa", config));//$NON-NLS-1$
        assertEquals(false, sdq.checkAndSet("other", config));//$NON-NLS-1$
    }
}
