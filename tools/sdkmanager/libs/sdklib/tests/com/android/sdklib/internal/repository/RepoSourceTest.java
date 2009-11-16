/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

import com.android.sdklib.repository.SdkRepository;

import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

/**
 * Tests for {@link RepoSource}
 */
public class RepoSourceTest extends TestCase {

    private static class MockMonitor implements ITaskMonitor {
        public void setResult(String resultFormat, Object... args) {
        }

        public void setProgressMax(int max) {
        }

        public void setDescription(String descriptionFormat, Object... args) {
        }

        public boolean isCancelRequested() {
            return false;
        }

        public void incProgress(int delta) {
        }

        public int getProgress() {
            return 0;
        }

        public boolean displayPrompt(String title, String message) {
            return false;
        }

        public ITaskMonitor createSubMonitor(int tickCount) {
            return null;
        }
    }

    /**
     * An internal helper class to give us visibility to the protected members we want
     * to test.
     */
    private static class MockRepoSource extends RepoSource {
        public MockRepoSource() {
            super("fake-url", false /*userSource*/);
        }

        public Document _findAlternateToolsXml(InputStream xml) {
            return super.findAlternateToolsXml(xml);
        }

        public boolean _parsePackages(Document doc, String nsUri, ITaskMonitor monitor) {
            return super.parsePackages(doc, nsUri, monitor);
        }
    }

    private MockRepoSource mSource;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mSource = new MockRepoSource();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        mSource = null;
    }

    public void testFindAlternateToolsXml_Errors() {
        // Support null as input
        Document result = mSource._findAlternateToolsXml(null);
        assertNull(result);

        // Support an empty input
        String str = "";
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes());
        result = mSource._findAlternateToolsXml(input);
        assertNull(result);

        // Support a random string as input
        str = "Some random string, not even HTML nor XML";
        input = new ByteArrayInputStream(str.getBytes());
        result = mSource._findAlternateToolsXml(input);
        assertNull(result);

        // Support an HTML input, e.g. a typical 404 document as returned by DL
        str = "<html><head> " +
        "<meta http-equiv=\"content-type\" content=\"text/html;charset=utf-8\"> " +
        "<title>404 Not Found</title> " + "<style><!--" + "body {font-family: arial,sans-serif}" +
        "div.nav { ... blah blah more css here ... color: green}" +
        "//--></style> " + "<script><!--" + "var rc=404;" + "//-->" + "</script> " + "</head> " +
        "<body text=#000000 bgcolor=#ffffff> " +
        "<table border=0 cellpadding=2 cellspacing=0 width=100%><tr><td rowspan=3 width=1% nowrap> " +
        "<b><font face=times color=#0039b6 size=10>G</font><font face=times color=#c41200 size=10>o</font><font face=times color=#f3c518 size=10>o</font><font face=times color=#0039b6 size=10>g</font><font face=times color=#30a72f size=10>l</font><font face=times color=#c41200 size=10>e</font>&nbsp;&nbsp;</b> " +
        "<td>&nbsp;</td></tr> " +
        "<tr><td bgcolor=\"#3366cc\"><font face=arial,sans-serif color=\"#ffffff\"><b>Error</b></td></tr> " +
        "<tr><td>&nbsp;</td></tr></table> " + "<blockquote> " + "<H1>Not Found</H1> " +
        "The requested URL <code>/404</code> was not found on this server." + " " + "<p> " +
        "</blockquote> " +
        "<table width=100% cellpadding=0 cellspacing=0><tr><td bgcolor=\"#3366cc\"><img alt=\"\" width=1 height=4></td></tr></table> " +
        "</body></html> ";
        input = new ByteArrayInputStream(str.getBytes());
        result = mSource._findAlternateToolsXml(input);
        assertNull(result);

        // Support some random XML document, totally unrelated to our sdk-repository schema
        str = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
        "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"" +
        "    package=\"some.cool.app\" android:versionName=\"1.6.04\" android:versionCode=\"1604\">" +
        "    <application android:label=\"@string/app_name\" android:icon=\"@drawable/icon\"/>" +
        "</manifest>";
        input = new ByteArrayInputStream(str.getBytes());
        result = mSource._findAlternateToolsXml(input);
        assertNull(result);
    }

    public void testFindAlternateToolsXml_1() {
        InputStream xmlStream = this.getClass().getResourceAsStream(
                    "/com/android/sdklib/testdata/repository_sample_1.xml");

        Document result = mSource._findAlternateToolsXml(xmlStream);
        assertNotNull(result);
        assertTrue(mSource._parsePackages(result,
                SdkRepository.NS_SDK_REPOSITORY, new MockMonitor()));

        // check the packages we found... we expected to find 2 tool packages with 1 archive each.
        Package[] pkgs = mSource.getPackages();
        assertEquals(2, pkgs.length);
        for (Package p : pkgs) {
            assertEquals(ToolPackage.class, p.getClass());
            assertEquals(1, p.getArchives().length);
        }
    }

}
