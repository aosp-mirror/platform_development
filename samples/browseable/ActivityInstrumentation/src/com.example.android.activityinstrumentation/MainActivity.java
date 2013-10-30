/*
 * Copyright 2013 The Android Open Source Project
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

package com.example.android.activityinstrumentation;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Basic activity with a spinner. The spinner should persist its position to disk every time a
 * new selection is made.
 */
public class MainActivity extends Activity {

    /** Shared preferences key: Holds spinner position. Must not be negative. */
    private static final String PREF_SPINNER_POS = "spinner_pos";
    /** Magic constant to indicate that no value is stored for PREF_SPINNER_POS. */
    private static final int PREF_SPINNER_VALUE_ISNULL = -1;
    /** Values for display in spinner. */
    private static final String[] SPINNER_VALUES = new String[] {
            "Select Weather...", "Sunny", "Partly Cloudy", "Cloudy", "Rain", "Snow", "Hurricane"};

    // Constants representing each of the options in SPINNER_VALUES. Declared package-private
    // so that they can be accessed from our test suite.
    static final int WEATHER_NOSELECTION = 0;
    static final int WEATHER_SUNNY = 1;
    static final int WEATHER_PARTLY_CLOUDY = 2;
    static final int WEATHER_CLOUDY = 3;
    static final int WEATHER_RAIN = 4;
    static final int WEATHER_SNOW = 5;
    static final int WEATHER_HURRICANE = 6;

    /** Handle to default shared preferences for this activity. */
    private SharedPreferences mPrefs;
    /** Handle to the spinner in this Activity's layout. */
    private Spinner mSpinner;

    /**
     * Setup activity state.
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate UI from res/layout/activity_main.xml
        setContentView(R.layout.sample_main);

        // Get handle to default shared preferences for this activity
        mPrefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        // Populate spinner with sample values from an array
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mSpinner.setAdapter(
                new ArrayAdapter<String>(
                        this,                                                   // Context
                        android.R.layout.simple_list_item_1,                    // Layout
                        new ArrayList<String>(Arrays.asList(SPINNER_VALUES))    // Data source
                ));

        // Read in a sample value, if it's not set.
        int selection = mPrefs.getInt(PREF_SPINNER_POS, PREF_SPINNER_VALUE_ISNULL);
        if (selection != PREF_SPINNER_VALUE_ISNULL) {
            mSpinner.setSelection(selection);
        }

        // Callback to persist spinner data whenever a new value is selected. This will be the
        // focus of our sample unit test.
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            // The methods below commit the ID of the currently selected item in the spinner
            // to disk, using a SharedPreferences file.
            //
            // Note: A common mistake here is to forget to call .commit(). Try removing this
            // statement and running the tests to watch them fail.
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mPrefs.edit().putInt(PREF_SPINNER_POS, position).commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mPrefs.edit().remove(PREF_SPINNER_POS).commit();
            }
        });
    }
}
