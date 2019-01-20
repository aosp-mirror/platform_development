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


import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.AppTask;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.intentplayground.InlineAdapter.TaskViewHolder;
import com.example.android.intentplayground.Tracking.Task;

import java.util.Collections;
import java.util.List;

public class InlineAdapter extends RecyclerView.Adapter<TaskViewHolder> {

    private final List<Task> mTasks;
    private int mCurrentTaskIndex;
    private FragmentActivity mActivity;

    public InlineAdapter(List<Task> tasks, FragmentActivity activity) {
        this.mActivity = activity;
        this.mTasks = tasks;
        this.mCurrentTaskIndex = indexOfRunningTask();
    }

    public int indexOfRunningTask() {
        int currentTaskId = mActivity.getTaskId();

        int index = 0;
        for (int i = 0; i < mTasks.size(); i++) {
            Task task = mTasks.get(i);
            if (task.id == currentTaskId) {
                index = i;
                break;
            }
        }

        return index;
    }


    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View task = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tree_node_composite, parent, false);
        return new TaskViewHolder(task);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        boolean highLightRunningActivity = position == mCurrentTaskIndex;
        holder.setTask(mTasks.get(position), mActivity.getSupportFragmentManager(),
                highLightRunningActivity);
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTaskIdTextView;
        private final LinearLayout mActivitiesLayout;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            mTaskIdTextView = itemView.findViewById(R.id.task_id);
            mActivitiesLayout = itemView.findViewById(R.id.activity_node_container);
        }

        public void setTask(Task task, FragmentManager manager, boolean highLightRunningActivity) {
            mTaskIdTextView.setText(task.id == Node.NEW_TASK_ID
                    ? mTaskIdTextView.getContext().getString(R.string.new_task)
                    : String.valueOf(task.id));
            int taskColor = mTaskIdTextView.getContext()
                    .getResources().getColor(ColorManager.getColorForTask(task.id),
                            null /* theme */);
            mTaskIdTextView.setTextColor(taskColor);

            mActivitiesLayout.removeAllViews();
            for (Activity activity : task.mActivities) {
                View activityView = LayoutInflater.from(mActivitiesLayout.getContext())
                        .inflate(R.layout.activity_node, mActivitiesLayout, false);

                TextView activityName = activityView.findViewById(R.id.activity_name);

                activityName.setText(activity.getComponentName().getShortClassName());
                activityName.setOnClickListener(clickedView -> {
                    Intent intent = activity.getIntent();
                    List<String> flags;
                    if (intent != null) {
                        flags = FlagUtils.discoverFlags(intent);
                        if (flags.size() == 0) {
                            flags.add("None");
                        }
                    } else {
                        flags = Collections.singletonList("None");
                    }
                    showDialogWithFlags(manager, activity.getComponentName().getShortClassName(),
                            flags, task.id);
                });


                if (highLightRunningActivity) {
                    highLightRunningActivity = false;
                    activityName.setTextColor(taskColor);
                }

                mActivitiesLayout.addView(activityView);
            }
        }

        /**
         * Shows a dialog with a list.
         *
         * @param shortClassName The activity name and title of the dialog.
         * @param flags          The flags to list.
         */
        private void showDialogWithFlags(FragmentManager manager,
                String shortClassName, List<String> flags, int taskId) {
            IntentDialogFragment.newInstance(shortClassName, flags, taskId).show(manager,
                    "intentDialog");
        }
    }
}
