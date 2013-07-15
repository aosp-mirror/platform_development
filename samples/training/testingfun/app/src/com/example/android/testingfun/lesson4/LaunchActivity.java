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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Launches NextActivity and passes a payload in the Bundle.
 */
public class LaunchActivity extends Activity {

    /**
     * The payload that is passed as Intent data to NextActivity.
     */
    public final static String STRING_PAYLOAD = "Started from LaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch_next);
        Button launchNextButton = (Button) findViewById(R.id.launch_next_activity_button);
        launchNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(NextActivity.makeIntent(LaunchActivity.this, STRING_PAYLOAD));
                finish();
            }
        });
    }
}
