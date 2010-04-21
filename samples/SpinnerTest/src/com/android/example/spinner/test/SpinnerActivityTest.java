/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.example.spinner.test;

import com.android.example.spinner.SpinnerActivity;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.view.KeyEvent;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

/*
 * Tests the example application Spinner. Uses the instrumentation test class
 * ActivityInstrumentationTestCase2 as its base class. The tests include
 *   - test initial conditions
 *   - test the UI
 *   - state management - preserving state after the app is shut down and restarted, preserving
 *     state after the app is hidden (paused) and re-displayed (resumed)
 *
 * Demonstrates the use of JUnit setUp() and assert() methods.
 */
public class SpinnerActivityTest extends ActivityInstrumentationTestCase2<SpinnerActivity> {

    // Number of items in the spinner's backing mLocalAdapter

    public static final int ADAPTER_COUNT = 9;

    // The location of Saturn in the backing mLocalAdapter array (0-based)

    public static final int TEST_POSITION = 5;

    // Set the initial position of the spinner to zero

    public static final int INITIAL_POSITION = 0;

    // The initial position corresponds to Mercury

    public static final String INITIAL_SELECTION = "Mercury";

    // Test values of position and selection for the testStateDestroy test

    public static final int TEST_STATE_DESTROY_POSITION = 2;
    public static final String TEST_STATE_DESTROY_SELECTION = "Earth";

    // Test values of position and selection for the testStatePause test

    public static final int TEST_STATE_PAUSE_POSITION = 4;
    public static final String TEST_STATE_PAUSE_SELECTION = "Jupiter";

    // The Application object for the application under test

    private SpinnerActivity mActivity;

    // String displayed in the spinner in the app under test

    private String mSelection;

    // The currently selected position in the spinner in the app under test

    private int mPos;

    /*
     *  The Spinner object in the app under test. Used with instrumentation to control the
     *  app under test.
     */

    private Spinner mSpinner;

    /*
     * The data backing the Spinner in the app under test.
     */

    private SpinnerAdapter mPlanetData;

    /*
     * Constructor for the test class. Required by Android test classes. The constructor
     * must call the super constructor, providing the Android package name of the app under test
     * and the Java class name of the activity in that application that handles the MAIN intent.
     */
    public SpinnerActivityTest() {

        super("com.android.example.spinner", SpinnerActivity.class);
    }

    /*
     * Sets up the test environment before each test.
     * @see android.test.ActivityInstrumentationTestCase2#setUp()
     */
    @Override
    protected void setUp() throws Exception {

        /*
         * Call the super constructor (required by JUnit)
         */

        super.setUp();

        /*
         * prepare to send key events to the app under test by turning off touch mode.
         * Must be done before the first call to getActivity()
         */

        setActivityInitialTouchMode(false);

        /*
         * Start the app under test by starting its main activity. The test runner already knows
         * which activity this is from the call to the super constructor, as mentioned
         * previously. The tests can now use instrumentation to directly access the main
         * activity through mActivity.
         */
        mActivity = getActivity();

        /*
         * Get references to objects in the application under test. These are
         * tested to ensure that the app under test has initialized correctly.
         */

        mSpinner = (Spinner)mActivity.findViewById(com.android.example.spinner.R.id.Spinner01);

        mPlanetData = mSpinner.getAdapter();

    }

    /*
     * Tests the initial values of key objects in the app under test, to ensure the initial
     * conditions make sense. If one of these is not initialized correctly, then subsequent
     * tests are suspect and should be ignored.
     */

    public void testPreconditions() {

        /*
         *  An example of an initialization test. Assert that the item select listener in
         *  the main Activity is not null (has been set to a valid callback)
         */
        assertTrue(mSpinner.getOnItemSelectedListener() != null);

        /*
         * Test that the spinner's backing mLocalAdapter was initialized correctly.
         */

        assertTrue(mPlanetData != null);

        /*
         *  Also ensure that the backing mLocalAdapter has the correct number of entries.
         */

        assertEquals(mPlanetData.getCount(), ADAPTER_COUNT);
    }

