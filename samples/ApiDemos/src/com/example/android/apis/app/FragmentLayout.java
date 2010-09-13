/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.example.android.apis.app;

import com.example.android.apis.R;
import com.example.android.apis.Shakespeare;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Demonstration of using fragments to implement different activity layouts.
 * This sample provides a different layout (and activity flow) when run in
 * landscape.
 */
public class FragmentLayout extends Activity {

//BEGIN_INCLUDE(main)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.fragment_layout);
    }
//END_INCLUDE(main)

    /**
     * This is a secondary activity, to show what the user has selected
     * when the screen is not large enough to show it all in one activity.
     */
//BEGIN_INCLUDE(details_activity)
    public static class DetailsActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE) {
                // If the screen is now in landscape mode, we can show the
                // dialog in-line with the list so we don't need this activity.
                finish();
                return;
            }

            if (savedInstanceState == null) {
                // During initial setup, plug in the details fragment.
                DetailsFragment details = new DetailsFragment();
                getFragmentManager().openTransaction().add(android.R.id.content, details).commit();
                details.setText(getIntent().getIntExtra("text", -1));
            }
        }
    }
//END_INCLUDE(details_activity)

//BEGIN_INCLUDE(titles)
    public static class TitlesFragment extends ListFragment {
        DetailsFragment mDetails;
        int mCurCheckPosition = 0;

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Populate list with our static array of titles.
            setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_activated_1, Shakespeare.TITLES));

            // Restore last state for checked position.
            if (savedInstanceState != null) {
                mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
            }

            // If we are showing details in the screen, set up the list to highlight.
            mDetails = (DetailsFragment)getFragmentManager().findFragmentById(R.id.details);
            if (mDetails != null && mDetails.isInLayout()) {
                getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                getListView().setItemChecked(mCurCheckPosition, true);
                mDetails.setText(mCurCheckPosition);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("curChoice", mCurCheckPosition);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            mCurCheckPosition = position;

            if (mDetails != null && mDetails.isVisible()) {
                // If the activity has a fragment to display the dialog,
                // point it to what the user has selected.
                mDetails.setText(position);
                getListView().setItemChecked(position, true);
            } else {
                // Otherwise we need to launch a new activity to display
                // the dialog fragment with selected text.
                Intent intent = new Intent();
                intent.setClass(getActivity(), DetailsActivity.class);
                intent.putExtra("text", position);
                startActivity(intent);
            }
        }
    }
//END_INCLUDE(titles)

//BEGIN_INCLUDE(details)
    public static class DetailsFragment extends Fragment {
        int mDisplayedText = -1;
        TextView mText;

        public void setText(int id) {
            mDisplayedText = id;
            if (mText != null && id >= 0) {
                mText.setText(Shakespeare.DIALOGUE[id]);
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mDisplayedText = savedInstanceState.getInt("text", -1);
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt("text", mDisplayedText);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            ScrollView scroller = new ScrollView(getActivity());
            mText = new TextView(getActivity());
            int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    4, getActivity().getResources().getDisplayMetrics());
            mText.setPadding(padding, padding, padding, padding);
            scroller.addView(mText);
            if (mDisplayedText >= 0) {
                mText.setText(Shakespeare.DIALOGUE[mDisplayedText]);
            }
            return scroller;
        }
    }
//END_INCLUDE(details)
}
