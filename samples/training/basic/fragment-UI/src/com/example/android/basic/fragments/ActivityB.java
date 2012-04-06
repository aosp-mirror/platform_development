/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.example.android.basic.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.example.com.example.android.basic.fragments.R;

/**
 * Sample Activity entry point for demonstrating a multi-pane UI using Fragments, assembled
 * using Java code as an alternative to XML in the layout file.
 */
public class ActivityB extends FragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_b);

        // Create FragmentA and add it to the 'main' FrameLayout
        FragmentA fragmentA = new FragmentA();
        fragmentA.setArguments(getIntent().getExtras());
        getSupportFragmentManager().beginTransaction().add(R.id.main, fragmentA).commit();

        // Create FragmentB, if in landscape orientation, and add it to the 'content' FrameLayout
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            FragmentB fragmentB = new FragmentB();
            fragmentB.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(R.id.content, fragmentB).commit();
        }
    }
}
