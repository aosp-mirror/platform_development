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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * This fragment displays a hierarchy of tasks and activities in an expandable list.
 */
public class TreeFragment extends Fragment {
    public static final String TREE_NODE = "com.example.android.NODE_TREE";
    public static final String FRAGMENT_TITLE = "com.example.android.TREE_FRAGMENT_TITLE";
    private Activity mActivity;
    private String mTitle;
    private ViewGroup mContainer;
    private BaseActivityViewModel mViewModel;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Bundle args = getArguments();
        if (args != null) {
            mTitle = args.getString(FRAGMENT_TITLE);
        }
        return inflater.inflate(R.layout.fragment_tree, container, false /* attachToRoot */);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = (new ViewModelProvider(getActivity(),
                new ViewModelProvider.NewInstanceFactory())).get(BaseActivityViewModel.class);

        mViewModel.getRefresh().observe(this, this::onResumeHelper);

    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.actOnFab(BaseActivityViewModel.FabAction.Show);
    }

    private void onResumeHelper(List<Tracking.Task> tasks) {
        mActivity = getActivity();
        LinearLayout treeLayout = (LinearLayout) getView();

        TextView titleView = treeLayout.findViewById(R.id.task_tree_title);
        RecyclerView recyclerView = treeLayout.findViewById(R.id.tree_recycler);
        if (mTitle != null) {
            titleView.setText(mTitle);
        }
        displayRecycler(tasks, recyclerView);
    }

    private void displayRecycler(List<Tracking.Task> tasks, RecyclerView recyclerView) {
        recyclerView.setHasFixedSize(true);
        recyclerView.setNestedScrollingEnabled(false);
        InlineAdapter adapter = new InlineAdapter(tasks, getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
    }

    /**
     * Expand a task group to show its child activities.
     *
     * @param i The index of the task to expand.
     */
    public void openTask(int i) {
        View taskView = mContainer.getChildAt(i);
        if (taskView != null) {
            taskView.callOnClick();
        }
    }
}

