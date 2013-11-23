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
package com.example.android.testingfun.lesson4;

import com.example.android.testingfun.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * This activity is started from LaunchActivity. It reads the payload from the given bundle and
 * displays it using a TextView.
 */
public class NextActivity extends Activity {

    /**
     * Extras key for the payload.
     */
    public final static String EXTRAS_PAYLOAD_KEY
            = "com.example.android.testingfun.lesson4.EXTRAS_PAYLOAD_KEY";

    /**
     * Factory method to create a launch Intent for this activity.
     *
     * @param context the context that intent should be bound to
     * @param payload the payload data that should be added for this intent
     * @return a configured intent to launch this activity with a String payload.
     */
    public static Intent makeIntent(Context context, String payload) {
        return new Intent(context, NextActivity.class).putExtra(EXTRAS_PAYLOAD_KEY, payload);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        final String stringPayload = getIntent().getStringExtra(EXTRAS_PAYLOAD_KEY);

        if (stringPayload != null) {
            ((TextView) findViewById(R.id.next_activity_info_text_view)).setText(stringPayload);
        }

    }
}
