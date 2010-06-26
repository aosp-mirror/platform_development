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

import com.example.android.apis.Shakespeare;

import android.app.Activity;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.LoaderManagingFragment;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts.Phones;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

/**
 * Demonstration of more complex use if a ListFragment, including showing
 * an empty view and loading progress.
 */
public class FragmentComplexList extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create the list fragment and add it as our sole content.
        if (findFragmentById(android.R.id.content) == null) {
            ExampleComplexListFragment list = new ExampleComplexListFragment();
            openFragmentTransaction().add(android.R.id.content, list).commit();
        }
    }
    
    public static class ContactsLoader extends LoaderManagingFragment<Cursor> {
        ExampleComplexListFragment mListFragment;
        
        void setListFragment(ExampleComplexListFragment fragment) {
            mListFragment = fragment;
        }
        
        @Override
        protected Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Phones.CONTENT_URI, null, null, null, null);
        }

        @Override
        protected void onInitializeLoaders() {
        }

        @Override
        protected void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mListFragment.loadFinished(loader, data);
        }
    }
    
    public static class ExampleComplexListFragment extends ListFragment {
        ContactsLoader mLoader;
        boolean mInitializing;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            mInitializing = true;
            
            // Make sure we have the loader for our content.
            mLoader = (ContactsLoader)getActivity().findFragmentByTag(
                    ContactsLoader.class.getName());
            if (mLoader == null) {
                mLoader = new ContactsLoader();
                getActivity().openFragmentTransaction().add(mLoader,
                        ContactsLoader.class.getName()).commit();
            }
            mLoader.setListFragment(this);
        }

        @Override
        public void onReady(Bundle savedInstanceState) {
            super.onReady(savedInstanceState);
            
            // Assume we don't yet have data to display.
            setListShown(false, false);
            
            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText("No phone numbers");
            
            // Check if we already have content for the list.
            Loader<Cursor> ld = mLoader.getLoader(0);
            if (ld == null) {
                // No loader started yet...  do it now.
                ld = mLoader.startLoading(0, null);
            } else {
                // Already have a loader -- poke it to report its cursor
                // if it already has one.  This will immediately call back
                // to us for us to update the list right now.
                ld.startLoading();
            }
            mInitializing = false;
        }
        
        void loadFinished(Loader<Cursor> loader, Cursor data) {
            ListAdapter adapter = new SimpleCursorAdapter(getActivity(),
                    android.R.layout.simple_list_item_2, data, 
                            new String[] { Phones.NAME, Phones.NUMBER }, 
                            new int[] { android.R.id.text1, android.R.id.text2 });
            setListAdapter(adapter);
            setListShown(true, !mInitializing);
        }
        
        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("FragmentComplexList", "Item clicked: " + id);
        }
        
        @Override
        public void onDestroy() {
            mLoader.setListFragment(this);
            super.onDestroy();
        }
    }
}
