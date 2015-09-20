/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.fragmenttransition;

import com.example.android.common.logger.Log;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.transition.Scene;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailFragment extends Fragment implements Animation.AnimationListener {

    private static final String TAG = "DetailFragment";

    private static final String ARG_RESOURCE_ID = "resource_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_X = "x";
    private static final String ARG_Y = "y";
    private static final String ARG_WIDTH = "width";
    private static final String ARG_HEIGHT = "height";

    /**
     * Create a new instance of DetailFragment.
     *
     * @param resourceId The resource ID of the Drawable image to show
     * @param title The title of the image
     * @param x The horizontal position of the grid item in pixel
     * @param y The vertical position of the grid item in pixel
     * @param width The width of the grid item in pixel
     * @param height The height of the grid item in pixel
     * @return a new instance of DetailFragment
     */
    public static DetailFragment newInstance(int resourceId, String title,
                                             int x, int y, int width, int height) {
        DetailFragment fragment = new DetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_RESOURCE_ID, resourceId);
        args.putString(ARG_TITLE, title);
        args.putInt(ARG_X, x);
        args.putInt(ARG_Y, y);
        args.putInt(ARG_WIDTH, width);
        args.putInt(ARG_HEIGHT, height);
        fragment.setArguments(args);
        return fragment;
    }

    public DetailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        FrameLayout root = (FrameLayout) view;
        Context context = view.getContext();
        assert context != null;
        // This is how the fragment looks at first. Since the transition is one-way, we don't need to make
        // this a Scene.
        View item = LayoutInflater.from(context).inflate(R.layout.item_meat_grid, root, false);
        assert item != null;
        bind(item);
        // We adjust the position of the initial image with LayoutParams using the values supplied
        // as the fragment arguments.
        Bundle args = getArguments();
        FrameLayout.LayoutParams params = null;
        if (args != null) {
            params = new FrameLayout.LayoutParams(
                    args.getInt(ARG_WIDTH), args.getInt(ARG_HEIGHT));
            params.topMargin = args.getInt(ARG_Y);
            params.leftMargin = args.getInt(ARG_X);
        }
        root.addView(item, params);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Bind the views inside of parent with the fragment arguments.
     *
     * @param parent The parent of views to bind.
     */
    private void bind(View parent) {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        ImageView image = (ImageView) parent.findViewById(R.id.image);
        image.setImageResource(args.getInt(ARG_RESOURCE_ID));
        TextView title = (TextView) parent.findViewById(R.id.title);
        title.setText(args.getString(ARG_TITLE));
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = AnimationUtils.loadAnimation(getActivity(),
                enter ? android.R.anim.fade_in : android.R.anim.fade_out);
        // We bind a listener for the fragment transaction. We only bind it when
        // this fragment is entering.
        if (animation != null && enter) {
            animation.setAnimationListener(this);
        }
        return animation;
    }

    @Override
    public void onAnimationStart(Animation animation) {
        // This method is called at the end of the animation for the fragment transaction.
        // There is nothing we need to do in this sample.
    }

    @Override
    public void onAnimationEnd(Animation animation) {
        // This method is called at the end of the animation for the fragment transaction,
        // which is perfect time to start our Transition.
        Log.i(TAG, "Fragment animation ended. Starting a Transition.");
        final Scene scene = Scene.getSceneForLayout((ViewGroup) getView(),
                R.layout.fragment_detail_content, getActivity());
        TransitionManager.go(scene);
        // Note that we need to bind views with data after we call TransitionManager.go().
        bind(scene.getSceneRoot());
    }

    @Override
    public void onAnimationRepeat(Animation animation) {
        // This method is never called in this sample because the animation doesn't repeat.
    }

}
