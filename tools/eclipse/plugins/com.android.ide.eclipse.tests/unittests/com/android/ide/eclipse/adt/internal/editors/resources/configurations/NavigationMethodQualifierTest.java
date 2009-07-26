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
import com.android.ide.eclipse.adt.internal.resources.configurations.NavigationMethodQualifier;

import junit.framework.TestCase;

public class NavigationMethodQualifierTest extends TestCase {

    private FolderConfiguration config;
    private NavigationMethodQualifier nmq;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = new FolderConfiguration();
        nmq = new NavigationMethodQualifier();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        config = null;
        nmq = null;
    }
    
    public void testDPad() {
        assertEquals(true, nmq.checkAndSet("dpad", config)); //$NON-NLS-1$
        assertTrue(config.getNavigationMethodQualifier() != null);
        assertEquals(NavigationMethodQualifier.NavigationMethod.DPAD,
                config.getNavigationMethodQualifier().getValue());
        assertEquals("dpad", config.getNavigationMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void testTrackball() {
        assertEquals(true, nmq.checkAndSet("trackball", config)); //$NON-NLS-1$
        assertTrue(config.getNavigationMethodQualifier() != null);
        assertEquals(NavigationMethodQualifier.NavigationMethod.TRACKBALL,
                config.getNavigationMethodQualifier().getValue());
        assertEquals("trackball", config.getNavigationMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void testWheel() {
        assertEquals(true, nmq.checkAndSet("wheel", config)); //$NON-NLS-1$
        assertTrue(config.getNavigationMethodQualifier() != null);
        assertEquals(NavigationMethodQualifier.NavigationMethod.WHEEL,
                config.getNavigationMethodQualifier().getValue());
        assertEquals("wheel", config.getNavigationMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, nmq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, nmq.checkAndSet("WHEEL", config));//$NON-NLS-1$
        assertEquals(false, nmq.checkAndSet("other", config));//$NON-NLS-1$
    }
}
