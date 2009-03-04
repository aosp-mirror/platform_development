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
import android.os.Bundle;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

//BEGIN_INCLUDE(1_1)  
public class MyRssReader extends Activity {
    /** Called with the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        // Load screen layout.
        setContentView(R.layout.main_screen);

//END_INCLUDE(1_1)
//BEGIN_INCLUDE(1_2)        
        // Load some simple values into the ListView
        mRssList = (ListView) findViewById(R.id.rssListView);
        mRssList.setAdapter(
                new ArrayAdapter<String>(
                        this, 
                        R.layout.list_element, 
                        new String[] { "Scientific American", "BBC", "The Onion", "Engadget" }));
//END_INCLUDE(1_2)
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
        switch (item.getId()) {
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

    ListView mRssList;
}
