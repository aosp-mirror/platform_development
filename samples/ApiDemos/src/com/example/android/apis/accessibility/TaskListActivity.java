/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.apis.accessibility;

import com.example.android.apis.R;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

/** Starts up the task list that will interact with the AccessibilityService sample. */
public class TaskListActivity extends ListActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tasklist_main);

        // Hardcoded hand-waving here.
        boolean[] checkboxes = {true, true, false, true, false, false, false};
        String[] labels = {"Take out Trash", "Do Laundry",
                           "Conquer World", "Nap", "Do Taxes",
                           "Abolish IRS", "Tea with Aunt Sharon" };

        TaskAdapter myAdapter = new TaskAdapter(this, labels, checkboxes);
        this.setListAdapter(myAdapter);
    }
}
