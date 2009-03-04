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

public class TextInputMethodQualifierTest extends TestCase {
    
    private TextInputMethodQualifier timq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        timq = new TextInputMethodQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        timq = null;
        config = null;
    }

    public void testQuerty() {
        assertEquals(true, timq.checkAndSet("qwerty", config)); //$NON-NLS-1$
        assertTrue(config.getTextInputMethodQualifier() != null);
        assertEquals(TextInputMethodQualifier.TextInputMethod.QWERTY,
                config.getTextInputMethodQualifier().getValue());
        assertEquals("qwerty", config.getTextInputMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void test12Key() {
        assertEquals(true, timq.checkAndSet("12key", config)); //$NON-NLS-1$
        assertTrue(config.getTextInputMethodQualifier() != null);
        assertEquals(TextInputMethodQualifier.TextInputMethod.TWELVEKEYS,
                config.getTextInputMethodQualifier().getValue());
        assertEquals("12key", config.getTextInputMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void testNoKey() {
        assertEquals(true, timq.checkAndSet("nokeys", config)); //$NON-NLS-1$
        assertTrue(config.getTextInputMethodQualifier() != null);
        assertEquals(TextInputMethodQualifier.TextInputMethod.NOKEY,
                config.getTextInputMethodQualifier().getValue());
        assertEquals("nokeys", config.getTextInputMethodQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, timq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, timq.checkAndSet("QWERTY", config));//$NON-NLS-1$
        assertEquals(false, timq.checkAndSet("12keys", config));//$NON-NLS-1$
        assertEquals(false, timq.checkAndSet("*12key", config));//$NON-NLS-1$
        assertEquals(false, timq.checkAndSet("other", config));//$NON-NLS-1$
    }
}
