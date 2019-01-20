/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.android.intentplayground.BaseActivity.Mode;
import com.example.android.intentplayground.IntentBuilderView.OnLaunchCallback;

public class LaunchFragment extends Fragment {
    private IntentBuilderView mIntentBuilderView;
    private BaseActivityViewModel mViewModel;
    private OnLaunchCallback mOnLaunchCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mIntentBuilderView = new IntentBuilderView(getContext(), Mode.LAUNCH);
        FragmentActivity activity = getActivity();
        if (activity instanceof OnLaunchCallback) {
            mOnLaunchCallback = (OnLaunchCallback) activity;
        }

        setHasOptionsMenu(true);
        return mIntentBuilderView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel = (new ViewModelProvider(getActivity(),
                new ViewModelProvider.NewInstanceFactory())).get(BaseActivityViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.actOnFab(BaseActivityViewModel.FabAction.Hide);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.launch_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.app_bar_launch && mOnLaunchCallback != null) {
            Intent intent = mIntentBuilderView.currentIntent();
            boolean forResult = mIntentBuilderView.startForResult();
            mOnLaunchCallback.launchActivity(mIntentBuilderView.currentIntent(), forResult);
        }

        return super.onOptionsItemSelected(item);
    }
}
