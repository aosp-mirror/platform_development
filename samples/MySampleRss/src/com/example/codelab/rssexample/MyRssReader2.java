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
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
public class MyRssReader2 extends Activity{
    private ArrayList<RssItem> mFeeds = null;
    ListView mRssList = null;
    private Logger mLogger = Logger.getLogger("com.example.codelab.rssexample");
    
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        // Load screen layout.
        setContentView(R.layout.main_screen2);
        
       // Populate our list
        mFeeds = initializeList();
        mLogger.info("MyRssReader.onCreate-1  mFeeds value:" + mFeeds.size());
// BEGIN_INCLUDE(2_2)       
        // Populate ArrayAdapter and bind it to ListView
        mRssList = (ListView)findViewById(R.id.rssListView);
        if(mRssList == null){
            // Note: Calling showAlert() would fail here because dialogs opened
            // in onCreate won't be displayed properly, if at all.
            mLogger.warning("MyRssReader.onCreate-2 -- Couldn't instantiate a ListView!");
        }
        RssDataAdapter<RssItem> adap = new RssDataAdapter<RssItem>(this, R.layout.add_item, mFeeds);
        if(adap == null){
            mLogger.warning("MyRssReader.onCreate-3 -- Couldn't instantiate RssDataAdapter!");
        }
        if(mFeeds == null){
            mLogger.warning("MyRssReader.onCreate-4 -- Couldn't instantiate a ListView!");
        }
        mRssList.setAdapter(adap);   
// END_INCLUDE(2_2)
       
        mLogger.info("MyRssReader.onCreate-5 -- Loading preferences.");
        // Set the last selected item.
        // (icicle is only set if this is being restarted).
        if(savedInstanceState != null && savedInstanceState.containsKey("lastIndexItem"))
        {
            Integer selectedItem = savedInstanceState.getInteger("lastIndexItem");
            if(selectedItem >= 0 && selectedItem < mRssList.getChildCount()){
                mRssList.setSelection(savedInstanceState.getInteger("lastIndexItem"));
            }
            mLogger.info("MyRssReader.onCreate-6 -- Last selected item:" + selectedItem);
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
        
        menu.add(0, 0, "Start RSS Service", null);
        menu.add(0, 1, "Stop RSS Service", null);
        menu.add(0, 2, "Add New Feed", null);
        menu.add(0, 3, "Delete Feed", null);
        menu.add(0, 4, "Update All Feeds", null);
        
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
        switch (item.getId()){
            case 0:
              showAlert(null, "You clicked 'start'!", "ok", null, false, null);
              break;
            case 1:
              showAlert(null, "You clicked stop!", "ok", null, false, null);
              break;
            case 2:
                showAlert(null, "You clicked 'Add'!", "ok", null, false, null);
                break;
            case 3:
                showAlert(null, "You clicked 'Delete'!", "ok", null, false, null);
                break;
            case 4:
                showAlert(null, "You clicked 'Update'!", "ok", null, false, null);
                break;
            default:
                showAlert(null, "I have no idea what you clicked!", "ok", null, false, null);
                break;
            }
        return true;
    }
    
    
    // Our private ArrayAdapter implementation that returns a bold TextView for 
    // RSS items that are unread, or a normal TextView for items that have been read.
// BEGIN_INCLUDE(2_3)    
    private class RssDataAdapter<T> extends ArrayAdapter<T> {
        public RssDataAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
        }
// END_INCLUDE(2_3)       
        // Here's our only important override--returning the list item.
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            TextView view = null;
            // Get the item from the underlying array,
            // Create a TextView wrapper, and change the typeface, if necessary.
            RssItem item = (RssItem)this.getItem(position);
            if(item != null)
            {
                view = new TextView(parent.getContext());
                view.setText(item.toString());
                
                if(! item.hasBeenRead){
                    Typeface type = view.getTypeface();
                    view.setTypeface(Typeface.create(type, Typeface.BOLD_ITALIC));
                }
            }
            return view;    
        }
     }

//BEGIN_INCLUDE(2_1) 
    // Method to initialize our list of RSS items.
    private ArrayList<RssItem> initializeList(){
        ArrayList<RssItem> list = new ArrayList<RssItem>();
        list.add(new RssItem("http://www.sciam.com/xml/sciam.xml", "Scientific American"));
        list.add(new RssItem("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml", "BBC"));
        list.add(new RssItem("http://www.theonion.com/content/feeds/daily.", "The Onion"));
        list.add(new RssItem("http://feeds.engadget.com/weblogsinc/engadget", "Engadget"));
        return list;
    }
//END_INCLUDE(2_1)     
}
