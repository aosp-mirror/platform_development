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

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * This demos how we read content-description to properly open the
 * All Apps view and select and application to launch. Then we will
 * use the package name to verify that the current window is actually
 * from the expected package
 */
public class LaunchSettings extends UiAutomatorTestCase {

    public void testDemo() throws UiObjectNotFoundException {
        // Good practice to start from a common place
        getUiDevice().pressHome();

        // When we use the uiautomatorviewer in the DSK/tools we find that this
        // button has the following content-description
        UiObject allAppsButton = new UiObject(new UiSelector().description("Apps"));

        // ** NOTE **
        // Any operation on a UiObject that is not currently present on the display
        // will result in a UiObjectNotFoundException to be thrown. If we want to
        // first check if the object exists we can use allAppsButton.exists() which
        // return boolean.
        // ** NOTE **

        //The operation below expects the click will result a new  window.
        allAppsButton.clickAndWaitForNewWindow();

        // On the this view, we expect two tabs, one for APPS and another for
        // WIDGETS. We will want to click the APPS just so we're sure apps and
        // not widgets is our current display
        UiObject appsTab = new UiObject(new UiSelector().text("Apps"));

        // ** NOTE **
        // The above operation assumes that there is only one text "Apps" in the
        // current view. If not, then the first "Apps" is selected. But if we
        // want to be certain that we click on the tab "Apps", we can use the
        // uiautomatorview from the SDK/tools and see if we can further narrow the
        // selector. Comment the line above and uncomment the one bellow to use
        // the more specific selector.
        // ** NOTE **

        // This creates a selector hierarchy where the first selector is for the
        // TabWidget and the second will search only inside the layout of TabWidget
        // To use this instead, uncomment the lines bellow and comment the above appsTab
        //UiSelector appsTabSelector =
        //        new UiSelector().className(android.widget.TabWidget.class.getName())
        //            .childSelector(new UiSelector().text("Apps"));
        //UiObject appsTab = new UiObject(appsTabSelector);


        // The operation below we only cause a content change so a click() is good
        appsTab.click();

        // Since our device may have many apps on it spanning multiple views, we
        // may need to scroll to find our app. Here we use UiScrollable to help.
        // We declare the object with a selector to a scrollable view. Since in this
        // case the firt scrollable view happens to be the one containing our apps
        // list, we should be ok.
        UiScrollable appViews = new UiScrollable(new UiSelector().scrollable(true));
        // swipe horizontally when searching (default is vertical)
        appViews.setAsHorizontalList();

        // the appsViews will perform horizontal scrolls to find the Settings app
        UiObject settingsApp = appViews.getChildByText(
                new UiSelector().className(android.widget.TextView.class.getName()), "Settings");
        settingsApp.clickAndWaitForNewWindow();

        // create a selector for anything on the display and check if the package name
        // is the expected one
        UiObject settingsValidation =
                new UiObject(new UiSelector().packageName("com.android.settings"));

        assertTrue("Unable to detect Settings", settingsValidation.exists());
    }
}
