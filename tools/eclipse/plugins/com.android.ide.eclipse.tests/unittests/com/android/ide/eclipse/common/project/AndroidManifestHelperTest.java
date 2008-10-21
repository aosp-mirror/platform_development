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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

public class AndroidManifestHelperTest extends TestCase {
    private File mFile;
    private AndroidManifestHelper mManifest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mFile = File.createTempFile("androidManifest", "xml");  //$NON-NLS-1$ //$NON-NLS-2$
        assertNotNull(mFile);

        FileWriter fw = new FileWriter(mFile);
        fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");  //$NON-NLS-1$
        fw.write("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n");  //$NON-NLS-1$
        fw.write("          package=\"com.android.testapp\">\n");  //$NON-NLS-1$
        fw.write("  <application android:icon=\"@drawable/icon\">\n");  //$NON-NLS-1$
        fw.write("    <activity android:name=\".MainActivity\" android:label=\"@string/app_name\">\n");  //$NON-NLS-1$
        fw.write("      <intent-filter>\n");  //$NON-NLS-1$
        fw.write("        <action android:name=\"android.intent.action.MAIN\" />\n");  //$NON-NLS-1$
        fw.write("          <category android:name=\"android.intent.category.LAUNCHER\" />\"\n");  //$NON-NLS-1$
        fw.write("          <category android:name=\"android.intent.category.DEFAULT\" />\n");  //$NON-NLS-1$
        fw.write("      </intent-filter>\n");  //$NON-NLS-1$
        fw.write("    </activity>\n");  //$NON-NLS-1$
        fw.write("    <activity android:name=\".OptionsActivity\" android:label=\"@string/options\"\n");  //$NON-NLS-1$
        fw.write("              android:theme=\"@style/Theme.Floating\">\n");  //$NON-NLS-1$
        fw.write("      <intent-filter>\n");  //$NON-NLS-1$
        fw.write("        <action android:name=\"com.android.mandelbrot.action.EDIT_OPTIONS\" />\n");  //$NON-NLS-1$
        fw.write("        <category android:name=\"android.intent.category.PREFERENCE_CATEGORY\" />\n");  //$NON-NLS-1$
        fw.write("      </intent-filter>\n");  //$NON-NLS-1$
        fw.write("    </activity>\n");  //$NON-NLS-1$
        fw.write("    <activity android:name=\".InfoActivity\" android:label=\"@string/options\"\n");  //$NON-NLS-1$
        fw.write("             android:theme=\"@style/Theme.Floating\">\n");  //$NON-NLS-1$
        fw.write("      <intent-filter>\n");  //$NON-NLS-1$
        fw.write("        <action android:name=\"com.android.mandelbrot.action.DISPLAY_INFO\" />\n");  //$NON-NLS-1$
        fw.write("      </intent-filter>\n");  //$NON-NLS-1$
        fw.write("    </activity>\n");  //$NON-NLS-1$
        fw.write("  </application>\n");  //$NON-NLS-1$
        fw.write("</manifest>\n");  //$NON-NLS-1$
        fw.flush();
        fw.close();

        mManifest = new AndroidManifestHelper(mFile.getAbsolutePath());
    }

    @Override
    protected void tearDown() throws Exception {
        assertTrue(mFile.delete());
        super.tearDown();
    }

    public void testExists() {
        assertTrue(mManifest.exists());
    }

    public void testNotExists() throws IOException {
        File f = File.createTempFile("androidManifest2", "xml");  //$NON-NLS-1$ //$NON-NLS-2$
        assertTrue(f.delete());
        AndroidManifestHelper manifest = new AndroidManifestHelper(f.getAbsolutePath());
        assertFalse(manifest.exists());
    }

    public void testGetPackageName() {
        assertEquals("com.android.testapp", mManifest.getPackageName());
    }

    public void testGetActivityName() {
        assertEquals("", mManifest.getActivityName(0));  //$NON-NLS-1$
        assertEquals(".MainActivity", mManifest.getActivityName(1));  //$NON-NLS-1$
        assertEquals(".OptionsActivity", mManifest.getActivityName(2));  //$NON-NLS-1$
        assertEquals(".InfoActivity", mManifest.getActivityName(3));  //$NON-NLS-1$
        assertEquals("", mManifest.getActivityName(4));  //$NON-NLS-1$
    }

}
