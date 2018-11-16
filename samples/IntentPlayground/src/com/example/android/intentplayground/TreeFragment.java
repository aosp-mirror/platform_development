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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

/**
 * This fragment displays a hierarchy of tasks and activities in an expandable list.
 */
public class TreeFragment extends Fragment {
    public static final String TREE_NODE = "com.example.android.NODE_TREE";
    public static final String FRAGMENT_TITLE = "com.example.android.TREE_FRAGMENT_TITLE";
    private Activity mActivity;
    private Node mTree;
    private String mTitle;
    private ViewGroup mContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
             Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mTree = args.getParcelable(TREE_NODE);
            mTitle = args.getString(FRAGMENT_TITLE);
        }
        return inflater.inflate(R.layout.fragment_tree, container, false /* attachToRoot */);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity = getActivity();
        LinearLayout treeLayout = (LinearLayout) getView();
        LinearLayout treeView = treeLayout.findViewById(R.id.task_tree);
        mContainer = treeView;
        TextView titleView = treeLayout.findViewById(R.id.task_tree_title);
        if (mTitle != null) {
            titleView.setText(mTitle);
        }
        if (mTree != null) {
            displayHierarchy(mTree, treeView);
        } else {
            displayHierarchy(TestBase.describeTaskHierarchy(mActivity), treeView);
        }
    }

    /**
     * Takes a Node and creates views corresponding to the task hierarchy
     * @param tree a {@link Node} that models the task hierarchy
     * @param container the {@link LinearLayout} in which to display them
     */
    protected void displayHierarchy(Node tree, LinearLayout container) {
        ExpandableAdapter adapter = new ExpandableAdapter(getActivity(), tree);
        View view;
        // fill container
        container.removeAllViews();
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            view = makeCompositeView(adapter, container, i);
            if (view != null) container.addView(view);
        }
    }

    private View makeCompositeView(ExpandableAdapter adapter, ViewGroup parent, int group) {
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout compositeLayout = (LinearLayout) inflater
                .inflate(R.layout.tree_node_composite, parent, false /* attachToRoot */);
        LinearLayout parentLayout = compositeLayout.findViewById(R.id.group_item);
        LinearLayout childLayout = compositeLayout.findViewById(R.id.child_item);
        LinearLayout buttonBarlayout = compositeLayout.findViewById(R.id.move_task_to_front_bar);
        if (adapter.getChildrenCount(group) == 0) {
            return null;
        }
        parentLayout.addView(adapter.getGroupView(group, false /* isExpanded */,
                null /* convertView */, parent));
        for (int i = 0; i < adapter.getChildrenCount(group); i++) {
            childLayout.addView(adapter.getChildView(group, i, false /* isLastChild */,
                    null /* convertView */, parentLayout));
        }
        compositeLayout.setOnClickListener(view -> {
            LinearLayout childView1 = view.findViewById(R.id.child_item);
            childView1.setVisibility(childView1.getVisibility() == View.GONE ?
                    View.VISIBLE : View.GONE);
            if (group > 0) {
                buttonBarlayout.setVisibility(buttonBarlayout.getVisibility() == View.GONE ?
                        View.VISIBLE : View.GONE);
            }
            parentLayout.removeAllViews();
            parentLayout.addView(adapter.getGroupView(group,
                    !(childView1.getVisibility() == View.GONE), null /* convertView */, parent));
        });
        // Set a no-op childView click listener so the event doesn't bubble up to the composite view
        childLayout.setOnClickListener(view -> {});
        //Set onclick listener for button bar
        int taskId = ((Node) adapter.getGroup(group)).mTaskId;
        if (group == 0) {
            // hide the button bar, it is the current task
            buttonBarlayout.setVisibility(View.GONE);
        } else {
            int color = mActivity.getResources()
                    .getColor(ColorManager.getColorForTask(taskId), null /* theme */);
            Button moveTaskButton = buttonBarlayout.findViewById(R.id.move_task_to_front_button);
            moveTaskButton.setOnClickListener(view -> moveTaskToFront(taskId));
            moveTaskButton.setTextColor(color);
            Button removeTaskButton = buttonBarlayout.findViewById(R.id.kill_task_button);
            removeTaskButton.setOnClickListener(view -> removeTask(taskId));
            removeTaskButton.setTextColor(color);
        }
        return compositeLayout;
    }

    private void removeTask(int taskId) {
        ActivityManager am = mActivity.getSystemService(ActivityManager.class);
        am.getAppTasks().forEach(task -> {
            if (task.getTaskInfo().persistentId == taskId) {
                task.finishAndRemoveTask();
            }
        });
        onResume(); // manually trigger UI refresh
    }

    private void moveTaskToFront(int taskId) {
        ActivityManager am = mActivity.getSystemService(ActivityManager.class);
        am.moveTaskToFront(taskId, 0);
    }

    /**
     * Expand a task group to show its child activities.
     * @param i The index of the task to expand.
     */
    public void openTask(int i) {
        View taskView = mContainer.getChildAt(i);
        if (taskView != null) taskView.callOnClick();
    }
}

