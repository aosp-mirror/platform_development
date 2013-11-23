/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.testingfun.tests.lesson4;

import com.example.android.testingfun.R;
import com.example.android.testingfun.lesson4.LaunchActivity;
import com.example.android.testingfun.lesson4.NextActivity;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

/**
 * Tests LaunchActivity in isolation from the system.
 */
public class LaunchActivityTest extends ActivityUnitTestCase<LaunchActivity> {

    private Intent mLaunchIntent;

    public LaunchActivityTest() {
        super(LaunchActivity.class);
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //Create an intent to launch target Activity
        mLaunchIntent = new Intent(getInstrumentation().getTargetContext(),
                LaunchActivity.class);
    }

    /**
     * Tests the preconditions of this test fixture.
     */
    @MediumTest
    public void testPreconditions() {
        //Start the activity under test in isolation, without values for savedInstanceState and
        //lastNonConfigurationInstance
        startActivity(mLaunchIntent, null, null);
        final Button launchNextButton = (Button) getActivity().findViewById(R.id.launch_next_activity_button);

        assertNotNull("mLaunchActivity is null", getActivity());
        assertNotNull("mLaunchNextButton is null", launchNextButton);
    }


    @MediumTest
    public void testLaunchNextActivityButton_labelText() {
        startActivity(mLaunchIntent, null, null);
        final Button launchNextButton = (Button) getActivity().findViewById(R.id.launch_next_activity_button);

        final String expectedButtonText = getActivity().getString(R.string.label_launch_next);
        assertEquals("Unexpected button label text", expectedButtonText,
                launchNextButton.getText());
    }

    @MediumTest
    public void testNextActivityWasLaunchedWithIntent() {
        startActivity(mLaunchIntent, null, null);
        final Button launchNextButton = (Button) getActivity().findViewById(R.id.launch_next_activity_button);
        //Because this is an isolated ActivityUnitTestCase we have to directly click the
        //button from code
        launchNextButton.performClick();

        // Get the intent for the next started activity
        final Intent launchIntent = getStartedActivityIntent();
        //Verify the intent was not null.
        assertNotNull("Intent was null", launchIntent);
        //Verify that LaunchActivity was finished after button click
        assertTrue(isFinishCalled());


        final String payload = launchIntent.getStringExtra(NextActivity.EXTRAS_PAYLOAD_KEY);
        //Verify that payload data was added to the intent
        assertEquals("Payload is empty", LaunchActivity.STRING_PAYLOAD
                , payload);
    }
}