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

package com.example.android.adaptertransition;

import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.transition.AutoTransition;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Main screen for AdapterTransition sample.
 */
public class AdapterTransitionFragment extends Fragment implements Transition.TransitionListener {

    /**
     * Since the transition framework requires all relevant views in a view hierarchy to be marked
     * with IDs, we use this ID to mark the root view.
     */
    private static final int ROOT_ID = 1;

    /**
     * A tag for saving state whether the mAbsListView is ListView or GridView.
     */
    private static final String STATE_IS_LISTVIEW = "is_listview";

    /**
     * This is where we place our AdapterView (ListView / GridView).
     */
    private FrameLayout mContent;

    /**
     * This is where we carry out the transition.
     */
    private FrameLayout mCover;

    /**
     * This list shows our contents. It can be ListView or GridView, and we toggle between them
     * using the transition framework.
     */
    private AbsListView mAbsListView;

    /**
     * This is our contents.
     */
    private MeatAdapter mAdapter;

    public static AdapterTransitionFragment newInstance() {
        return new AdapterTransitionFragment();
    }

    public AdapterTransitionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // If savedInstanceState is available, we restore the state whether the list is a ListView
        // or a GridView.
        boolean isListView;
        if (null == savedInstanceState) {
            isListView = true;
        } else {
            isListView = savedInstanceState.getBoolean(STATE_IS_LISTVIEW, true);
        }
        inflateAbsList(inflater, container, isListView);
        return inflater.inflate(R.layout.fragment_adapter_transition, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IS_LISTVIEW, mAbsListView instanceof ListView);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Retaining references for FrameLayouts that we use later.
        mContent = (FrameLayout) view.findViewById(R.id.content);
        mCover = (FrameLayout) view.findViewById(R.id.cover);
        // We are attaching the list to the screen here.
        mContent.addView(mAbsListView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_adapter_transition, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // We change the look of the icon every time the user toggles between list and grid.
        MenuItem item = menu.findItem(R.id.action_toggle);
        if (null != item) {
            if (mAbsListView instanceof ListView) {
                item.setIcon(R.drawable.ic_action_grid);
                item.setTitle(R.string.show_as_grid);
            } else {
                item.setIcon(R.drawable.ic_action_list);
                item.setTitle(R.string.show_as_list);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle: {
                toggle();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onTransitionStart(Transition transition) {
    }

    // BEGIN_INCLUDE(on_transition_end)
    @Override
    public void onTransitionEnd(Transition transition) {
        // When the transition ends, we remove all the views from the overlay and hide it.
        mCover.removeAllViews();
        mCover.setVisibility(View.INVISIBLE);
    }
    // END_INCLUDE(on_transition_end)

    @Override
    public void onTransitionCancel(Transition transition) {
    }

    @Override
    public void onTransitionPause(Transition transition) {
    }

    @Override
    public void onTransitionResume(Transition transition) {
    }

    /**
     * Inflate a ListView or a GridView with a corresponding ListAdapter.
     *
     * @param inflater The LayoutInflater.
     * @param container The ViewGroup that contains this AbsListView. The AbsListView won't be
     *                  attached to it.
     * @param inflateListView Pass true to inflate a ListView, or false to inflate a GridView.
     */
    private void inflateAbsList(LayoutInflater inflater, ViewGroup container,
                                boolean inflateListView) {
        if (inflateListView) {
            mAbsListView = (AbsListView) inflater.inflate(R.layout.fragment_meat_list,
                    container, false);
            mAdapter = new MeatAdapter(inflater, R.layout.item_meat_list);
        } else {
            mAbsListView = (AbsListView) inflater.inflate(R.layout.fragment_meat_grid,
                    container, false);
            mAdapter = new MeatAdapter(inflater, R.layout.item_meat_grid);
        }
        mAbsListView.setAdapter(mAdapter);
        mAbsListView.setOnItemClickListener(mAdapter);
    }

    /**
     * Toggle the UI between ListView and GridView.
     */
    private void toggle() {
        // We use mCover as the overlay on which we carry out the transition.
        mCover.setVisibility(View.VISIBLE);
        // This FrameLayout holds all the visible views in the current list or grid. We use this as
        // the starting Scene of the Transition later.
        FrameLayout before = copyVisibleViews();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mCover.addView(before, params);
        // Swap the actual list.
        swapAbsListView();
        // We also swap the icon for the toggle button.
        ActivityCompat.invalidateOptionsMenu(getActivity());
        // It is now ready to start the transition.
        mAbsListView.post(new Runnable() {
            @Override
            public void run() {
                // BEGIN_INCLUDE(transition_with_listener)
                Scene scene = new Scene(mCover, copyVisibleViews());
                Transition transition = new AutoTransition();
                transition.addListener(AdapterTransitionFragment.this);
                TransitionManager.go(scene, transition);
                // END_INCLUDE(transition_with_listener)
            }
        });
    }

    /**
     * Swap ListView with GridView, or GridView with ListView.
     */
    private void swapAbsListView() {
        // We save the current scrolling position before removing the current list.
        int first = mAbsListView.getFirstVisiblePosition();
        // If the current list is a GridView, we replace it with a ListView. If it is a ListView,
        // a GridView.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        inflateAbsList(inflater, (ViewGroup) mAbsListView.getParent(),
                mAbsListView instanceof GridView);
        mAbsListView.setAdapter(mAdapter);
        // We restore the scrolling position here.
        mAbsListView.setSelection(first);
        // The new list is ready, and we replace the existing one with it.
        mContent.removeAllViews();
        mContent.addView(mAbsListView);
    }

    /**
     * Copy all the visible views in the mAbsListView into a new FrameLayout and return it.
     *
     * @return a FrameLayout with all the visible views inside.
     */
    private FrameLayout copyVisibleViews() {
        // This is the FrameLayout we return afterwards.
        FrameLayout layout = new FrameLayout(getActivity());
        // The transition framework requires to set ID for all views to be animated.
        layout.setId(ROOT_ID);
        // We only copy visible views.
        int first = mAbsListView.getFirstVisiblePosition();
        int index = 0;
        while (true) {
            // This is one of the views that we copy. Note that the argument for getChildAt is a
            // zero-oriented index, and it doesn't usually match with its position in the list.
            View source = mAbsListView.getChildAt(index);
            if (null == source) {
                break;
            }
            // This is the copy of the original view.
            View destination = mAdapter.getView(first + index, null, layout);
            assert destination != null;
            destination.setId(ROOT_ID + first + index);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    source.getWidth(), source.getHeight());
            params.leftMargin = (int) source.getX();
            params.topMargin = (int) source.getY();
            layout.addView(destination, params);
            ++index;
        }
        return layout;
    }

}
