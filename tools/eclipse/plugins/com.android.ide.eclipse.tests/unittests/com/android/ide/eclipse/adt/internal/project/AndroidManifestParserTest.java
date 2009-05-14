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

package com.android.ide.eclipse.adt.internal.project;

import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser;
import com.android.ide.eclipse.adt.internal.project.AndroidManifestParser.Activity;
import com.android.ide.eclipse.tests.AdtTestData;

import junit.framework.TestCase;

/**
 * Tests for {@link AndroidManifestParser}
 */
public class AndroidManifestParserTest extends TestCase {
    private AndroidManifestParser mManifestTestApp;
    private AndroidManifestParser mManifestInstrumentation;
    
    private static final String INSTRUMENTATION_XML = "AndroidManifest-instrumentation.xml";  //$NON-NLS-1$
    private static final String TESTAPP_XML = "AndroidManifest-testapp.xml";  //$NON-NLS-1$
    private static final String PACKAGE_NAME =  "com.android.testapp"; //$NON-NLS-1$
    private static final String ACTIVITY_NAME = "com.android.testapp.MainActivity"; //$NON-NLS-1$
    private static final String LIBRARY_NAME = "android.test.runner"; //$NON-NLS-1$
    private static final String INSTRUMENTATION_NAME = "android.test.InstrumentationTestRunner"; //$NON-NLS-1$
    private static final String INSTRUMENTATION_TARGET = "com.android.AndroidProject"; //$NON-NLS-1$
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        String testFilePath = AdtTestData.getInstance().getTestFilePath(
                TESTAPP_XML);
        mManifestTestApp = AndroidManifestParser.parseForData(testFilePath);
        assertNotNull(mManifestTestApp);
        
        testFilePath = AdtTestData.getInstance().getTestFilePath(
                INSTRUMENTATION_XML);
        mManifestInstrumentation = AndroidManifestParser.parseForData(testFilePath);
        assertNotNull(mManifestInstrumentation);
    }

    public void testGetInstrumentationInformation() {
        assertEquals(1, mManifestInstrumentation.getInstrumentations().length);
        assertEquals(INSTRUMENTATION_NAME, 
                mManifestInstrumentation.getInstrumentations()[0].getName());
        assertEquals(INSTRUMENTATION_TARGET, 
                mManifestInstrumentation.getInstrumentations()[0].getTargetPackage());
    }
    
    public void testGetPackage() {
        assertEquals(PACKAGE_NAME, mManifestTestApp.getPackage());
    }

    public void testGetActivities() {
        assertEquals(1, mManifestTestApp.getActivities().length);
        Activity activity = new AndroidManifestParser.Activity(ACTIVITY_NAME, true);
        activity.setHasAction(true);
        activity.setHasLauncherCategory(true);
        activity.setHasMainAction(true);
        assertEquals(activity, mManifestTestApp.getActivities()[0]);
    }

    public void testGetLauncherActivity() {
        Activity activity = new AndroidManifestParser.Activity(ACTIVITY_NAME, true);
        activity.setHasAction(true);
        activity.setHasLauncherCategory(true);
        activity.setHasMainAction(true);
        assertEquals(activity, mManifestTestApp.getLauncherActivity()); 
    }
    
    private void assertEquals(Activity lhs, Activity rhs) {
        assertTrue(lhs == rhs || (lhs != null && rhs != null));
        if (lhs != null && rhs != null) {
            assertEquals(lhs.getName(),        rhs.getName());
            assertEquals(lhs.isExported(),     rhs.isExported());
            assertEquals(lhs.hasAction(),      rhs.hasAction());
            assertEquals(lhs.isHomeActivity(), rhs.isHomeActivity());
        }
    }

    public void testGetUsesLibraries() {
        assertEquals(1, mManifestTestApp.getUsesLibraries().length);
        assertEquals(LIBRARY_NAME, mManifestTestApp.getUsesLibraries()[0]); 
    }

    public void testGetPackageName() {
        assertEquals(PACKAGE_NAME, mManifestTestApp.getPackage());
    }
}
