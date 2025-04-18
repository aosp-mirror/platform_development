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
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

/** Display for the Aconfig C++ / Rust codelab. */
public class AconfigNativeCodelabActivity extends Activity {
    private static final String TAG = "AconfigNativeCodelabActivity";
    private static boolean sLoadedCppLib = false;
    private static boolean sLoadedRustLib = false;

    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.native_codelab);

        TextView mainTextView = (TextView) findViewById(R.id.mainTextView);
        mainTextView.setMovementMethod(new ScrollingMovementMethod());

        if (sLoadedCppLib) {
            mainTextView.append("C/C++ Flags: \n\n");
            mainTextView.append(printCFlag());
        } else {
            mainTextView.append("Can't show C/C++ flags; error loading native lib.\n");
        }

        if (sLoadedRustLib) {
            mainTextView.append("\n\nRust Flags: \n\n");
            mainTextView.append(printRustFlag());
        } else {
            mainTextView.append("Can't show Rust flags; error loading native lib.\n");
        }
    }

    /** Prints the C++ flag. */
    public native String printCFlag();

    /** Prints the Rust flag. */
    public native String printRustFlag();

    static {
        try {
            System.loadLibrary("example_cpp_lib");
            sLoadedCppLib = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Error loading c++ library:", e);
        }

        try {
            System.loadLibrary("example_rust_jni");
            sLoadedRustLib = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Error loading rust library:", e);
        }
    }
}
