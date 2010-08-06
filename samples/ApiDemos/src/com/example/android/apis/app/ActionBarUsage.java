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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.android.apis.R;

/**
 * This demonstrates idiomatic usage of the Action Bar. The theme Theme.WithActionBar
 * is specified for this activity in AndroidManifest.xml and a menu resource is used
 * to populate the menu data itself. If you'd like to see how these things work under
 * the hood, see ActionBarMechanics.
 */
public class ActionBarUsage extends Activity {
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Toast.makeText(this, "Selected Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        return true;
    }

    // This method is specified as an onClick handler in the menu xml and will
    // take precedence over the Activity's onOptionsItemSelected method.
    // See res/menu/actions.xml for more info.
    public void onSearch(MenuItem item) {
        Toast.makeText(this, "Searching...", Toast.LENGTH_SHORT).show();
    }
}
