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

package com.android.ide.eclipse.adt.internal.editors.resources.configurations;

import com.android.ide.eclipse.adt.internal.resources.configurations.CountryCodeQualifier;
import com.android.ide.eclipse.adt.internal.resources.configurations.FolderConfiguration;

import junit.framework.TestCase;

public class CountryCodeQualifierTest extends TestCase {

    private CountryCodeQualifier mccq;
    private FolderConfiguration config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mccq = new CountryCodeQualifier();
        config = new FolderConfiguration();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mccq = null;
        config = null;
    }

    public void testCheckAndSet() {
        assertEquals(true, mccq.checkAndSet("mcc123", config));//$NON-NLS-1$
        assertTrue(config.getCountryCodeQualifier() != null);
        assertEquals(123, config.getCountryCodeQualifier().getCode());
        assertEquals("mcc123", config.getCountryCodeQualifier().toString()); //$NON-NLS-1$
    }

    public void testFailures() {
        assertEquals(false, mccq.checkAndSet("", config));//$NON-NLS-1$
        assertEquals(false, mccq.checkAndSet("mcc", config));//$NON-NLS-1$
        assertEquals(false, mccq.checkAndSet("MCC123", config));//$NON-NLS-1$
        assertEquals(false, mccq.checkAndSet("123", config));//$NON-NLS-1$
        assertEquals(false, mccq.checkAndSet("mccsdf", config));//$NON-NLS-1$
    }

}
