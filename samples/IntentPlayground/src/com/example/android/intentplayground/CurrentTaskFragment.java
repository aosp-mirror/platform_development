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
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.List;
import java.util.Locale;

/**
 * Displays details about the current task and activity.
 */
public class CurrentTaskFragment extends Fragment {
    private TextView mCurrentTaskView, mCurrentActivityView, mLastTaskView, mLastActivityView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout layout =  (LinearLayout) inflater.inflate(R.layout.fragment_current_task,
                container, false /* attachToRoot */);
        mCurrentTaskView = layout.findViewById(R.id.current_task);
        mCurrentActivityView = layout.findViewById(R.id.current_activity);
        mLastTaskView = layout.findViewById(R.id.last_task);
        mLastActivityView = layout.findViewById(R.id.last_activity);
        return layout;
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        Resources res = activity.getResources();
        List<AppTask> tasks = activity.getSystemService(ActivityManager.class).getAppTasks();
        RecentTaskInfo currentTask = tasks.get(0).getTaskInfo();
        RecentTaskInfo lastTask = tasks.size() > 1 && tasks.get(1) != null ?
                tasks.get(1).getTaskInfo() : null;
        mCurrentTaskView.setText(String.format(Locale.ENGLISH, "#%d", currentTask.persistentId));
        mCurrentTaskView.setTextColor(res.getColor(ColorManager.getColorForTask(
                currentTask.persistentId), null /* theme */));
        mCurrentActivityView.setText(currentTask.topActivity.getShortClassName());
        mCurrentActivityView.setTextColor(res.getColor(ColorManager.getColorForActivity(
                currentTask.topActivity), null /* theme */));
        if (lastTask != null) {
            mLastTaskView.setText(String.format(Locale.ENGLISH, "#%d", lastTask.persistentId));
            mLastTaskView.setTextColor(res.getColor(ColorManager.getColorForTask(
                    lastTask.persistentId), null /* theme */));
            if (lastTask.topActivity != null) {
                mLastActivityView.setText(lastTask.topActivity.getShortClassName());
                mLastActivityView.setTextColor(res.getColor(ColorManager.getColorForActivity(
                        lastTask.topActivity), null /* theme */));
            }
        }
    }
}
