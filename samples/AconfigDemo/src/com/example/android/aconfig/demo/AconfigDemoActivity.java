/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;

import com.example.android.aconfig.demo.flags.Flags;

import javax.inject.Inject;


/**
 * A minimal "Hello, World!" application.
 */
public class AconfigDemoActivity extends Activity {
    @Inject InjectedContent injectedContent;
    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((AconfigDemoApplication)getApplicationContext()).appComponent.inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        TextView simpleTextView = (TextView) findViewById(R.id.simpleTextView);
        simpleTextView.setMovementMethod(new ScrollingMovementMethod());

        simpleTextView.setText("Show Java Flags: \n\n");

        StaticContent cp = new StaticContent();
        simpleTextView.append(cp.getContent());

        simpleTextView.append(injectedContent.getContent());

        simpleTextView.append("Show C/C++ Flags: \n\n");
        simpleTextView.append(printCFlag());

        if (Flags.awesomeFlag1()) {
            Log.v("AconfigDemoActivity", Flags.FLAG_AWESOME_FLAG_1 + " is on!");
        }

        if (Flags.awesomeFlag2()) {
            Log.v("AconfigDemoActivity", Flags.FLAG_AWESOME_FLAG_2 + " is on!");
        }

        simpleTextView.append("\n\nShow Rust Flags: \n\n");
        simpleTextView.append(printRustFlag());
    }

    public native String printCFlag();
    public native String printRustFlag();

    static {
        System.loadLibrary("example_cpp_lib");
        System.loadLibrary("example_rust_jni");
    }
}
