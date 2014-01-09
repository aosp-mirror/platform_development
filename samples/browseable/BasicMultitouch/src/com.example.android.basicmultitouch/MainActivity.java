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

package com.example.android.basicmultitouch;

import android.app.Activity;
import android.os.Bundle;

/**
 * This is an example of keeping track of individual touches across multiple
 * {@link android.view.MotionEvent}s.
 * <p>
 * This is illustrated by a View ({@link TouchDisplayView}) that responds to
 * touch events and draws coloured circles for each pointer, stores the last
 * positions of this pointer and draws them. This example shows the relationship
 * between MotionEvent indices, pointer identifiers and actions.
 *
 * @see android.view.MotionEvent
 */
public class MainActivity extends Activity {
    TouchDisplayView mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.layout_mainactivity);
    }

}
