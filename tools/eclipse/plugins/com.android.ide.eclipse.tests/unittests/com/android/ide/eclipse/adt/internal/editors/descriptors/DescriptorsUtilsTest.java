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


package com.android.ide.eclipse.adt.internal.editors.descriptors;

import com.android.ide.eclipse.adt.internal.editors.descriptors.DescriptorsUtils;
import com.android.ide.eclipse.adt.internal.editors.descriptors.ElementDescriptor;

import junit.framework.TestCase;

/**
 * Unit tests for DescriptorsUtils in the editors plugin
 */
public class DescriptorsUtilsTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPrettyAttributeUiName() {
        assertEquals("", DescriptorsUtils.prettyAttributeUiName(""));

        assertEquals("Max width for view",
                DescriptorsUtils.prettyAttributeUiName("maxWidthForView"));

        assertEquals("Layout width",
                DescriptorsUtils.prettyAttributeUiName("layout_width"));

        // X Y and Z are capitalized when used as single words (so "T" becomes "t")
        assertEquals("Axis X", DescriptorsUtils.prettyAttributeUiName("axisX"));
        assertEquals("Axis Y", DescriptorsUtils.prettyAttributeUiName("axisY"));
        assertEquals("Axis Z", DescriptorsUtils.prettyAttributeUiName("axisZ"));
        assertEquals("Axis t", DescriptorsUtils.prettyAttributeUiName("axisT"));

        assertEquals("The X axis", DescriptorsUtils.prettyAttributeUiName("theXAxis"));
        assertEquals("The Y axis", DescriptorsUtils.prettyAttributeUiName("theYAxis"));
        assertEquals("The Z axis", DescriptorsUtils.prettyAttributeUiName("theZAxis"));
        assertEquals("The t axis", DescriptorsUtils.prettyAttributeUiName("theTAxis"));
    }

    public void testCapitalize() {
        assertEquals("UPPER", DescriptorsUtils.capitalize("UPPER"));
        assertEquals("Lower", DescriptorsUtils.capitalize("lower"));
        assertEquals("Capital", DescriptorsUtils.capitalize("Capital"));
        assertEquals("CamelCase", DescriptorsUtils.capitalize("camelCase"));
        assertEquals("", DescriptorsUtils.capitalize(""));
    }

    public void testFormatTooltip() {
        assertEquals("", DescriptorsUtils.formatTooltip(""));

        assertEquals("\"application\"",
                DescriptorsUtils.formatTooltip(
                        "<code>application</code>"));

        assertEquals("android.content.Intent",
                DescriptorsUtils.formatTooltip(
                        "{@link android.content.Intent}"));
        
        assertEquals("FLAG_ACTIVITY_SINGLE_TOP",
                DescriptorsUtils.formatTooltip(
                        "{@link android.content.Intent#FLAG_ACTIVITY_SINGLE_TOP}"));
        
        assertEquals("activity-alias",
                DescriptorsUtils.formatTooltip(
                        "{@link \t  #AndroidManifestActivityAlias  \tactivity-alias }"));
        
        assertEquals("\"permission\"",
                DescriptorsUtils.formatTooltip(
                        "{@link #AndroidManifestPermission &lt;permission&gt;}"));
        
        assertEquals("and etc.",
                DescriptorsUtils.formatTooltip(
                        "{@link #IntentCategory <category> and etc. }"));
        
        assertEquals("Activity.onNewIntent()",
                DescriptorsUtils.formatTooltip(
                        "{@link android.app.Activity#onNewIntent Activity.onNewIntent()}"));
    }

    public void testFormatFormText() {
        ElementDescriptor desc = new ElementDescriptor("application");
        desc.setSdkUrl(DescriptorsUtils.MANIFEST_SDK_URL + "TagApplication");
        String docBaseUrl = "http://base";
        assertEquals("<form><li style=\"image\" value=\"image\"></li></form>", DescriptorsUtils.formatFormText("", desc, docBaseUrl));

        assertEquals("<form><li style=\"image\" value=\"image\"><a href=\"http://base/reference/android/R.styleable.html#TagApplication\">application</a></li></form>",
                DescriptorsUtils.formatFormText(
                        "<code>application</code>",
                        desc, docBaseUrl));

        assertEquals("<form><li style=\"image\" value=\"image\"><b>android.content.Intent</b></li></form>",
                DescriptorsUtils.formatFormText(
                        "{@link android.content.Intent}",
                        desc, docBaseUrl));

        assertEquals("<form><li style=\"image\" value=\"image\"><a href=\"http://base/reference/android/R.styleable.html#AndroidManifestPermission\">AndroidManifestPermission</a></li></form>",
                DescriptorsUtils.formatFormText(
                        "{@link #AndroidManifestPermission}",
                        desc, docBaseUrl));

        assertEquals("<form><li style=\"image\" value=\"image\"><a href=\"http://base/reference/android/R.styleable.html#AndroidManifestPermission\">\"permission\"</a></li></form>",
                DescriptorsUtils.formatFormText(
                        "{@link #AndroidManifestPermission &lt;permission&gt;}",
                        desc, docBaseUrl));
    }
}
