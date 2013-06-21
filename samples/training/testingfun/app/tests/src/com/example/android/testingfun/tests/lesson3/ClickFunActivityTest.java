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
package com.example.android.testingfun.tests.lesson3;


import com.example.android.testingfun.R;
import com.example.android.testingfun.lesson3.ClickFunActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.test.ViewAsserts;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Tests for ClickFunActivity. Introduces touch mode, test size annotations and TouchUtils.
 */
public class ClickFunActivityTest extends ActivityInstrumentationTestCase2<ClickFunActivity> {
    
    private ClickFunActivity mClickFunActivity;
    private Button mClickMeButton;
    private TextView mInfoTextView;

    public ClickFunActivityTest() {
        super(ClickFunActivity.class);
    }

    /**
     * Sets up the test fixture for this test case. This method is always called before every test
     * run.
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        //Sets the initial touch mode for the Activity under test. This must be called before
        //getActivity()
        setActivityInitialTouchMode(true);

        //Get a reference to the Activity under test, starting it if necessary.
        mClickFunActivity = getActivity();

        //Get references to all views
        mClickMeButton = (Button) mClickFunActivity.findViewById(R.id.launch_next_activity_button);
        mInfoTextView = (TextView) mClickFunActivity.findViewById(R.id.info_text_view);
    }

    /**
     * Tests the preconditions of this test fixture.
     */
    @MediumTest
    public void testPreconditions() {
        assertNotNull("mClickFunActivity is null", mClickFunActivity);
        assertNotNull("mClickMeButton is null", mClickMeButton);
        assertNotNull("mInfoTextView is null", mInfoTextView);
    }

    @MediumTest
    public void testClickMeButton_layout() {
        //Retrieve the top-level window decor view
        final View decorView = mClickFunActivity.getWindow().getDecorView();

        //Verify that the mClickMeButton is on screen
        ViewAsserts.assertOnScreen(decorView, mClickMeButton);

        //Verify width and heights
        final ViewGroup.LayoutParams layoutParams = mClickMeButton.getLayoutParams();
        assertNotNull(layoutParams);
        assertEquals(layoutParams.width, WindowManager.LayoutParams.MATCH_PARENT);
        assertEquals(layoutParams.height, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    @MediumTest
    public void testClickMeButton_labelText() {
        //Verify that mClickMeButton uses the correct string resource
        final String expectedNextButtonText = mClickFunActivity.getString(R.string.label_click_me);
        final String actualNextButtonText = mClickMeButton.getText().toString();
        assertEquals(expectedNextButtonText, actualNextButtonText);
    }

    @MediumTest
    public void testInfoTextView_layout() {
        //Retrieve the top-level window decor view
        final View decorView = mClickFunActivity.getWindow().getDecorView();

        //Verify that the mInfoTextView is on screen and is not visible
        ViewAsserts.assertOnScreen(decorView, mInfoTextView);
        assertTrue(View.GONE == mInfoTextView.getVisibility());
    }

    @MediumTest
    public void testInfoTextViewText_isEmpty() {
        //Verify that the mInfoTextView is initialized with the correct default value
        assertEquals("", mInfoTextView.getText());
    }

    @MediumTest
    public void testClickMeButton_clickButtonAndExpectInfoText() {
        String expectedInfoText = mClickFunActivity.getString(R.string.info_text);
        //Perform a click on mClickMeButton
        TouchUtils.clickView(this, mClickMeButton);
        //Verify the that mClickMeButton was clicked. mInfoTextView is visible and contains
        //the correct text.
        assertTrue(View.VISIBLE == mInfoTextView.getVisibility());
        assertEquals(expectedInfoText, mInfoTextView.getText());
    }
}