    /*
     * Tests the UI of the main activity. Sends key events (keystrokes) to the UI, then checks
     * if the resulting spinner state is consistent with the attempted selection.
     */
    public void testSpinnerUI() {

        /*
         * Request focus for the spinner widget in the application under test,
         * and set its initial position. This code interacts with the app's View
         *  so it has to run on the app's thread not the test's thread.
         *
         * To do this, pass the necessary code to the application with
         * runOnUiThread(). The parameter is an anonymous Runnable object that
         * contains the Java statements put in it by its run() method.
         */
        mActivity.runOnUiThread(
            new Runnable() {
                public void run() {
                    mSpinner.requestFocus();
                    mSpinner.setSelection(INITIAL_POSITION);
                }
            }
        );

        // Activate the spinner by clicking the center keypad key

        this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

        // send 5 down arrow keys to the spinner

        for (int i = 1; i <= TEST_POSITION; i++) {

            this.sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        }

        // select the item at the current spinner position

        this.sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);

        // get the position of the selected item

        mPos = mSpinner.getSelectedItemPosition();

        /*
         * from the spinner's data mLocalAdapter, get the object at the selected position
         * (this is a String value)
         */

        mSelection = (String)mSpinner.getItemAtPosition(mPos);

        /*
         * Get the TextView widget that displays the result of selecting an item from the spinner
         */

        TextView resultView =
                (TextView) mActivity.findViewById(com.android.example.spinner.R.id.SpinnerResult);

        // Get the String value in the EditText object

        String resultText = (String) resultView.getText();

        /*
         * Confirm that the EditText contains the same value as the data in the mLocalAdapter
         */

        assertEquals(resultText,mSelection);
    }

    /*
     *  Tests that the activity under test maintains the spinner state when the activity halts
     *  and then restarts (for example, if the device reboots). Sets the spinner to a
     *  certain state, calls finish() on the activity, restarts the activity, and then
     *  checks that the spinner has the same state.
     *
     */

    public void testStateDestroy() {

        /*
         * Set the position and value of the spinner in the Activity. The test runner's
         * instrumentation enables this by running the test app and the main app in the same
         * process.
         */


        mActivity.setSpinnerPosition(TEST_STATE_DESTROY_POSITION);

        mActivity.setSpinnerSelection(TEST_STATE_DESTROY_SELECTION);

        // Halt the Activity by calling Activity.finish() on it

        mActivity.finish();

        // Restart the activity by calling ActivityInstrumentationTestCase2.getActivity()

        mActivity = this.getActivity();

        /*
         * Get the current position and selection from the activity.
         */

        int currentPosition = mActivity.getSpinnerPosition();
        String currentSelection = mActivity.getSpinnerSelection();

        // test that they are the same.

        assertEquals(TEST_STATE_DESTROY_POSITION, currentPosition);

        assertEquals(TEST_STATE_DESTROY_SELECTION, currentSelection);
    }

    /*
     * Tests that the activity under test maintains the spinner's state when the activity is
     * paused and then resumed.
     *
     * Calls the activity's onResume() method. Changes the spinner's state by
     * altering the activity's View. This means the test must run
     * on the UI Thread. All the statements in the test method may be run on
     * that thread, so instead of using the runOnUiThread() method, the
     * @UiThreadTest is used.
     */
    @UiThreadTest

    public void testStatePause() {

        /*
         * Get the instrumentation object for this application. This object
         * does all the instrumentation work for the test runner
         */

        Instrumentation instr = this.getInstrumentation();

        /*
         * Set the activity's fields for the position and value of the spinner
         */

        mActivity.setSpinnerPosition(TEST_STATE_PAUSE_POSITION);

        mActivity.setSpinnerSelection(TEST_STATE_PAUSE_SELECTION);

        /*
         *  Use the instrumentation to onPause() on the currently running Activity.
         *  This analogous to calling finish() in the testStateDestroy() method.
         *  This way demonstrates using the test class' instrumentation.
         */

        instr.callActivityOnPause(mActivity);

        /*
         * Set the spinner to a test position
         */

        mActivity.setSpinnerPosition(0);

        mActivity.setSpinnerSelection("");

        /*
         * Call the activity's onResume() method. This forces the activity
         * to restore its state.
         */

        instr.callActivityOnResume(mActivity);

        /*
         * Get the current state of the spinner
         */

        int currentPosition = mActivity.getSpinnerPosition();

        String currentSelection = mActivity.getSpinnerSelection();

        assertEquals(TEST_STATE_PAUSE_POSITION,currentPosition);
        assertEquals(TEST_STATE_PAUSE_SELECTION,currentSelection);
  }

}
