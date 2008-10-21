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

public class TouchScreenQualifierTest extends TestCase {

    private TouchScreenQualifier tsq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tsq = new TouchScreenQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tsq = null;
        config = null;
    }

    public void testNoTouch() {
        assertEquals(true, tsq.checkAndSet("notouch", config)); //$NON-NLS-1$
        assertTrue(config.getTouchTypeQualifier() != null);
        assertEquals(TouchScreenQualifier.TouchScreenType.NOTOUCH,
                config.getTouchTypeQualifier().getValue());
        assertEquals("notouch", config.getTouchTypeQualifier().toString()); //$NON-NLS-1$
    }

    public void testFinger() {
        assertEquals(true, tsq.checkAndSet("finger", config)); //$NON-NLS-1$
        assertTrue(config.getTouchTypeQualifier() != null);
        assertEquals(TouchScreenQualifier.TouchScreenType.FINGER,
                config.getTouchTypeQualifier().getValue());
        assertEquals("finger", config.getTouchTypeQualifier().toString()); //$NON-NLS-1$
    }

    public void testStylus() {
        assertEquals(true, tsq.checkAndSet("stylus", config)); //$NON-NLS-1$
        assertTrue(config.getTouchTypeQualifier() != null);
        assertEquals(TouchScreenQualifier.TouchScreenType.STYLUS,
                config.getTouchTypeQualifier().getValue());
        assertEquals("stylus", config.getTouchTypeQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, tsq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, tsq.checkAndSet("STYLUS", config));//$NON-NLS-1$
        assertEquals(false, tsq.checkAndSet("other", config));//$NON-NLS-1$
    }

}
