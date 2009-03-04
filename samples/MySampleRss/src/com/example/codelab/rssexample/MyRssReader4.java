/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.codelab.rssexample;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;


public class MyRssReader4 extends Activity {

    ListView mRssList;
    Cursor mCur;
    RssCursorAdapter mAdap;
    private static final int ADD_ELEMENT_REQUEST = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        // Load screen layout.
        setContentView(R.layout.main_screen2);
       
         // Populate ArrayAdapter and bind it to ListView
        mRssList = (ListView)findViewById(R.id.rssListView);
        
        mCur = managedQuery(RssContentProvider.CONTENT_URI, // Query for all items.
                            null, 
                            null, 
                            RssContentProvider.DEFAULT_SORT_ORDER);
// BEGIN_INCLUDE(4_1)                           
        mAdap = new RssCursorAdapter(
                this,
                R.layout.list_element,                  // Our layout resource.
                mCur, 
                new String[]{RssContentProvider.TITLE}, // Columns to retrieve.
                new int[]{R.id.list_item});             // IDs of widgets to display
        mRssList.setAdapter(mAdap);                     //    the corresponding column.
// END_INCLUDE(4_1)
        
        // Set the last selected item.
        // (icicle is only set if this is being restarted).
        if(savedInstanceState != null && savedInstanceState.containsKey("lastIndexItem")){
            mRssList.setSelection(savedInstanceState.getInteger("lastIndexItem"));
        }
    }
   
    // Store our state before we are potentially bumped from memory.
    // We'd like to store the current ListView selection.
    @Override
    protected void onSaveInstanceState(Bundle outState){
        int index = mRssList.getSelectedItemIndex();
        if(index > -1){
            outState.putInteger("lastIndexItem", index);
        }
    }
    
    
    // Add our initial menu options. We will tweak this menu when it's loaded swap out 
    // "start service" or "stop service", depending on whether the service is currently running.
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Always call the superclass implementation to 
        // provide standard items.
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, 0, R.string.menu_option_start, null);
        menu.add(0, 1, R.string.menu_option_stop, null);
        menu.add(0, 2, R.string.menu_option_add, null);
        menu.add(0, 3, R.string.menu_option_delete, null);
        menu.add(0, 4, R.string.menu_option_update, null);
        
        return true;
    }
    
    // Toggle out start service/stop service depending on whether the service is running.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        return true;
    }
    
    // Handle our menu clicks.
    @Override
    public boolean onOptionsItemSelected(Menu.Item item){
        super.onOptionsItemSelected(item);
        
        switch (item.getId()){
            case 0:     // Start service
                showAlert(null, "You clicked 'start'!", "ok", null, false, null);
                break;
            case 1:    // Stop service
              showAlert(null, "You clicked stop!", "ok", null, false, null);
              break;                    
            case 2:     // Add Item
                Intent addIntent = new Intent(AddRssItem.class);

                // Use an ID so that if we create a "remove item" form we
                // can tell which form is returning a value.
                startActivityForResult(addIntent, ADD_ELEMENT_REQUEST); 
                break; 
            case 3:     // Delete item.
                if(mRssList.hasFocus()){
                  int currentSelectionIndex = mRssList.getSelectedItemIndex();

                  // Create our content URI by adding the ID of the currently selected item using a 
                  // convenience method.
                  Long itemID = mAdap.getItemId(currentSelectionIndex);
                  getContentResolver().delete(RssContentProvider.CONTENT_URI.addId(itemID), null);
                }
                break;
            case 4:    // Update all
                showAlert(null, "You clicked 'Update'!", "ok", null, false, null);
                break;
            default:
                showAlert(null, "I have no idea what you clicked!", "ok", null, false, null);
                break;
        }
        return true;
    }
    
    // Called by the "Add RSS Item" floating screen when it closes.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case ADD_ELEMENT_REQUEST:
                    ContentValues vals = new ContentValues(4);
                    vals.put(RssContentProvider.TITLE, data.getStringExtra(RssContentProvider.TITLE));
                    vals.put(RssContentProvider.URL, data.getStringExtra(RssContentProvider.URL));
                    vals.put(RssContentProvider.CONTENT, data.getStringExtra(RssContentProvider.CONTENT));
                    vals.put(RssContentProvider.LAST_UPDATED, data.getIntExtra(RssContentProvider.LAST_UPDATED, 0));
                    Uri uri = getContentResolver().insert(
                            RssContentProvider.CONTENT_URI, 
                            vals);
                        if(uri != null){
                            mRssList.setSelection(mRssList.getCount() - 1);
                        }
                    break;
                default:
                    break;
            }
        }
    }
    
    // Our private ArrayAdapter implementation that returns a bold TextView for 
    // RSS items that are unread, or a normal TextView for items that have been read.
    private class RssCursorAdapter extends SimpleCursorAdapter {
        public RssCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to){
            super(context, layout, c, from, to);
        }
        
        // Here's our only important override--returning the list item.
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            TextView view = (TextView)super.getView(position, convertView, parent);
            
            if(view != null){
                
                // Now get the hasBeenRead value to determine the font.
                int hasBeenReadColumnIndex = getCursor().getColumnIndex(
                        RssContentProvider.HAS_BEEN_READ);
                boolean hasBeenRead = (getCursor().getInt(hasBeenReadColumnIndex) == 1 ? true : false);
                if(! hasBeenRead){
                    Typeface type = view.getTypeface();
                    view.setTypeface(Typeface.create(type, Typeface.BOLD_ITALIC));
                }
            }
            return view;
        } 
    }
}

