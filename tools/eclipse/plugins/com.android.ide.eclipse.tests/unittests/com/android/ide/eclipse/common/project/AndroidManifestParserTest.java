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

package com.android.ide.eclipse.common.project;

import junit.framework.TestCase;

import com.android.ide.eclipse.mock.FileMock;

/**
 * Tests for {@link AndroidManifestParser}
 */
public class AndroidManifestParserTest extends TestCase {
    private AndroidManifestParser mManifest;
    
    private static final String PACKAGE_NAME = "com.android.testapp"; //$NON-NLS-1$
    private static final String ACTIVITY_NAME = "com.android.testapp.MainActivity"; //$NON-NLS-1$
    private static final String LIBRARY_NAME = "android.test.runner"; //$NON-NLS-1$
    private static final String INSTRUMENTATION_NAME = "android.test.InstrumentationTestRunner"; //$NON-NLS-1$
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        // create the test data
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");  //$NON-NLS-1$
        sb.append("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");  //$NON-NLS-1$
        sb.append("          package=\""); //$NON-NLS-1$
        sb.append(PACKAGE_NAME);
        sb.append("\">\n");  //$NON-NLS-1$
        sb.append("  <application android:icon=\"@drawable/icon\">\n");  //$NON-NLS-1$
        sb.append("    <activity android:name=\"");  //$NON-NLS-1$
        sb.append(ACTIVITY_NAME);
        sb.append("\" android:label=\"@string/app_name\">\n");  //$NON-NLS-1$
        sb.append("      <intent-filter>\n");  //$NON-NLS-1$
        sb.append("        <action android:name=\"android.intent.action.MAIN\" />\n");  //$NON-NLS-1$
        sb.append("          <category android:name=\"android.intent.category.LAUNCHER\" />\"\n");  //$NON-NLS-1$
        sb.append("          <category android:name=\"android.intent.category.DEFAULT\" />\n");  //$NON-NLS-1$
        sb.append("      </intent-filter>\n");  //$NON-NLS-1$
        sb.append("    </activity>\n");  //$NON-NLS-1$
        sb.append("    <uses-library android:name=\""); //$NON-NLS-1$
        sb.append(LIBRARY_NAME);
        sb.append("\" />\n");  //$NON-NLS-1$
        sb.append("  </application>"); //$NON-NLS-1$
        sb.append("  <instrumentation android:name=\""); //$NON-NLS-1$
        sb.append(INSTRUMENTATION_NAME);
        sb.append("\"\n");
        sb.append("                   android:targetPackage=\"com.example.android.apis\"\n");
        sb.append("                   android:label=\"Tests for Api Demos.\"/>\n");
        sb.append("</manifest>\n");  //$NON-NLS-1$

        FileMock mockFile = new FileMock("AndroidManifest.xml", sb.toString().getBytes());
        
        mManifest = AndroidManifestParser.parseForData(mockFile);
        assertNotNull(mManifest);
    }

    public void testGetPackage() {
        assertEquals("com.android.testapp", mManifest.getPackage());
    }

    public void testGetActivities() {
        assertEquals(1, mManifest.getActivities().length);
        assertEquals(ACTIVITY_NAME, mManifest.getActivities()[0]); 
    }

    public void testGetLauncherActivity() {
        assertEquals(ACTIVITY_NAME, mManifest.getLauncherActivity()); 
    }
    
    public void testGetUsesLibraries() {
        assertEquals(1, mManifest.getUsesLibraries().length);
        assertEquals(LIBRARY_NAME, mManifest.getUsesLibraries()[0]); 
    }
    
    public void testGetInstrumentations() {
        assertEquals(1, mManifest.getInstrumentations().length);
        assertEquals(INSTRUMENTATION_NAME, mManifest.getInstrumentations()[0]); 
    }
}
