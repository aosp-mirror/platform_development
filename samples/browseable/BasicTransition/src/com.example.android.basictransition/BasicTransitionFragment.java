/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.basictransition;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Scene;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

public class BasicTransitionFragment extends Fragment
        implements RadioGroup.OnCheckedChangeListener {

    // We transition between these Scenes
    private Scene mScene1;
    private Scene mScene2;
    private Scene mScene3;

    /** A custom TransitionManager */
    private TransitionManager mTransitionManagerForScene3;

    /** Transitions take place in this ViewGroup. We retain this for the dynamic transition on scene 4. */
    private ViewGroup mSceneRoot;

    public static BasicTransitionFragment newInstance() {
        return new BasicTransitionFragment();
    }

    public BasicTransitionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_basic_transition, container, false);
        assert view != null;
        RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.select_scene);
        radioGroup.setOnCheckedChangeListener(this);
        mSceneRoot = (ViewGroup) view.findViewById(R.id.scene_root);

        // BEGIN_INCLUDE(instantiation_from_view)
        // A Scene can be instantiated from a live view hierarchy.
        mScene1 = new Scene(mSceneRoot, (ViewGroup) mSceneRoot.findViewById(R.id.container));
        // END_INCLUDE(instantiation_from_view)

        // BEGIN_INCLUDE(instantiation_from_resource)
        // You can also inflate a generate a Scene from a layout resource file.
        mScene2 = Scene.getSceneForLayout(mSceneRoot, R.layout.scene2, getActivity());
        // END_INCLUDE(instantiation_from_resource)

        // Another scene from a layout resource file.
        mScene3 = Scene.getSceneForLayout(mSceneRoot, R.layout.scene3, getActivity());

        // BEGIN_INCLUDE(custom_transition_manager)
        // We create a custom TransitionManager for Scene 3, in which ChangeBounds and Fade
        // take place at the same time.
        mTransitionManagerForScene3 = TransitionInflater.from(getActivity())
                .inflateTransitionManager(R.transition.scene3_transition_manager, mSceneRoot);
        // END_INCLUDE(custom_transition_manager)

        return view;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.select_scene_1: {
                // BEGIN_INCLUDE(transition_simple)
                // You can start an automatic transition with TransitionManager.go().
                TransitionManager.go(mScene1);
                // END_INCLUDE(transition_simple)
                break;
            }
            case R.id.select_scene_2: {
                TransitionManager.go(mScene2);
                break;
            }
            case R.id.select_scene_3: {
                // BEGIN_INCLUDE(transition_custom)
                // You can also start a transition with a custom TransitionManager.
                mTransitionManagerForScene3.transitionTo(mScene3);
                // END_INCLUDE(transition_custom)
                break;
            }
            case R.id.select_scene_4: {
                // BEGIN_INCLUDE(transition_dynamic)
                // Alternatively, transition can be invoked dynamically without a Scene.
                // For this, we first call TransitionManager.beginDelayedTransition().
                TransitionManager.beginDelayedTransition(mSceneRoot);
                // Then, we can just change view properties as usual.
                View square = mSceneRoot.findViewById(R.id.transition_square);
                ViewGroup.LayoutParams params = square.getLayoutParams();
                int newSize = getResources().getDimensionPixelSize(R.dimen.square_size_expanded);
                params.width = newSize;
                params.height = newSize;
                square.setLayoutParams(params);
                // END_INCLUDE(transition_dynamic)
                break;
            }
        }
    }

}
