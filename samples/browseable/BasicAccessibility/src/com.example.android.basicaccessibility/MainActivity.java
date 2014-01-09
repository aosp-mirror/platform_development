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

package com.example.android.basicaccessibility;

import android.app.Activity;
import android.os.Bundle;

/**
 * Basic activity class.
 *
 * <p>Responsible for rendering layout, and displaying some toasts to give buttons feedback.
 * There's nothing terribly interesting in this class. All the interesting stuff is in
 * res/layout/activity_main.xml and {@link DialView}.
 */
public class MainActivity extends Activity {

    /**
     * Standard onCreate() implementation. Sets R.layout.activity_main as the layout.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
    }
}
