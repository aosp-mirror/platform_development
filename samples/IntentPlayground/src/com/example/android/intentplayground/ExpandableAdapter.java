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

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A two-level adapter for tasks and the activities that they hold (represented by Node).
 */
class ExpandableAdapter extends BaseExpandableListAdapter {
    private FragmentActivity mActivity;
    private Node mTasks;

    /**
     * Constructs a new ExpandableAdapter.
     * @param activity The activity that holds this adapter.
     * @param tasks The {@link Node} root of the task hierarchy.
     */
    public ExpandableAdapter(FragmentActivity activity, Node tasks) {
        mActivity = activity;
        mTasks = tasks;
    }
    @Override
    public int getGroupCount() {
        return mTasks.mChildren.size();
    }

    @Override
    public int getChildrenCount(int group) {
        return mTasks.mChildren.get(group).mChildren.size();
    }

    @Override
    public Object getGroup(int group) {
        return mTasks.mChildren.get(group);
    }

    @Override
    public Object getChild(int group, int child) {
        return mTasks.mChildren.get(group).mChildren.get(child);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int group, boolean isExpanded, View view, ViewGroup parent) {
        String numActivitiesText;
        TaskViewHolder holder;
        Node task = (Node) getGroup(group);
        int nActivities = getChildrenCount(group);
        switch (nActivities) {
            case 0: numActivitiesText = mActivity.getString(R.string.no_activities_text);
                break;
            case 1: numActivitiesText = mActivity.getString(R.string.one_activity_text);
                break;
            default: numActivitiesText = String.format(Locale.ENGLISH,
                    mActivity.getString(R.string.plural_activities_text), nActivities);
        }
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(this.mActivity);
            view = inflater.inflate(R.layout.task_node,parent, false);
            holder = new TaskViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (TaskViewHolder) view.getTag();
        }
        if (isExpanded) {
            holder.indicatorImageView.setImageResource(R.drawable.expand_less_mtrl);
        } else {
            holder.indicatorImageView.setImageResource(R.drawable.expand_more_mtrl);
        }
        holder.taskIdTextView.setText(task.mTaskId == Node.NEW_TASK_ID ?
                mActivity.getString(R.string.new_task) : String.valueOf(task.mTaskId));
        holder.taskIdTextView.setTextColor(mActivity
                .getResources().getColor(ColorManager.getColorForTask(task.mTaskId),
                null /* theme */));
        holder.numActivitiesTextView.setText(numActivitiesText);
        return view;
    }

    @Override
    public View getChildView(int group, int child, boolean lastChild, View view, ViewGroup parent) {
        Node activity = (Node) getChild(group, child);
        ActivityViewHolder holder;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            view = inflater.inflate(R.layout.activity_node, parent, false /* attachToRoot */);
            holder = new ActivityViewHolder(view);
            view.setTag(holder);
        } else {
            holder = (ActivityViewHolder) view.getTag();
        }
        holder.activityNameTextView.setText(activity.mName.getShortClassName());
        holder.activityNumTextView.setText(String.format(Locale.ENGLISH, "%d",
                getChildrenCount(group) - child));
        int color = mActivity.getResources()
                .getColor(ColorManager.getColorForActivity(activity.mName), null /* theme */);
        holder.labelImageView.setColorFilter(color);
        holder.intentButtonView.setColorFilter(color);
        holder.intentButtonView.setOnClickListener(clickedView -> {
            Intent intent = ((Node) getChild(group, child)).getIntent();
            List<String> flags;
            if (intent != null) {
                flags = FlagUtils.discoverFlags(intent);
                if (flags.size() == 0) {
                    flags.add("None");
                }
            } else {
                flags = Collections.singletonList("None");
            }
            showDialogWithFlags(activity.mName.getShortClassName(), flags);
        });
        return view;
    }

    /**
     * Shows a dialog with a list.
     * @param shortClassName The activity name and title of the dialog.
     * @param flags The flags to list.
     */
    private void showDialogWithFlags(String shortClassName, List<String> flags) {
        FragmentTransaction transaction = mActivity.getSupportFragmentManager().beginTransaction();
        IntentDialogFragment.newInstance(shortClassName, flags).show(transaction, "intentDialog");
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    @Override
    public long getGroupId(int group) {
        return mTasks.mChildren.get(group).mTaskId;
    }

    @Override
    public long getChildId(int group, int child) {
        return ((Node) getChild(group, child)).mName.hashCode();
    }

    private static class TaskViewHolder {
        TextView taskIdTextView, numActivitiesTextView;
        ImageView indicatorImageView;

        TaskViewHolder(View view) {
            indicatorImageView = view.findViewById(R.id.group_indicator);
            taskIdTextView = view.findViewById(R.id.task_id);
            numActivitiesTextView = view.findViewById(R.id.num_activities);
        }
    }
    private static class ActivityViewHolder {
        TextView activityNameTextView, activityNumTextView;
        ImageView labelImageView;
        ImageButton intentButtonView;

        ActivityViewHolder(View view) {
            activityNameTextView = view.findViewById(R.id.activity_name);
            activityNumTextView = view.findViewById(R.id.activity_label);
            labelImageView = view.findViewById(R.id.color_label);
            intentButtonView = view.findViewById(R.id.intent_button);
        }
    }
}

