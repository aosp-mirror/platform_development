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
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.logging.Logger;

public class MyRssReader5 extends Activity implements OnItemSelectedListener {
    
    private ListView mRssList;
    private Cursor mCur;
    private RssCursorAdapter mAdap;
    private WebView mWebView;
    private static final int ADD_ELEMENT_REQUEST = 1;
    private Logger mLogger = Logger.getLogger(this.getPackageName());
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
                                                                                                    //
        // Load screen layout.
        setContentView(R.layout.main_screen2);
       
        // Populate ArrayAdapter and bind it to ListView
        mRssList = (ListView)findViewById(R.id.rssListView);
        mRssList.setOnItemSelectedListener(this);
        
        mWebView = (WebView)findViewById(R.id.rssWebView);
        
        mCur = managedQuery(RssContentProvider.CONTENT_URI, // Query for all items.
                       null, 
                       null, 
                       RssContentProvider.DEFAULT_SORT_ORDER);
               
        mAdap = new RssCursorAdapter(
                this,
                R.layout.list_element,                  // Our layout resource.
                mCur, 
                new String[]{RssContentProvider.TITLE}, // Columns to retrieve.
                new int[]{R.id.list_item});             // IDs of widgets to display 
        mRssList.setAdapter(mAdap);                    //      the corresponding column.
        
        // Set the last selected item.
        // (icicle is only set if this is being restarted).
        if(savedInstanceState != null && savedInstanceState.containsKey("lastIndexItem")){
            mRssList.setSelection(savedInstanceState.getInteger("lastIndexItem"));
        }
    }

//BEGIN_INCLUDE(5_4)
    // Listener to listen for list selection changes, and send the new text to
    // the web view.
    public void onItemSelected(AdapterView parent, View v, int position, long id){
        // Need to nest this in a try block because we get called sometimes
        // with the index of a recently deleted item.
        String content = "";
        try{
            content = mCur.getString(mCur.getColumnIndex(RssContentProvider.CONTENT));
            mLogger.info("MyRssReader5 content string:" + content);
        }
        catch (Exception e){
            // This method is sometimes called after a backing data item
            // is deleted. In that case, we don't want to throw an error.
            mLogger.warning("MyRssReader5.onItemSelected() couldn't get the content" +
                            "from the cursor " + e.getMessage()); 
        }
        mWebView.loadData(content, "text/html", null);
    }
//END_INCLUDE(5_4)
    
    public void onNothingSelected(AdapterView parent){
        mWebView.loadData("<html><body><p>No selection chosen</p></body></html>", "text/html", null);
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
                Intent basicStartIntent = new Intent(RssService.class);
                startService(basicStartIntent);
                break;
            case 1:    // Stop service
                Intent stopIntent = new Intent(RssService.class);
                stopService(stopIntent);
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
                    mLogger.info("MyRssReader5.onOptionsItemSelected(): Deleting list member:" + 
                            mRssList.getSelectedItemIndex());
                    // Create our content URI by adding the ID of the currently selected item using a 
                    // convenience method.
                    Long itemID = mAdap.getItemId(currentSelectionIndex);
                    getContentResolver().delete(RssContentProvider.CONTENT_URI.addId(itemID), null);
                }
                break;
            case 4:     // Requery all
                Bundle startupVals = new Bundle(1);
                startupVals.putBoolean(RssService.REQUERY_KEY, true);
                Intent requeryIntent = new Intent(RssService.class);
                startService(requeryIntent, startupVals);
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
                      ContentValues vals = new ContentValues(5);
                      vals.put(RssContentProvider.TITLE, data.getStringExtra(RssContentProvider.TITLE));
                      vals.put(RssContentProvider.URL, data.getStringExtra(RssContentProvider.URL));
                      vals.put(RssContentProvider.CONTENT, data.getStringExtra(RssContentProvider.CONTENT));
                      vals.put(RssContentProvider.LAST_UPDATED, data.getIntExtra(RssContentProvider.LAST_UPDATED, 0));
                      Uri uri = getContentResolver().insert(
                              RssContentProvider.CONTENT_URI, 
                              vals);
                      if(uri != null){
                          // Tell the service to requery the service, then set
                          // it as the active selection.
                          Bundle startupVals = new Bundle(1);
                          startupVals.putString(RssService.RSS_URL, data.getStringExtra("URL"));
                          Intent startIntent = new Intent(RssService.class);
                          startIntent.putExtras(startupVals);
                          startService(startIntent);
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
        public RssCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
        }
        
        // Here's our only important override--returning the list item.
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            TextView view = (TextView)super.getView(position, convertView, parent);
            
            if(view != null){
                
                // Now get the hasBeenRead value to determine the font.
                int hasBeenReadColumnIndex = getCursor().getColumnIndex(RssContentProvider.HAS_BEEN_READ);
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

