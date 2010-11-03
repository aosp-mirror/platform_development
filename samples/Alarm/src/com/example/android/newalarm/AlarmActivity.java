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

package com.example.android.newalarm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * This is the activity that controls AlarmService.
 * <p>
 * When the user clicks the "Start Alarm Service" button, it triggers a repeating countdown
 * timer. Every thirty seconds, the timer starts AlarmService, which then runs for 15 seconds
 * and shuts itself down.
 * </p>
 * <p>
 * When the user clicks the "Stop Alarm Service" button, it stops the countdown timer.
 * </p>
 */

public class AlarmActivity extends Activity {
    // 30 seconds in milliseconds
    private static final long THIRTY_SECONDS_MILLIS = 30 * 1000;

    // An intent for AlarmService, to trigger it as if the Activity called startService().
    private PendingIntent mAlarmSender;

    // Contains a handle to the system alarm service
    private AlarmManager mAlarmManager;

    /**
     * This method is called when Android starts the activity. It initializes the UI.
     * <p>
     * This method is automatically called when Android starts the Activity
     * </p>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a PendingIntent to trigger a startService() for AlarmService
        mAlarmSender = PendingIntent.getService(  // set up an intent for a call to a service
            AlarmActivity.this,  // the current context
            0,  // request code (not used)
            new Intent(AlarmActivity.this, AlarmService.class),  // A new Service intent
            0   // flags (none are required for a service)
        );

        // Creates the main view
        setContentView(R.layout.main);

        // Finds the button that starts the repeating countdown timer
        Button button = (Button)findViewById(R.id.start_alarm);

        // Sets the listener for the start button
        button.setOnClickListener(mStartAlarmListener);

        // Finds the button that stops countdown timer
        button = (Button)findViewById(R.id.stop_alarm);

        // Sets the listener for the stop button
        button.setOnClickListener(mStopAlarmListener);

        // Gets the handle to the system alarm service
        mAlarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
    }

    // Creates a new anonymous click listener for the start button. It starts the repeating
    //  countdown timer.
    private OnClickListener mStartAlarmListener = new OnClickListener() {
        // Sets the callback for when the button is clicked
        public void onClick(View v) {

            // Sets the time when the alarm will first go off
            // The Android AlarmManager uses this form of the current time.
            long firstAlarmTime = SystemClock.elapsedRealtime();

            // Sets a repeating countdown timer that triggers AlarmService
            mAlarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP, // based on time since last wake up
                firstAlarmTime,  // sends the first alarm immediately
                THIRTY_SECONDS_MILLIS,  // repeats every thirty seconds
                mAlarmSender  // when the alarm goes off, sends this Intent
            );

            // Notifies the user that the repeating countdown timer has been started
            Toast.makeText(
                AlarmActivity.this,  //  the current context
                R.string.repeating_started,  // the message to display
                Toast.LENGTH_LONG  // how long to display the message
            ).show();  // show the message on the screen
        }
    };

    // Creates a new anonymous click listener for the stop button. It shuts off the repeating
    // countdown timer.
    private OnClickListener mStopAlarmListener = new OnClickListener() {
        // Sets the callback for when the button is clicked
        public void onClick(View v) {

            // Cancels the repeating countdown timer
            mAlarmManager.cancel(mAlarmSender);

            // Notifies the user that the repeating countdown timer has been stopped
            Toast.makeText(
                AlarmActivity.this,  //  the current context
                R.string.repeating_stopped,  // the message to display
                Toast.LENGTH_LONG  // how long to display the message
            ).show(); // display the message
        }
    };
}
