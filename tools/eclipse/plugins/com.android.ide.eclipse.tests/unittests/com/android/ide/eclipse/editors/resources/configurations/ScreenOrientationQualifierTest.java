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

package com.android.ide.eclipse.editors.resources.configurations;

import junit.framework.TestCase;

public class ScreenOrientationQualifierTest extends TestCase {

    private ScreenOrientationQualifier soq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        soq = new ScreenOrientationQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        soq = null;
        config = null;
    }

    public void testPortrait() {
        assertEquals(true, soq.checkAndSet("port", config)); //$NON-NLS-1$
        assertTrue(config.getScreenOrientationQualifier() != null);
        assertEquals(ScreenOrientationQualifier.ScreenOrientation.PORTRAIT,
                config.getScreenOrientationQualifier().getValue());
        assertEquals("port", config.getScreenOrientationQualifier().toString()); //$NON-NLS-1$
    }

    public void testLanscape() {
        assertEquals(true, soq.checkAndSet("land", config)); //$NON-NLS-1$
        assertTrue(config.getScreenOrientationQualifier() != null);
        assertEquals(ScreenOrientationQualifier.ScreenOrientation.LANDSCAPE,
                config.getScreenOrientationQualifier().getValue());
        assertEquals("land", config.getScreenOrientationQualifier().toString()); //$NON-NLS-1$
    }

    public void testSquare() {
        assertEquals(true, soq.checkAndSet("square", config)); //$NON-NLS-1$
        assertTrue(config.getScreenOrientationQualifier() != null);
        assertEquals(ScreenOrientationQualifier.ScreenOrientation.SQUARE,
                config.getScreenOrientationQualifier().getValue());
        assertEquals("square", config.getScreenOrientationQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, soq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, soq.checkAndSet("PORT", config));//$NON-NLS-1$
        assertEquals(false, soq.checkAndSet("landscape", config));//$NON-NLS-1$
        assertEquals(false, soq.checkAndSet("portrait", config));//$NON-NLS-1$
        assertEquals(false, soq.checkAndSet("other", config));//$NON-NLS-1$
    }
}
