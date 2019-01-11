/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static java.util.Collections.singletonList;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * TestBase holds methods to query, test and compare task hierarchies.
 */
public class TestBase {
    static final String TAG = "TestBase";
    private List<TaskStackBuilder> mBuilders;
    private Context mContext;

    TestBase(Context context, Node hierarchy) {
        mBuilders = new LinkedList<>();
        mContext = context;
        setActivities(hierarchy);
    }

    /**
     * Launch the activities specified by the constructor.
     *
     * @param style An enum that chooses which method to use to launch the activities.
     */
    void startActivities(LaunchStyle style) {
        switch (style) {
            // COMMAND_LINE will only work if the application is installed with system permissions
            // that allow it to use am shell command "am start ..."
            case COMMAND_LINE:
                mBuilders.forEach(tsb -> Arrays.stream(tsb.getIntents())
                        .forEach(AMControl::launchInBackground));
                break;
            case TASK_STACK_BUILDER:
                mBuilders.forEach(tsb -> {
                    // TODO: does this indicate bug in ActivityManager?
                    // The launch of each activity needs to be delayed a bit or ActivityManager will7
                    // skip creating most of them
                    try {
                        Thread.sleep(500);
                        tsb.startActivities();
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, ie.getMessage());
                    }
                });
                break;
            case LAUNCH_FORWARD:
                mBuilders.forEach(tsb -> {
                    // The launch of each activity needs to be delayed a bit or ActivityManager will
                    // skip creating most of them
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, ie.getMessage());
                    }
                    ArrayList<Intent> nextIntents = new ArrayList<>(Arrays.asList(
                            tsb.getIntents()));
                    Intent launch = nextIntents.remove(0)
                            .putParcelableArrayListExtra(BaseActivity.EXTRA_LAUNCH_FORWARD,
                                    nextIntents);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Launching " + launch.getComponent().toString());
                    }
                    mContext.startActivity(launch);
                });
                break;
        }
    }

    void setActivities(Node hierarchy) {
        // Build list of TaskStackBuilders from task hierarchy modeled by Node
        if (hierarchy.mChildren.isEmpty()) return;
        mBuilders.clear();
        hierarchy.mChildren.forEach(taskParent -> {
            TaskStackBuilder tb = TaskStackBuilder.create(mContext);
            Intent taskRoot = new Intent()
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .setComponent(taskParent.mChildren.get(0).mName);
            tb.addNextIntent(taskRoot);
            taskParent.mChildren.subList(1, taskParent.mChildren.size()).forEach(activity ->
                    tb.addNextIntent(new Intent().setComponent(activity.mName)));
            mBuilders.add(tb);
        });
        // Edit the mIntent of the last activity in the last task so that it will relaunch the
        // activity that constructed this TestBase
        TaskStackBuilder tsb = mBuilders.get(mBuilders.size() - 1);
        Intent lastIntent = tsb.editIntentAt(tsb.getIntentCount() - 1);
        Intent launcherIntent =  new Intent(mContext, mContext.getClass());
        lastIntent.putParcelableArrayListExtra(BaseActivity.EXTRA_LAUNCH_FORWARD,
                new ArrayList<>(singletonList(launcherIntent)));
    }

    public Context getContext() { return mContext; }

    /**
     * An enum representing options for launching a series of tasks using this TestBase.
     */
    enum LaunchStyle { TASK_STACK_BUILDER, COMMAND_LINE, LAUNCH_FORWARD}
}

