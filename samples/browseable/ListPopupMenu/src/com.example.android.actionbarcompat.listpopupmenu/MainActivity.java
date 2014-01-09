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

package com.example.android.actionbarcompat.listpopupmenu;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

/**
 * This sample shows you how to use {@link android.support.v7.widget.PopupMenu PopupMenu} from
 * ActionBarCompat to create a list, with each item having a dropdown menu.
 * <p>
 * The interesting part of this sample is in {@link PopupListFragment}.
 *
 * This Activity extends from {@link ActionBarActivity}, which provides all of the function
 * necessary to display a compatible Action Bar on devices running Android v2.1+.
 */
public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set content view (which contains a PopupListFragment)
        setContentView(R.layout.sample_main);
    }

}
