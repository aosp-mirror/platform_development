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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * All of the other activities extend BaseActivity, the shared functionality is implemented here
 */
public abstract class BaseActivity extends Activity {
    public final static String LAUNCH_FORWARD = "com.example.android.launchForward";
    public final static String BUILDER_FRAGMENT = "com.example.android.builderFragment";
    protected ComponentName mActivityToLaunch;
    protected List<ActivityManager.AppTask> mTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (BuildConfig.DEBUG) Log.d(this.getLocalClassName(), "onCreate()");
        Intent intent = getIntent();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, new CurrentTaskFragment());
        TreeFragment currentTaskFrag = new TreeFragment();
        Bundle args = new Bundle();
        args.putString(TreeFragment.FRAGMENT_TITLE,
                getString(R.string.current_task_hierarchy_title));
        currentTaskFrag.setArguments(args);
        transaction.add(R.id.fragment_container, currentTaskFrag);

        if (intent.hasExtra(TestBase.EXPECTED_HIERARCHY)) {
            // That means this activity was launched as a test show the result fragment
            TreeFragment expectedView = new TreeFragment();
            Bundle expectedArgs = new Bundle();
            expectedArgs.putParcelable(TreeFragment.TREE_NODE,
                    intent.getParcelableExtra(TestBase.EXPECTED_HIERARCHY));
            expectedArgs.putString(TreeFragment.FRAGMENT_TITLE,
                    getString(R.string.expected_task_hierarchy_title));
            expectedView.setArguments(expectedArgs);
            transaction.add(R.id.fragment_container, expectedView);
        }

        transaction.add(R.id.fragment_container, new IntentFragment());
        transaction.add(R.id.fragment_container, new IntentBuilderFragment(), BUILDER_FRAGMENT);
        transaction.commit();

        if (intent.hasExtra(LAUNCH_FORWARD)) {
            ArrayList<Intent> intents = intent.getParcelableArrayListExtra(LAUNCH_FORWARD);
            if (!intents.isEmpty()) {
                Intent nextIntent = intents.remove(0);
                nextIntent.putParcelableArrayListExtra(LAUNCH_FORWARD, intents);
                if (BuildConfig.DEBUG) {
                    Log.d(this.getLocalClassName(),
                            LAUNCH_FORWARD + " " + nextIntent.getComponent().toString());
                }
                startActivity(nextIntent);
            }
        }
    }

    /**
     * Launches activity with the selected options
     */
    public void launchActivity(View view) {
        Intent customIntent = new Intent();
        LinearLayout flagBuilder = findViewById(R.id.build_intent_flags);
        // Gather flags from flag builder checkbox list
        childrenOfGroup(flagBuilder, CheckBox.class)
                .forEach(checkbox -> {
                    int flagVal = FlagUtils.value(checkbox.getText().toString());
                    if (checkbox.isChecked()) customIntent.addFlags(flagVal);
                    else customIntent.removeFlags(flagVal);
                });
        customIntent.setComponent(mActivityToLaunch);
        startActivity(customIntent);
    }

    /**
     * Convenience method to retrieve children of a certain type from a {@link ViewGroup}
     * @param group the ViewGroup to retrieve children from
     */
    protected static <T> List<T> childrenOfGroup(ViewGroup group, Class<T> viewType) {
        List<T> list = new LinkedList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View v = group.getChildAt(i);
            if (viewType.isAssignableFrom(v.getClass())) list.add(viewType.cast(v));
        }
        return list;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}
