/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.pingme;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Intent mServiceIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creates an explicit Intent to start the service that constructs and
        // issues the notification.
        mServiceIntent = new Intent(getApplicationContext(), PingService.class);
    }

    /*
     * Gets the values the user entered and adds them to the intent that will be
     * used to launch the IntentService that runs the timer and issues the
     * notification.
     */
    public void onPingClick(View v) {
        int seconds;

        // Gets the reminder text the user entered.
        EditText msgText = (EditText) findViewById(R.id.edit_reminder);
        String message = msgText.getText().toString();

        mServiceIntent.putExtra(CommonConstants.EXTRA_MESSAGE, message);
        mServiceIntent.setAction(CommonConstants.ACTION_PING);
        Toast.makeText(this, R.string.timer_start, Toast.LENGTH_SHORT).show();

        // The number of seconds the timer should run.
        EditText editText = (EditText)findViewById(R.id.edit_seconds);
        String input = editText.getText().toString();

        if(input == null || input.trim().equals("")){
            // If user didn't enter a value, sets to default.
            seconds = R.string.seconds_default;
        } else {
            seconds = Integer.parseInt(input);
        }
        int milliseconds = (seconds * 1000);
        mServiceIntent.putExtra(CommonConstants.EXTRA_TIMER, milliseconds);
        // Launches IntentService "PingService" to set timer.
        startService(mServiceIntent);
    }
}
