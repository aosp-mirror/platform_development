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

package com.example.android.apis.preference;

import com.example.android.apis.R;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Demonstration of PreferenceFragment, splitting the activity into a
 * list categories and preferences.
 */
//BEGIN_INCLUDE(activity)
public class FragmentPreferences extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The content for this activity places CategoriesFragment to one
        // side, and leaves the rest to dynamically add the prefs fragment.
        setContentView(R.layout.fragment_preferences);
    }

    /**
     * This fragment shows a list of categories the user can pick.  When they
     * pick one, the corresponding preferences fragment is created and shown.
     */
    public static class CategoriesFragment extends ListFragment {
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setListAdapter(new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1,
                    new String[] { "Prefs 1", "Prefs 2" }));
            switchPreferences(0);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            switchPreferences(position);
        }

        /**
         * Show the given preferences, replacing whatever was last shown.
         */
        void switchPreferences(int which) {
            Fragment f = which == 0 ? new Prefs1Fragment() : new Prefs2Fragment();
            getActivity().openFragmentTransaction().replace(R.id.prefs, f).commit();
        }
    }

//BEGIN_INCLUDE(fragment)
    public static class Prefs1Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
//END_INCLUDE(fragment)

    public static class Prefs2Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference_dependencies);
        }
    }
}
//END_INCLUDE(activity)
