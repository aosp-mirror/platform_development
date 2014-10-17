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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * Activities that serve as the root of a task may give up certain task identifiers to activities
 * above it in the task stack. These identifiers include the task base Intent, and the task name,
 * color and icon used in the recent task list. The base @link{Intent} is used to match the task when
 * relaunching based on an incoming Intent.
 *
 * <p>
 * To relinquish its identity the base activity must have the activity attribute
 * android:relinquishTaskIdentity=”true” in the manifest.
 * </p>
 */
public class RelinquishIdentityActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_relinquish_identity);
    }

    public void createNewDocument(View view) {
        final Intent intent = newDocumentIntent();
        startActivity(intent);
    }

    /**
     * Returns an new intent to start {@link NewDocumentActivity}
     * as a new document in recents..
     */
    private Intent newDocumentIntent() {
        final Intent newDocumentIntent = new Intent(this, NewDocumentActivity.class);
        newDocumentIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        // Always launch in a new task.
        newDocumentIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        return newDocumentIntent;
    }

}
