/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.intentplayground;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * A singleInstance activity that is responsible for a launching a bootstrap stack of activities
 */
public class LauncherActivity extends BaseActivity {
    private TestBase mTester;
    public static final String TAG = "LauncherActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Node mRoot = new Node(new ComponentName(this, LauncherActivity.class));
        // Describe initial setup of tasks
        // create singleTask, singleInstance, and two documents in separate tasks
        mRoot.addChild( new Node(new ComponentName(this, SingleTaskActivity.class)))
                .addChild( new Node(new ComponentName(this, DocumentLaunchAlwaysActivity.class)))
                .addChild( new Node(new ComponentName(this, DocumentLaunchIntoActivity.class)));
        // Create three tasks with three activities each, with affinity set
        Node taskAffinity1 = new Node(new ComponentName(this, TaskAffinity1Activity.class));
        taskAffinity1
                .addChild(new Node(new ComponentName(this, TaskAffinity1Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity1Activity.class)));
        Node taskAffinity2 = new Node(new ComponentName(this, ClearTaskOnLaunchActivity.class));
        taskAffinity2
                .addChild(new Node(new ComponentName(this, TaskAffinity2Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity2Activity.class)));
        Node taskAffinity3 = new Node(new ComponentName(this, TaskAffinity3Activity.class));
        taskAffinity3
                .addChild(new Node(new ComponentName(this, TaskAffinity3Activity.class)))
                .addChild(new Node(new ComponentName(this, TaskAffinity3Activity.class)));
        mRoot.addChild(taskAffinity1).addChild(taskAffinity2).addChild(taskAffinity3);
        mTester = new TestBase(this, mRoot);
        mTester.setupActivities(TestBase.LaunchStyle.TASK_STACK_BUILDER);
    }

    /**
     * Launches activity with the selected options
     */
    public void launchActivity(Intent customIntent) {
        customIntent.putExtra(TestBase.EXPECTED_HIERARCHY, mTester.computeExpected(customIntent));
        startActivity(customIntent);
    }
}