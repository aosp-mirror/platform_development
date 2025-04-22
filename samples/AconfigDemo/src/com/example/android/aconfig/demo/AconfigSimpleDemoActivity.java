/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.example.android.aconfig.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.example.android.aconfig.demo.flags.Flags;

/** Display for the simple demo. */
public class AconfigSimpleDemoActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_demo);

        TextView flaggedTextView = (TextView) findViewById(R.id.flaggedTextView);
        // flaggedTextView.setText("The flag doesn't exist yet.");
        // flaggedTextView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        // flaggedTextView.setTextColor(Color.parseColor("#000000"));

        if (Flags.awesomeDemoFlag()) {
            flaggedTextView.setText("Flag is ON.");
            flaggedTextView.setBackgroundColor(Color.parseColor("#1e8e3e"));
            flaggedTextView.setTextColor(Color.parseColor("#ffffff"));
        } else {
            flaggedTextView.setText("Flag is OFF.");
            flaggedTextView.setBackgroundColor(Color.parseColor("#d93025"));
            flaggedTextView.setTextColor(Color.parseColor("#ffffff"));
        }
    }
}
