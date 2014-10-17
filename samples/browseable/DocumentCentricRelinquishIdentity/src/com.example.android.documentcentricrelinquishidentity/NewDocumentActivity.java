/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.documentcentricrelinquishidentity;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

/**
 * Activity that changes its task identifiers by setting a new {@link android.app.ActivityManager.TaskDescription}
 */
public class NewDocumentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document);
        // Set a new task description to change label and icon
        setTaskDescription(newTaskDescription());
    }

    /**
     * Creates a new {@link android.app.ActivityManager.TaskDescription} with a new label and icon to change the
     * appearance of this activity in the recents stack.
     */
    private ActivityManager.TaskDescription newTaskDescription() {
        Bitmap newIcon = BitmapFactory.decodeResource(getResources(), R.drawable.new_icon);
        String newDocumentRecentsLabel = getString(R.string.new_document_recents_label);
        ActivityManager.TaskDescription newTaskDescription = new ActivityManager.TaskDescription(
                newDocumentRecentsLabel, newIcon);
        return newTaskDescription;
    }

}
