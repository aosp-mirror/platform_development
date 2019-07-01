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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.function.Consumer;

public class BaseActivityViewModel extends ViewModel {
    enum FabAction {Show, Hide}

    private final MutableLiveData<FabAction> mFabActions;

    /**
     * Republish {@link BaseActivity#addTrackerListener(Consumer)} in a lifecycle safe manner.
     * The {@link BaseActivity#addTrackerListener(Consumer)} that is registered in this class,
     * forwards the value to this, to enjoy all the guarantees {@link import
     * androidx.lifecycle.LiveData;} gives.
     */
    private final MutableLiveData<List<Tracking.Task>> mRefreshTree;
    private final Consumer<List<Tracking.Task>> mTrackingListener;

    public BaseActivityViewModel() {
        mFabActions = new MutableLiveData<>();
        mRefreshTree = new MutableLiveData<>();
        mTrackingListener = tasks -> mRefreshTree.setValue(tasks);
        BaseActivity.addTrackerListener(mTrackingListener);
    }


    public void actOnFab(FabAction action) {
        mFabActions.setValue(action);
    }

    public LiveData<FabAction> getFabActions() {
        return mFabActions;
    }

    /**
     * @return LiveData that publishes the new state of {@link com.android.server.wm.Task} and
     * {@link android.app.Activity}-s whenever that state has been changed.
     */
    public LiveData<List<Tracking.Task>> getRefresh() {
        return mRefreshTree;
    }

    @Override
    public void onCleared() {
        BaseActivity.removeTrackerListener(mTrackingListener);
    }
}

