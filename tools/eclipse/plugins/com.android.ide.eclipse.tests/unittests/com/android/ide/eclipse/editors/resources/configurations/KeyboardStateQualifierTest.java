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

package com.android.ide.eclipse.editors.resources.configurations;

import junit.framework.TestCase;

public class KeyboardStateQualifierTest extends TestCase {

    private KeyboardStateQualifier ksq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ksq = new KeyboardStateQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        ksq = null;
        config = null;
    }

    public void testExposed() {
        assertEquals(true, ksq.checkAndSet("keysexposed", config)); //$NON-NLS-1$
        assertTrue(config.getKeyboardStateQualifier() != null);
        assertEquals(KeyboardStateQualifier.KeyboardState.EXPOSED,
                config.getKeyboardStateQualifier().getValue());
        assertEquals("keysexposed", config.getKeyboardStateQualifier().toString()); //$NON-NLS-1$
    }

    public void testHidden() {
        assertEquals(true, ksq.checkAndSet("keyshidden", config)); //$NON-NLS-1$
        assertTrue(config.getKeyboardStateQualifier() != null);
        assertEquals(KeyboardStateQualifier.KeyboardState.HIDDEN,
                config.getKeyboardStateQualifier().getValue());
        assertEquals("keyshidden", config.getKeyboardStateQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, ksq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, ksq.checkAndSet("KEYSEXPOSED", config));//$NON-NLS-1$
        assertEquals(false, ksq.checkAndSet("other", config));//$NON-NLS-1$
    }
}
