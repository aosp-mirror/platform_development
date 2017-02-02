/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.checkcolor.lint;

import com.android.annotations.NonNull;
import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HardcodedColorDetectorTest extends LintDetectorTest {

    private Set<Issue> mEnabled = new HashSet<Issue>();

    @Override
    protected Detector getDetector() {
        return new HardcodedColorDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(HardcodedColorDetector.ISSUE);
    }

    @Override
    protected TestConfiguration getConfiguration(LintClient client, Project project) {
        return new TestConfiguration(client, project, null) {
            @Override
            public boolean isEnabled(@NonNull Issue issue) {
                return super.isEnabled(issue) && mEnabled.contains(issue);
            }
        };
    }

    public void testHardcodeColorInSelector() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/layout/selector.xml:4: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        android:color=\"#ffffff\" />\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "1 errors, 0 warnings\n";
        String result = lintProject(xml("res/layout/selector.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <item android:state_checked=\"true\"\n" +
                "        android:color=\"#ffffff\" />\n" +
                "    <item android:color=\"@android:color/black\"\n" +
                "        android:alpha=\".54\" />\n" +
                "</selector>"));
        assertEquals(expected, result);
    }

    public void testHardcodeColorInLayout() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/layout/layout_battery.xml:13: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        android:textColor=\"#ff0\" />\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "res/layout/layout_battery.xml:25: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        systemui:textColor=\"#66FFFFFF\" />\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "2 errors, 0 warnings\n";
        String result = lintProject(xml("res/layout/layout_battery.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:systemui=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"wrap_content\"\n" +
                "    android:orientation=\"vertical\">\n" +
                "\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/charge_and_estimation\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:textColor=\"#ff0\" />\n" +
                "\n" +
                "    <com.android.systemui.ResizingSpace\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"@dimen/battery_detail_graph_space_top\" />\n" +
                "\n" +
                "    <com.android.settingslib.graph.UsageView\n" +
                "        android:id=\"@+id/battery_usage\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"141dp\"\n" +
                "        systemui:sideLabels=\"@array/battery_labels\"\n" +
                "        android:colorAccent=\"?android:attr/colorAccent\"\n" +
                "        systemui:textColor=\"#66FFFFFF\" />\n" +
                "</LinearLayout>"));
        assertEquals(expected, result);
    }

    public void testHardcodeColorInStyle() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/values/pxsp.xml:6: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        <item name=\"android:textColor\">#b2ffffff</item>\n" +
                "                                       ^\n" +
                "res/values/pxsp.xml:7: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        <item name=\"android:background\">#b2ffffff</item>\n" +
                "                                        ^\n" +
                "2 errors, 0 warnings\n";
        String result = lintProject(xml("res/values/pxsp.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <style name=\"TextAppearance.StatusBar.Expanded.Date\">\n" +
                "        <item name=\"android:textSize\">@dimen/qs_date_collapsed_size</item>\n" +
                "        <item name=\"android:textStyle\">normal</item>\n" +
                "        <item name=\"android:textColor\">#b2ffffff</item>\n" +
                "        <item name=\"android:background\">#b2ffffff</item>\n" +
                "    </style>\n" +
                "</resources>"));
        assertEquals(expected, result);
    }

    public void testIndirectedColor() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/values/colors.xml:3: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "    <color name=\"color_red\">#ffffff</color>\n" +
                "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "res/layout/layout_battery.xml:13: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        android:textColor=\"@color/color_red\" />\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "res/layout/layout_battery.xml:19: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        systemui:textColor=\"#b2ffffff\" />\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "3 errors, 0 warnings\n";
        String result = lintProject(xml("res/layout/layout_battery.xml", "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:systemui=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"wrap_content\"\n" +
                "    android:orientation=\"vertical\">\n" +
                "\n" +
                "    <TextView\n" +
                "        android:id=\"@+id/charge_and_estimation\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:textColor=\"@color/color_red\" />\n" +
                "\n" +
                "    <com.android.settingslib.graph.UsageView\n" +
                "        android:id=\"@+id/battery_usage\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"141dp\"\n" +
                "        systemui:textColor=\"#b2ffffff\" />\n" +
                "</LinearLayout>"),
                xml("res/values/colors.xml","<resources>\n" +
                        "    <!-- Keyboard shortcuts colors -->\n" +
                        "    <color name=\"color_red\">#ffffff</color>\n" +
                        "    <color name=\"ksh_key_item_background\">@color/material_grey_100</color>\n" +
                        "</resources>\n"));
        assertEquals(expected, result);
    }

    public void testHardcodeColorInColorElement() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/values/colors.xml:3: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "    <color name=\"color_red\">#ffffff</color>\n" +
                "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                "1 errors, 0 warnings\n";
        String result = lintProject(xml("res/values/colors.xml", "<resources>\n" +
                "    <!-- Keyboard shortcuts colors -->\n" +
                "    <color name=\"color_red\">#ffffff</color>\n" +
                "    <color name=\"ksh_key_item_background\">@color/material_grey_100</color>\n" +
                "</resources>\n"));
        assertEquals(expected, result);
    }

    public void testHardcodeColorInVectorDrawable() throws Exception {
        mEnabled = Collections.singleton(HardcodedColorDetector.ISSUE);
        String expected = "res/values/colors.xml:2: Error: Using hardcoded colors is not allowed [HardcodedColor]\n" +
                "        android:tint=\"#FFFFFF\">\n" +
                "        ~~~~~~~~~~~~~~~~~~~~~~\n" +
                "1 errors, 0 warnings\n";
        String result = lintProject(xml("res/values/colors.xml", "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "        android:tint=\"#FFFFFF\">\n" +
                "    <path\n" +
                "        android:fillColor=\"#FFFFFF\"/>\n" +
                "</vector>"));
        assertEquals(expected, result);
    }
}
