/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.customtransition;

import com.example.android.common.logger.Log;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class CustomTransitionFragment extends Fragment implements View.OnClickListener {

    private static final String STATE_CURRENT_SCENE = "current_scene";

    /** Tag for the logger */
    private static final String TAG = "CustomTransitionFragment";

    /** These are the Scenes we use. */
    private Scene[] mScenes;

    /** The current index for mScenes. */
    private int mCurrentScene;

    /** This is the custom Transition we use in this sample. */
    private Transition mTransition;

    public CustomTransitionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_custom_transition, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Context context = getActivity();
        FrameLayout container = (FrameLayout) view.findViewById(R.id.container);
        view.findViewById(R.id.show_next_scene).setOnClickListener(this);
        if (null != savedInstanceState) {
            mCurrentScene = savedInstanceState.getInt(STATE_CURRENT_SCENE);
        }
        // We set up the Scenes here.
        mScenes = new Scene[] {
                Scene.getSceneForLayout(container, R.layout.scene1, context),
                Scene.getSceneForLayout(container, R.layout.scene2, context),
                Scene.getSceneForLayout(container, R.layout.scene3, context),
        };
        // This is the custom Transition.
        mTransition = new ChangeColor();
        // Show the initial Scene.
        TransitionManager.go(mScenes[mCurrentScene % mScenes.length]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_SCENE, mCurrentScene);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.show_next_scene: {
                mCurrentScene = (mCurrentScene + 1) % mScenes.length;
                Log.i(TAG, "Transitioning to scene #" + mCurrentScene);
                // Pass the custom Transition as second argument for TransitionManager.go
                TransitionManager.go(mScenes[mCurrentScene], mTransition);
                break;
            }
        }
    }

}
