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

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts.Phones;
import android.util.Log;
import android.view.View;
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
    
    public static class ExampleComplexListFragment extends ListFragment
            implements LoaderManager.LoaderCallbacks<Cursor> {
        boolean mInitializing;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            mInitializing = true;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            
            // Assume we don't yet have data to display.
            setListShown(false, false);
            
            // Give some text to display if there is no data.  In a real
            // application this would come from a resource.
            setEmptyText("No phone numbers");
            
            // Check if we already have content for the list.
            Loader<Cursor> ld = getLoaderManager().getLoader(0);
            if (ld == null) {
                // No loader started yet...  do it now.
                ld = getLoaderManager().startLoading(0, null, this);
            } else {
                // Already have a loader -- poke it to report its cursor
                // if it already has one.  This will immediately call back
                // to us for us to update the list right now.
                ld.startLoading();
            }
            mInitializing = false;
        }
        
        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            Log.i("FragmentComplexList", "Item clicked: " + id);
        }
        
        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Phones.CONTENT_URI, null, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            ListAdapter adapter = new SimpleCursorAdapter(getActivity(),
                    android.R.layout.simple_list_item_2, data, 
                            new String[] { Phones.NAME, Phones.NUMBER }, 
                            new int[] { android.R.id.text1, android.R.id.text2 });
            setListAdapter(adapter);
            setListShown(true, !mInitializing);
        }
    }
}
