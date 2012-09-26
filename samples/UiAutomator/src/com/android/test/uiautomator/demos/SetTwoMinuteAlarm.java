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

import android.widget.TextView;

import com.android.uiautomator.core.UiObject;
import com.android.uiautomator.core.UiObjectNotFoundException;
import com.android.uiautomator.core.UiScrollable;
import com.android.uiautomator.core.UiSelector;
import com.android.uiautomator.testrunner.UiAutomatorTestCase;

/**
 * Test demonstrates using the UiAutomator APIs to set an alarm to
 * go off in 2 minutes
 */
public class SetTwoMinuteAlarm extends UiAutomatorTestCase {

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
     * Set an alarm 2 minutes from now and verify it goes off. Also check the notification
     * shades for an alarm. (Crude test but it demos the use of Android resources)
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
        UiObject clockApp =
                allAppsScreen.getChildByText(LauncherHelper.LAUNCHER_ITEM, "Clock");
        clockApp.click();

        // Set an alarm to go off in about 2 minutes
        setAlarm(2);

        // wait for the alarm alert dialog
        UiObject alarmAlert =
                new UiObject(new UiSelector().packageName("com.google.android.deskclock")
                        .className(TextView.class.getName()).text("Alarm"));

        assertTrue("Timeout while waiting for alarm to go off",
                alarmAlert.waitForExists(2 * 60 * 1000));

        clickByText("Dismiss");
    }

    /**
     * Helper function to set an alarm
     * @param minutesFromNow
     * @throws UiObjectNotFoundException
     */
    private void setAlarm(int minutesFromNow) throws UiObjectNotFoundException {
        UiObject setAlarm = new UiObject(new UiSelector().textStartsWith("Alarm set"));
        if (!setAlarm.exists())
            setAlarm = new UiObject(new UiSelector().textStartsWith("Set alarm"));
        setAlarm.click();

        // let's add an alarm
        clickByDescription("Add alarm");
        // let's set the time
        clickByText("Time");

        // we want the minutes only
        UiSelector minuteAreaSelector = new UiSelector().className(
                android.widget.NumberPicker.class.getName()).instance(1);
        UiSelector minuteIncreaseButtonSelector = minuteAreaSelector.childSelector(
                new UiSelector().className(android.widget.Button.class.getName()).instance(1));

        // increment minutes a couple of times
        for (int x = 0; x < minutesFromNow; x++)
            new UiObject(minuteIncreaseButtonSelector).click();
        clickByText("Done");

        // few confirmations to click thru
        UiObject doneButton = new UiObject(new UiSelector().text("Done"));
        UiObject okButton = new UiObject(new UiSelector().text("OK"));
        // working around some inconsistencies in phone vs tablet UI
        if (doneButton.exists()) {
            doneButton.click();
        } else {
            okButton.click(); // let it fail if neither exists
        }

        // we're done. Let's return to home screen
        clickByText("Done");
        getUiDevice().pressHome();
    }

    /**
     * Helper to click on objects that match the content-description text
     * @param text
     * @throws UiObjectNotFoundException
     */
    private void clickByDescription(String text) throws UiObjectNotFoundException {
        UiObject obj = new UiObject(new UiSelector().description(text));
        obj.clickAndWaitForNewWindow();
    }

    /**
     * Helper to click on object that match the text value
     * @param text
     * @throws UiObjectNotFoundException
     */
    private void clickByText(String text) throws UiObjectNotFoundException {
        UiObject obj = new UiObject(new UiSelector().text(text));
        obj.clickAndWaitForNewWindow();
    }
}