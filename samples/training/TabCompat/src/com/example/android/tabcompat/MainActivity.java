/*
 * Copyright 2012 The Android Open Source Project
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

package com.example.android.tabcompat;

import com.example.android.tabcompat.lib.CompatTab;
import com.example.android.tabcompat.lib.CompatTabListener;
import com.example.android.tabcompat.lib.TabCompatActivity;
import com.example.android.tabcompat.lib.TabHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends TabCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        TabHelper tabHelper = getTabHelper();

        CompatTab photosTab = tabHelper.newTab("photos")
                .setText(R.string.tab_photos)
                .setIcon(R.drawable.ic_tab_photos)
                .setTabListener(new InstantiatingTabListener(this, PhotosFragment.class));
        tabHelper.addTab(photosTab);

        CompatTab videosTab = tabHelper.newTab("videos")
                .setText(R.string.tab_videos)
                .setIcon(R.drawable.ic_tab_videos)
                .setTabListener(new InstantiatingTabListener(this, VideosFragment.class));
        tabHelper.addTab(videosTab);
    }

    /**
     * Implementation of {@link CompatTabListener} to handle tab change events. This implementation
     * instantiates the specified fragment class with no arguments when its tab is selected.
     */
    public static class InstantiatingTabListener implements CompatTabListener {

        private final TabCompatActivity mActivity;
        private final Class mClass;

        /**
         * Constructor used each time a new tab is created.
         *
         * @param activity The host Activity, used to instantiate the fragment
         * @param cls      The class representing the fragment to instantiate
         */
        public InstantiatingTabListener(TabCompatActivity activity, Class<? extends Fragment> cls) {
            mActivity = activity;
            mClass = cls;
        }

        /* The following are each of the ActionBar.TabListener callbacks */
        @Override
        public void onTabSelected(CompatTab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            Fragment fragment = tab.getFragment();
            if (fragment == null) {
                // If not, instantiate and add it to the activity
                fragment = Fragment.instantiate(mActivity, mClass.getName());
                tab.setFragment(fragment);
                ft.add(android.R.id.tabcontent, fragment, tab.getTag());
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(fragment);
            }
        }

        @Override
        public void onTabUnselected(CompatTab tab, FragmentTransaction ft) {
            Fragment fragment = tab.getFragment();
            if (fragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(fragment);
            }
        }

        @Override
        public void onTabReselected(CompatTab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Do nothing.
        }
    }

    public static class PhotosFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            textView.setText(R.string.tab_photos);
            return textView;
        }
    }

    public static class VideosFragment extends Fragment {

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            TextView textView = new TextView(getActivity());
            textView.setGravity(Gravity.CENTER);
            textView.setText(R.string.tab_videos);
            return textView;
        }
    }
}
