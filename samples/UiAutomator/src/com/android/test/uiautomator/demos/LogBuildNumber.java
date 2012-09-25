/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.test.uiautomator.demos;

import android.util.Log;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * This demos how we can scroll list views and verify data in list view
 * items. Here we do the following:
 * <ul>
 * <li> Launch Settings </li>
 * <li> Select the About </li>
 * <li> Read the Build string </li>
 * </ul>
 */
public class LogBuildNumber extends UiAutomatorTestCase {
    public static final String LOG_TAG = LogBuildNumber.class.getSimpleName();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * For the purpose of this demo, we're declaring the Launcher signatures here.
     * It may be more appropriate to declare signatures and methods related
     * to Launcher in their own reusable Launcher helper file.
     */
    public static class LauncherHelper {
        public static final UiSelector ALL_APPS_BUTTON = new UiSelector().description("Apps");
        public static final UiSelector LAUNCHER_CONTAINER = new UiSelector().scrollable(true);
        public static final UiSelector LAUNCHER_ITEM =
                new UiSelector().className(android.widget.TextView.class.getName());
    }

    /**
     * For the purpose of this demo, we're declaring the Settings signatures here.
     * It may be more appropriate to declare signatures and methods related
     * to Settings in their own reusable Settings helper file.
     */
    public static class SettingsHelper {
        public static final UiSelector LIST_VIEW =
                new UiSelector().className(android.widget.ListView.class.getName());
        public static final UiSelector LIST_VIEW_ITEM =
                new UiSelector().className(android.widget.LinearLayout.class.getName());
    }

    /**
     * Script starts here
     * @throws UiObjectNotFoundException
     */
    public void testDemo() throws UiObjectNotFoundException {
        // The following code is documented in the LaunchSettings demo. For detailed description
        // of how this code works, look at the demo LaunchSettings

        // Good to start from here
        getUiDevice().pressHome();

        // open the All Apps view
        UiObject allAppsButton = new UiObject(LauncherHelper.ALL_APPS_BUTTON);
        allAppsButton.click();

        // clicking the APPS tab
        UiSelector appsTabSelector =
                new UiSelector().className(android.widget.TabWidget.class.getName())
                    .childSelector(new UiSelector().text("Apps"));
        UiObject appsTab = new UiObject(appsTabSelector);
        appsTab.click();

        // Clicking the Settings
        UiScrollable allAppsScreen = new UiScrollable(LauncherHelper.LAUNCHER_CONTAINER);
        allAppsScreen.setAsHorizontalList();
        UiObject settingsApp =
                allAppsScreen.getChildByText(LauncherHelper.LAUNCHER_ITEM, "Settings");
        settingsApp.click();

        // Now we will select the settings we need to work with. To make this operation a little
        // more generic we will put it in a function. We will try it as a phone first then as a
        // tablet if phone is not our device type
        if (!selectSettingsFor("About phone"))
            selectSettingsFor("About tablet");

        // Now we need to read the Build number text and return it
        String buildNum = getAboutItem("Build number");

        // Log it - Use adb logcat to view the results
        Log.i(LOG_TAG, "Build = " + buildNum);
    }

    /**
     * Select a settings items and perform scroll if needed to find it.
     * @param name
     */
    private boolean selectSettingsFor(String name)  {
        try {
            UiScrollable appsSettingsList = new UiScrollable(SettingsHelper.LIST_VIEW);
            UiObject obj = appsSettingsList.getChildByText(SettingsHelper.LIST_VIEW_ITEM, name);
            obj.click();
        } catch (UiObjectNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * This function will detect the presence of 2 or 1 list view display fragments and
     * targets the correct list view for the About item details
     * @param item
     * @return the details string of an about item entry
     * @throws UiObjectNotFoundException
     */
    private String getAboutItem(String item) throws UiObjectNotFoundException {
        // try accessing the second list view if one exists else we will assume the
        // device is displaying a single list view
        UiScrollable aboutSettingsList = new UiScrollable(SettingsHelper.LIST_VIEW.instance(1));
        if (!aboutSettingsList.exists())
            aboutSettingsList = new UiScrollable(SettingsHelper.LIST_VIEW.instance(0));

        // the returned aboutItem will be pointing at the node matching the
        // SettingsOsr.LIST_VIEW_ITEM. So, aboutItem is a container of widgets where one
        // actually contains the text (item) we're looking for.
        UiObject aboutItem = aboutSettingsList.getChildByText(SettingsHelper.LIST_VIEW_ITEM, item);

        // Since aboutItem contains the text widgets for the requested details, we're assuming
        // here that the param 'item' refers to the label and the second text is the value for it.
        UiObject txt = aboutItem.getChild(
                new UiSelector().className(android.widget.TextView.class.getName()).instance(1));

        // This is interesting. Since aboutItem is returned pointing a the layout containing the
        // test values, we know it is visible else an exception would've been thrown. However,
        // we're not certain that the instance(1) or second text view inside this layout is
        // in fact fully visible and not off the screen.
        if (!txt.exists())
            aboutSettingsList.scrollForward(); // scroll it into view

        return txt.getText();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
